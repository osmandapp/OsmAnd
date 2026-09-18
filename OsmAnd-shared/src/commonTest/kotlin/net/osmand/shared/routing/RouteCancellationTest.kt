package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryMapIndexReader
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A cancelled search has to come back as a result, not as an exception.
 *
 * Java throws `InterruptedException` out of the planner and android catches it around the call;
 * on iOS there is nothing to catch with - a Kotlin exception reaching Objective-C terminates the
 * app - so the shared planner returns the reason instead of throwing it.
 */
class RouteCancellationTest {

	@Test
	fun cancelledSearchReturnsAnErrorInsteadOfThrowing() {
		val entry = RoutingTestFixtures.entries("test_routing.json").first { it.params["map"] == null }
		val readers = listOf(BinaryMapIndexReader(RoutingTestFixtures.resource("routing/Routing_test_archive.obf")))
		try {
			val config = RoutingTestFixtures.defaultBuilder()
				.build("car", RoutingTestFixtures.memoryLimits(), LinkedHashMap())
			val fe = RoutePlannerFrontEnd()
			RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false
			val ctx = fe.buildRoutingContext(config, readers, RouteCalculationMode.NORMAL)
			val progress = RouteCalculationProgress()
			progress.isCancelled = true // as the app does when a newer calculation supersedes this one
			ctx.calculationProgress = progress

			val res = fe.searchRoute(ctx, entry.startPoint, entry.endPoint, null)

			assertTrue(!res.isCorrect(), "a cancelled search must not come back with a route")
			assertNotNull(res.getError(), "the result has to carry the reason there is no route")
		} finally {
			readers.forEach { it.close() }
		}
	}
}
