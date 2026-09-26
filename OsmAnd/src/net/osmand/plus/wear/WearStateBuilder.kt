package net.osmand.plus.wear

import android.location.Location

import net.osmand.plus.OsmandApplication
import net.osmand.plus.mapmarkers.MapMarker
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin
import net.osmand.plus.routing.NextDirectionInfo
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.FormattedValue
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.OsmAndFormatterParams
import net.osmand.router.TurnType
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.wear.api.AppModeInfo
import net.osmand.wear.api.ManeuverInfo
import net.osmand.wear.api.LocationState
import net.osmand.wear.api.MarkerInfo
import net.osmand.wear.api.Metric
import net.osmand.wear.api.NavigationState
import net.osmand.wear.api.PhoneState
import net.osmand.wear.api.ProfileInfo
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

	private val iconRenderer = WearIconRenderer(app)

	/** A snapshot plus the arrow images its manoeuvres refer to by key. */
	data class Snapshot(val state: PhoneState, val icons: Map<String, ByteArray>)

	fun build(): Snapshot {
		val collectedIcons = mutableMapOf<String, ByteArray>()
		val state = PhoneState(
			updatedAt = System.currentTimeMillis(),
			appMode = buildAppMode(),
			navigation = buildNavigation(collectedIcons),
			recording = buildRecording(),
			profiles = buildProfiles(collectedIcons),
			markers = buildMarkers(),
			location = buildLocation(),
			headingDegrees = app.mapViewTrackingUtilities.heading
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
			iconRenderer.renderTurn(type)?.let { png ->
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

		// OsmAnd has no paused flag of its own: a session that has stopped writing points but
		// still holds unsaved data is what the user thinks of as paused.
		val writing = plugin.isRecordingTrack
		val hasData = plugin.hasDataToSave()
		val paused = !writing && hasData

		val analysis = trackAnalysis()
		val speed = app.locationProvider?.lastKnownLocation?.takeIf { it.hasSpeed() && writing }?.speed

		return RecordingState(
			active = writing || hasData,
			paused = paused,
			distance = metric(OsmAndFormatter.getFormattedDistanceValue(trackHelper.distance, app)),
			timeSpan = OsmAndFormatter.getFormattedDuration(trackHelper.duration / 1000, app),
			// The unit travels even when the figure does not: a paused session shows a dash under
			// an unchanged "SPEED KM/H" label rather than dropping the unit with the value.
			speed = metric(OsmAndFormatter.getFormattedSpeedValue(speed ?: 0f, app))
				.let { if (speed == null) it.copy(value = "") else it },
			uphill = elevationMetric(analysis?.diffElevationUp),
			downhill = elevationMetric(analysis?.diffElevationDown),
			pointCount = trackHelper.points
		)
	}

	/**
	 * Track analysis walks every recorded point, so on a long session it is far too expensive to
	 * redo for each published snapshot. The elevation figures move slowly enough that a few
	 * seconds of staleness is invisible.
	 */
	private fun trackAnalysis(): GpxTrackAnalysis? {
		val now = System.currentTimeMillis()
		if (now - lastAnalysisAt < ANALYSIS_INTERVAL_MS && cachedAnalysis != null) {
			return cachedAnalysis
		}
		return try {
			app.savingTrackHelper.currentTrack.getTrackAnalysis(app).also {
				cachedAnalysis = it
				lastAnalysisAt = now
			}
		} catch (e: Exception) {
			cachedAnalysis
		}
	}

	private fun buildProfiles(icons: MutableMap<String, ByteArray>): List<ProfileInfo> {
		val current = app.settings.applicationMode
		// The active profile is the last used one, so it leads; the rest keep OsmAnd's own
		// order, which sortedBy preserves because it is stable.
		return ApplicationMode.values(app).sortedBy { it != current }.map { mode ->
			val iconKey = iconRenderer.renderDrawable(mode.iconRes)?.let { png ->
				WearProtocol.profileIconKey(mode.stringKey).also { icons[it] = png }
			}
			ProfileInfo(
				key = mode.stringKey,
				title = mode.toHumanString(),
				iconKey = iconKey,
				selected = mode == current
			)
		}
	}

	private fun buildMarkers(): List<MarkerInfo> {
		val markers = app.mapMarkersHelper.mapMarkers
		if (markers.isEmpty()) {
			return emptyList()
		}
		val from = currentLocation()
		return markers.take(MAX_MARKERS).map { marker ->
			// distanceBetween reports the bearing out of the marker towards us, the same way
			// MapMarkersBarWidget reads it, so 180 turns it back around to point at the marker.
			val result = FloatArray(2)
			if (from != null) {
				Location.distanceBetween(
					marker.latitude, marker.longitude,
					from.latitude, from.longitude, result
				)
			}
			MarkerInfo(
				id = marker.id,
				name = marker.getName(app),
				distanceText = if (from == null) "" else formatDistance(result[0].toInt()),
				distanceMeters = quantize(result[0].toInt()),
				bearingDegrees = result[1] + 180f,
				colorArgb = app.getColor(MapMarker.getColorId(marker.colorIndex))
			)
		}
	}

	private fun buildLocation(): LocationState? {
		val location = currentLocation() ?: return null
		return LocationState(latitude = location.latitude, longitude = location.longitude)
	}

	private fun currentLocation(): net.osmand.Location? {
		val provider = app.locationProvider
		return provider.lastKnownLocation ?: provider.lastStaleKnownLocation
	}

	private fun metric(formatted: FormattedValue): Metric = Metric(formatted.value, formatted.unit)

	private fun elevationMetric(meters: Double?): Metric =
		if (meters == null || meters.isNaN()) Metric()
		else metric(OsmAndFormatter.getFormattedAltitudeValue(meters, app, app.settings.ALTITUDE_METRIC.get()))

	private fun formatDistance(meters: Int): String =
		OsmAndFormatter.getFormattedDistance(meters.toFloat(), app, OsmAndFormatterParams.USE_LOWER_BOUNDS)

	private fun formatEta(leftTimeSeconds: Int): String =
		SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
			.format(Date(System.currentTimeMillis() + leftTimeSeconds * 1000L))

	@Volatile
	private var cachedAnalysis: GpxTrackAnalysis? = null
	private var lastAnalysisAt = 0L

	companion object {
		private const val ANALYSIS_INTERVAL_MS = 5000L
		private const val MAX_MANEUVERS = 3
		private const val MAX_MARKERS = 10
		private const val DISTANCE_STEP_METERS = 10
	}
}
