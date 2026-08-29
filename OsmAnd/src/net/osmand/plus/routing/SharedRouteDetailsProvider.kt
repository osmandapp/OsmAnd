package net.osmand.plus.routing

import android.content.Context
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.plus.R
import net.osmand.router.TurnType
import net.osmand.shared.data.KLatLon
import net.osmand.shared.routing.details.RouteCumulativeInfo
import net.osmand.shared.routing.details.RouteGeometryCalculation
import net.osmand.shared.routing.details.RouteGeometryCalculator
import net.osmand.shared.routing.details.RouteManeuverCalculator

/**
 * Android compatibility entry point for the shared route-details backend.
 *
 * Android navigation and UI models remain the public compatibility surface. This provider copies
 * only the inputs needed by a shared calculation and maps its result back to those existing types.
 */
object SharedRouteDetailsProvider {

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
	fun calculateIntermediateIndexes(
		context: Context?,
		locations: List<Location>,
		intermediates: List<LatLon>?,
		directions: MutableList<RouteDirectionInfo>,
		intermediatePoints: IntArray,
	) {
		if (intermediates == null) {
			return
		}
		val originalManeuvers = directions.map(RouteCalculationResultSnapshotAdapter::copyManeuver)
		val calculation = RouteManeuverCalculator.calculateIntermediateIndexes(
			locations.map { it.toSharedLocation() },
			originalManeuvers,
			intermediates.map { KLatLon(it.latitude, it.longitude) },
		)
		val updatedDirections = ArrayList<RouteDirectionInfo>(calculation.maneuvers.size)
		var originalIndex = 0
		for (maneuver in calculation.maneuvers) {
			if (originalIndex < originalManeuvers.size &&
				maneuver.routePointOffset == originalManeuvers[originalIndex].routePointOffset) {
				updatedDirections.add(directions[originalIndex])
				originalIndex++
			} else {
				val toSplit = directions[originalIndex]
				updatedDirections.add(RouteDirectionInfo(
					maneuver.averageSpeedMetersPerSecond,
					TurnType.straight(),
				).apply {
					ref = maneuver.ref
					streetName = maneuver.streetName
					routeDataObject = toSplit.routeDataObject
					destinationName = maneuver.destinationName
					routePointOffset = maneuver.routePointOffset
					setDescriptionRoute(requireNotNull(context).getString(R.string.route_head))
				})
			}
		}
		directions.clear()
		directions.addAll(updatedDirections)
		calculation.intermediateDirectionIndices.forEachIndexed { index, directionIndex ->
			intermediatePoints[index] = directionIndex
		}
	}

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
