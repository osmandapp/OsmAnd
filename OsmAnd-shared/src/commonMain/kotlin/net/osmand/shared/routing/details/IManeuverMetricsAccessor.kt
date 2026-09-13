package net.osmand.shared.routing.details

/**
 * Read-only boundary for the two maneuver values used by cumulative Route Details rows.
 * Platform direction arrays can be read without copying complete [RouteManeuver] objects.
 */
interface IManeuverMetricsAccessor {
	fun getManeuversCount(): Int

	fun getDistanceMeters(index: Int): Int

	fun getExpectedTimeSeconds(index: Int): Int
}

internal class ManeuverMetricsAccessor(
	private val maneuvers: List<RouteManeuver>,
) : IManeuverMetricsAccessor {
	override fun getManeuversCount(): Int = maneuvers.size

	override fun getDistanceMeters(index: Int): Int = maneuvers[index].distanceMeters

	override fun getExpectedTimeSeconds(index: Int): Int = maneuvers[index].expectedTimeSeconds
}
