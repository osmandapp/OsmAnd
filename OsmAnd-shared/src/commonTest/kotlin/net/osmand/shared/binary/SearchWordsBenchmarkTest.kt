package net.osmand.shared.binary

import net.osmand.shared.map.OsmandRegions
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.util.KLocationParser
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.readJavaDump
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy takes to build the common and frequent words with those of the regions,
 * which the search does once, before its first phrase; to rank words by them; and to read places
 * typed as coordinates. The java half is `SearchWordsBenchmarkTest` in OsmAnd-java, over the same
 * words and phrases: the ones `CommonWordsCompatTest` and `LocationParserCompatTest` write to their
 * dumps, which this reads.
 *
 * **Not part of a normal run**: it only does anything when `OSMAND_SEARCH_WORDS_BENCHMARK` is set.
 * On Kotlin/Native only the **release** binary says anything about speed:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * SIMCTL_CHILD_OSMAND_SEARCH_WORDS_BENCHMARK=1 SIMCTL_CHILD_OSMAND_REGIONS_OCBF=<regions.ocbf> \
 *   SIMCTL_CHILD_OSMAND_COMMON_WORDS_JAVA_DUMP=<dump> SIMCTL_CHILD_OSMAND_LOCATION_PARSER_JAVA_DUMP=<dump> \
 *   xcrun simctl spawn --standalone <udid> build/bin/iosSimulatorArm64/releaseTest/test.kexe \
 *   --ktest_filter='net.osmand.shared.binary.SearchWordsBenchmarkTest.*'
 * ```
 *
 * On the `regions.ocbf` of 24.09.2026 and master of 26.09.2026, best of five in ms, next to the
 * java half:
 * ```
 * what                               java    copy  native   count
 * first build in the process         57.0    43.4    28.6       1
 * build the words                    13.6    12.7    28.8       1
 * rank 91170 words                    2.1     1.9     6.1   51655
 * parse 78372 phrases               139.8   128.9  1085.9   54609
 * ```
 * Kotlin/Native builds the words in 29 ms, 2.3 times the jvm once it is warm and 0.66 of a jvm's
 * first time. It ranks a word in 67 ns and reads a phrase in 14 us, 8.4 times the copy on the jvm;
 * the search reads one phrase per query.
 */
class SearchWordsBenchmarkTest {

	@Test
	fun benchmarkSearchWords() {
		if (testEnvironment("OSMAND_SEARCH_WORDS_BENCHMARK") == null) {
			println("SearchWordsBenchmarkTest: set OSMAND_SEARCH_WORDS_BENCHMARK to run")
			return
		}
		val wordLines = readJavaDump(
			"SearchWordsBenchmarkTest", "OSMAND_COMMON_WORDS_JAVA_DUMP", "common-words-java.txt", "CommonWordsCompatTest"
		) ?: return
		val phraseLines = readJavaDump(
			"SearchWordsBenchmarkTest", "OSMAND_LOCATION_PARSER_JAVA_DUMP", "location-parser-java.txt",
			"LocationParserCompatTest"
		) ?: return
		val words = wordLines.filter { it.startsWith("W ") }.map { dumpUnhex(it.substring(2, it.indexOf('\t'))) }
		val phrases = phraseLines.filter { it.startsWith("L ") }.map { dumpUnhex(it.substring(2, it.indexOf('\t'))) }
		val path = testEnvironment("OSMAND_REGIONS_OCBF") ?: "../OsmAnd-java/src/main/resources/net/osmand/map/regions.ocbf"
		val regions = OsmandRegions(path)

		val best = DoubleArray(3) { Double.MAX_VALUE }
		var first = 0.0
		var ranked = 0
		var places = 0
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			var mark = TimeSource.Monotonic.markNow()
			val cw = CommonWords.create(regions)
			val build = mark.elapsedNow().inWholeMicroseconds / 1000.0

			mark = TimeSource.Monotonic.markNow()
			var r = 0
			for (w in words) {
				if (cw.getCommonSearch(w) != -1) {
					r++
				}
			}
			val rank = mark.elapsedNow().inWholeMicroseconds / 1000.0

			mark = TimeSource.Monotonic.markNow()
			var p = 0
			for (phrase in phrases) {
				if (KLocationParser.parseLocation(phrase) != null) {
					p++
				}
			}
			val parse = mark.elapsedNow().inWholeMicroseconds / 1000.0

			if (round == 0) {
				first = build
			}
			if (round >= WARMUP_ROUNDS) {
				val times = doubleArrayOf(build, rank, parse)
				for (i in times.indices) {
					if (times[i] < best[i]) {
						best[i] = times[i]
					}
				}
				ranked = r
				places = p
			}
		}
		regions.close()
		println("")
		println("### search words, shared copy on ${testPlatformName()}, best of $MEASURED_ROUNDS")
		println("  ${"what".padEnd(34)} ${"ms".padStart(9)} ${"count".padStart(9)}")
		row("first build in the process", first, 1)
		row("build the words", best[0], 1)
		row("rank ${words.size} words", best[1], ranked)
		row("parse ${phrases.size} phrases", best[2], places)
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
