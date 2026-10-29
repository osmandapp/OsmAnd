package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.BaseDetailsObject
import net.osmand.shared.data.Building
import net.osmand.shared.data.City
import net.osmand.shared.data.CityType
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.map.WorldRegion
import net.osmand.shared.search.core.HashQuadTree
import net.osmand.shared.search.core.spatial.SpatialPoiSearch.SpatialPoiType
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtom
import net.osmand.shared.util.JavaFormat
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import kotlin.math.max
import kotlin.math.min

/**
 * One result of the spatial search: one combination of the atoms the words of the query matched, the
 * objects of those atoms and how the result ranks.
 *
 * A copy of `SpatialSearchResult` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS.
 */
class SpatialSearchResult internal constructor(
	parentList: SpatialSearchResultsList, internal val parentInd: Int, internal val preciseLatlon: KLatLon?,
	extraName: String?, surplusWords: Int?
) : Comparable<SpatialSearchResult> {

	internal val parent: SpatialSearchResultsList = parentList
	internal val objs: MutableList<SpatialSearchResultRef> = ArrayList()
	internal val extraNameMatch: String? = extraName // refs and interpolation
	internal val surplusWords: Int // negative some building numbers not found, positive some extra tokens matched
	internal var visibleLevel = 0
	var unitedObject: BaseDetailsObject? = null
	internal var biggestCityType = -1
	internal var score = 0.0 // set by sortResults(), see SpatialSearchRanking

	// dedup linked list
	internal var dedupName: String? = null
	internal var prevDedupSameName: SpatialSearchResult? = null

	init {
		for (i in 0 until parent.tCount) {
			val atom = parent.linearResults[parentInd * parentList.tCount + i]
			val token = parent.tokens[i]
			var ref: SpatialSearchResultRef? = null
			// find same object or object & parent
			for (check in objs) {
				if (atom.id == check.atom.id) {
					ref = check
					if (!atom.isBuilding() && !atom.isPOIRef()) {
						if (ref.otherWordsFound > 0) {
							ref.otherWordsFound--
						} else if (ref.otherWordsNotFound > 0) {
							ref.otherWordsNotFound--
						}
					}
					// building-street, but a house the map does not have stays its street
					if (if (check.atom.type > atom.type) atom.bldObject != null || atom.type != SpatialSearchToken.BUILDING_TYPE
						else check.atom.type == SpatialSearchToken.BUILDING_TYPE && check.atom.bldObject == null
					) {
						// existing street - swap
						check.atom = atom
						break
					}
				}
			}
			if (ref == null) {
				ref = SpatialSearchResultRef(atom)
				ref.otherWordsFound = atom.otherFoundCnt
				if (atom.isBuilding() || atom.isPOIRef()) {
					ref.otherWordsFound++
				}
				ref.otherWordsNotFound = atom.otherWordsCnt
				objs.add(ref)
			}
			ref.tokens.add(token)
		}
		this.surplusWords = surplusWords ?: 0
		sortObjects()
	}

	internal fun sortObjects() {
		for (r in objs) {
			r.tokens.sortWith { o1, o2 -> o1.originalOrder.compareTo(o2.originalOrder) }
		}
		objs.sortWith { o1, o2 ->
			val r = o1.typeOrder(true).compareTo(o2.typeOrder(true))
			if (r != 0) r else o1.tokens[0].originalOrder.compareTo(o2.tokens[0].originalOrder)
		}
	}

	fun getFirstRef(): SpatialSearchResultRef? {
		if (objs.size > 0) {
			return objs[0]
		}
		return null
	}

	/** atoms of the combination this result stands for, one per query token */
	fun getAtoms(): List<NameIndexAtom> = parent.getRawAtoms(parentInd)

	private fun getFirstRefObject(useUnited: Boolean): MapObject? {
		val united = unitedObject
		if (useUnited && united != null) {
			val addressObject = united.getAddressObject()
			return addressObject ?: united.getSyntheticAmenity()
		}
		if (objs.size > 0) {
			val o = objs[0]
			val bldObject = o.atom.bldObject
			if (bldObject != null) {
				return bldObject
			}
			return o.atom.`object`
		}
		return null
	}

	private inline fun <reified T> getMatchRefObject(): T? {
		for (r in objs) {
			val obj = r.atom.`object`
			if (obj is T) {
				return obj
			}
		}
		return null
	}

	fun getReferenceObject(): MapObject? {
		val united = unitedObject
		if (united != null) {
			val addressObject = united.getAddressObject()
			return addressObject ?: united.getSyntheticAmenity()
		}
		for (ref in objs) {
			val bldObject = ref.atom.bldObject
			if (bldObject != null) {
				return bldObject
			}
			val obj = ref.atom.`object`
			if (obj != null) {
				return obj
			}
		}
		return null
	}

	fun getViewBBox31(): IntArray? {
		val obj = getReferenceObject() ?: return null
		val bbox31 = obj.getBbox31()
		if (bbox31 != null) {
			return bbox31
		}
		val latLon = obj.getLocation()!!
		var radius = 1000
		if (obj is City) {
			radius = obj.getType().getRadius().toInt()
		} else if (obj is Amenity) {
			val ct = CityType.valueFromString(obj.getSubType())
			if (ct != null) {
				radius = ct.getRadius().toInt()
			}
		}
		val bbox = KMapUtils.calculate31Bbox(latLon.latitude, latLon.longitude, radius)
		return intArrayOf(bbox.left.toInt(), bbox.top.toInt(), bbox.right.toInt(), bbox.bottom.toInt())
	}

	fun getBBox31(): IntArray? = getReferenceObject()?.getBbox31()

	/**
	 * @return main object location (if search is poi category use getReferenceObject)
	 */
	fun getMainObject(): MapObject? = getFirstRefObject(true)

	fun getObjects(): List<MapObject> {
		// street & building overlap same object so filter them
		val o = ArrayList<MapObject>()
		var firstRefObject: MapObject? = null
		val united = unitedObject
		if (united != null) {
			firstRefObject = getFirstRefObject(false)
			val addressObject = united.getAddressObject()
			if (addressObject != null) {
				o.add(addressObject)
			}
			o.add(united.getSyntheticAmenity())
		}
		for (r in objs) {
			val bldObject = r.atom.bldObject
			if (bldObject != null && bldObject !== firstRefObject) {
				o.add(bldObject)
			}
			val obj = r.atom.`object`
			if (obj != null && obj !== firstRefObject && !o.contains(obj)) {
				o.add(obj)
			}
		}
		return o
	}

	fun isPoiCategory(): Boolean = objs.isNotEmpty() && objs[0].isPoiCategory()

	fun getPoiCategory(poiSearch: SpatialPoiSearch): SpatialPoiType? {
		if (objs.isNotEmpty() && objs[0].isPoiCategory()) {
			return poiSearch.getById(objs[0].atom.id.toInt())
		}
		return null
	}

	// the category name the query words matched: "schu" -> Schule
	fun getPoiCategoryMatchedName(poiSearch: SpatialPoiSearch): String? {
		val category = getPoiCategory(poiSearch) ?: return null
		for (name in category.names) {
			if (objs[0].tokens.all { it.getMainCollator().matches(name) }) {
				return category.getOriginalName(name)
			}
		}
		return null
	}

	fun getExtraNameMatch(): String? = extraNameMatch

	fun getLatLon(): KLatLon? {
		if (preciseLatlon != null) {
			return preciseLatlon
		}
		val united = unitedObject
		if (united != null && isRoute()) {
			return united.getLocation() // see moveToNearestRouteSegment()
		}
		for (r in objs) {
			if (!r.atom.isPoiCategory()) {
				return r.atom.getResultLocation()
			}
		}
		return null
	}

	fun visibleLevel(): Int = visibleLevel

	fun extraDeduplicateKeys(ctx: SpatialSearchContext): List<String>? {
		var result: MutableList<String>? = null
		val mapObject = getFirstRefObject(true)
		if (mapObject is Amenity && (mapObject.isRouteTrack() || mapObject.isSuperRoute())) {
			return addResult(result, mapObject.getRouteId())
		}
		result = addResult(result, getWikidata(ctx))
		result = addResult(result, getRouteId())
		if (mapObject is Amenity) {
			if (mapObject.getType()!!.getKeyName() == "natural") {
				val name = KSearchAlgorithms.normalizeToken(KSearchAlgorithms.alignChars(mapObject.getName()))
				val link = getShortLink(ZOOM_SIMILARITY_10_KM)
				result = addResult(result, name + "_" + link)
			}
		}
		if (mapObject is Building) {
			result = addResult(result, (extraNameMatch ?: mapObject.getName()) + "_" + getShortLink(ZOOM_SIMILARITY_10_M))
		}
		val wikipedia = getWikipedia()
		if (wikipedia != null) {
			result = addResult(result, getShortLink(ZOOM_SIMILARITY_10_M) + wikipedia)
		}
		return result
	}

	private fun addResult(result: MutableList<String>?, value: String?): MutableList<String>? {
		var res = result
		if (!KAlgorithms.isEmpty(value)) {
			if (res == null) {
				res = ArrayList()
			}
			res.add(value!!)
		}
		return res
	}

	internal fun moveToNearestRouteSegment(location: KLatLon?) {
		// a route is split into segments: show the merged row at the segment nearest to the user
		val united = unitedObject
		if (location == null || united == null || !isRoute()) {
			return
		}
		var nearest: KLatLon? = null
		for (o in united.getObjects()) {
			val l = if (o is Amenity && o.getRouteId() != null) o.getLocation() else null
			if (l != null && (nearest == null || KMapUtils.getDistance(l, location) < KMapUtils.getDistance(nearest, location))) {
				nearest = l
			}
		}
		if (nearest != null) {
			united.getSyntheticAmenity().setLocation(nearest)
		}
	}

	fun addExtraResult(other: SpatialSearchResult, lang: String?) {
		val obj = getFirstRefObject(false)
		val otherObj = other.getFirstRefObject(false)
		if (otherObj == null) {
			// poi category search find bbox that has a point on merge (not necessary sameobject)
			if (obj == null) {
				val targetObject = getReferenceObject()
				val refObject = other.getReferenceObject()
				val location = targetObject?.getLocation()
				if (targetObject is Amenity && targetObject.getBbox31() == null && refObject != null && location != null) {
					val bbox31 = refObject.getBbox31()
					val y = KMapUtils.get31TileNumberY(location.latitude)
					val x = KMapUtils.get31TileNumberX(location.longitude)
					if (bbox31 != null && bbox31[0] <= x && bbox31[2] >= x && bbox31[1] <= y && bbox31[3] >= y) {
						targetObject.setBbox31(refObject.getBbox31())
					}
				}
			}
			return // nothing to merge
		}
		val united = unitedObject
		if (united != null && unitesSame(united, otherObj)) {
			// the same object found again with other words: every merge combines all united objects once more,
			return
		}
		if (obj is Amenity && unitedObject == null) {
			unitedObject = BaseDetailsObject(obj, lang)
		}
		val united2 = unitedObject
		if (united2 != null) {
			united2.addObject(otherObj)
		} else if (otherObj is Amenity) {
			val newUnited = BaseDetailsObject(otherObj, lang)
			unitedObject = newUnited
			// here we merge city, building,
			newUnited.addObject(obj)
		} else {
			// case 2 objects are not amenities (same buildings)
			if (obj is Building && otherObj is Building) {
				val s1 = getMatchRefObject<Street>()
				val s2 = other.getMatchRefObject<Street>()
				// add suburb city part to first item
				if (s1 != null && s2 != null && s1.getCity()!!.getName() != s2.getCity()!!.getName() &&
					!s1.getName().contains("(")
				) {
					s1.setName(s1.getName() + " (" + s2.getCity()!!.getName() + ")")
					val namesMap = s1.getNamesMap(true)
					for ((key, value) in namesMap) {
						s1.setName(key, value + " (" + s2.getCity()!!.getName() + ")")
					}
				}
			}
		}
	}

	fun getIdDeduplication(): Long {
		if (objs.isNotEmpty()) {
			val first = objs[0]
			// street intersection (!) or building interpolation
			if (preciseLatlon != null && !first.isPoiCategory()) {
				val y31 = KMapUtils.get31TileNumberY(preciseLatlon.latitude)
				val x31 = KMapUtils.get31TileNumberX(preciseLatlon.longitude)
				return HashQuadTree.encodeTileId31(19, x31, y31)
			}
			val obj = first.atom.`object`
			if (obj != null) {
				return ObfConstants.getOsmObjectId(obj)
			}
			if (first.isPoiCategory()) {
				var poiTypeId = (first.atom.id shl 3)
				// suggest poi category for different cities in query (we can't chose which is better?)
				if (objs.size == 2) {
					val second = objs[1]
					poiTypeId += second.tokens[0].originalOrder
				}
				return poiTypeId
			}
			return first.atom.id
		}
		return -1
	}

	override fun toString(): String = toString(null)

	fun toString(ctx: SpatialSearchContext?): String {
		var r = ""
		val wikidata = getWikidata(ctx)
		if (wikidata != null) {
			r += "$wikidata "
		}
		if (preciseLatlon != null) {
			r += JavaFormat.fixed(preciseLatlon.latitude, 4) + ", " + JavaFormat.fixed(preciseLatlon.longitude, 4) + " "
		}
		if (extraNameMatch != null) {
			r += "$extraNameMatch "
		}
		return r + objs.toString()
	}

	class SpatialSearchResultRef(atom: NameIndexAtom) {
		internal var atom: NameIndexAtom = atom
		internal val tokens: MutableList<SpatialSearchToken> = ArrayList()
		internal var otherWordsFound = 0
		internal var otherWordsNotFound = 0

		fun extraNameRelated(): Boolean = atom.buildingOrRefInd >= 0

		fun isPoiCategory(): Boolean = atom.isPoiCategory()

		// This is final order for words in result
		// It also affects on sorting order that's why (boundary == 4 == city) to sort by distance
		fun typeOrder(wordsOrder: Boolean): Int {
			if (atom.isPoiCategory()) {
				// push to first token
				return -2
			} else if (atom.isBuilding()) {
				return -1
			} else if (atom.isPOI()) {
				return 0
			} else if (atom.isStreet()) {
				return 1
			} else if (atom.isPostcode()) {
				return 3
			} else if (atom.isBoundary()) {
				// different sorting words in result & different between results
				return if (wordsOrder) MAX_TYPE_ORDER else 4
			}
			// all cities, villages, hamlets
			return 4
		}

		override fun toString(): String {
			val words = StringBuilder()
			for (s in tokens) {
				words.append(" ").append(s.word)
			}
			val idObject = atom.`object`
			if (idObject != null) {
				var name = idObject.getName()
				var type = atom.typeStr()
				val bldObject = atom.bldObject
				if (bldObject != null) {
					name = bldObject.getName() + " " + name
				} else if (idObject is Amenity) {
					type += " " + idObject.getSubTypeStr()
					if (idObject.getTravelEloNumber() > Amenity.DEFAULT_ELO) {
						type += " " + idObject.getTravelEloNumber() // " " + a.getCityFromTagGroups("");
					}
				}
				val resLoc = atom.getResultLocation()!!
				return "\"" + words.toString().trim() + "\" [" + type + "] '" + name + "' " +
						ObfConstants.getOsmObjectId(idObject) + " " + (atom.id % 0xffff) + " (" +
						JavaFormat.fixed(resLoc.latitude, 4) + " " + JavaFormat.fixed(resLoc.longitude, 4) + ")"
			} else if (atom.isPoiCategory()) {
				return "\"" + words.toString().trim() + "\" [" + atom.typeStr() + "] '" + atom.name + "' id=" + atom.id +
						", obj=" + JavaFormat.grouped(atom.otherWordsCnt.toLong()) + " "
			}
			return atom.simpleName(words.toString())
		}

		fun getNameIndexAtom(): NameIndexAtom = atom

		companion object {
			internal const val MAX_TYPE_ORDER = 5
		}
	}

	fun getObjectsSize(): Int = objs.size

	fun matchedTokens(): Int = parent.tCount

	fun getParent(): SpatialSearchResultsList = parent

	fun sumOther(): Int {
		var s1 = 0
		for (r in objs) {
			s1 += r.otherWordsNotFound
		}
		return s1
	}

	fun sumTypeOrder(): Int {
		var s1 = 0
		for (r in objs) {
			s1 += r.typeOrder(false)
		}
		return s1
	}

	fun getMainRating(): Int {
		var elo = parent.MIN_ELO_RATING
		if (objs.size > 0) {
			val o = objs[0]
			elo = max(elo, o.atom.elo)
			val obj = o.atom.`object`
			if (obj is Amenity) {
				elo = max(elo, obj.getTravelEloNumber())
			}
			val united = unitedObject
			if (united != null) {
				elo = max(elo, united.getSyntheticAmenity().getTravelEloNumber())
			}
		}
		return elo
	}

	fun getTotalRating(): Int {
		var rating = getMainRating()
		for (r in objs) {
			val obj = r.atom.`object`
			if (obj is Amenity) {
				rating = max(rating, obj.getTravelEloNumber())
				rating = max(rating, r.atom.elo)
			}
		}
		return rating
	}

	fun compareKey(): Long = compareKey(this)

	private fun getBiggestCityType(): Int {
		if (biggestCityType == -1) {
			biggestCityType = CityType.entries.size
			for (ref in objs) {
				val obj = ref.atom.`object`
				if (obj is Street) {
					// immediately return as other <city> are not related to street <suburb>
					biggestCityType = obj.getCity()!!.getType().ordinal
					return biggestCityType
				} else if (obj is City) {
					biggestCityType = min(biggestCityType, obj.getType().ordinal)
				}
			}
		}
		return biggestCityType
	}

	override fun compareTo(other: SpatialSearchResult): Int = compare(this, other, null, null)

	private fun getWikidata(ctx: SpatialSearchContext?): String? {
		val mapObject = getFirstRefObject(true)
		if (mapObject != null) {
			return mapObject.getWikidata()
		}
		if (ctx == null) {
			return null
		}
		val cat = getPoiCategory(ctx.poiSearch)
		if (cat != null) {
			if (cat.wikidataId != null) {
				// suggest poi category for different cities in query (we can't chose which is better?)
				var suffixPos = ""
				if (objs.size > 1) {
					suffixPos += "_" + objs[1].tokens[0].word
				}
				return "TYPE_" + cat.wikidataId + suffixPos
			}
		}
		return null
	}

	private fun isRoute(): Boolean {
		// the first object can be the wiki article of the route (wiki_place with route_id)
		val obj = getFirstRefObject(false)
		return preciseLatlon == null && obj is Amenity && obj.getRouteId() != null
	}

	private fun getRouteId(): String? {
		val obj = getFirstRefObject(true)
		if (obj is Amenity) {
			return obj.getRouteId()
		}
		return null
	}

	private fun getShortLink(zoom: Int): String {
		val loc = getLatLon() ?: return ""
		return KMapUtils.createShortLinkString(loc.latitude, loc.longitude, zoom)
	}

	private fun getWikipedia(): String? {
		val obj = getFirstRefObject(true)
		if (obj is Amenity) {
			val wiki = obj.getAdditionalInfo(Amenity.WIKIPEDIA)
			if (wiki != null) {
				val split = WorldRegion.splitLikeJava(wiki, "/")
				return split[split.size - 1]
			}
		}
		return null
	}

	companion object {
		private val FILTER_DUPLICATE_POI_SUBTYPE: List<String> =
			listOf("building", "historic_building", "internet_access_yes", "atm")
		private const val ZOOM_SIMILARITY_70_KM = 9 - 8 // 1 symbol - tile z=9 - 1 pixel of z=1
		private const val ZOOM_SIMILARITY_10_KM = 12 - 8 // 2 symbols - tile z=12
		private const val ZOOM_SIMILARITY_1_KM = 15 - 8 // 3 symbols
		private const val ZOOM_SIMILARITY_10_M = 18 - 8 // 4 symbols

		private fun unitesSame(united: BaseDetailsObject, obj: MapObject): Boolean {
			for (o in united.getObjects()) {
				if (o === obj) {
					return true
				}
			}
			return false
		}

		private fun addCompareKey(key: Long, bits: Int, v: Int): Long {
			var value = v
			val max = (1 shl bits) - 1
			value = if (value < 0) {
				max(0, max + value)
			} else {
				min(max, value)
			}
			return (key shl bits) + value
		}

		fun compareKeyString(o: SpatialSearchResult): String {
			val sw = if (o.surplusWords >= 0) ("+" + o.surplusWords) else ("" + o.surplusWords)
			if (o.parent.ranking != null) {
				return "t" + o.parent.tCount + sw + "-w" + o.objs.size + "-sc" + JavaFormat.fixed(o.score, 2)
			}
			val e = (o.getTotalRating() - o.parent.MIN_ELO_RATING) / 64
			val elo = if (e > 0) "-" + e + "elo" else ""
			return "t" + o.parent.tCount + sw + "-w" + o.objs.size + "-oth" + min(o.sumOther(), 3) + elo +
					"-tp" + o.sumTypeOrder()
		}

		fun compareKey(o: SpatialSearchResult): Long {
			var key = 0L
			key = addCompareKey(key, 6, -o.parent.tCount) // 6 bit - 64
			key = addCompareKey(key, 3, -o.surplusWords) // 3 bit - 8
			val ranking = o.parent.ranking
			if (ranking != null) {
				// only the tiers that make an answer a DIFFERENT KIND of answer: the score is a
				// continuum, and cutting it into buckets put "show more" after the third row in half
				// of all queries
				key = addCompareKey(key, 6, ranking.answerParts(o)) // 6 bit - 64
				return key
			}
			key = addCompareKey(key, 6, o.objs.size) // 6 bit - 64
			key = addCompareKey(key, 3, min(o.sumOther(), 3)) // 3 bit - 3
			key = addCompareKey(key, 6, -(o.getTotalRating() - o.parent.MIN_ELO_RATING) / 64) // 6 bit - 64 - group by 64 bucket
			key = addCompareKey(key, 6, -o.sumTypeOrder()) // 6 bit - 64
			// total 6+6+3+5+6+12 = 35
			return key
		}

		fun compare(o1: SpatialSearchResult, o2: SpatialSearchResult, center: KLatLon?, ranking: SpatialSearchRanking?): Int {
			var res = -o1.isPoiCategory().compareTo(o2.isPoiCategory())
			if (res != 0) {
				return res
			}
			res = -o1.parent.tCount.compareTo(o2.parent.tCount)
			if (res != 0) {
				return res
			}
			res = -o1.surplusWords.compareTo(o2.surplusWords) // buildings 18 matches 18 B
			if (res != 0) {
				return res
			}
			if (ranking != null) {
				res = ranking.answerParts(o1).compareTo(ranking.answerParts(o2))
				if (res != 0) {
					return res
				}
				res = -o1.score.compareTo(o2.score) // the 7 tiers below, see SpatialSearchRanking
				if (res != 0) {
					return res
				}
				if (o1.isPoiCategory() && o2.isPoiCategory()) {
					// the more objects a category has, the higher
					res = -o1.getFirstRef()!!.atom.otherFoundCnt.compareTo(o2.getFirstRef()!!.atom.otherFoundCnt)
					if (res != 0) {
						return res
					}
				}
			} else {
				res = o1.objs.size.compareTo(o2.objs.size)
				if (res != 0) {
					return res
				}
				res = o1.sumOther().compareTo(o2.sumOther())
				if (res != 0) {
					return res
				}
				res = -o1.getMainRating().compareTo(o2.getMainRating())
				if (res != 0) {
					return res
				}
				// sort poi intersection differently
				res = -o1.getTotalRating().compareTo(o2.getTotalRating())
				if (res != 0) {
					return res
				}
				res = -o1.sumTypeOrder().compareTo(o2.sumTypeOrder())
				if (res != 0) {
					return res
				}
				if (center != null) {
					val d1 = getDistance(o1, center)
					val d2 = getDistance(o2, center)
					if (d1.toInt() != d2.toInt()) {
						res = d1.compareTo(d2)
						if (res != 0) {
							return res
						}
					}
				}
			}
			res = o1.getBiggestCityType().compareTo(o2.getBiggestCityType())
			if (res != 0) {
				return res
			}

			val a1 = o1.getFirstRefObject(false)
			val a2 = o2.getFirstRefObject(false)
			if (a1 is Amenity && a2 is Amenity) {
				val i1 = FILTER_DUPLICATE_POI_SUBTYPE.indexOf(a1.getSubType())
				val i2 = FILTER_DUPLICATE_POI_SUBTYPE.indexOf(a2.getSubType())
				res = i1.compareTo(i2)
				if (res != 0) {
					return res
				}
			}
			return -o1.getFirstRef()!!.atom.id.compareTo(o2.getFirstRef()!!.atom.id)
		}

		internal fun getDistance(o1: SpatialSearchResult, center: KLatLon): Double {
			val latLon = o1.getLatLon()
			var d1 = if (latLon == null) 0.0 else KMapUtils.getDistance(center, latLon)
			val obj = o1.getFirstRefObject(false)
			if (obj is City) {
				// distance to center shorten by its radius (so boundary will be sorted down comparing to city)
				d1 -= obj.getType().getRadius()
			}
			return d1
		}
	}
}
