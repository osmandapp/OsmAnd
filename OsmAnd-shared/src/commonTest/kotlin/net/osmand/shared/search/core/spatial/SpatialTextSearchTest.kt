package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.routing.RoutingTestFixtures
import net.osmand.shared.routing.testAppCall
import net.osmand.shared.routing.testEnvironment
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.core.SearchCoreFactoryTest
import net.osmand.shared.search.core.spatial.SpatialSearchContext.SpatialSearchStats
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import net.osmand.shared.util.dumpBits
import net.osmand.shared.util.dumpFnv
import net.osmand.shared.util.dumpHex
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.dumpUnhexList
import net.osmand.shared.util.openJavaDump
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [SpatialTextSearch] wherever it runs, on Kotlin/Native as well as the jvm.
 *
 * [sameAsJava] reads what `SpatialTextSearchCompatTest` in OsmAnd-java wrote to
 * `../OsmAnd-java/build/spatial-search-java.txt`, or wherever `OSMAND_SPATIAL_SEARCH_JAVA_DUMP` points:
 * phrases made of the objects of real maps, searched from the center of each map with the settings of
 * [settings], the words of each query as the search left them, its combinations, the suggestion of a
 * typo and every result, and the amenities of poi categories; the names of the poi categories, also in the
 * languages it added before the phrases of a map. It searches the same, in the same order, each query by a
 * new [SpatialTextSearch], over the same maps, and holds the copy to java as [Comparison] tells: java walks
 * some of its collections in the order of their hashes, and the copy walks them in the order of the file,
 * of insertion or of the values, so the two agree but for the choices that order makes.
 */
class SpatialTextSearchTest {

	private var rulesDir = ""
	private val poiPhrases = HashMap<String, String>()
	private val langPhrases = HashMap<String, MutableMap<String, String>>()
	private var poiSearch: SpatialPoiSearch? = null
	private var readers: List<BinaryMapIndexReader> = emptyList()
	private var location = KLatLon(0.0, 0.0)
	private val onJvm = testPlatformName().startsWith("jvm")
	// the names the languages added to the poi categories, in hex
	private val languageNames = HashSet<String>()
	// the streets of the results, and those whose settlement does not keep the file it was read from
	private var streets = 0
	private val streetsWithoutFile = ArrayList<String>()

