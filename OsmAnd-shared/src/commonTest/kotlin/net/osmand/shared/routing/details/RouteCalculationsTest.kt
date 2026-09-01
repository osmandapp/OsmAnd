package net.osmand.shared.routing.details

import net.osmand.shared.data.KLatLon
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RouteCalculationsTest {

	@Test
	fun routeGeometryUsesAndroidWgs84DistanceAndPerEdgeRounding() {
		val locations = listOf(
			KLatLon(0.0, 0.0),
			KLatLon(0.0, 1.0),
			KLatLon(1.0, 1.0),
		)

		assertEquals(
			expected = 111_319.49f,
			actual = RouteGeometryCalculator.distanceBetween(locations[0], locations[1]),
			absoluteTolerance = 0.01f,
		)
		assertContentEquals(
			expected = intArrayOf(221_893, 110_574, 0),
			actual = RouteGeometryCalculator.calculate(locations).distanceToFinishMeters,
		)
	}

	@Test
	fun routeGeometryHandlesEmptyAndSinglePointRoutes() {
		assertContentEquals(
			intArrayOf(),
			RouteGeometryCalculator.calculate(emptyList()).distanceToFinishMeters,
		)
		assertContentEquals(
			intArrayOf(0),
			RouteGeometryCalculator.calculate(listOf(KLatLon(0.0, 0.0))).distanceToFinishMeters,
		)
	}

	@Test
	fun locationByDistanceUsesAndroidSignedDirectionAndDirectDistance() {
		val locations = (0..4).map { index -> KLatLon(0.0, index * 0.001) }

		assertEquals(locations[3], RouteGeometryCalculator.locationByDistance(locations, 2, 100))
		assertEquals(locations[1], RouteGeometryCalculator.locationByDistance(locations, 2, -100))
		assertEquals(locations[1], RouteGeometryCalculator.locationByDistance(locations, 2, 0))
		assertNull(RouteGeometryCalculator.locationByDistance(locations, 2, 1_000))
		assertNull(RouteGeometryCalculator.locationByDistance(locations, locations.size, 100))
	}

	@Test
	fun maneuverDistancesAndTimesMatchAndroidReverseCalculation() {
		val geometry = RouteGeometryCalculator.calculate(
			listOf(
				KLatLon(0.0, 0.0),
				KLatLon(0.0, 1.0),
				KLatLon(1.0, 1.0),
			),
		)
		val maneuvers = listOf(
			maneuver(offset = 0, averageSpeed = 10f),
			maneuver(offset = 1, averageSpeed = 10f),
			maneuver(offset = 2, averageSpeed = 1f),
		)

		val updated = RouteManeuverCalculator.updateDistancesAndTimes(maneuvers, geometry)

		assertEquals(listOf(111_319, 110_574, 0), updated.map(RouteManeuver::distanceMeters))
		assertEquals(listOf(11_132, 11_057, 0), updated.map(RouteManeuver::expectedTimeSeconds))
		assertEquals(listOf(22_189, 11_057, 0), updated.map(RouteManeuver::afterLeftTimeSeconds))
		assertEquals(
			RouteCumulativeInfo(distanceMeters = 221_893, timeSeconds = 22_189),
			RouteManeuverCalculator.cumulativeInfoBefore(2, updated),
		)
		assertEquals(
			RouteCumulativeInfo(distanceMeters = 0, timeSeconds = 0),
			RouteManeuverCalculator.cumulativeInfoBefore(updated.size, updated),
		)
		assertEquals(
			(0..updated.size).map { position ->
				RouteManeuverCalculator.cumulativeInfoBefore(position, updated)
			},
			RouteManeuverCalculator.cumulativeInfoByPosition(updated),
		)
	}

	@Test
	fun routePointCumulativeInfoUsesOneForwardManeuverPass() {
		val distanceToFinish = IntArray(11) { index -> 1_000 - index * 100 }
		val maneuvers = listOf(
			maneuver(offset = 0, averageSpeed = 10f),
			maneuver(offset = 4, averageSpeed = 20f),
			maneuver(offset = 7, averageSpeed = 5f),
		)

		assertEquals(
			listOf(
				RouteCumulativeInfo(0, 0),
				RouteCumulativeInfo(100, 10),
				RouteCumulativeInfo(300, 30),
				RouteCumulativeInfo(400, 35),
				RouteCumulativeInfo(400, 35),
				RouteCumulativeInfo(800, 85),
			),
			RouteManeuverCalculator.cumulativeInfoAtRoutePoints(
				ManeuverAccessor(maneuvers),
				distanceToFinish,
				currentRoutePointIndex = 1,
				currentManeuverIndex = 0,
				routePointOffsets = intArrayOf(0, 2, 4, 5, 5, 9),
			),
		)
	}

	private fun maneuver(
		offset: Int,
		averageSpeed: Float,
		turnTypeValue: Int = RouteManeuverType.STRAIGHT.legacyValue,
		streetName: String? = null,
		ref: String? = null,
		destinationName: String? = null,
	): RouteManeuver {
		return RouteManeuver(
			turnTypeValue = turnTypeValue,
			routePointOffset = offset,
			routeEndPointOffset = 0,
			distanceMeters = 0,
			expectedTimeSeconds = 0,
			afterLeftTimeSeconds = 0,
			averageSpeedMetersPerSecond = averageSpeed,
			turnAngleDegrees = 0f,
			exitNumber = 0,
			streetName = streetName,
			ref = ref,
			destinationName = destinationName,
		)
	}
}
