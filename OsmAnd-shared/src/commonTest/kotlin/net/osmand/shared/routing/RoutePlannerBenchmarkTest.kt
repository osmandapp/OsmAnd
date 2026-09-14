package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared planner takes on real maps, for the question the port is asked: whether it
 * is fast enough on Kotlin/Native to replace the C++ router on iOS.
 *
 * Each route is calculated the way the app asks for it - a car, the base map first and then the
 * detailed search along it - and the wall time, the segments settled and the tiles loaded are
 * printed. The same routes, the same maps and the same profile run through the java planner, the
 * C++ router and this planner on the jvm in `RoutePlannerBenchmarkTest` in OsmAnd-java, so the
 * four columns can be put side by side. A second table takes the same routes as tracks and
 * attaches them back to the roads, the routing-based and the geometry-based way.
 *
 * The maps are real ones and are not in the repository; they are looked for in [OBF_DIRECTORIES]
 * and in `OSMAND_OBF_DIRECTORY`. **Not part of a normal run**: it only does anything when
 * `OSMAND_ROUTING_BENCHMARK` is set, since a Kotlin/Native test binary cannot be told to run an
 * ignored test. On the jvm, from the repository root:
 * ```
 * OSMAND_ROUTING_BENCHMARK=1 ./gradlew --no-daemon :OsmAnd-shared:jvmTest --tests "*RoutePlannerBenchmarkTest" -i
 * ```
 * On Kotlin/Native only the **release** binary says anything about speed:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * cd OsmAnd-shared && SIMCTL_CHILD_OSMAND_ROUTING_BENCHMARK=1 xcrun simctl spawn --standalone <udid> \
 *   build/bin/iosSimulatorArm64/releaseTest/test.kexe --ktest_filter='net.osmand.shared.routing.RoutePlannerBenchmarkTest.*'
 * ```
 */
class RoutePlannerBenchmarkTest {

	class Route(val name: String, val maps: List<String>, val start: KLatLon, val end: KLatLon)

	@Test
	fun benchmarkRoutes() {
		if (testEnvironment("OSMAND_ROUTING_BENCHMARK") == null) {
			println("RoutePlannerBenchmarkTest: set OSMAND_ROUTING_BENCHMARK to run")
			return
		}
		RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false
		println("")
		println("### route planner, shared copy on ${testPlatformName()}, A* and HH, $WARMUP_ROUNDS warmup / $MEASURED_ROUNDS measured, best time")
		println("")
		println("  ${"route".padEnd(28)} ${"by".padEnd(10)} ${"ms".padStart(8)} ${"segments".padStart(9)} ${"km".padStart(8)} " +
				"${"routing s".padStart(10)} ${"visited".padStart(9)} ${"tiles".padStart(7)}")
		for (route in ROUTES) {
			val paths = route.maps.map { find(it) }
			if (paths.any { it == null }) {
				println("  ${route.name.padEnd(28)} map not found: ${route.maps.filterIndexed { i, _ -> paths[i] == null }}")
				continue
			}
			for (hh in listOf(false, true)) {
				benchmark(route, paths.map { it!! }, hh)
			}
		}
		println("")
		// the same routes as tracks - the A* route's road points thinned to the spacing of a recorded
		// track - attached back to the roads, the routing-based way and the geometry-based way
		println("### gpx approximation, shared copy on ${testPlatformName()}, routing-based and geometry-based, $WARMUP_ROUNDS warmup / $MEASURED_ROUNDS measured, best time")
		println("")
		println("  ${"route".padEnd(28)} ${"by".padEnd(14)} ${"ms".padStart(8)} ${"segments".padStart(9)} ${"km".padStart(8)} " +
				"${"points".padStart(8)} ${"visited".padStart(9)} ${"tiles".padStart(7)}")
		for (route in ROUTES) {
			val paths = route.maps.map { find(it) }
			if (paths.any { it == null }) {
				println("  ${route.name.padEnd(28)} map not found: ${route.maps.filterIndexed { i, _ -> paths[i] == null }}")
				continue
			}
			val readers = paths.map { BinaryMapIndexReader(it!!) }
			try {
				val track = track(route, readers)
				for (geometry in listOf(false, true)) {
					approximation(route, readers, track, geometry)
				}
			} finally {
				readers.forEach { it.close() }
			}
		}
		println("")
	}

	/**
	 * The route's road points thinned to the spacing of a track recorded at driving speed, a point a
	 * second, and moved a couple of metres off the road: a recording never lies on the road's own nodes,
	 * and on a node several roads are the same distance away, where the planners pick by the order they
	 * met the roads in - java and the copy iterate their hash tables differently, so the route they attach
	 * would differ by a road here and there for a reason that has nothing to do with the approximation.
	 */
	private fun track(route: Route, readers: List<BinaryMapIndexReader>): List<KLatLon> {
		val config = RoutingTestFixtures.defaultBuilder().build("car", limits(), LinkedHashMap())
		val fe = RoutePlannerFrontEnd()
		val ctx = fe.buildRoutingContext(config, readers)
		val res = fe.searchRoute(ctx, route.start, route.end, null)
		val track = ArrayList<KLatLon>()
		var last: KLatLon? = null
		for (s in res.detailed) {
			val step = if (s.getStartPointIndex() < s.getEndPointIndex()) 1 else -1
			var i = s.getStartPointIndex()
			while (true) {
				val road = s.getPoint(i)
				val p = KLatLon(road.latitude + TRACK_OFFSET_LAT, road.longitude + TRACK_OFFSET_LON)
				last = p
				if (track.isEmpty() || KMapUtils.getDistance(track[track.size - 1], p) >= TRACK_SPACING_M) {
					track.add(p)
				}
				if (i == s.getEndPointIndex()) {
					break
				}
				i += step
			}
		}
		if (last != null && track[track.size - 1] != last) {
			track.add(last)
		}
		return track
	}

	/** One row: the track attached to the roads, by the A* searches between its points or by the walk along the road graph. */
	private fun approximation(route: Route, readers: List<BinaryMapIndexReader>, track: List<KLatLon>, geometry: Boolean) {
		var best = Double.MAX_VALUE
		var line = ""
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val mark = TimeSource.Monotonic.markNow()
			val config = RoutingTestFixtures.defaultBuilder().build("car", limits(), LinkedHashMap())
			val fe = RoutePlannerFrontEnd()
			fe.setUseGeometryBasedApproximation(geometry)
			val ctx = fe.buildRoutingContext(config, readers, RouteCalculationMode.NORMAL)
			val gctx = GpxRouteApproximation(ctx)
			val points = fe.generateGpxPoints(gctx, track, null)
			val res = fe.searchGpxRoute(gctx, points, null, false)
			val ms = mark.elapsedNow().inWholeMicroseconds / 1000.0
			if (round >= WARMUP_ROUNDS && ms < best) {
				best = ms
				var km = 0.0
				for (s in res.fullRoute) {
					km += s.getDistance()
				}
				val progress = ctx.calculationProgress!!
				line = "${res.fullRoute.size.toString().padStart(9)} ${(km / 1000).format1().padStart(8)} " +
						"${points.size.toString().padStart(8)} ${progress.visitedSegments.toString().padStart(9)} " +
						progress.loadedTiles.toString().padStart(7)
			}
		}
		println("  ${route.name.padEnd(28)} ${("shared gpx" + if (geometry) " geo" else "").padEnd(14)} ${best.format1().padStart(8)} $line")
	}

	/** One row: the route by the A* planner, or over the hub graph as the apps calculate it by default (only HH, so a fallback shows as an error). */
	private fun benchmark(route: Route, paths: List<String>, hh: Boolean) {
		val readers = paths.map { BinaryMapIndexReader(it) }
		try {
			var best = Double.MAX_VALUE
			var line = ""
			repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
				val mark = TimeSource.Monotonic.markNow()
				val config = RoutingTestFixtures.defaultBuilder().build("car", limits(), LinkedHashMap())
				val fe = RoutePlannerFrontEnd()
				RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false
				if (hh) {
					fe.setDefaultHHRoutingConfig()
					fe.setUseOnlyHHRouting(true)
				}
				val ctx = fe.buildRoutingContext(config, readers)
				val res = fe.searchRoute(ctx, route.start, route.end, null)
				val ms = mark.elapsedNow().inWholeMicroseconds / 1000.0
				if (round >= WARMUP_ROUNDS && ms < best) {
					best = ms
					val error = res.getError()
					line = if (error != null) {
						"error: $error"
					} else {
						var km = 0.0
						for (s in res.detailed) {
							km += s.getDistance()
						}
						val progress = ctx.calculationProgress!!
						"${res.detailed.size.toString().padStart(9)} ${(km / 1000).format1().padStart(8)} " +
								"${ctx.routingTime.toDouble().format1().padStart(10)} ${progress.visitedSegments.toString().padStart(9)} " +
								progress.loadedTiles.toString().padStart(7)
					}
				}
			}
			println("  ${route.name.padEnd(28)} ${(if (hh) "shared hh" else "shared").padEnd(10)} ${best.format1().padStart(8)} $line")
		} finally {
			readers.forEach { it.close() }
		}
	}

	/** The 256 MB the C++ router is given, for the java planner too, so the tile cache does not thrash on the long routes. */
	private fun limits() = RoutingConfiguration.RoutingMemoryLimits(
		RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT, RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT
	)

	private fun Double.format1(): String {
		val rounded = kotlin.math.round(this * 10) / 10
		val s = rounded.toString()
		return if (s.endsWith(".0")) s else s
	}

	private fun find(name: String): String? {
		val directories = ArrayList(OBF_DIRECTORIES)
		testEnvironment("OSMAND_OBF_DIRECTORY")?.let { directories.add(0, it) }
		for (directory in directories) {
			val path = (directory + "/" + name).toPath()
			if (FileSystem.SYSTEM.exists(path)) {
				return path.toString()
			}
		}
		return null
	}

	companion object {
		const val WARMUP_ROUNDS = 1
		const val MEASURED_ROUNDS = 2
		const val TRACK_SPACING_M = 15.0 // a point a second at 55 km/h
		const val TRACK_OFFSET_LAT = 0.00002 // ~2 m north
		const val TRACK_OFFSET_LON = 0.00003 // ~2 m east

		val OBF_DIRECTORIES = listOf(
			"/Users/crimean/tmp/maps",
			"/Users/crimean/Library/Developer/CoreSimulator/Devices/2B4A49F7-4769-4207-93AD-2DFF3B315735/data/Containers/Data/Application/1D97C071-99BF-41BC-BC02-A1CF7EEF3BCD/Documents/Resources",
		)

		private const val NOORD_HOLLAND = "Netherlands_noord-holland_europe.obf"
		private const val BAVARIA = "Germany_bayern_upper-bavaria_europe.obf"
		private const val LOWER_AUSTRIA = "Austria_lower-austria_europe.obf"

		/** The same list as in OsmAnd-java's `RoutePlannerBenchmarkTest`; keep the two together. */
		val ROUTES = listOf(
			Route("amsterdam schiphol 17 km", listOf(NOORD_HOLLAND), KLatLon(52.3791, 4.9003), KLatLon(52.3105, 4.7683)),
			Route("amsterdam haarlem 20 km", listOf(NOORD_HOLLAND), KLatLon(52.3791, 4.9003), KLatLon(52.3874, 4.6462)),
			Route("amsterdam den helder 80 km", listOf(NOORD_HOLLAND), KLatLon(52.3791, 4.9003), KLatLon(52.9563, 4.7606)),
			Route("munich airport 35 km", listOf(BAVARIA), KLatLon(48.1374, 11.5755), KLatLon(48.3538, 11.7861)),
			Route("munich rosenheim 65 km", listOf(BAVARIA), KLatLon(48.1374, 11.5755), KLatLon(47.8561, 12.1289)),
			Route("munich garmisch 90 km", listOf(BAVARIA), KLatLon(48.1374, 11.5755), KLatLon(47.4917, 11.0954)),
			Route("vienna schwechat 20 km", listOf(LOWER_AUSTRIA), KLatLon(48.2082, 16.3738), KLatLon(48.1103, 16.5697)),
			Route("st poelten krems 30 km", listOf(LOWER_AUSTRIA), KLatLon(48.2047, 15.6256), KLatLon(48.4103, 15.6136)),
			Route("st poelten wr neustadt 70 km", listOf(LOWER_AUSTRIA), KLatLon(48.2047, 15.6256), KLatLon(47.8100, 16.2450)),
		)
	}
}
