package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.KLatLon
import net.osmand.shared.map.OsmandRegions
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testAppCall
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testHeapAfterGc
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.SearchUICore
import net.osmand.shared.search.core.SearchCoreFactoryTest
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.dumpUnhexList
import net.osmand.shared.util.openJavaDump
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the spatial text search of the shared copy takes as the search of the app runs it,
 * [SearchUICore.immediateSearch] of `init(true)`, wherever it runs, over the queries `SpatialTextSearchAPICompatTest`
 * in OsmAnd-java wrote to `../OsmAnd-java/build/spatial-search-api-java.txt`: complete and still being typed, from the
 * centers of the dump over all its maps, by one core as the app keeps one, best of [ROUNDS] rounds after one to warm
 * up. `SpatialTextSearchAPIBenchmarkTest` in OsmAnd-java times java over the same.
 *
 * **Not part of a normal run**: it only does anything when `OSMAND_SPATIAL_SEARCH_BENCHMARK` is set. On Kotlin/Native
 * only the **release** binary says anything about speed, see `SpatialTextSearchBenchmarkTest`.
 */
class SpatialTextSearchAPIBenchmarkTest {

	private class Run(val center: KLatLon) {
		val texts = ArrayList<String>()
	}

	@Test
	fun search() {
		if (testEnvironment("OSMAND_SPATIAL_SEARCH_BENCHMARK") == null) {
			println("SpatialTextSearchAPIBenchmarkTest: set OSMAND_SPATIAL_SEARCH_BENCHMARK to run")
			return
		}
		val source = openJavaDump(
			"SpatialTextSearchAPIBenchmarkTest", "OSMAND_SPATIAL_SEARCH_API_JAVA_DUMP", "spatial-search-api-java.txt",
			"SpatialTextSearchAPICompatTest"
		) ?: return
		var rulesDir = ""
		var regionsPath = ""
		var maps = emptyList<String>()
		val poiPhrases = HashMap<String, String>()
		val langPhrases = HashMap<String, MutableMap<String, String>>()
		val runs = ArrayList<Run>()
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				when (f[0]) {
					"D" -> rulesDir = dumpUnhex(f[1])
					"G" -> regionsPath = dumpUnhex(f[1])
					"M" -> maps = dumpUnhexList(f[1])
					"X" -> poiPhrases[dumpUnhex(f[1])] = dumpUnhex(f[2])
					"Y" -> langPhrases.getOrPut(f[1]) { HashMap() }[dumpUnhex(f[2])] = dumpUnhex(f[3])
					"C" -> runs.add(Run(KLatLon(Double.fromBits(f[1].toULong(16).toLong()), Double.fromBits(f[2].toULong(16).toLong()))))
					// the queries of the English search, by one core as the app keeps one
					"Q" -> if (f[2] == "en") runs.last().texts.add(dumpUnhex(f[1]))
				}
			}
		} finally {
			source.close()
		}
		// the words of the regions in the common words of a phrase
		SearchPhraseTest.setUp
		// the default of the tests before: this test names the poi types its own way
		val defaultTypes = MapPoiTypes.getDefault()
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		MapPoiTypes.setDefault(types)
		types.setPoiTranslator(SearchCoreFactoryTest.Translator(poiPhrases, langPhrases))
		val readers = maps.map { BinaryMapIndexReader(it) }
		val regions = OsmandRegions(regionsPath)
		try {
			val core = SearchUICore(types, "en", false)
			core.init(true, rulesDir)
			core.getSearchSettings().setOfflineIndexes(readers)
			core.getSearchSettings().setRegions(regions)
			val queries = IntArray(2)
			val best = doubleArrayOf(Double.MAX_VALUE, Double.MAX_VALUE)
			// what the core keeps after each round
			val heap = ArrayList<Long>()
			for (round in 0..ROUNDS) {
				val ms = DoubleArray(2)
				for (run in runs) {
					for (text in run.texts) {
						val kind = if (text.endsWith(" ")) 0 else 1
						val start = TimeSource.Monotonic.markNow()
						testAppCall { core.immediateSearch(text, run.center) }
						ms[kind] += start.elapsedNow().inWholeMicroseconds / 1000.0
						if (round == 0) {
							queries[kind]++
						}
					}
				}
				if (round > 0) {
					best[0] = minOf(best[0], ms[0])
					best[1] = minOf(best[1], ms[1])
				}
				heap.add(testHeapAfterGc() / (1 shl 20))
			}
			println(
				"SpatialTextSearchAPIBenchmarkTest on ${testPlatformName()}: ${queries[0]} complete queries, " +
						"${(best[0] * 10).toLong() / 10.0} ms; ${queries[1]} being typed, ${(best[1] * 10).toLong() / 10.0} ms; " +
						"best of $ROUNDS rounds; heap after each round $heap MB"
			)
		} finally {
			readers.forEach { it.close() }
			regions.getReader()?.close()
			MapPoiTypes.setDefault(defaultTypes)
		}
	}

	companion object {
		private const val ROUNDS = 5
	}
}
