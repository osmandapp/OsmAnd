package net.osmand.shared.search.rules

import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner
import net.osmand.shared.util.dumpFnv
import net.osmand.shared.util.dumpHex
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.readJavaDump
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [SearchModRules] wherever it runs, on Kotlin/Native as well as the jvm.
 *
 * [sameAsJava] reads what `SearchRulesCompatTest` in OsmAnd-java wrote to
 * `../OsmAnd-java/build/search-rules-java.txt`, or wherever `OSMAND_SEARCH_RULES_JAVA_DUMP` points:
 * the directory of the rules files java read, the locales, words, map names and random rules files
 * it asked about, and a hash of each of its answers. It asks the copy the same and holds it to java
 * exactly, except on Kotlin/Native for the random rules with a file that ends before `</rules>`:
 * kxml, the parser of java and of the copy on the jvm, reads such a file up to its end, libxml2 on
 * iOS refuses it, and the copy searches without it. [brokenFilesAreSearchedWithout] needs no java.
 */
class SearchModRulesTest {

	@Test
	fun sameAsJava() {
		val lines = readJavaDump(
			"SearchModRulesTest", "OSMAND_SEARCH_RULES_JAVA_DUMP", "search-rules-java.txt", "SearchRulesCompatTest"
		) ?: return
		var dir = ""
		val locales = ArrayList<String?>()
		val words = ArrayList<String>()
		val splits = ArrayList<Set<String>>()
		val randomWords = ArrayList<String>()
		val randomSplits = ArrayList<Set<String>>()
		val maps = ArrayList<String?>()
		val randomFiles = LinkedHashMap<Int, MutableList<Pair<String, String>>>()
		val java = LinkedHashMap<String, String>()
		for (line in lines) {
			val f = line.split("\t")
			when (f[0]) {
				"D" -> dir = dumpUnhex(f[1])
				"L" -> locales.add(unhexOrNull(f[1]))
				"W" -> {
					words.add(dumpUnhex(f[1]))
					splits.add(unhexSet(f[2]))
				}
				"V" -> {
					randomWords.add(dumpUnhex(f[1]))
					randomSplits.add(unhexSet(f[2]))
				}
				"M" -> maps.add(unhexOrNull(f[1]))
				"F" -> randomFiles.getOrPut(f[1].toInt()) { ArrayList() }.add(f[2] to dumpUnhex(f[3]))
				"E" -> java[f[1]] = f[2] + "\t" + f[3]
			}
		}
		val copy = LinkedHashMap<String, String>()
		fun same(name: String, answer: String) {
			copy[name] = answer.length.toString() + "\t" + dumpFnv(answer)
		}

		val rules = SearchModRules.forSearch(dir)
		for (locale in locales) {
			same("locale " + hexOrNull(locale), answers { describe(rules.rules(locale), rules.dictionary(locale), words, splits) })
		}
		val m = StringBuilder()
		for (name in maps) {
			m.append(rules.locales().forMap(name)).append('|').append(rules.locales().mapPrefix(name)).append('\n')
		}
		same("maps", m.toString())
		val n = StringBuilder()
		for (locale in locales) {
			n.append(answers { rules.locales().normalize(locale) }).append('|')
				.append(answers { rules.dictionary(locale).locale() }).append('\n')
		}
		same("locale names", n.toString())
		val r = StringBuilder()
		for (i in words.indices) {
			r.append(if (rules.likelyPartOfRef(words[i], splits[i])) '1' else '0')
		}
		same("refs", r.toString())

		val none = SearchModRules.forSearch("$dir/no-such-dir")
		val o = StringBuilder()
		for (locale in locales) {
			o.append(answers { describe(none.rules(locale), none.dictionary(locale), words, splits) })
				.append(none.locales().forMap("Germany_europe")).append('\n')
		}
		same("no rules", o.toString())

		val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "search-rules-${Random.nextLong().toULong()}"
		var endless = 0
		val kxml = testPlatformName().startsWith("jvm")
		try {
			for ((case, files) in randomFiles) {
				// kxml reads a file up to where it ends early, libxml2 drops it: the parsers of the jvm and of iOS
				if (!kxml && files.any { !it.second.trimEnd().endsWith("</rules>") }) {
					java.remove("random $case")
					endless++
					continue
				}
				val caseDir = root / case.toString()
				FileSystem.SYSTEM.createDirectories(caseDir)
				for ((file, text) in files) {
					FileSystem.SYSTEM.write(caseDir / file) { writeUtf8(text) }
				}
				same("random $case", answers {
					val random = SearchModRules.forSearch(caseDir.toString())
					val sb = StringBuilder()
					for (name in RANDOM_PREFIXES) {
						sb.append(random.locales().forMap(name + "_europe")).append(' ')
					}
					sb.append('\n').append(describe(random.rules(RANDOM_LOCALE), random.dictionary(RANDOM_LOCALE),
						randomWords, randomSplits)).toString()
				})
			}
		} finally {
			FileSystem.SYSTEM.deleteRecursively(root)
		}

		val different = java.keys.filter { java[it] != copy[it] }
		if (different.isNotEmpty() || java.keys != copy.keys) {
			fail("different from java: $different; java has ${java.keys - copy.keys}, the copy ${copy.keys - java.keys}")
		}
		assertTrue(java.size > 500, "answers compared: ${java.size}")
		println("SearchModRulesTest: ${java.size} answers the same as java over ${locales.size} locales, " +
			"${words.size} words and ${randomFiles.size - endless} random rules; $endless with a file that ends early " +
			"not compared")
	}

