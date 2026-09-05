package net.osmand.shared.routing.details

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils

/** Exact integer totals used by Android Route Details before a direction row. */
data class RouteCumulativeInfo(
	val distanceMeters: Int,
	val timeSeconds: Int,
)

/** Primitive direction values calculated without constructing complete maneuver snapshots. */
data class RouteManeuverUpdate(
	val distanceMeters: IntArray,
	val expectedTimeSeconds: IntArray,
	val afterLeftTimeSeconds: IntArray,
)

/** One straight direction that a platform must insert before [directionIndex]. */
data class RouteIntermediateInsertion(
	val directionIndex: Int,
	val routePointOffset: Int,
	val averageSpeedMetersPerSecond: Float,
)

/** Result of Android-compatible intermediate-point association and direction splitting. */
data class RouteIntermediateCalculation(
	val insertions: List<RouteIntermediateInsertion>,
	val intermediateDirectionIndices: IntArray,
)

/**
 * Shared home for ports of Android direction calculations used by Route Details; Android has no
 * class named `RouteManeuverCalculator`.
 *
 * The arithmetic and indexing follow `RouteCalculationResult.updateDirectionsTime`,
 * `RouteCalculationResult.calculateIntermediateIndexes`, and
 * `RouteDetailsFragment.getRouteDirectionCumulativeInfo`. Intermediate insertions contain only
 * calculation results; each platform supplies its localized `route_head` description and retains
 * its native route object from the direction being split.
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
		val update = calculateDistanceAndTimeUpdates(
			ManeuverAccessor(maneuvers),
			distanceToFinishMeters,
		)
		return maneuvers.mapIndexed { index, maneuver ->
			maneuver.copy(
				distanceMeters = update.distanceMeters[index],
				expectedTimeSeconds = update.expectedTimeSeconds[index],
				afterLeftTimeSeconds = update.afterLeftTimeSeconds[index],
			)
		}
	}

	/** Calculates only the primitive values written back to platform direction objects. */
	fun calculateDistanceAndTimeUpdates(
		accessor: IManeuverAccessor,
		distanceToFinishMeters: IntArray,
	): RouteManeuverUpdate {
		val maneuverCount = accessor.getManeuversCount()
		require((0 until maneuverCount).all {
			accessor.getRoutePointOffset(it) in distanceToFinishMeters.indices
		}) {
			"Maneuver route point offset must reference Android listDistance"
		}
		val distances = IntArray(maneuverCount)
		val expectedTimes = IntArray(maneuverCount)
		val afterLeftTimes = IntArray(maneuverCount)
		var sumExpectedTime = 0
		for (index in maneuverCount - 1 downTo 0) {
			var distance = distanceToFinishMeters[accessor.getRoutePointOffset(index)]
			if (index < maneuverCount - 1) {
				distance -= distanceToFinishMeters[accessor.getRoutePointOffset(index + 1)]
			}
			val expectedTime = javaRoundFloat(
				distance / accessor.getAverageSpeedMetersPerSecond(index),
			)
			sumExpectedTime += expectedTime
			distances[index] = distance
			expectedTimes[index] = expectedTime
			afterLeftTimes[index] = sumExpectedTime
		}
		return RouteManeuverUpdate(
			distanceMeters = distances,
			expectedTimeSeconds = expectedTimes,
			afterLeftTimeSeconds = afterLeftTimes,
		)
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
			ManeuverAccessor(maneuvers),
			KLatLonAccessor(intermediates),
		)
	}

	fun calculateIntermediateIndexesFromAccessors(
		routeLocations: ILocationAccessor,
		maneuvers: IManeuverAccessor,
		intermediateLocations: ILocationAccessor,
	): RouteIntermediateCalculation {
		val routeLocationCount = routeLocations.getLocationsCount()
		val intermediateLocationCount = intermediateLocations.getLocationsCount()
		val intermediateDirectionIndices = IntArray(intermediateLocationCount)
		if (intermediateLocationCount == 0 || maneuvers.getManeuversCount() == 0) {
			return RouteIntermediateCalculation(
				insertions = emptyList(),
				intermediateDirectionIndices = intermediateDirectionIndices,
			)
		}
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
					insertions = emptyList(),
					intermediateDirectionIndices = intermediateDirectionIndices,
				)
			}
		}

		val insertions = mutableListOf<RouteIntermediateInsertion>()
		val maneuverCount = maneuvers.getManeuversCount()
		var currentManeuver = 0
		var currentDirection = 0
		var currentIntermediate = 0
		var previousAverageSpeed = 0f
		while (currentIntermediate < intermediateLocationCount && currentManeuver < maneuverCount) {
			val locationIndex = maneuvers.getRoutePointOffset(currentManeuver)
			if (locationIndex >= matchedRouteLocationIndices[currentIntermediate]) {
				if (locationIndex > matchedRouteLocationIndices[currentIntermediate] &&
					KMapUtils.getDistance(
						intermediateLocations.getLatitude(currentIntermediate),
						intermediateLocations.getLongitude(currentIntermediate),
						routeLocations.getLatitude(locationIndex),
						routeLocations.getLongitude(locationIndex),
					) > MANEUVER_SPLIT_DISTANCE_METERS) {
					val currentAverageSpeed = if (currentDirection == 0) {
						maneuvers.getAverageSpeedMetersPerSecond(currentManeuver)
					} else {
						previousAverageSpeed
					}
					insertions.add(
						RouteIntermediateInsertion(
							directionIndex = currentDirection,
							routePointOffset = matchedRouteLocationIndices[currentIntermediate],
							averageSpeedMetersPerSecond = currentAverageSpeed,
						),
					)
					previousAverageSpeed = currentAverageSpeed
					intermediateDirectionIndices[currentIntermediate] = currentDirection
					currentIntermediate++
					currentDirection++
					continue
				}
				intermediateDirectionIndices[currentIntermediate] = currentDirection
				currentIntermediate++
			}
			previousAverageSpeed = maneuvers.getAverageSpeedMetersPerSecond(currentManeuver)
			currentManeuver++
			currentDirection++
		}
		return RouteIntermediateCalculation(
			insertions = insertions,
			intermediateDirectionIndices = intermediateDirectionIndices,
		)
	}
}
