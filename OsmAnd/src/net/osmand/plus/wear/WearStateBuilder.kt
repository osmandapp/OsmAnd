package net.osmand.plus.wear

import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin
import net.osmand.plus.routing.NextDirectionInfo
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.OsmAndFormatterParams
import net.osmand.wear.api.AppModeInfo
import net.osmand.wear.api.NavigationState
import net.osmand.wear.api.PhoneState
import net.osmand.wear.api.RecordingState

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Flattens live OsmAnd state into the snapshot sent to the watch.
 *
 * Formatting happens here rather than on the watch: unit settings and locale live on the
 * phone, and duplicating that logic in the watch APK would mean pulling OsmAnd code into it.
 */
class WearStateBuilder(private val app: OsmandApplication) {

	fun build(): PhoneState = PhoneState(
		updatedAt = System.currentTimeMillis(),
		appMode = buildAppMode(),
		navigation = buildNavigation(),
		recording = buildRecording()
	)

	private fun buildAppMode(): AppModeInfo {
		val mode = app.settings.APPLICATION_MODE.get()
		return AppModeInfo(key = mode.stringKey, title = mode.toHumanString())
	}

	private fun buildNavigation(): NavigationState? {
		val routingHelper = app.routingHelper
		if (!routingHelper.isRouteCalculated) {
			return null
		}
		if (!routingHelper.isFollowingMode && !routingHelper.isPauseNavigation) {
			return null
		}

		val deviated = routingHelper.isDeviatedFromRoute
		val calc = NextDirectionInfo()
		val next = routingHelper.getNextRouteDirectionInfo(calc, true)
		val directionInfo = next?.directionInfo

		val distanceToTurn = if (deviated) routingHelper.routeDeviation.toInt() else next?.distanceTo ?: 0
		val leftTime = routingHelper.leftTime

		return NavigationState(
			turnType = directionInfo?.turnType?.value ?: 0,
			distanceToTurnMeters = distanceToTurn,
			distanceToTurnText = formatDistance(distanceToTurn),
			streetName = directionInfo?.getDescriptionRoutePart(app),
			leftDistanceText = formatDistance(routingHelper.leftDistance),
			leftTimeText = OsmAndFormatter.getFormattedDuration(leftTime.toLong(), app),
			etaText = formatEta(leftTime),
			deviatedFromRoute = deviated,
			paused = routingHelper.isPauseNavigation
		)
	}

	private fun buildRecording(): RecordingState? {
		val plugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin::class.java) ?: return null
		val trackHelper = app.savingTrackHelper
		return RecordingState(
			active = plugin.isRecordingTrack,
			paused = false,
			distanceText = formatDistance(trackHelper.distance.toInt()),
			durationText = OsmAndFormatter.getFormattedDuration(trackHelper.duration / 1000, app),
			pointCount = trackHelper.points
		)
	}

	private fun formatDistance(meters: Int): String =
		OsmAndFormatter.getFormattedDistance(meters.toFloat(), app, OsmAndFormatterParams.USE_LOWER_BOUNDS)

	private fun formatEta(leftTimeSeconds: Int): String =
		SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
			.format(Date(System.currentTimeMillis() + leftTimeSeconds * 1000L))
}
