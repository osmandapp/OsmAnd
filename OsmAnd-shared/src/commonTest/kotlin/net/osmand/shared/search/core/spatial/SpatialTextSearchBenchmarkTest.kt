package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.KLatLon
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testAppCall
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.core.SearchCoreFactoryTest
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.dumpUnhexList
import net.osmand.shared.util.openJavaDump
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy of the spatial text search, [SpatialTextSearch.searchAPI], takes wherever it runs, over the
 * queries `SpatialTextSearchCompatTest` in OsmAnd-java wrote to `../OsmAnd-java/build/spatial-search-java.txt`: those
 * of the default settings and of the old intersections, over the maps and from the centers of the dump, with the names
 * of the languages it added, by one [SpatialTextSearch] as the app keeps one, best of [ROUNDS] rounds after one to warm
 * up. `SpatialTextSearchBenchmarkTest` in OsmAnd-java times java over the same.
 *
 * **Not part of a normal run**: it only does anything when `OSMAND_SPATIAL_SEARCH_BENCHMARK` is set. On Kotlin/Native
 * only the **release** binary says anything about speed:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * cd OsmAnd-shared && SIMCTL_CHILD_OSMAND_SPATIAL_SEARCH_BENCHMARK=1 SIMCTL_CHILD_OSMAND_SPATIAL_SEARCH_JAVA_DUMP=<dump> \
 *   SIMCTL_CHILD_OSMAND_JAVA_TEST_RESOURCES=<OsmAnd-java/src/test/resources> \
 *   xcrun simctl spawn --standalone <udid> build/bin/iosSimulatorArm64/releaseTest/test.kexe \
 *   --ktest_filter='net.osmand.shared.search.core.spatial.SpatialTextSearchBenchmarkTest.*'
 * ```
 */
class SpatialTextSearchBenchmarkTest {

	private class Run(val paths: List<String>, val center: KLatLon, val languages: List<String>) {
		val variants = ArrayList<Int>()
		val phrases = ArrayList<String>()
	}

	@Test
	fun search() {
		if (testEnvironment("OSMAND_SPATIAL_SEARCH_BENCHMARK") == null) {
			println("SpatialTextSearchBenchmarkTest: set OSMAND_SPATIAL_SEARCH_BENCHMARK to run")
			return
		}
		val source = openJavaDump(
			"SpatialTextSearchBenchmarkTest", "OSMAND_SPATIAL_SEARCH_JAVA_DUMP", "spatial-search-java.txt",
			"SpatialTextSearchCompatTest"
		) ?: return
		var rulesDir = ""
		val poiPhrases = HashMap<String, String>()
		val langPhrases = HashMap<String, MutableMap<String, String>>()
		val runs = ArrayList<Run>()
		var languages = ArrayList<String>()
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				when (f[0]) {
					"D" -> rulesDir = dumpUnhex(f[1])
					"X" -> poiPhrases[dumpUnhex(f[1])] = dumpUnhex(f[2])
					"Y" -> langPhrases.getOrPut(f[1]) { HashMap() }[dumpUnhex(f[2])] = dumpUnhex(f[3])
					"L" -> languages.add(f[1])
					"C" -> {
						val center = KLatLon(Double.fromBits(f[2].toULong(16).toLong()), Double.fromBits(f[3].toULong(16).toLong()))
						runs.add(Run(dumpUnhexList(f[1]), center, languages))
						languages = ArrayList()
					}
					"Q" -> {
						val variant = f[1].toInt()
						if (variant == 0 || variant == 3) {
							runs.last().variants.add(variant)
							runs.last().phrases.add(dumpUnhex(f[2]))
						}
					}
				}
			}
		} finally {
			source.close()
		}
		// the default of the tests before, if there is one: this test names the poi types its own way
		val defaultTypes = runCatching { MapPoiTypes.getDefault() }.getOrNull()
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		MapPoiTypes.setDefault(types)
		types.setPoiTranslator(SearchCoreFactoryTest.Translator(poiPhrases, langPhrases))
		try {
			val poiSearch = SpatialPoiSearch(types)
			val search = SpatialTextSearch(rulesDir)
			val queries = IntArray(2)
			val best = doubleArrayOf(Double.MAX_VALUE, Double.MAX_VALUE)
			for (round in 0..ROUNDS) {
				val ms = DoubleArray(2)
				for (run in runs) {
					for (lang in run.languages) {
						poiSearch.addLanguage(lang)
					}
					val readers = run.paths.map { BinaryMapIndexReader(it) }
					try {
						for (i in run.phrases.indices) {
							val v = run.variants[i]
							val ctx = SpatialSearchContext(SpatialTextSearchTest.settings(v), readers, poiSearch, run.center)
							ctx.stats.printLogs = false
							val start = TimeSource.Monotonic.markNow()
							testAppCall { search.searchAPI(run.phrases[i], ctx) }
							ms[if (v == 0) 0 else 1] += start.elapsedNow().inWholeMicroseconds / 1000.0
							if (round == 0) {
								queries[if (v == 0) 0 else 1]++
							}
						}
					} finally {
						readers.forEach { it.close() }
					}
				}
				if (round > 0) {
					best[0] = minOf(best[0], ms[0])
					best[1] = minOf(best[1], ms[1])
				}
			}
			println(
				"SpatialTextSearchBenchmarkTest on ${testPlatformName()}: ${queries[0]} queries by default, " +
						"${(best[0] * 10).toLong() / 10.0} ms; ${queries[1]} by the old intersections, " +
						"${(best[1] * 10).toLong() / 10.0} ms; best of $ROUNDS rounds"
			)
		} finally {
			defaultTypes?.let { MapPoiTypes.setDefault(it) }
		}
	}

	companion object {
		private const val ROUNDS = 5
	}
}
