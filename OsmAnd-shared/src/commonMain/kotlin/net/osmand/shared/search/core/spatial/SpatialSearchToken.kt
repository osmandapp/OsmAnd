package net.osmand.shared.search.core.spatial

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.binary.AddressNameIndexDataAtom
import net.osmand.shared.binary.CityBlocks
import net.osmand.shared.binary.NameIndexReader
import net.osmand.shared.binary.NameIndexReader.NameIndexReaderMatcher
import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.binary.OsmAndPoiNameIndexDataAtom
import net.osmand.shared.data.Building
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.search.core.HashQuadTree
import net.osmand.shared.search.core.HashSkipTileQuadTree
import net.osmand.shared.search.core.spatial.SpatialSearchContext.SpatialSearchStats
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import net.osmand.shared.search.rules.SearchModDictionary.QueryForm
import net.osmand.shared.search.rules.SearchModRules
import net.osmand.shared.search.rules.SearchModRules.SearchModRuleOwner
import net.osmand.shared.util.JavaFormat
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollationKey
import net.osmand.shared.util.KCollatorStringMatcher
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTLongHashSet
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A word of the query of the spatial search: how it matches the keys and the names of a name index
 * by the search rules of the locale of each map, and the atoms of the names it matched with the tile
 * trees of their places.
 *
 * A copy of `SpatialSearchToken` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS. Without `wordSpacePrefixCache` and `wordSpaceCollatorSuffix`, which nothing reads. The
 * deleted atoms are a set of longs, the indexes of the atoms widened, read as [deletedAtoms] where java
 * also has `getDeletedAtoms()`.
 */
