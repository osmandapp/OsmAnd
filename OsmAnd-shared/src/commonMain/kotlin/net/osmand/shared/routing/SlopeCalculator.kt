package net.osmand.shared.routing

import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory

/**
 * Utility object responsible for calculating track slopes based on elevation and coordinates.
 */
object SlopeCalculator {
	private val LOG = LoggerFactory.getLogger("SlopeCalculator")

	// Default configuration values
	private var MAX_CORRECT_ELEVATION_DISTANCE = 100.0 // in meters
	private var SLOPE_RANGE = 150.0 // 150 meters

	/**
	 * Calculates slopes from elevations.
	 *
	 * @param latitudes Array of latitudes
	 * @param longitudes Array of longitudes
	 * @param elevations Array of elevations (will be smoothed during calculation)
	 * @param slopeRange The range over which to calculate the derivative (e.g., 150 meters)
	 * @return Array of slopes. The beginning and end of the array may contain NaN values.
	 */
	fun calculateSlopesByElevations(
		latitudes: DoubleArray,
		longitudes: DoubleArray,
		elevations: DoubleArray,
		slopeRange: Double = SLOPE_RANGE
	): DoubleArray {
		var currentElevations = elevations

		// 1. Correct missing/NaN elevations
		correctElevations(latitudes, longitudes, currentElevations)

		// 2. Smooth elevations (Moving average over 5 points)
		val newElevations = currentElevations
		for (i in 2 until currentElevations.size - 2) {
			newElevations[i] = (currentElevations[i - 2] + currentElevations[i - 1] +
					currentElevations[i] + currentElevations[i + 1] +
					currentElevations[i + 2]) / 5.0
		}
		currentElevations = newElevations

		val slopes = DoubleArray(currentElevations.size)
		if (latitudes.size != longitudes.size || latitudes.size != currentElevations.size) {
			LOG.warn("Sizes of arrays latitudes, longitudes and values are not match")
			return slopes
		}

		// 3. Calculate cumulative distances
		val distances = DoubleArray(currentElevations.size)
		var totalDistance = 0.0
		distances[0] = totalDistance
		for (i in 0 until currentElevations.size - 1) {
			totalDistance += KMapUtils.getDistance(
				latitudes[i], longitudes[i],
				latitudes[i + 1], longitudes[i + 1]
			)
			distances[i + 1] = totalDistance
		}

		// 4. Calculate derivative (slope) for each point
		for (i in currentElevations.indices) {
			if (distances[i] < slopeRange / 2 || distances[i] > totalDistance - slopeRange / 2) {
				slopes[i] = Double.NaN
			} else {
				val arg = findDerivativeArguments(distances, currentElevations, i, slopeRange)
				slopes[i] = (arg[1] - arg[0]) / (arg[3] - arg[2])
			}
		}

		return slopes
	}

	private fun correctElevations(
		latitudes: DoubleArray,
		longitudes: DoubleArray,
		elevations: DoubleArray
	) {
		for (i in elevations.indices) {
			if (elevations[i].isNaN()) {
				var leftDist = MAX_CORRECT_ELEVATION_DISTANCE
				var rightDist = MAX_CORRECT_ELEVATION_DISTANCE
				var leftElevation = Double.NaN
				var rightElevation = Double.NaN

				// Find nearest valid left elevation
				var left = i - 1
				while (left > 0 && leftDist <= MAX_CORRECT_ELEVATION_DISTANCE) {
					if (!elevations[left].isNaN()) {
						val dist = KMapUtils.getDistance(
							latitudes[left], longitudes[left],
							latitudes[i], longitudes[i]
						)
						if (dist < leftDist) {
							leftDist = dist
							leftElevation = elevations[left]
						} else {
							break
						}
					}
					left--
				}

				// Find nearest valid right elevation
				var right = i + 1
				while (right < elevations.size && rightDist <= MAX_CORRECT_ELEVATION_DISTANCE) {
					if (!elevations[right].isNaN()) {
						val dist = KMapUtils.getDistance(
							latitudes[right], longitudes[right],
							latitudes[i], longitudes[i]
						)
						if (dist < rightDist) {
							rightElevation = elevations[right]
							rightDist = dist
						} else {
							break
						}
					}
					right++
				}

				// Interpolate or fallback
				if (!leftElevation.isNaN() && !rightElevation.isNaN()) {
					elevations[i] = (leftElevation + rightElevation) / 2
				} else if (leftElevation.isNaN() && !rightElevation.isNaN()) {
					elevations[i] = rightElevation
				} else if (!leftElevation.isNaN() && rightElevation.isNaN()) {
					elevations[i] = leftElevation
				} else {
					for (r in i + 1 until elevations.size) {
						if (!elevations[r].isNaN()) {
							elevations[i] = elevations[r]
							break
						}
					}
				}
			}
		}
	}

	/**
	 * Finds arguments for derivative calculation.
	 * @return DoubleArray: [minElevation, maxElevation, minDist, maxDist]
	 */
	private fun findDerivativeArguments(
		distances: DoubleArray,
		elevations: DoubleArray,
		index: Int,
		slopeRange: Double
	): DoubleArray {
		val result = DoubleArray(4)
		val minDist = distances[index] - slopeRange / 2
		val maxDist = distances[index] + slopeRange / 2
		result[0] = Double.NaN
		result[1] = Double.NaN
		result[2] = minDist
		result[3] = maxDist

		var closestMaxIndex = -1
		var closestMinIndex = -1

		for (i in index until distances.size) {
			if (distances[i] == maxDist) {
				result[1] = elevations[i]
				break
			}
			if (distances[i] > maxDist) {
				closestMaxIndex = i
				break
			}
		}

		for (i in index downTo 0) {
			if (distances[i] == minDist) {
				result[0] = elevations[i]
				break
			}
			if (distances[i] < minDist) {
				closestMinIndex = i
				break
			}
		}

		if (closestMaxIndex > 0) {
			val diff = distances[closestMaxIndex] - distances[closestMaxIndex - 1]
			val coef = (maxDist - distances[closestMaxIndex - 1]) / diff
			if (coef > 1 || coef < 0) {
				LOG.warn("Coefficient for max must be 0..1, coef=$coef")
			}
			result[1] = (1 - coef) * elevations[closestMaxIndex - 1] + coef * elevations[closestMaxIndex]
		}

		if (closestMinIndex >= 0) {
			val diff = distances[closestMinIndex + 1] - distances[closestMinIndex]
			val coef = (minDist - distances[closestMinIndex]) / diff
			if (coef > 1 || coef < 0) {
				LOG.warn("Coefficient for min must be 0..1, coef=$coef")
			}
			result[0] = (1 - coef) * elevations[closestMinIndex] + coef * elevations[closestMinIndex + 1]
		}

		if (result[0].isNaN() || result[1].isNaN()) {
			LOG.warn("Elevations weren't calculated properly")
		}

		return result
	}
}