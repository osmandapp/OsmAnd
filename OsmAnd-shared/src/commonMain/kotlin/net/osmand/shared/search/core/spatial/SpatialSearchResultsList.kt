package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.BinaryMapPoiReaderAdapter
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.Building
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.search.core.HashQuadTree
import net.osmand.shared.search.core.HashSkipTileQuadTree
import net.osmand.shared.search.core.HashSkipTileQuadTreeJoiner
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtom
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import net.osmand.shared.util.JavaFormat
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTIntObjectMap
import net.osmand.shared.util.collections.KTLongArrayList
import net.osmand.shared.util.collections.KTLongHashSet
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The combinations of the atoms of some words of the query that lie in one place: one row per
 * combination, one atom of each word, and the objects of the atoms read to check the houses, the refs
 * and the crossings of the streets.
 *
 * A copy of `SpatialSearchResultsList` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS. Without `getRawAtomsSumId`, which only a line commented out in java calls.
 */
class SpatialSearchResultsList : Comparable<SpatialSearchResultsList> {

	internal val tokens: Array<SpatialSearchToken> // non modifiable!
	internal val tCount: Int

	internal var MIN_ELO_RATING = Amenity.DEFAULT_ELO
	internal var ranking: SpatialSearchRanking? = null

	// NameIndexAtom[][] -- should be double array to store list of combinations
	internal val linearResults = ArrayList<NameIndexAtom>()
	internal val typeIntersections = KTIntArrayList()
	internal val tileIds = KTLongArrayList()
	internal val tileZooms = KTIntArrayList()
	internal val quadTree = HashQuadTree<Int>(16)
	internal val quadTreeSkip = HashSkipTileQuadTree<Int>()

	internal var limitIntersection = -1

	internal val skipResults = KTIntObjectMap<Boolean>()
	internal val preciseLocations = HashMap<Int, KLatLon>()
	internal val extraNameMatch = HashMap<Int, String>()
	internal val surplusWords = HashMap<Int, Int>()
	internal var finalResult: MutableList<SpatialSearchResult>? = null

	internal val tempBuildNames1 = ArrayList<String>()
	internal val tempBuildNames2 = ArrayList<String>()
	internal val extraIdsResults = HashMap<String, SpatialSearchResult>()
	internal val uniqueIdsResults = KTLongObjectMap<SpatialSearchResult>()

	constructor() : this(null, null, null)

	internal constructor(ctx: SpatialSearchContext?, tokensList: List<SpatialSearchToken>?) {
		if (tokensList == null || tokensList.isEmpty()) {
			this.tokens = emptyArray()
		} else {
			this.tokens = tokensList.toTypedArray()
		}
		if (ctx != null) {
			MIN_ELO_RATING = ctx.settings.MIN_ELO_RATING
			ranking = ctx.ranking
		}
		this.tCount = this.tokens.size
	}

	constructor(ctx: SpatialSearchContext?, token: SpatialSearchToken?, parent: SpatialSearchResultsList?) {
		if (token == null) {
			tokens = emptyArray()
		} else {
			val parentTokens = parent!!.tokens
			tokens = Array(parentTokens.size + 1) { k -> if (k == 0) token else parentTokens[k - 1] }
		}
		tCount = tokens.size
		if (ctx != null) {
			MIN_ELO_RATING = ctx.settings.MIN_ELO_RATING
			ranking = ctx.ranking
		}
		if (parent != null) {
			limitIntersection = if (parent.limitIntersection == -1) ctx!!.limitLocationBboxes.size else parent.limitIntersection
			calculateMainIntersection(ctx!!, token!!, parent)
		}
	}

	private fun loadObjects(ctx: SpatialSearchContext, type: Int, cache: KTLongObjectMap<MapObject>) {
		val lstMap = KTLongObjectMap<Long>()

		val poiBboxes = LinkedHashMap<Int, KTLongHashSet>()
		for (a in linearResults) {
			val obj = a.`object`
			if (obj != null) {
				cache.put(a.id, obj)
				continue
			}
			if (a.type == SpatialSearchToken.POI_TYPE) {
				val indInd = ctx.getFileInd(a.id)
				poiBboxes.getOrPut(indInd) { KTLongHashSet() }.add(
					HashQuadTree.encodeTileId31(
						BinaryMapPoiReaderAdapter.EVAL_TAG_GROUP_ZOOM,
						a.coords.x16 shl 15, a.coords.y16 shl 15
					)
				)
			}
			if (type == SpatialSearchToken.STREET_TYPE && a.isStreetBuilding()) {
				lstMap.put(a.id, a.parentid)
			} else if (a.type == type || (type == SpatialSearchToken.ALL_CITY_TYPE && !a.isPOI()
						&& !a.isStreetBuilding() && !a.isPoiCategory())
			) {
				lstMap.put(a.id, a.parentid)
			} else if (type == SpatialSearchToken.ALL_CITY_TYPE && a.isStreetBuilding()) {
				lstMap.put(a.parentid, 0L)
			}
		}
		for ((key, value) in poiBboxes) {
			if (ctx.isCancelled()) {
				return
			}
			ctx.readPOIBboxes(key, value)
		}
		val lst = lstMap.keys()
		lst.sort() // sort is not correct for file ind last bits >>> 12
		val wanted = KTLongHashSet(lst.size)
		var i = 0
		while (type == SpatialSearchToken.POI_TYPE && i < lst.size) {
			wanted.add(SpatialSearchContext.poiObjectId(lst[i]))
			i++
		}
		for (id in lst) {
			if (ctx.isCancelled()) {
				return
			}
			// java puts null for an object that could not be read: the same as no object in the cache
			val obj = if (type == SpatialSearchToken.POI_TYPE) {
				ctx.readPoiObject(id, cache, wanted)
			} else {
				ctx.readAddrObject(id, lstMap[id]!!, cache)
			}
			if (obj != null) {
				cache.put(id, obj)
			} else {
				cache.remove(id)
			}
		}
		for (a in linearResults) {
			val mo = cache[a.id]
			if (mo != null) {
				a.`object` = mo
			}
		}
	}

