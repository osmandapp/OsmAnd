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
	init {
		require(schemaVersion > 0) { "Route details schema version must be positive" }
		require(points.zipWithNext().all { (first, second) ->
			first.distanceToFinishMeters >= second.distanceToFinishMeters
		}) { "Android route distances to finish must be ordered" }
		if (points.isNotEmpty()) {
			require(points.last().distanceToFinishMeters == 0) {
				"The final Android route point must have zero distance to finish"
			}
			require(points.first().distanceToFinishMeters == summary.totalDistanceMeters) {
				"Route summary distance must match the first Android listDistance entry"
			}
			require(currentRoutePointIndex in 0..points.size) {
				"Current route point index must reference the geometry or Android's finished-route sentinel"
			}
		} else {
			require(summary.totalDistanceMeters == 0 && currentRoutePointIndex == 0) {
				"An empty route must have zero distance and current point index"
			}
		}
		require(segments.all { segment ->
			segment.routePointStartIndex in points.indices && segment.routePointEndIndex in points.indices
		}) { "Route segment references a point outside the route geometry" }
		require(maneuvers.all { it.routePointOffset in points.indices }) {
			"Route maneuver references a point outside the route geometry"
		}
		require(currentDirectionIndex >= 0 && currentDirectionIndex <= maneuvers.size) {
			"Current direction index must be a direction or the end sentinel"
		}
		require(nextIntermediateIndex >= 0 && nextIntermediateIndex <= intermediateRoutePointOffsets.size) {
			"Next intermediate index must reference an intermediate or the end sentinel"
		}
		require(intermediateRoutePointOffsets.all { it in points.indices }) {
			"Intermediate point offset references a point outside the route geometry"
		}
	}

	companion object {
		const val CURRENT_SCHEMA_VERSION = 1
	}
}
