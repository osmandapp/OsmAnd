package net.osmand.shared.routing.details

/**
 * Read-only boundary for maneuver values needed while preparing a route.
 *
 * Platform direction arrays expose these values directly so geometry and intermediate-point
 * calculations do not need complete [RouteManeuver] snapshots.
 */
interface IManeuverAccessor {
	fun getManeuversCount(): Int

	fun getRoutePointOffset(index: Int): Int

	fun getAverageSpeedMetersPerSecond(index: Int): Float
}

internal class ManeuverAccessor(
	private val maneuvers: List<RouteManeuver>,
) : IManeuverAccessor {
	override fun getManeuversCount(): Int = maneuvers.size

	override fun getRoutePointOffset(index: Int): Int = maneuvers[index].routePointOffset

	override fun getAverageSpeedMetersPerSecond(index: Int): Float =
		maneuvers[index].averageSpeedMetersPerSecond
}
