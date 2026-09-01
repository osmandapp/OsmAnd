package net.osmand.plus.routing

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.osmand.Location
import net.osmand.router.TurnType
import net.osmand.shared.routing.details.RouteCumulativeInfo
import net.osmand.util.MapUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** Differential checks against the Android implementations replaced by Commit 6. */
@RunWith(AndroidJUnit4::class)
class SharedRouteDetailsProviderCompatibilityTest {

	@Test
	fun cumulativeGeometryMatchesFrozenAndroidCalculation() {
		val locations = listOf(
			location(0.0, 0.0),
			location(0.0, 0.001),
			location(0.001, 0.001),
			location(0.001, 0.002),
		)

		assertArrayEquals(
			legacyDistancesToFinish(locations),
			SharedRouteDetailsProvider.calculateDistancesToFinish(locations),
		)
		assertArrayEquals(
			intArrayOf(),
			SharedRouteDetailsProvider.calculateDistancesToFinish(emptyList()),
		)
		assertArrayEquals(
			intArrayOf(0),
			SharedRouteDetailsProvider.calculateDistancesToFinish(listOf(locations.first())),
		)
	}

	@Test
	fun maneuverDistancesTimesAndCumulativeTotalsMatchFrozenAndroidCalculation() {
		val distanceToFinish = intArrayOf(450, 300, 125, 0)
		val directions = listOf(
			direction(offset = 0, averageSpeed = 12f),
			direction(offset = 2, averageSpeed = 7f),
			direction(offset = 3, averageSpeed = 1f),
		)
		val expected = legacyDirectionValues(directions, distanceToFinish)

		SharedRouteDetailsProvider.updateDirectionDistancesAndTimes(directions, distanceToFinish)

		assertEquals(expected.map(LegacyDirectionValue::distance), directions.map { it.distance })
		assertEquals(expected.map(LegacyDirectionValue::afterLeftTime), directions.map { it.afterLeftTime })
		val cumulativeInfoByPosition = SharedRouteDetailsProvider.getCumulativeInfoByPosition(directions)
		for (position in 0..directions.size) {
			assertEquals(
				legacyCumulativeInfo(position, directions),
				cumulativeInfoByPosition[position],
			)
		}
		assertEquals(
			listOf(
				RouteCumulativeInfo(0, 0),
				RouteCumulativeInfo(150, 13),
				RouteCumulativeInfo(325, 27),
				RouteCumulativeInfo(450, 45),
			),
			SharedRouteDetailsProvider.getCumulativeInfoAtRoutePoints(
				directions,
				distanceToFinish,
				currentRoutePointIndex = 0,
				currentDirectionIndex = 0,
				routePointOffsets = intArrayOf(0, 1, 2, 3),
			),
		)
	}

	@Test
	fun cumulativeTotalsAcceptAndroidNanAverageSpeed() {
		val directions = listOf(
			direction(offset = 0, averageSpeed = Float.NaN).apply {
				distance = 0
			},
		)

		assertEquals(
			legacyCumulativeInfo(1, directions),
			SharedRouteDetailsProvider.getCumulativeInfoByPosition(directions)[1],
		)
	}

	@Test
	fun signedRouteLocationLookupReturnsTheSameAndroidLocationInstance() {
		val locations = (0..4).map { index -> location(0.0, index * 0.001) }
		val currentRoutePointIndex = 2

		for (distance in listOf(-1_000, -100, 0, 100, 1_000)) {
			val expected = legacyRouteLocationByDistance(locations, currentRoutePointIndex, distance)
			val actual = SharedRouteDetailsProvider.getRouteLocationByDistance(
				locations,
				currentRoutePointIndex,
				distance,
			)
			if (expected == null) {
				assertNull(actual)
			} else {
				assertSame(expected, actual)
			}
		}
	}

	private fun legacyDistancesToFinish(locations: List<Location>): IntArray {
		val result = IntArray(locations.size)
		if (result.isNotEmpty()) {
			result[locations.lastIndex] = 0
			for (index in locations.lastIndex downTo 1) {
				result[index - 1] = Math.round(locations[index - 1].distanceTo(locations[index]))
				result[index - 1] += result[index]
			}
		}
		return result
	}

	private fun legacyDirectionValues(
		directions: List<RouteDirectionInfo>,
		distanceToFinish: IntArray,
	): List<LegacyDirectionValue> {
		val result = MutableList(directions.size) { LegacyDirectionValue(0, 0) }
		var sumExpectedTime = 0
		for (index in directions.lastIndex downTo 0) {
			val direction = directions[index]
			var distance = distanceToFinish[direction.routePointOffset]
			if (index < directions.lastIndex) {
				distance -= distanceToFinish[directions[index + 1].routePointOffset]
			}
			sumExpectedTime += Math.round(distance / direction.averageSpeed)
			result[index] = LegacyDirectionValue(distance, sumExpectedTime)
		}
		return result
	}

	private fun legacyCumulativeInfo(
		position: Int,
		directions: List<RouteDirectionInfo>,
	): RouteCumulativeInfo {
		if (position >= directions.size) {
			return RouteCumulativeInfo(0, 0)
		}
		var distance = 0
		var time = 0
		for (index in 0 until position) {
			distance += directions[index].distance
			time += directions[index].expectedTime
		}
		return RouteCumulativeInfo(distance, time)
	}

	private fun legacyRouteLocationByDistance(
		locations: List<Location>,
		currentRoutePointIndex: Int,
		distanceMeters: Int,
	): Location? {
		val increment = if (distanceMeters > 0) 1 else -1
		var offset = increment
		while (currentRoutePointIndex < locations.size &&
			currentRoutePointIndex + offset >= 0 &&
			currentRoutePointIndex + offset < locations.size
		) {
			val location = locations[currentRoutePointIndex + offset]
			val distance = MapUtils.getDistance(locations[currentRoutePointIndex], location)
			if (distance >= abs(distanceMeters)) {
				return location
			}
			offset += increment
		}
		return null
	}

	private fun location(latitude: Double, longitude: Double): Location = Location("test").apply {
		this.latitude = latitude
		this.longitude = longitude
	}

	private fun direction(offset: Int, averageSpeed: Float): RouteDirectionInfo =
		RouteDirectionInfo(averageSpeed, TurnType.straight()).apply {
			routePointOffset = offset
		}

	private data class LegacyDirectionValue(
		val distance: Int,
		val afterLeftTime: Int,
	)
}
