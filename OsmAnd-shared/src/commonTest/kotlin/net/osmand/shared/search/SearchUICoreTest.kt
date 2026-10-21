package net.osmand.shared.search

import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.concurrency.AtomicInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.data.KLatLon
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.SearchUICore.SearchResultCollection
import net.osmand.shared.search.SearchUICore.SearchResultMatcher
import net.osmand.shared.search.core.ObjectType
import net.osmand.shared.search.core.SearchCoreAPI
import net.osmand.shared.search.core.SearchCoreFactory
import net.osmand.shared.search.core.SearchCoreFactoryTest
import net.osmand.shared.search.core.SearchCoreFactoryTest.Companion.DIFFERENT
import net.osmand.shared.search.core.SearchCoreFactoryTest.Companion.IN_ULPS
import net.osmand.shared.search.core.SearchCoreFactoryTest.Companion.SAME
import net.osmand.shared.search.core.SearchCoreFactoryTest.Companion.SHIFTED
import net.osmand.shared.search.core.SearchCoreFactoryTest.Companion.close
import net.osmand.shared.search.core.SearchCoreFactoryTest.Companion.describe
import net.osmand.shared.search.core.SearchCoreFactoryTest.Companion.resultLine
import net.osmand.shared.search.core.SearchCoreFactoryTest.Companion.v
import net.osmand.shared.search.core.SearchPhrase
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.search.core.SearchResult
import net.osmand.shared.search.core.SearchSettings
import net.osmand.shared.search.core.SearchWord
import net.osmand.shared.util.KCollator
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTreeSet
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.openJavaDump
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The core of the search, [SearchUICore], wherever it runs, on Kotlin/Native as well as the jvm.
 *
 * [sameAsJava] reads what `SearchUICoreCompatTest` in OsmAnd-java wrote to
 * `../OsmAnd-java/build/search-core-java.txt`, or wherever `OSMAND_SEARCH_CORE_JAVA_DUMP` points:
 * the phrases of the cases of `SearchUICoreTest`, typed in, searched at wider radii and selected,
 * with what the core of java made of what the apis found for each. It searches them again here and
 * holds the copy to java: the results kept, merged api by api and sorted with the duplicates, which
 * neighbours are the same place, and what the core answers about searching further. The results of
 * each api go into the collections in the order of their lines, as they do there. Words are compared
 * by the collation keys java gave them.
 *
 * What `SearchCoreFactoryTest` allows, this allows too: coordinates, distances and the weights that
 * go through them off in their last digits, and streets, houses and crossings a unit of zoom 24
 * north; the distance and the weight in the text the tests compare may then round the other way.
 * Where the api of pois by type could take one of several types that match as well, the phrase and
 * the ones selected from it are not compared.
 *
 * [searchTestCases] runs the cases of `SearchUICoreTest` as it runs them and compares what they
 * expect, with the words compared by the collation keys java gave them; with the collator of the
 * platform, it counts the results that are not the ones expected. The reverse geocoding of the
 * results marked with `@` comes with the geocoding.
 */
class SearchUICoreTest {

