package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.AddressNameIndexDataAtom
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.NameIndexReader
import net.osmand.shared.binary.NameIndexReader.NameIndexReaderMatcher
import net.osmand.shared.binary.NameIndexReader.PrefixNameValue
import net.osmand.shared.binary.OsmAndPoiNameIndexData
import net.osmand.shared.binary.OsmAndPoiNameIndexDataAtom
import net.osmand.shared.data.Building
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.routing.testPlatformName
import net.osmand.shared.search.AmenitySearcherTest
import net.osmand.shared.search.core.SearchCoreFactoryTest
import net.osmand.shared.search.core.spatial.SpatialSearchContext.SpatialSearchStats
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtom
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtomXY
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import net.osmand.shared.search.rules.SearchModRules
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.dumpBits
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.dumpHex
import net.osmand.shared.util.dumpHexList
import net.osmand.shared.util.dumpUnhex
import net.osmand.shared.util.dumpUnhexList
import net.osmand.shared.util.openJavaDump
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * [SpatialSearchToken] wherever it runs, on Kotlin/Native as well as the jvm.
 *
 * [sameAsJava] reads what `SpatialSearchTokenCompatTest` in OsmAnd-java wrote to
 * `../OsmAnd-java/build/search-tokens-java.txt`, or wherever `OSMAND_SEARCH_TOKENS_JAVA_DUMP` points:
 * the words of queries over the name indexes of the search maps of the tests, what each answered on
 * the keys and the names of the index, the atoms added to words with what each add answered and
 * what the words kept, the places of the atoms, the statistics as text and the settings of the
 * search. It asks the copy the same, in the same order over the same files, and holds it to java
 * line by line: on the jvm bit by bit; on Kotlin/Native the size of a place and the location of an
 * atom may be a few ulps off, as the functions of the platform give them, see [nearlySame].
 */
class SpatialSearchTokenTest {

	private lateinit var rules: SearchModRules
	private var locales: List<String> = emptyList()
	private var reader: BinaryMapIndexReader? = null
	private var mapLocale = ""
	private var address = false
	private var blocks: Map<Long, PrefixNameValue> = emptyMap()
	private var keys: List<String> = emptyList()
	private var names: List<String> = emptyList()
	private var token: SpatialSearchToken? = null
	// the same word on its own, matched as a whole word: the keys a word matched are cached, whatever broad was then
	private var broadToken: SpatialSearchToken? = null
	private val added = ArrayList<NameIndexAtom>()
	private val onJvm = testPlatformName().startsWith("jvm")

	@Test
	fun sameAsJava() {
		val source = openJavaDump(
			"SpatialSearchTokenTest", "OSMAND_SEARCH_TOKENS_JAVA_DUMP", "search-tokens-java.txt",
			"SpatialSearchTokenCompatTest"
		) ?: return
		val compared = HashMap<Char, Int>()
		val differentByKind = HashMap<Char, Int>()
		val different = ArrayList<String>()
		var inUlps = 0
		try {
			while (true) {
				val line = source.readUtf8Line() ?: break
				val f = line.split("\t")
				val kind = f[0][0]
				val copy = answer(kind, f, line)
				compared[kind] = (compared[kind] ?: 0) + 1
				if (copy == line) {
					continue
				}
				// the size of a place and the location of an atom go through the sines and logarithms of the platform
				if (kind in PLACE_LINES && !onJvm && nearlySame(line, copy)) {
					inUlps++
					continue
				}
				differentByKind[kind] = (differentByKind[kind] ?: 0) + 1
				if (different.size < 20) {
					different.add("java: ${line.take(600)}\ncopy: ${copy.take(600)}")
				}
			}
		} finally {
			source.close()
			reader?.close()
		}
		println("SpatialSearchTokenTest: compared $compared; $inUlps lines of places and atoms with a size or a location off " +
				"in the last digits")
		if (different.isNotEmpty()) {
			fail("the copy answers otherwise than java: $differentByKind, the first:\n" + different.joinToString("\n"))
		}
		assertTrue((compared['T'] ?: 0) > 3000, "words compared: $compared")
		assertTrue((compared['X'] ?: 0) > 10000, "places compared: $compared")
	}

