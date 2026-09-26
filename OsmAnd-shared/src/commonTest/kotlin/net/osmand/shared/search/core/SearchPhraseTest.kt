package net.osmand.shared.search.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.CommonWords
import net.osmand.shared.binary.PoiSubType
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.City
import net.osmand.shared.data.CityType
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.data.Street
import net.osmand.shared.map.OsmandRegions
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.util.KCollator
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTreeSet
import net.osmand.shared.util.dumpBits
import net.osmand.shared.util.dumpHex
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.primaryCollator
import net.osmand.shared.util.readJavaDump
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [SearchPhrase], [SearchResult] and [SearchSettings] wherever they run, on Kotlin/Native as well as
 * the jvm.
 *
 * [sameAsJava] reads what `SearchPhraseCompatTest` in OsmAnd-java wrote to
 * `../OsmAnd-java/build/search-phrase-java.txt`, or wherever `OSMAND_SEARCH_PHRASE_JAVA_DUMP`
 * points: the settings, phrases, results and file choices it made, with java's answers. It makes
 * them again here and holds the copy to those answers; without the dump that check says so and
 * passes. The words the results are matched with are compared by the collation keys java gave
 * them, so that the copy is held to java's collator; the collator of the platform is tried as well
 * and what it changes is counted, not failed. Distances, and the weights that go through them, may
 * be up to three ulps off. The common words take the words of the regions of `regions.ocbf`, the
 * pois their types from the `poi_types.xml` of the tests of OsmAnd-java, and the file choices its
 * obf files, the search ones as `SearchPhraseCompatTest` unpacked them into its build directory.
 */
class SearchPhraseTest {

	@Test
	fun wordsOfAPhrase() {
		setUp
		val settings = SearchSettings(ArrayList()).setLang("en", false)
		val phrase = SearchPhrase.emptyPhrase(settings).generateNewPhrase("main st, 12a ", settings)
		assertEquals("main", phrase.getFirstUnknownSearchWord())
		assertEquals(listOf("st", "12a"), phrase.getUnknownSearchWords())
		assertTrue(phrase.isLastUnknownSearchWordComplete())
		assertEquals("12a", phrase.getLastUnknownSearchWord())
		assertEquals(2, SearchPhrase.countWords("a,b"))
		assertEquals("Frankfurt", SearchPhrase.stripBraces("Frankfurt (Oder)"))
		assertEquals("a  c", SearchPhrase.stripBraces("a (b) c"))
	}

	@Test
	fun treeSetKeepsTheFirstOfEqualElements() {
		val set = KTreeSet<String>(Comparator { a, b -> a.lowercase().compareTo(b.lowercase()) })
		assertTrue(set.add("b"))
		assertTrue(set.add("a"))
		assertFalse(set.add("A"))
		assertEquals(listOf("a", "b"), set.toList())
		assertTrue(set.contains("B"))
		assertTrue(set.remove("B"))
		assertEquals(listOf("a"), set.toList())
	}