	/** A broken or missing file is logged and the search runs without it: without all rules for rules.xml. */
	@Test
	fun brokenFilesAreSearchedWithout() {
		val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "search-rules-${Random.nextLong().toULong()}"
		try {
			val locales = "<locales><map locale=\"en-us\" prefixes=\"Us\"/></locales>"
			val query = "<query><rule from=\"st\" to=\"Street\" object=\"street\"/><skipPenalty>the</skipPenalty></query>"
			val good = rules(root / "good", "<rules version=\"5\">$locales$query</rules>",
				"rules_en.xml" to "<rules version=\"5\"><query><rule from=\"ave\" to=\"Avenue\"/></query></rules>",
				"rules_en_US.xml" to "<rules version=\"5\"><query><rule from=\"st\" enabled=\"false\"/></query></rules>")
			assertEquals("en_US", good.locales().forMap("Us_texas_northamerica_2.obf"))
			assertEquals(listOf("street"), good.dictionary("en").getQueryForms("St").map { it.word })
			assertEquals(listOf("avenue"), good.dictionary("en").getQueryForms("ave").map { it.word })
			assertEquals(emptyList(), good.dictionary("en-us").getQueryForms("st"))
			assertTrue(good.dictionary("en_US").isCommonSkipOtherCnt("the", SearchModRuleOwner.POI))

			val brokenLayer = rules(root / "layer", "<rules version=\"5\">$locales$query</rules>",
				"rules_en.xml" to "<rules version=\"5\"><query><rule from=\"ave\" to=\"Avenue\"></query></rules>",
				"rules_en_US.xml" to "<rules version=\"4\"><query><rule from=\"st\" enabled=\"false\"/></query></rules>")
			assertEquals(listOf("street"), brokenLayer.dictionary("en_US").getQueryForms("st").map { it.word })
			assertEquals(emptyList(), brokenLayer.dictionary("en_US").getQueryForms("ave"))

			for (base in listOf("<rules version=\"4\">$locales$query</rules>", "<rules version=\"5\">$query</rules>",
				"<rules version=\"5\">$locales<query></rules>", null)) {
				val broken = rules(root / "base", base)
				assertEquals("", broken.locales().forMap("Us_texas"))
				assertEquals(emptyList(), broken.dictionary("").getQueryForms("st"))
				assertFalse(broken.dictionary("").isCommonSkipOtherCnt("the", SearchModRuleOwner.POI))
				FileSystem.SYSTEM.deleteRecursively(root / "base")
			}
		} finally {
			FileSystem.SYSTEM.deleteRecursively(root)
		}
	}

	/**
	 * Threads asking for the dictionaries of many spellings of many locales at once, on a fresh [SearchModRules]
	 * each round, half of the rounds with the dictionaries already read: each locale has one dictionary, the one
	 * asked alone gets, with the forms of its own rules file.
	 */
	@Test
	fun concurrentCallersGetOneDictionary() {
		val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "search-rules-${Random.nextLong().toULong()}"
		try {
			val languages = ('a'..'t').map { "a$it" }
			val layers = languages.map { "rules_$it.xml" to "<rules version=\"5\"><query><rule from=\"w\" to=\"Form$it\"/></query></rules>" }
			val dir = root / "rules"
			rules(dir, "<rules version=\"5\"><locales><map locale=\"aa\" prefixes=\"Aa\"/></locales></rules>",
				*layers.toTypedArray())
			val spellings = languages.flatMap { listOf(it, it.uppercase(), " $it", it.replaceFirstChar { c -> c.uppercase() }) }
			repeat(ROUNDS) { round ->
				val rules = SearchModRules.forSearch(dir.toString())
				if (round % 2 == 1) {
					languages.forEach { rules.rules(it) }
				}
				val got = runBlocking(Dispatchers.Default) {
					List(THREADS) { t ->
						async { spellings.shuffled(Random(round * THREADS + t)).map { it to rules.dictionary(it) } }
					}.awaitAll()
				}
				for ((spelling, dictionary) in got.flatten()) {
					val language = spelling.trim().lowercase()
					assertSame(rules.dictionary(language), dictionary, "round $round, '$spelling'")
					assertEquals(listOf("form$language"), dictionary.getQueryForms("w").map { it.word })
				}
			}
		} finally {
			FileSystem.SYSTEM.deleteRecursively(root)
		}
	}