	fun loadObjects(ctx: SpatialSearchContext) {
		if (ctx.isCancelled()) {
			return
		}
		val cache = KTLongObjectMap<MapObject>()
		loadObjects(ctx, SpatialSearchToken.POI_TYPE, cache)
		cache.clear()
		loadObjects(ctx, SpatialSearchToken.ALL_CITY_TYPE, cache)
		loadObjects(ctx, SpatialSearchToken.STREET_TYPE, cache)
	}

	fun getMissingTokens(ctx: SpatialSearchContext): List<SpatialSearchToken> {
		val lst = ArrayList(ctx.tokens!!)
		for (s in tokens) {
			lst.remove(s)
		}
		return lst
	}

	fun loadObjectsAndCalcBuildings(ctx: SpatialSearchContext) {
		ctx.stats.sub2LoadObjectsBldTime.start()
		loadObjects(ctx)
		if (ctx.isCancelled()) {
			return
		}
		val missingTokens = getMissingTokens(ctx)
		if (ctx.settings.SEARCH_POI_REF) {
			for (indx in 0 until getCombinations()) {
				if (ctx.isCancelled()) {
					return
				}
				if (!skipResults.containsKey(indx)) {
					checkAmenityRef(missingTokens, indx)
				}
			}
		}
		if (ctx.settings.SEARCH_BUILDINGS) {
			val bldCheckCache = HashMap<String, BuildingCache>()
			for (indx in 0 until getCombinations()) {
				if (ctx.isCancelled()) {
					return
				}
				if (!skipResults.containsKey(indx)) {
					calcBuilding(ctx, indx, missingTokens, bldCheckCache)
				}
			}
		}
		if (ctx.settings.SEARCH_STREET_INTERSECTIONS) {
			for (indx in 0 until getCombinations()) {
				if (ctx.isCancelled()) {
					return
				}
				if (!skipResults.containsKey(indx)) {
					calcStreetIntersections(ctx, indx)
				}
			}
		}
		// filter incomplete brand name if there are enough results
		filterIncompleteBrands(ctx)
		ctx.stats.sub2LoadObjectsBldTime.finish()
	}

	private fun filterIncompleteBrands(ctx: SpatialSearchContext) {
		for (indx in 0 until getCombinations()) {
			if (!skipResults.containsKey(indx)) {
				var poiTypeTokens = 0
				var poiType: NameIndexAtom? = null
				var partOnBuilding = false
				val categoryWords = ArrayList<String>()
				for (i in 0 until tCount) {
					val atom = linearResults[indx * tCount + i]
					if (atom.isPoiCategory()) {
						partOnBuilding = partOnBuilding or (tokens[i].likelyPartOfBuilding() || tokens[i].getMainNumber() > 0)
						poiTypeTokens++
						poiType = atom
						categoryWords.add(KSearchAlgorithms.alignChars(tokens[i].word.lowercase()))
					}
				}
				// the category words must cover a name of the type they match, not the shortest name of the type
				if (poiTypeTokens > 0 && tCount > 1 &&
					ctx.poiSearch.getById(poiType!!.id.toInt())!!.tokensInMatchedName(categoryWords) > poiTypeTokens
				) {
					skipResults.put(indx, true)
				} else if (tCount > 1 && poiTypeTokens == 1 && partOnBuilding) {
					skipResults.put(indx, true)
				}
			}
		}
	}

	private fun checkAmenityRef(missingTokens: List<SpatialSearchToken>, indx: Int) {
		var queryRef: String? = null
		var objectRef: Set<String>? = null
		for (refInd in 0 until tCount) {
			val refAtom = linearResults[indx * tCount + refInd]
			if (refAtom.buildingOrRefInd >= 0 && refAtom.isPOI()) {
				val amenityTokenInd = getTokenByOriginalOrder(refAtom.buildingOrRefInd)
				if (amenityTokenInd < 0) {
					skipResults.put(indx, true)
					return
				}
				var wrong = false
				var poiAtom: NameIndexAtom? = null
				// mix of 2 refs or not present or street
				for (k in 0 until tCount) {
					val check = linearResults[indx * tCount + k]
					if (check.isPOI() && !check.isPOIRef()) {
						if (check.id == refAtom.id) {
							poiAtom = check
						} else {
							wrong = true
							break
						}
					}
				}
				if (poiAtom == null || poiAtom.`object` == null || wrong) {
					skipResults.put(indx, true)
					return
				}
				val asAmenity = poiAtom.`object` as Amenity
				var ref = asAmenity.getAdditionalInfo("ref")
				if (ref == null) {
					ref = asAmenity.getAdditionalInfo("ref_post")
				}
				if (objectRef == null && ref != null) {
					objectRef = KSearchAlgorithms.getBuildingCompareSet(ref, tempBuildNames2)
				} else {
					val num = KAlgorithms.extractIntegerNumber(asAmenity.getName())
					if (num > 0) {
						val objectSet = KSearchAlgorithms.getBuildingCompareSet(poiAtom.name, tempBuildNames2)
						// don't count twice (as query already assigned)
						val numInTheName = num.toString()
						if (!objectSet.contains(numInTheName)) {
							objectRef = setOf(numInTheName)
						}
					}
				}
				if (queryRef == null) {
					queryRef = tokens[refInd].word
				} else {
					queryRef += " " + tokens[refInd].word
				}
			}
		}
		if (queryRef != null) {
			val querySetRef = KSearchAlgorithms.getBuildingCompareSet(queryRef, tempBuildNames1)
			var match = -1
			if (objectRef != null && objectRef.isNotEmpty()) {
				if (objectRef == querySetRef) {
					// no use case to count duplicates
					match = 0
				} else if (querySetRef.size == objectRef.size + 1 && querySetRef.containsAll(objectRef)) {
					match = 1
				}
			}
			if (match >= 0) {
				extraNameMatch[indx] = queryRef
				if (match > 0) {
					surplusWords[indx] = -match
				}
			} else {
				skipResults.put(indx, true)
			}
		}
	}

