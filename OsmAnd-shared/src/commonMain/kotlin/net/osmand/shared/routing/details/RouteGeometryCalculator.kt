package net.osmand.shared.routing.details

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import kotlin.math.abs
import kotlin.math.floor

/** Android-compatible `RouteCalculationResult.listDistance` values. */
data class RouteGeometryCalculation(
	val distanceToFinishMeters: List<Int>,
) {
	init {
		require(distanceToFinishMeters.all { it >= 0 }) {
			"Android route distances to finish must not be negative"
		}
		require(distanceToFinishMeters.zipWithNext().all { (first, second) -> first >= second }) {
			"Android route distances to finish must be ordered"
		}
		require(distanceToFinishMeters.isEmpty() || distanceToFinishMeters.last() == 0) {
			"The final Android route distance must be zero"
		}
	}

}

/**
 * Shared home for ports of Android route-geometry methods; Android has no class named
 * `RouteGeometryCalculator`.
 *
 * Cumulative distances use the WGS84 inverse formula from `Location.computeDistanceAndBearing`,
 * followed by Android's per-edge `Math.round(float)` in
 * `RouteCalculationResult.updateListDistanceTime`. Direct location lookup intentionally continues
 * to use `MapUtils.getDistance`, matching `RouteCalculationResult.getRouteLocationByDistance`.
 */
object RouteGeometryCalculator {

	fun calculate(locations: List<KLatLon>): RouteGeometryCalculation {
		if (locations.isEmpty()) {
			return RouteGeometryCalculation(emptyList())
		}
		val distanceToFinish = MutableList(locations.size) { 0 }
		for (index in locations.lastIndex downTo 1) {
			distanceToFinish[index - 1] = javaRoundFloat(distanceBetween(locations[index - 1], locations[index]))
			distanceToFinish[index - 1] += distanceToFinish[index]
		}
		return RouteGeometryCalculation(distanceToFinish.toList())
	}

	fun withCalculatedDistances(
		points: List<RoutePoint>,
		calculation: RouteGeometryCalculation = calculate(points.map(RoutePoint::location)),
	): List<RoutePoint> {
		require(points.size == calculation.distanceToFinishMeters.size) {
			"Route points and Android listDistance values must have the same size"
		}
		return points.mapIndexed { index, point ->
			point.copy(distanceToFinishMeters = calculation.distanceToFinishMeters[index])
		}
	}

	/** Android `Location.distanceTo` value, using the equivalent shared WGS84 implementation. */
	fun distanceBetween(start: KLatLon, end: KLatLon): Float {
		return KMapUtils.getEllipsoidDistance(
			start.latitude,
			start.longitude,
			end.latitude,
			end.longitude,
		).toFloat()
	}

	/** Exact port of Android `RouteCalculationResult.getRouteLocationByDistance(int)`. */
	fun locationByDistance(
		locations: List<KLatLon>,
		currentRoutePointIndex: Int,
		distanceMeters: Int,
	): KLatLon? {
		if (currentRoutePointIndex !in locations.indices) {
			return null
		}
		val increment = if (distanceMeters > 0) 1 else -1
		var offset = increment
		while (currentRoutePointIndex + offset in locations.indices) {
			val location = locations[currentRoutePointIndex + offset]
			val distance = KMapUtils.getDistance(locations[currentRoutePointIndex], location)
			if (distance >= abs(distanceMeters).toDouble()) {
				return location
			}
			offset += increment
		}
		return null
	}
}

/** Common equivalent of Java `Math.round(float)`. */
internal fun javaRoundFloat(value: Float): Int {
	return floor((value + 0.5f).toDouble()).toInt()
}