	@Test
	fun sameAsJava() {
		val source = openJavaDump(
			"SpatialTextSearchTest", "OSMAND_SPATIAL_SEARCH_JAVA_DUMP", "spatial-search-java.txt",
			"SpatialTextSearchCompatTest"
		) ?: return
		// the default of the tests before, if there is one: this test names the poi types its own way
		val defaultTypes = runCatching { MapPoiTypes.getDefault() }.getOrNull()
		val comparison = Comparison(onJvm)
		// the answer of java to the query or category before, and of the copy
		val javaLines = ArrayList<String>()
		val copyLines = ArrayList<String>()
		var variant = -1
		// what the copy answered, after the lines of java that ask, to see every difference
		val copyDump = testEnvironment("OSMAND_SPATIAL_SEARCH_COPY_DUMP")?.let { FileSystem.SYSTEM.sink(it.toPath()).buffer() }
		fun flush() {
			if (javaLines.isEmpty() && copyLines.isEmpty()) {
				return
			}
			copyLines.forEach { copyDump?.writeUtf8(it)?.writeUtf8("\n") }
			comparison.answer(javaLines, copyLines, variant, languageNames)
			javaLines.clear()
			copyLines.clear()
		}
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				if (f[0] == "A" || f[0] == "R" || f[0] == "B") {
					javaLines.add(line)
					continue
				}
				flush()
				if (f[0] != "P") {
					copyDump?.writeUtf8(line)?.writeUtf8("\n")
				}
				when (f[0]) {
					"D" -> rulesDir = dumpUnhex(f[1])
					"X" -> poiPhrases[dumpUnhex(f[1])] = dumpUnhex(f[2])
					"Y" -> langPhrases.getOrPut(f[1]) { HashMap() }[dumpUnhex(f[2])] = dumpUnhex(f[3])
					"L" -> addLanguage(f[1])
					"C" -> openMaps(f)
					"Q" -> {
						variant = f[1].toInt()
						copyLines.addAll(testAppCall { query(variant, dumpUnhex(f[2])) })
					}
					"K" -> {
						variant = -1
						copyLines.addAll(testAppCall { category(dumpUnhex(f[1])) })
					}
					"P" -> {
						if (poiSearch == null) {
							init()
						}
						val copy = "P\t" + poiTypes(poiSearch!!)
						copyDump?.writeUtf8(copy)?.writeUtf8("\n")
						comparison.answer(listOf(line), listOf(copy), -1, languageNames)
					}
				}
			}
			flush()
		} finally {
			source.close()
			copyDump?.close()
			readers.forEach { it.close() }
			defaultTypes?.let { MapPoiTypes.setDefault(it) }
		}
		println("SpatialTextSearchTest on ${testPlatformName()}: ${comparison.report()}")
		if (comparison.different.isNotEmpty()) {
			fail("the copy answers otherwise than java: ${comparison.answers[DIFFERENT]}, the first:\n" +
					comparison.different.joinToString("\n"))
		}
		assertTrue(streetsWithoutFile.isEmpty(), "settlements of streets without their file: $streetsWithoutFile")
		assertTrue(streets > 1000, "streets: $streets")
		assertTrue((comparison.lines['A'] ?: 0) > 1000, "queries compared: ${comparison.report()}")
		assertTrue((comparison.lines['R'] ?: 0) > 10000, "results compared: ${comparison.report()}")
	}

	// as SpatialTextSearchAPI adds the language of the app and of the search location
	private fun addLanguage(lang: String) {
		val poiSearch = poiSearch!!
		val before = names(poiSearch)
		poiSearch.addLanguage(lang)
		for (n in names(poiSearch)) {
			if (n !in before) {
				languageNames.add(dumpHex(n))
			}
		}
	}


	/**
	 * What the copy answers to a query, to a category or for the poi types, against java: each answer is
	 * [EXACT] or one of the kinds java's order of hashes makes, counted, or [DIFFERENT], which fails the test.
	 *
	 * Java walks in the order of hashes the blocks of the name index a word read before
	 * (`NameIndexReader.getMatchedPrefixes`), the boundaries of the words (`SpatialSearchContext.groupAtomsByTokens`),
	 * the atomic objects of an intersection (`SpatialSearchResultsList.acceptIntersection`), the objects and masks of
	 * the pipeline (`SpatialPipelineSearch`), the boxes added to a tile tree after it is built
	 * (`HashSkipTileQuadTree.build`) and the poi types it adds the names of a language to
	 * (`SpatialPoiSearch.addLanguage`). With all of them walked as the copy walks them java answers as the copy does,
	 * line by line. As they are, the order decides:
	 * - [JAVA_ORDER]: the order of the atoms of a word, the combinations and the counts of the search, with the
	 *   same results;
	 * - [SECONDARY]: which atoms stand for a result, and what they make of it, as its poi category and the name that
 *   matched it, with the same level, keys, score, words and main object: with the same atoms the result is the same;
	 * - [TIES]: the order of results of equal level, keys and score, the same main objects;
	 * - [TAIL]: which of the objects of words of more atoms than `LIMIT_READ_OBJECTS` each are read, and so the
	 *   results of a query of those words;
	 * - [LANGUAGE]: results of a poi category matched by a name of a language added, in java and not in the copy
	 *   or back: the order of those names in the prefix tree of the categories.
	 * In the last two the results both have, by level, keys and main object, have the same score, and those of the same
	 * atoms are the same.
	 *
	 * On Kotlin/Native the doubles of a line, marked by '~', may be a few ulps off, as the functions of the platform
	 * give the distances, see [nearlySame]; and the coordinates of a result and its box to show by a millionth, see
	 * [shiftedSame]: the reader drops the fraction of a tile or a box worked out from them. [places] are the fields
	 * of a line of a result that hold those coordinates.
	 *
	 * Where a line of a result has the objects deduplication united into it, [united], and the fields those make,
	 * [unitedMakes], the order also decides which duplicates of a result are united into it: [MERGED], with the same
	 * atoms the result is the same but for those.
	 */
	internal class Comparison(
		private val onJvm: Boolean,
		private val places: Set<Int> = setOf(LAT_LON, MAIN),
		private val united: Int = -1,
		private val unitedMakes: Set<Int> = emptySet()
	) {
		/** The lines of java compared, by kind. */
		val lines = HashMap<Char, Int>()
		/** The answers by how they compare. */
		val answers = HashMap<String, Int>()
		var inUlps = 0
		var shifted = 0
		val different = ArrayList<String>()

		fun report(): String =
			"lines $lines; answers $answers; $inUlps lines with doubles off in the last digits, $shifted with " +
					"coordinates or boxes off by up to a millionth"

		fun answer(java: List<String>, copy: List<String>, variant: Int, languageNames: Set<String>) {
			for (l in java) {
				lines[l[0]] = (lines[l[0]] ?: 0) + 1
			}
			val kind = compare(java, copy, variant, languageNames)
			answers[kind] = (answers[kind] ?: 0) + 1
			if (kind == DIFFERENT && different.size < 20) {
				for (i in 0 until maxOf(java.size, copy.size)) {
					val j = java.getOrNull(i) ?: "-"
					val c = copy.getOrNull(i) ?: "-"
					if (j != c) {
						different.add("java: ${j.take(800)}\ncopy: ${c.take(800)}")
						break
					}
				}
			}
		}

		private fun compare(java: List<String>, copy: List<String>, variant: Int, languageNames: Set<String>): String {
			if (java.size == copy.size) {
				var ulps = 0
				var shift = 0
				var all = true
				for (i in java.indices) {
					when (same(java[i], copy[i])) {
						IN_ULPS -> ulps++
						SHIFTED -> shift++
						DIFFERENT_LINE, MERGED_LINE -> all = false
					}
				}
				if (all) {
					inUlps += ulps
					shifted += shift
					return EXACT
				}
			}
			if (variant < 0 || java.isEmpty() || copy.isEmpty()) {
				return DIFFERENT
			}
			val ja = java[0].split("\t")
			val ca = copy[0].split("\t")
			// the input, the suggestion of a typo and the words with as many atoms of each
			if (ja[0] != "A" || ja.size < 7 || ja.size != ca.size || ja[1] != ca[1] || ja[2] != ca[2] ||
				withoutAtomOrder(ja[3]) != withoutAtomOrder(ca[3])
			) {
				return DIFFERENT
			}
			val rj = java.subList(1, java.size)
			val rc = copy.subList(1, copy.size)
			if (rj.size == rc.size) {
				val kinds = rj.indices.map { same(rj[it], rc[it]) }
				if (DIFFERENT_LINE !in kinds) {
					inUlps += kinds.count { it == IN_ULPS }
					shifted += kinds.count { it == SHIFTED }
					return if (MERGED_LINE in kinds) MERGED else JAVA_ORDER
				}
				val fj = rj.map { it.split("\t") }
				val fc = rc.map { it.split("\t") }
				if (fj.indices.all { kinds[it] != DIFFERENT_LINE || primarySame(fj[it], fc[it]) }) {
					return SECONDARY
				}
				if (tiesSame(rj, rc, fj, fc)) {
					return TIES
				}
			}
			if (!sharedRowsSame(rj, rc) || !sharedRowsSame(rc, rj)) {
				return DIFFERENT
			}
			if (tail(ja[3], variant)) {
				return TAIL
			}
			if (onlyLanguage(rj, rc, languageNames)) {
				return LANGUAGE
			}
			return DIFFERENT
		}

		private fun same(java: String, copy: String): Int {
			val kind = when {
				java == copy -> SAME_LINE
				onJvm -> DIFFERENT_LINE
				nearlySame(java, copy) -> IN_ULPS
				shiftedSame(java, copy) -> SHIFTED
				else -> DIFFERENT_LINE
			}
			if (kind != DIFFERENT_LINE || united < 0) {
				return kind
			}
			// the same but for other objects united into it and what they make of it
			val j = java.split("\t").toMutableList()
			val c = copy.split("\t").toMutableList()
			if (j[0] != "R" || j.size != c.size || j.size <= united || j[united] == c[united]) {
				return DIFFERENT_LINE
			}
			for (i in unitedMakes + united) {
				j[i] = ""
				c[i] = ""
			}
			return if (same(j.joinToString("\t"), c.joinToString("\t")) == DIFFERENT_LINE) DIFFERENT_LINE else MERGED_LINE
		}

		// the same result but for what other atoms of it make: the same atoms make the same result
		private fun primarySame(j: List<String>, c: List<String>): Boolean {
			if (j.size != c.size || j.size <= MAIN || j[REFS] == c[REFS]) {
				return false
			}
			for (i in PRIMARY) {
				if (j[i] != c[i] && (onJvm || !nearlySame(j[i], c[i]) && !(i == MAIN && numbersClose(j[i], c[i])))) {
					return false
				}
			}
			return true
		}

		// the same levels, keys and scores in a row, the main objects of those equal in them the same, and the results
		// of the same atoms among them the same
		private fun tiesSame(lj: List<String>, lc: List<String>, j: List<List<String>>, c: List<List<String>>): Boolean {
			fun key(r: List<String>) = if (r.size <= MAIN) "" else r[LEVEL] + "\t" + r[KEY] + "\t" + r[KEY_STRING]
			var start = 0
			while (start < j.size) {
				if (key(j[start]) != key(c[start]) || !scoreSame(j[start], c[start])) {
					return false
				}
				var end = start + 1
				while (end < j.size && key(j[end]) == key(j[start]) && j[end][SCORE] == j[start][SCORE]) {
					end++
				}
				for (i in start until end) {
					if (key(c[i]) != key(j[start]) || !scoreSame(c[i], j[start])) {
						return false
					}
				}
				val mj = (start until end).map { primary(j[it]) }.sorted()
				val mc = (start until end).map { primary(c[it]) }.sorted()
				if (mj != mc) {
					return false
				}
				val byRefs = HashMap<String, String>()
				for (i in start until end) {
					byRefs[c[i][REFS]] = lc[i]
				}
				for (i in start until end) {
					val copy = byRefs[j[i][REFS]]
					if (copy != null && !sameResult(lj[i], copy)) {
						return false
					}
				}
				start = end
			}
			return true
		}

		private fun scoreSame(a: List<String>, b: List<String>): Boolean =
			a[SCORE] == b[SCORE] || !onJvm && nearlySame(a[SCORE], b[SCORE])

		// the same result wherever it is in the list: the line without its number
		private fun sameResult(java: String, copy: String): Boolean =
			same(withoutNumber(java), withoutNumber(copy)) != DIFFERENT_LINE

		private fun withoutNumber(r: String): String = "R\t-" + r.substring(r.indexOf('\t', 2))

		// a result of [one] the [other] has, by its level, keys and main object, has the same score there, and is the same
		// as the one of the same atoms
		private fun sharedRowsSame(one: List<String>, other: List<String>): Boolean {
			val byKey = HashMap<String, MutableList<String>>()
			for (r in other) {
				byKey.getOrPut(resultKey(r)) { ArrayList() }.add(r)
			}
			for (r in one) {
				val same = byKey[resultKey(r)] ?: continue
				val f = r.split("\t")
				var score = false
				for (o in same) {
					val g = o.split("\t")
					score = score || scoreSame(f, g)
					if (f[REFS] == g[REFS] && !sameResult(r, o)) {
						return false
					}
				}
				if (!score) {
					return false
				}
			}
			return true
		}

		// a query each word of which has more atoms than the objects the search reads: of more single objects than
		// those, the pipeline reads the ones of the best prescore, of equal prescore in the order of its objects
		private fun tail(tokens: String, variant: Int): Boolean {
			val words = tokens.split(";").filter { it.isNotEmpty() }
			if (words.isEmpty()) {
				return false
			}
			val atoms = words.minOf { it.split(":")[3].toInt() }
			val limit = settings(variant).LIMIT_READ_OBJECTS
			return limit in 1 until atoms
		}

		// the results one has and the other has not are all of a poi category matched by a name of a language added
		private fun onlyLanguage(java: List<String>, copy: List<String>, languageNames: Set<String>): Boolean {
			val counts = HashMap<String, Int>()
			for (r in java) {
				val k = resultKey(r)
				counts[k] = (counts[k] ?: 0) + 1
			}
			for (r in copy) {
				val k = resultKey(r)
				counts[k] = (counts[k] ?: 0) - 1
			}
			var any = false
			for (r in java + copy) {
				if ((counts[resultKey(r)] ?: 0) != 0) {
					any = true
					if (r.split("\t").getOrNull(MATCHED_NAME) !in languageNames) {
						return false
					}
				}
			}
			return any
		}

		private fun resultKey(r: String): String {
			val f = r.split("\t")
			return if (f.size <= MAIN) r else f[LEVEL] + "\t" + f[KEY] + "\t" + f[KEY_STRING] + "\t" + mainObject(f)
		}

		// what other atoms do not change in a result, the main object without its coordinates
		private fun primary(r: List<String>): String =
			PRIMARY.filter { it != SCORE }.joinToString("\t") { if (it == MAIN) mainObject(r) else r[it] }

		// the main object without its coordinates
		private fun mainObject(r: List<String>): String {
			val m = r[MAIN]
			val i = m.indexOf(",~")
			return if (i < 0) m else m.substring(0, i)
		}

		/**
		 * The same result but for its coordinates and its box to show, by up to a millionth, and its score, which
		 * goes by the distance to them: the reader works out the tile of a street or a house from the coordinates of
		 * its settlement, and the box from the coordinates of the result, and drops the fraction, which the functions
		 * of the platform may cross where java's do not.
		 */
		private fun shiftedSame(java: String, copy: String): Boolean {
			val j = java.split("\t")
			val c = copy.split("\t")
			if (j.size != c.size || j[0] != "R" && j[0] != "B") {
				return false
			}
			var shifted = false
			var score = true
			for (i in j.indices) {
				if (j[i] == c[i] || nearlySame(j[i], c[i])) {
					continue
				}
				val close = when {
					j[0] == "B" -> numbersClose(j[i], c[i])
					i in places -> numbersClose(j[i], c[i])
					i == BOX -> boxClose(j[i], c[i])
					i == SCORE -> {
						score = numbersClose(j[i], c[i])
						continue
					}
					else -> false
				}
				if (!close) {
					return false
				}
				shifted = true
			}
			return score && (shifted || j[0] == "B")
		}
	}

		private fun init() {
		val types = MapPoiTypes(RoutingTestFixtures.resource("poi_types.xml"))
		MapPoiTypes.setDefault(types)
		types.setPoiTranslator(SearchCoreFactoryTest.Translator(poiPhrases, langPhrases))
		poiSearch = SpatialPoiSearch(types)
	}

	private fun openMaps(f: List<String>) {
		readers.forEach { it.close() }
		readers = dumpUnhexList(f[1]).map { BinaryMapIndexReader(it) }
		location = KLatLon(Double.fromBits(f[2].toULong(16).toLong()), Double.fromBits(f[3].toULong(16).toLong()))
	}

	private fun query(variant: Int, phrase: String): List<String> {
		val ctx = SpatialSearchContext(settings(variant), readers, poiSearch!!, if (variant == 1) null else location)
		ctx.stats.printLogs = false
		val lines = ArrayList<String>()
		val res = try {
			SpatialTextSearch(rulesDir).searchAPI(phrase, ctx)
		} catch (e: RuntimeException) {
			lines.add("A\t!\t" + e::class.simpleName)
			return lines
		}
		val mainResults = res.mainResults!!
		lines.add(
			"A\t" + dumpHex(res.input!!) + "\t" + (res.typoSuggestion?.let { dumpHex(it) } ?: "-") + "\t" +
					tokens(res.tokens!!) + "\t" + combinations(res.combinations!!) + "\t" + stats(res.stats!!) + "\t" +
					mainResults.size
		)
		for (i in mainResults.indices) {
			lines.add("R\t$i\t" + result(mainResults[i], ctx))
			// a settlement read with its street keeps the file, as SpatialSearchContextTest of OsmAnd-java checks
			for (o in mainResults[i].getObjects()) {
				if (o is Street) {
					streets++
					if (o.getCity()?.getReferenceFile() !in readers && streetsWithoutFile.size < 10) {
						streetsWithoutFile.add(o.getName() + " in " + o.getCity()?.getName())
					}
				}
			}
		}
		return lines
	}

	private fun category(key: String): List<String> {
		val box = KQuadRect(
			location.longitude - CATEGORY_BOX, location.latitude + CATEGORY_BOX,
			location.longitude + CATEGORY_BOX, location.latitude - CATEGORY_BOX
		)
		val ctx = SpatialSearchContext(
			SpatialTextSearchSettings.searchPoiByCategorySettings(CATEGORY_ZOOM, box), readers, poiSearch!!, location
		)
		ctx.stats.printLogs = false
		val list = SpatialTextSearch(rulesDir).searchPoiByCategory(ctx, key, box, CATEGORY_ZOOM, CATEGORY_LIMIT)
		val sb = StringBuilder()
		for (a in list) {
			sb.append(mapObject(a)).append(',').append(dumpHex(a.getSubType()!!)).append(',')
				.append(latLon(a.getLocation())).append(';')
		}
		return listOf("B\t${list.size}\t$sb")
	}

	companion object {
		private const val CATEGORY_ZOOM = 15
		private const val CATEGORY_BOX = 0.05
		private const val CATEGORY_LIMIT = 100

		// how the answers compare, see Comparison
		internal const val EXACT = "exact"
		internal const val JAVA_ORDER = "java order"
		internal const val SECONDARY = "secondary"
		internal const val TIES = "ties"
		internal const val TAIL = "tail"
		internal const val LANGUAGE = "language"
		internal const val MERGED = "merged"
		internal const val DIFFERENT = "different"

		private const val SAME_LINE = 0
		private const val IN_ULPS = 1
		private const val SHIFTED = 2
		private const val DIFFERENT_LINE = 3
		private const val MERGED_LINE = 4

		// the fields of a line of a result, see result
		private const val LEVEL = 2
		private const val KEY = 3
		private const val KEY_STRING = 4
		private const val SCORE = 5
		internal const val LAT_LON = 7
		private const val MATCHED_NAME = 12
		private const val BOX = 13
		private const val REFS = 14
		internal const val MAIN = 16
		// what other atoms of a result do not change: level, keys, score, the words it leaves, the name of another
		// language, the words it matched, the main object
		private val PRIMARY = intArrayOf(LEVEL, KEY, KEY_STRING, SCORE, 8, 9, 10, MAIN)

		// the words of a query without their atoms, the last of the fields of each: java's order of hashes decides which
		// atoms a word keeps, not only their order
		private fun withoutAtomOrder(tokens: String): String {
			val sb = StringBuilder()
			for (t in tokens.split(";")) {
				sb.append(t.substring(0, maxOf(0, t.lastIndexOf(':')))).append(';')
			}
			return sb.toString()
		}

		// the same text but for the doubles after '~', each within a millionth
		private fun numbersClose(java: String, copy: String): Boolean {
			val a = ArrayList<String>()
			val b = ArrayList<String>()
			return skeleton(java, a) == skeleton(copy, b) && a.size == b.size && a.indices.all {
				val x = Double.fromBits(a[it].toULong(16).toLong())
				val y = Double.fromBits(b[it].toULong(16).toLong())
				abs(x - y) <= 1e-6 * maxOf(abs(x), abs(y))
			}
		}

		// boxes "left,top,right,bottom" of 31-bit tiles, each within a millionth
		private fun boxClose(java: String, copy: String): Boolean {
			val a = java.split(",")
			val b = copy.split(",")
			return a.size == 4 && b.size == 4 && a.indices.all {
				val x = a[it].toLongOrNull() ?: return false
				val y = b[it].toLongOrNull() ?: return false
				abs(x - y) <= 1e-6 * maxOf(abs(x), abs(y))
			}
		}

		/** `SpatialTextSearchCompatTest.settings` of OsmAnd-java. */
		internal fun settings(variant: Int): SpatialTextSearchSettings {
			val s = when (variant) {
				5 -> SpatialTextSearchSettings.suggestionSettings()
				6 -> SpatialTextSearchSettings.searchPoiCategoriesSettings(CATEGORY_ZOOM, null)
				else -> SpatialTextSearchSettings.defaultSettings()
			}
			if (variant == 2) {
				s.SCORE_RANKING = false
			}
			if (variant == 3 || variant == 4) {
				s.DEV_USE_PIPELINE = false
			}
			if (variant == 4) {
				s.DEV_USE_SKIP_HASH_TREE = false
			}
			return s
		}

		/** `SpatialTextSearchAccess.tokens` of OsmAnd-java. */
		internal fun tokens(tokens: List<SpatialSearchToken>): String {
			val sb = StringBuilder()
			for (t in tokens) {
				val atoms = StringBuilder()
				for (a in t.atoms) {
					atoms.append(a.id).append(',').append(a.type).append(',').append(a.nearbyRadius).append(';')
				}
				sb.append(dumpHex(t.word)).append(':').append(t.originalOrder).append(':').append(t.sortedOrder).append(':')
					.append(t.atoms.size).append(':').append(t.broad).append(':').append(t.incomplete).append(':')
					.append(t.deletedAtoms.size()).append(':').append(t.mainNumber).append(':')
					.append(t.numberNamedByOther).append(':').append(t.hasPoiCategoryKeys()).append(':')
					.append(dumpFnv(atoms.toString()).toULong().toString(16)).append(';')
			}
			return sb.toString()
		}

		/** `SpatialTextSearchAccess.names` of OsmAnd-java: the names of all the poi types of the search. */
		internal fun names(poiSearch: SpatialPoiSearch): Set<String> {
			val names = HashSet<String>()
			for (t in poiSearch.byKey.values) {
				names.addAll(t.names)
			}
			return names
		}

		/** `SpatialTextSearchAccess.poiTypes` of OsmAnd-java. */
		internal fun poiTypes(poiSearch: SpatialPoiSearch): String {
			val sb = StringBuilder()
			for (id in 0 until poiSearch.ids.get()) {
				val t = poiSearch.getById(id)
				if (t == null) {
					sb.append("-;")
					continue
				}
				sb.append(dumpHex(t.key)).append(':').append(t.place).append(':')
					.append(t.wikidataId?.let { dumpHex(it) } ?: "-").append(':').append(t.tokensInName).append(':')
				for (n in t.names) {
					sb.append(dumpHex(n)).append(',')
				}
				sb.append(';')
			}
			return sb.toString()
		}

		/** `SpatialTextSearchAccess.combinations` of OsmAnd-java. */
		internal fun combinations(lists: List<SpatialSearchResultsList>): String {
			val sb = StringBuilder()
			for (l in lists) {
				sb.append(dumpHex(l.toString(false))).append(';')
			}
			return sb.toString()
		}

		/** `SpatialTextSearchAccess.stats` of OsmAnd-java. */
		internal fun stats(s: SpatialSearchStats): String =
			"${s.tokenObjs},${s.maxCombinations},${s.sub1FileAtomsTime.intervals},${s.sub1MatchTime.intervals}," +
					"${s.sub2ReadObjTime.intervals},${s.poiByTypeBboxes}"

		/** `SpatialTextSearchAccess.result` of OsmAnd-java. */
		internal fun result(r: SpatialSearchResult, ctx: SpatialSearchContext): String {
			val sb = StringBuilder()
			sb.append(r.visibleLevel).append('\t').append(SpatialSearchResult.compareKey(r)).append('\t')
				.append(dumpHex(SpatialSearchResult.compareKeyString(r))).append('\t').append(number(r.score)).append('\t')
				.append(r.getIdDeduplication()).append('\t').append(latLon(r.getLatLon())).append('\t')
				.append(r.surplusWords).append('\t')
				.append(r.getExtraNameMatch()?.let { dumpHex(it) } ?: "-").append('\t')
				.append(r.matchedTokens()).append('\t')
			val category = r.getPoiCategory(ctx.poiSearch)
			sb.append(category?.let { dumpHex(it.getKey()) } ?: "-").append('\t')
			sb.append(r.getPoiCategoryMatchedName(ctx.poiSearch)?.let { dumpHex(it) } ?: "-").append('\t')
			val bbox = r.getViewBBox31()
			sb.append(if (bbox == null) "-" else "${bbox[0]},${bbox[1]},${bbox[2]},${bbox[3]}").append('\t')
			for (ref in r.objs) {
				val a = ref.atom
				sb.append(a.id).append(',').append(a.type).append(',').append(dumpHex(a.name)).append(',')
				for (t in ref.tokens) {
					sb.append(t.originalOrder).append('.')
				}
				val obj = a.`object`
				val bld = a.bldObject
				sb.append(',').append(ref.otherWordsFound).append(',').append(ref.otherWordsNotFound).append(',')
					.append(if (obj == null) "-" else ObfConstants.getOsmObjectId(obj).toString()).append(',')
					.append(if (bld == null) "-" else dumpHex(bld.getName())).append(',').append(ref.typeOrder(false))
					.append(';')
			}
			sb.append('\t')
			for (o in r.getObjects()) {
				sb.append(mapObject(o)).append(';')
			}
			sb.append('\t')
			val main = r.getMainObject()
			sb.append(if (main == null) "-" else mapObject(main) + "," + latLon(main.getLocation())).append('\t')
			sb.append(dumpHex(r.toString(ctx)))
			return sb.toString()
		}

		internal fun mapObject(o: MapObject): String =
			o::class.simpleName + "," + ObfConstants.getOsmObjectId(o) + "," + dumpHex(o.getName())

		internal fun latLon(l: KLatLon?): String = if (l == null) "-" else number(l.latitude) + "," + number(l.longitude)

		internal fun number(v: Double): String = "~" + dumpBits(v)

		/** The same line but for the doubles after '~', each within a few ulps. */
		internal fun nearlySame(java: String, copy: String): Boolean {
			val a = ArrayList<String>()
			val b = ArrayList<String>()
			return skeleton(java, a) == skeleton(copy, b) && a.size == b.size &&
					a.indices.all { SearchCoreFactoryTest.nearlyEqual(a[it], b[it]) }
		}

		// the line with each double after '~' replaced by '#', the doubles into [doubles]
		private fun skeleton(line: String, doubles: MutableList<String>): String {
			val sb = StringBuilder(line.length)
			var i = 0
			while (i < line.length) {
				val c = line[i++]
				sb.append(c)
				if (c != '~') {
					continue
				}
				var end = i
				while (end < line.length && (line[end] in '0'..'9' || line[end] in 'a'..'f')) {
					end++
				}
				doubles.add(line.substring(i, end))
				sb.append('#')
				i = end
			}
			return sb.toString()
		}
	}
}
