package net.osmand.plus.wear

import android.util.Log

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin
import net.osmand.wear.api.WearCodec
import net.osmand.wear.api.WearCommand
import net.osmand.wear.api.WearProtocol

/**
 * Applies actions coming from the watch and echoes the resulting state back.
 *
 * The echo is what makes the watch's optimistic UI safe: whatever it drew on tap is
 * replaced by the truth as soon as the phone has applied — or refused — the command.
 */
class WearCommandService : WearableListenerService() {

	private val app: OsmandApplication
		get() = applicationContext as OsmandApplication

	override fun onMessageReceived(event: MessageEvent) {
		if (event.path != WearProtocol.PATH_COMMAND) {
			return
		}
		val command = WearCodec.decodeCommand(event.data)
		if (command == null) {
			LOG.warn("Dropped an unreadable command from the watch")
			return
		}
		app.runInUIThread {
			// Any contact from the watch means someone is looking, so start following the route.
			WearBridge.start(app)
			handle(command)
			WearBridge.publish(app)
		}
	}

	private fun handle(command: WearCommand) {
		Log.d(TAG, "handling $command")
		when (command) {
			is WearCommand.RequestState -> {
				// Nothing to apply: the publish below is the whole point of this command.
			}

			is WearCommand.StopNavigation -> app.stopNavigation()

			is WearCommand.PauseNavigation -> {
				val routingHelper = app.routingHelper
				if (command.paused) {
					routingHelper.pauseNavigation()
				} else {
					routingHelper.setRoutePlanningMode(false)
					routingHelper.setFollowingMode(true)
				}
			}

			is WearCommand.StartRecording -> monitoringPlugin()?.startGPXMonitoring(null)

			// The plugin exposes a single toggle; the watch sends the direction it means, so a
			// command that arrives twice cannot flip the session the wrong way.
			is WearCommand.PauseRecording -> monitoringPlugin()?.takeIf { it.isRecordingTrack }
				?.pauseOrResumeRecording()

			is WearCommand.ResumeRecording -> monitoringPlugin()?.takeIf { !it.isRecordingTrack }
				?.pauseOrResumeRecording()

			// Silent saves: the sheet that renames a track belongs to whoever is looking at the
			// phone, and nobody is — the request came from the wrist.
			// Saving runs in the background, so the snapshot published right after this command
			// would still report unsaved data — that is, a paused session. The callback publishes
			// again once the track is actually written.
			is WearCommand.FinishRecording -> monitoringPlugin()
				?.saveCurrentTrack({ WearBridge.publish(app) }, null, true, false, false)

			// Keeps writing points, unlike Finish: the no-argument overload would have stopped
			// the session, which is the opposite of what this button says.
			is WearCommand.SaveAndContinueRecording -> monitoringPlugin()
				?.saveCurrentTrack({ WearBridge.publish(app) }, null, false, false, false)

			is WearCommand.SelectProfile -> {
				val mode = ApplicationMode.valueOfStringKey(command.appModeKey, null)
				if (mode != null) {
					app.settings.setApplicationMode(mode)
				} else {
					LOG.warn("Watch asked for an unknown profile: " + command.appModeKey)
				}
			}

			is WearCommand.SetPreference -> applyPreference(command)
		}
	}

	private fun monitoringPlugin(): OsmandMonitoringPlugin? =
		PluginsHelper.getActivePlugin(OsmandMonitoringPlugin::class.java)

	private fun applyPreference(command: WearCommand.SetPreference) {
		// Routed through the very entry point the AIDL API uses, so the watch is just another
		// external consumer rather than a second settings code path with its own quirks.
		val settings = app.settings
		val preference = settings.getPreference(command.prefId)
		if (preference == null || !settings.isExportAvailableForPref(preference)) {
			LOG.warn("Watch asked for an unavailable preference: " + command.prefId)
			return
		}
		val mode = ApplicationMode.valueOfStringKey(command.appModeKey, settings.applicationMode)
		settings.setPreference(command.prefId, command.value, mode)
	}

	companion object {
		private const val TAG = "OsmAndWear"
		private val LOG = PlatformUtil.getLog(WearCommandService::class.java)
	}
}
