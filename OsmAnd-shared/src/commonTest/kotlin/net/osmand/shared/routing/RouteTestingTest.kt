package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryMapIndexReader
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The routing cases of OsmAnd-java's `RouteTestingTest`, run through the shared planner: each
 * case routes over the routing test archive - and its own map when it names one - in all three
 * road directions, and the route has to contain the roads the case expects and avoid the ones it
 * forbids.
 *
 * The one case that asks for HH routing is left out until the HH planner is copied. Every other
 * case runs, and the failures are reported together at the end, since this runs wherever the
 * shared code runs and has no parameterized runner to lean on.
 */
class RouteTestingTest {

	@Test
	fun testRouting() {
		RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false
		val failures = ArrayList<String>()
		var cases = 0
		for (entry in RoutingTestFixtures.entries("test_routing.json")) {
			val params = entry.params
			val maps = ArrayList<String>()
			params["map"]?.let { maps.add(RoutingTestFixtures.resource("routing/$it")) }
			maps.add(RoutingTestFixtures.resource("routing/Routing_test_archive.obf"))
			val readers = maps.map { BinaryMapIndexReader(it) }
			try {
				for (planRoadDirection in -1..1) {
					if (params["wrongPlanRoadDirection"] == planRoadDirection.toString()) {
						continue
					}
					cases++
					try {
						routeAndCheck(entry, readers, planRoadDirection)
					} catch (e: Throwable) {
						failures.add(entry.testName + " direction " + planRoadDirection + ": " + e.message)
					}
					if (entry.expectedResults == null) {
						// a case without expectations is about the calculation finishing, one direction is enough
						break
					}
				}
			} finally {
				readers.forEach { it.close() }
			}
		}
		println("RouteTestingTest on ${testPlatformName()}: $cases cases, ${failures.size} failed")
		assertTrue(cases > 200, "cases run: $cases")
		assertTrue(failures.isEmpty(), failures.joinToString("\n"))
	}

	private fun routeAndCheck(entry: RoutingTestFixtures.Entry, readers: List<BinaryMapIndexReader>, planRoadDirection: Int) {
		val params = LinkedHashMap(entry.params)
		val config = RoutingTestFixtures.defaultBuilder().build(params["vehicle"] ?: "car", RoutingTestFixtures.memoryLimits(), params)
		params["routeCalculationTime"]?.let { config.routeCalculationTime = it.toLong() } // conditional
		params["heuristicCoefficient"]?.let { config.heuristicCoefficient = it.toFloat() }
		config.planRoadDirection = planRoadDirection

		val fe = RoutePlannerFrontEnd()
		RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false
		if (params["hh"] == "true") {
			fe.setDefaultHHRoutingConfig()
			fe.setUseOnlyHHRouting(true)
		}
		val ctx = fe.buildRoutingContext(config, readers, RouteCalculationMode.NORMAL)
		ctx.leftSideNavigation = false
		val routeSegments = fe.searchRoute(ctx, entry.startPoint, entry.endPoint, entry.transitPoints).detailed
		val reachedSegments = HashSet<Long>()
		val reachedSegmentPoints = HashSet<String>()
		for (seg in routeSegments) {
			val id = RoutingTestFixtures.osmObjectId(seg.getObject())
			for (point in minOf(seg.getStartPointIndex(), seg.getEndPointIndex())..maxOf(seg.getStartPointIndex(), seg.getEndPointIndex())) {
				reachedSegmentPoints.add("$id:$point")
			}
			reachedSegments.add(id)
		}
		val expectedResults = entry.expectedResults ?: return
		params["maxRoutingTime"]?.let {
			val maxRoutingTime = it.toFloat()
			assertTrue(ctx.routingTime < maxRoutingTime, "Calculated routing time ${ctx.routingTime} is bigger then max routing time $maxRoutingTime")
		}
		for ((key, value) in expectedResults) {
			val id = RoutingTestFixtures.roadId(key)
			val point = RoutingTestFixtures.roadStartPoint(key)
			val pointInSegment = "$id:$point"
			when (value) {
				"false" -> if (point == -1) {
					assertTrue(!reachedSegments.contains(id), "Expected segment $id was wrongly reached in route segments $reachedSegments")
				} else {
					assertTrue(!reachedSegmentPoints.contains(pointInSegment), "Unexpected pointInSegment $pointInSegment is found in $reachedSegmentPoints")
				}
				"true" -> if (point == -1) {
					assertTrue(reachedSegments.contains(id), "Expected segment $id weren't reached in route segments $reachedSegments")
				} else {
					assertTrue(reachedSegmentPoints.contains(pointInSegment), "Expected pointInSegment $pointInSegment is not found in $reachedSegmentPoints")
				}
				"visitedSegments" -> assertTrue(
					ctx.getVisitedSegments() < id,
					"Expected segments visit $id less then actually visited segments ${ctx.getVisitedSegments()}"
				)
				else -> assertTrue(false, "Invalid key $key value $value")
			}
		}
	}
}