	/** The line the copy writes for [line] of java: the inputs of java and the answers of the copy. */
	private fun answer(kind: Char, f: List<String>, line: String): String = when (kind) {
		'D' -> {
			rules = SearchModRules.forSearch(dumpUnhex(f[1]))
			line
		}
		'L' -> {
			locales = dumpUnhexList(f[1])
			line
		}
		'V' -> {
			val a = tilePlace(f[1].toInt(), f[2].toLong())
			val b = tilePlace(f[3].toInt(), f[4].toLong())
			"V\t${f[1]}\t${f[2]}\t${f[3]}\t${f[4]}\t" + a.intersects(b) + b.intersects(a) + a.contains(b) + b.contains(a)
		}
		'O' -> {
			val box = parseInts(f[1])
			val enlarged = box.copyOf()
			NameIndexAtomXY.enlargeBbox(f[2].toDouble(), enlarged)
			"O\t${f[1]}\t${f[2]}\t${ints(enlarged)}\t${dumpBits(NameIndexAtomXY.distanceInM(box))}\t" +
					dumpBits(NameIndexAtomXY.distanceInM(box[2], box[3], box[0], box[1]))
		}
		'H' -> "H\t${f[1]}\t${f[2]}\t${f[3]}\t${f[4]}\t" +
				ints(KMapUtils.calc31BboxRhumb(f[1].toInt(), f[2].toInt(), f[3].toInt(), f[4].toInt()))
		'B' -> {
			val data = OsmAndPoiNameIndexData.parseFrom(unhexBytes(f[1]))
			val sb = StringBuilder()
			for (b in data.getAtomsList()) {
				for (variant in 0 until 3) {
					val xy = NameIndexAtomXY(null, b, settings(variant))
					sb.append(xy(xy)).append('|').append(dumpBits(xy.dimensionInM())).append(';')
				}
			}
			"B\t${f[1]}\t$sb"
		}
		'S' -> "S\t" + f[1] + "\t" + stats(f[1] == "0")
		'G' -> "G\t" + f[1] + "\t" + describe(presets()[f[1].toInt()])
		'E' -> {
			val s = SpatialTextSearchSettings.defaultSettings()
			"E\t" + ENLARGE_DIMS.joinToString("") { dumpBits(s.evalEnlargeBoundary(s.ENLARGE_BOUNDARIES, it)) + "," }
		}
		'F' -> {
			reader?.close()
			val r = BinaryMapIndexReader(AmenitySearcherTest.path(dumpUnhex(f[1])))
			reader = r
			mapLocale = dumpUnhex(f[2])
			val own = rules.locales().forMap(region(dumpUnhex(f[1]), r))
			if (own == mapLocale) line else "F the copy reads the map locale $own"
		}
		'I' -> {
			val r = reader!!
			val i = f[1].toInt()
			address = i < r.getAddressIndexes().size
			val nameReader = if (address) NameIndexReader(r.getAddressIndexes()[i])
			else NameIndexReader(r.getPoiIndexes()[i - r.getAddressIndexes().size])
			val all = r.readFullNameIndex(nameReader.setQuery("all", EveryKey())) ?: emptyList()
			blocks = all.associateBy { it.shift }
			previous = NameIndexAtomXY(null, null, null)
			"I\t${f[1]}\t${all.size}"
		}
		'K' -> {
			keys = dumpUnhexList(f[1])
			line
		}
		'N' -> {
			names = dumpUnhexList(f[1])
			line
		}
		'T' -> "T\t${f[1]}\t${f[2]}\t${f[3]}\t${f[4]}\t${f[5]}\t${f[6]}\t" + token(f)
		'M' -> "M\t${f[1]}\t${f[2]}\t" + matchKeys(f[1].toInt(), f[2] == "1")
		'P' -> "P\t${f[1]}\t" + matchNames(dumpUnhex(f[1]))
		'Q' -> "Q\t" + splits()
		'U' -> {
			val t = token!!
			"U\t${t.likelyPartOfBuilding()}\t${t.likelyPartOfBuilding(mapLocale)}"
		}
		'W' -> {
			val t = SpatialSearchToken(rules, 2, dumpUnhex(f[1]), dumpUnhex(f[1]), 0)
			t.numberNamedByOther = f[2] == "1"
			token = t
			added.clear()
			line
		}
		'A' -> {
			val atom = atom(f)
			val res = add(atom)
			added.add(atom)
			f.subList(0, f.size - 1).joinToString("\t") + "\t" + res
		}
		'C' -> {
			val atom = NameIndexAtom(dumpUnhex(f[1]), f[2].toLong(), f[3].toInt())
			"C\t${f[1]}\t${f[2]}\t${f[3]}\t" + add(atom)
		}
		'R' -> "R\t${f[1]}\t" + add(added[f[1].toInt()])
		'Y' -> {
			val copy = NameIndexAtom(added[f[1].toInt()])
			val described = describe(copy)
			val res = add(copy)
			added.add(copy)
			"Y\t${f[1]}\t$described\t$res"
		}
		'Z' -> "Z\t" + summary(token!!)
		'X' -> "X\t${f[1]}\t${f[2]}\t${f[3]}\t" + place(f[1].toLong(), f[2].toInt(), f[3].toInt())
		else -> "unknown line $kind"
	}