	private fun calcStreetIntersections(ctx: SpatialSearchContext, indx: Int) {
		var first: NameIndexAtom? = null
		var second: NameIndexAtom? = null
		for (i in 0 until tCount) {
			val atom = linearResults[indx * tCount + i]
			if (atom.`object` is Street && !atom.isCityStreetName()) {
				if (first == null || first.`object`!!.getId()!! == atom.`object`!!.getId()) {
					first = atom
				} else {
					second = atom
				}
			}
		}
		val s1 = first?.`object`
		val s2 = second?.`object`
		if (first != null && s1 is Street && second != null && s2 is Street) {
			var insLoc: KLatLon? = null
			for (ins in s1.getIntersectedStreets()) {
				if (ins.getName() == s2.getName()) {
					insLoc = ins.getLocation()
					break
				}
			}
			if (insLoc == null) {
				for (ins in s2.getIntersectedStreets()) {
					if (ins.getName() == s1.getName()) {
						insLoc = ins.getLocation()
						break
					}
				}
			}
			if (insLoc == null && ctx.settings.ALLOW_VIRTUAL_STREET_INTERSECTIONS &&
				s2.getIntersectedStreets().isNotEmpty() && s1.getIntersectedStreets().isNotEmpty()
			) {
				// some virtual streets from addr:place end up without names and with 0 intersected streets Venezia
				val l1 = s1.getLocation()!!
				val l2 = s2.getLocation()!!
				insLoc = KLatLon(l1.latitude / 2 + l2.latitude / 2, l1.longitude / 2 + l2.longitude / 2)
			}
			if (insLoc != null && !first.isCityStreetName() && !second.isCityStreetName()) {
				preciseLocations[indx] = insLoc
			} else {
				skipResults.put(indx, true)
			}
		}
	}

	private class BuildingCache(val bld: Building?, val indx: Int, val loc: KLatLon?, val surplus: Int)

	private fun calcBuilding(
		ctx: SpatialSearchContext, indx: Int, missingTokens: List<SpatialSearchToken>,
		bldCheckCache: MutableMap<String, BuildingCache>
	) {
		val blds = ArrayList<NameIndexAtom>()
		var searchKey = ""
		var noBldStreet: NameIndexAtom? = null
		for (i in 0 until tCount) {
			val bld = linearResults[indx * tCount + i]
			if (bld.buildingOrRefInd >= 0 && !bld.isPOI() && !bld.isPoiCategory()) {
				val strTokenInd = getTokenByOriginalOrder(bld.buildingOrRefInd)
				if (strTokenInd < 0) {
					skipResults.put(indx, true)
					break
				}
				val missing = false
				var otherStreet = false
				for (k in 0 until tCount) {
					val streetAtom = linearResults[indx * tCount + k]
					if (streetAtom.isStreet()) {
						if (streetAtom.id != bld.id) {
							otherStreet = true
							break
						}
					}
				}
				if (missing || (otherStreet && !bld.isCityStreetName())) {
					skipResults.put(indx, true)
					return
				}
				blds.add(bld)
				searchKey += tokens[i].word + " "
			} else if (bld.isStreet() && bld.isCityStreetName()) {
				noBldStreet = bld
			}
		}
		if (blds.size == 0 && noBldStreet != null) {
			// don't display such streets
			skipResults.put(indx, true)
		}
		// check many buildings on same street possibly unit or ref
		if (blds.size > 0) {
			val bldRefObj = blds[0]
			val bldName = searchKey.trim()
			val cacheKey = bldRefObj.id.toString() + " " + bldName
			var bldObj = bldCheckCache[cacheKey]
			var loc: KLatLon? = null
			if (bldObj == null) {
				val matchExtraWord = IntArray(1)
				val bldres = checkBuilding(ctx, bldRefObj, bldRefObj.`object` as Street, bldName, matchExtraWord)
				loc = null
				if (bldres != null) {
					loc = if (bldres.isInterpolation()) bldres.getLocation(bldres.interpolation(bldName)) else null
				}
				bldObj = BuildingCache(bldres, indx, loc, matchExtraWord[0])
				bldCheckCache[cacheKey] = bldObj
			}
			val bld = bldObj.bld
			if (bld == null) {
				// no such house in the map: offer the street under the houses found, as 18 for 18 B
				surplusWords[indx] = -1
			} else if (!checkBuildingPoiLocation(ctx, indx, bld, loc)) {
				skipResults.put(indx, true)
			} else {
				// assign buildings
				val bldObject = bldRefObj.bldObject
				if (bldObject == null || bldObject.getName().length < bld.getName().length) {
					bldRefObj.bldObject = bld
				}
				if (bldObj.surplus != 0) {
					surplusWords[indx] = bldObj.surplus
				}
				if (bldObj.loc != null) {
					preciseLocations[indx] = bldObj.loc
					extraNameMatch[indx] = bldName
				}
			}
		}
	}

