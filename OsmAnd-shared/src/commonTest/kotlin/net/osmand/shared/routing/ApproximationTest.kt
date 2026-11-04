package net.osmand.shared.routing

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.io.KFile
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import okio.FileSystem
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The approximation cases of OsmAnd-java's `ApproximationTest`, run through the shared copy: each
 * track of `approximation/test.json` is attached to the roads of its own small obf, the routing-based
 * and the geometry-based way, with the profiles and `minPointApproximation` values the case lists,
 * and the result has to be the length the case expects, contain the roads it expects, avoid the
 * ones it forbids, and turn where and how it says.
 *
 * The failures are reported together at the end, since this runs wherever the shared code runs
 * and has no parameterized runner to lean on.
 */
class ApproximationTest {

	@Serializable
	private class Entry(
		val gpxFile: String,
		val obfFile: String,
		val ignore: Boolean = false,
		val types: List<String>? = null,
		val profiles: List<String>? = null,
		val minPointApproximation: List<Int>? = null,
		val expectedDistMin: Double = 0.0,
		val expectedDistMax: Double = 0.0,
		val expectedWays: Map<Long, Boolean>? = null,
		val expectedTurns: Map<Long, String>? = null,
		val expectedTurnsAngle: Map<Long, Int>? = null
	)

	private val json = Json { ignoreUnknownKeys = true; isLenient = true }

	@Test
	fun testApproximation() {
		RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false
		val text = FileSystem.SYSTEM.read(RoutingTestFixtures.resourcesDir() / "approximation/test.json") { readUtf8() }
		val failures = ArrayList<String>()
		var cases = 0
		var segments = 0
		var metres = 0.0
		for (entry in json.decodeFromString<List<Entry>>(text)) {
			if (entry.ignore) {
				continue
			}
			val gpx = GpxUtilities.loadGpxFile(KFile(RoutingTestFixtures.resource("approximation/" + entry.gpxFile)))
			val waypoints = gpx.tracks[0].segments[0].points
			val readers = listOf(BinaryMapIndexReader(RoutingTestFixtures.resource("approximation/" + entry.obfFile)))
			try {
				for (type in entry.types ?: DEFAULT_TYPES) {
					for (profile in entry.profiles ?: DEFAULT_PROFILES) {
						for (minPointApproximation in entry.minPointApproximation ?: DEFAULT_MIN_POINT_APPROXIMATION) {
							cases++
							val tag = "${entry.gpxFile} $type $profile [$minPointApproximation]"
							try {
								val (count, distance) = approximateAndCheck(entry, tag, type, profile, minPointApproximation, waypoints.map { KLatLon(it.lat, it.lon) }, readers)
								segments += count
								metres += distance
							} catch (e: Throwable) {
								failures.add("$tag: ${e.message}")
							}
						}
					}
				}
			} finally {
				readers.forEach { it.close() }
			}
		}
		// the totals are the same wherever this runs; a platform that attaches a track differently shows here
		println("ApproximationTest on ${testPlatformName()}: $cases cases, ${failures.size} failed, $segments segments, ${metres.toLong()} m")
		assertTrue(cases > 80, "cases run: $cases")
		assertTrue(failures.isEmpty(), failures.joinToString("\n"))
	}

	/** Runs one case and checks it; returns the number of segments and the length of the route. */
	private fun approximateAndCheck(
		entry: Entry, tag: String, type: String, profile: String, minPointApproximation: Int,
		locations: List<KLatLon>, readers: List<BinaryMapIndexReader>
	): Pair<Int, Double> {
		val router = RoutePlannerFrontEnd()
		if (type == "routing") {
			router.setUseGeometryBasedApproximation(false)
		} else if (type == "geometry") {
			GpxRouteApproximation.GPX_SEGMENT_ALGORITHM = GpxRouteApproximation.GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM
			router.setUseGeometryBasedApproximation(true)
		}
		val config = RoutingTestFixtures.defaultBuilder().build(profile, RoutingConfiguration.RoutingMemoryLimits(MEM_LIMIT, MEM_LIMIT), LinkedHashMap())
		config.routeCalculationTime = currentTimeMillis() // ENABLE_TIME_CONDITIONAL_ROUTING
		if (minPointApproximation > 0) {
			config.minPointApproximation = minPointApproximation.toFloat()
		}
		val ctx = router.buildRoutingContext(config, readers, RouteCalculationMode.NORMAL)
		val gctx = GpxRouteApproximation(ctx)
		val gpxPoints = router.generateGpxPoints(gctx, locations, null)
		val result = router.searchGpxRoute(gctx, gpxPoints, null, false).collectFinalPointsAsRoute()

		var distance = 0.0
		val waysInResult = HashSet<Long>()
		val turnsInResult = HashMap<Long, String>()
		val turnsAngleInResult = HashMap<Long, Int>()
		for (segment in result) {
			val osmId = segment.getObject().id / 64
			distance += calcSegmentDistance(segment)
			waysInResult.add(osmId)
			val turnType = segment.getTurnType()
			if (turnType != null && segment.getObject().id != -1L) {
				val lanes = turnType.lanes?.let { TurnType.lanesToString(it) }
				val turn = turnType.toXmlString()
				val turnLanesString = (if (turnType.isSkipToSpeak) "[MUTE] " else "") + turn + (if (!KAlgorithms.isEmpty(lanes)) ":$lanes" else "")
				turnsInResult[osmId] = turnLanesString
				turnsAngleInResult[osmId] = turnType.turnAngle.toInt()
			}
		}

		val expectedDistMin = if (entry.expectedDistMin == 0.0) Double.NEGATIVE_INFINITY else entry.expectedDistMin
		val expectedDistMax = if (entry.expectedDistMax == 0.0) Double.POSITIVE_INFINITY else entry.expectedDistMax
		assertTrue(
			distance > expectedDistMin && distance < expectedDistMax,
			"distance ($distance) is outside of min / max ($expectedDistMin / $expectedDistMax)"
		)
		entry.expectedWays?.forEach { (osmId, expected) ->
			assertTrue(waysInResult.contains(osmId) == expected, "expectedWays ($expected) failed for $osmId")
		}
		entry.expectedTurnsAngle?.forEach { (osmId, expected) ->
			assertTrue(turnsAngleInResult[osmId] == expected, "expectedTurnsAngle ($expected) failed for $osmId (${turnsAngleInResult[osmId]})")
		}
		entry.expectedTurns?.forEach { (osmId, expected) ->
			val got = turnsInResult[osmId]
			assertTrue(expected == got, "expectedTurns ($expected) failed for $osmId ($got)")
		}
		return Pair(result.size, distance)
	}

	private fun calcSegmentDistance(rr: RouteSegmentResult): Double {
		var distance = 0.0
		val road = rr.getObject()
		val plus = rr.getStartPointIndex() < rr.getEndPointIndex()
		var j = rr.getStartPointIndex()
		while (j != rr.getEndPointIndex()) {
			val next = if (plus) j + 1 else j - 1
			distance += KMapUtils.squareRootDist31(
				road.getPoint31XTile(j), road.getPoint31YTile(j), road.getPoint31XTile(next), road.getPoint31YTile(next)
			)
			j = next
		}
		return distance
	}

	companion object {
		private val DEFAULT_TYPES = listOf("routing", "geometry")
		private val DEFAULT_PROFILES = listOf("car")
		private val DEFAULT_MIN_POINT_APPROXIMATION = listOf(50)
		private const val MEM_LIMIT = RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT * 8 * 2 // ~ 4 GB, as the java test
	}
}
