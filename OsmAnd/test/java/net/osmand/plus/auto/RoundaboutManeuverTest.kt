package net.osmand.plus.auto

import androidx.car.app.navigation.model.Maneuver
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.osmand.plus.onlinerouting.engine.GraphhopperEngine
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
	fun uTurnWithFlippedSignStaysUTurn() {
		assertEquals(356, exitAngle(exit = 4, turnAngle = 176.2f, leftSide = false))
		assertEquals(356, exitAngle(exit = 3, turnAngle = -176.2f, leftSide = true))
		assertEquals(4, exitAngle(exit = 1, turnAngle = -176.2f, leftSide = false))
		assertEquals(4, exitAngle(exit = 1, turnAngle = 176.2f, leftSide = true))
	}

	@Test
	fun secondExitKeepsCalculatedAngleNearBoundary() {
		assertEquals(4, exitAngle(exit = 2, turnAngle = 176.2f, leftSide = false))
		assertEquals(356, exitAngle(exit = 2, turnAngle = -176.2f, leftSide = false))
	}

	@Test
	fun graphhopperRadiansBecomeExitAngle() {
		assertEquals(90, graphhopperExitAngle(exit = 1, radians = -Math.PI / 2, leftSide = false))
		assertEquals(270, graphhopperExitAngle(exit = 3, radians = -Math.PI * 3 / 2, leftSide = false))
		assertEquals(90, graphhopperExitAngle(exit = 1, radians = Math.PI / 2, leftSide = true))
		assertEquals(270, graphhopperExitAngle(exit = 3, radians = Math.PI * 3 / 2, leftSide = true))
		assertEquals(350, graphhopperExitAngle(exit = 4, radians = -Math.toRadians(350.0), leftSide = false))
	}

	@Test
	fun unknownExitAngleKeepsPlainRoundaboutManeuver() {
		val rightHand = TurnType.getExitTurn(2, 0f, false).apply { isExitAngleUnknown = true }
		val rightManeuver = TripUtils.createManeuverBuilder(rightHand).build()
		assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, rightManeuver.type)
		assertEquals(2, rightManeuver.roundaboutExitNumber)

		val leftHand = TurnType.getExitTurn(2, 0f, true).apply { isExitAngleUnknown = true }
		val leftManeuver = TripUtils.createManeuverBuilder(leftHand).build()
		assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW, leftManeuver.type)
		assertEquals(2, leftManeuver.roundaboutExitNumber)
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

	private fun graphhopperExitAngle(exit: Int, radians: Double, leftSide: Boolean): Int {
		val turnAngle = GraphhopperEngine.getRoundaboutTurnAngle(radians, leftSide)
		return exitAngle(exit, turnAngle, leftSide)
	}
}