package net.osmand.shared.routing

import kotlin.jvm.JvmField

/**
 * What a route calculation is asked for: where it starts, where it has to reach, on what profile,
 * and where to report progress. Not how it is worked out - the search itself, its tile cache and its
 * graph nodes belong to whichever planner runs, and stay there.
 *
 * This is the request half of `net.osmand.router.RoutingContext`, which stays whole in OsmAnd-java
 * for android and tools. The copy exists for iOS: the shared turn preparation reads the profile, the
 * calculation mode and the side of the road off it, and iOS fills those in from its C++ context.
 * The field names are java's, so the two can be compared side by side.
 */
open class RoutingRequest(
	/** The profile and the limits this calculation runs under. */
	@JvmField val config: RoutingConfiguration,
	/** How much of the network the calculation may see. */
	@JvmField val calculationMode: RouteCalculationMode
) {

	// 1. Where the route runs

	@JvmField var startX: Int = 0
	@JvmField var startY: Int = 0
	@JvmField var startRoadId: Long = 0
	@JvmField var startSegmentInd: Int = 0
	@JvmField var startTransportStop: Boolean = false

	@JvmField var targetX: Int = 0
	@JvmField var targetY: Int = 0
	@JvmField var targetRoadId: Long = 0
	@JvmField var targetSegmentInd: Int = 0
	@JvmField var targetTransportStop: Boolean = false

	@JvmField var intermediatesX: IntArray? = null
	@JvmField var intermediatesY: IntArray? = null

	@JvmField var publicTransport: Boolean = false

	/** Names of the regions the ends fall into, so a route across a missing map can be reported. */
	@JvmField var regionsCoveringStartAndTargets: Array<String> = emptyArray()

	// 2. Progress, and a route to lean on

	/** Which side of the road the driver is on, which decides what a sharp turn is called. */
	@JvmField var leftSideNavigation: Boolean = false

	@JvmField var calculationProgress: RouteCalculationProgress? = null

	@JvmField var precalculatedRouteDirection: PrecalculatedRouteDirection? = null

	// 3. Counters the core writes back into

	@JvmField var alertFasterRoadToVisitedSegments: Int = 0
	@JvmField var alertSlowerSegmentedWasVisitedEarlier: Int = 0

	// What the request says about the profile, which lives on the configuration

	fun setRouter(router: GeneralRouter) {
		config.router = router
	}

	fun getRouter(): VehicleRouter = config.router

	fun setHeuristicCoefficient(heuristicCoefficient: Float) {
		config.heuristicCoefficient = heuristicCoefficient
	}

	fun planRouteIn2Directions(): Boolean = config.planRoadDirection == 0

	fun getPlanRoadDirection(): Int = config.planRoadDirection

	// What the request says about how far it has got

	fun getVisitedSegments(): Int = calculationProgress?.visitedSegments ?: 0

	fun getLoadedTiles(): Int = calculationProgress?.loadedTiles ?: 0
}
