package net.osmand.shared.routing.details

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import kotlin.math.abs
import kotlin.math.floor

/** Android-compatible `RouteCalculationResult.listDistance` values. */
data class RouteGeometryCalculation(
	val distanceToFinishMeters: IntArray,
)

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
		return calculate(KLatLonAccessor(locations))
	}

	fun calculate(accessor: ILocationAccessor): RouteGeometryCalculation {
		val locationCount = accessor.getLocationsCount()
		val distanceToFinish = IntArray(locationCount)
		calculateInto(accessor, distanceToFinish)
		return RouteGeometryCalculation(distanceToFinish)
	}

	/** Fills an existing Android `listDistance` array without allocating a second route-sized array. */
	fun calculateInto(
		accessor: ILocationAccessor,
		distanceToFinishMeters: IntArray,
	) {
		val locationCount = accessor.getLocationsCount()
		require(distanceToFinishMeters.size == locationCount) {
			"Route locations and Android listDistance values must have the same size"
		}
		if (locationCount > 0) {
			distanceToFinishMeters[locationCount - 1] = 0
		}
		if (locationCount < 2) {
			return
		}
		var endLatitude = accessor.getLatitude(locationCount - 1)
		var endLongitude = accessor.getLongitude(locationCount - 1)
		for (index in locationCount - 1 downTo 1) {
			val startLatitude = accessor.getLatitude(index - 1)
			val startLongitude = accessor.getLongitude(index - 1)
			distanceToFinishMeters[index - 1] = javaRoundFloat(
				distanceBetween(
					startLatitude,
					startLongitude,
					endLatitude,
					endLongitude,
				),
			)
			distanceToFinishMeters[index - 1] += distanceToFinishMeters[index]
			endLatitude = startLatitude
			endLongitude = startLongitude
		}
	}

	fun withCalculatedDistances(
		points: List<RoutePoint>,
		calculation: RouteGeometryCalculation = calculate(RoutePointAccessor(points)),
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
		return distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude)
	}

	private fun distanceBetween(
		startLatitude: Double,
		startLongitude: Double,
		endLatitude: Double,
		endLongitude: Double,
	): Float {
		return KMapUtils.getEllipsoidDistance(
			startLatitude,
			startLongitude,
			endLatitude,
			endLongitude,
		).toFloat()
	}

	/** Exact port of Android `RouteCalculationResult.getRouteLocationByDistance(int)`. */
	fun locationByDistance(
		locations: List<KLatLon>,
		currentRoutePointIndex: Int,
		distanceMeters: Int,
	): KLatLon? {
		val index = locationIndexByDistance(
			KLatLonAccessor(locations),
			currentRoutePointIndex,
			distanceMeters,
		)
		if (index < 0) {
			return null
		}
		return locations[index]
	}

	/**
	 * Platform boundary without per-route-point or nullable-index allocation for
	 * `getRouteLocationByDistance(int)`. Returns `-1` when Android would return `null`.
	 */
	fun locationIndexByDistance(
		accessor: ILocationAccessor,
		currentRoutePointIndex: Int,
		distanceMeters: Int,
	): Int {
		val locationCount = accessor.getLocationsCount()
		if (currentRoutePointIndex !in 0 until locationCount) {
			return -1
		}
		val increment = if (distanceMeters > 0) 1 else -1
		val currentLatitude = accessor.getLatitude(currentRoutePointIndex)
		val currentLongitude = accessor.getLongitude(currentRoutePointIndex)
		var offset = increment
		while (currentRoutePointIndex + offset in 0 until locationCount) {
			val index = currentRoutePointIndex + offset
			val distance = KMapUtils.getDistance(
				currentLatitude,
				currentLongitude,
				accessor.getLatitude(index),
				accessor.getLongitude(index),
			)
			if (distance >= abs(distanceMeters).toDouble()) {
				return index
			}
			offset += increment
		}
		return -1
	}
}

/** Common equivalent of Java `Math.round(float)`. */
internal fun javaRoundFloat(value: Float): Int {
	return floor((value + 0.5f).toDouble()).toInt()
}
