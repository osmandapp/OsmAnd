package net.osmand.shared.routing.details

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import kotlin.math.max

/** Exact integer totals used by Android Route Details before a direction row. */
data class RouteCumulativeInfo(
	val distanceMeters: Int,
	val timeSeconds: Int,
)

/** Result of Android-compatible intermediate-point association and direction splitting. */
data class RouteIntermediateCalculation(
	val maneuvers: List<RouteManeuver>,
	val intermediateDirectionIndices: IntArray,
)

/**
 * Shared home for ports of Android direction calculations used by Route Details; Android has no
 * class named `RouteManeuverCalculator`.
 *
 * The arithmetic and indexing follow `RouteCalculationResult.updateDirectionsTime`,
 * `RouteCalculationResult.calculateIntermediateIndexes`, and
 * `RouteDetailsFragment.getRouteDirectionCumulativeInfo`. It does not localize the Android
 * `route_head` description or retain a native `RouteDataObject` on an inserted direction.
 */
object RouteManeuverCalculator {

	private const val MAX_INTERMEDIATE_DISTANCE_METERS = 3000.0
	private const val CLOSE_INTERMEDIATE_DISTANCE_METERS = 25.0
	private const val MANEUVER_SPLIT_DISTANCE_METERS = 50.0

	/** Immutable port of Android `updateDirectionsTime`. */
	fun updateDistancesAndTimes(
		maneuvers: List<RouteManeuver>,
		geometry: RouteGeometryCalculation,
	): List<RouteManeuver> = updateDistancesAndTimes(
		maneuvers,
		geometry.distanceToFinishMeters,
	)

	/** Platform overload for an already calculated Android `listDistance` array. */
	fun updateDistancesAndTimes(
		maneuvers: List<RouteManeuver>,
		distanceToFinishMeters: IntArray,
	): List<RouteManeuver> {
		require(maneuvers.all { it.routePointOffset in distanceToFinishMeters.indices }) {
			"Maneuver route point offset must reference Android listDistance"
		}
		val updated = maneuvers.toMutableList()
		var sumExpectedTime = 0
		for (index in maneuvers.lastIndex downTo 0) {
			val maneuver = maneuvers[index]
			var distance = distanceToFinishMeters[maneuver.routePointOffset]
			if (index < maneuvers.lastIndex) {
				distance -= distanceToFinishMeters[maneuvers[index + 1].routePointOffset]
			}
			val expectedTime = javaRoundFloat(distance / maneuver.averageSpeedMetersPerSecond)
			sumExpectedTime += expectedTime
			updated[index] = maneuver.copy(
				distanceMeters = distance,
				expectedTimeSeconds = expectedTime,
				afterLeftTimeSeconds = sumExpectedTime,
			)
		}
		return updated
	}

	/** Immutable port of Android Route Details cumulative row totals. */
	fun cumulativeInfoBefore(position: Int, maneuvers: List<RouteManeuver>): RouteCumulativeInfo {
		if (position >= maneuvers.size) {
			return RouteCumulativeInfo(0, 0)
		}
		var distance = 0
		var time = 0
		for (index in 0 until position) {
			distance += maneuvers[index].distanceMeters
			time += maneuvers[index].expectedTimeSeconds
		}
		return RouteCumulativeInfo(distance, time)
	}

	/**
	 * Calculates every legacy position result in one pass.
	 *
	 * The final entry deliberately remains zero because [cumulativeInfoBefore] returns zero when
	 * `position == maneuvers.size`; iOS currently relies on that boundary behavior.
	 */
	fun cumulativeInfoByPosition(maneuvers: List<RouteManeuver>): List<RouteCumulativeInfo> =
		cumulativeInfoByPosition(ManeuverMetricsAccessor(maneuvers))

	fun cumulativeInfoByPosition(accessor: IManeuverMetricsAccessor): List<RouteCumulativeInfo> {
		val maneuverCount = accessor.getManeuversCount()
		val result = ArrayList<RouteCumulativeInfo>(maneuverCount + 1)
		var distance = 0
		var time = 0
		for (index in 0 until maneuverCount) {
			result.add(RouteCumulativeInfo(distance, time))
			distance += accessor.getDistanceMeters(index)
			time += accessor.getExpectedTimeSeconds(index)
		}
		result.add(RouteCumulativeInfo(0, 0))
		return result
	}

