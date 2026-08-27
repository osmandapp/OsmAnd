package net.osmand.shared.routing.details

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Stable mapping of Android `RouteService` enum constants. */
@Serializable
enum class RouteServiceType {
	@SerialName("osmand")
	OSMAND,

	@SerialName("brouter")
	BROUTER,

	@SerialName("straight")
	STRAIGHT,

	@SerialName("direct_to")
	DIRECT_TO,

	@SerialName("online")
	ONLINE,
}

/**
 * Route-wide values copied from Android `RouteCalculationResult` without reinterpreting zeroes.
 * [totalTimeSeconds] is the first direction's `afterLeftTime`, or zero when no direction exists.
 */
@Serializable
data class RouteSummary(
	val totalDistanceMeters: Int,
	val totalTimeSeconds: Int,
	val profileId: String? = null,
	val routeService: RouteServiceType? = null,
	val routingTimeSeconds: Float = 0f,
	val calculationTimeSeconds: Float = 0f,
	val visitedSegments: Int = 0,
	val loadedTiles: Int = 0,
	val initialCalculation: Boolean = false,
) {
	init {
		require(totalDistanceMeters >= 0) { "Total route distance must not be negative" }
		require(totalTimeSeconds >= 0) { "Total route time must not be negative" }
		require(routingTimeSeconds.isFinite()) { "Routing time must be finite" }
		require(calculationTimeSeconds.isFinite()) { "Calculation time must be finite" }
		require(visitedSegments >= 0) { "Visited segment count must not be negative" }
		require(loadedTiles >= 0) { "Loaded tile count must not be negative" }
	}
}
