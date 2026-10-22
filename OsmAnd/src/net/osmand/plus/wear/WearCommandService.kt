package net.osmand.plus.wear

import android.util.Log

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

import net.osmand.PlatformUtil
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
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
		val sourceNodeId = event.sourceNodeId
		app.runInUIThread {
			// Any contact from the watch means someone is looking, so start following the route.
			WearBridge.start(app)
			handle(command, sourceNodeId)
			WearBridge.publish(app)
		}
	}

	private fun handle(command: WearCommand, sourceNodeId: String) {
		Log.d(TAG, "handling $command")
		when (command) {
			is WearCommand.RequestState -> {
				// SPIKE HOOK, to be removed: runs the renderer probe when a marker file is
				// present, because the development plugin's own row does not respond to taps.
				val marker = app.getAppPath("wear_map_probe")
				if (marker.exists()) {
					// The file's contents, when a number, say how many seconds to hold the
					// renderer up for — a battery reading needs a steady state to measure.
					val hold = marker.readText().trim().toIntOrNull() ?: 0
					marker.delete()
					WearMapProbe.run(app, 384, 384, 2.0f, hold) { result ->
						app.runInUIThread { app.showToastMessage(result) }
					}
				}
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

			is WearCommand.MarkMarkerPassed -> {
				val marker = app.mapMarkersHelper.getMapMarker(command.id)
				if (marker != null) {
					app.mapMarkersHelper.moveMapMarkerToHistory(marker)
				} else {
					LOG.warn("Watch asked to pass an unknown marker: " + command.id)
				}
			}

			is WearCommand.MoveMarkerToTop -> {
				val marker = app.mapMarkersHelper.getMapMarker(command.id)
				if (marker != null) {
					app.mapMarkersHelper.moveMarkerToTop(marker)
				} else {
					LOG.warn("Watch asked to raise an unknown marker: " + command.id)
				}
			}

			is WearCommand.AddMarkerHere -> addMarkerAtCurrentLocation()

			is WearCommand.StartMapStream ->
				mapStreamer(app).start(sourceNodeId, command.width, command.height, command.density)

			is WearCommand.StopMapStream -> mapStreamer(app).stop()

			is WearCommand.PauseMapStream -> mapStreamer(app).setPaused(command.paused)

			is WearCommand.ZoomMap -> mapStreamer(app).zoom(command.factor, command.seq)

			is WearCommand.PanMap -> mapStreamer(app).pan(command.dx, command.dy, command.seq)

			is WearCommand.RecenterMap -> mapStreamer(app).recenter(command.seq)

			is WearCommand.SetMapRenderer -> mapStreamer(app).setLegacyRenderer(command.legacy)

			is WearCommand.PreviewRoute -> previewRoute(command)

			is WearCommand.StartNavigation -> app.osmandMap.mapActions.startNavigation()

			is WearCommand.CancelRoutePreview ->
				app.osmandMap.mapActions.stopNavigationWithoutConfirm()

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

	/**
	 * Works the route out and stops there. Planning mode is what OsmAnd calls a route it has
	 * calculated but is not following, which is exactly a preview; setting off later is then
	 * the same single call the phone's own button makes.
	 */
	private fun previewRoute(command: WearCommand.PreviewRoute) {
		val target = LatLon(command.latitude, command.longitude)
		val description = PointDescription(PointDescription.POINT_TYPE_LOCATION, command.name)
		app.targetPointsHelper.navigateToPoint(target, true, -1, description)
		app.osmandMap.mapActions.enterRoutePlanningMode(null, null)
	}

	private fun addMarkerAtCurrentLocation() {
		// A stale fix still says where the phone was; the map centre, which getDefaultLocation
		// would fall back to, says only where someone last panned to.
		val provider = app.locationProvider
		val location = provider.lastKnownLocation ?: provider.lastStaleKnownLocation
		if (location == null) {
			LOG.warn("Watch asked for a marker here, but the phone has no fix")
			return
		}
		app.mapMarkersHelper.addMapMarker(
			LatLon(location.latitude, location.longitude),
			PointDescription(PointDescription.POINT_TYPE_LOCATION, ""),
			null
		)
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
		/**
		 * One streamer per process: the renderer it owns is expensive enough that a second
		 * would be a bug, and the service is recreated for every message.
		 */
		@Volatile
		private var streamer: WearMapStreamer? = null

		@Synchronized
		private fun mapStreamer(app: OsmandApplication): WearMapStreamer =
			streamer ?: WearMapStreamer(app).also { streamer = it }

		private const val TAG = "OsmAndWear"
		private val LOG = PlatformUtil.getLog(WearCommandService::class.java)
	}
}