	// as SpatialSearchFileCache names the region of a map
	private fun region(path: String, r: BinaryMapIndexReader): String? {
		var region: String? = path.substring(path.lastIndexOf('/') + 1)
		for (a in r.getAddressIndexes()) {
			region = a.name
		}
		for (a in r.getPoiIndexes()) {
			region = if (r.getAddressIndexes().isEmpty()) a.name else region
		}
		return region
	}

	private fun token(f: List<String>): String {
		val min = f[3].toInt()
		val t = SpatialSearchToken(rules, min, dumpUnhex(f[1]), dumpUnhex(f[2]), f[4].toInt())
		t.numberNamedByOther = f[5] == "1"
		if (f[6] == "1") {
			t.addPoiCategoryMatch(3)
		}
		token = t
		broadToken = SpatialSearchToken(rules, min, dumpUnhex(f[1]), dumpUnhex(f[2]), f[4].toInt()).also { it.broad = true }
		val sb = StringBuilder()
		sb.append(dumpHex(t.word)).append('|').append(dumpHex(t.originalWord)).append('|').append(dumpHex(t.wordAligned))
			.append('|').append(dumpHex(t.wordNoDot)).append('|').append(t.incomplete).append('|').append(t.getMainNumber())
			.append('|').append(t.categoryMatchMode).append('|').append(dumpHexList(t.bldWordSplit.toList())).append('|')
			.append(matcher(t.collatorMain)).append('|').append(t.noDotCollatorMain?.let { matcher(it) } ?: "-").append('|')
			.append(t.noHyphenCollatorMain?.let { matcher(it) } ?: "-").append('|')
			.append(t.likelyRef()).append('|').append(t.isOnlyFullMatch()).append('|').append(dumpHex(t.toString()))
			.append('|').append(dumpHex(t.getToken())).append('|').append(t.hasPoiCategoryKeys()).append('|')
			.append(t.likelyPartOfBuilding()).append('|')
		for (type in TYPES) {
			sb.append(t.ruleOwner(type).name).append(',')
		}
		sb.append('|')
		for (l in locales) {
			sb.append(if (t.likelyPartOfBuilding(l)) '1' else '0')
		}
		sb.append(if (t.likelyPartOfBuilding(mapLocale)) '1' else '0').append('|').append(t.likelyPartOfBuilding())
		return sb.toString()
	}

	private fun matcher(m: net.osmand.shared.util.KCollatorStringMatcher): String = dumpHex(m.part.text) + ":" + m.mode

	private fun matchKeys(locale: Int, broad: Boolean): String {
		val t = if (broad) broadToken!! else token!!
		val stats = SpatialSearchStats()
		val m = when (locale) {
			0 -> t.getPrefixMatcher(stats)
			1 -> t.getPrefixMatcher(stats, mapLocale)
			else -> t.getPrefixMatcher(stats, "en_US")
		}
		val bits = StringBuilder()
		for (key in keys) {
			bits.append(if (m.matchKey(key)) '1' else '0')
		}
		return bits.toString() + "\t" + t.isOnlyFullMatch()
	}

