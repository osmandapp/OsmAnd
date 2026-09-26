package net.osmand.shared.search.core

import co.touchlab.stately.concurrency.AtomicInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.Building
import net.osmand.shared.data.City
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.Street
import net.osmand.shared.osm.AbstractPoiType
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiTranslator
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.SearchUICore.SearchResultMatcher
import net.osmand.shared.search.core.SearchCoreFactory.PoiAdditionalCustomFilter
import net.osmand.shared.util.KGeoParsedPoint
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.dumpBits
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.openJavaDump
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The search apis of [SearchCoreFactory] wherever they run, on Kotlin/Native as well as the jvm.
 *
 * [sameAsJava] reads what `SearchApisCompatTest` in OsmAnd-java wrote to
 * `../OsmAnd-java/build/search-apis-java.txt`, or wherever `OSMAND_SEARCH_APIS_JAVA_DUMP` points:
 * the phrases of the cases of `SearchUICoreTest`, typed in, searched at wider radii and selected,
 * with what every api answered and published for each. It runs the apis again here, over the obf
 * files the compat test unpacked into its build directory, and holds the copy to java. Words are
 * compared by the collation keys java gave them.
 *
 * Some things are allowed to differ. The pois one file finds by name may come in another order,
 * as in the compat test. The types of pois come in the order of java's `HashMap` of their names,
 * which Kotlin/Native does not keep, so they are compared in any order, and where the api of pois
 * by type could take one of several types that match as well, its answers are not compared.
 * Coordinates the reader works out from the tiles, distances, and the weights that go through them,
 * may be off in their last digits, and the latitude of a street, a house or a crossing by a unit of
 * zoom 24. A poi in two settlements of one kind may be in the other one: java takes the last one its
 * HashMap of the tag groups of the poi hands out. The last digits of numbers from 10^10 may be other
 * ones where the JDK does not write the fewest digits that name the double. And the api of pois by
 * name may take the other record of a poi stored twice under one id.
 */
class SearchCoreFactoryTest {

