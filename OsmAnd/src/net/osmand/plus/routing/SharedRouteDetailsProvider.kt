package net.osmand.plus.routing

import net.osmand.Location
import net.osmand.shared.data.KLatLon
import net.osmand.shared.routing.details.RouteCumulativeInfo
import net.osmand.shared.routing.details.RouteDetailsSnapshot
import net.osmand.shared.routing.details.RouteGeometryCalculation
import net.osmand.shared.routing.details.RouteGeometryCalculator
import net.osmand.shared.routing.details.RouteManeuverCalculator
import net.osmand.shared.routing.details.RouteSummary

/**
 * Android compatibility entry point for the shared route-details backend.
 *
 * Android navigation and UI models remain the public compatibility surface. This provider copies
 * only the inputs needed by a shared calculation and maps its result back to those existing types.
 */
object SharedRouteDetailsProvider {

	@JvmStatic
	fun getSnapshot(route: RouteCalculationResult): RouteDetailsSnapshot = route.routeDetailsSnapshot

	@JvmStatic
	fun getSummary(route: RouteCalculationResult): RouteSummary = getSnapshot(route).summary

	@JvmStatic
	fun calculateDistancesToFinish(locations: List<Location>): IntArray =
		RouteGeometryCalculator.calculate(locations.map { it.toSharedLocation() })
			.distanceToFinishMeters
			.toIntArray()

	@JvmStatic
	fun updateDirectionDistancesAndTimes(
		directions: List<RouteDirectionInfo>,
		distanceToFinishMeters: IntArray,
	) {
		val geometry = RouteGeometryCalculation(distanceToFinishMeters.toList())
		val updated = RouteManeuverCalculator.updateDistancesAndTimes(
			directions.map(RouteCalculationResultSnapshotAdapter::copyManeuver),
			geometry,
		)
		directions.zip(updated).forEach { (direction, maneuver) ->
			direction.distance = maneuver.distanceMeters
			direction.afterLeftTime = maneuver.afterLeftTimeSeconds
		}
	}

	@JvmStatic
	fun getCumulativeInfo(
		position: Int,
		directions: List<RouteDirectionInfo>,
	): RouteCumulativeInfo = RouteManeuverCalculator.cumulativeInfoBefore(
		position,
		directions.map(RouteCalculationResultSnapshotAdapter::copyManeuver),
	)

	@JvmStatic
	fun getRouteLocationByDistance(
		locations: List<Location>,
		currentRoutePointIndex: Int,
		distanceMeters: Int,
	): Location? {
		val sharedLocations = locations.map { it.toSharedLocation() }
		val sharedLocation = RouteGeometryCalculator.locationByDistance(
			sharedLocations,
			currentRoutePointIndex,
			distanceMeters,
		) ?: return null
		val index = sharedLocations.indexOfFirst { it === sharedLocation }
		return locations.getOrNull(index)
	}

	private fun Location.toSharedLocation(): KLatLon = KLatLon(latitude, longitude)
}