	@Test
	fun sameAsJava() {
		val lines = readJavaDump(
			"SearchPhraseTest", "OSMAND_SEARCH_PHRASE_JAVA_DUMP", "search-phrase-java.txt", "SearchPhraseCompatTest"
		) ?: return
		setUp
		val settings = HashMap<Int, SearchSettings>()
		val phrases = HashMap<Int, Pair<Int, String>>()
		val compared = HashMap<Char, Int>()
		val different = ArrayList<String>()
		var distancesInUlps = 0
		var orderedOtherwise = 0
		var matchedOtherwise = 0
		val matchedOtherwiseExamples = ArrayList<String>()
		val copyLines = StringBuilder()
		val readers = ArrayList<BinaryMapIndexReader>()
		for (line in lines) {
			val f = line.split("\t")
			val copy: String
			val java: String
			when (line[0]) {
				'S' -> {
					val i = f[0].substring(2).toInt()
					val json = Json.parseToJsonElement(dumpUnhex(f[1])) as JsonObject
					settings[i] = SearchSettings.parseJSON(json)
					java = f[2]
					copy = settingsLine(SearchSettings.parseJSON(json))
				}
				'P' -> {
					val i = f[0].substring(2).toInt()
					val text = dumpUnhex(f[2])
					phrases[i] = Pair(f[1].toInt(), text)
					val names = unhexList(f[3])
					java = f[4] + "\t" + f[5]
					copy = phraseLine(phrase(settings, phrases, i), names) + "\t" + staticLine(text)
				}
				'W' -> {
					val p = phrase(settings, phrases, f[0].substring(2).toInt(), JavaCollator(f[3]))
					java = f[2]
					copy = selectedLine(p, build(f[1], p))
				}
				'R' -> {
					val i = f[0].substring(2).toInt()
					java = f[2]
					copy = resultLine(phrase(settings, phrases, i, JavaCollator(f[3])), f[1])
					val withPlatformCollator = resultLine(phrase(settings, phrases, i, primaryCollator()), f[1])
					val drifts = weightFollowsDistance(f[1])
					if (!closeEnough(java, withPlatformCollator, drifts)) {
						if (closeEnough(inTextOrder(java), inTextOrder(withPlatformCollator), drifts)) {
							orderedOtherwise++
						} else {
							matchedOtherwise++
							if (matchedOtherwiseExamples.size < 5) {
								matchedOtherwiseExamples.add("${f[1].take(120)}\n  java $java\n  here $withPlatformCollator")
							}
						}
					}
				}
				'B' -> {
					val lat = Double.fromBits(f[0].substring(2).toULong(16).toLong())
					val lon = Double.fromBits(f[1].toULong(16).toLong())
					java = f[3]
					copy = str(KMapUtils.calculate31BboxUsingRhumb(f[2].toInt(), KLatLon(lat, lon)))
				}
				'V' -> {
					java = f[1]
					copy = dumpHex(TopIndexFilter.getValueKey(dumpUnhex(f[0].substring(2))))
				}
				'T' -> {
					java = f[3] + "\t" + f[4]
					copy = topIndexLine(dumpUnhex(f[0].substring(2)), dumpUnhex(f[1]), unhexList(f[2]))
				}
				'O' -> {
					readers.forEach { it.close() }
					readers.clear()
					val dir = RoutingTestFixtures.resourcesDir().parent!!.parent!!.parent!!
					for (path in f[0].substring(2).split(",").map { (dir / dumpUnhex(it)).toString() }) {
						if (!FileSystem.SYSTEM.exists(path.toPath())) {
							fail("$path not found: SearchPhraseCompatTest unpacks the search maps into OsmAnd-java/build/search-obf")
						}
						readers.add(BinaryMapIndexReader(path))
					}
					continue
				}
				'F' -> {
					val i = f[0].substring(2).toInt()
					val (s, text) = phrases.getValue(i)
					val ss = SearchSettings(settings.getValue(s))
					ss.setOfflineIndexes(if (f[1] == "all") readers else readers.subList(0, readers.size / 2))
					java = f[2]
					copy = filesLine(SearchPhrase.emptyPhrase(ss).generateNewPhrase(text, ss), readers)
				}
				else -> continue
			}
			compared[line[0]] = (compared[line[0]] ?: 0) + 1
			copyLines.append(f[0]).append('\t').append(copy).append('\n')
			if (copy != java) {
				if (line[0] == 'R' && closeEnough(java, copy, weightFollowsDistance(f[1]))) {
					distancesInUlps++
				} else if (different.size < 10) {
					different.add("${f[0]} ${f.getOrNull(1)?.take(200)}\njava $java\ncopy $copy")
				}
			}
		}
		readers.forEach { it.close() }
		assertTrue((compared['F'] ?: 0) > 100, "file choices compared: ${compared['F']}")
		testEnvironment("OSMAND_SEARCH_PHRASE_COPY_DUMP")?.let { out ->
			FileSystem.SYSTEM.write(out.toPath()) { writeUtf8(copyLines.toString()) }
		}
		if (different.isNotEmpty()) {
			fail("different from java:\n" + different.joinToString("\n"))
		}
		assertTrue((compared['R'] ?: 0) > 20000, "results compared: ${compared['R']}")
		println(
			"SearchPhraseTest: the same as java: " + compared.entries.sortedBy { it.key }.joinToString { "${it.value} ${it.key}" } +
					"; $distancesInUlps results with a distance a few ulps off; with the collator of ${testPlatformName()}, " +
					"$orderedOtherwise results keep the words they matched in another order and " +
					"$matchedOtherwise match other words" + matchedOtherwiseExamples.joinToString("") { "\n$it" }
		)
	}

