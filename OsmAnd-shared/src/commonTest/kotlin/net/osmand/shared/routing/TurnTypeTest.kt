package net.osmand.shared.routing

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TurnTypeTest {

	private val allTurns = intArrayOf(
		TurnType.C, TurnType.TL, TurnType.TSLL, TurnType.TSHL, TurnType.TR, TurnType.TSLR,
		TurnType.TSHR, TurnType.KL, TurnType.KR, TurnType.TU, TurnType.TRU, TurnType.OFFR
	)

	@Test
	fun testValueOfMapsToLeftSideVariants() {
		assertEquals(TurnType.TRU, TurnType.valueOf(TurnType.TU, true).value)
		assertEquals(TurnType.TU, TurnType.valueOf(TurnType.TU, false).value)
		assertEquals(TurnType.RNLB, TurnType.valueOf(TurnType.RNDB, true).value)
		assertEquals(TurnType.RNDB, TurnType.valueOf(TurnType.RNDB, false).value)
		// every other type is unaffected by the side of the road
		assertEquals(TurnType.TL, TurnType.valueOf(TurnType.TL, true).value)
	}

	@Test
	fun testStraight() {
		val straight = TurnType.straight()
		assertEquals(TurnType.C, straight.value)
		assertTrue(straight.goAhead())
		assertFalse(straight.isRoundAbout())
		assertNull(straight.lanes)
	}

	@Test
	fun testXmlStringRoundTrip() {
		for (turn in allTurns) {
			val xml = TurnType.valueOf(turn, false).toXmlString()
			assertEquals(turn, TurnType.fromString(xml, false).value, "round trip of $xml")
		}
	}

	@Test
	fun testRoundaboutXmlCarriesTheExit() {
		val turn = TurnType.getExitTurn(3, 45f, false)
		assertTrue(turn.isRoundAbout())
		assertEquals(3, turn.exitOut)
		assertEquals(45f, turn.turnAngle)
		assertEquals("RNDB3", turn.toXmlString())

		val parsed = TurnType.fromString("RNDB3", false)
		assertTrue(parsed.isRoundAbout())
		assertEquals(3, parsed.exitOut)

		val left = TurnType.fromString("RNLB2", false)
		assertEquals(TurnType.RNLB, left.value)
		assertEquals(2, left.exitOut)
		assertTrue(left.isLeftSide())

		assertEquals(5, TurnType.fromString("EXIT5", false).exitOut)
	}

	@Test
	fun testFromStringFallsBackToStraight() {
		assertEquals(TurnType.C, TurnType.fromString(null, false).value)
		assertEquals(TurnType.C, TurnType.fromString("", false).value)
		assertEquals(TurnType.C, TurnType.fromString("nonsense", false).value)
		// a roundabout without a number cannot be parsed and must not blow up
		assertEquals(TurnType.C, TurnType.fromString("RNDB", false).value)
		assertEquals(TurnType.C, TurnType.fromString("EXITx", false).value)
	}

	@Test
	fun testLanePackingIsIndependentPerSlot() {
		val lanes = IntArray(1)
		TurnType.setPrimaryTurnAndReset(lanes, 0, TurnType.TL)
		assertEquals(TurnType.TL, TurnType.getPrimaryTurn(lanes[0]))
		assertEquals(0, TurnType.getSecondaryTurn(lanes[0]))
		assertEquals(0, TurnType.getTertiaryTurn(lanes[0]))
		assertEquals(0, lanes[0] % 2, "the active bit must stay clear")

		TurnType.setSecondaryTurn(lanes, 0, TurnType.TSLR)
		TurnType.setTertiaryTurn(lanes, 0, TurnType.TU)
		assertEquals(TurnType.TL, TurnType.getPrimaryTurn(lanes[0]))
		assertEquals(TurnType.TSLR, TurnType.getSecondaryTurn(lanes[0]))
		assertEquals(TurnType.TU, TurnType.getTertiaryTurn(lanes[0]))

		// overwriting one slot must leave the others alone
		TurnType.setPrimaryTurn(lanes, 0, TurnType.TR)
		assertEquals(TurnType.TR, TurnType.getPrimaryTurn(lanes[0]))
		assertEquals(TurnType.TSLR, TurnType.getSecondaryTurn(lanes[0]))
		assertEquals(TurnType.TU, TurnType.getTertiaryTurn(lanes[0]))

		// setPrimaryTurnAndReset clears the rest again
		TurnType.setPrimaryTurnAndReset(lanes, 0, TurnType.KL)
		assertEquals(TurnType.KL, TurnType.getPrimaryTurn(lanes[0]))
		assertEquals(0, TurnType.getSecondaryTurn(lanes[0]))
		assertEquals(0, TurnType.getTertiaryTurn(lanes[0]))
	}

	@Test
	fun testActiveBitSurvivesTurnUpdates() {
		val lanes = IntArray(1)
		TurnType.setPrimaryTurnAndReset(lanes, 0, TurnType.TL)
		lanes[0] = lanes[0] or 1
		TurnType.setSecondaryTurn(lanes, 0, TurnType.TR)
		TurnType.setTertiaryTurn(lanes, 0, TurnType.TU)
		TurnType.setPrimaryTurn(lanes, 0, TurnType.C)
		assertEquals(1, lanes[0] % 2, "the lane must still be active")
	}

	@Test
	fun testShiftAndSwapHelpers() {
		val lanes = IntArray(1)
		TurnType.setPrimaryTurnAndReset(lanes, 0, TurnType.TL)
		TurnType.setSecondaryTurn(lanes, 0, TurnType.TR)
		TurnType.setTertiaryTurn(lanes, 0, TurnType.TU)

		TurnType.setPrimaryTurnShiftOthers(lanes, 0, TurnType.KR)
		assertEquals(TurnType.KR, TurnType.getPrimaryTurn(lanes[0]))
		assertEquals(TurnType.TL, TurnType.getSecondaryTurn(lanes[0]))
		assertEquals(TurnType.TR, TurnType.getTertiaryTurn(lanes[0]))

		TurnType.setSecondaryToPrimary(lanes, 0)
		assertEquals(TurnType.TL, TurnType.getPrimaryTurn(lanes[0]))
		assertEquals(TurnType.KR, TurnType.getSecondaryTurn(lanes[0]))

		TurnType.setTertiaryToPrimary(lanes, 0)
		assertEquals(TurnType.TR, TurnType.getPrimaryTurn(lanes[0]))
		assertEquals(TurnType.TL, TurnType.getSecondaryTurn(lanes[0]))
		assertEquals(TurnType.KR, TurnType.getTertiaryTurn(lanes[0]))
	}

	@Test
	fun testLanesStringRoundTrip() {
		val lanes = IntArray(3)
		TurnType.setPrimaryTurnAndReset(lanes, 0, TurnType.TL)
		lanes[0] = lanes[0] or 1
		TurnType.setPrimaryTurnAndReset(lanes, 1, TurnType.C)
		TurnType.setSecondaryTurn(lanes, 1, TurnType.TSLR)
		TurnType.setPrimaryTurnAndReset(lanes, 2, TurnType.TR)
		TurnType.setSecondaryTurn(lanes, 2, TurnType.TSHR)
		TurnType.setTertiaryTurn(lanes, 2, TurnType.TU)

		val asString = TurnType.lanesToString(lanes)
		assertEquals("+TL|C,TSLR|TR,TSHR,TU", asString)
		assertContentEquals(lanes, TurnType.lanesFromString(asString))
	}

	@Test
	fun testLanesFromStringEdgeCases() {
		assertNull(TurnType.lanesFromString(null))
		assertNull(TurnType.lanesFromString(""))
		val single = TurnType.lanesFromString("C")
		assertEquals(1, single!!.size)
		assertEquals(TurnType.C, TurnType.getPrimaryTurn(single[0]))
		// a trailing separator must not produce an extra lane, matching Java's String.split
		assertEquals(2, TurnType.lanesFromString("C|TR|")!!.size)
	}

	@Test
	fun testCountingDirections() {
		val turn = TurnType.valueOf(TurnType.C, false)
		assertEquals(0, turn.countTurnTypeDirections(TurnType.C, false), "no lanes means no directions")

		val lanes = IntArray(3)
		TurnType.setPrimaryTurnAndReset(lanes, 0, TurnType.TL)
		lanes[0] = lanes[0] or 1
		TurnType.setPrimaryTurnAndReset(lanes, 1, TurnType.C)
		TurnType.setSecondaryTurn(lanes, 1, TurnType.TL)
		TurnType.setPrimaryTurnAndReset(lanes, 2, TurnType.TL)
		turn.lanes = lanes

		assertEquals(3, turn.countTurnTypeDirections(TurnType.TL, false))
		assertEquals(1, turn.countTurnTypeDirections(TurnType.TL, true), "only the active lane counts")
		assertEquals(2, turn.countDirections(), "TL and C")
		assertEquals(TurnType.TL, turn.getActiveCommonLaneTurn())
	}

	@Test
	fun testClassificationHelpers() {
		assertTrue(TurnType.isLeftTurn(TurnType.TU))
		assertFalse(TurnType.isLeftTurnNoUTurn(TurnType.TU))
		assertTrue(TurnType.isRightTurn(TurnType.TRU))
		assertFalse(TurnType.isRightTurnNoUTurn(TurnType.TRU))
		assertTrue(TurnType.isSlightTurn(TurnType.C))
		assertTrue(TurnType.isKeepDirectionTurn(TurnType.KR))
		assertFalse(TurnType.isKeepDirectionTurn(TurnType.TL))
		assertTrue(TurnType.isSharpOrReverse(TurnType.TSHL))
		assertTrue(TurnType.isSharpLeftOrUTurn(TurnType.TU))
		assertTrue(TurnType.isSharpRightOrUTurn(TurnType.TU))
		assertFalse(TurnType.isSharpLeftOrUTurn(TurnType.TSHR))

		val lane = IntArray(1)
		TurnType.setPrimaryTurnAndReset(lane, 0, TurnType.TL)
		TurnType.setSecondaryTurn(lane, 0, TurnType.TSLR)
		assertTrue(TurnType.hasAnySlightTurnLane(lane[0]))
		assertTrue(TurnType.hasAnyTurnLane(lane[0], TurnType.TL))
		assertFalse(TurnType.hasAnyTurnLane(lane[0], TurnType.TU))

		val collected = LinkedHashSet<Int>()
		TurnType.collectTurnTypes(lane[0], collected)
		assertEquals(setOf(TurnType.TL, TurnType.TSLR), collected)
	}

	@Test
	fun testOrderFromLeftToRight() {
		val ordered = listOf(
			TurnType.TU, TurnType.TSHL, TurnType.TL, TurnType.TSLL, TurnType.KL,
			TurnType.C, TurnType.KR, TurnType.TSLR, TurnType.TR, TurnType.TSHR, TurnType.TRU
		)
		val positions = ordered.map { TurnType.orderFromLeftToRight(it) }
		assertEquals(positions.sorted(), positions, "left to right order must be monotonic")
	}

	@Test
	fun testPrevAndNextWalkTheTurnOrder() {
		assertEquals(TurnType.TSHL, TurnType.getNext(TurnType.TU))
		assertEquals(TurnType.TU, TurnType.getPrev(TurnType.TSHL))
		assertEquals(TurnType.C, TurnType.getNext(TurnType.TSLL))
		// the ends of the order stay put
		assertEquals(TurnType.TU, TurnType.getPrev(TurnType.TU))
		assertEquals(TurnType.TRU, TurnType.getNext(TurnType.TRU))
		// a type outside the order is returned unchanged
		assertEquals(TurnType.OFFR, TurnType.getNext(TurnType.OFFR))
	}

	@Test
	fun testConvertType() {
		assertEquals(TurnType.C, TurnType.convertType("none"))
		assertEquals(TurnType.C, TurnType.convertType("through"))
		assertEquals(TurnType.C, TurnType.convertType("merge_to_left"))
		assertEquals(TurnType.C, TurnType.convertType("merge_to_right"))
		assertEquals(TurnType.TSLR, TurnType.convertType("slight_right"))
		assertEquals(TurnType.TSLL, TurnType.convertType("slight_left"))
		assertEquals(TurnType.TR, TurnType.convertType("right"))
		assertEquals(TurnType.TL, TurnType.convertType("left"))
		assertEquals(TurnType.TSHR, TurnType.convertType("sharp_right"))
		assertEquals(TurnType.TSHL, TurnType.convertType("sharp_left"))
		assertEquals(TurnType.TU, TurnType.convertType("reverse"))
		assertEquals(TurnType.C, TurnType.convertType("something_new"))
	}

	@Test
	fun testToStringMentionsLanes() {
		val turn = TurnType.valueOf(TurnType.TL, false)
		assertEquals("Turn left", turn.toString())
		val lanes = IntArray(1)
		TurnType.setPrimaryTurnAndReset(lanes, 0, TurnType.TL)
		turn.lanes = lanes
		assertEquals("Turn left (TL)", turn.toString())
		assertEquals("Take 2 exit", TurnType.getExitTurn(2, 0f, false).toString())
	}

	@Test
	fun testExitInfo() {
		val info = ExitInfo()
		assertTrue(info.isEmpty())
		info.ref = "12"
		assertFalse(info.isEmpty())
		info.ref = null
		info.exitStreetName = "Some street"
		assertFalse(info.isEmpty())
	}
}
