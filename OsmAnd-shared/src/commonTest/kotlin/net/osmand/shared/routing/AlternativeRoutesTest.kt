package net.osmand.shared.routing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Alternative routes of [HHRoutePlanner], the shared side of OsmAnd-java's `AlternativeRoutesTest`
 * (OsmAnd-Issues #2843): the same cases from `alternatives/test_alternative_routes.json` - the
 * map, start and end, and what the alternatives are expected to do - with the same general checks
 * on every case, each of which stands for something that has actually gone wrong.
 *
 * The cases need whole city maps, far too big for test resources, so a map is looked for in
 * `OSMAND_OBF_DIRECTORY`, by the name the case gives and by that name without its `_2` download
 * suffix, and a case whose map is not there is skipped. Without the variable nothing runs.
 */
class AlternativeRoutesTest {

	/** no route may drive the same piece of road twice beyond this (u-turn manoeuvres) */
	private val maxRetraced = 100.0

	/** a larger gap between consecutive points means a shortcut was not expanded into roads */
	private val maxPointGap = 4000.0

	/** an alternative must offer, and must avoid, at least this share of the main route */
	private val minDistinctRel = 0.2

	/** ... and never less than this, however short the route is */
	private val minDistinctFloor = 300.0

	private class ExpectedVia(val name: String, val latitude: Double, val longitude: Double, val toleranceMeters: Double)

	private class Case(
		val testName: String,
		val map: String,
		val vehicle: String,
		val startPoint: KLatLon,
		val endPoint: KLatLon,
		val minAlternatives: Int,
		val maxAlternatives: Int,
		val ignore: Boolean,
		val expectedVia: List<ExpectedVia>,
		val unexpectedVia: List<ExpectedVia>
	)

	private class Route {
		val points = ArrayList<IntArray>() // x31, y31
		var cost = 0.0
	}

	@Test
	fun testAlternatives() {
		val directory = testEnvironment("OSMAND_OBF_DIRECTORY")
		val failures = ArrayList<String>()
		var run = 0
		var skipped = 0
		for (case in cases()) {
			if (case.ignore) {
				continue
			}
			val map = findMap(directory, case.map)
			if (map == null) {
				skipped++
				continue
			}
			run++
			try {
				check(case, map)
			} catch (e: Throwable) {
				failures.add(case.testName + ": " + e.message)
			}
		}
		println("AlternativeRoutesTest on ${testPlatformName()}: $run cases, ${failures.size} failed, $skipped skipped for a missing map")
		assertTrue(failures.isEmpty(), failures.joinToString("\n"))
	}

	private fun findMap(directory: String?, name: String): String? {
		if (directory == null) {
			return null
		}
		val candidates = listOf(name, name.replace("_2.obf", ".obf"))
		for (c in candidates) {
			val path = directory.toPath() / c
			if (FileSystem.SYSTEM.exists(path)) {
				return path.toString()
			}
		}
		return null
	}

	private fun check(case: Case, map: String) {
		val routes = calculate(case, map)
		val main = routes[0]
		val found = routes.size - 1
		assertTrue(found >= case.minAlternatives, "expected at least ${case.minAlternatives} alternative(s), found $found")
		assertTrue(found <= case.maxAlternatives, "expected at most ${case.maxAlternatives} alternative(s), found $found")
		assertSaneAlternatives(routes, main)
		for (via in case.expectedVia) {
			val d = closestAlternative(routes, via)
			assertTrue(d <= via.toleranceMeters, "no alternative passes through ${via.name}, closest one is ${d.roundToLong()} m away")
		}
		for (via in case.unexpectedVia) {
			val d = closestAlternative(routes, via)
			assertTrue(d > via.toleranceMeters, "an alternative passes through ${via.name} (${d.roundToLong()} m)")
		}
	}

	private fun assertSaneAlternatives(routes: List<Route>, main: Route) {
		val mainRoads = roads(main)
		val mainLength = length(mainRoads)
		val limits = HHRoutingConfig()
		val maxCost = main.cost * (1 + limits.ALT_STRETCH) + limits.ALT_STRETCH_ABS + 1
		for (i in 1 until routes.size) {
			val alt = routes[i]
			val id = "alternative $i"
			val altRoads = roads(alt)
			val shared = shared(altRoads, mainRoads)

			assertTrue(alt.cost <= maxCost, "$id costs ${(100 * (alt.cost / main.cost - 1)).roundToLong()}% more than the main route")
			val minDistinct = max(minDistinctFloor, minDistinctRel * mainLength)
			assertTrue(length(altRoads) - shared >= minDistinct, "$id has only ${(length(altRoads) - shared).roundToLong()} m of roads of its own")
			assertTrue(
				mainLength - shared >= minDistinct / 2,
				"$id avoids only ${(mainLength - shared).roundToLong()} m of the main route, so it is the main route with a detour"
			)
			assertTrue(retraced(alt) <= maxRetraced, "$id drives ${retraced(alt).roundToLong()} m of its own roads twice")
			assertTrue(maxGap(alt) <= maxPointGap, "$id has a ${maxGap(alt).roundToLong()} m gap, a shortcut was not expanded into roads")
			assertTrue(distance(alt.points[0], main.points[0]) <= 200, "$id does not start where the main route starts")
			assertTrue(
				distance(alt.points[alt.points.size - 1], main.points[main.points.size - 1]) <= 200,
				"$id does not end where the main route ends"
			)
		}
	}

	private fun closestAlternative(routes: List<Route>, via: ExpectedVia): Double {
		val vx = KMapUtils.get31TileNumberX(via.longitude)
		val vy = KMapUtils.get31TileNumberY(via.latitude)
		var best = Double.MAX_VALUE
		for (i in 1 until routes.size) {
			for (p in routes[i].points) {
				best = min(best, KMapUtils.squareRootDist31(p[0], p[1], vx, vy))
			}
		}
		return best
	}

	private fun calculate(case: Case, map: String): List<Route> {
		val reader = BinaryMapIndexReader(map)
		try {
			RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false
			RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false
			val router = RoutePlannerFrontEnd()
			val config = RoutingTestFixtures.defaultBuilder().build(case.vehicle, RoutingTestFixtures.memoryLimits(), LinkedHashMap())
			val ctx = router.buildRoutingContext(config, listOf(reader), RouteCalculationMode.NORMAL)
			ctx.calculationProgress = RouteCalculationProgress()

			val hh = HHRoutePlanner.prepareDefaultRoutingConfig(null)
			hh.calcAlternative()
			router.setUseOnlyHHRouting(true).setHHRoutingConfig(hh)

			val res = router.searchRoute(ctx, case.startPoint, case.endPoint, ArrayList())
			assertTrue(res.getError() == null, res.getError() ?: "")
			assertTrue(res.getList().isNotEmpty(), "main route is empty")

			val routes = ArrayList<Route>()
			routes.add(toRoute(res.getList()))
			for (alt in res.getAlternatives()) {
				routes.add(toRoute(alt))
			}
			return routes
		} finally {
			reader.close()
		}
	}

	private fun toRoute(segments: List<RouteSegmentResult>): Route {
		val r = Route()
		for (s in segments) {
			r.cost += s.getRoutingTime()
			val o = s.getObject()
			var i = s.getStartPointIndex()
			val end = s.getEndPointIndex()
			val step = if (i <= end) 1 else -1
			while (true) {
				val p = intArrayOf(o.getPoint31XTile(i), o.getPoint31YTile(i))
				val last = if (r.points.isEmpty()) null else r.points[r.points.size - 1]
				if (last == null || last[0] != p[0] || last[1] != p[1]) {
					r.points.add(p)
				}
				if (i == end) {
					break
				}
				i += step
			}
		}
		return r
	}

	/** road piece -> length, so that two routes can be compared on the roads themselves */
	private fun roads(r: Route): Map<Long, Double> {
		val m = HashMap<Long, Double>()
		for (i in 1 until r.points.size) {
			val key = key(r.points[i - 1], r.points[i])
			val prev = m[key]
			m[key] = (prev ?: 0.0) + distance(r.points[i - 1], r.points[i])
		}
		return m
	}

	private fun key(a: IntArray, b: IntArray): Long {
		val p = (a[0].toLong() shl 32) or (a[1].toLong() and 0xffffffffL)
		val q = (b[0].toLong() shl 32) or (b[1].toLong() and 0xffffffffL)
		return min(p, q) * 1000003L + max(p, q)
	}

	private fun distance(a: IntArray, b: IntArray): Double {
		return KMapUtils.squareRootDist31(a[0], a[1], b[0], b[1])
	}

	private fun length(roads: Map<Long, Double>): Double {
		var d = 0.0
		for (v in roads.values) {
			d += v
		}
		return d
	}

	private fun shared(a: Map<Long, Double>, b: Map<Long, Double>): Double {
		var common = 0.0
		for (e in a.entries) {
			val v = b[e.key]
			if (v != null) {
				common += min(v, e.value)
			}
		}
		return common
	}

	private fun retraced(r: Route): Double {
		val seen = HashSet<Long>()
		var dup = 0.0
		for (i in 1 until r.points.size) {
			val k = key(r.points[i - 1], r.points[i])
			if (!seen.add(k)) {
				dup += distance(r.points[i - 1], r.points[i])
			}
		}
		return dup
	}

	private fun maxGap(r: Route): Double {
		var mx = 0.0
		for (i in 1 until r.points.size) {
			mx = max(mx, distance(r.points[i - 1], r.points[i]))
		}
		return mx
	}

	private fun cases(): List<Case> {
		val text = FileSystem.SYSTEM.read(RoutingTestFixtures.resourcesDir() / "alternatives" / "test_alternative_routes.json") { readUtf8() }
		val json = Json { ignoreUnknownKeys = true }
		val cases = ArrayList<Case>()
		for (element in json.parseToJsonElement(text).jsonArray) {
			val o = element.jsonObject
			cases.add(
				Case(
					testName = o.string("testName") ?: "",
					map = o.string("map") ?: "",
					vehicle = o.string("vehicle") ?: "car",
					startPoint = o.point("startPoint"),
					endPoint = o.point("endPoint"),
					minAlternatives = o.string("minAlternatives")?.toInt() ?: 1,
					maxAlternatives = o.string("maxAlternatives")?.toInt() ?: Int.MAX_VALUE,
					ignore = o.string("ignore") == "true",
					expectedVia = o.vias("expectedVia"),
					unexpectedVia = o.vias("unexpectedVia")
				)
			)
		}
		return cases
	}

	private fun JsonObject.string(key: String): String? {
		val v = this[key] ?: return null
		if (v is JsonNull) {
			return null
		}
		return (v as JsonPrimitive).content
	}

	private fun JsonObject.point(key: String): KLatLon {
		val o = this[key]!!.jsonObject
		return KLatLon(o.string("latitude")!!.toDouble(), o.string("longitude")!!.toDouble())
	}

	private fun JsonObject.vias(key: String): List<ExpectedVia> {
		val v = this[key] ?: return emptyList()
		if (v is JsonNull) {
			return emptyList()
		}
		val vias = ArrayList<ExpectedVia>()
		for (element in v.jsonArray) {
			val o = element.jsonObject
			vias.add(
				ExpectedVia(
					o.string("name") ?: "", o.string("latitude")!!.toDouble(), o.string("longitude")!!.toDouble(),
					o.string("toleranceMeters")?.toDouble() ?: 500.0
				)
			)
		}
		return vias
	}
}