	/**
	 * Whether a weight of the result goes through the distance from a street to a settlement: it
	 * does when a street, or a house on one, lies under a settlement of another name.
	 */
	private fun weightFollowsDistance(spec: String): Boolean {
		val objects = spec.split("|").map { it.split(";")[7].split(":") }
		val streetCities = objects.filter { it[0] == "S" }.map { it[1] }
		return streetCities.isNotEmpty() && objects.any { it[0] == "C" && it[1] !in streetCities }
	}

	/**
	 * The same result line, but for the distances to the result, and the weights that go through a
	 * distance when [weightDrifts], which may be up to three ulps off: they go through the sines
	 * and cosines of the platform.
	 */
	private fun closeEnough(java: String, copy: String, weightDrifts: Boolean): Boolean {
		val j = java.split(" | ").map { it.split(" ") }
		val c = copy.split(" | ").map { it.split(" ") }
		if (j.size != c.size || j.indices.any { j[it].size != c[it].size }) {
			return false
		}
		for (part in j.indices) {
			for (i in j[part].indices) {
				val a = j[part][i]
				val b = c[part][i]
				val isDistance = weightDrifts && part <= 1 && i == 1 || part == 0 && (i == 10 || i == 11)
				if (a != b && !(isDistance && ulps(a, b) <= 3)) {
					return false
				}
			}
		}
		return true
	}

	/** A result line with the words the result matched in the order of their text, not the collator's. */
	private fun inTextOrder(line: String): String = line.split(" | ").mapIndexed { part, text ->
		if (part > 1) text else text.split(" ").mapIndexed { i, token ->
			if (i == 4 && token.startsWith("[")) token.removePrefix("[").removeSuffix("]").split(",").sorted().joinToString(",") else token
		}.joinToString(" ")
	}.joinToString(" | ")

	private fun ulps(a: String, b: String): Long {
		val x = a.toULongOrNull(16)?.toLong() ?: return Long.MAX_VALUE
		val y = b.toULongOrNull(16)?.toLong() ?: return Long.MAX_VALUE
		return if ((x < 0) == (y < 0)) kotlin.math.abs(x - y) else Long.MAX_VALUE
	}

	private fun phrase(
		settings: Map<Int, SearchSettings>, phrases: Map<Int, Pair<Int, String>>, i: Int,
		collator: KCollator = primaryCollator()
	): SearchPhrase {
		val (s, text) = phrases[i] ?: fail("no phrase $i")
		val ss = settings[s] ?: fail("no settings $s")
		return SearchPhrase.emptyPhrase(ss, collator).generateNewPhrase(text, ss)
	}

	/**
	 * Compares words by the collation keys java's collator gave them, so that a phrase compares
	 * them here as it did in java whatever the collator of the platform says.
	 */
	private class JavaCollator(keys: String) : KCollator {
		private val keys = HashMap<String, String>()

		init {
			if (keys != "-") {
				for (entry in keys.split(",")) {
					val (word, key) = entry.split(":")
					this.keys[dumpUnhex(word)] = if (key == "-") "" else key
				}
			}
		}

		override fun compare(source: String, target: String): Int {
			val a = keys[source] ?: fail("java compared no ${dumpHex(source)}")
			val b = keys[target] ?: fail("java compared no ${dumpHex(target)}")
			return a.compareTo(b).coerceIn(-1, 1)
		}

		override fun equals(source: String, target: String): Boolean = compare(source, target) == 0
	}

	// the same lines as SearchPhraseCompatTest writes

	private fun settingsLine(s: SearchSettings): String {
		val sb = StringBuilder()
		for (v in listOf<Any?>(
			s.getOriginalLocation(), s.getRegionLang(), s.getRadiusLevel(), s.getTotalLimit(), s.getAppLang(),
			s.getLang(), s.isTransliterate(), s.getSearchTypes(), s.isCustomSearch(), s.isEmptyQueryAllowed(),
			s.getSearchBBox31(), s.getSortType(), s.isExportObjects(), s.hasRegionPriority(), s.getOfflineIndexes()
		)) {
			sb.append(str(v)).append(' ')
		}
		val export = s.getExportSettings()
		if (export != null) {
			sb.append(str(export.isExportEmptyCities())).append(str(export.isExportBuildings())).append(str(export.getMaxDistance()))
		}
		for (t in listOf("POI", "CITY", "WPT")) {
			sb.append(' ').append(str(s.hasCustomSearchType(ObjectType.valueOf(t))))
		}
		return sb.toString()
	}

