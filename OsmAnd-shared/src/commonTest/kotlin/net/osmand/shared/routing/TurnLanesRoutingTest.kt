package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryMapIndexReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The turn lane cases of OsmAnd-java's `RouteResultPreparationTest`, run through the shared
 * planner: each case routes over `Turn_lanes_test.obf` and, at every road the case names, the
 * manoeuvre and the lanes the driver is shown have to be the ones expected.
 *
 * The check is the java one: a road's expectation matches the turn with its lanes, the lanes
 * alone, or the turn alone; a muted turn is prefixed `[MUTE]`; a road named without an expectation
 * must carry no turn; and every road named must be on the route.
 */
class TurnLanesRoutingTest {

	@Test
	fun testLanes() {
		RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false
		val failures = ArrayList<String>()
		var cases = 0
		val reader = BinaryMapIndexReader(RoutingTestFixtures.resource("Turn_lanes_test.obf"))
		try {
			for (entry in RoutingTestFixtures.entries("test_turn_lanes.json")) {
				cases++
				try {
					routeAndCheck(entry, listOf(reader))
				} catch (e: Throwable) {
					failures.add(entry.testName + ": " + e.message)
				}
			}
		} finally {
			reader.close()
		}
		println("TurnLanesRoutingTest on ${testPlatformName()}: $cases cases, ${failures.size} failed")
		assertTrue(cases > 100, "cases run: $cases")
		assertTrue(failures.isEmpty(), failures.joinToString("\n"))
	}

	private fun routeAndCheck(entry: RoutingTestFixtures.Entry, readers: List<BinaryMapIndexReader>) {
		val params = LinkedHashMap(entry.params)
		params["car"] = "true"
		val config = RoutingTestFixtures.defaultBuilder().build("car", RoutingTestFixtures.memoryLimits(), params)
		val fe = RoutePlannerFrontEnd()
		val ctx = fe.buildRoutingContext(config, readers, RouteCalculationMode.NORMAL)
		ctx.leftSideNavigation = false

		val routeSegments = fe.searchRoute(ctx, entry.startPoint, entry.endPoint, null).detailed
		val reachedSegmentsWithStartPoint = HashSet<String>()
		val reachedSegments = HashMap<Long, RouteSegmentResult>()
		val checkedSegments = HashSet<Long>()
		val expectedResults = entry.expectedResults ?: emptyMap()
		var prevSegment = -1
		for (i in 0..routeSegments.size) {
			if (i == routeSegments.size || routeSegments[i].getTurnType() != null) {
				if (prevSegment >= 0) {
					val segment = routeSegments[prevSegment]
					val lanes = lanesString(segment)
					val turn = segment.getTurnType()!!.toXmlString()
					var turnLanes = "$turn:$lanes"
					if (segment.getTurnType()!!.isSkipToSpeak) {
						turnLanes = "[MUTE] $turnLanes"
					}
					val segmentId = RoutingTestFixtures.osmObjectId(segment.getObject())
					var expectedResult: String? = null
					var startPoint = -1
					for (er in expectedResults.entries) {
						if (RoutingTestFixtures.roadId(er.key) == segmentId) {
							expectedResult = er.value
							startPoint = RoutingTestFixtures.roadStartPoint(er.key)
						}
					}
					if (expectedResult != null) {
						if (startPoint < 0 || segment.getStartPointIndex() == startPoint) {
							if (expectedResult != turnLanes && expectedResult != lanes && expectedResult != turn) {
								assertEquals(expectedResult, turnLanes, "Segment $segmentId")
							}
						}
					}
				}
				prevSegment = i
				if (i < routeSegments.size) {
					checkedSegments.add(RoutingTestFixtures.osmObjectId(routeSegments[i].getObject()))
				}
			}
			if (i < routeSegments.size) {
				val id = RoutingTestFixtures.osmObjectId(routeSegments[i].getObject())
				reachedSegmentsWithStartPoint.add(id.toString() + ":" + routeSegments[i].getStartPointIndex())
				reachedSegments[id] = routeSegments[i]
			}
		}
		for ((roadInfo, expectedResult) in expectedResults) {
			val id = RoutingTestFixtures.roadId(roadInfo)
			val startPoint = RoutingTestFixtures.roadStartPoint(roadInfo)
			assertTrue(
				if (startPoint == -1) reachedSegments.containsKey(id) else reachedSegmentsWithStartPoint.contains(roadInfo),
				"Segment $roadInfo was not reached in $reachedSegmentsWithStartPoint"
			)
			if (!checkedSegments.contains(id)) {
				if (expectedResult.isNotEmpty()) {
					assertEquals(expectedResult, "NULL", "Segment $id")
				}
			}
		}
		val expectedExits = entry.expectedExits
		if (expectedExits != null) {
			for ((key, expectedRef) in expectedExits) {
				val id = RoutingTestFixtures.roadId(key)
				assertTrue(reachedSegments.containsKey(id), "exit road $id was not reached")
				val hasExitInfo = reachedSegments[id]!!.hasExitInfo()
				if (expectedRef.isEmpty()) {
					assertTrue(!hasExitInfo, "road $id has exit info")
				} else {
					val actualRef = reachedSegments[id]!!.getObject().getExitRef()
					assertTrue(hasExitInfo && expectedRef == actualRef, "road $id exit ref $actualRef, expected $expectedRef")
				}
			}
		}
	}

	private fun lanesString(segment: RouteSegmentResult): String? {
		val lns = segment.getTurnType()!!.lanes ?: return null
		return TurnType.lanesToString(lns)
	}
}
