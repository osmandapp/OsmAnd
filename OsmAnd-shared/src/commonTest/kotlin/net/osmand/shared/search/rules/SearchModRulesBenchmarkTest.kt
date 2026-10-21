package net.osmand.shared.search.rules

import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.readJavaDump
import kotlin.test.Test
import kotlin.time.TimeSource

/**
 * How long the shared copy of the search rules takes wherever it runs, over what `SearchRulesCompatTest` in
 * OsmAnd-java wrote to `../OsmAnd-java/build/search-rules-java.txt`: the rules of every locale read, the forms of
 * every word asked in every locale, whether every word penalizes an object of each owner, as the spatial search asks
 * it (`dictionary(locale).isCommonSkipOtherCnt`), and whether every word is a part of a house number. The java half,
 * `SearchRulesBenchmarkTest` in OsmAnd-java, puts java next to the copy on the jvm over the same words.
 *
 * **Not part of a normal run**: it only does anything when `OSMAND_SEARCH_RULES_BENCHMARK` is set. On Kotlin/Native
 * only the **release** binary says anything about speed:
 * ```
 * ./gradlew :OsmAnd-shared:linkReleaseTestIosSimulatorArm64
 * cd OsmAnd-shared && SIMCTL_CHILD_OSMAND_SEARCH_RULES_BENCHMARK=1 SIMCTL_CHILD_OSMAND_SEARCH_RULES_JAVA_DUMP=<dump> \
 *   xcrun simctl spawn --standalone <udid> build/bin/iosSimulatorArm64/releaseTest/test.kexe \
 *   --ktest_filter='net.osmand.shared.search.rules.SearchModRulesBenchmarkTest.*'
 * ```
 */
class SearchModRulesBenchmarkTest {

	@Test
	fun searchRules() {
		if (testEnvironment("OSMAND_SEARCH_RULES_BENCHMARK") == null) {
			println("SearchModRulesBenchmarkTest: set OSMAND_SEARCH_RULES_BENCHMARK to run")
			return
		}
		val lines = readJavaDump(
			"SearchModRulesBenchmarkTest", "OSMAND_SEARCH_RULES_JAVA_DUMP", "search-rules-java.txt", "SearchRulesCompatTest"
		) ?: return
		var dir = ""
		val locales = ArrayList<String?>()
		val words = ArrayList<String>()
		val splits = ArrayList<Set<String>>()
		for (line in lines) {
			val f = line.split("\t")
			when (f[0]) {
				"D" -> dir = dumpUnhex(f[1])
				// the two that throw in both are not asked
				"L" -> if (f[1] != "x005f" && f[1] != "x002d") locales.add(if (f[1] == "null") null else dumpUnhex(f[1]))
				"W" -> {
					words.add(dumpUnhex(f[1]))
					splits.add(if (f[2] == "-") emptySet() else f[2].split(",").mapTo(LinkedHashSet()) { dumpUnhex(it) })
				}
			}
		}
		val owners = SearchModRuleOwner.entries
		val best = LongArray(4) { Long.MAX_VALUE }
		val found = LongArray(4)
		repeat(WARMUP_ROUNDS + MEASURED_ROUNDS) { round ->
			val t = LongArray(4)
			found.fill(0)
			var mark = TimeSource.Monotonic.markNow()
			val k = SearchModRules.forSearch(dir)
			for (l in locales) {
				found[0] += k.dictionary(l).locale().length
			}
			t[0] = mark.elapsedNow().inWholeNanoseconds

			mark = TimeSource.Monotonic.markNow()
			for (l in locales) {
				for (w in words) {
					found[1] += k.dictionary(l).getQueryForms(w).size
				}
			}
			t[1] = mark.elapsedNow().inWholeNanoseconds

			mark = TimeSource.Monotonic.markNow()
			for (l in locales) {
				for (w in words) {
					for (o in owners) {
						found[2] += if (k.dictionary(l).isCommonSkipOtherCnt(w, o)) 1 else 0
					}
				}
			}
			t[2] = mark.elapsedNow().inWholeNanoseconds

			mark = TimeSource.Monotonic.markNow()
			for (l in locales) {
				for (i in words.indices) {
					found[3] += if (k.dictionary(l).likelyPartOfBuilding(words[i], splits[i])) 1 else 0
				}
			}
			t[3] = mark.elapsedNow().inWholeNanoseconds
			if (round >= WARMUP_ROUNDS) {
				for (n in t.indices) {
					best[n] = minOf(best[n], t[n])
				}
			}
		}
		val asked = locales.size * words.size
		println("### search rules on ${testPlatformName()}, best of $MEASURED_ROUNDS in ms")
		val names = listOf("read ${locales.size} locales", "query forms $asked", "skip penalty ${asked * owners.size}",
			"part of building $asked")
		for (n in names.indices) {
			println("${names[n].padEnd(28)} ${(best[n] / 1e4).toLong() / 100.0}   found ${found[n]}")
		}
	}

	private companion object {
		const val WARMUP_ROUNDS = 3
		const val MEASURED_ROUNDS = 5
	}
}