	private fun rules(dir: Path, base: String?, vararg layers: Pair<String, String>): SearchModRules {
		FileSystem.SYSTEM.createDirectories(dir)
		if (base != null) {
			FileSystem.SYSTEM.write(dir / SearchModLocaleRules.BASE_FILE) { writeUtf8(base) }
		}
		for ((file, text) in layers) {
			FileSystem.SYSTEM.write(dir / file) { writeUtf8(text) }
		}
		return SearchModRules.forSearch(dir.toString())
	}

	private companion object {
		const val ROUNDS = 50
		const val THREADS = 8

		// as SearchRulesCompatTest
		val RANDOM_PREFIXES = listOf("Us", "us_texas", "Germany", "switzerland_ticino", "Switzerland", "fr")
		const val RANDOM_LOCALE = "xx_YY_ZZ"

		fun answers(answer: () -> String): String =
			try {
				answer()
			} catch (e: Exception) {
				"throws"
			}

		fun unhexOrNull(h: String): String? = if (h == "null") null else dumpUnhex(h)

		fun hexOrNull(s: String?): String = if (s == null) "null" else dumpHex(s)

		fun unhexSet(h: String): Set<String> =
			if (h == "-") emptySet() else h.split(",").mapTo(LinkedHashSet()) { dumpUnhex(it) }

		// what SearchRulesCompatTest.describe writes for java
		fun describe(rules: SearchModLocaleRules, dictionary: SearchModDictionary, words: List<String>,
			splits: List<Set<String>>): String {
			val sb = StringBuilder()
			for (rule in rules.query()) {
				sb.append("q ").append(rule.word).append(" =")
				for (f in rule.forms) {
					sb.append(' ').append(f.`object`).append(':').append(f.word)
				}
				sb.append('\n')
			}
			sb.append("fw ").append(rules.formWords().joinToString(" ")).append('\n')
			for (w in rules.formWords()) {
				sb.append("f ").append(w).append(" =")
				for (f in rules.forms(w)) {
					sb.append(' ').append(f.`object`).append(':').append(f.word)
				}
				sb.append('\n')
			}
			for (w in words) {
				if (rules.forms(w).isNotEmpty()) {
					sb.append("fv ").append(w).append(' ').append(rules.forms(w).size).append('\n')
				}
			}
			sb.append("b ").append(rules.buildings().joinToString(" ")).append('\n')
			sb.append("i ").append(rules.ignorables().joinToString(" ")).append('\n')
			for ((w, list) in rules.skipPenalty()) {
				sb.append("sp ").append(w).append(" = ").append(list).append('\n')
			}
			sb.append("d ").append(dictionary.locale()).append(" | ").append(dictionary).append('\n')
			for (i in words.indices) {
				val w = words[i]
				val forms = dictionary.getQueryForms(w)
				if (forms.isNotEmpty()) {
					sb.append("qf ").append(w).append(" =")
					for (f in forms) {
						sb.append(' ').append(f.`object`).append(':').append(f.word).append(':').append(f.reverse)
							.append(':').append(f.isUnscoped()).append(':')
						for (o in f.owners()) {
							sb.append(o.tag).append(',')
						}
						for (o in SearchModRuleOwner.entries) {
							sb.append(if (f.appliesTo(o)) '1' else '0')
						}
					}
					sb.append('\n')
				}
				val building = dictionary.likelyPartOfBuilding(w, null)
				val split = dictionary.likelyPartOfBuilding(w, splits[i])
				val skip = StringBuilder()
				for (o in SearchModRuleOwner.entries) {
					skip.append(if (dictionary.isCommonSkipOtherCnt(w, o)) '1' else '0')
					skip.append(if (dictionary.isCommonSkipOtherCnt(w.lowercase(), o)) '1' else '0')
				}
				if (building || split || skip.indexOf("1") >= 0) {
					sb.append("w ").append(w).append(' ').append(if (building) 1 else 0).append(if (split) 1 else 0)
						.append(' ').append(skip).append('\n')
				}
			}
			return sb.toString()
		}
	}
}