	private fun matchNames(locale: String): String {
		val t = token!!
		val bits = StringBuilder()
		for (name in names) {
			val poiTypes = poiTypes(name)
			t.matchName(name, poiTypes, locale, 7)
			for (type in TYPES) {
				bits.append(if (t.matchName(name, poiTypes, locale, type)) '1' else '0')
			}
		}
		return bits.toString()
	}

	private fun splits(): String {
		val t = token!!
		val split = StringBuilder()
		for (name in names) {
			val res = try {
				t.matchSplitName(name)
			} catch (e: IndexOutOfBoundsException) {
				split.append("!;")
				continue
			}
			if (res == null) {
				split.append('-')
			} else {
				split.append(dumpHex(res[0])).append(',').append(dumpHex(res[1]))
			}
			split.append(';')
		}
		return split.toString()
	}

	private fun atom(f: List<String>): NameIndexAtom {
		val name = dumpUnhex(f[1])
		val objId = f[6].toLong()
		val place = f[15].split(":")
		val block = blocks[place[0].toLong()]!!
		val ai = place[1].toInt()
		val a: AddressNameIndexDataAtom? = if (address) block.getAddr()!!.getAtom(ai) else null
		val b: OsmAndPoiNameIndexDataAtom? = if (address) null else block.getPoi()!!.getAtoms(ai)
		val locale = f[14].toInt()
		val atom = NameIndexAtom(name, f[2].toInt(), f[3].toLong(), f[4].toLong(), mapObject(f[5].toInt(), objId),
			f[7] == "1", f[8].toInt(), f[9].toInt(), NameIndexAtomXY(a, b, settings(place[2].toInt())), f[10].toInt(),
			f[11].toInt())
		atom.locale = if (locale < locales.size) locales[locale] else mapLocale
		atom.ruleSpelling = f[12] == "1"
		atom.distinctFoundCnt = f[13].toInt()
		atom.poiTypes = poiTypes(name)
		if (f[16] == "1") {
			atom.bldObject = mapObject(0, objId + 1)
		}
		return atom
	}

	private fun add(atom: NameIndexAtom): Boolean = token!!.addAtom(atom)

	private fun mapObject(kind: Int, id: Long): MapObject? {
		val obj: MapObject = when (kind) {
			0 -> Building()
			1 -> Street(null)
			else -> return null
		}
		obj.setId(id)
		obj.setName("o$id")
		obj.setLocation(40 + id / 1000.0, -74 - id / 1000.0)
		return obj
	}

	private fun summary(t: SpatialSearchToken): String {
		val atoms = t.atoms
		val sb = StringBuilder()
		for (a in atoms) {
			sb.append(describe(a)).append('/').append(atomAnswers(a)).append(';')
		}
		for (i in atoms.indices) {
			val a = atoms[i]
			sb.append(if (t.getAtomToken(a) === a) '=' else '+')
			if (i % 3 == 0) {
				t.addPartialCommonAtom(a, listOf(t), i % 2 == 0)
			}
			if (i % 5 == 0) {
				t.addPartialOtherAtom(a, emptyList(), i % 4 == 0)
			}
		}
		sb.append('|').append(partials(t.getPartialExactMatch())).append('|').append(partials(t.getPartialMatch()))
		t.clearPartialAtoms()
		sb.append('|').append(t.getPartialExactMatch().size).append(t.getPartialMatch().size)
		sb.append('|').append(t.index.size).append('|').append(t.indexByOsmIds.size).append('|')
			.append(t.poiCategoryKeysToAutocomplete.sorted()).append('|').append(t.poiCategoryIds.sorted()).append('|')
			.append(t.hasPoiCategoryKeys()).append('|').append(trees(t)).append('|')
		var i = 0
		while (i < atoms.size) {
			t.removeAtom(atoms[i])
			i += 4
		}
		val deleted = ArrayList<Long>()
		t.deletedAtoms.forEach { deleted.add(it) }
		deleted.sort()
		sb.append(deleted).append('|')
		for (a in atoms) {
			if (a.isGeoArea() || a.coords.bbox31 == null) {
				t.enlargeBbox(a, if (a.isGeoArea()) 0.5 else 1.5)
				sb.append(xy(a.coords)).append(';')
			}
		}
		sb.append('|').append(trees(t))
		return sb.toString()
	}

