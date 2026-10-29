package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.CityType
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.MapObject
import net.osmand.shared.map.OsmandRegions
import net.osmand.shared.osm.AbstractPoiType
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testAppCall
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.SearchUICore
import net.osmand.shared.search.core.ObjectType
import net.osmand.shared.search.core.SearchCoreFactoryTest
import net.osmand.shared.search.core.SearchPhraseTest
import net.osmand.shared.search.core.SearchResult
import net.osmand.shared.search.core.TopIndexFilter
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import net.osmand.shared.search.core.spatial.SpatialTextSearchTest.Companion.LAT_LON
import net.osmand.shared.search.core.spatial.SpatialTextSearchTest.Companion.MAIN
import net.osmand.shared.search.core.spatial.SpatialTextSearchTest.Companion.latLon
import net.osmand.shared.search.core.spatial.SpatialTextSearchTest.Companion.number
import net.osmand.shared.search.core.spatial.SpatialTextSearchTest.Companion.nearlySame
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.dumpHex
import net.osmand.shared.util.dumpHexList
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.dumpUnhexList
import net.osmand.shared.util.openJavaDump
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [SpatialTextSearchAPI] as the search of the app runs it, [SearchUICore] of the spatial search, wherever it runs, on
 * Kotlin/Native as well as the jvm.
 *
 * [sameAsJava] reads what `SpatialTextSearchAPICompatTest` in OsmAnd-java wrote to
 * `../OsmAnd-java/build/spatial-search-api-java.txt`, or wherever `OSMAND_SPATIAL_SEARCH_API_JAVA_DUMP` points: the
 * rows java's search gave to phrases made of the objects of real maps, searched from the center of each map over all
 * of them, some with the types of a search, and the rows of the poi categories and streets it selected. It searches
 * the same, each query by a new core, and holds the copy to java as [SpatialTextSearchTest.Comparison] tells, each row
 * of a spatial result with what the api made of it: the same atoms make the same row. The rows the spatial search does
 * not stand for, the suggestion of a typo, are the same, and so are the rows of the categories and streets selected.
 */
class SpatialTextSearchAPITest {

	private var rulesDir = ""
	private var regions: OsmandRegions? = null
	private val poiPhrases = HashMap<String, String>()
	private val langPhrases = HashMap<String, MutableMap<String, String>>()
	private var readers: List<BinaryMapIndexReader> = emptyList()
	private var center = KLatLon(0.0, 0.0)
	private val onJvm = testPlatformName().startsWith("jvm")
	// the poi categories with the names the languages added, in hex
	private var names: SpatialPoiSearch? = null
	private val languages = HashSet<String>()
	private val languageNames = HashSet<String>()