	private fun checkBuildingPoiLocation(ctx: SpatialSearchContext, indx: Int, bld: Building, location: KLatLon?): Boolean {
		val loc = location ?: bld.getLocation()
		val lst = getRawAtoms(indx)
		var bldBbox: IntArray? = null
		for (p in lst) {
			if (p.isPOI()) {
				if (bldBbox == null) {
					val qr = KMapUtils.calculate31BboxUsingRhumb(ctx.settings.POI_HOUSE_DEFAULT_RADIUS, loc!!)
					bldBbox = intArrayOf(qr.left.toInt(), qr.top.toInt(), qr.right.toInt(), qr.bottom.toInt())
				}
				if (!p.coords.intersects(bldBbox)) {
					return false
				}
			}
		}
		return true
	}

	private fun checkBuilding(ctx: SpatialSearchContext, atom: NameIndexAtom, street: Street, bld: String, matchExtraWord: IntArray): Building? {
		var interpolation: Building? = null
		var partial2: Building? = null
		var distPartial1 = 0.0
		var partial1: Building? = null
		var distExact = 0.0
		var exact: Building? = null
		val query = KSearchAlgorithms.getBuildingCompareSet(bld, tempBuildNames1)
		for (b in street.getBuildings()) {
			if (b.isInterpolation()) {
				// interpolation only over 1 set
				if (query.size == 1 && b.belongsToInterpolation(query.iterator().next())) {
					interpolation = b
				} else if (query.size > 1 && b.belongsToInterpolation(bld)) {
					interpolation = b
				}
			} else {
				val bldSet = KSearchAlgorithms.getBuildingCompareSet(b.getName(), tempBuildNames2)
				if (bldSet == query) {
					// exact match but could be duplicates 8-8
					// duplicate could be in query or in house number
					if ((tempBuildNames2.size > bldSet.size || tempBuildNames1.size > query.size)
						&& tempBuildNames2 != tempBuildNames1
					) {
						val d = if (ctx.location == null) 0.0 else KMapUtils.getDistance(ctx.location, b.getLocation()!!)
						// just for tests makes sense
						if (distPartial1 == 0.0 || d < distPartial1) {
							partial1 = b
							distPartial1 = d
						}
					} else {
						// just for tests makes sense
						val d = if (ctx.location == null) 0.0 else KMapUtils.getDistance(ctx.location, b.getLocation()!!)
						if (distExact == 0.0 || d < distExact) {
							exact = b
							distExact = d
						}
					}
				}
				if (bldSet.size == query.size + 1 || bldSet.size == query.size + 2) {
					// case data only 18-B present, 18 searched (151 + unit Upper level)
					if (bldSet.containsAll(query)) {
						val d = if (ctx.location == null) 0.0 else KMapUtils.getDistance(ctx.location, b.getLocation()!!)
						// just for tests makes sense
						if (distPartial1 == 0.0 || d < distPartial1) {
							partial1 = b
							distPartial1 = d
						}
					}
				} else if (bldSet.size + 1 == query.size) {
					// case data only 18 present, 18-B searched
					if (query.containsAll(bldSet)) {
						partial2 = b
					}
				}
			}
		}
		if (exact != null) {
			return exact
		}
		if (partial1 != null) {
			if (tempBuildNames1.size > query.size) {
				matchExtraWord[0] = -1
			}
			return partial1
		}
		if (partial2 != null && query.size > 1) {
			matchExtraWord[0] = -1
			return partial2
		}
		if (interpolation != null) {
			if (query.size > 1) {
				matchExtraWord[0] = -(query.size - 1)
			}
			if (tempBuildNames1.size > 1) {
				matchExtraWord[0] = -(tempBuildNames1.size - 1)
			}
			return interpolation
		}
		if (partial2 != null) {
			matchExtraWord[0] = -1
			return partial2
		}
		return null
	}

	private fun getTokenByOriginalOrder(originalOrder: Int): Int {
		for (ind in tokens.indices) {
			if (tokens[ind].originalOrder == originalOrder) {
				return ind
			}
		}
		return -1
	}

	fun getFirstToken(): SpatialSearchToken? = if (tokens.isEmpty()) null else tokens[0]

	fun nearbyResult(ind: Int): Int {
		var min = 0
		for (i in 0 until tCount) {
			val ni = linearResults[ind * tCount + i]
			min = max(ni.nearbyRadius, min)
		}
		return min
	}

	fun getRawAtoms(ind: Int): List<NameIndexAtom> = linearResults.subList(ind * tCount, (ind + 1) * tCount)

	fun getCombinations(): Int = tileIds.size()

	fun getTokenCount(): Int = tCount

	fun getFinalResult(): List<SpatialSearchResult>? = finalResult

	fun sortResults(ctx: SpatialSearchContext, deduplicate: Boolean): List<SpatialSearchResult> {
		var res: MutableList<SpatialSearchResult> = ArrayList(tileIds.size())
		for (i in 0 until tileIds.size()) {
			if (!skipResults.containsKey(i)) {
				res.add(SpatialSearchResult(this, i, preciseLocations[i], extraNameMatch[i], surplusWords[i]))
			}
		}
		res = sortResults(ctx, res, deduplicate)
		finalResult = res
		return res
	}