	private fun partials(list: List<SpatialSearchToken.PartialMatch>): String =
		list.joinToString("") { it.atom.getName() + ":" + it.other!!.size + ":" + it.nonNumericMatch + ";" }

	private fun describe(a: NameIndexAtom): String =
		a.name + "/" + a.type + "/" + a.id + "/" + a.parentid + "/" + a.cityAsStreet + "/" + a.otherWordsCnt + "/" +
				a.otherFoundCnt + "/" + a.distinctFoundCnt + "/" + a.indexInToken + "/" + a.nearbyRadius + "/" +
				a.buildingOrRefInd + "/" + a.locale + "/" + a.ruleSpelling + "/" +
				(a.poiTypes?.toArray()?.joinToString(", ", "[", "]") ?: "null") + "/" + a.elo

	private fun atomAnswers(a: NameIndexAtom): String {
		val l = a.getResultLocation()
		return "${a.isCityStreetName()},${a.isStreetBuilding()},${a.isPoiCategory()},${a.isPostcode()},${a.isBoundary()}," +
				"${a.isGeoArea()},${a.isCityVillage()},${a.isCity()},${a.isStreet()},${a.atomicObject()},${a.isBuilding()}," +
				"${a.isPOI()},${a.isPOIRef()},${a.typeStr()},${dumpHex(a.toString())}," +
				(if (l == null) "null" else dumpBits(l.latitude) + ":" + dumpBits(l.longitude)) + "," +
				"${dumpHex(a.getName())},${a.getObject()?.getId() ?: "null"},${a.getBuilding()?.getId() ?: "null"}"
	}

	private fun trees(t: SpatialSearchToken): String {
		val sb = StringBuilder()
		for (a in t.atoms) {
			t.quadTree.forEachMatch(a.coords.bboxTileZoom, 0, a.coords.bboxTileId) { list -> sb.append(list).append(' ') }
			sb.append(';')
		}
		for (e in t.quadTreeSkip.getTileEntries()) {
			sb.append(e.objId).append(':').append(e.obj).append(':').append(ints(e.bbox31)).append(':').append(e.z)
				.append(':').append(e.tileId).append(';')
		}
		return sb.toString()
	}

	private fun place(shift: Long, ai: Int, variant: Int): String {
		val block = blocks[shift]!!
		val a: AddressNameIndexDataAtom? = if (address) block.getAddr()!!.getAtom(ai) else null
		val b: OsmAndPoiNameIndexDataAtom? = if (address) null else block.getPoi()!!.getAtoms(ai)
		val xy = NameIndexAtomXY(a, b, settings(variant))
		val prev = previous
		val sb = StringBuilder(xy(xy))
		sb.append('|').append(xy.tileIdString()).append('|').append(dumpBits(xy.dimensionInM())).append('|')
			.append(xy.intersects(prev)).append(prev.intersects(xy)).append(xy.contains(prev)).append(prev.contains(xy))
			.append('|')
		val box = prev.bbox31
		if (box != null) {
			sb.append(xy.intersects(box))
		}
		for (mult in ENLARGE_MULTS) {
			val e = NameIndexAtomXY(a, b, settings(variant))
			e.enlargeBbox31(mult)
			sb.append('|').append(xy(e)).append(e.intersects(xy)).append(e.contains(xy)).append(xy.contains(e))
		}
		previous = xy
		return sb.toString()
	}

	// the place before in the index, the whole world before the first
	private var previous = NameIndexAtomXY(null, null, null)

	private fun xy(xy: NameIndexAtomXY): String =
		ints(xy.bbox31) + ":" + xy.bboxTileZoom + ":" + xy.bboxTileId + ":" + xy.x16 + ":" + xy.y16

	private fun ints(a: IntArray?): String = a?.joinToString(", ", "[", "]") ?: "null"

