package net.osmand.wear.api

import kotlinx.serialization.Serializable

/**
 * Everything the watch is allowed to know, published by the phone as a single snapshot.
 *
 * Values come in pairs: a raw number for the watch to make decisions on (thresholds,
 * haptics) and a preformatted string for display. Formatting stays on the phone because
 * it depends on OsmAnd's unit settings and locale, which the watch has no access to.
 */
@Serializable
data class PhoneState(
	val protocolVersion: Int = WearProtocol.VERSION,
	val updatedAt: Long = 0L,
	val appMode: AppModeInfo? = null,
	val navigation: NavigationState? = null,
	val recording: RecordingState? = null,
	val location: LocationState? = null,
	/** Profiles offered by the recording profile picker, in the order the phone lists them. */
	val profiles: List<ProfileInfo> = emptyList(),
	/** Active map markers in OsmAnd's own order, so the first one is the one its widget tracks. */
	val markers: List<MarkerInfo> = emptyList()
)

/**
 * One active map marker.
 *
 * [bearingDegrees] is how far to turn an up-pointing arrow so it aims at the marker, already
 * corrected for the phone's heading the same way OsmAnd's marker widget corrects it. The watch
 * has no compass of its own yet, so the arrow is only as fresh as the last snapshot.
 */
@Serializable
data class MarkerInfo(
	val id: String,
	val name: String,
	val distanceText: String = "",
	val distanceMeters: Int = 0,
	val bearingDegrees: Float = 0f,
	/** Marker colour as OsmAnd assigned it, so the watch does not invent its own palette. */
	val colorArgb: Int = 0
)

/**
 * A number with its unit kept apart, because the watch lays them out separately: the unit sits
 * in the small label row ("DISTANCE  KM") and the value below it in display type.
 */
@Serializable
data class Metric(
	val value: String = "",
	val unit: String = ""
)

/** One OsmAnd profile as the watch's picker shows it. */
@Serializable
data class ProfileInfo(
	val key: String,
	val title: String,
	/** Key of the profile glyph carried alongside this snapshot as a Data Layer asset. */
	val iconKey: String? = null,
	val selected: Boolean = false
)

/** Currently selected OsmAnd profile. Settings changes apply to it unless stated otherwise. */
@Serializable
data class AppModeInfo(
	val key: String,
	val title: String
)

/** Populated only while a route is being followed; null means "not navigating". */
@Serializable
data class NavigationState(
	val leftDistanceText: String = "",
	val leftTimeText: String = "",
	val etaText: String = "",
	val speedText: String? = null,
	val deviatedFromRoute: Boolean = false,
	val paused: Boolean = false,
	/** Nearest manoeuvres ahead, closest first. Empty while the route is still calculating. */
	val maneuvers: List<ManeuverInfo> = emptyList()
)

/**
 * One upcoming manoeuvre.
 *
 * [distanceMeters] is quantised on the phone so that a metre-by-metre drift does not produce a
 * new payload on every GPS fix; the watch uses it for thresholds, [distanceText] for display.
 * For the first manoeuvre the distance is measured from the current position, for the rest it
 * is the leg length from the manoeuvre before it — the same convention OsmAnd's own "then in…"
 * announcements use.
 */
@Serializable
data class ManeuverInfo(
	val turnType: Int = 0,
	/** Roundabout exit number; 0 when the turn is not a roundabout. */
	val exitOut: Int = 0,
	/**
	 * Key of the arrow image carried next to this snapshot as a Data Layer asset. The phone
	 * draws it with OsmAnd's own turn drawable, so the watch never reimplements the glyphs.
	 */
	val iconKey: String? = null,
	val distanceMeters: Int = 0,
	val distanceText: String = "",
	val streetName: String? = null
)

/**
 * Trip recording status; null means the monitoring plugin is off or unavailable.
 *
 * OsmAnd keeps no explicit "paused" flag: recording either writes points or it does not, and a
 * paused session is one that has stopped writing while still holding unsaved data. Both states
 * are resolved on the phone so the watch does not have to know that rule.
 */
@Serializable
data class RecordingState(
	/** A session exists — the watch shows the recording pager rather than the start screen. */
	val active: Boolean = false,
	val paused: Boolean = false,
	val distance: Metric = Metric(),
	val timeSpan: String = "",
	/** Empty while paused: the mockups show a dash instead of a stale speed. */
	val speed: Metric = Metric(),
	val uphill: Metric = Metric(),
	val downhill: Metric = Metric(),
	val pointCount: Int = 0
)

@Serializable
data class LocationState(
	val latitude: Double = 0.0,
	val longitude: Double = 0.0,
	val altitudeText: String? = null,
	val accuracyText: String? = null
)
