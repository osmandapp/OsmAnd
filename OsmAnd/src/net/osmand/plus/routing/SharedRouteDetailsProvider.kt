package net.osmand.plus.routing

import android.content.Context
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.plus.R
import net.osmand.router.TurnType
import net.osmand.shared.routing.details.ILocationAccessor
import net.osmand.shared.routing.details.IManeuverAccessor
import net.osmand.shared.routing.details.IManeuverMetricsAccessor
import net.osmand.shared.routing.details.RouteCumulativeInfo
import net.osmand.shared.routing.details.RouteGeometryCalculator
import net.osmand.shared.routing.details.RouteManeuverCalculator

/**
 * Android compatibility entry point for the shared route-details backend.
 *
 * Android navigation and UI models remain the public compatibility surface. This provider exposes
 * route coordinates through a read-only accessor, copies only the smaller structured inputs needed
 * by a shared calculation, and maps its result back to those existing types.
 */
object SharedRouteDetailsProvider {

	@JvmStatic
	fun calculateDistancesToFinish(locations: List<Location>): IntArray =
		IntArray(locations.size).also { result -> calculateDistancesToFinish(locations, result) }

	@JvmStatic
	fun calculateDistancesToFinish(locations: List<Location>, result: IntArray) {
		RouteGeometryCalculator.calculateInto(LocationAccessor(locations), result)
	}

	@JvmStatic
	fun updateDirectionDistancesAndTimes(
		directions: List<RouteDirectionInfo>,
		distanceToFinishMeters: IntArray,
	) {
		val update = RouteManeuverCalculator.calculateDistanceAndTimeUpdates(
			ManeuverAccessor(directions),
			distanceToFinishMeters,
		)
		for (index in directions.indices) {
			val direction = directions[index]
			direction.distance = update.distanceMeters[index]
			direction.afterLeftTime = update.afterLeftTimeSeconds[index]
		}
	}

	@JvmStatic
	fun getCumulativeInfoByPosition(
		directions: List<RouteDirectionInfo>,
	): List<RouteCumulativeInfo> = RouteManeuverCalculator.cumulativeInfoByPosition(
		ManeuverMetricsAccessor(directions),
	)

	@JvmStatic
	fun getCumulativeInfoAtRoutePoints(
		directions: List<RouteDirectionInfo>,
		distanceToFinishMeters: IntArray,
		currentRoutePointIndex: Int,
		currentDirectionIndex: Int,
		routePointOffsets: IntArray,
	): List<RouteCumulativeInfo> = RouteManeuverCalculator.cumulativeInfoAtRoutePoints(
		ManeuverAccessor(directions),
		distanceToFinishMeters,
		currentRoutePointIndex,
		currentDirectionIndex,
		routePointOffsets,
	)

	@JvmStatic
	fun calculateIntermediateIndexes(
		context: Context?,
		locations: List<Location>,
		intermediates: List<LatLon>?,
		directions: MutableList<RouteDirectionInfo>,
		intermediatePoints: IntArray,
	) {
		if (intermediates.isNullOrEmpty()) {
			return
		}
		val calculation = RouteManeuverCalculator.calculateIntermediateIndexesFromAccessors(
			LocationAccessor(locations),
			ManeuverAccessor(directions),
			LatLonAccessor(intermediates),
		)
		for (insertion in calculation.insertions) {
			val toSplit = directions[insertion.directionIndex]
			val direction = RouteDirectionInfo(
				insertion.averageSpeedMetersPerSecond,
				TurnType.straight(),
			).apply {
				ref = toSplit.ref
				streetName = toSplit.streetName
				routeDataObject = toSplit.routeDataObject
				destinationName = toSplit.destinationName
				routePointOffset = insertion.routePointOffset
				setDescriptionRoute(requireNotNull(context).getString(R.string.route_head))
			}
			directions.add(insertion.directionIndex, direction)
		}
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
		val index = RouteGeometryCalculator.locationIndexByDistance(
			LocationAccessor(locations),
			currentRoutePointIndex,
			distanceMeters,
		)
		if (index < 0) {
			return null
		}
		return locations[index]
	}

	private class LocationAccessor(
		private val locations: List<Location>,
	) : ILocationAccessor {
		override fun getLocationsCount(): Int = locations.size

		override fun getLatitude(index: Int): Double = locations[index].latitude

		override fun getLongitude(index: Int): Double = locations[index].longitude
	}

	private class LatLonAccessor(
		private val locations: List<LatLon>,
	) : ILocationAccessor {
		override fun getLocationsCount(): Int = locations.size

		override fun getLatitude(index: Int): Double = locations[index].latitude

		override fun getLongitude(index: Int): Double = locations[index].longitude
	}

	private class ManeuverAccessor(
		private val directions: List<RouteDirectionInfo>,
	) : IManeuverAccessor {
		override fun getManeuversCount(): Int = directions.size

		override fun getRoutePointOffset(index: Int): Int = directions[index].routePointOffset

		override fun getAverageSpeedMetersPerSecond(index: Int): Float =
			directions[index].averageSpeed
	}

	private class ManeuverMetricsAccessor(
		private val directions: List<RouteDirectionInfo>,
	) : IManeuverMetricsAccessor {
		override fun getManeuversCount(): Int = directions.size

		override fun getDistanceMeters(index: Int): Int = directions[index].distance

		override fun getExpectedTimeSeconds(index: Int): Int = directions[index].expectedTime
	}
}