	fun sortResults(ctx: SpatialSearchContext, results: MutableList<SpatialSearchResult>, deduplicate: Boolean): MutableList<SpatialSearchResult> {
		var finalResult = results
		val ranking = ctx.ranking
		if (ranking != null) {
			for (r in finalResult) {
				r.score = ranking.score(r, ctx.location)
			}
		}
		finalResult.sortWith { o1, o2 -> SpatialSearchResult.compare(o1, o2, ctx.location, ctx.ranking) }
		if (deduplicate) {
			uniqueIdsResults.clear()
			extraIdsResults.clear()
			val result = ArrayList<SpatialSearchResult>()
			// the same osm object can come without wikidata from one map and with it from another (Khotiv, #25940)
			val wikidataByOsmId = HashMap<Long, String>()
			for (s in finalResult) {
				val o = s.getMainObject()
				val wikidata = o?.getWikidata()
				if (o != null && !KAlgorithms.isEmpty(wikidata) && s.getIdDeduplication() != -1L) {
					val id = s.getIdDeduplication()
					if (!wikidataByOsmId.containsKey(id)) {
						wikidataByOsmId[id] = wikidata!!
					}
				}
			}

			for (s in finalResult) {
				var isUniq = true
				val uniqueId = s.getIdDeduplication()
				val existing = uniqueIdsResults[uniqueId]
				if (existing != null) {
					existing.addExtraResult(s, ctx.settings.LANG_DEDUPLICATE)
					existing.moveToNearestRouteSegment(ctx.location)
					isUniq = false
				} else if (uniqueId != -1L) {
					uniqueIdsResults.put(uniqueId, s)
				}
				var extraDuplicateKeys = s.extraDeduplicateKeys(ctx)
				val o = s.getMainObject()
				val wikidata = if (o == null || !KAlgorithms.isEmpty(o.getWikidata())) null else wikidataByOsmId[uniqueId]
				if (wikidata != null) {
					val keys = if (extraDuplicateKeys == null) ArrayList() else ArrayList(extraDuplicateKeys)
					keys.add(wikidata)
					extraDuplicateKeys = keys
				}
				if (extraDuplicateKeys != null) {
					for (key in extraDuplicateKeys) {
						val unique = extraIdsResults[key]
						if (unique != null) {
							unique.addExtraResult(s, ctx.settings.LANG_DEDUPLICATE)
							unique.moveToNearestRouteSegment(ctx.location)
							isUniq = false
						} else {
							extraIdsResults[key] = s
						}
					}
				}
				if (isUniq) {
					result.add(s)
				}
			}
			finalResult = deduplicateByProximity(result, ctx)
		}
		return finalResult
	}

	private fun dedupName(r: SpatialSearchResult): String? {
		val o = r.getMainObject()
		val name = o?.getName()
		if (KAlgorithms.isEmpty(name)) {
			return null
		}
		val n = KSearchAlgorithms.normalizeToken(KSearchAlgorithms.alignChars(name!!))
		return if (KAlgorithms.isEmpty(n)) null else n.trim().lowercase()
	}

	private fun deduplicateByProximity(sorted: List<SpatialSearchResult>, ctx: SpatialSearchContext): MutableList<SpatialSearchResult> {
		// first pass: name every row and chain it to the previous row of that name
		val lastSameName = HashMap<String, SpatialSearchResult>()
		for (s in sorted) {
			val name = dedupName(s)
			s.dedupName = name
			if (name != null) {
				s.prevDedupSameName = lastSameName.put(name, s)
			}
		}
		val out = ArrayList<SpatialSearchResult>(sorted.size)
		// quick check - a category search asked for all the objects themselves
		val categoryId = if (sorted.isNotEmpty() && sorted[0].isPoiCategory()) sorted[0].getFirstRef()!!.atom.id.toInt() else -1
		for (s in sorted) {
			var same: SpatialSearchResult? = null
			if (s.dedupName != null && s.getLatLon() != null && !s.isPoiCategory() && !isOfCategory(s, categoryId)) {
				var prev = s.prevDedupSameName
				var i = 0
				while (i < MAX_SAME_NAME && prev != null) {
					if (prev.getLatLon() != null && isSamePlace(prev, s)) {
						same = prev
						break
					}
					prev = prev.prevDedupSameName
					i++
				}
			}
			if (same == null) {
				out.add(s)
			}
		}
		return out
	}

	private fun isSamePlace(a: SpatialSearchResult, b: SpatialSearchResult): Boolean {
		val distance = KMapUtils.getDistance(a.getLatLon()!!, b.getLatLon()!!)
		val alikeA = SpatialSearchRanking.isSubordinateNode(a)
		val alikeB = SpatialSearchRanking.isSubordinateNode(b)
		if (alikeA || alikeB) {
			// searched by name, what is named alike a place is absorbed by it: a stop
			// called after the street, the village, the station it stands at - pref-0001, pref-0082, pref-0103, pref-0108;
			if (alikeA != alikeB && SpatialSearchRanking.isProminent(if (alikeA) a else b)) {
				return false
			}
			return distance <= SAME_FACILITY_M
		}
		val streetA = a.getMainObject()
		val streetB = b.getMainObject()
		val street = streetA is Street
		if (street != (streetB is Street)) {
			return false
		}
		if (streetA is Street && streetB is Street) {
			// a line's coordinate says little, so the city decides: pref-0107
			val c1 = streetA.getCity()
			val c2 = streetB.getCity()
			return c1 != null && c2 != null && c1.getName() == c2.getName() && distance <= SAME_STREET_M
		}
		if (isMetro(a) != isMetro(b)) {
			return false
		}
		return distance <= SAME_PLACE_M
	}

	private fun isMetro(r: SpatialSearchResult?): Boolean {
		val head = r?.getFirstRef()
		val obj = head?.atom?.`object`
		return obj is Amenity && obj.getAdditionalInfo("subway_station") != null
	}

	private fun isOfCategory(r: SpatialSearchResult, categoryId: Int): Boolean {
		val head = r.getFirstRef()
		return categoryId != -1 && head != null && head.atom.poiTypes != null && head.atom.poiTypes!!.contains(categoryId)
	}

