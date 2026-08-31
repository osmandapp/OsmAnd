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
)