	@Test
	fun sameAsJava() {
		val source = openJavaDump(
			"SearchUICoreTest", "OSMAND_SEARCH_CORE_JAVA_DUMP", "search-core-java.txt", "SearchUICoreCompatTest"
		) ?: return
		SearchPhraseTest.setUp
		val defaultTypes = MapPoiTypes.getDefault()
		val poiPhrases = HashMap<String, String>()
		var types: MapPoiTypes? = null
		val readers = HashMap<String, BinaryMapIndexReader>()
		val stats = Stats()
		var run: CaseRun? = null
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.substring(2).split("\t")
				when (line[0]) {
					'X' -> poiPhrases[dumpUnhex(f[0])] = dumpUnhex(f[1])
					'C' -> {
						val t = types ?: typesWith(poiPhrases).also { types = it }
						val keys = source.readUtf8Line()!!
						assertTrue(keys.startsWith("K "), "the keys of case ${f[0]}")
						SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = f[4] == "true"
						run = CaseRun(dumpUnhex(f[1]), settings(f, readers), SearchPhraseTest.JavaCollator(keys.substring(2)), t)
					}
					'Q' -> run!!.phrase(f, stats)
					'F', 'I', 'J', 'N' -> run!!.order(line[0], f, stats)
					'R' -> run!!.result(f, stats)
					'Y' -> run!!.same(f, stats)
					'M' -> run!!.queries(f, stats)
				}
			}
		} finally {
			source.close()
			SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = false
			MapPoiTypes.setDefault(defaultTypes)
			readers.values.forEach { it.close() }
		}
		if (stats.different.isNotEmpty()) {
			fail("different from java:\n" + stats.different.joinToString("\n"))
		}
		assertTrue((stats.compared['R'] ?: 0) > 100000, "results compared: ${stats.compared['R']}")
		println(
			"SearchUICoreTest: the same as java on ${testPlatformName()}: " +
					stats.compared.entries.sortedBy { it.key }.joinToString { "${it.value} ${it.key}" } +
					"; ${stats.inUlps} results with coordinates, a distance or a weight off in the last digits " +
					"(${stats.inUlpsRounded} of them with the weight or the distance in the text of the tests rounded the other way), " +
					"${stats.shifted} streets, houses or crossings a unit of zoom 24 off (${stats.shiftedRounded} of them rounded so), " +
					"${stats.notCompared} phrases not compared, where the api of pois by type could take another type"
		)
	}

	private class Stats {
		val compared = HashMap<Char, Int>()
		val different = ArrayList<String>()
		var inUlps = 0
		var shifted = 0
		var inUlpsRounded = 0
		var shiftedRounded = 0
		var notCompared = 0

		fun check(kind: Char, java: String, copy: String, what: String) {
			compared[kind] = (compared[kind] ?: 0) + 1
			if (java != copy && different.size < 10) {
				different.add("$kind $what\njava $java\ncopy $copy")
			}
		}
	}

	/** What the copy made of the phrase being compared: each list with the copies of the results it was made of. */
	private class Made(
		val qid: Int, val phrase: SearchPhrase, val found: List<SearchResult>,
		val lists: Map<Char, Pair<List<SearchResult>, List<SearchResult>>>, val compared: Boolean
	)

	/** The core of a case, its phrases and the results kept for selecting them. */
	private class CaseRun(
		val name: String, val settings: SearchSettings, val collator: SearchPhraseTest.JavaCollator, types: MapPoiTypes
	) {
		val core = SearchUICore(types, "en", false) { false }
		val phrases = HashMap<Int, SearchPhrase>()
		val results = HashMap<Int, List<SearchResult>>()
		val notCompared = HashSet<Int>()
		var made: Made? = null

		init {
			core.init()
		}

		fun phrase(f: List<String>, stats: Stats) {
			val qid = f[0].toInt()
			val parent = f[1].toInt()
			val r = f[3].toInt()
			val text = dumpUnhex(f[4])
			val radius = f[5].toInt()
			val phrase = when (f[2]) {
				"T" -> {
					val s = if (radius == 1) settings else settings.setRadiusLevel(radius)
					SearchPhrase.emptyPhrase(s, collator).generateNewPhrase(text, s)
				}
				"S" -> {
					val p = phrases.getValue(parent)
					val selected = p.selectWord(results.getValue(parent)[r], p.getSettings())
					selected.generateNewPhrase(selected.getText(true), p.getSettings())
				}
				else -> {
					val res = results.getValue(parent)[r]
					val p = SearchPhrase.emptyPhrase(settings, collator).generateNewPhrase(text, settings)
					p.getWords().add(SearchWord(res.localeName!!, res))
					p
				}
			}
			phrases[qid] = phrase
			val byApi = search(core, phrase)
			val found = canonical(byApi)
			val tie = f[9]
			val compared = parent !in notCompared && (tie == "-" || tie == "none" || tie == "one")
			if (!compared) {
				notCompared.add(qid)
				stats.notCompared++
			} else {
				stats.check('P', f[8], byApi.joinToString(",") { it.size.toString() }.ifEmpty { "-" }, "$name $qid $phrase sizes")
			}
			val lists = HashMap<Char, Pair<List<SearchResult>, List<SearchResult>>>()
			lists['F'] = kept(phrase, copies(found), true, true)
			lists['I'] = merged(phrase, byApi, true, true)
			lists['J'] = merged(phrase, byApi, false, true)
			lists['N'] = kept(phrase, copies(found), true, false)
			made = Made(qid, phrase, found, lists, compared)
			if (f[6] == "true") {
				results[qid] = lists.getValue('F').first
			}
		}

		fun order(kind: Char, f: List<String>, stats: Stats) {
			val m = current(f) ?: return
			val (kept, from) = m.lists.getValue(kind)
			stats.check(kind, f[1], indices(kept, from), "$name ${m.qid} ${m.phrase}")
		}

		fun result(f: List<String>, stats: Stats) {
			val m = current(f) ?: return
			val i = f[1].toInt()
			val kept = m.lists.getValue('F').first
			val java = f[2]
			val copy = if (i < kept.size) finalLine(kept[i], m.phrase) else "missing"
			// the text the tests compare is last, with the weight and the distance rounded
			val javaText = java.substringAfterLast(' ')
			val copyText = copy.substringAfterLast(' ')
			var closeness = if (java == copy) SAME else close(java.substringBeforeLast(' '), copy.substringBeforeLast(' '))
			if (closeness != SAME && closeness != DIFFERENT && javaText != copyText) {
				// a number off in its last digits may round the other way
				if (roundedAlike(javaText, copyText)) {
					if (closeness == SHIFTED) {
						stats.shiftedRounded++
					} else {
						stats.inUlpsRounded++
					}
				} else {
					closeness = DIFFERENT
				}
			}
			when (closeness) {
				IN_ULPS -> stats.inUlps++
				SHIFTED -> stats.shifted++
				DIFFERENT -> {
					stats.check('R', java, copy, "$name ${m.qid} ${m.phrase} result $i")
					return
				}
			}
			stats.check('R', java, java, "")
		}

		fun same(f: List<String>, stats: Stats) {
			val m = current(f) ?: return
			val collection = SearchResultCollection(m.phrase)
			val sb = StringBuilder()
			for (a in m.found.indices) {
				for (b in a + 1 until minOf(m.found.size, a + SAME_DEPTH + 1)) {
					val same = outcome { collection.sameSearchResult(m.found[a], m.found[b]) }
					sb.append(if (same == "true") '1' else if (same == "false") '0' else 'x')
				}
			}
			stats.check('Y', f[1], sb.ifEmpty { "-" }.toString(), "$name ${m.qid} ${m.phrase}")
		}

		fun queries(f: List<String>, stats: Stats) {
			val m = current(f) ?: return
			stats.check('M', dumpUnhex(f[1]), queries(core, m.phrase), "$name ${m.qid} ${m.phrase}")
		}

		private fun current(f: List<String>): Made? {
			val m = made!!
			assertEquals(m.qid, f[0].toInt(), "the lines of phrase ${m.qid}")
			return if (m.compared) m else null
		}
	}

	@Test
	fun searchTestCases() {
		val source = openJavaDump(
			"SearchUICoreTest", "OSMAND_SEARCH_CORE_JAVA_DUMP", "search-core-java.txt", "SearchUICoreCompatTest"
		) ?: return
		SearchPhraseTest.setUp
		val defaultTypes = MapPoiTypes.getDefault()
		val poiPhrases = HashMap<String, String>()
		val readers = HashMap<String, BinaryMapIndexReader>()
		val mismatches = ArrayList<String>()
		var types: MapPoiTypes? = null
		var cases = 0
		var compared = 0
		var orderedOtherwise = 0
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				if (line.startsWith("X ")) {
					val f = line.substring(2).split("\t")
					poiPhrases[dumpUnhex(f[0])] = dumpUnhex(f[1])
				} else if (line.startsWith("C ")) {
					val f = line.substring(2).split("\t")
					val keys = source.readUtf8Line()!!
					if (f[5] == "-") {
						continue
					}
					if (types == null) {
						types = typesWith(poiPhrases)
					}
					SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = f[4] == "true"
					val json = Json.parseToJsonElement(dumpUnhex(f[5])).jsonObject
					val files = if (f[3] == "-") emptyList() else f[3].split(",").map { dumpUnhex(it) }
					val name = dumpUnhex(f[1])
					compared += testCase(name, json, files, readers, SearchPhraseTest.JavaCollator(keys.substring(2)), mismatches)
					val platform = ArrayList<String>()
					testCase(name, json, files, readers, null, platform)
					orderedOtherwise += platform.size
					cases++
				}
			}
		} finally {
			source.close()
			SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = false
			MapPoiTypes.setDefault(defaultTypes)
			readers.values.forEach { it.close() }
		}
		if (mismatches.isNotEmpty()) {
			fail("${mismatches.size} results not as expected:\n" + mismatches.take(10).joinToString("\n"))
		}
		assertTrue(cases > 50 && compared > 500, "$cases cases, $compared results")
		println(
			"SearchUICoreTest: $cases cases of SearchUICoreTest as expected on ${testPlatformName()}, $compared results; " +
					"with the collator of the platform, $orderedOtherwise results are not the ones expected"
		)
	}

	/**
	 * `SearchUICoreTest.testSearch` of OsmAnd-java, with the words compared by [collator], or by the
	 * collator of the platform; the number of results compared.
	 */
	private fun testCase(
		name: String, json: JsonObject, files: List<String>, readers: HashMap<String, BinaryMapIndexReader>,
		collator: KCollator?, mismatches: MutableList<String>
	): Int {
		val settingsJson = json.getValue("settings").jsonObject
		if (settingsJson["disabled"]?.jsonPrimitive?.booleanOrNull == true) {
			return 0
		}
		val phrases = ArrayList<String>()
		json["phrase"]?.jsonPrimitive?.contentOrNull?.let { phrases.add(it) }
		json["phrases"]?.jsonArray?.forEach { phrases.add(it.jsonPrimitive.content) }
		val results = phrases.map { ArrayList<String>() }
		parseResults(json, "results", results)
		parseResults(json, "extra-results", results)
		val s = SearchSettings.parseJSON(settingsJson)
		val useData = settingsJson["useData"]?.jsonPrimitive?.booleanOrNull ?: true
		if (useData && files.isNotEmpty()) {
			s.setOfflineIndexes(files.map { file -> readers.getOrPut(file) { BinaryMapIndexReader(obf(file)) } })
		}
		val core = SearchUICore(MapPoiTypes.getDefault(), "en", false)
		core.init()
		val emptyPhrase = if (collator == null) SearchPhrase.emptyPhrase(s) else SearchPhrase.emptyPhrase(s, collator)
		var compared = 0
		for (k in phrases.indices) {
			val text = phrases[k]
			var phrase: SearchPhrase
			var searchResults: List<SearchResult>
			val arr = text.split(Regex("[\\\\{}]"))
			if (arr.isNotEmpty() && arr[0] == "POI_TYPE:") {
				phrase = emptyPhrase.generateNewPhrase("", s)
				searchResults = searchResults(core, phrase)
				for (searchResult in searchResults) {
					if (arr.size > 1 && arr[1] == searchResult.localeName) {
						phrase = emptyPhrase.generateNewPhrase(if (arr.size > 2) arr[2] else "", s)
						phrase.getWords().add(SearchWord(searchResult.localeName!!, searchResult))
						searchResults = searchResults(core, phrase)
						break
					}
				}
			} else {
				phrase = emptyPhrase.generateNewPhrase(text, s)
				searchResults = searchResults(core, phrase)
			}
			for (i in results[k].indices) {
				var expected = results[k][i]
				if (expected.indexOf('[') != -1) {
					expected = expected.substring(0, expected.indexOf('[')).trim { it <= ' ' }
				}
				expected = expected.removePrefix("@")
				val present = if (i >= searchResults.size) "#MISSING ${i + 1}" else
					SearchUICore.formatSearchResultForTest(true, searchResults[i], phrase)
				if (expected != present) {
					mismatches.add("$name '$text' ${i + 1}: expected '$expected', found '$present'")
				}
				compared++
			}
		}
		return compared
	}

	private fun parseResults(json: JsonObject, tag: String, results: List<MutableList<String>>) {
		val arr = json[tag]?.jsonArray ?: return
		var result = results[0]
		val hasInnerArray = arr.isNotEmpty() && arr[0] is JsonArray
		for (i in arr.indices) {
			if (hasInnerArray) {
				val inner = arr[i] as? JsonArray
				if (inner != null && results.size > i) {
					result = results[i]
					inner.forEach { result.add(it.jsonPrimitive.content) }
				}
			} else {
				result.add((arr[i] as JsonPrimitive).content)
			}
		}
	}

	private fun searchResults(core: SearchUICore, phrase: SearchPhrase): List<SearchResult> {
		val matcher = SearchResultMatcher(ACCEPT_ALL, phrase, 1, AtomicInt(1), -1)
		core.searchInternal(phrase, matcher)
		val collection = SearchResultCollection(phrase)
		collection.addSearchResults(matcher.getRequestResults(), true, true)
		return collection.getCurrentSearchResults()
	}

	// the collections of SearchUICoreGenericTest

	@Test
	fun duplicates() {
		val phrase = originPhrase()
		val cll = SearchResultCollection(phrase)
		val rs = ArrayList<SearchResult>()
		val a1 = searchResult(rs, phrase, "a", 100)
		val b2 = searchResult(rs, phrase, "b", 200)
		val b1 = searchResult(rs, phrase, "b", 100)
		searchResult(rs, phrase, "a", 100)
		cll.addSearchResults(rs, true, true)
		assertEquals(3, cll.getCurrentSearchResults().size)
		assertSame(a1, cll.getCurrentSearchResults()[0])
		assertSame(b1, cll.getCurrentSearchResults()[1])
		assertSame(b2, cll.getCurrentSearchResults()[2])
	}

	@Test
	fun noResort() {
		val phrase = originPhrase()
		val cll = SearchResultCollection(phrase)
		val rs = ArrayList<SearchResult>()
		val a1 = searchResult(rs, phrase, "a", 100)
		cll.addSearchResults(rs, false, true)
		rs.clear()
		val b2 = searchResult(rs, phrase, "b", 200)
		cll.addSearchResults(rs, false, true)
		rs.clear()
		val b1 = searchResult(rs, phrase, "b", 100)
		cll.addSearchResults(rs, false, true)
		rs.clear()
		searchResult(rs, phrase, "a", 100)
		cll.addSearchResults(rs, false, true)
		assertEquals(3, cll.getCurrentSearchResults().size)
		assertSame(a1, cll.getCurrentSearchResults()[0])
		assertSame(b2, cll.getCurrentSearchResults()[1])
		assertSame(b1, cll.getCurrentSearchResults()[2])
	}

	@Test
	fun noResortDuplicate() {
		val phrase = originPhrase()
		val cll = SearchResultCollection(phrase)
		val rs = ArrayList<SearchResult>()
		val a1 = searchResult(rs, phrase, "a", 100)
		val b2 = searchResult(rs, phrase, "b", 200)
		val b1 = searchResult(rs, phrase, "b", 100)
		cll.addSearchResults(rs, false, true)
		rs.clear()
		searchResult(rs, phrase, "a", 100)
		cll.addSearchResults(rs, false, true)
		assertEquals(3, cll.getCurrentSearchResults().size)
		assertSame(a1, cll.getCurrentSearchResults()[0])
		assertSame(b1, cll.getCurrentSearchResults()[1])
		assertSame(b2, cll.getCurrentSearchResults()[2])
	}

	private fun originPhrase(): SearchPhrase {
		SearchPhraseTest.setUp
		val ss = SearchSettings(null as SearchSettings?).setOriginalLocation(KLatLon(0.0, 0.0))
		return SearchPhrase.emptyPhrase(ss)
	}

	private fun searchResult(rs: MutableList<SearchResult>, phrase: SearchPhrase, text: String, dist: Int): SearchResult {
		val res = SearchResult(phrase)
		res.localeName = text
		val d1 = KMapUtils.getDistance(0.0, 0.0, 0.0, 1.0)
		res.location = KLatLon(0.0, dist / d1)
		rs.add(res)
		return res
	}

	// the search as the ui runs it

	/** An api that finds one poi named as what was typed, and notes whether it ever runs twice at once. */
	private class OneResultApi : SearchCoreAPI {
		val running = AtomicInt(0)
		val overlaps = AtomicInt(0)
		val searches = AtomicInt(0)

		override fun getSearchPriority(p: SearchPhrase): Int = 1

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			if (running.incrementAndGet() > 1) {
				overlaps.incrementAndGet()
			}
			searches.incrementAndGet()
			val sr = SearchResult(phrase)
			sr.localeName = phrase.getUnknownSearchPhrase()
			sr.objectType = ObjectType.POI
			sr.location = KLatLon(0.0, 0.0)
			resultMatcher.publish(sr)
			running.decrementAndGet()
			return true
		}

		override fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean = false

		override fun isSearchAvailable(p: SearchPhrase): Boolean = true

		override fun getMinimalSearchRadius(phrase: SearchPhrase): Int = 0

		override fun getNextSearchRadius(phrase: SearchPhrase): Int = 0
	}

	/** What a search published, the object type of each and the name of each result, and the results the core kept then. */
	private class Events(private val core: SearchUICore, private val log: MutableList<String>, private val name: String) :
		ResultMatcher<SearchResult> {
		val finished = AtomicInt(0)

		override fun publish(obj: SearchResult): Boolean {
			val type = obj.objectType
			log.add(
				"$name $type" + when (type) {
					ObjectType.POI -> " " + obj.localeName
					ObjectType.FILTER_FINISHED -> " " + core.getCurrentSearchResult().getCurrentSearchResults().map { it.localeName }
					else -> ""
				}
			)
			if (type == ObjectType.SEARCH_FINISHED) {
				finished.incrementAndGet()
			}
			return true
		}

		override fun isCancelled(): Boolean = false
	}

	@Test
	fun searchRunsInTurn() = runBlocking {
		SearchPhraseTest.setUp
		val core = SearchUICore(MapPoiTypes.getDefault(), "en", false)
		val api = OneResultApi()
		core.registerAPI(api)
		val log = ConcurrentMutableList<String>()
		val started = AtomicInt(0)
		val complete = AtomicInt(0)
		core.setOnSearchStart { started.incrementAndGet() }
		core.setOnResultsComplete { complete.incrementAndGet() }

		// the next search cancels the one before, which has not searched yet
		val first = Events(core, log, "first")
		val second = Events(core, log, "second")
		core.search("caf", false, first)
		core.search("cafe", false, second)
		until { second.finished.get() == 1 }
		// called after the search has published that it finished
		until { complete.get() >= 1 }
		assertEquals(
			listOf("first SEARCH_STARTED", "second SEARCH_STARTED", "second POI cafe", "second SEARCH_API_FINISHED", "second SEARCH_FINISHED"),
			log.toList()
		)
		assertEquals(2, started.get())
		assertEquals(1, complete.get())
		assertEquals(listOf("cafe"), core.getCurrentSearchResult().getCurrentSearchResults().map { it.localeName })
		assertEquals("cafe", core.getPhrase().getFullSearchPhrase())

		// waiting for the next letter, it shows the results kept that match, then searches
		log.clear()
		val third = Events(core, log, "third")
		val fourth = Events(core, log, "fourth")
		core.search("cafe b", true, third)
		until { third.finished.get() == 1 }
		core.search("bar", true, fourth)
		until { fourth.finished.get() == 1 }
		assertEquals(
			listOf(
				"third SEARCH_STARTED", "third FILTER_FINISHED [cafe]", "third POI cafe b", "third SEARCH_API_FINISHED",
				"third SEARCH_FINISHED", "fourth SEARCH_STARTED", "fourth FILTER_FINISHED []", "fourth POI bar",
				"fourth SEARCH_API_FINISHED", "fourth SEARCH_FINISHED"
			),
			log.toList()
		)

		// what throws in a search leaves the searches after it
		log.clear()
		val thrown = AtomicInt(0)
		core.setOnSearchStart {
			if (thrown.incrementAndGet() == 1) {
				throw IllegalStateException("thrown in the search")
			}
		}
		val fifth = Events(core, log, "fifth")
		val sixth = Events(core, log, "sixth")
		core.search("pub", false, fifth)
		until { thrown.get() == 1 }
		core.search("pubs", false, sixth)
		until { sixth.finished.get() == 1 }
		assertEquals(listOf("sixth SEARCH_STARTED", "sixth POI pubs", "sixth SEARCH_API_FINISHED", "sixth SEARCH_FINISHED"), log.toList())

		// the search of one api in the background waits for the search before it
		log.clear()
		val seventh = Events(core, log, "seventh")
		core.search("inn", true, seventh)
		val callback = AtomicInt(0)
		core.shallowSearchAsync(OneResultApi::class, "hotel", null, true, true, core.getSearchSettings()) {
			log.add("shallow " + it?.getCurrentSearchResults()?.map { r -> r.localeName })
			callback.incrementAndGet()
			true
		}
		until { callback.get() == 1 }
		assertEquals(
			listOf(
				"seventh SEARCH_STARTED", "seventh FILTER_FINISHED []", "seventh POI inn", "seventh SEARCH_API_FINISHED",
				"seventh SEARCH_FINISHED", "shallow [hotel]"
			),
			log.toList()
		)
		assertEquals(0, api.overlaps.get())
	}

	private suspend fun until(condition: () -> Boolean) {
		withTimeout(30_000) {
			while (!condition()) {
				delay(5)
			}
		}
	}

	companion object {
		/** How far apart two results can be for the dump to say whether they are the same place. */
		private const val SAME_DEPTH = 3

		private val ACCEPT_ALL = object : ResultMatcher<SearchResult> {
			override fun publish(obj: SearchResult): Boolean = true

			override fun isCancelled(): Boolean = false
		}

		private fun typesWith(poiPhrases: Map<String, String>): MapPoiTypes {
			val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
			MapPoiTypes.setDefault(types)
			types.setPoiTranslator(SearchCoreFactoryTest.Translator(poiPhrases))
			return types
		}

		/** The path of a map the compat tests unpacked, or were given by its full path. */
		internal fun obf(name: String): String {
			// the maps the compat test was given are there by their full paths
			val dir = RoutingTestFixtures.resourcesDir().parent!!.parent!!.parent!! / "build" / "search-obf"
			val path = if (name.startsWith("/")) name else (dir / name).toString()
			if (!FileSystem.SYSTEM.exists(path.toPath())) {
				fail("$path not found: SearchApisCompatTest unpacks the search maps into OsmAnd-java/build/search-obf")
			}
			return path
		}

		private fun settings(f: List<String>, readers: HashMap<String, BinaryMapIndexReader>): SearchSettings {
			val files = if (f[3] == "-") emptyList() else f[3].split(",").map { dumpUnhex(it) }
			val s = SearchSettings.parseJSON(Json.parseToJsonElement(dumpUnhex(f[2])) as JsonObject)
			s.setOfflineIndexes(files.map { name -> readers.getOrPut(name) { BinaryMapIndexReader(obf(name)) } })
			return s
		}

		/** The results of each api, in the order the core ran the apis, each list as the api published it. */
		private fun search(core: SearchUICore, phrase: SearchPhrase): List<List<SearchResult>> {
			val recorded = ArrayList<SearchResult>()
			val recorder = object : ResultMatcher<SearchResult> {
				override fun publish(obj: SearchResult): Boolean {
					recorded.add(obj)
					return true
				}

				override fun isCancelled(): Boolean = false
			}
			core.searchInternal(phrase, SearchResultMatcher(recorder, phrase, 0, AtomicInt(0), -1))
			val byApi = ArrayList<List<SearchResult>>()
			var api = ArrayList<SearchResult>()
			for (r in recorded) {
				val type = r.objectType.toString()
				if (type == "SEARCH_API_FINISHED") {
					byApi.add(api)
					api = ArrayList()
				} else if (!type.startsWith("SEARCH_")) {
					api.add(r)
				}
			}
			if (api.isNotEmpty()) {
				// an api that threw, after it published
				byApi.add(api)
			}
			return byApi
		}

		/** `SearchUICoreCompatTest.canonical`: each api's results by their lines without the weight of the match. */
		private fun canonical(byApi: List<List<SearchResult>>): List<SearchResult> {
			val all = ArrayList<SearchResult>()
			for (api in byApi) {
				all.addAll(api.map { it to resultLine(it, false) }.sortedBy { it.second }.map { it.first })
			}
			return all
		}

		private fun kept(phrase: SearchPhrase, from: List<SearchResult>, resort: Boolean, removeDuplicates: Boolean) =
			SearchResultCollection(phrase).addSearchResults(from, resort, removeDuplicates).getCurrentSearchResults().toList() to from

		/** The results of each api made a collection and merged into the ones before, as the ui does. */
		private fun merged(
			phrase: SearchPhrase, byApi: List<List<SearchResult>>, resort: Boolean, removeDuplicates: Boolean
		): Pair<List<SearchResult>, List<SearchResult>> {
			var all = SearchResultCollection(phrase)
			val from = ArrayList<SearchResult>()
			for (api in byApi) {
				val results = copies(canonical(listOf(api)))
				from.addAll(results)
				val collection = SearchResultCollection(phrase).addSearchResults(results, true, true)
				all = all.combineWithCollection(collection, resort, removeDuplicates)
			}
			return all.getCurrentSearchResults().toList() to from
		}

		/**
		 * `SearchUICoreCompatTest.copies`: the results as the apis published them, for a collection of
		 * their own. A collection changes the results it unites, and the weight of a match stays as it
		 * is first asked for.
		 */
		internal fun copies(results: List<SearchResult>): List<SearchResult> = results.map { r ->
			val c = SearchResult(r.requiredSearchPhrase)
			c.parentSearchResult = r.parentSearchResult
			c.wordsSpan = r.wordsSpan
			c.firstUnknownWordMatches = r.firstUnknownWordMatches
			c.otherWordsMatch = r.otherWordsMatch?.let { words ->
				val collator = r.requiredSearchPhrase.getCollator()
				KTreeSet<String>(Comparator { a, b -> collator.compare(a, b) }).also { it.addAll(words) }
			}
			c.`object` = r.`object`
			c.objectType = r.objectType
			c.file = r.file
			c.priority = r.priority
			c.priorityDistance = r.priorityDistance
			c.location = r.location
			c.preferredZoom = r.preferredZoom
			c.localeName = r.localeName
			c.alternateName = r.alternateName
			c.addressName = r.addressName
			c.cityName = r.cityName
			c.otherNames = r.otherNames?.let { ArrayList(it) }
			c.localeRelatedObjectName = r.localeRelatedObjectName
			c.relatedObject = r.relatedObject
			c.distRelatedObjectName = r.distRelatedObjectName
			c.setImpreciseCoordinates(r.hasImpreciseCoordinates())
			c
		}

		private fun indices(kept: List<SearchResult>, results: List<SearchResult>): String {
			// a result has no equality of its own
			val at = HashMap<SearchResult, Int>()
			results.forEachIndexed { i, r -> at[r] = i }
			return kept.joinToString(",") { (at[it] ?: -1).toString() }.ifEmpty { "-" }
		}

		/** `SearchUICoreCompatTest.finalLines`, for one result. */
		private fun finalLine(r: SearchResult, phrase: SearchPhrase): String =
			resultLine(r, true) + " " + v(r.addressName) + " " +
					v(outcome { SearchUICore.formatSearchResultForTest(true, r, phrase) }) + " " +
					v(outcome { SearchUICore.formatSearchResultForTest(false, r, phrase) })

		/** `SearchUICoreCompatTest.queries`. */
		private fun queries(core: SearchUICore, phrase: SearchPhrase): String =
			outcome { core.isSearchMoreAvailable(phrase) } + " " + outcome { core.getMinimalSearchRadius(phrase) } + " " +
					outcome { core.getNextSearchRadius(phrase) } + " " + describe(core.getUnselectedPoiType()) + " " +
					v(core.getCustomNameFilter())

		/** Whether two texts of the tests differ only in numbers a unit apart in their last digit. */
		private fun roundedAlike(java: String, copy: String): Boolean {
			val number = Regex("[0-9]+\\.[0-9]+")
			if (java.replace(number, "#") != copy.replace(number, "#")) {
				return false
			}
			val a = number.findAll(java).map { it.value }.toList()
			val b = number.findAll(copy).map { it.value }.toList()
			return a.indices.all {
				val unit = 1.0 / 10.0.pow(a[it].length - a[it].indexOf('.') - 1)
				abs(a[it].toDouble() - b[it].toDouble()) <= unit * 1.5
			}
		}

		private fun outcome(c: () -> Any?): String = try {
			c().toString()
		} catch (t: Throwable) {
			"threw:" + t::class.simpleName
		}
	}
}