	private fun stats(empty: Boolean): String {
		val s = SpatialSearchStats()
		if (empty) {
			s.doTiming = false
			s.sub1MatchTime.start()
			s.sub1MatchTime.finish()
		} else {
			s.requestTime.time = 123_456_789L
			s.step1Atoms.time = 45_678_901L
			s.tokenObjs = 1_234_567
			s.sub1FileAtomsTime.time = 999_950L
			s.sub1FileAtomsTime.intervals = 42
			s.sub1MatchTime.time = 50_000L
			s.sub1MatchTime.intervals = 12_345
			s.sub1PoiNameBoundaryTime.time = 7_049_999L
			s.step2Compute.time = 1_000_000_000_000L
			s.maxCombinations = 999
			s.sub2LoadObjectsBldTime.time = 1L
			s.sub2ReadObjTime.time = 150_000L
			s.readTableBytes = 5_000_000_000L
			s.readAtomsBytes = 1023
			s.readObjsBytes = 1
		}
		return dumpHex(s.toString()) + "\t" + s.sub1PartMatchTime.intervals + "\t" + s.sub1MatchTime.time
	}

	private fun presets(): List<SpatialTextSearchSettings> {
		val r = KQuadRect(-74.1, 40.9, -73.8, 40.6)
		return listOf(SpatialTextSearchSettings.defaultSettings(),
			SpatialTextSearchSettings.searchPoiByCategorySettings(15, r),
			SpatialTextSearchSettings.searchPoiByCategorySettings(12, null),
			SpatialTextSearchSettings.searchPoiCategoriesSettings(14, r),
			SpatialTextSearchSettings.searchPoiCategoriesSettings(14, null),
			SpatialTextSearchSettings.suggestionSettings())
	}