	@Test
	fun sameAsJava() {
		val source = openJavaDump(
			"SpatialTextSearchAPITest", "OSMAND_SPATIAL_SEARCH_API_JAVA_DUMP", "spatial-search-api-java.txt",
			"SpatialTextSearchAPICompatTest"
		) ?: return
		// the words of the regions in the common words of a phrase
		SearchPhraseTest.setUp
		// the default of the tests before: this test names the poi types its own way
		val defaultTypes = MapPoiTypes.getDefault()
		val comparison = SpatialTextSearchTest.Comparison(
			onJvm, setOf(LAT_LON, MAIN, ROW + LOCATION), UNITED, setOf(ROW + ADDRESS_NAME, ROW + CITY_NAME)
		)
		val javaLines = ArrayList<String>()
		val copyLines = ArrayList<String>()
		var variant = -1
		var others = 0
		val othersDifferent = ArrayList<String>()
		var otherCity = 0
		// what the copy answered, after the lines of java that ask, to see every difference
		val copyDump = testEnvironment("OSMAND_SPATIAL_SEARCH_API_COPY_DUMP")?.let { FileSystem.SYSTEM.sink(it.toPath()).buffer() }
		fun flush() {
			if (javaLines.isEmpty() && copyLines.isEmpty()) {
				return
			}
			copyLines.forEach { copyDump?.writeUtf8(it)?.writeUtf8("\n") }
			if (!onJvm) {
				// on Kotlin/Native a poi in two settlements of one kind is in the other one, see withoutPoiCity
				val twoSettlements = javaLines.any { twoSettlementsOfOneKind(it) }
				val java = javaLines.map { withoutPoiCity(it, twoSettlements) }
				val copy = copyLines.map { withoutPoiCity(it, twoSettlements) }
				if (javaLines != copyLines && java == copy) {
					otherCity++
				}
				javaLines.clear()
				javaLines.addAll(java)
				copyLines.clear()
				copyLines.addAll(copy)
			}
			// the rows of other apis and whether the core left the order of the spatial search, whatever the results
			val ja = javaLines[0].split("\t")
			val ca = copyLines[0].split("\t")
			if (ja[0] == "A" && ca[0] == "A") {
				others++
				if (ja.size != ca.size || ja[8] != ca[8] || ja[7] != ca[7] && (onJvm || !nearlySame(ja[7], ca[7]))) {
					othersDifferent.add("java: ${javaLines[0].take(800)}\ncopy: ${copyLines[0].take(800)}")
				}
			}
			comparison.answer(javaLines, copyLines, variant, languageNames)
			javaLines.clear()
			copyLines.clear()
		}
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				if (f[0] == "A" || f[0] == "R" || f[0] == "S" || f[0] == "B") {
					javaLines.add(line)
					continue
				}
				flush()
				copyDump?.writeUtf8(line)?.writeUtf8("\n")
				when (f[0]) {
					"D" -> rulesDir = dumpUnhex(f[1])
					"G" -> regions = OsmandRegions(dumpUnhex(f[1]))
					"X" -> poiPhrases[dumpUnhex(f[1])] = dumpUnhex(f[2])
					"Y" -> langPhrases.getOrPut(f[1]) { HashMap() }[dumpUnhex(f[2])] = dumpUnhex(f[3])
					"M" -> {
						init()
						readers = dumpUnhexList(f[1]).map { BinaryMapIndexReader(it) }
					}
					"C" -> {
						center = KLatLon(Double.fromBits(f[1].toULong(16).toLong()), Double.fromBits(f[2].toULong(16).toLong()))
						for (lang in langPhrases.keys) {
							addLanguage(lang)
						}
					}
					"Q" -> {
						val text = dumpUnhex(f[1])
						// the settings of SpatialTextSearchTest the api searches with: by default for a complete word
						variant = if (text.endsWith(" ")) 0 else 5
						copyLines.addAll(testAppCall { query(text, f[2]) })
					}
					"T" -> {
						val text = dumpUnhex(f[1])
						variant = if (text.endsWith(" ")) 0 else 5
						val types = f[3].split(",").map { ObjectType.valueOf(it) }
						copyLines.addAll(testAppCall { query(text, f[2], types) })
					}
					"K" -> {
						variant = -1
						copyLines.addAll(testAppCall { select(dumpUnhex(f[1]), dumpUnhex(f[2]), dumpUnhex(f[3]), f[4].toInt()) })
					}
				}
			}
			flush()
		} finally {
			source.close()
			copyDump?.close()
			readers.forEach { it.close() }
			regions?.getReader()?.close()
			MapPoiTypes.setDefault(defaultTypes)
		}
		println(
			"SpatialTextSearchAPITest on ${testPlatformName()}: ${comparison.report()}; " +
					"$otherCity answers the same but for the settlement of a poi"
		)
		if (comparison.different.isNotEmpty()) {
			fail("the copy answers otherwise than java: ${comparison.answers[SpatialTextSearchTest.DIFFERENT]}, the first:\n" +
					comparison.different.joinToString("\n"))
		}
		assertTrue(othersDifferent.isEmpty(), "other rows of ${othersDifferent.size} of $others queries differ:\n" +
				othersDifferent.take(10).joinToString("\n"))
		assertTrue((comparison.lines['A'] ?: 0) > 1000, "queries compared: ${comparison.report()}")
		assertTrue((comparison.lines['R'] ?: 0) > 100000, "rows compared: ${comparison.report()}")
		assertTrue((comparison.lines['B'] ?: 0) > 100, "rows of categories compared: ${comparison.report()}")
	}

	private fun init() {
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		MapPoiTypes.setDefault(types)
		types.setPoiTranslator(SearchCoreFactoryTest.Translator(poiPhrases, langPhrases))
		names = SpatialPoiSearch(types)
	}

	// the names the api adds to the poi categories for the language
	private fun addLanguage(lang: String) {
		if (!languages.add(lang)) {
			return
		}
		val poiSearch = names!!
		val before = SpatialTextSearchTest.names(poiSearch)
		poiSearch.addLanguage(lang)
		for (n in SpatialTextSearchTest.names(poiSearch)) {
			if (n !in before) {
				languageNames.add(dumpHex(n))
			}
		}
	}

	private fun core(lang: String): SearchUICore {
		val core = SearchUICore(MapPoiTypes.getDefault(), lang, false)
		core.init(true, rulesDir)
		core.getSearchSettings().setOfflineIndexes(readers)
		core.getSearchSettings().setRegions(regions)
		return core
	}

	// a query of the search in [lang], with [types] the types of a search or null
	private fun query(text: String, lang: String, types: List<ObjectType>? = null): List<String> {
		val core = core(lang)
		if (types != null) {
			core.updateSettings(core.getSearchSettings().setSearchTypes(*types.toTypedArray()))
		}
		val c = core.immediateSearch(text, center)
		val ctx = context(core)
		val rs = c.getCurrentSearchResults()
		val spatial = ArrayList<SpatialSearchResult>()
		val others = StringBuilder()
		var typo = "-"
		for (i in rs.indices) {
			val r = rs[i]
			val s = r.spatialResult
			if (s != null) {
				spatial.add(s)
				continue
			}
			others.append(i).append(':').append(row(r).replace('\t', '|')).append(';')
			if (r.objectType == ObjectType.SUGGESTION && typo == "-") {
				typo = dumpHex(r.localeName!!)
			}
		}
		val lines = ArrayList<String>()
		lines.add(
			"A\t" + dumpHex(text) + "\t" + typo + "\t" + tokensOf(spatial, words(text)) + "\t-\t-\t" + spatial.size + "\t" +
					(if (others.isEmpty()) "-" else others.toString()) + "\t" + c.isSkipSorting()
		)
		var i = 0
		for (r in rs) {
			val s = r.spatialResult ?: continue
			lines.add("R\t" + i++ + "\t" + SpatialTextSearchTest.result(s, ctx) + "\t" + row(r) + "\t" + united(s))
		}
		return lines
	}

	// the row of [key] found by [text] selected in its query and searched, alone and with [typed] after it, in the radius of
	// [radiusLevel]
	private fun select(text: String, key: String, typed: String, radiusLevel: Int): List<String> {
		val core = core("en")
		val row = core.immediateSearch(text, center).getCurrentSearchResults().firstOrNull { obj(it.`object`) == key }
			?: return listOf("S\t-")
		core.updateSettings(core.getSearchSettings().setRadiusLevel(radiusLevel))
		core.resetPhrase(text)
		core.selectSearchResult(row)
		val selectedText = core.getPhrase().getText(true)
		val lines = ArrayList<String>()
		for (t in listOf(selectedText, selectedText + typed)) {
			val c = core.immediateSearch(t, center)
			val rs = c.getCurrentSearchResults()
			lines.add("S\t" + dumpHex(t) + "\t" + c.isSkipSorting() + "\t" + rs.size)
			for (i in rs.indices) {
				lines.add("B\t" + i + "\t" + row(rs[i]))
			}
		}
		return lines
	}

	companion object {
		// the fields of what the api made of a result in a line of a result, see row, and of the objects united in it
		private const val ROW = 18
		private const val ADDRESS_NAME = 4
		private const val CITY_NAME = 5
		private const val LOCATION = 6
		private const val TWO_SETTLEMENTS = 17
		private const val UNITED = ROW + 18

		// the first field of what the api made of a result in a line, or -1
		private fun rowStart(line: String): Int = when (line[0]) {
			'R' -> ROW
			'B' -> 2
			else -> -1
		}

		private fun twoSettlementsOfOneKind(line: String): Boolean {
			val row = rowStart(line)
			return row >= 0 && line.split("\t").getOrNull(row + TWO_SETTLEMENTS) == "true"
		}

		/**
		 * The line of a result without the settlement of a poi in two settlements of one kind, which on Kotlin/Native may
		 * be the other one: java's HashMap of the tag groups of the poi hands them out in the order of their keys,
		 * Kotlin/Native's in the order they were read, as SearchCoreFactoryTest allows; and, in an answer with such a
		 * poi, [twoSettlements], without the address of every poi, which names the settlement most of them are in.
		 */
		private fun withoutPoiCity(line: String, twoSettlements: Boolean): String {
			val row = rowStart(line)
			if (!twoSettlements || row < 0) {
				return line
			}
			val f = line.split("\t").toMutableList()
			if (f.size <= row + TWO_SETTLEMENTS || f[row] != "POI") {
				return line
			}
			f[row + ADDRESS_NAME] = "*"
			if (f[row + TWO_SETTLEMENTS] == "true") {
				f[row + CITY_NAME] = "*"
			}
			return f.joinToString("\t")
		}

		/** `SpatialTextSearchAPICompatTest.words` of OsmAnd-java. */
		private fun words(text: String): Int = KSearchAlgorithms.splitAndNormalize(text, false).count { it != "." }

		/** A context of the poi types of the api of [core], all [SpatialTextSearchTest.result] asks of one. */
		private fun context(core: SearchUICore): SpatialSearchContext {
			val api = core.getApiByClass(SpatialTextSearchAPI::class)!!
			return SpatialSearchContext(SpatialTextSearchSettings.defaultSettings(), emptyList(), api.poiSearch, null)
		}

		/** `SpatialTextSearchAccess.tokensOf` of OsmAnd-java. */
		private fun tokensOf(results: List<SpatialSearchResult>, words: Int): String {
			val byOrder = HashMap<Int, SpatialSearchToken>()
			for (r in results) {
				for (ref in r.objs) {
					for (t in ref.tokens) {
						if (t.originalOrder !in byOrder) {
							byOrder[t.originalOrder] = t
						}
					}
				}
			}
			val sb = StringBuilder()
			for (i in 0 until maxOf(words, (byOrder.keys.maxOrNull() ?: -1) + 1)) {
				val t = byOrder[i]
				sb.append(if (t == null) "-:$i:-:0:-;" else SpatialTextSearchTest.tokens(listOf(t)))
			}
			return sb.toString()
		}

		/** `SpatialTextSearchAPICompatTest.row` of OsmAnd-java. */
		private fun row(r: SearchResult): String =
			"${r.objectType}\t${hexOrDash(r.localeName)}\t${r.otherNames?.let { dumpHexList(it.toList()) } ?: "-"}\t" +
					"${hexOrDash(r.alternateName)}\t${hexOrDash(r.addressName)}\t${hexOrDash(r.cityName)}\t" +
					"${latLon(r.location)}\t${number(r.priority)}\t${number(r.priorityDistance)}\t${r.preferredZoom}\t" +
					"${r.file?.let { dumpHex(it.getRegionName()) } ?: "-"}\t${obj(r.relatedObject)}\t" +
					"${hexOrDash(r.localeRelatedObjectName)}\t${parent(r.parentSearchResult)}\t" +
					"${r.spatialSearchVisibleLevel}\t${obj(r.`object`)}\t${dumpHexList(r.filterUnknownSearchWord(null))}\t" +
					twoSettlementsOfOneKind(r.`object`, r.requiredSearchPhrase.getSettings()!!.getLang())

		/** `SpatialTextSearchAPICompatTest.twoSettlementsOfOneKind` of OsmAnd-java. */
		private fun twoSettlementsOfOneKind(o: Any?, lang: String?): Boolean {
			val groups = (o as? Amenity)?.getTagGroups() ?: return false
			val names = HashMap<CityType, MutableSet<String>>()
			for (group in groups.values) {
				var translated: String? = null
				var nonTranslated: String? = null
				var type: CityType? = null
				for (tv in group) {
					val tag = tv.tag!!
					if (tag.endsWith("name:$lang")) {
						translated = tv.value
					} else if (tag.endsWith("name")) {
						nonTranslated = tv.value
					} else if (tag == "place") {
						type = CityType.valueFromString(tv.value)
					}
				}
				val name = if (translated.isNullOrEmpty()) nonTranslated else translated
				if (!name.isNullOrEmpty() && type != null && type.storedAsSeparateAdminEntity()) {
					names.getOrPut(type) { HashSet() }.add(name)
				}
			}
			return names.values.any { it.size > 1 }
		}

		// the objects deduplication united in a result, in the order they were
		private fun united(r: SpatialSearchResult): String {
			val united = r.unitedObject ?: return "-"
			val sb = StringBuilder()
			for (o in united.getObjects()) {
				sb.append(obj(o)).append(';')
			}
			return sb.toString()
		}

		private fun parent(p: SearchResult?): String =
			if (p == null) "-" else "${p.objectType},${hexOrDash(p.localeName)},${obj(p.relatedObject)}," +
					hexOrDash(p.localeRelatedObjectName)

		/** `SpatialTextSearchAPICompatTest.object` of OsmAnd-java. */
		private fun obj(o: Any?): String = when (o) {
			null -> "-"
			is MapObject -> SpatialTextSearchTest.mapObject(o)
			is AbstractPoiType -> "T," + dumpHex(o.getKeyName())
			is TopIndexFilter -> "F," + dumpHex(o.getFilterId())
			is BinaryMapIndexReader -> "O," + dumpHex(o.getRegionName())
			is String -> "S," + dumpHex(o)
			else -> "?"
		}

		private fun hexOrDash(s: String?): String = if (s == null) "-" else dumpHex(s)
	}
}
