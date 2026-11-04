package net.osmand.shared.routing

/**
 * One road as a route passes along it: which road it is, where the route enters and leaves it, and
 * whether that runs with the direction the road was drawn in.
 *
 * [VehicleRouter] asks a routing segment for nothing else. This interface lets it work with a
 * traversal without depending on the planner's graph representation.
 */
interface RoadTraversal {

	fun getRoad(): RouteDataObject

	/** Index of the point the route enters the road at. */
	fun getSegmentStart(): Short

	/** Index of the point the route leaves the road at. */
	fun getSegmentEnd(): Short

	/** True when the route runs along the road in the direction it was drawn in. */
	fun isPositive(): Boolean
}
