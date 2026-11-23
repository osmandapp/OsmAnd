package net.osmand.plus.auto

import androidx.car.app.navigation.model.Maneuver
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.osmand.router.TurnType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoundaboutManeuverTest {

	@Test
	fun rightHandTrafficExitAngles() {
		assertEquals(90, exitAngle(exit = 1, turnAngle = 90f, leftSide = false))
		assertEquals(180, exitAngle(exit = 2, turnAngle = 0f, leftSide = false))
		assertEquals(270, exitAngle(exit = 3, turnAngle = -90f, leftSide = false))
		assertEquals(345, exitAngle(exit = 4, turnAngle = -165f, leftSide = false))
	}

	@Test
	fun leftHandTrafficExitAnglesAreMirrored() {
		assertEquals(90, exitAngle(exit = 1, turnAngle = -90f, leftSide = true))
		assertEquals(180, exitAngle(exit = 2, turnAngle = 0f, leftSide = true))
		assertEquals(270, exitAngle(exit = 3, turnAngle = 90f, leftSide = true))
	}

	@Test
	fun exitAngleStaysWithinCarAppRange() {
		assertEquals(360, exitAngle(exit = 4, turnAngle = 180f, leftSide = false))
		assertEquals(360, exitAngle(exit = 4, turnAngle = 179.8f, leftSide = false))
		assertEquals(1, exitAngle(exit = 1, turnAngle = 180f, leftSide = false))
		assertEquals(360, exitAngle(exit = 4, turnAngle = -180f, leftSide = true))
		assertEquals(90, exitAngle(exit = 1, turnAngle = 450f, leftSide = false))
		assertEquals(270, exitAngle(exit = 3, turnAngle = -450f, leftSide = false))
	}

	@Test
	fun roundaboutManeuverCarriesExitNumberAndAngle() {
		val rightHand = TripUtils.createManeuverBuilder(TurnType.getExitTurn(3, -90f, false)).build()
		assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE, rightHand.type)
		assertEquals(3, rightHand.roundaboutExitNumber)
		assertEquals(270, rightHand.roundaboutExitAngle)

		val leftHand = TripUtils.createManeuverBuilder(TurnType.getExitTurn(1, -90f, true)).build()
		assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE, leftHand.type)
		assertEquals(1, leftHand.roundaboutExitNumber)
		assertEquals(90, leftHand.roundaboutExitAngle)
	}

	@Test
	fun regularTurnHasNoRoundaboutData() {
		val maneuver = TripUtils.createManeuverBuilder(TurnType.valueOf(TurnType.TR, false)).build()
		assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, maneuver.type)
	}

	private fun exitAngle(exit: Int, turnAngle: Float, leftSide: Boolean): Int {
		return TripUtils.getRoundaboutExitAngle(TurnType.getExitTurn(exit, turnAngle, leftSide))
	}
}