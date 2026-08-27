package net.osmand.shared.routing.details

import net.osmand.shared.data.KLatLon
import kotlin.test.Test
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
		assertEquals(
			expected = listOf(221_893, 110_574, 0),
			actual = RouteGeometryCalculator.calculate(locations).distanceToFinishMeters,
		)
	}

	@Test
	fun routeGeometryHandlesEmptyAndSinglePointRoutes() {
		assertEquals(emptyList(), RouteGeometryCalculator.calculate(emptyList()).distanceToFinishMeters)
		assertEquals(
			listOf(0),
			RouteGeometryCalculator.calculate(listOf(KLatLon(0.0, 0.0))).distanceToFinishMeters,
		)
	}

	@Test
	fun calculatedDistancesAreCopiedToRoutePointsWithoutChangingSourceFields() {
		val points = listOf(
			RoutePoint(
				location = KLatLon(0.0, 0.0),
				distanceToFinishMeters = 0,
				altitudeMeters = 12.5,
				provider = "test",
			),
			RoutePoint(
				location = KLatLon(0.0, 0.001),
				distanceToFinishMeters = 0,
				altitudeMeters = 13.5,
				provider = "test",
			),
		)

		val updated = RouteGeometryCalculator.withCalculatedDistances(points)

		assertEquals(listOf(111, 0), updated.map(RoutePoint::distanceToFinishMeters))
		assertEquals(points.map(RoutePoint::location), updated.map(RoutePoint::location))
		assertEquals(points.map(RoutePoint::altitudeMeters), updated.map(RoutePoint::altitudeMeters))
		assertEquals(points.map(RoutePoint::provider), updated.map(RoutePoint::provider))
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
	}

	@Test
	fun intermediatePointSplitsDirectionLikeAndroid() {
		val locations = (0..3).map { index -> KLatLon(0.0, index * 0.001) }
		val original = listOf(
			maneuver(offset = 0, averageSpeed = 12f),
			maneuver(
				offset = 3,
				averageSpeed = 8f,
				turnTypeValue = RouteManeuverType.TURN_RIGHT.legacyValue,
				streetName = "Main Street",
				ref = "A1",
				destinationName = "Centre",
			),
		)

		val result = RouteManeuverCalculator.calculateIntermediateIndexes(
			locations = locations,
			maneuvers = original,
			intermediates = listOf(locations[1], locations[3]),
		)

		assertEquals(listOf(0, 1, 3), result.maneuvers.map(RouteManeuver::routePointOffset))
		assertEquals(listOf(1, 2), result.intermediateDirectionIndices)
		val inserted = result.maneuvers[1]
		assertEquals(RouteManeuverType.STRAIGHT.legacyValue, inserted.turnTypeValue)
		assertEquals(12f, inserted.averageSpeedMetersPerSecond)
		assertEquals("Main Street", inserted.streetName)
		assertEquals("A1", inserted.ref)
		assertEquals("Centre", inserted.destinationName)
		assertEquals(original[1], result.maneuvers[2])
	}

	@Test
	fun unmatchedIntermediateLeavesAndroidZeroSentinelsAndDirectionsUnchanged() {
		val locations = listOf(KLatLon(0.0, 0.0), KLatLon(0.0, 0.001))
		val maneuvers = listOf(maneuver(offset = 0, averageSpeed = 10f))

		val result = RouteManeuverCalculator.calculateIntermediateIndexes(
			locations = locations,
			maneuvers = maneuvers,
			intermediates = listOf(KLatLon(50.0, 50.0), KLatLon(51.0, 51.0)),
		)

		assertEquals(maneuvers, result.maneuvers)
		assertEquals(listOf(0, 0), result.intermediateDirectionIndices)
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
