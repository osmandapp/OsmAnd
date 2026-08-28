package net.osmand.plus.routing

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.osmand.Location
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.router.ExitInfo
import net.osmand.router.TurnType
import net.osmand.shared.routing.details.RouteEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RouteCalculationResultSnapshotAdapterTest {

	@Test
	fun calculatedRouteOwnsOneCachedSnapshot() {
		val route = RouteCalculationResult("test error")

		val first = route.routeDetailsSnapshot
		val second = route.routeDetailsSnapshot

		assertSame(first, second)
		assertTrue(first.points.isEmpty())
		assertTrue(first.segments.isEmpty())
		assertTrue(first.maneuvers.isEmpty())
		assertTrue(first.events.isEmpty())
		assertEquals(0, first.summary.totalDistanceMeters)
	}

	@Test
	fun routeConstructorCachesPointsManeuversAndSummaryAfterAndroidCalculations() {
		val start = Location("start").apply {
			latitude = 0.0
			longitude = 0.0
			altitude = 12.5
			speed = 10f
			time = 1234L
		}
		val finish = Location("finish").apply {
			latitude = 0.0
			longitude = 0.001
		}
		val direction = RouteDirectionInfo(10f, TurnType.straight()).apply {
			routePointOffset = 0
		}
		val params = RouteCalculationParams().apply {
			mode = ApplicationMode.DEFAULT
			initialCalculation = true
		}

		val route = RouteCalculationResult(
			listOf(start, finish),
			listOf(direction),
			params,
			null,
			false,
		)
		val snapshot = route.routeDetailsSnapshot

		assertEquals(listOf(111, 0), snapshot.points.map { it.distanceToFinishMeters })
		assertEquals(12.5, snapshot.points.first().altitudeMeters)
		assertEquals(10f, snapshot.points.first().speedMetersPerSecond)
		assertEquals(1234L, snapshot.points.first().timeMillis)
		assertEquals("start", snapshot.points.first().provider)
		assertEquals(111, snapshot.summary.totalDistanceMeters)
		assertEquals(11, snapshot.summary.totalTimeSeconds)
		assertEquals(ApplicationMode.DEFAULT.stringKey, snapshot.summary.profileId)
		assertEquals(true, snapshot.summary.initialCalculation)
		assertEquals(111, snapshot.maneuvers.single().distanceMeters)
		assertEquals(11, snapshot.maneuvers.single().expectedTimeSeconds)
		assertSame(snapshot, route.routeDetailsSnapshot)

		start.latitude = 1.0
		direction.streetName = "Changed after snapshot"
		assertEquals(0.0, snapshot.points.first().location.latitude, 0.0)
		assertNull(snapshot.maneuvers.single().streetName)
	}

	@Test
	fun maneuverCopyPreservesAndroidFieldsAndOwnsMutableCollections() {
		val lanes = intArrayOf(11, 22)
		val otherAngles = mutableListOf(12.5f, 45f)
		val turn = TurnType(
			TurnType.TR,
			3,
			90f,
			true,
			lanes,
			true,
			false,
		).apply {
			setOtherTurnAngles(otherAngles)
		}
		val direction = RouteDirectionInfo(10f, turn).apply {
			routePointOffset = 4
			routeEndPointOffset = 6
			distance = 100
			afterLeftTime = 25
			streetName = "Main Street"
			ref = "A 1"
			destinationName = "Centre"
			destinationRef = "B 2"
			exitInfo = ExitInfo().apply {
				ref = "3"
				exitStreetName = "Exit Road"
			}
		}

		val snapshot = RouteCalculationResultSnapshotAdapter.copyManeuver(direction)

		assertEquals(TurnType.TR, snapshot.turnTypeValue)
		assertEquals(4, snapshot.routePointOffset)
		assertEquals(6, snapshot.routeEndPointOffset)
		assertEquals(100, snapshot.distanceMeters)
		assertEquals(10, snapshot.expectedTimeSeconds)
		assertEquals(25, snapshot.afterLeftTimeSeconds)
		assertEquals(10f, snapshot.averageSpeedMetersPerSecond)
		assertEquals(90f, snapshot.turnAngleDegrees)
		assertEquals(3, snapshot.exitNumber)
		assertEquals(listOf(11, 22), snapshot.lanes)
		assertEquals(true, snapshot.skipToSpeak)
		assertEquals(true, snapshot.possibleLeftTurn)
		assertEquals(false, snapshot.possibleRightTurn)
		assertEquals(listOf(12.5f, 45f), snapshot.otherTurnAngles)
		assertEquals("Main Street", snapshot.streetName)
		assertEquals("A 1", snapshot.ref)
		assertEquals("Centre", snapshot.destinationName)
		assertEquals("B 2", snapshot.destinationRef)
		assertEquals("3", snapshot.exitInfo?.ref)
		assertEquals("Exit Road", snapshot.exitInfo?.exitStreetName)

		lanes[0] = 99
		otherAngles.clear()
		direction.streetName = "Changed"
		assertEquals(listOf(11, 22), snapshot.lanes)
		assertEquals(listOf(12.5f, 45f), snapshot.otherTurnAngles)
		assertEquals("Main Street", snapshot.streetName)
	}

	@Test
	fun maneuverCopyPreservesAndroidNanAverageSpeed() {
		val direction = RouteDirectionInfo(Float.NaN, TurnType.straight()).apply {
			distance = 0
		}

		val snapshot = RouteCalculationResultSnapshotAdapter.copyManeuver(direction)

		assertTrue(snapshot.averageSpeedMetersPerSecond.isNaN())
		assertEquals(0, snapshot.expectedTimeSeconds)
	}

	@Test
	fun alarmCopyMapsEveryStoredBackendValueWithoutUiData() {
		val alarm = AlarmInfo(AlarmInfoType.RED_LIGHT_CAMERA, 7).apply {
			lastLocationIndex = 9
			intValue = 50
			floatValue = 13.8889f
			setLatLon(51.5, -0.1)
		}

		val snapshot = RouteCalculationResultSnapshotAdapter.copyEvent(alarm)

		assertEquals(RouteEventType.RED_LIGHT_CAMERA, snapshot.type)
		assertEquals(51.5, snapshot.location.latitude, 0.0)
		assertEquals(-0.1, snapshot.location.longitude, 0.0)
		assertEquals(7, snapshot.locationIndex)
		assertEquals(9, snapshot.lastLocationIndex)
		assertEquals(50, snapshot.intValue)
		assertEquals(13.8889f, snapshot.floatValue)
	}

	@Test
	fun pointAlignedSegmentRunsBecomeInclusiveSharedRanges() {
		assertEquals(
			0 to 2,
			RouteCalculationResultSnapshotAdapter.routePointRange(0, 2, 0, 2, 5),
		)
		assertEquals(
			2 to 4,
			RouteCalculationResultSnapshotAdapter.routePointRange(0, 2, 2, 5, 5),
		)
		assertEquals(
			0 to 2,
			RouteCalculationResultSnapshotAdapter.routePointRange(0, 2, 0, 3, 6),
		)
		assertEquals(
			3 to 4,
			RouteCalculationResultSnapshotAdapter.routePointRange(0, 1, 4, 5, 5),
		)
	}
}
