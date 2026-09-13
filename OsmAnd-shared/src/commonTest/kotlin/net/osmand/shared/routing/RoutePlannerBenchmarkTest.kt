package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.KLatLon
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
 * four columns can be put side by side.
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
		println("### route planner, shared copy on ${testPlatformName()}, $WARMUP_ROUNDS warmup / $MEASURED_ROUNDS measured, best time")
		println("")
		println("  ${"route".padEnd(28)} ${"ms".padStart(8)} ${"segments".padStart(9)} ${"km".padStart(8)} " +
				"${"routing s".padStart(10)} ${"visited".padStart(9)} ${"tiles".padStart(7)}")
		for (route in ROUTES) {
			val paths = route.maps.map { find(it) }
			if (paths.any { it == null }) {
				println("  ${route.name.padEnd(28)} map not found: ${route.maps.filterIndexed { i, _ -> paths[i] == null }}")
				continue
			}
			val readers = paths.map { BinaryMapIndexReader(it!!) }
			try {
				var best = Double.MAX_VALUE
				var line = ""
				repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
					val mark = TimeSource.Monotonic.markNow()
					val config = RoutingTestFixtures.defaultBuilder().build("car", limits(), LinkedHashMap())
					val fe = RoutePlannerFrontEnd()
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
				println("  ${route.name.padEnd(28)} ${best.format1().padStart(8)} $line")
			} finally {
				readers.forEach { it.close() }
			}
		}
		println("")
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

		val OBF_DIRECTORIES = listOf(
			"/Users/crimean/Library/Developer/CoreSimulator/Devices/04D37C3C-5D03-45A0-8B2D-69512DE2391C/data/Containers/Data/Application/DA50C80C-B421-4B6C-9009-AE8109DA6E6E/Documents/Resources",
			"/Users/crimean/Library/Developer/CoreSimulator/Devices/2B4A49F7-4769-4207-93AD-2DFF3B315735/data/Containers/Data/Application/1D97C071-99BF-41BC-BC02-A1CF7EEF3BCD/Documents/Resources",
		)

		private const val KYIV = "Ukraine_kyiv-city_europe.obf"
		private const val MOSCOW = "Russia_moscow_asia.obf"
		private const val BAVARIA = "Germany_bayern_upper-bavaria_europe.obf"

		/** The same list as in OsmAnd-java's `RoutePlannerBenchmarkTest`; keep the two together. */
		val ROUTES = listOf(
			Route("kyiv centre 3 km", listOf(KYIV), KLatLon(50.4501, 30.5234), KLatLon(50.4400, 30.4890)),
			Route("kyiv airport 35 km", listOf(KYIV), KLatLon(50.4501, 30.5234), KLatLon(50.3450, 30.8947)),
			Route("moscow sheremetyevo 35 km", listOf(MOSCOW), KLatLon(55.7539, 37.6208), KLatLon(55.9726, 37.4146)),
			Route("moscow zelenograd 40 km", listOf(MOSCOW), KLatLon(55.9825, 37.1814), KLatLon(55.7539, 37.6208)),
			Route("moscow vnukovo-svo 50 km", listOf(MOSCOW), KLatLon(55.5915, 37.2615), KLatLon(55.9726, 37.4146)),
			Route("munich rosenheim 65 km", listOf(BAVARIA), KLatLon(48.1374, 11.5755), KLatLon(47.8561, 12.1289)),
			Route("munich garmisch 90 km", listOf(BAVARIA), KLatLon(48.1374, 11.5755), KLatLon(47.4917, 11.0954)),
			Route("munich regensburg 120 km", listOf(BAVARIA), KLatLon(48.1374, 11.5755), KLatLon(49.0134, 12.1016)),
		)
	}
}