	private fun iterateIntersection(
		parent: SpatialSearchResultsList, token: SpatialSearchToken,
		settings: SpatialTextSearchSettings, iterate: (parentIndx: Int, atom: NameIndexAtom, atomIndex: Int) -> Unit
	) {
		if (settings.DEV_USE_SKIP_HASH_TREE) {
			token.quadTreeSkip.build()
			val joiner = HashSkipTileQuadTreeJoiner(token.quadTreeSkip, parent.quadTreeSkip)
			joiner.joinAllBuckets { i1, i2 ->
				iterate(i2.obj, token.atoms[i1.obj], i1.obj)
			}
			quadTreeSkip.build()
			return
		}
		// 1. iterate parent objects and find all objects from <parent>
		//    that are fully inside object <token> or have same the same tile
		for (i in 0 until parent.tileIds.size()) {
			val tileId = parent.tileIds[i]
			val zoom = parent.tileZooms[i]
			token.quadTree.forEachMatch(zoom, tileId) { atoms ->
				for (ij in atoms.indices) {
					val indxAtom = atoms[ij]
					iterate(i, token.atoms[indxAtom], indxAtom)
				}
			}
		}
		// 2. reverse search quad tree from <token> and find objects
		//    that are fully inside any object <parent> and not the same the same tile!
		for (indxAtom in token.atoms.indices) {
			val atom = token.atoms[indxAtom]
			parent.quadTree.forEachMatchHigherZoom(atom.coords.bboxTileZoom, atom.coords.bboxTileId) { indxs ->
				for (indx in indxs) {
					iterate(indx, atom, indxAtom)
				}
			}
		}
	}

	private fun calculateMainIntersection(ctx: SpatialSearchContext, token: SpatialSearchToken, parent: SpatialSearchResultsList) {
		if (parent.getTokenCount() == 0) {
			for (indxAtom in token.atoms.indices) {
				val atom = token.atoms[indxAtom]
				if (token.deletedAtoms.contains(indxAtom.toLong())) {
					continue
				}
				addResult(null, 0, atom, 0, ctx.settings)
			}
		} else if (parent.getCombinations() > 0) {
			val nt = nanoTime()
			val intersections = Array(limitIntersection + 1) { KTIntArrayList() }
			val interPoiStreet = Array(limitIntersection + 1) { KTIntArrayList() } // type 1
			val interPoiPoiOrStreetStreet = Array(limitIntersection + 1) { KTIntArrayList() } // type 2
			val originalLimit = limitIntersection
			val typeIntersection = intArrayOf(0)
			iterateIntersection(parent, token, ctx.settings) { parentIndx, atom, indxAtom ->
				if (token.deletedAtoms.contains(indxAtom.toLong())) {
					return@iterateIntersection
				}
				val level = max(atom.nearbyRadius, parent.nearbyResult(parentIndx))
				if (level > limitIntersection) {
					return@iterateIntersection
				}

				val acceptIntersection = acceptIntersection(ctx, parent, parentIndx, token, atom, typeIntersection)
				if (acceptIntersection) {
					var c = intersections[level]
					if (typeIntersection[0] == 2) {
						c = interPoiPoiOrStreetStreet[level]
					} else if (typeIntersection[0] == 1) {
						c = interPoiStreet[level]
					}
					c.add(parentIndx)
					c.add(indxAtom)
					c.add(typeIntersection[0])
				}
			}
			val sizes = ArrayList<String>()
			for (k in intersections.indices) {
				sizes.add(
					JavaFormat.grouped((intersections[k].size() / 3).toLong()) + "/" +
							JavaFormat.grouped((interPoiStreet[k].size() / 3).toLong()) + "/" +
							JavaFormat.grouped((interPoiPoiOrStreetStreet[k].size() / 3).toLong())
				)
			}
			val res = KTIntArrayList()
			val newLevel = addResIntersections(ctx.settings.OPTIM_LIMIT_INTERSECTIONS * 3, intersections, limitIntersection, res)
			addResIntersections(ctx.settings.OPTIM_LIMIT_INTERSECTIONS * 3, interPoiStreet, newLevel, res)
			addResIntersections(ctx.settings.OPTIM_LIMIT_INTERSECTIONS * 3, interPoiPoiOrStreetStreet, newLevel, res)

			if (ctx.stats.printLogs) {
				println(
					"Intersect (" + JavaFormat.fixed((nanoTime() - nt) / 1e6, 0) + " ms) /\\: " + sizes + " (" + originalLimit +
							") -> " + JavaFormat.grouped((res.size() / 3).toLong()) + " (" + JavaFormat.grouped(newLevel.toLong()) +
							"): '" + token.originalWord + "' (" + JavaFormat.grouped(token.atoms.size.toLong()) + ") + " +
							parent.wordTokens() + " (" + JavaFormat.grouped(parent.getCombinations().toLong()) + ")"
				)
			}
			limitIntersection = newLevel
			val it = res.iterator()
			while (it.hasNext()) {
				val parentIndx = it.next()
				val indxAtom = it.next()
				val type = it.next()
				addResult(parent, parentIndx, token.atoms[indxAtom], type, ctx.settings)
			}
		}
		if (ctx.settings.DEV_USE_SKIP_HASH_TREE) {
			quadTreeSkip.build()
		}
	}

	private fun addResIntersections(limit: Int, intersections: Array<KTIntArrayList>, maxLevel: Int, res: KTIntArrayList): Int {
		var newLevel = 0
		for (level in 0..maxLevel) {
			val toAdd = intersections[level]
			if (res.size() == 0 || (level == 0 && maxLevel > 0) || res.size() + toAdd.size() < limit) {
				res.addAll(toAdd)
				newLevel = level
			} else {
				break
			}
		}
		return newLevel
	}

