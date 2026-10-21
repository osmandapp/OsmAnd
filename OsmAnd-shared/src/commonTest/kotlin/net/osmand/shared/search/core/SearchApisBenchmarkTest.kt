package net.osmand.shared.search.core

import co.touchlab.stately.concurrency.AtomicInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.SearchUICore.SearchResultMatcher
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.openJavaDump
import kotlin.math.round
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long each search api of [SearchCoreFactory] takes over the phrases `SearchApisCompatTest`
 * typed in, at the first two radius levels, for the cases of `SearchUICoreTest` and the real maps
 * it was given. The phrases and the maps come from its dump; an api searches only when the core
 * would have it search. `SearchApisBenchmarkTest` in OsmAnd-java measures java on the same.
 *
 * Not part of a normal run: it only does anything when `OSMAND_SEARCH_APIS_BENCHMARK` is set.
 */
class SearchApisBenchmarkTest {

	private class Case(val settings: String, val files: List<String>) {
		val texts = ArrayList<String>()
		val radii = ArrayList<Int>()
	}

	@Test
	fun benchmarkSearchApis() {
		if (testEnvironment("OSMAND_SEARCH_APIS_BENCHMARK") == null) {
			println("SearchApisBenchmarkTest: set OSMAND_SEARCH_APIS_BENCHMARK to run")
			return
		}
		val source = openJavaDump(
			"SearchApisBenchmarkTest", "OSMAND_SEARCH_APIS_JAVA_DUMP", "search-apis-java.txt", "SearchApisCompatTest"
		) ?: return
		SearchPhraseTest.setUp
		val poiPhrases = HashMap<String, String>()
		val cases = ArrayList<Case>()
		try {
			var c: Case? = null
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				when {
					line.startsWith("X ") -> poiPhrases[dumpUnhex(f[0].substring(2))] = dumpUnhex(f[1])
					line.startsWith("C -1") -> c = null
					line.startsWith("C ") -> {
						c = Case(dumpUnhex(f[2]), if (f[3] == "-") emptyList() else f[3].split(",").map { dumpUnhex(it) })
						cases.add(c)
					}
					line.startsWith("Q ") && c != null && f[2] == "T" -> {
						c.texts.add(dumpUnhex(f[5]))
						c.radii.add(f[6].toInt())
					}
				}
			}
		} finally {
			source.close()
		}
		val defaultTypes = MapPoiTypes.getDefault()
		val readers = HashMap<String, BinaryMapIndexReader>()
		val names = SearchCoreFactoryTest.API_NAMES
		val best = DoubleArray(names.size) { Double.MAX_VALUE }
		val counts = LongArray(names.size)
		try {
			val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
			MapPoiTypes.setDefault(types)
			types.setPoiTranslator(SearchCoreFactoryTest.Translator(poiPhrases))
			val dir = RoutingTestFixtures.resourcesDir().parent!!.parent!!.parent!! / "build" / "search-obf"
			for (c in cases) {
				for (name in c.files) {
					readers.getOrPut(name) { BinaryMapIndexReader(if (name.startsWith("/")) name else (dir / name).toString()) }
				}
			}
			repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
				val nanos = LongArray(names.size)
				val published = LongArray(names.size)
				for (c in cases) {
					val settings = SearchSettings.parseJSON(Json.parseToJsonElement(c.settings) as JsonObject)
					settings.setOfflineIndexes(c.files.map { readers.getValue(it) })
					val apis = SearchCoreFactoryTest.apis(types)
					for (i in c.texts.indices) {
						val s = if (c.radii[i] == 1) settings else settings.setRadiusLevel(c.radii[i])
						val phrase = SearchPhrase.emptyPhrase(s).generateNewPhrase(c.texts[i], s)
						phrase.sortFiles()
						for (a in apis.indices) {
							val api = apis[a]
							val mark = TimeSource.Monotonic.markNow()
							if (api.isSearchAvailable(phrase) && api.getSearchPriority(phrase) != -1) {
								val matcher = SearchResultMatcher(null, phrase, 0, AtomicInt(0), -1)
								try {
									api.search(phrase, matcher)
								} catch (e: Exception) {
									// the core goes on to the next api
								}
								published[a] += matcher.getCount().toLong()
							}
							nanos[a] += mark.elapsedNow().inWholeNanoseconds
						}
					}
				}
				if (round >= WARMUP_ROUNDS) {
					for (a in names.indices) {
						best[a] = minOf(best[a], nanos[a] / 1e6)
						counts[a] = published[a]
					}
				}
			}
		} finally {
			readers.values.forEach { it.close() }
			MapPoiTypes.setDefault(defaultTypes)
		}
		println("")
		println("### search apis, shared copy on ${testPlatformName()}, ${cases.size} cases, ${cases.sumOf { it.texts.size }} phrases, best of $MEASURED_ROUNDS")
		for (a in names.indices) {
			row(names[a] + if (a == 7) " (far)" else "", best[a], counts[a])
		}
		row("all", best.sum(), 0)
	}

	private fun row(what: String, ms: Double, count: Long) {
		val rounded = (round(ms * 10) / 10).toString()
		println("  ${what.padEnd(48)} ${rounded.padStart(9)} ${count.toString().padStart(9)}")
	}

	companion object {
		const val WARMUP_ROUNDS = 2
		const val MEASURED_ROUNDS = 5
	}
}
