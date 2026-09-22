package net.osmand.plus.wear

import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin
import net.osmand.plus.routing.NextDirectionInfo
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.OsmAndFormatterParams
import net.osmand.wear.api.AppModeInfo
import net.osmand.router.TurnType
import net.osmand.wear.api.ManeuverInfo
import net.osmand.wear.api.NavigationState
import net.osmand.wear.api.PhoneState
import net.osmand.wear.api.RecordingState
import net.osmand.wear.api.WearProtocol

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

	private val turnIcons = WearTurnIconRenderer(app)

	/** A snapshot plus the arrow images its manoeuvres refer to by key. */
	data class Snapshot(val state: PhoneState, val icons: Map<String, ByteArray>)

	fun build(): Snapshot {
		val collectedIcons = mutableMapOf<String, ByteArray>()
		val state = PhoneState(
			updatedAt = System.currentTimeMillis(),
			appMode = buildAppMode(),
			navigation = buildNavigation(collectedIcons),
			recording = buildRecording()
		)
		return Snapshot(state, collectedIcons)
	}

	private fun buildAppMode(): AppModeInfo {
		val mode = app.settings.APPLICATION_MODE.get()
		return AppModeInfo(key = mode.stringKey, title = mode.toHumanString())
	}

	private fun buildNavigation(icons: MutableMap<String, ByteArray>): NavigationState? {
		val routingHelper = app.routingHelper
		if (!routingHelper.isRouteCalculated) {
			return null
		}
		if (!routingHelper.isFollowingMode && !routingHelper.isPauseNavigation) {
			return null
		}

		val leftTime = routingHelper.leftTime
		val speed = routingHelper.lastFixedLocation?.takeIf { it.hasSpeed() }?.speed

		return NavigationState(
			leftDistanceText = formatDistance(routingHelper.leftDistance),
			leftTimeText = OsmAndFormatter.getFormattedDuration(leftTime.toLong(), app),
			etaText = formatEta(leftTime),
			speedText = speed?.let { OsmAndFormatter.getFormattedSpeed(it, app) },
			deviatedFromRoute = routingHelper.isDeviatedFromRoute,
			paused = routingHelper.isPauseNavigation,
			maneuvers = buildManeuvers(icons)
		)
	}

	/**
	 * The nearest manoeuvres ahead. Off-route is reported as a single synthetic manoeuvre so the
	 * watch has something to show instead of an empty list while OsmAnd recalculates.
	 */
	private fun buildManeuvers(icons: MutableMap<String, ByteArray>): List<ManeuverInfo> {
		val routingHelper = app.routingHelper
		if (routingHelper.isDeviatedFromRoute) {
			val offRoute = TurnType.valueOf(TurnType.OFFR, false)
			return listOf(
				toManeuver(offRoute, routingHelper.routeDeviation.toInt(), null, 0, icons)
			)
		}

		val maneuvers = mutableListOf<ManeuverInfo>()
		// Separate buffers: getNextRouteDirectionInfoAfter() writes into the one it is handed,
		// so reusing a single instance would overwrite the entry already collected.
		var previous = routingHelper.getNextRouteDirectionInfo(NextDirectionInfo(), true)
		while (previous?.directionInfo != null && maneuvers.size < MAX_MANEUVERS) {
			val direction = previous.directionInfo
			maneuvers.add(
				toManeuver(
					turnType = direction.turnType,
					distanceTo = previous.distanceTo,
					streetName = direction.getDescriptionRoutePart(app),
					index = maneuvers.size,
					icons = icons
				)
			)
			previous = routingHelper.getNextRouteDirectionInfoAfter(previous, NextDirectionInfo(), true)
		}
		return maneuvers
	}

	private fun toManeuver(
		turnType: TurnType?,
		distanceTo: Int,
		streetName: String?,
		index: Int,
		icons: MutableMap<String, ByteArray>
	): ManeuverInfo {
		val iconKey = turnType?.let { type ->
			turnIcons.render(type)?.let { png ->
				WearProtocol.turnIconKey(index).also { icons[it] = png }
			}
		}
		return ManeuverInfo(
			turnType = turnType?.value ?: TurnType.C,
			exitOut = turnType?.exitOut ?: 0,
			iconKey = iconKey,
			distanceMeters = quantize(distanceTo),
			distanceText = formatDistance(distanceTo),
			streetName = streetName?.takeIf { it.isNotEmpty() }
		)
	}

	/**
	 * Rounds to [DISTANCE_STEP_METERS] so a snapshot only differs when something a rider could
	 * notice has changed — the publisher drops payloads identical to the previous one.
	 */
	private fun quantize(meters: Int): Int =
		if (meters <= 0) 0 else (meters / DISTANCE_STEP_METERS) * DISTANCE_STEP_METERS

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

	companion object {
		private const val MAX_MANEUVERS = 3
		private const val DISTANCE_STEP_METERS = 10
	}
}