	/**
	 * Immutable port of Android `calculateIntermediateIndexes` indexing and split logic.
	 *
	 * The returned index array intentionally starts with Android's zero-filled sentinel values. If
	 * any intermediate is farther than 3000 metres, Android returns before splitting directions and
	 * leaves every index at zero; this method does the same.
	 */
	fun calculateIntermediateIndexes(
		locations: List<KLatLon>,
		maneuvers: List<RouteManeuver>,
		intermediates: List<KLatLon>,
	): RouteIntermediateCalculation {
		return calculateIntermediateIndexesFromAccessors(
			KLatLonAccessor(locations),
			maneuvers,
			KLatLonAccessor(intermediates),
		)
	}

	fun calculateIntermediateIndexesFromAccessors(
		routeLocations: ILocationAccessor,
		maneuvers: List<RouteManeuver>,
		intermediateLocations: ILocationAccessor,
	): RouteIntermediateCalculation {
		val routeLocationCount = routeLocations.getLocationsCount()
		val intermediateLocationCount = intermediateLocations.getLocationsCount()
		val intermediateDirectionIndices = IntArray(intermediateLocationCount)
		val matchedRouteLocationIndices = IntArray(intermediateLocationCount)
		for (currentIntermediate in 0 until intermediateLocationCount) {
			var closestDistance = MAX_INTERMEDIATE_DISTANCE_METERS
			val intermediateLatitude = intermediateLocations.getLatitude(currentIntermediate)
			val intermediateLongitude = intermediateLocations.getLongitude(currentIntermediate)
			val previousLocation = if (currentIntermediate == 0) {
				0
			} else {
				matchedRouteLocationIndices[currentIntermediate - 1]
			}
			for (currentLocation in previousLocation until routeLocationCount) {
				val currentDistance = KMapUtils.getDistance(
					intermediateLatitude,
					intermediateLongitude,
					routeLocations.getLatitude(currentLocation),
					routeLocations.getLongitude(currentLocation),
				)
				if (currentDistance < closestDistance) {
					matchedRouteLocationIndices[currentIntermediate] = currentLocation
					closestDistance = currentDistance
				} else if (currentDistance > CLOSE_INTERMEDIATE_DISTANCE_METERS &&
					closestDistance < CLOSE_INTERMEDIATE_DISTANCE_METERS) {
					break
				}
			}
			if (closestDistance == MAX_INTERMEDIATE_DISTANCE_METERS) {
				return RouteIntermediateCalculation(
					maneuvers = maneuvers.toList(),
					intermediateDirectionIndices = intermediateDirectionIndices,
				)
			}
		}

		val updatedManeuvers = maneuvers.toMutableList()
		var currentDirection = 0
		var currentIntermediate = 0
		while (currentIntermediate < intermediateLocationCount && currentDirection < updatedManeuvers.size) {
			val locationIndex = updatedManeuvers[currentDirection].routePointOffset
			if (locationIndex >= matchedRouteLocationIndices[currentIntermediate]) {
				if (locationIndex > matchedRouteLocationIndices[currentIntermediate] &&
					KMapUtils.getDistance(
						intermediateLocations.getLatitude(currentIntermediate),
						intermediateLocations.getLongitude(currentIntermediate),
						routeLocations.getLatitude(locationIndex),
						routeLocations.getLongitude(locationIndex),
					) > MANEUVER_SPLIT_DISTANCE_METERS) {
					val toSplit = updatedManeuvers[currentDirection]
					val currentAverageSpeed = updatedManeuvers[max(0, currentDirection - 1)]
						.averageSpeedMetersPerSecond
					updatedManeuvers.add(
						currentDirection,
						RouteManeuver(
							turnTypeValue = RouteManeuverType.STRAIGHT.legacyValue,
							routePointOffset = matchedRouteLocationIndices[currentIntermediate],
							routeEndPointOffset = 0,
							distanceMeters = 0,
							expectedTimeSeconds = 0,
							afterLeftTimeSeconds = 0,
							averageSpeedMetersPerSecond = currentAverageSpeed,
							turnAngleDegrees = 0f,
							exitNumber = 0,
							streetName = toSplit.streetName,
							ref = toSplit.ref,
							destinationName = toSplit.destinationName,
						),
					)
				}
				intermediateDirectionIndices[currentIntermediate] = currentDirection
				currentIntermediate++
			}
			currentDirection++
		}
		return RouteIntermediateCalculation(
			maneuvers = updatedManeuvers,
			intermediateDirectionIndices = intermediateDirectionIndices,
		)
	}
}
