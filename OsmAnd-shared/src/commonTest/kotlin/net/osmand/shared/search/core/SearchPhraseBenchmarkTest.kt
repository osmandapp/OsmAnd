package net.osmand.shared.search.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.readJavaDump
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long [SearchPhrase] takes to read what is typed, which the search does on every key; to
 * count its words in results and weigh them, which it does for every result it sorts; and to
 * select a result and read on. The settings, phrases and results are the ones
 * `SearchPhraseCompatTest` in OsmAnd-java writes to its dump; the results are made before each
 * round, as the search makes them elsewhere. `SearchPhraseBenchmarkTest` in OsmAnd-java measures
 * java on the same.
 *
 * Not part of a normal run: it only does anything when `OSMAND_SEARCH_PHRASE_BENCHMARK` is set.
 */
class SearchPhraseBenchmarkTest {

	@Test
	fun benchmarkSearchPhrase() {
		if (testEnvironment("OSMAND_SEARCH_PHRASE_BENCHMARK") == null) {
			println("SearchPhraseBenchmarkTest: set OSMAND_SEARCH_PHRASE_BENCHMARK to run")
			return
		}
		val lines = readJavaDump(
			"SearchPhraseBenchmarkTest", "OSMAND_SEARCH_PHRASE_JAVA_DUMP", "search-phrase-java.txt", "SearchPhraseCompatTest"
		) ?: return
		SearchPhraseTest.setUp
		val builder = SearchPhraseTest()

		val settings = HashMap<Int, SearchSettings>()
		val phraseSettings = HashMap<Int, SearchSettings>()
		val texts = HashMap<Int, String>()
		val phrases = ArrayList<Int>()
		val resultPhrases = ArrayList<Int>()
		val resultSpecs = ArrayList<String>()
		val selectPhrases = ArrayList<Int>()
		val selectSpecs = ArrayList<String>()
		for (line in lines) {
			val f = line.split("\t")
			when (line[0]) {
				'S' -> settings[f[0].substring(2).toInt()] =
					SearchSettings.parseJSON(Json.parseToJsonElement(dumpUnhex(f[1])) as JsonObject)
				'P' -> {
					val p = f[0].substring(2).toInt()
					phrases.add(p)
					phraseSettings[p] = settings.getValue(f[1].toInt())
					texts[p] = dumpUnhex(f[2])
				}
				'R' -> {
					resultPhrases.add(f[0].substring(2).toInt())
					resultSpecs.add(f[1])
				}
				'W' -> {
					selectPhrases.add(f[0].substring(2).toInt())
					selectSpecs.add(f[1])
				}
			}
		}

		val best = DoubleArray(3) { Double.MAX_VALUE }
		var counts = IntArray(3)
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			var mark = TimeSource.Monotonic.markNow()
			val typed = HashMap<Int, SearchPhrase>()
			var words = 0
			for (p in phrases) {
				val s = phraseSettings.getValue(p)
				val phrase = SearchPhrase.emptyPhrase(s).generateNewPhrase(texts.getValue(p), s)
				words += phrase.getUnknownWordToSearch().length + phrase.getUnknownSearchWords().size
				phrase.getMainUnknownNameStringMatcher()
				typed[p] = phrase
			}
			val type = mark.elapsedNow().inWholeMicroseconds / 1000.0

			val results = resultSpecs.indices.map { builder.build(resultSpecs[it], typed.getValue(resultPhrases[it])) }
			mark = TimeSource.Monotonic.markNow()
			var matched = 0
			for (i in results.indices) {
				val r = results[i]
				matched += typed.getValue(resultPhrases[i]).countUnknownWordsMatchMainResult(r)
				r.getUnknownPhraseMatchWeight()
				r.filterUnknownSearchWord(null)
			}
			val weigh = mark.elapsedNow().inWholeMicroseconds / 1000.0

			val selected = selectSpecs.indices.map { builder.build(selectSpecs[it], typed.getValue(selectPhrases[it])) }
			mark = TimeSource.Monotonic.markNow()
			var selections = 0
			for (i in selected.indices) {
				val phrase = typed.getValue(selectPhrases[i])
				var next = phrase.selectWord(selected[i], phrase.getSettings())
				next = next.generateNewPhrase(next.getText(true) + "12 ", phrase.getSettings())
				selections += next.getWords().size
			}
			val select = mark.elapsedNow().inWholeMicroseconds / 1000.0

			if (round >= WARMUP_ROUNDS) {
				val times = doubleArrayOf(type, weigh, select)
				for (i in times.indices) {
					if (times[i] < best[i]) {
						best[i] = times[i]
					}
				}
				counts = intArrayOf(words, matched, selections)
			}
		}
		println("")
		println("### search phrase, shared copy on ${testPlatformName()}, best of $MEASURED_ROUNDS")
		println("  ${"what".padEnd(34)} ${"ms".padStart(9)} ${"count".padStart(9)}")
		row("type ${phrases.size} phrases", best[0], counts[0])
		row("weigh ${resultSpecs.size} results", best[1], counts[1])
		row("select ${selectSpecs.size} results", best[2], counts[2])
	}

	private fun row(what: String, ms: Double, count: Int) {
		val rounded = (kotlin.math.round(ms * 10) / 10).toString()
		println("  ${what.padEnd(34)} ${rounded.padStart(9)} ${count.toString().padStart(9)}")
	}

	companion object {
		const val WARMUP_ROUNDS = 3
		const val MEASURED_ROUNDS = 5
	}
}