	fun sumTokenAtomSize(): Int {
		var sum = 0
		for (s in tokens) {
			sum += s.atoms.size
		}
		return sum
	}

	internal fun addResult(
		parent: SpatialSearchResultsList?, pindx: Int, a: NameIndexAtom, typeIntersection: Int,
		settings: SpatialTextSearchSettings
	): Boolean {
		finalResult = null
		val pzoom = if (parent == null) 0 else parent.tileZooms[pindx]
		val zoom = max(pzoom, a.coords.bboxTileZoom)
		val tileId = if (pzoom > a.coords.bboxTileZoom) parent!!.tileIds[pindx] else a.coords.bboxTileId
		val insIndx = this.tileIds.size()
		this.linearResults.add(a)
		var i = 0
		while (parent != null && i < parent.tCount) {
			this.linearResults.add(parent.linearResults[pindx * parent.tCount + i])
			i++
		}
		this.typeIntersections.add(typeIntersection)
		this.tileIds.add(tileId)
		this.tileZooms.add(zoom)
		quadTree.put(zoom, tileId, insIndx)
		if (settings.DEV_USE_SKIP_HASH_TREE) {
			val bbox31 = a.coords.bbox31!!
			val target = intArrayOf(bbox31[0], bbox31[1], bbox31[2], bbox31[3])
			var k = 0
			while (parent != null && k < parent.tCount) {
				val pa = parent.linearResults[pindx * parent.tCount + k]
				clipBbox(target, pa.coords.bbox31!!)
				k++
			}
			quadTreeSkip.addObject(insIndx, target, insIndx.toLong())
		}
		return true
	}

	private fun acceptIntersection(
		ctx: SpatialSearchContext, parent: SpatialSearchResultsList?, pindx: Int, token: SpatialSearchToken, a: NameIndexAtom,
		typeIntersection: IntArray
	): Boolean {
		val settings = ctx.settings
		typeIntersection[0] = -1
		val pCount = parent?.tCount ?: 0
		// 1. speed up for same id
		// Don't allow intersect potential building with other object
		for (i in 0 until pCount) {
			val pa = parent!!.linearResults[pindx * parent.tCount + i]
			if (pa.id == a.id) {
				typeIntersection[0] = parent.typeIntersections[pindx]
				continue
			}
			// pa and a using same tokens for street & house but different streets / poi same as below
			if (parent.tokens[i].originalOrder == a.buildingOrRefInd) {
				return false
			} else if (pa.buildingOrRefInd == token.originalOrder) {
				return false
			}
			// don't intersect building with other street except if street-building is city
			if ((pa.buildingOrRefInd >= 0) && a.isStreetBuilding() && !pa.isCityStreetName()) {
				return false
			} else if ((a.buildingOrRefInd >= 0) && pa.isStreetBuilding() && !a.isCityStreetName()) {
				return false
			}
			// if poi doesn't have bbox don't intersect or add bbox! (transport stops take street names)
			if (!ctx.settings.ALLOW_HOUSE_POI_TYPE_INTERSECTION) {
				if ((pa.buildingOrRefInd >= 0) && a.isPOI() && a.coords.bbox31 == null) {
					return false
				} else if ((a.buildingOrRefInd >= 0) && pa.isPOI() && pa.coords.bbox31 == null) {
					return false
				}
			}
		}
		// 2. Duplicate words in query
		for (i in 0 until pCount) {
			val pa = parent!!.linearResults[pindx * parent.tCount + i]
			if (pa.id == a.id && tokens[0].word == tokens[i + 1].word &&
				!pa.isBuilding() && !a.isBuilding()
			) {
				// NameIndexAtom supports "<word> <...> <word>" but it's not present in DATA now
				val indexOf = a.name.indexOf(tokens[0].word, pindx)
				if (indexOf != -1 && a.name.indexOf(tokens[0].word, indexOf + 1) >= 0) {
					// duplicate name in object
				} else {
					return false
				}
			}
		}
		var poiTypeToken = tokens[0]
		var poiType: NameIndexAtom? = if (a.isPoiCategory()) a else null
		var buildingPresent = a.isBuilding()
		for (i in 0 until pCount) {
			val pa = parent!!.linearResults[pindx * parent.tCount + i]
			if (pa.isBuilding()) {
				buildingPresent = true
			} else if (pa.isPoiCategory()) {
				if (poiType == null || pa.id == poiType.id) {
					poiType = pa
					poiTypeToken = parent.tokens[i]
				} else {
					// no 2 poi categories for now (vegan cafe)
					return false
				}
			}
		}
		if (poiType != null && buildingPresent) {
			return false
		}
		// speed up for same id checks
		// It affects correctness [results on duplicate words (buildings - unit test) see below]
		if (typeIntersection[0] >= 0) {
			return true
		}
		typeIntersection[0] = 0
		// 3. Precise intersection
		// no cache for parent now needed
		if (!settings.DEV_USE_SKIP_HASH_TREE) {
			// not needed for skip hash tree to be deleted
			var intersect = true
			for (i in 0 until pCount) {
				val pa = parent!!.linearResults[pindx * parent.tCount + i]
				if (!pa.coords.intersects(a.coords)) {
					intersect = false
					break
				}
			}
			if (!intersect) {
				return false
			}
		}
		// 4. New intersection check the limits
		// in the order the atoms come: the checks of a category look at the first of them, java's at the first of a hash map
		val atomObjs = LinkedHashMap<Long, NameIndexAtom>(4)
		// we check that poi category is complete in the end
		var poiCategoryOnMatchingWord = false
		var poiCategoryOnNumber = false
		if (a.atomicObject()) {
			atomObjs[a.id] = a
		}
		if (!a.isPoiCategory()) {
			poiCategoryOnMatchingWord = poiCategoryOnMatchingWord or token.hasPoiCategoryKeys()
		} else {
			poiCategoryOnNumber = token.likelyPartOfBuilding() || token.getMainNumber() > 0
		}

		var duplicateWord = false
		for (i in 0 until pCount) {
			val pa = parent!!.linearResults[pindx * parent.tCount + i]
			if (pa.id == a.id) {
				continue
			}
			// check that token is reused in parent
			// ignore every object that has this name already (except duplicate words && numbers assigned to building)
			if (tokens[0].word != parent.tokens[i].word && !duplicateWord) {
				var existing = parent.tokens[i].index[a.id]
				// it might be duplicate word which is not explored yet see test 'SW Summit Valley Ln, Lee's Summit'
				// 'valley' x 'ln' x 'lee' (already has combination of 2) x 'summit' (breaks here) 'summit'
				if (existing != null && !existing.isBuilding() && !existing.isPOIRef()) {
					return false
				}
				existing = tokens[0].index[pa.id]
				if (existing != null && !existing.isBuilding() && !existing.isPOIRef()) {
					return false
				}
			} else {
				duplicateWord = true
			}
			val parentToken = parent.tokens[i]
			if (!pa.isPoiCategory()) {
				poiCategoryOnMatchingWord = poiCategoryOnMatchingWord or parentToken.hasPoiCategoryKeys()
			} else {
				poiCategoryOnNumber = poiCategoryOnNumber or (parentToken.likelyPartOfBuilding() || parentToken.getMainNumber() > 0)
			}
			if (pa.atomicObject()) {
				atomObjs[pa.id] = pa
			}
			if (atomObjs.size > settings.LIMIT_ATOMIC_OBJECTS) {
				return false
			}
			// allow intersection for Buildings associated with place
			// Don't intersect <City Street> ('<Salt Lake City>') with Street ('Pennsylvania street')
		}

		if (poiType != null && poiCategoryOnMatchingWord) {
			// don't add intersection poi type on common word (not search apple city)
			return false
		}
		// investigate to find types
		if (poiType != null && atomObjs.size > 0) {
			val p = atomObjs.values.iterator().next()
			if (poiTypeToken.incomplete) {
				return false
			}
			if (p.isPOI()) {
				val poiTypes = p.poiTypes ?: return false
				var match = false
				for (k in 0 until poiTypes.size()) {
					val pType = poiTypes[k]
					if (ctx.poiSearch.getById(pType)!!.isPlace()) {
						match = true
						break
					}
				}
				if (!match) {
					return false
				}
			} else if (poiCategoryOnNumber) {
				return false
			}
		}
		if (atomObjs.size > 1) {
			val it = atomObjs.values.iterator()
			val a1 = it.next()
			val a2 = it.next()
			// if one object is same as city name don't intersect with street poi...
			if (a1.sameNameAreaObj != null || a2.sameNameAreaObj != null) {
				return false
			}
			if ((a1.isStreetBuilding() != a2.isStreetBuilding()) && !it.hasNext()) {
				typeIntersection[0] = 1
			} else {
				typeIntersection[0] = 2
				val twoStreets = a1.isStreetBuilding() && a2.isStreetBuilding()
				val twoPOIs = !a1.isStreetBuilding() && !a2.isStreetBuilding()
				if (!settings.SEARCH_STREET_INTERSECTIONS && twoStreets) {
					return false
				} else if (!settings.SEARCH_POI_INTERSECTIONS && twoPOIs) {
					return false
				}
			}
		}
		return true
	}

