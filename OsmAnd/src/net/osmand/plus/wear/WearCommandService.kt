package net.osmand.plus.wear

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
			handle(command)
			WearBridge.publish(app)
		}
	}

	private fun handle(command: WearCommand) {
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

			is WearCommand.ToggleRecording -> {
				val plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin::class.java)
				plugin?.setRecordingTrack(command.active)
			}

			is WearCommand.SetPreference -> applyPreference(command)
		}
	}

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
		private val LOG = PlatformUtil.getLog(WearCommandService::class.java)
	}
}
