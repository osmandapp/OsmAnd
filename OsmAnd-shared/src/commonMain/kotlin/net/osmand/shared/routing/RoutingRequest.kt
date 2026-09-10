package net.osmand.shared.routing

import kotlin.jvm.JvmField

/**
 * What a route calculation is asked for: where it starts, where it has to reach, on what profile,
 * and where to report progress. Not how it is worked out - the search itself, its tile cache and its
 * graph nodes belong to whichever planner runs, and stay there.
 *
 * This is the half of the routing context that crosses the boundary to the C++ router. Every one of
 * the twenty two fields `native/src/java_wrap.cpp` resolves on
 * `net/osmand/router/RoutingContext` is declared here, and that still works because Java's
 * `RoutingContext` extends this class: `GetFieldID` on a subclass resolves fields declared in a
 * superclass, whatever their visibility. So the core keeps looking the subclass up by name and
 * needs no change.
 *
 * The fields are `@JvmField` for a different reason: the java planner reads and assigns them as
 * fields (`ctx.startX`), which Kotlin accessors would break, and javac catches that.
 *
 * Renaming one of them, or changing its type, still means changing `java_wrap.cpp` in the same
 * breath. No build says so - the app consumes OsmAndCore as a prebuilt snapshot, so a mismatch
 * first shows up as a failed route calculation on a device.
 */
open class RoutingRequest(
	/** The profile and the limits this calculation runs under. Read by the core. */
	@JvmField val config: RoutingConfiguration,
	/** How much of the network the calculation may see. The core reads its `ordinal()`. */
	@JvmField val calculationMode: RouteCalculationMode
) {

	// 0. The native router and its session, so several routes can reuse one C++ context

	/**
	 * The C++ router, or null when there is none and the java planner has to do the work: the
	 * server and the tools always, android in SAFE_MODE. Read as "is native routing available"
	 * in a dozen places.
	 */
	@JvmField var nativeLib: NativeRouting? = null

	@JvmField var nativeRoutingContext: Long = 0

	@JvmField var keepNativeRoutingContext: Boolean = false

	/** Let the core prepare the turns as well. OK for tests and tools, not for the Android UI. */
	@JvmField var requestNativePrepareResult: Boolean = false

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