	override fun compareTo(other: SpatialSearchResultsList): Int {
		var s1 = tCount
		var s2 = other.tCount
		if (s1 != s2) {
			return -s1.compareTo(s2)
		}
		s1 = sumTokenAtomSize()
		s2 = other.sumTokenAtomSize()
		return s1.compareTo(s2)
	}

	fun toString(extended: Boolean): String {
		val words = wordTokens()
		val fr = finalResult
		return "Results " + tCount + " tokens " + JavaFormat.grouped(getCombinations().toLong()) +
				(if (fr == null) "" else " (" + JavaFormat.grouped(fr.size.toLong()) + " unique)") + " - " + words + " " +
				(if (!extended) "" else (" results: $linearResults"))
	}

	fun wordTokens(): List<String> {
		val words = ArrayList<String>()
		for (t in tokens) {
			words.add(t.originalWord)
		}
		return words
	}

	override fun toString(): String = toString(false)

	companion object {
		// how far apart two rows of the same name may be and still be one place, all three measured
		// on judged cases - see preferences.md, "What deduplication is allowed to merge"
		private const val SAME_PLACE_M = 30.0 // pref-0071, pref-0092
		private const val SAME_FACILITY_M = 400.0 // pref-0007, pref-0082
		private const val SAME_STREET_M = 2000.0 // pref-0111
		private const val MAX_SAME_NAME = 10 // how far back the chain is walked

		fun clipBbox(srcAndTarget: IntArray, bbox31: IntArray) {
			srcAndTarget[0] = max(srcAndTarget[0], bbox31[0]) // xleft
			srcAndTarget[1] = max(srcAndTarget[1], bbox31[1]) // ytop
			srcAndTarget[2] = min(srcAndTarget[2], bbox31[2]) // xright
			srcAndTarget[3] = min(srcAndTarget[3], bbox31[3]) // ybottom
		}
	}
}