	private fun phraseLine(p: SearchPhrase, names: List<String>): String {
		val sb = StringBuilder()
		for (v in listOf<Any?>(
			p.getFullSearchPhrase(), p.getUnknownSearchPhrase(), p.getFirstUnknownSearchWord(), p.getUnknownSearchWords(),
			p.isLastUnknownSearchWordComplete(), p.isFirstUnknownSearchWordComplete(), p.hasMoreThanOneUnknownSearchWord(),
			p.isUnknownSearchWordPresent(), p.getLastUnknownSearchWord(), p.getUnknownWordToSearch(),
			p.isMainUnknownSearchWordComplete(), p.getUnknownWordToSearchBuilding(), p.getTextWithoutLastWord(),
			p.getStringRerpresentation(), p.isEmpty(), p.isNoSelectedType(), p.getWords(), p.getWordLocation(),
			p.getLastTokenLocation(), p.get1km31Rect(), p.getRadiusLevel()
		)) {
			sb.append(str(v)).append(' ')
		}
		sb.append(str(p.getText(true))).append(' ').append(str(p.getText(false))).append(' ')
		sb.append(str(p.getRadiusSearch(1000))).append(' ').append(str(p.getNextRadiusSearch(1000))).append(' ')
		sb.append(str(p.getRadiusBBoxToSearch(5000))).append(' ')
		val others = p.getUnknownSearchWords().size
		for (name in names) {
			sb.append(matches(p.getMainUnknownNameStringMatcher(), name))
			sb.append(matches(p.getFirstUnknownNameStringMatcher(), name))
			sb.append(matches(p.getUnknownWordToSearchBuildingNameMatcher(), name))
			for (i in 0 until others) {
				sb.append(matches(p.getUnknownNameStringMatcher(i), name))
			}
			sb.append(',')
		}
		return sb.toString()
	}

	private fun matches(matcher: NameStringMatcher, name: String): Char = if (matcher.matches(name)) '1' else '0'

	private fun staticLine(text: String): String {
		// the words as java's split gives them to the compat test
		val parts = text.split(" ")
		val words = (if (parts.size == 1) parts else parts.dropLastWhile { it.isEmpty() }).toMutableList()
		return SearchPhrase.countWords(text).toString() + " " +
				str(SearchPhrase.splitWords(text, ArrayList(), SearchPhrase.ALLDELIMITERS)) + " " +
				str(SearchPhrase.splitWords(text, ArrayList(), SearchPhrase.ALLDELIMITERS_WITH_HYPHEN)) + " " +
				str(SearchPhrase.stripBraces(text)) + " " + str(SearchPhrase.stripBraces(words as Collection<String>)) + " " +
				str(SearchPhrase.selectMainUnknownWordToSearch(words)) + " " + str(words)
	}

	private fun selectedLine(phrase: SearchPhrase, res: SearchResult): String {
		val sb = StringBuilder()
		sb.append(str(phrase.countUnknownWordsMatchMainResult(res))).append(' ')
		val left = res.filterUnknownSearchWord(null)
		sb.append(str(left)).append(' ')
		val last = phrase.getLastUnknownSearchWord()
		val lastComplete = phrase.isLastUnknownSearchWordComplete() || !left.contains(last)
		val selected = phrase.selectWord(res, left, lastComplete)
		sb.append(phraseLine(selected, emptyList())).append(" | ")
		val plain = phrase.selectWord(res, phrase.getSettings())
		sb.append(phraseLine(plain, emptyList())).append(" | ")
		val typed = selected.getText(true) + "12 "
		val next = selected.generateNewPhrase(typed, selected.getSettings())
		sb.append(phraseLine(next, emptyList())).append(" | ")
		val shorter = selected.generateNewPhrase(typed.substring(0, typed.length / 2), selected.getSettings())
		sb.append(phraseLine(shorter, emptyList())).append(" | ")
		next.syncWordsWithResults()
		sb.append(str(next.getWords())).append(' ').append(str(next.getExclusiveSearchType()))
		for (t in listOf("CITY", "STREET", "HOUSE", "POI", "UNKNOWN_NAME_FILTER")) {
			sb.append(' ').append(str(next.hasObjectType(ObjectType.valueOf(t))))
			sb.append(str(next.isLastWord(ObjectType.valueOf(t))))
		}
		return sb.toString()
	}