	/** Every setting by name, as java lists its public fields. */
	private fun describe(s: SpatialTextSearchSettings): String {
		val fields = listOf(
			"SEARCH_SUGGESTION" to s.SEARCH_SUGGESTION, "SUGGESTED_SEARCH_RADIUS_KM" to s.SUGGESTED_SEARCH_RADIUS_KM,
			"TYPO_SUGGESTION" to s.TYPO_SUGGESTION, "LANG_DEDUPLICATE" to s.LANG_DEDUPLICATE,
			"SEARCH_ADDR" to s.SEARCH_ADDR, "SEARCH_POI" to s.SEARCH_POI, "SEARCH_BUILDINGS" to s.SEARCH_BUILDINGS,
			"SEARCH_STREET_INTERSECTIONS" to s.SEARCH_STREET_INTERSECTIONS,
			"SEARCH_POI_INTERSECTIONS" to s.SEARCH_POI_INTERSECTIONS, "SEARCH_POI_CATEGORIES" to s.SEARCH_POI_CATEGORIES,
			"SEARCH_POI_REF" to s.SEARCH_POI_REF,
			"SUGGEST_SEARCH_POI_CATEGORY_WITH_REF" to s.SUGGEST_SEARCH_POI_CATEGORY_WITH_REF,
			"SEARCH_POI_BY_CATEGORY_ONLY" to s.SEARCH_POI_BY_CATEGORY_ONLY,
			"SEARCH_POI_BY_CATEGORY_ZOOM" to s.SEARCH_POI_BY_CATEGORY_ZOOM,
			"SEARCH_POI_BY_CATEGORY_BBOX" to ints(s.SEARCH_POI_BY_CATEGORY_BBOX),
			"AUTO_CLEAR_PREFIX_CACHE_LIMIT" to s.AUTO_CLEAR_PREFIX_CACHE_LIMIT,
			"AUTO_CLEAR_PREFIX_CACHE_BYTES" to s.AUTO_CLEAR_PREFIX_CACHE_BYTES, "DEDUPLICATE_RES" to s.DEDUPLICATE_RES,
			"LIMIT_POI_CATEGORY_BY_FREQ" to s.LIMIT_POI_CATEGORY_BY_FREQ,
			"ALLOW_HOUSE_POI_TYPE_INTERSECTION" to s.ALLOW_HOUSE_POI_TYPE_INTERSECTION,
			"ALLOW_VIRTUAL_STREET_INTERSECTIONS" to s.ALLOW_VIRTUAL_STREET_INTERSECTIONS, "USE_POI_BBOX" to s.USE_POI_BBOX,
			"ENLARGE_BOUNDARIES" to s.ENLARGE_BOUNDARIES, "SHOW_MORE_WORDS_COUNT" to ints(s.SHOW_MORE_WORDS_COUNT),
			"SHOW_MORE_SCORE_DROP" to s.SHOW_MORE_SCORE_DROP, "SHOW_MORE_MAX_LEVEL_TIMES" to s.SHOW_MORE_MAX_LEVEL_TIMES,
			"MIN_CHARACTERS_INCOMPLETE" to s.MIN_CHARACTERS_INCOMPLETE, "SCORE_RANKING" to s.SCORE_RANKING,
			"LIMIT_READ_SINGLE_OBJECTS" to s.LIMIT_READ_SINGLE_OBJECTS, "LIMIT_READ_OBJECTS" to s.LIMIT_READ_OBJECTS,
			"LIMIT_INCOMPLETE_BYTES" to s.LIMIT_INCOMPLETE_BYTES, "MIN_ELO_RATING" to s.MIN_ELO_RATING,
			"WORLD_ELO_RATING" to s.WORLD_ELO_RATING, "LIMIT_ATOMIC_OBJECTS" to s.LIMIT_ATOMIC_OBJECTS,
			"KIND_WORD_NONINDEXED_SHARE" to s.KIND_WORD_NONINDEXED_SHARE, "POI_DEFAULT_RADIUS" to s.POI_DEFAULT_RADIUS,
			"ADDR_DEFAULT_RADIUS" to s.ADDR_DEFAULT_RADIUS, "POI_HOUSE_DEFAULT_RADIUS" to s.POI_HOUSE_DEFAULT_RADIUS,
			"DEV_USE_SKIP_HASH_TREE" to s.DEV_USE_SKIP_HASH_TREE, "DEV_USE_PIPELINE" to s.DEV_USE_PIPELINE,
			"TEST_PRINT_POI_CAT_LIMIT" to s.TEST_PRINT_POI_CAT_LIMIT,
			"TEST_PRINT_POI_CAT_RADIUS_KM" to s.TEST_PRINT_POI_CAT_RADIUS_KM,
			"OPTIM_DELETE_EMBEDDED_BOUNDARIES" to s.OPTIM_DELETE_EMBEDDED_BOUNDARIES,
			"OPTIM_FLAG_POI_SAME_AS_CITY_STREET" to s.OPTIM_FLAG_POI_SAME_AS_CITY_STREET,
			"DEV_FLAG_POI_SAME_AS_CITY_TREE" to s.DEV_FLAG_POI_SAME_AS_CITY_TREE,
			"OPTIM_READ_COMMON_WORDS_LIMIT" to s.OPTIM_READ_COMMON_WORDS_LIMIT,
			"OPTIM_READ_COMMON_WITH_OTH_NON_FOUND_ATOMS" to s.OPTIM_READ_COMMON_WITH_OTH_NON_FOUND_ATOMS,
			"OPTIM_READ_POI_CATEGORY_WORD_ATOMS" to s.OPTIM_READ_POI_CATEGORY_WORD_ATOMS,
			"MIN_ELO_RATING_TO_KEEP_IN_ATOM" to s.MIN_ELO_RATING_TO_KEEP_IN_ATOM,
			"OPTIM_LIMIT_RADIUS" to ints(s.OPTIM_LIMIT_RADIUS), "OPTIM_LIMIT_INTERSECTIONS" to s.OPTIM_LIMIT_INTERSECTIONS,
			"LIMIT_STOP_GOALS_ANY_LEVEL_WHEN_REACHED_RES" to s.LIMIT_STOP_GOALS_ANY_LEVEL_WHEN_REACHED_RES,
			"LIMIT_STOP_GOALS_LEVEL_1__WHEN_REACHED_RES" to s.LIMIT_STOP_GOALS_LEVEL_1__WHEN_REACHED_RES,
			"MAX_TOTAL_LIMIT_GOAL_LEVEL" to s.MAX_TOTAL_LIMIT_GOAL_LEVEL, "PIPELINE_MAX_STEPS" to s.PIPELINE_MAX_STEPS,
			"MAX_PIPELINE_ANY_RES" to s.MAX_PIPELINE_ANY_RES,
			"MAX_PIPELINE_RES_TO_STOP" to ints(s.MAX_PIPELINE_RES_TO_STOP),
			"PIPELINE_FREQUENT_OBJECTS_THRESHOLD" to s.PIPELINE_FREQUENT_OBJECTS_THRESHOLD,
			"PIPELINE_MAX_VIRTUAL_MASKS" to s.PIPELINE_MAX_VIRTUAL_MASKS)
		return fields.sortedBy { it.first }.joinToString("") { it.first + "=" + it.second + ";" }
	}

