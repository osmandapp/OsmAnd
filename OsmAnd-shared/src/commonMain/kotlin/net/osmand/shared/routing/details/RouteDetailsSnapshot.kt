package net.osmand.shared.routing.details

import kotlinx.serialization.Serializable

/**
 * Immutable backend snapshot whose schema is defined by Android route-detail source values.
 *
 * It contains no Android resources, localized descriptions, renderer objects, UI state, or native
 * route objects. Active-route indices remain explicit so a later compatibility layer can reproduce
 * Android's complete-route and remaining-route projections without discarding source data.
 */
@Serializable
data class RouteDetailsSnapshot(
	val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
	val points: List<RoutePoint>,
	val segments: List<RouteSegment>,
	val maneuvers: List<RouteManeuver>,
	val events: List<RouteEvent>,
	val summary: RouteSummary,
	val statistics: List<RouteStatistic> = emptyList(),
	val currentRoutePointIndex: Int = 0,
	val currentDirectionIndex: Int = 0,
	val nextIntermediateIndex: Int = 0,
	val intermediateRoutePointOffsets: List<Int> = emptyList(),
) {
	companion object {
		const val CURRENT_SCHEMA_VERSION = 1
	}
}
