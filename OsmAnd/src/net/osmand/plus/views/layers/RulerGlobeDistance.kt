package net.osmand.plus.views.layers

import kotlin.math.abs
import kotlin.math.min

/**
 * Finds the ground distance that the globe projection draws with a given screen radius.
 * The first guess comes from the Web Mercator density of the tile box, which near the poles
 * or with the map centre moved off the screen middle can be hundreds of times too small.
 * Such a guess projects to less than a pixel, so it is enlarged until it can be measured.
 */
internal class RulerGlobeDistance(
	private val maxDistance: Double,
	private val maxProjectedDistance: Double
) {

	fun find(pixelRadius: Double, initialDistance: Double, projectRadius: (Double) -> Double): Double {
		var distance = initialDistance
		var refinements = 0
		for (attempt in 0 until MAX_ATTEMPTS) {
			if (!isValid(distance)) {
				return Double.NaN
			}
			val projectedRadius = projectRadius(distance)
			if (!projectedRadius.isFinite()) {
				return Double.NaN
			}
			if (projectedRadius < MIN_MEASURABLE_RADIUS) {
				if (distance >= maxProjectedDistance) {
					return Double.NaN
				}
				distance = min(distance * GROWTH_FACTOR, maxProjectedDistance)
				continue
			}
			val correctedDistance = distance * pixelRadius / projectedRadius
			if (!isValid(correctedDistance)) {
				return Double.NaN
			}
			val converged = abs(correctedDistance - distance) <= distance * CONVERGENCE_TOLERANCE
			distance = correctedDistance
			if (converged || ++refinements >= MAX_REFINEMENTS) {
				return distance
			}
		}
		return Double.NaN
	}

	private fun isValid(distance: Double): Boolean =
		distance.isFinite() && distance > 0 && distance <= maxDistance

	private companion object {
		const val MIN_MEASURABLE_RADIUS = 8.0
		const val GROWTH_FACTOR = 10.0
		const val CONVERGENCE_TOLERANCE = 0.01
		const val MAX_REFINEMENTS = 4
		const val MAX_ATTEMPTS = 16
	}
}