	@Test
	fun sameAsJava() {
		val source = openJavaDump(
			"SearchCoreFactoryTest", "OSMAND_SEARCH_APIS_JAVA_DUMP", "search-apis-java.txt", "SearchApisCompatTest"
		) ?: return
		SearchPhraseTest.setUp
		// the types with english names are this test's; the tests after it get the default back
		val defaultTypes = MapPoiTypes.getDefault()
		val poiPhrases = HashMap<String, String>()
		var types: MapPoiTypes? = null
		val dir = RoutingTestFixtures.resourcesDir().parent!!.parent!!.parent!! / "build" / "search-obf"
		val readers = HashMap<String, BinaryMapIndexReader>()
		val stats = Stats()
		var run: CaseRun? = null
		var pending: String? = null
		try {
			while (true) {
				val line = pending ?: source.readUtf8Line() ?: break
				pending = null
				when (line[0]) {
					'X' -> {
						val f = line.substring(2).split("\t")
						poiPhrases[dumpUnhex(f[0])] = dumpUnhex(f[1])
					}
					'C' -> {
						val t = types ?: MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml")).also {
							MapPoiTypes.setDefault(it)
							it.setPoiTranslator(Translator(poiPhrases))
							types = it
						}
						val f = line.substring(2).split("\t")
						val files = if (f[3] == "-") emptyList() else f[3].split(",").map { dumpUnhex(it) }
						val s = SearchSettings.parseJSON(Json.parseToJsonElement(dumpUnhex(f[2])) as JsonObject)
						s.setOfflineIndexes(files.map { name ->
							readers.getOrPut(name) {
								// the maps the compat test was given are there by their full paths
								val path = if (name.startsWith("/")) name else (dir / name).toString()
								if (!FileSystem.SYSTEM.exists(path.toPath())) {
									fail("$path not found: SearchApisCompatTest unpacks the search maps into OsmAnd-java/build/search-obf")
								}
								BinaryMapIndexReader(path)
							}
						})
						val keys = source.readUtf8Line()!!
						assertTrue(keys.startsWith("K "), "the keys of case ${f[0]}")
						SearchCoreFactory.DISPLAY_DEFAULT_POI_TYPES = f[4] == "true"
						run = CaseRun(dumpUnhex(f[1]), s, SearchPhraseTest.JavaCollator(keys.substring(2)), t)
					}
					'Q' -> {
						val answers = ArrayList<String>()
						while (true) {
							val next = source.readUtf8Line() ?: break
							if (next[0] == 'A' || next[0] == 'R') {
								answers.add(next)
							} else {
								pending = next
								break
							}
						}
						run!!.phrase(line, answers, stats)
					}
					'D' -> {
						val f = line.substring(2).split("\t")
						val value = Double.fromBits(f[0].toULong(16).toLong())
						val api = SearchCoreFactory.SearchLocationAndUrlAPI(SearchCoreFactory.SearchAmenityByNameAPI())
						val java = dumpUnhex(f[1])
						val copy = api.formatLatLon(value)
						// from 10^10 up the text shows the sixteenth and seventeenth digits of the double as the
						// platform writes it, and Kotlin/Native picks other ones than the JDK for some doubles
						if (java != copy && abs(value) >= 1e10 && java.toDouble() == copy.toDouble()) {
							stats.otherDigits++
							stats.check("D", java, java, f[0])
						} else {
							stats.check("D", java, copy, f[0])
						}
					}
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
			"SearchCoreFactoryTest: the same as java on ${testPlatformName()}: " +
					stats.compared.entries.sortedBy { it.key }.joinToString { "${it.value} ${it.key}" } +
					"; ${stats.inUlps} results with coordinates or a distance off in the last digits, ${stats.shifted} streets, houses or crossings " +
					"a unit of zoom 24 off, ${stats.ties} choices of a poi type not compared, " +
					"${stats.otherDigits} numbers from 10^10 written with other last digits, " +
					"${stats.otherDuplicates} searches with the other record of a poi stored twice, " +
					"${stats.otherCity} pois in another of two settlements"
		)
	}

	private class Stats {
		val compared = HashMap<Char, Int>()
		val different = ArrayList<String>()
		var inUlps = 0
		var shifted = 0
		var otherDigits = 0
		var otherDuplicates = 0
		var otherCity = 0
		var ties = 0

		fun check(kind: String, java: String, copy: String, what: String) {
			compared[kind[0]] = (compared[kind[0]] ?: 0) + 1
			if (java != copy && different.size < 10) {
				different.add("$kind $what\njava $java\ncopy $copy")
			}
		}
	}

	/** The apis of one case, its phrases and the results kept for selecting them. */
	private class CaseRun(
		val name: String, val settings: SearchSettings, val collator: SearchPhraseTest.JavaCollator, types: MapPoiTypes
	) {
		val apis: List<SearchCoreAPI>
		val phrases = HashMap<Int, SearchPhrase>()
		val results = HashMap<Int, List<List<SearchResult>>>()
		val copyResults = HashMap<Int, List<List<String>>>()
		val javaResults = HashMap<Int, List<List<String>>>()

		init {
			apis = apis(types)
		}

		/** Makes the phrase of [line] as java made it, runs the apis java ran and compares [answers]. */
		fun phrase(line: String, answers: List<String>, stats: Stats) {
			val f = line.substring(2).split("\t")
			val qid = f[0].toInt()
			val parent = f[1].toInt()
			val text = dumpUnhex(f[5])
			val radius = f[6].toInt()
			val phrase = when (f[2]) {
				"T" -> {
					val s = if (radius == 1) settings else settings.setRadiusLevel(radius)
					SearchPhrase.emptyPhrase(s, collator).generateNewPhrase(text, s)
				}
				else -> {
					val p = phrases[parent]
					val api = f[3].toInt()
					// the result as it was when found: the streets of a settlement get their houses later
					val javaLine = javaResults[parent]?.get(api)?.get(f[4].toInt())
					val lines = copyResults[parent]?.get(api)
					var index = lines?.indexOf(javaLine) ?: -1
					if (index < 0 && lines != null && javaLine != null) {
						index = lines.indexOfFirst { close(javaLine, it) != DIFFERENT }
					}
					if (index < 0 && lines != null && javaLine != null) {
						index = lines.indexOfFirst { close(withoutSubtype(javaLine), withoutSubtype(it)) != DIFFERENT }
					}
					if (index < 0 && lines != null && javaLine != null) {
						index = lines.indexOfFirst { close(withoutCity(javaLine), withoutCity(it)) != DIFFERENT }
					}
					val res = if (index < 0) null else results.getValue(parent)[api][index]
					if (p == null || res == null) {
						stats.check("S", "$javaLine", "no such result", "$name ${f[0]}")
						return
					}
					if (f[2] == "S") {
						p.selectWord(res, p.getSettings())
					} else {
						val np = p.generateNewPhrase(text, p.getSettings())
						np.getWords().add(SearchWord(res.localeName!!, res))
						np
					}
				}
			}
			for (w in phrase.getWords()) {
				val file = w.getResult()?.file
				if (file != null) {
					phrase.selectFile(file)
				}
			}
			phrase.sortFiles()
			phrases[qid] = phrase
			val found = ArrayList<List<SearchResult>>()
			val copyFound = ArrayList<List<String>>()
			val javaFound = ArrayList<List<String>>()
			if (f[8] == "false") {
				// run as java ran it, for what the apis keep for the phrases after it; its answers are not in the dump
				for (api in apis) {
					apiLine(api, phrase, ArrayList())
				}
				return
			}
			var k = 0
			while (k < answers.size) {
				val a = answers[k].substring(2).split("\t")
				val api = a[1].toInt()
				var end = k + 1
				while (end < answers.size && answers[end][0] == 'R') {
					end++
				}
				val javaLines = answers.subList(k + 1, end).map { it.substring(2).split("\t", limit = 4)[3] }
				while (found.size < api) {
					found.add(emptyList())
					copyFound.add(emptyList())
					javaFound.add(emptyList())
				}
				val published = ArrayList<SearchResult>()
				val copyLine = apiLine(apis[api], phrase, published) + "\t" + extras(apis[api], phrase) + "\t" + a[4]
				val copyLines = published.map { resultLine(it) }
				val what = "$name ${f[0]} ${API_NAMES[api]} ${phrase}"
				if (a[4].startsWith("tie")) {
					stats.ties++
				} else {
					stats.check("A", a[2] + "\t" + a[3] + "\t" + a[4], copyLine, what)
					compareResults(api, javaLines, copyLines, stats, what)
				}
				found.add(published)
				copyFound.add(copyLines)
				javaFound.add(javaLines)
				k = end
			}
			if (f[7] == "true") {
				results[qid] = found
				copyResults[qid] = copyFound
				javaResults[qid] = javaFound
			}
		}

		private fun compareResults(api: Int, java: List<String>, copy: List<String>, stats: Stats, what: String) {
			var j = inOrder(api, java)
			var c = inOrder(api, copy)
			if (api == 0 && j.size == c.size && j.indices.any { j[it] != c[it] && close(j[it], c[it]) == DIFFERENT }) {
				val jm = j.map { withoutSubtype(it) }.sorted()
				val cm = c.map { withoutSubtype(it) }.sorted()
				if (jm.indices.all { jm[it] == cm[it] || close(jm[it], cm[it]) != DIFFERENT }) {
					// a poi stored twice under one id, which the api takes once: the one the reader hands it first
					stats.otherDuplicates++
					j = jm
					c = cm
				}
			}
			if (j.size != c.size) {
				stats.check("R", "${j.size} results", "${c.size} results", what)
				return
			}
			for (r in j.indices) {
				val close = if (j[r] == c[r]) SAME else close(j[r], c[r])
				if (close == IN_ULPS) {
					stats.inUlps++
				} else if (close == SHIFTED) {
					stats.shifted++
				}
				if (close == DIFFERENT && j[r].startsWith("POI ") && close(withoutCity(j[r]), withoutCity(c[r])) != DIFFERENT) {
					// a poi in two places of one kind is in the last one java's HashMap of them hands out
					stats.otherCity++
					stats.check("R", j[r], j[r], what)
					continue
				}
				stats.check("R", j[r], if (close == DIFFERENT) c[r] else j[r], "$what result $r")
			}
		}
	}

	/** The english names of the poi types, as `SearchUICoreTest` gives them. */
	internal class Translator(val phrases: Map<String, String>) : PoiTranslator {
		private fun translation(keyName: String): String? {
			val v = phrases["poi_$keyName"] ?: return null
			val ind = v.indexOf(';')
			return if (ind > 0) v.substring(0, ind) else v
		}

		private fun synonyms(keyName: String): String? {
			val v = phrases["poi_$keyName"] ?: return null
			val ind = v.indexOf(';')
			return if (ind > 0) v.substring(ind + 1) else ""
		}

		override fun getTranslation(type: AbstractPoiType): String? {
			val base = type.getBaseLangType()
			if (base != null) {
				return getTranslation(base) + " (" + type.getLang()!!.lowercase() + ")"
			}
			return getTranslation(type.getFormattedKeyName())
		}

		override fun getTranslation(keyName: String): String? = translation(keyName)

		override fun getEnTranslation(type: AbstractPoiType): String? {
			val base = type.getBaseLangType()
			if (base != null) {
				return getEnTranslation(base) + " (" + type.getLang()!!.lowercase() + ")"
			}
			return getEnTranslation(type.getFormattedKeyName())
		}

		override fun getEnTranslation(keyName: String): String? = translation(keyName)

		override fun getSynonyms(type: AbstractPoiType): String? {
			val base = type.getBaseLangType()
			if (base != null) {
				return getSynonyms(base)
			}
			return getSynonyms(type.getFormattedKeyName())
		}

		override fun getSynonyms(keyName: String): String? = synonyms(keyName)

		override fun getAllLanguagesTranslationSuffix(): String = "all languages"
	}

	companion object {
		/** The apis in the order `SearchUICore.init` adds them, and the one of regions it leaves out, told there is no internet. */
		internal fun apis(types: MapPoiTypes): List<SearchCoreAPI> {
			val amenitiesApi = SearchCoreFactory.SearchAmenityByNameAPI()
			val typesApi = SearchCoreFactory.SearchAmenityTypesAPI(types)
			val streetsApi = SearchCoreFactory.SearchBuildingAndIntersectionsByStreetAPI()
			val cityApi = SearchCoreFactory.SearchStreetByCityAPI(streetsApi)
			val cache = SearchCoreFactory.TownCitiesCache()
			return listOf(
				amenitiesApi, SearchCoreFactory.SearchLocationAndUrlAPI(amenitiesApi) { false }, typesApi,
				SearchCoreFactory.SearchAmenityByTypeAPI(types, typesApi), streetsApi, cityApi,
				SearchCoreFactory.SearchAddressByNameAPI(streetsApi, cityApi, false, cache),
				SearchCoreFactory.SearchAddressByNameAPI(streetsApi, cityApi, true, cache),
				SearchCoreFactory.SearchRegionByNameAPI()
			)
		}

		internal val API_NAMES = listOf(
			"SearchAmenityByNameAPI", "SearchLocationAndUrlAPI", "SearchAmenityTypesAPI", "SearchAmenityByTypeAPI",
			"SearchBuildingAndIntersectionsByStreetAPI", "SearchStreetByCityAPI", "SearchAddressByNameAPI",
			"SearchAddressByNameAPI", "SearchRegionByNameAPI"
		)
		private const val TYPES_API = 2

		// the same text as SearchApisCompatTest writes

		/** The line without the settlement of the poi, which is the 14th field. */
		private fun withoutCity(line: String): String {
			val f = line.split(' ').toMutableList()
			if (f.size > 13) {
				f[13] = "*"
			}
			return f.joinToString(" ")
		}

		/** `SearchApisCompatTest.withoutSubtypes`, for one line. */
		private fun withoutSubtype(line: String): String = line.replace(SUBTYPE, "$1*:")

		private val SUBTYPE = Regex("(A[-0-9a-z]+:[^: ]*:)'[^: ]*:")

		/** `SearchApisCompatTest.inOrder`, and the types of pois in any order: java keeps a HashMap's. */
		private fun inOrder(api: Int, results: List<String>): List<String> {
			if (api == TYPES_API) {
				return results.sorted()
			}
			if (api != 0) {
				return results
			}
			val ordered = ArrayList<String>()
			val file = ArrayList<String>()
			for (r in results) {
				if (r.startsWith("SEARCH_API_REGION_FINISHED")) {
					ordered.addAll(file.sorted())
					file.clear()
					ordered.add(r)
				} else {
					file.add(r)
				}
			}
			ordered.addAll(file.sorted())
			return ordered
		}

		private fun apiLine(api: SearchCoreAPI, phrase: SearchPhrase, published: MutableList<SearchResult>): String {
			val recorder = object : ResultMatcher<SearchResult> {
				override fun publish(obj: SearchResult): Boolean {
					published.add(obj)
					return true
				}

				override fun isCancelled(): Boolean = false
			}
			val matcher = SearchResultMatcher(recorder, phrase, 0, AtomicInt(0), -1)
			val available = outcome { api.isSearchAvailable(phrase) }
			val priority = outcome { api.getSearchPriority(phrase) }
			val searched = if (available == "true" && priority != "-1") outcome { api.search(phrase, matcher) } else "-"
			return available + " " + priority + " " + searched + " " + outcome { api.isSearchMoreAvailable(phrase) } + " " +
					outcome { api.getMinimalSearchRadius(phrase) } + " " + outcome { api.getNextSearchRadius(phrase) } + " " +
					matcher.getCount()
		}

		private fun extras(api: SearchCoreAPI, phrase: SearchPhrase): String {
			val sb = StringBuilder()
			if (api is SearchCoreFactory.SearchAmenityByTypeAPI) {
				sb.append(describe(api.getUnselectedPoiType())).append(' ').append(v(api.getNameFilter())).append(' ')
			}
			sb.append(describe(phrase.getUnselectedPoiType()))
			return sb.toString()
		}

		private fun outcome(c: () -> Any?): String = try {
			c().toString()
		} catch (t: Throwable) {
			"threw:" + t::class.simpleName
		}

		private fun resultLine(r: SearchResult): String {
			val type = r.objectType
			val sb = StringBuilder()
			sb.append(type).append(' ')
			if (type != null && type.name.startsWith("SEARCH_")) {
				sb.append(describe(r.`object`)).append(' ').append(describe(r.file))
			} else {
				sb.append(v(r.localeName)).append(' ').append(v(r.alternateName)).append(' ').append(v(r.otherNames)).append(' ')
				sb.append(v(r.localeRelatedObjectName)).append(' ')
				sb.append(describe(r.relatedObject)).append(' ')
				sb.append(dumpBits(r.distRelatedObjectName)).append(' ')
				sb.append(v(r.location)).append(' ')
				sb.append(dumpBits(r.priority)).append(' ')
				sb.append(dumpBits(r.priorityDistance)).append(' ')
				sb.append(r.preferredZoom).append(' ')
				sb.append(describe(r.file)).append(' ')
				sb.append(describe(r.`object`)).append(' ')
				sb.append(v(r.cityName)).append(' ')
				sb.append(v(r.wordsSpan)).append(' ')
				sb.append(r.firstUnknownWordMatches).append(' ')
				val words = r.otherWordsMatch
				sb.append(if (words == null) "null" else v(ArrayList(words))).append(' ')
				sb.append(dumpBits(r.getUnknownPhraseMatchWeight())).append(' ')
				sb.append(r.hasImpreciseCoordinates()).append(' ')
				sb.append(v(r.requiredSearchPhrase.getText(true)))
			}
			sb.append(' ').append(parents(r))
			return sb.toString()
		}

		private fun parents(r: SearchResult): String {
			val sb = StringBuilder()
			var p = r.parentSearchResult
			var depth = 0
			while (p != null && depth < 6) {
				sb.append('<').append(p.objectType).append(':').append(v(p.localeName)).append(':').append(describe(p.`object`))
				p = p.parentSearchResult
				depth++
			}
			return if (sb.isEmpty()) "-" else sb.toString()
		}

		private fun describe(o: Any?): String = when (o) {
			null -> "-"
			is City -> "C" + v(o.getId()) + ":" + v(o.getName()) + ":" + o.getType() + ":" + v(o.getLocation()) + ":" +
					(o.getBbox31()?.contentToString() ?: "null") + ":" + o.getStreets().size
			is Street -> {
				val city = o.getCity()
				"S" + v(o.getId()) + ":" + v(o.getName()) + ":" + v(o.getLocation()) + ":" +
						(if (city == null) "-" else v(city.getId()) + v(city.getName())) + ":" +
						o.getBuildings().size + ":" + o.getIntersectedStreets().size
			}
			is Building -> "B" + v(o.getId()) + ":" + v(o.getName()) + ":" + v(o.getName2()) + ":" +
					o.getInterpolationType() + ":" + o.getInterpolationInterval() + ":" + v(o.getLocation()) + ":" +
					v(o.getLatLon2()) + ":" + v(o.getPostcode())
			is Amenity -> "A" + v(o.getId()) + ":" + (o.getType()?.getKeyName() ?: "-") + ":" + v(o.getSubType()) + ":" +
					v(o.getName()) + ":" + v(o.getLocation())
			is PoiAdditionalCustomFilter -> "T" + o::class.simpleName + ":" + o.getKeyName() +
					o.additionalPoiTypes.map { it.getKeyName() }.sorted()
			is AbstractPoiType -> "T" + o::class.simpleName + ":" + o.getKeyName()
			is TopIndexFilter -> "I" + v(o.getFilterId()) + ":" + v(o.getName())
			is BinaryMapIndexReader -> "R" + o.getFile().name()
			is KLatLon -> "L" + v(o)
			is KGeoParsedPoint -> "G" + dumpBits(o.getLatitude()) + ":" + dumpBits(o.getLongitude()) + ":" + o.getZoom() +
					":" + v(o.getLabel())
			is SearchCoreAPI -> "P" + o::class.simpleName
			is CustomSearchPoiFilter -> "F" + v(o.getFilterId()) + ":" + v(o.getName())
			else -> "?" + o::class.simpleName
		}

		/** `SearchApisCompatTest.v`: text escaped and quoted, the rest as `SearchPhraseCompatTest.str`. */
		private fun v(o: Any?): String = when (o) {
			null -> "null"
			is String -> {
				val sb = StringBuilder("'")
				for (c in o) {
					val e = "\\ \t\n\r,[]:<|".indexOf(c)
					if (e >= 0) {
						sb.append('\\').append("\\stnrcbBol|"[e])
					} else {
						sb.append(c)
					}
				}
				sb.toString()
			}
			is Collection<*> -> o.joinToString("", "[", "]") { v(it) + "," }
			is Double -> dumpBits(o)
			is KLatLon -> "(" + dumpBits(o.latitude) + "," + dumpBits(o.longitude) + ")"
			is Enum<*> -> o.name
			else -> o.toString()
		}

		private const val SAME = 0
		private const val IN_ULPS = 1
		private const val SHIFTED = 2
		private const val DIFFERENT = 3

		/**
		 * Whether two lines are equal but for doubles apart in their last digits, by a ten billionth:
		 * coordinates the reader works out from the tiles, distances, and what goes through them, which
		 * go through the sines and cosines of the platform. A distance between two points an ulp off is
		 * hundreds of ulps off. Streets, their houses and crossings may also be a unit of zoom 24 north:
		 * the reader works the tile of the settlement or the street they are stored from out again from
		 * its latitude and drops the fraction, which loses a unit for one in five, and the platform's
		 * functions lose it on other ones than java's.
		 */
		private fun close(java: String, copy: String): Int {
			val a = BITS.findAll(java).map { it.value }.toList()
			val b = BITS.findAll(copy).map { it.value }.toList()
			if (a.size != b.size || java.replace(BITS, "#") != copy.replace(BITS, "#")) {
				return DIFFERENT
			}
			if (a.indices.all { nearlyEqual(a[it], b[it]) }) {
				return IN_ULPS
			}
			if (!java.startsWith("STREET ") && !java.startsWith("HOUSE ") && !java.startsWith("STREET_INTERSECTION ")) {
				return DIFFERENT
			}
			val latitudes = HashSet<Int>()
			for (m in PAIR.findAll(java)) {
				latitudes.add(BITS.findAll(java.substring(0, m.range.first + 1)).count())
			}
			for (i in a.indices) {
				if (nearlyEqual(a[i], b[i])) {
					continue
				}
				val tiles = abs(tileY(a[i]) - tileY(b[i]))
				if (i !in latitudes || tiles > 1.0 + 1e-6) {
					return DIFFERENT
				}
			}
			return SHIFTED
		}

		private fun nearlyEqual(a: String, b: String): Boolean {
			if (ulps(a, b) <= 3) {
				return true
			}
			val x = Double.fromBits(a.toULong(16).toLong())
			val y = Double.fromBits(b.toULong(16).toLong())
			return abs(x - y) <= 1e-10 * maxOf(abs(x), abs(y))
		}

		private fun tileY(bits: String): Double = KMapUtils.getTileNumberY(24.0, Double.fromBits(bits.toULong(16).toLong()))

		private val BITS = Regex("[0-9a-f]{16}")
		private val PAIR = Regex("\\(([0-9a-f]{16}),([0-9a-f]{16})\\)")

		private fun ulps(a: String, b: String): Long {
			val x = a.toULong(16).toLong()
			val y = b.toULong(16).toLong()
			return if (x > y) x - y else y - x
		}
	}
}
