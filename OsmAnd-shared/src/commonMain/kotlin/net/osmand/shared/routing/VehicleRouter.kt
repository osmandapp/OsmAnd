package net.osmand.shared.routing

/**
 * What one vehicle profile makes of a road: whether it may be used at all, how fast it is taken,
 * and what the obstacles and turns along it cost.
 *
 * The planner asks these questions for every road it considers, so everything here is on the hot
 * path of a route calculation.
 */
interface VehicleRouter {

	fun containsAttribute(attribute: String): Boolean

	fun getAttribute(attribute: String): String?

	/** Whether the road is accepted for routing. */
	fun acceptLine(way: RouteDataObject): Boolean

	/** +/- 1 when the road is one way, 0 when it may be taken both ways. */
	fun isOneWay(road: RouteDataObject): Int

	/** Penalty for entering the road, in seconds. */
	fun getPenaltyTransition(road: RouteDataObject): Float

	/** Delay at the point, in seconds, 0 when there is no obstacle. */
	fun defineObstacle(road: RouteDataObject, point: Int, isBackwardDir: Boolean): Float

	/** Delay for the climbs and descents between the two points, in seconds. */
	fun defineHeightObstacle(road: RouteDataObject, startIndex: Short, endIndex: Short): Double

	/** Delay at the point that only routing accounts for, in seconds. */
	fun defineRoutingObstacle(road: RouteDataObject, point: Int, isBackwardDir: Boolean): Float

	/** Speed the router plans with on this road, in m/s. */
	fun defineRoutingSpeed(road: RouteDataObject, dir: Boolean): Float

	/** Speed the vehicle is expected to actually travel at, in m/s. */
	fun defineVehicleSpeed(road: RouteDataObject, dir: Boolean): Float

	/** Factor the speed is multiplied by for g(x) of the A* search. */
	fun defineSpeedPriority(road: RouteDataObject, dir: Boolean): Float

	fun defineDestinationPriority(road: RouteDataObject): Float

	/**
	 * Used for A* routing to calculate g(x).
	 * @return minimal speed at road in m/s
	 */
	fun getDefaultSpeed(): Float

	/**
	 * Used as minimal threshold of default speed.
	 * @return minimal speed at road in m/s
	 */
	fun getMinSpeed(): Float

	/**
	 * Used for A* routing to predict h(x) : it should be great any g(x).
	 * @return maximum speed to calculate shortest distance
	 */
	fun getMaxSpeed(): Float

	/** Whether the profile is aware of road restrictions. */
	fun restrictionsAware(): Boolean

	/** Whether the road supports area routing. */
	fun isArea(obj: RouteDataObject): Boolean

	/** Cost of turning out of [prev] into [segment], in seconds. */
	fun calculateTurnTime(segment: RoadTraversal, prev: RoadTraversal): Double

	/** The same profile with [params] applied. */
	fun build(params: Map<String, String>): VehicleRouter

	fun getProfile(): GeneralRouterProfile
}
