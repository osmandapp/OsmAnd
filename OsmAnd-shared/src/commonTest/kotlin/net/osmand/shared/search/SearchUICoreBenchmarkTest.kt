package net.osmand.shared.search

import co.touchlab.stately.concurrency.AtomicInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.SearchUICore.SearchResultCollection
import net.osmand.shared.search.SearchUICore.SearchResultMatcher
import net.osmand.shared.search.core.SearchCoreFactory
import net.osmand.shared.search.core.SearchCoreFactoryTest
import net.osmand.shared.search.core.SearchPhrase
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.search.core.SearchResult
import net.osmand.shared.search.core.SearchSettings
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.openJavaDump
import kotlin.math.round
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the core takes over the phrases `SearchUICoreCompatTest` typed in, at the first two radius
 * levels, for the cases of `SearchUICoreTest` and the real maps it was given: to run the apis for a
 * phrase, to keep what they found sorted, united and without duplicates, and to merge it api by api
 * as the ui shows it while the search goes on. The phrases and the maps come from its dump; each case
 * gets a core of its own every round. `SearchUICoreBenchmarkTest` in OsmAnd-java measures java on the same.
 *
 * Not part of a normal run: it only does anything when `OSMAND_SEARCH_CORE_BENCHMARK` is set.
 */
class SearchUICoreBenchmarkTest {

	private class Case(val settings: String, val files: List<String>, val poiTypes: Boolean) {
		val texts = ArrayList<String>()
		val radii = ArrayList<Int>()
	}

	@Test
	fun benchmarkSearchUICore() {
		if (testEnvironment("OSMAND_SEARCH_CORE_BENCHMARK") == null) {
			println("SearchUICoreBenchmarkTest: set OSMAND_SEARCH_CORE_BENCHMARK to run")
			return
		}
		val source = openJavaDump(
			"SearchUICoreBenchmarkTest", "OSMAND_SEARCH_CORE_JAVA_DUMP", "search-core-java.txt", "SearchUICoreCompatTest"
		) ?: return
		SearchPhraseTest.setUp
		val poiPhrases = HashMap<String, String>()
		val cases = ArrayList<Case>()
		try {
			var c: Case? = null
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.substring(2).split("\t")
				when (line[0]) {
					'X' -> poiPhrases[dumpUnhex(f[0])] = dumpUnhex(f[1])
					'C' -> {
						c = Case(dumpUnhex(f[2]), if (f[3] == "-") emptyList() else f[3].split(",").map { dumpUnhex(it) }, f[4] == "true")
						cases.add(c)
					}
					'Q' -> if (f[2] == "T") {
						c!!.texts.add(dumpUnhex(f[4]))
						c.radii.add(f[5].toInt())
					}
				}
			}
		} finally {
			source.close()
		}
		val defaultTypes = MapPoiTypes.getDefault()
		val readers = HashMap<String, BinaryMapIndexReader>()
		val best = DoubleArray(KINDS.size) { Double.MAX_VALUE }
		val counts = LongArray(KINDS.size)
		try {
			val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
			MapPoiTypes.setDefault(types)
			types.setPoiTranslator(SearchCoreFactoryTest.Translator(poiPhrases))
			for (c in cases) {
				for (name in c.files) {
					readers.getOrPut(name) { BinaryMapIndexReader(SearchUICoreTest.obf(name)) }
				}
			}
			repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
				val nanos = LongArray(KINDS.size)
				val found = LongArray(KINDS.size)
				for (c in cases) {
					SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = c.poiTypes
					val settings = SearchSettings.parseJSON(Json.parseToJsonElement(c.settings) as JsonObject)
					settings.setOfflineIndexes(c.files.map { readers.getValue(it) })
					val core = SearchUICore(types, "en", false) { false }
					core.init()
					for (i in c.texts.indices) {
						val s = if (c.radii[i] == 1) settings else settings.setRadiusLevel(c.radii[i])
						val phrase = SearchPhrase.emptyPhrase(s).generateNewPhrase(c.texts[i], s)
						val byApi = ArrayList<MutableList<SearchResult>>()
						val recorder = object : ResultMatcher<SearchResult> {
							override fun publish(obj: SearchResult): Boolean {
								if (obj.objectType.toString() == "SEARCH_API_FINISHED") {
									byApi.add(ArrayList())
								} else if (!obj.objectType.toString().startsWith("SEARCH_")) {
									if (byApi.isEmpty()) {
										byApi.add(ArrayList())
									}
									byApi.last().add(obj)
								}
								return true
							}

							override fun isCancelled(): Boolean = false
						}
						var mark = TimeSource.Monotonic.markNow()
						val matcher = SearchResultMatcher(recorder, phrase, 0, AtomicInt(0), -1)
						core.searchInternal(phrase, matcher)
						nanos[0] += mark.elapsedNow().inWholeNanoseconds
						found[0] += matcher.getCount().toLong()

						val all = SearchUICoreTest.copies(matcher.getRequestResults())
						val apis = byApi.map { SearchUICoreTest.copies(it) }
						mark = TimeSource.Monotonic.markNow()
						val kept = SearchResultCollection(phrase).addSearchResults(all, true, true)
						nanos[1] += mark.elapsedNow().inWholeNanoseconds
						found[1] += kept.getCurrentSearchResults().size.toLong()

						mark = TimeSource.Monotonic.markNow()
						var merged = SearchResultCollection(phrase)
						for (api in apis) {
							merged = merged.combineWithCollection(SearchResultCollection(phrase).addSearchResults(api, true, true), true, true)
						}
						nanos[2] += mark.elapsedNow().inWholeNanoseconds
						found[2] += merged.getCurrentSearchResults().size.toLong()
					}
				}
				if (round >= WARMUP_ROUNDS) {
					for (k in KINDS.indices) {
						best[k] = minOf(best[k], nanos[k] / 1e6)
						counts[k] = found[k]
					}
				}
			}
		} finally {
			SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = false
			readers.values.forEach { it.close() }
			MapPoiTypes.setDefault(defaultTypes)
		}
		println("")
		println("### search core, shared copy on ${testPlatformName()}, ${cases.size} cases, ${cases.sumOf { it.texts.size }} phrases, best of $MEASURED_ROUNDS")
		for (k in KINDS.indices) {
			row(KINDS[k], best[k], counts[k])
		}
		row("all", best.sum(), 0)
	}

	private fun row(what: String, ms: Double, count: Long) {
		val rounded = (round(ms * 10) / 10).toString()
		println("  ${what.padEnd(40)} ${rounded.padStart(9)} ${count.toString().padStart(9)}")
	}

	companion object {
		const val WARMUP_ROUNDS = 2
		const val MEASURED_ROUNDS = 5
		private val KINDS = listOf("apis run for each phrase", "results kept", "results merged api by api")
	}
}