	private fun topIndexLine(tag: String, value: String, values: List<String>): String {
		val types = MapPoiTypes.getDefault()
		val sub = PoiSubType()
		sub.name = tag
		val filter = TopIndexFilter(sub, types, value)
		val sb = StringBuilder()
		sb.append(str(filter.getTag())).append(' ').append(str(filter.getFilterId())).append(' ')
			.append(str(filter.getName())).append(' ').append(str(filter.getIconResource())).append(' ')
			.append(str(filter.getValue())).append(' ')
		for (v in values) {
			sb.append(str(filter.accept(sub, v)))
		}
		sb.append('\t')
		for (other in values) {
			val o = TopIndexFilter(sub, types, other)
			sb.append(filter == o).append(' ').append(filter.hashCode() == o.hashCode()).append(',')
		}
		return sb.toString()
	}

	private fun filesLine(phrase: SearchPhrase, readers: List<BinaryMapIndexReader>): String {
		val sb = StringBuilder()
		for (dt in SearchPhrase.SearchPhraseDataType.entries) {
			for (meters in intArrayOf(0, 1000, 20000, 400000)) {
				sb.append(fileNames(phrase.getRadiusOfflineIndexes(meters, dt), readers)).append(' ')
			}
			sb.append(fileNames(phrase.getRadiusOfflineIndexes(0, 60000, dt), readers)).append(' ')
			sb.append(fileNames(phrase.getRadiusOfflineIndexes(100000, 300000, dt), readers)).append(' ')
		}
		for (r in readers) {
			sb.append(phrase.getRegionPriority(r)).append(',')
		}
		phrase.sortFiles()
		sb.append(' ').append(fileNames(phrase.getOfflineIndexes().iterator(), readers))
		phrase.selectFile(readers[0])
		sb.append(' ').append(fileNames(phrase.getOfflineIndexes().iterator(), readers))
		return sb.toString()
	}

	/** The files as their places in [readers], as `SearchPhraseCompatTest` writes them. */
	private fun fileNames(it: Iterator<BinaryMapIndexReader>, readers: List<BinaryMapIndexReader>): String {
		val sb = StringBuilder("[")
		while (it.hasNext()) {
			val r = it.next()
			sb.append(readers.indexOfFirst { it === r }).append(',')
		}
		return sb.append(']').toString()
	}

	private fun resultLine(p: SearchPhrase, spec: String): String {
		val sb = StringBuilder()
		val res = build(spec, p)
		sb.append(str(p.countUnknownWordsMatchMainResult(res))).append(' ')
		sb.append(resultState(res)).append(' ')
		val location = p.getSettings()!!.getOriginalLocation()
		sb.append(str(res.getSearchDistance(location))).append(' ')
		sb.append(str(res.getSearchDistance(location, 0.001))).append(" | ")

		val res2 = build(spec, p)
		sb.append(str(p.countUnknownWordsMatchMainResult(res2, "Main Street", 2))).append(' ')
		sb.append(resultState(res2)).append(" | ")

		val res3 = build(spec, p)
		val backup = res3.stripBracesNames()
		sb.append(str(backup)).append(' ').append(names(res3)).append(' ')
		res3.restoreBraceNames(backup)
		sb.append(names(res3))
		return sb.toString()
	}

	private fun resultState(res: SearchResult): String {
		val weight = res.getUnknownPhraseMatchWeight()
		val complete = res.getCompleteMatchRes()!!
		return str(weight) + " " + str(complete.allWordsEqual) + str(complete.allWordsInPhraseAreInResult) + " " +
				str(res.getFoundWordCount()) + " " + str(res.getOtherWordsMatch()) + " " +
				str(res.filterUnknownSearchWord(null)) + " " + str(res.getDepth()) + " " +
				str(res.toString()) + " " + str(res.isFullPhraseEqualLocaleName()) + " " + str(res.getResourceType())
	}

	private fun names(res: SearchResult): String = str(res.localeName) + " " + str(res.alternateName) + " " + str(res.otherNames)

