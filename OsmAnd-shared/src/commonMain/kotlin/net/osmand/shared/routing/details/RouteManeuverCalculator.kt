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
	val intermediateDirectionIndices: List<Int>,
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
	): List<RouteManeuver> {
		require(maneuvers.all { it.routePointOffset in geometry.distanceToFinishMeters.indices }) {
			"Maneuver route point offset must reference Android listDistance"
		}
		val updated = maneuvers.toMutableList()
		var sumExpectedTime = 0
		for (index in maneuvers.lastIndex downTo 0) {
			val maneuver = maneuvers[index]
			var distance = geometry.distanceToFinishMeters[maneuver.routePointOffset]
			if (index < maneuvers.lastIndex) {
				distance -= geometry.distanceToFinishMeters[maneuvers[index + 1].routePointOffset]
			}
			val expectedTime = javaRoundFloat(distance / maneuver.averageSpeedMetersPerSecond)
			sumExpectedTime += expectedTime
			updated[index] = maneuver.copy(
				distanceMeters = distance,
				expectedTimeSeconds = expectedTime,
				afterLeftTimeSeconds = sumExpectedTime,
			)
		}
		return updated.toList()
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
	 * Immutable port of Android `calculateIntermediateIndexes` indexing and split logic.
	 *
	 * The returned index list intentionally starts with Android's zero-filled sentinel values. If
	 * any intermediate is farther than 3000 metres, Android returns before splitting directions and
	 * leaves every index at zero; this method does the same.
	 */
	fun calculateIntermediateIndexes(
		locations: List<KLatLon>,
		maneuvers: List<RouteManeuver>,
		intermediates: List<KLatLon>,
	): RouteIntermediateCalculation {
		val intermediateDirectionIndices = MutableList(intermediates.size) { 0 }
		val intermediateLocations = MutableList(intermediates.size) { 0 }
		for (currentIntermediate in intermediates.indices) {
			var closestDistance = MAX_INTERMEDIATE_DISTANCE_METERS
			val intermediate = intermediates[currentIntermediate]
			val previousLocation = if (currentIntermediate == 0) {
				0
			} else {
				intermediateLocations[currentIntermediate - 1]
			}
			for (currentLocation in previousLocation until locations.size) {
				val currentDistance = KMapUtils.getDistance(intermediate, locations[currentLocation])
				if (currentDistance < closestDistance) {
					intermediateLocations[currentIntermediate] = currentLocation
					closestDistance = currentDistance
				} else if (currentDistance > CLOSE_INTERMEDIATE_DISTANCE_METERS &&
					closestDistance < CLOSE_INTERMEDIATE_DISTANCE_METERS) {
					break
				}
			}
			if (closestDistance == MAX_INTERMEDIATE_DISTANCE_METERS) {
				return RouteIntermediateCalculation(
					maneuvers = maneuvers.toList(),
					intermediateDirectionIndices = intermediateDirectionIndices.toList(),
				)
			}
		}

		val updatedManeuvers = maneuvers.toMutableList()
		var currentDirection = 0
		var currentIntermediate = 0
		while (currentIntermediate < intermediates.size && currentDirection < updatedManeuvers.size) {
			val locationIndex = updatedManeuvers[currentDirection].routePointOffset
			if (locationIndex >= intermediateLocations[currentIntermediate]) {
				if (locationIndex > intermediateLocations[currentIntermediate] &&
					KMapUtils.getDistance(
						intermediates[currentIntermediate],
						locations[locationIndex],
					) > MANEUVER_SPLIT_DISTANCE_METERS) {
					val toSplit = updatedManeuvers[currentDirection]
					val currentAverageSpeed = updatedManeuvers[max(0, currentDirection - 1)]
						.averageSpeedMetersPerSecond
					updatedManeuvers.add(
						currentDirection,
						RouteManeuver(
							turnTypeValue = RouteManeuverType.STRAIGHT.legacyValue,
							routePointOffset = intermediateLocations[currentIntermediate],
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
			maneuvers = updatedManeuvers.toList(),
			intermediateDirectionIndices = intermediateDirectionIndices.toList(),
		)
	}
}