class SpatialSearchToken(
	globalRules: SearchModRules,
	MIN_CHAR_INCOMPLETE: Int,
	ow: String,
	original: String,
	order: Int
) {

	internal var MIN_CHAR_INCOMPLETE: Int = MIN_CHAR_INCOMPLETE
	internal var broad = false // a word still being typed with too many continuations: matched as a whole word

	internal var originalOrder = 0
	internal var sortedOrder = 0

	internal var incomplete = false
	internal var originalWord: String = original
	internal var word: String = ow
	internal var wordAligned: String
	internal var wordNoDot: String
	internal var bldWordSplit: Set<String>

	internal val poiCategoryKeysToAutocomplete = HashSet<String>()
	internal val poiCategoryIds = HashSet<Int>()
	internal val atoms = ArrayList<NameIndexAtom>()
	internal val index = KTLongObjectMap<NameIndexAtom>()
	internal val quadTree = HashQuadTree<Int>(16)
	internal val quadTreeSkip = HashSkipTileQuadTree<Int>()

	internal val indexByOsmIds = KTLongObjectMap<NameIndexAtom>()
	internal val deletedAtoms = KTLongHashSet()

	// partial place holder
	internal val partialExactMatch = ArrayList<PartialMatch>()
	internal val partialMatch = ArrayList<PartialMatch>()

	internal var collatorMain: KCollatorStringMatcher
	internal var noDotCollatorMain: KCollatorStringMatcher? = null
	internal var noHyphenCollatorMain: KCollatorStringMatcher? = null

	internal var mainNumber = -1
	/** a bare number whose value another query word already carries: '28' next to '28-ма' */
	internal var numberNamedByOther = false
	internal val globalRules: SearchModRules = globalRules
	// the word by the rules of the locale of every map it is matched with
	internal val localeRules = LinkedHashMap<String, LocaleRules>()
	// the word without the rules of a map: POI categories, a word not matched with a map yet
	internal val noRules: LocaleRules
	internal var categoryMatchMode = false
	internal val cacheCategoryFilterObjects = KTLongHashSet()

	data class PartialMatch(val atom: NameIndexAtom, val other: List<SpatialSearchToken>?, val nonNumericMatch: Boolean)

	init {
		categoryMatchMode = ow.startsWith(NameIndexReader.POI_CATEGORY_PREFIX)
		wordAligned = KSearchAlgorithms.alignChars(word)
		bldWordSplit = KSearchAlgorithms.getBuildingCompareSet(word, null)
		originalOrder = order
		var noDot = wordAligned
		if (wordAligned.endsWith(DOT_INCOMPLETE_STRING)) {
			incomplete = true
			noDot = wordAligned.substring(0, wordAligned.length - 1)
		}
		this.wordNoDot = noDot
		// . already in collator w.endsWith(DOT_INCOMPLETE_STRING)
		collatorMain = KCollatorStringMatcher(wordAligned, KStringMatcherMode.CHECK_EQUALS_FROM_SPACE)
		if (incomplete && word.length <= MIN_CHAR_INCOMPLETE + 1) {
			noDotCollatorMain = KCollatorStringMatcher(noDot, KStringMatcherMode.CHECK_EQUALS_FROM_SPACE)
		} else {
			if (KSearchAlgorithms.letters(noDot) == 0) {
				// pos case '4', '#4' query should match 4th, wrong case token '4' should not match '48th'
				// we use number to compare if we use is isNumber2Letters to many weird results on '2B'
				mainNumber = KAlgorithms.extractFirstIntegerNumber(noDot)
			}
		}
		if (wordAligned.indexOf('-') != -1) {
			// PA-21
			noHyphenCollatorMain = KCollatorStringMatcher(wordAligned.replace("-", ""), KStringMatcherMode.CHECK_EQUALS_FROM_SPACE)
		}
		noRules = LocaleRules("")
	}

	internal data class QueryMatcher(val form: QueryForm, val matcher: KCollatorStringMatcher)

	internal inner class LocaleRules(locale: String) {
		// forms for every owner of a name
		val otherMatch: Array<KCollatorStringMatcher>?
		// forms for some owners only ("pl" -> "Place" of a street)
		val scopedMatch = ArrayList<QueryMatcher>()
		// all forms: a key of a name index has no owner
		val prefixMatch: Array<KCollatorStringMatcher>?
		val fastMatchCheck = HashMap<String, Boolean>()
		val fastPrefMatchCheck = HashMap<String, Boolean>()

		val partOfBuilding: Boolean

		init {
			val other = ArrayList<KCollatorStringMatcher>()
			val prefix = ArrayList<KCollatorStringMatcher>()
			partOfBuilding = globalRules.dictionary(locale).likelyPartOfBuilding(word, bldWordSplit)
			for (form in globalRules.dictionary(locale).getQueryForms(wordNoDot)) {
				val m = KCollatorStringMatcher(form.word, KStringMatcherMode.CHECK_EQUALS_FROM_SPACE)
				if (form.isUnscoped()) {
					other.add(m)
				} else {
					scopedMatch.add(QueryMatcher(form, m))
				}
				prefix.add(m)
			}
			otherMatch = if (other.isEmpty()) null else other.toTypedArray()
			prefixMatch = if (prefix.isEmpty()) null else prefix.toTypedArray()
		}
	}

	internal fun localeRules(locale: String): LocaleRules {
		var lr = localeRules[locale]
		if (lr == null) {
			lr = LocaleRules(locale)
			localeRules[locale] = lr
		}
		return lr
	}

	fun getMainNumber(): Int = mainNumber

	/** not tied to one map (a POI category next to the word): building-like in a locale of any map matched */
	fun likelyPartOfBuilding(): Boolean {
		if (localeRules.isEmpty()) {
			return noRules.partOfBuilding
		}
		for (lr in localeRules.values) {
			if (lr.partOfBuilding) {
				return true
			}
		}
		return false
	}

	fun likelyPartOfBuilding(locale: String): Boolean = localeRules(locale).partOfBuilding

	fun likelyRef(): Boolean = globalRules.likelyPartOfRef(word, bldWordSplit)

	fun getMainCollator(): KCollatorStringMatcher = collatorMain

	fun isOnlyFullMatch(): Boolean = incomplete && (word.length <= MIN_CHAR_INCOMPLETE + 1 || broad)

	override fun toString(): String = "$sortedOrder. $originalWord - ${atoms.size} atoms"

	internal fun getPrefixMatcher(stats: SpatialSearchStats): NameIndexReaderMatcher = getPrefixMatcher(stats, noRules)

	/** @param locale rules locale of the map whose name index is read */
	internal fun getPrefixMatcher(stats: SpatialSearchStats, locale: String): NameIndexReaderMatcher =
		getPrefixMatcher(stats, localeRules(locale))

	private fun getPrefixMatcher(stats: SpatialSearchStats, lf: LocaleRules): NameIndexReaderMatcher {
		return object : NameIndexReaderMatcher(if (broad) wordNoDot else word) {

			override fun matchKey(key: String): Boolean {
				stats.sub1PartMatchTime.start()
				if (categoryMatchMode) {
					val ret = word.startsWith(key)
					stats.sub1PartMatchTime.finish()
					return ret
				} else if (key.startsWith(NameIndexReader.POI_CATEGORY_PREFIX) && poiCategoryKeysToAutocomplete.size > 0) {
					for (poiCatKey in poiCategoryKeysToAutocomplete) {
						if (poiCatKey.startsWith(key.substring(NameIndexReader.POI_CATEGORY_PREFIX.length))) {
							stats.sub1PartMatchTime.finish()
							return true
						}
					}
				}
				val cache = lf.fastPrefMatchCheck[key]
				if (cache != null) {
					stats.sub1PartMatchTime.finish()
					return cache
				}

				val alignedKey = KSearchAlgorithms.alignChars(key)
				// could be empty after align so match = true! ("''" -> "")
				var matched = matchAlignedKey(alignedKey)
				if (!matched && mainNumber > 0) {
					// 4th - key, "4" token
					matched = KAlgorithms.extractFirstIntegerNumber(key) == mainNumber
				}
				val prefixMatch = lf.prefixMatch
				if (!matched && prefixMatch != null) {
					for (o in prefixMatch) {
						matched = matched or KCollatorStringMatcher.cmatches(o.part.text, alignedKey,
							KStringMatcherMode.CHECK_ONLY_STARTS_WITH)
						// o.matches(alignedKey) could be needed for matching data with non-processed abbrevations
					}
				}
				if (!matched && key.startsWith(wordNoDot)
					&& KSearchAlgorithms.letters(key) == KSearchAlgorithms.letters(wordNoDot)) {
					// query 'pa 21' match 'pa21' key
					matched = true
				}
				lf.fastPrefMatchCheck[key] = matched
				stats.sub1PartMatchTime.finish()
				return matched
			}
		}
	}

	internal fun getAtomToken(atom: NameIndexAtom): NameIndexAtom? = index[atom.id]

	internal fun removeAtom(atom: NameIndexAtom) {
		val na = index[atom.id]!!
		deletedAtoms.add(na.indexInToken.toLong())
	}

	internal fun enlargeBbox(atom: NameIndexAtom, mult: Double) {
		val na = index[atom.id]!!
		quadTree.delete(atom.coords.bboxTileZoom, atom.coords.bboxTileId, na.indexInToken)
		atom.coords.enlargeBbox31(mult)
		quadTree.put(atom.coords.bboxTileZoom, atom.coords.bboxTileId, na.indexInToken)
		quadTreeSkip.addObject(na.indexInToken, atom.coords.bbox31!!, na.indexInToken.toLong())
	}

	internal fun addPoiCategoryMatch(id: Int) {
		poiCategoryIds.add(id)
	}

	internal fun addAtom(atom: NameIndexAtom): Boolean {
		if (atom.isPoiCategory()) {
			poiCategoryKeysToAutocomplete.add(atom.name)
			poiCategoryIds.add(atom.id.toInt())
		}
		val obj = atom.`object`
		val objId = obj?.getId()
		if (obj != null && obj !is Street && objId != null && objId > 0) {
			// mostly not used as disabled in settings for speed up
			val osmId = ObfConstants.getOsmIdFromMapObjectId(objId)
			val ex = indexByOsmIds[osmId]
			if (ex != null) {
				return false
			}
			indexByOsmIds.put(osmId, atom)
		}

		val existing = index[atom.id]
		if (existing != null) {
			if (existing !== atom) {
				// compare convention like method important!
				var res = atom.name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX)
					.compareTo(existing.name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX))
				// select shortest available version (see number of tests 'Piazza Trento e Trieste', netherlands_amsterdam_eerste_helmersstraat...)
				if (res == 0 && !KSearchAlgorithms.isNumber2Letters(wordAligned)) {
					res = atom.otherWordsCnt.compareTo(existing.otherWordsCnt)
					if (res == 0 && atom.ruleSpelling != existing.ruleSpelling) {
						// a name of OSM and its spelling made by a rule ("weberstraße", "weberstr"): the one with more
						// words of the query, the name of OSM when they have as many
						res = existing.otherFoundCnt.compareTo(atom.otherFoundCnt)
						if (res == 0) {
							res = atom.ruleSpelling.compareTo(existing.ruleSpelling)
						}
					} else if (res == 0) {
						res = atom.otherFoundCnt.compareTo(existing.otherFoundCnt)
					}
				} else if (res == 0) {
					res = (atom.otherWordsCnt + atom.otherFoundCnt).compareTo(existing.otherWordsCnt + existing.otherFoundCnt)
				}
				// '2 south 2nd street' vs '25 садова вулиця' (25-та) -
				if (res == 0 && !KSearchAlgorithms.isNumber2Letters(wordAligned)) {
					// a school
					res = (atom.isBuilding() || atom.isPOIRef()).compareTo(existing.isBuilding() || existing.isPOIRef())
				}
				// 'вулиця 28-ма Лінія 28': '28-ма' names the street, the bare 28 keeps its house
				if (numberNamedByOther && (existing.isBuilding() || existing.isPOIRef()) && !(atom.isBuilding() || atom.isPOIRef())) {
					res = 0
				}
				val replace = res < 0
				if (replace) {
					atom.indexInToken = existing.indexInToken
					index.put(atom.id, atom)
					atoms[atom.indexInToken] = atom
				}
				return true
			}
			return false
		}
		index.put(atom.id, atom)
		atoms.add(atom)
		val indx = atoms.size - 1
		atom.indexInToken = indx
		quadTree.put(atom.coords.bboxTileZoom, atom.coords.bboxTileId, indx)
		val bbox31 = atom.coords.bbox31
		if (bbox31 != null) {
			quadTreeSkip.addObject(indx, bbox31, indx.toLong())
		}
		return true
	}

	/**
	 * @param locale rules locale of the map of the name
	 * @param type   type of the atom of the name: the forms of some owners apply only to their names
	 */
	internal fun matchName(name: String, poiTypes: KTIntArrayList?, locale: String, type: Int): Boolean {
		val lf = localeRules(locale)
		if (matchName(name, poiTypes, lf)) {
			return true
		}
		if (categoryMatchMode || name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX)) {
			return false
		}
		// not cached: the owner differs for one name
		val owner = ruleOwner(type)
		for (q in lf.scopedMatch) {
			if (q.form.appliesTo(owner) && q.matcher.matches(name)) {
				return true
			}
		}
		return false
	}

	/** the owner of a name of an atom type for the search rules; a building has the name of its street */
	internal fun ruleOwner(type: Int): SearchModRuleOwner {
		if (type == CityBlocks.STREET_TYPE.index || type == BUILDING_TYPE) {
			return SearchModRuleOwner.STREET
		} else if (type == POI_TYPE || type == POI_REF_TYPE) {
			return SearchModRuleOwner.POI
		} else if (type == CityBlocks.BOUNDARY_TYPE.index) {
			return SearchModRuleOwner.BOUNDARY
		} else if (type == CityBlocks.POSTCODES_TYPE.index) {
			return SearchModRuleOwner.POSTCODE
		}
		return SearchModRuleOwner.LOCALITY
	}

	private fun matchName(name: String, poiTypes: KTIntArrayList?, lf: LocaleRules): Boolean {
		if (categoryMatchMode) {
			return name == word
		}
		if (name.startsWith(NameIndexReader.POI_CATEGORY_PREFIX)) {
			return poiTypes != null && matchPoiCategoryKeys(poiTypes)
		}
		val cache = lf.fastMatchCheck[name]
		if (cache != null) {
			return cache
		}
		var res = false
		try {
			if (mainNumber > 0) {
				if (mainNumber == KAlgorithms.extractFirstIntegerNumber(name)) {
					res = true
					return res
				}
			}
			val otherMatch = lf.otherMatch
			if (otherMatch != null) {
				for (o in otherMatch) {
					if (o.matches(name)) {
						res = true
						return res
					}
				}
			}
			if ((noDotCollatorMain ?: collatorMain).matches(name)) {
				res = true
				return res
			}
			val noHyphen = noHyphenCollatorMain
			if (noHyphen != null && noHyphen.matches(name)) {
				res = true
				return res
			}
		} finally {
			lf.fastMatchCheck[name] = res
		}
		return false
	}

	fun getPartialExactMatch(): List<PartialMatch> = partialExactMatch

	fun getPartialMatch(): List<PartialMatch> = partialMatch

	fun clearPartialAtoms() {
		partialExactMatch.clear()
		partialMatch.clear()
	}

	fun hasPoiCategoryKeys(): Boolean = poiCategoryKeysToAutocomplete.isNotEmpty()

	fun matchPoiCategoryKeys(poiTypes: KTIntArrayList): Boolean {
		for (k in 0 until poiTypes.size()) {
			if (poiCategoryIds.contains(poiTypes.getQuick(k))) {
				return true
			}
		}
		return false
	}

	fun addPartialCommonAtom(atom: NameIndexAtom, otherTokens: List<SpatialSearchToken>?, numericNotMatch: Boolean) {
		partialExactMatch.add(PartialMatch(atom, otherTokens, numericNotMatch))
	}

	fun addPartialOtherAtom(atom: NameIndexAtom, otherTokens: List<SpatialSearchToken>?, numericNotMatch: Boolean) {
		partialMatch.add(PartialMatch(atom, otherTokens, numericNotMatch))
	}

	internal fun matchSplitName(fullName: String): Array<String>? {
		val name = KSearchAlgorithms.alignChars(fullName)
		if (wordAligned.length >= name.length) {
			return null
		}
		var res: Array<String>? = null
		val cutName = name.substring(0, wordAligned.length)
		val fastEquals = wordAligned == cutName
		val isTopIndex = cutName.startsWith(TOP_INDEX_CATEGORY) || wordAligned.startsWith(TOP_INDEX_CATEGORY)
		val collatorEquals = !fastEquals && !isTopIndex && KCollationKey.primaryEquals(cutName, wordAligned)
		if (fastEquals || collatorEquals) {
			// don't split numbers
			if (name[wordAligned.length].isDigit() && name[wordAligned.length - 1].isDigit()) {
				return null
			}
			var sub = wordAligned.length
			while (sub < name.length) {
				if (name[sub].isLetter() || name[sub].isDigit()) {
					break
				}
				sub++
			}
			if (sub >= name.length) {
				return null
			}
			res = arrayOf(cutName, name.substring(sub))
		}
		return res
	}

	class NameIndexAtomXY(a: AddressNameIndexDataAtom?, b: OsmAndPoiNameIndexDataAtom?, settings: SpatialTextSearchSettings?) {
		// SHOULD BE NOT MODIFIABLE AS WE INTERSECT OBJECTS atom x atom
		internal var bbox31: IntArray? = null // if exists [xleft, yleft, xright, yright]
		internal var bboxTileId = 0L // encodes zoom, tileX, tileY
		internal var bboxTileZoom = 0
		internal var x16 = 0
		internal var y16 = 0

		init {
			if (a != null) {
				init(a, settings)
			} else if (b != null) {
				init(b, settings)
			} else {
				// full world
				bboxTileZoom = 0
				bboxTileId = 0
				bbox31 = intArrayOf(0, 0, Int.MAX_VALUE, Int.MAX_VALUE)
			}
		}

		fun intersects(a: NameIndexAtomXY): Boolean {
			val abbox31 = a.bbox31
			if (bbox31 == null && abbox31 == null) {
				var z1 = bboxTileZoom
				var z2 = a.bboxTileZoom
				var tid1 = bboxTileId
				var tid2 = a.bboxTileId
				while (z1 > z2) {
					tid1 = tid1 shr 2
					z1--
				}
				while (z2 > z1) {
					tid2 = tid2 shr 2
					z2--
				}
				return tid1 == tid2
			} else if (abbox31 == null) {
				return a.intersects(this)
			} else {
				return intersects(abbox31)
			}
		}

		fun intersects(abbox31: IntArray): Boolean {
			// if exists [xleft, ytop, xright, ybottom]
			val bbox31 = this.bbox31
			if (bbox31 == null) {
				val xleft = abbox31[0] shr (31 - this.bboxTileZoom)
				val xright = abbox31[2] shr (31 - this.bboxTileZoom)
				val ytop = abbox31[1] shr (31 - this.bboxTileZoom)
				val ybottom = abbox31[3] shr (31 - this.bboxTileZoom)
				val x = KMapUtils.deinterleaveX(this.bboxTileId)
				val y = KMapUtils.deinterleaveY(this.bboxTileId)
				return xleft <= x && x <= xright && ytop <= y && y <= ybottom
			} else {
				// if exists [xleft, ytop, xright, ybottom]
				return bbox31[0] <= abbox31[2] && bbox31[2] >= abbox31[0] && bbox31[1] <= abbox31[3] && bbox31[3] >= abbox31[1]
			}
		}

		fun contains(a: NameIndexAtomXY): Boolean {
			val bbox31 = this.bbox31
			val abbox31 = a.bbox31
			if (bbox31 == null || abbox31 == null) {
				val z1 = bboxTileZoom
				var z2 = a.bboxTileZoom
				val tid1 = bboxTileId
				var tid2 = a.bboxTileId
				while (z2 > z1) {
					tid2 = tid2 shr 2
					z2--
				}
				return tid1 == tid2 && z2 == z1
			}
			// if exists [xleft, ytop, xright, ybottom]
			return bbox31[0] <= abbox31[0] && bbox31[2] >= abbox31[2] && bbox31[1] <= abbox31[1] && bbox31[3] >= abbox31[3]
		}

		fun tileIdString(): String =
			"$bboxTileZoom ${KMapUtils.deinterleaveX(bboxTileId)} ${KMapUtils.deinterleaveY(bboxTileId)}"

		// the settings are read only where java reads them
		private fun init(addr: AddressNameIndexDataAtom, settings: SpatialTextSearchSettings?) {
			if (addr.getXy16Count() >= 1) {
				val xy16 = addr.getXy16(0)
				this.x16 = xy16 ushr 16
				this.y16 = xy16 and ((1 shl 16) - 1)
				decodeBBox(if (addr.hasBbox()) addr.getBbox() else null)
				if (bbox31 == null) {
					if (settings!!.DEV_USE_SKIP_HASH_TREE) {
						bboxTileZoom = 15
						bboxTileId = HashQuadTree.encodeTileId(bboxTileZoom, x16 / 2, y16 / 2)
						bbox31 = KMapUtils.calc31BboxRhumb(settings.ADDR_DEFAULT_RADIUS, x16, y16, 16)
						calcTileFromBbox()
					} else {
						bboxTileZoom = 15
						bboxTileId = HashQuadTree.encodeTileId(bboxTileZoom, x16 / 2, y16 / 2)
					}
				}
			}
		}

		private fun decodeBBox(bbox: ByteArray?) {
			if (bbox != null) {
				bbox31 = KSearchAlgorithms.decodeBboxForNameAtomsBytes(bbox, x16, y16)
				calcTileFromBbox()
			}
		}

		private fun calcTileFromBbox() {
			val bbox31 = this.bbox31
			if (bbox31 != null) {
				// for 180 lat check max
				var xleft = bbox31[0]
				val xright = max(bbox31[2], bbox31[0])
				var ytop = bbox31[1]
				val ybottom = max(bbox31[3], bbox31[1])
				val xDiff = xleft xor xright
				val yDiff = ytop xor ybottom
				val maxDiff = xDiff or yDiff
				val bitsToShift = if (maxDiff == 0) 0 else 32 - maxDiff.countLeadingZeroBits()

				val z = min(31 - bitsToShift, 16)
				xleft = xleft shr (31 - z)
				ytop = ytop shr (31 - z)
				bboxTileZoom = z
				bboxTileId = HashQuadTree.encodeTileId(z, xleft, ytop)
			}
		}

		private fun init(poi: OsmAndPoiNameIndexDataAtom, settings: SpatialTextSearchSettings?) {
			this.x16 = poi.getX()
			this.y16 = poi.getY()
			bboxTileZoom = 16
			bboxTileId = HashQuadTree.encodeTileId(bboxTileZoom, x16, y16)
			if (settings!!.USE_POI_BBOX) {
				decodeBBox(if (poi.hasBbox()) poi.getBbox() else null)
			}
			if (bbox31 == null && settings.DEV_USE_SKIP_HASH_TREE) {
				bbox31 = KMapUtils.calc31BboxRhumb(settings.POI_DEFAULT_RADIUS, x16, y16, 16)
				calcTileFromBbox()
			}
		}

		fun enlargeBbox31(mult: Double) {
			if (mult == 0.0) {
				return
			}
			val bbox31 = this.bbox31
			if (bbox31 != null) {
				enlargeBbox(mult, bbox31)
			} else {
				val w = ceil(mult).toInt()
				val enlarged = IntArray(4)
				enlarged[0] = max((x16 - w) shl 15, 0) // xleft
				enlarged[2] = max((x16 + w) shl 15, enlarged[0]) // xright
				enlarged[1] = max((y16 - w) shl 15, 0) // ytop
				enlarged[3] = max((y16 + w) shl 15, enlarged[1]) // ybottom
				this.bbox31 = enlarged
			}
			calcTileFromBbox()
		}

		fun dimensionInM(): Double {
			var xleft = x16 shl 15
			var xright = (x16 + 1) shl 15
			var ytop = y16 shl 15
			var ybottom = (y16 + 1) shl 15
			val bbox31 = this.bbox31
			if (bbox31 != null) {
				xleft = bbox31[0]
				xright = bbox31[2]
				ytop = bbox31[1]
				ybottom = bbox31[3]
			}
			return distanceInM(xleft, ytop, xright, ybottom)
		}

		companion object {

			fun enlargeBbox(mult: Double, bbox31: IntArray) {
				val w = ((bbox31[2] - bbox31[0]) * mult).toInt()
				val h = ((bbox31[3] - bbox31[1]) * mult).toInt()
				bbox31[0] = max(min(bbox31[0], bbox31[0] - w), 0) // xleft
				bbox31[2] = max(bbox31[2] + w, bbox31[0]) // xright
				bbox31[1] = max(min(bbox31[1], bbox31[1] - h), 0) // ytop
				bbox31[3] = max(bbox31[3] + h, bbox31[1]) // ybottom
			}

			fun distanceInM(xleft: Int, ytop: Int, xright: Int, ybottom: Int): Double =
				KMapUtils.getDistance(KMapUtils.get31LatitudeY(ytop), KMapUtils.get31LongitudeX(xleft),
					KMapUtils.get31LatitudeY(max(ytop, ybottom)), KMapUtils.get31LongitudeX(max(xleft, xright)))

			fun distanceInM(bbox: IntArray): Double = distanceInM(bbox[0], bbox[1], bbox[2], bbox[3])
		}
	}

	class NameIndexAtom internal constructor(
		// SHOULD BE NOT MODIFIABLE AS WE INTERSECT OBJECTS atom x atom
		internal val name: String,
		internal val type: Int,
		internal val id: Long, // used to read object
		internal val parentid: Long, // used to read object
		internal var `object`: MapObject?, // same for all
		internal val cityAsStreet: Boolean,
		internal var otherWordsCnt: Int, // added before intersection
		internal var otherFoundCnt: Int,
		internal val coords: NameIndexAtomXY,
		internal val nearbyRadius: Int,
		internal val buildingOrRefInd: Int // added before intersection
	) {
		internal var bldObject: MapObject? = null // same for all

		// matched name words that are not common in this map: "Avenue" is, "York" is not
		internal var distinctFoundCnt = -1

		internal var indexInToken = 0
		internal var poiTypes: KTIntArrayList? = null
		internal var elo = 0
		internal var sameNameAreaObj: NameIndexAtom? = null
		// rules locale of the map of the atom
		internal var locale = ""
		// found by a name the OBF writer made by a search rule, not by a name of OSM
		internal var ruleSpelling = false

		internal constructor(name: String, id: Long, total: Int) : this(name, POI_CATEGORY_TYPE, id, 0, null, false,
			-total, total, NameIndexAtomXY(null, null, null), 0, -1)

		internal constructor(cp: NameIndexAtom) : this(cp.name, cp.type, cp.id, cp.parentid, cp.`object`, cp.cityAsStreet,
			cp.otherWordsCnt, cp.otherFoundCnt, cp.coords, cp.nearbyRadius, cp.buildingOrRefInd) {
			this.poiTypes = cp.poiTypes
			this.distinctFoundCnt = cp.distinctFoundCnt
			this.locale = cp.locale
			this.ruleSpelling = cp.ruleSpelling
		}

		fun isCityStreetName(): Boolean = cityAsStreet

		fun isStreetBuilding(): Boolean = type == STREET_TYPE || type == BUILDING_TYPE

		fun isPoiCategory(): Boolean = type == POI_CATEGORY_TYPE

		fun isPostcode(): Boolean = type == CityBlocks.POSTCODES_TYPE.index

		fun isBoundary(): Boolean = type == CityBlocks.BOUNDARY_TYPE.index

		fun isGeoArea(): Boolean = isBoundary() || isCityVillage() || isPostcode()

		fun isCityVillage(): Boolean = type == CityBlocks.CITY_TOWN_TYPE.index || type == CityBlocks.VILLAGES_TYPE.index

		fun isCity(): Boolean = type == CityBlocks.CITY_TOWN_TYPE.index

		fun isStreet(): Boolean = type == STREET_TYPE

		fun atomicObject(): Boolean = type == STREET_TYPE || type == POI_TYPE || type == BUILDING_TYPE || type == POI_REF_TYPE

		fun isBuilding(): Boolean = type == BUILDING_TYPE || (type == STREET_TYPE && bldObject != null)

		fun isPOI(): Boolean = type == POI_TYPE || type == POI_REF_TYPE

		fun isPOIRef(): Boolean = type == POI_REF_TYPE

		fun typeStr(): String {
			var typeS = ""
			if (isPoiCategory()) {
				typeS = "POI_TYPE"
			} else if (isPOI()) {
				typeS = "POI"
			} else if (isBuilding()) {
				typeS = "Building"
			} else {
				typeS = CityBlocks.getByType(type).toString()
			}
			return typeS
		}

		internal fun simpleName(name: String): String =
			typeStr() + " " + name + " " + (id % 0xffff) + " (" +
					JavaFormat.fixed(KMapUtils.get31LatitudeY(coords.y16 shl 15), 4) + ", " +
					JavaFormat.fixed(KMapUtils.get31LongitudeX(coords.x16 shl 15), 4) + ")"

		fun getResultLocation(): KLatLon? {
			val bld = bldObject
			if (bld != null) {
				return bld.getLocation()
			}
			val obj = `object`
			if (obj != null) {
				return obj.getLocation()
			}
			return KLatLon(KMapUtils.get31LatitudeY(coords.y16 shl 15), KMapUtils.get31LongitudeX(coords.x16 shl 15))
		}

		override fun toString(): String = `object`?.toString() ?: simpleName(name)

		fun getName(): String = name

		fun getObject(): MapObject? = `object`

		fun getBuilding(): Building? = bldObject as? Building
	}

	fun getToken(): String = word

	companion object {
		const val ALL_CITY_TYPE = -10

		const val POI_CATEGORY_TYPE = -5
		const val POI_REF_TYPE = -3
		const val BUILDING_TYPE = -2
		const val POI_TYPE = -1
		val STREET_TYPE = CityBlocks.STREET_TYPE.index
		val DOT_INCOMPLETE_STRING = KCollatorStringMatcher.INCOMPLETE_DOT.toString()

		private const val TOP_INDEX_CATEGORY = NameIndexReader.POI_CATEGORY_PREFIX + MapPoiTypes.TOP_INDEX_ADDITIONAL_PREFIX
	}
}