	/** The result `SearchPhraseCompatTest` wrote as [spec], its parents after it. */
	internal fun build(spec: String, phrase: SearchPhrase): SearchResult {
		var first: SearchResult? = null
		var child: SearchResult? = null
		val types = MapPoiTypes.getDefault()
		for (levelSpec in spec.split("|")) {
			val f = levelSpec.split(";")
			val r = SearchResult(phrase)
			r.objectType = ObjectType.valueOf(f[0])
			r.localeName = dumpUnhex(f[1])
			r.alternateName = if (f[2] == "-") null else dumpUnhex(f[2])
			r.otherNames = if (f[3] == "-") null else unhexList(f[3])
			r.cityName = if (f[4] == "-") null else dumpUnhex(f[4])
			val location = KLatLon(unbits(f[5]), unbits(f[6]))
			r.location = location
			val o = f[7].split(":")
			when (o[0]) {
				"C" -> r.`object` = city(o)
				"S" -> {
					val street = Street(city(o))
					street.setName(r.localeName)
					street.setLocation(location.latitude, location.longitude)
					r.`object` = street
					if (r.objectType == ObjectType.STREET_INTERSECTION) {
						r.localeRelatedObjectName = r.cityName
						r.relatedObject = street
					}
				}
				"A" -> {
					val a = Amenity()
					a.setType(types.getPoiCategoryByName(dumpUnhex(o[1])))
					a.setSubType(dumpUnhex(o[2]))
					a.setName(r.localeName)
					a.setLocation(location)
					if (o[3] != "-") {
						a.setAdditionalInfo(Amenity.TRAVEL_ELO, dumpUnhex(o[3]))
					}
					r.`object` = a
				}
				"T" -> {
					val category = types.getPoiCategoryByName(dumpUnhex(o[2]))
					r.`object` = when (o[1]) {
						"type" -> types.getAnyPoiTypeByKey(dumpUnhex(o[2]))
						"category" -> category
						else -> category!!.getPoiFilters()[0]
					}
				}
			}
			if (first == null) {
				first = r
			} else {
				child!!.parentSearchResult = r
			}
			child = r
		}
		return first!!
	}

	private fun city(o: List<String>): City {
		val city = City(CityType.CITY)
		city.setName(dumpUnhex(o[1]))
		city.setLocation(unbits(o[2]), unbits(o[3]))
		if (o[4] != "-") {
			city.setBbox31(o[4].split(",").map { it.toInt() }.toIntArray())
		}
		return city
	}

	/** Any value, written as `SearchPhraseCompatTest.str` writes it. */
	private fun str(v: Any?): String = when (v) {
		null -> "null"
		is String -> dumpHex(v)
		is Double -> dumpBits(v)
		is KLatLon -> "(" + dumpBits(v.latitude) + "," + dumpBits(v.longitude) + ")"
		is KQuadRect -> "(" + dumpBits(v.left) + "," + dumpBits(v.top) + "," + dumpBits(v.right) + "," + dumpBits(v.bottom) + ")"
		is Enum<*> -> v.name
		is SearchWord -> str(v.getWord()) + ":" + str(v.getType()) + ":" + str(v.getLocation())
		is Collection<*> -> v.joinToString(",", "[", ",]") { str(it) }.let { if (v.isEmpty()) "[]" else it }
		is Array<*> -> v.joinToString(",", "[", ",]") { str(it) }.let { if (v.isEmpty()) "[]" else it }
		else -> v.toString()
	}

	private fun unbits(s: String): Double = Double.fromBits(s.toULong(16).toLong())

	private fun unhexList(h: String): List<String> = if (h == "-") emptyList() else h.split(",").map { dumpUnhex(it) }

	companion object {
		internal val setUp: Unit by lazy {
			val regions = testEnvironment("OSMAND_REGIONS_OCBF") ?: "../OsmAnd-java/src/main/resources/net/osmand/map/regions.ocbf"
			if (!FileSystem.SYSTEM.exists(regions.toPath())) {
				fail("regions.ocbf not found at $regions; :OsmAnd-java:processResources downloads it, or set OSMAND_REGIONS_OCBF")
			}
			CommonWords.osmandRegions = OsmandRegions(regions)
			// the phrase ranks its words by the default instance; it has to hold the words of the regions
			if (CommonWords.getInstance().getFrequentlyUsed("gelderland") == -1) {
				fail("the common words were built before the regions were set")
			}
			MapPoiTypes.setDefault(MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml")))
		}
	}
}
