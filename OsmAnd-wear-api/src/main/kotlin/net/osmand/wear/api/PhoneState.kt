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
	val location: LocationState? = null
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
	val turnType: Int = 0,
	val distanceToTurnMeters: Int = 0,
	val distanceToTurnText: String = "",
	val streetName: String? = null,
	val leftDistanceText: String = "",
	val leftTimeText: String = "",
	val etaText: String = "",
	val speedText: String? = null,
	val deviatedFromRoute: Boolean = false,
	val paused: Boolean = false
)

/** Trip recording status; null means the monitoring plugin is off or unavailable. */
@Serializable
data class RecordingState(
	val active: Boolean = false,
	val paused: Boolean = false,
	val distanceText: String = "",
	val durationText: String = "",
	val pointCount: Int = 0
)

@Serializable
data class LocationState(
	val latitude: Double = 0.0,
	val longitude: Double = 0.0,
	val altitudeText: String? = null,
	val accuracyText: String? = null
)
