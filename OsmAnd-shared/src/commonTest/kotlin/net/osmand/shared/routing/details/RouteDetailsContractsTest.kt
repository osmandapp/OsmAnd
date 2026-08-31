package net.osmand.shared.routing.details

import net.osmand.shared.data.KLatLon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RouteDetailsContractsTest {

	@Test
	fun semanticTypesMatchAndroidConstantsAndPriorities() {
		assertEquals(RouteManeuverType.STRAIGHT, RouteManeuverType.fromLegacyValue(1))
		assertEquals(RouteManeuverType.ROUNDABOUT_LEFT, RouteManeuverType.fromLegacyValue(14))
		assertNull(RouteManeuverType.fromLegacyValue(15))
		assertNull(sampleSnapshot().maneuvers.first().copy(turnTypeValue = 99).type)
		assertEquals(1, RouteEventType.SPEED_CAMERA.androidPriority)
		assertEquals(10, RouteEventType.MAXIMUM.androidPriority)
		assertEquals(12, RouteEventType.RED_LIGHT_CAMERA.androidPriority)
	}

	@Test
	fun snapshotPreservesPlatformValuesWithoutRuntimeValidation() {
		val snapshot = sampleSnapshot()
		val sourcePoints = snapshot.points.reversed()
		val sourceSummary = snapshot.summary.copy(totalDistanceMeters = -1)
		val copied = snapshot.copy(
			points = sourcePoints,
			summary = sourceSummary,
			currentRoutePointIndex = -1,
		)

		assertEquals(sourcePoints, copied.points)
		assertEquals(sourceSummary, copied.summary)
		assertEquals(-1, copied.currentRoutePointIndex)
	}

	private fun sampleSnapshot(): RouteDetailsSnapshot {
		val points = listOf(
			RoutePoint(
				location = KLatLon(0.0, 0.0),
				distanceToFinishMeters = 111,
				altitudeMeters = 5.0,
				speedMetersPerSecond = 11.1319f,
				provider = "",
			),
			RoutePoint(
				location = KLatLon(0.0, 0.001),
				distanceToFinishMeters = 0,
				altitudeMeters = 6.0,
				speedMetersPerSecond = 11.1319f,
				provider = "",
			),
		)
		val segment = RouteSegment(
			routePointStartIndex = 0,
			routePointEndIndex = 1,
			nativeStartPointIndex = 0,
			nativeEndPointIndex = 1,
			distanceMeters = 111.319f,
			segmentTimeSeconds = 10f,
			segmentSpeedMetersPerSecond = 11.1319f,
			roadId = 42L,
			forward = true,
			highway = "residential",
			maximumSpeedMetersPerSecond = 13.8889f,
			lanes = 2,
			oneWayDirection = 1,
			routeTypes = listOf(
				RouteTypeAttribute("highway", "residential"),
				RouteTypeAttribute("access", "yes"),
				RouteTypeAttribute("access", "destination"),
			),
			heightValues = floatArrayOf(0f, 5f, 111.319f, 6f),
		)
		val maneuvers = listOf(
			RouteManeuver(
				turnTypeValue = RouteManeuverType.STRAIGHT.legacyValue,
				routePointOffset = 0,
				routeEndPointOffset = 0,
				distanceMeters = 111,
				expectedTimeSeconds = 10,
				afterLeftTimeSeconds = 10,
				averageSpeedMetersPerSecond = 11.1319f,
				turnAngleDegrees = 0f,
				exitNumber = 0,
			),
			RouteManeuver(
				turnTypeValue = RouteManeuverType.TURN_RIGHT.legacyValue,
				routePointOffset = 1,
				routeEndPointOffset = 0,
				distanceMeters = 0,
				expectedTimeSeconds = 0,
				afterLeftTimeSeconds = 0,
				averageSpeedMetersPerSecond = 11.1319f,
				turnAngleDegrees = 90f,
				exitNumber = 0,
				lanes = listOf(1, 2),
			),
		)
		return RouteDetailsSnapshot(
			points = points,
			segments = listOf(segment),
			maneuvers = maneuvers,
			events = listOf(
				RouteEvent(
					type = RouteEventType.STOP,
					location = points.last().location,
					locationIndex = points.size,
				),
			),
			summary = RouteSummary(
				totalDistanceMeters = 111,
				totalTimeSeconds = 10,
				profileId = "car",
				routeService = RouteServiceType.OSMAND,
			),
			statistics = listOf(
				RouteStatistic(
					name = "surface",
					elements = listOf(
						RouteStatisticElement("asphalt", "asphalt", -1, 111.319f),
					),
					partition = listOf(
						RouteStatisticElement("asphalt", "asphalt", -1, 111.319f),
					),
					totalDistanceMeters = 111.319f,
				),
			),
			currentRoutePointIndex = points.size,
			currentDirectionIndex = maneuvers.size,
		)
	}
}