	companion object {
		/** `SpatialSearchTokenCompatTest.TYPES`. */
		private val TYPES = intArrayOf(4, -2, -1, -3, 0, 2, 1, 3, -5, 7)
		private val ENLARGE_DIMS = doubleArrayOf(0.0, 500.0, 999.0, 1000.0, 1001.0, 5000.0, 10000.0, 10001.0, 50000.0,
			100000.0, 100001.0, 300000.0, 300001.0, 1e7, -1.0, Double.NaN)
		private val ENLARGE_MULTS = doubleArrayOf(0.2, 1.0, 20.0)

		/** `SpatialSearchTokenCompatTest.poiTypes`. */
		private fun poiTypes(name: String): KTIntArrayList? {
			if (name.length % 4 == 1) {
				return null
			}
			val res = KTIntArrayList()
			res.add(name.length % 5)
			res.add(name.length % 3 + 7)
			return res
		}

		private fun settings(variant: Int): SpatialTextSearchSettings {
			val s = SpatialTextSearchSettings.defaultSettings()
			if (variant == 1) {
				s.DEV_USE_SKIP_HASH_TREE = false
			} else if (variant == 2) {
				s.USE_POI_BBOX = true
			}
			return s
		}

		/** The kinds of lines with sizes and locations of places, from the sines and logarithms of the platform. */
		private val PLACE_LINES = setOf('X', 'Z', 'O', 'B')

		/**
		 * Whether two lines are equal but for doubles a few ulps or a ten billionth apart, as in
		 * `SearchCoreFactoryTest.close`. A double is a run of 16 hex digits as java writes its bits; a run that
		 * follows an 'x' or is longer is a string written in hex ("x0061..."), compared as it is. No regex: the one
		 * of Kotlin/Native takes minutes over the long lines of atoms.
		 */
		private fun nearlySame(java: String, copy: String): Boolean {
			val a = ArrayList<String>()
			val b = ArrayList<String>()
			return skeleton(java, a) == skeleton(copy, b) && a.size == b.size &&
					a.indices.all { SearchCoreFactoryTest.nearlyEqual(a[it], b[it]) }
		}

		// the line with each double replaced by '#', the doubles into [doubles]
		private fun skeleton(line: String, doubles: MutableList<String>): String {
			val sb = StringBuilder(line.length)
			var i = 0
			while (i < line.length) {
				var end = i
				while (end < line.length && isHexDigit(line[end])) {
					end++
				}
				if (end == i) {
					sb.append(line[i++])
					continue
				}
				if (end - i == 16 && (i == 0 || line[i - 1] != 'x')) {
					doubles.add(line.substring(i, end))
					sb.append('#')
				} else {
					sb.append(line, i, end)
				}
				i = end
			}
			return sb.toString()
		}

		private fun isHexDigit(c: Char): Boolean = c in '0'..'9' || c in 'a'..'f'

		private fun tilePlace(zoom: Int, tileId: Long): NameIndexAtomXY {
			val xy = NameIndexAtomXY(null, null, null)
			xy.bbox31 = null
			xy.bboxTileZoom = zoom
			xy.bboxTileId = tileId
			return xy
		}

		private fun parseInts(s: String): IntArray =
			s.removePrefix("[").removeSuffix("]").split(", ").map { it.toInt() }.toIntArray()

		private fun unhexBytes(h: String): ByteArray = ByteArray(h.length / 2) { h.substring(2 * it, 2 * it + 2).toInt(16).toByte() }
	}
}

/** Every key of a name index: what reads the whole index. */
internal class EveryKey : NameIndexReaderMatcher("") {
	override fun matchKey(key: String): Boolean = true
}
