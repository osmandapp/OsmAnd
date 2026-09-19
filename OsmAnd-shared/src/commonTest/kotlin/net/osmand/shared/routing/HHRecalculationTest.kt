package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.KLatLon
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The hub-graph edges a long route drops before its detailed phase come back with the costs the
 * roads corrected, so a recalculation converges the way it does with the edges kept.
 *
 * A route over a real map (the routing test fixture with an HH section is a cutout too small for
 * a hub edge): the first of the cases whose map is found where the planner benchmark looks for
 * its maps, or in `OSMAND_OBF_DIRECTORY`; skipped when none is.
 * The hub graph agrees with its roads, so the test ages it by hand: it halves the cost of every
 * hub edge of the route, as a map update would have, and routes again. The detailed
 * phase then finds every shortcut cheaper than the roads, corrects it and searches once more. With
 * the edges dropped and the corrections lost, that search would find the same wrong shortcuts on
 * every pass and spend all its recalculations - which the count catches.
 */
class HHRecalculationTest {

	private class Case(val map: String, val start: KLatLon, val end: KLatLon)

	private val cases = listOf(
		Case("Germany_bayern_lower-franconia_europe.obf", KLatLon(49.7913, 9.9534), KLatLon(50.0444, 10.2322)), // würzburg schweinfurt
		Case("Germany_bayern_upper-bavaria_europe.obf", KLatLon(48.1374, 11.5755), KLatLon(47.4917, 11.0954)), // munich garmisch
		Case("Netherlands_noord-holland_europe.obf", KLatLon(52.3791, 4.9003), KLatLon(52.9563, 4.7606)), // amsterdam den helder
		Case("Austria_lower-austria_europe.obf", KLatLon(48.2047, 15.6256), KLatLon(47.8100, 16.2450)) // st pölten wr. neustadt
	)

	private lateinit var start: KLatLon
	private lateinit var end: KLatLon

	@Test
	fun droppedEdgesKeepTheirCorrectedCosts() {
		val directories = ArrayList(RoutePlannerBenchmarkTest.OBF_DIRECTORIES)
		testEnvironment("OSMAND_OBF_DIRECTORY")?.let { directories.add(0, it) }
		val found = cases.firstNotNullOfOrNull { case ->
			directories.map { (it.toPath() / case.map) }.firstOrNull { FileSystem.SYSTEM.exists(it) }?.let { case to it }
		}
		if (found == null) {
			println("HHRecalculationTest: none of the maps of the cases found, skipped")
			return
		}
		val (case, map) = found
		start = case.start
		end = case.end
		val readers = listOf(BinaryMapIndexReader(map.toString()))
		val threshold = HHRoutePlanner.FREE_EDGES_SETTLED_POINTS
		val collect = HHRoutePlanner.COLLECT_GARBAGE_AFTER_FREEING_EDGES
		try {
			val config = RoutingTestFixtures.defaultBuilder().build("car", RoutingTestFixtures.memoryLimits(), LinkedHashMap())
			val fe = RoutePlannerFrontEnd()
			RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false
			val hhConfig = HHRoutingConfig.astar(0).calcDetailed(2).cacheContext(null)
			fe.setHHRoutingConfig(hhConfig)
			fe.setUseOnlyHHRouting(true)
			val ctx = fe.buildRoutingContext(config, readers, RouteCalculationMode.NORMAL)

			HHRoutePlanner.FREE_EDGES_SETTLED_POINTS = Int.MAX_VALUE
			val fresh = route(fe, ctx)
			assertTrue(fresh.hubEdges.isNotEmpty(), "the route has to run over the hub graph")
			assertEquals(0, fresh.recalculations, "a hub graph that agrees with the roads")

			age(hhConfig, fresh)
			val kept = route(fe, ctx)
			assertTrue(kept.recalculations > 0, "an aged hub graph has to make the planner recalculate")
			assertEquals(fresh.roads, kept.roads, "the roads win over the aged hub graph")

			HHRoutePlanner.FREE_EDGES_SETTLED_POINTS = 0
			for (gc in listOf(false, true)) {
				HHRoutePlanner.COLLECT_GARBAGE_AFTER_FREEING_EDGES = gc
				age(hhConfig, fresh)
				val dropped = route(fe, ctx)
				assertEquals(kept.recalculations, dropped.recalculations, "recalculations with the edges dropped (gc=$gc)")
				assertEquals(fresh.roads, dropped.roads, "route with the edges dropped (gc=$gc)")
			}
		} finally {
			HHRoutePlanner.FREE_EDGES_SETTLED_POINTS = threshold
			HHRoutePlanner.COLLECT_GARBAGE_AFTER_FREEING_EDGES = collect
			readers.forEach { it.close() }
		}
	}

	private class Outcome(val roads: List<String>, val hubEdges: List<NetworkDBSegment>, val recalculations: Int)

	private fun route(fe: RoutePlannerFrontEnd, ctx: RoutingContext): Outcome {
		val res = fe.searchRoute(ctx, start, end, null)
		assertTrue(res.isCorrect(), "route: " + res.getError())
		val hh = res as HHNetworkRouteRes
		val roads = hh.detailed.map { "${RoutingTestFixtures.osmObjectId(it.getObject())}:${it.getStartPointIndex()}-${it.getEndPointIndex()}" }
		return Outcome(roads, hh.segments.mapNotNull { it.segment }, hh.stats!!.recalculations)
	}

	/** Halves the cost of the route's hub edges in the cached context, the way a map update leaves shortcuts behind the roads. */
	private fun age(hhConfig: HHRoutingConfig, fresh: Outcome) {
		val hctx = hhConfig.cacheCtx!!
		for (edge in fresh.hubEdges) {
			val loaded = (if (edge.direction) edge.start.getSegment(edge.end, true) else edge.end.getSegment(edge.start, false)) ?: edge
			hctx.setEdgeCost(loaded, loaded.dist / 2)
		}
	}
}
