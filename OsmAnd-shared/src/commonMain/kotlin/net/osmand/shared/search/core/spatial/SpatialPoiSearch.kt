package net.osmand.shared.search.core.spatial

import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import co.touchlab.stately.concurrency.AtomicInt
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.PoiRegion
import net.osmand.shared.binary.PoiSubType
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.binary.SearchPoiAdditionalFilter
import net.osmand.shared.binary.SearchPoiTypeFilter
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.map.WorldRegion
import net.osmand.shared.osm.AbstractPoiType
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiAdditionalLabels
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.osm.PoiFilter
import net.osmand.shared.osm.PoiType
import net.osmand.shared.search.core.TopIndexFilter
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtom
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialSearchFileCache
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialSearchGlobalCache
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KLock
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.collections.KTLongHashSet
import net.osmand.shared.util.synchronized

/**
 * The poi categories of the spatial search: their names in a prefix tree to match the words of a
 * query, and the amenities of a category read by the poi index of the maps.
 *
 * A copy of `SpatialPoiSearch` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. The prefix tree of the names is guarded by one lock where java takes a read-write lock:
 * the readers of a search wait for each other here, a word at a time.
 */
class SpatialPoiSearch(types: MapPoiTypes) {

	internal val poiTypes: MapPoiTypes = types
	internal val poiTypesIndex = StringPrefixTree<SpatialPoiType>()
	internal val poiTypesIndexLock = KLock()
	internal val ids = AtomicInt(0)
	internal val byKey: MutableMap<String, SpatialPoiType> = ConcurrentMutableMap()
	internal val byWikidataKey: MutableMap<String, MutableList<SpatialPoiType>> = ConcurrentMutableMap()
	internal val byId: MutableMap<Int, SpatialPoiType> = ConcurrentMutableMap()
	internal val languages: MutableSet<String> = ConcurrentMutableSet()

	class SpatialPoiType private constructor(
		val singleType: AbstractPoiType?,
		val poiAdditional: String?,
		val poiSubType: PoiSubType?,
		internal val key: String,
		internal val id: Int
	) {
		internal val names = ArrayList<String>()
		internal var tokensInName = 0
		internal var place = false
		internal var wikidataId: String? = null
		internal var parentTypes: MutableList<AbstractPoiType>? = null

		constructor(pt: AbstractPoiType, id: Int) : this(pt, null, null, pt.getKeyName(), id)

		constructor(additional: String, key: String, id: Int) : this(null, additional, key, id)

		constructor(poiSubType: PoiSubType?, additional: String, key: String, id: Int) :
				this(null, additional, poiSubType, key, id)

		fun isPlace(): Boolean = place

		fun getWikidataId(): String? = wikidataId

		fun getKey(): String = key

		fun getParentTypes(): List<AbstractPoiType>? = parentTypes

		fun accept(a: Amenity): Boolean {
			if (key == a.getType()!!.getKeyName()) {
				return true
			}
			// contains for ';'
			val subType = a.getSubType()!!
			val multi = subType.indexOf(';') != -1
			if (key == subType
				|| (multi && key.startsWith("$subType;"))
				|| (multi && key.endsWith(";$subType"))
				|| (multi && key.contains(";$subType;"))
			) {
				return true
			}
			return false
		}

		fun addName(name: String): Boolean {
			val nms = KSearchAlgorithms.split(name)
			if (tokensInName == 0 || nms.size < tokensInName) {
				tokensInName = nms.size
			}
			names.add(name)
			return true
		}

		fun getOriginalName(alignedName: String): String {
			if (singleType != null) {
				for (name in getOriginalNames(singleType.getTranslation(), singleType.getSynonyms())) {
					if (KSearchAlgorithms.alignChars(name) == alignedName) {
						return name
					}
				}
			}
			return alignedName
		}

		/**
		 * Fewest words of a name of the type that has a word starting with one of the query words. A one-word synonym
		 * ("cistern", "temple") must not make a partial match of another name ("water" of "Water tank", "place" of
		 * "Place of worship") look complete.
		 */
		fun tokensInMatchedName(queryWords: Collection<String>): Int {
			var min = 0
			for (name in names) {
				val words = KSearchAlgorithms.split(name)
				var matched = false
				for (w in words) {
					val aligned = KSearchAlgorithms.alignChars(w.lowercase())
					for (q in queryWords) {
						matched = matched or aligned.startsWith(q)
					}
				}
				if (matched && (min == 0 || words.size < min)) {
					min = words.size
				}
			}
			return if (min == 0) tokensInName else min
		}

		override fun toString(): String = key
	}

	init {
		for (pc in poiTypes.getCategories()) {
			if (pc == poiTypes.getOtherMapCategory()) {
				continue
			}
			addToIndex(pc, null)
			for (pt in pc.getPoiFilters()) {
				if (pt.isTopVisible()) {
					addToIndex(pt, null)
				}
			}
			for (pt in pc.getPoiTypes()) {
				if (pt.isReference()) {
					continue
				}
				addToIndex(pt, null)
				for (add in pt.getPoiAdditionals()) {
					if (add.isTopVisible() && PoiAdditionalLabels.isAcceptedValue(add)) {
						addToIndex(add, pt)
					}
				}
			}
		}
	}

	private fun addToIndex(pt: AbstractPoiType, parent: PoiType?) {
		val existing = byKey[pt.getKeyName()]
		if (existing != null) {
			if (pt.isAdditional()) {
				existing.parentTypes!!.add(parent!!)
				return
			} else {
				throw IllegalStateException()
			}
		}
		val basePoiName = poiTypes.getBasePoiName(pt)
		val poiType = SpatialPoiType(pt, ids.incrementAndGet() - 1)
		if (pt is PoiType) {
			poiType.place = "place" == pt.getOsmTag()
		}
		if (parent != null) {
			poiType.parentTypes = ArrayList<AbstractPoiType>().also { it.add(parent) }
		}
		val translation = if (basePoiName == pt.getTranslation()) null else pt.getTranslation()
		for (name in getNames(translation, pt.getSynonyms())) {
			poiType.addName(name)
		}
		addToIndex(basePoiName, poiType)
	}

	// names of one more language, once: "Schule" in Leipzig for an app in Ukrainian; by the types in the order they
	// were made, where java walks them in the order of its hash map
	fun addLanguage(lang: String) {
		val translator = poiTypes.getPoiTranslator()
		val langTranslator = if (translator == null || !languages.add(lang)) null else translator.getTranslator(lang)
		if (langTranslator == null) {
			return
		}
		synchronized(poiTypesIndexLock) {
			for (poiType in byKey.values) {
				val pt = poiType.singleType ?: continue
				for (name in getNames(langTranslator.getTranslation(pt), langTranslator.getSynonyms(pt))) {
					if (!poiType.names.contains(name)) {
						poiType.addName(name)
						poiTypesIndex.put(name, poiType)
					}
				}
			}
		}
	}

	private fun updateNamesWikidataId(poiType: SpatialPoiType, names: String) {
		synchronized(poiTypesIndexLock) {
			val snames = WorldRegion.splitLikeJava(names, ";")
			if (poiType.wikidataId == null && snames[0].isNotEmpty()) {
				val wikidataId = snames[0]
				poiType.wikidataId = wikidataId
				byWikidataKey.getOrPut(wikidataId) { ArrayList() }.add(poiType)
			}
			for (i in 1 until snames.size) {
				val nameToAdd = snames[i]
				if (!poiType.names.contains(nameToAdd)) {
					if (poiType.addName(nameToAdd)) {
						poiTypesIndex.put(snames[i], poiType)
					}
				}
			}
		}
	}

	private fun addToIndex(basePoiName: String, poiType: SpatialPoiType) {
		poiType.addName(basePoiName)
		synchronized(poiTypesIndexLock) {
			SpatialSearchContext.checkPoiTypeId(poiType.id)
			byId[poiType.id] = poiType
			byKey[poiType.key] = poiType
			val wikidataId = poiType.wikidataId
			if (wikidataId != null && wikidataId.isNotEmpty()) {
				byWikidataKey.getOrPut(wikidataId) { ArrayList() }.add(poiType)
			}
			for (name in poiType.names) {
				poiTypesIndex.put(name, poiType)
			}
		}
	}

	internal fun init(cache: SpatialSearchGlobalCache, fc: SpatialSearchFileCache, bir: BinaryMapIndexReader, poiRegion: PoiRegion) {
		val poiFrequencies = fc.poiFrequencies!!
		val cats = poiRegion.getCategories()
		val subcategories = poiRegion.getSubcategories()
		val categoryFreqs = poiRegion.getCategoryFreqs()
		val subcatFreqs = poiRegion.getSubcategoryFreqs()
		for (i in cats.indices) {
			val lst = subcategories[i]
			val f = if (i < categoryFreqs.size()) categoryFreqs[i] else 0
			poiFrequencies[cats[i]] = f
			for (j in lst.indices) {
				val ft = if (i < subcatFreqs.size && j < subcatFreqs[i].size()) subcatFreqs[i][j] else 0
				val vkey = lst[j]
				poiFrequencies[vkey] = ft
			}
		}

		for (subType in poiRegion.getSubTypes()) {
			if (subType.text) {
				continue
			}
			if (subType.isTopIndex()) {
				val possibleValues = subType.possibleValues!!
				val wikidataIds = subType.wikidataIds
				val possibleValuesFreqs = subType.possibleValuesFreqs
				for (k in possibleValues.indices) {
					val topValueName = possibleValues[k]
					val valueKey = TopIndexFilter.getValueKey(topValueName)
					val fullKey = subType.name + "_" + valueKey
					val wikidataId = if (wikidataIds != null && k < wikidataIds.size) wikidataIds[k] else ""
					var freq = if (possibleValuesFreqs != null && k < possibleValuesFreqs.size()) possibleValuesFreqs[k] else 0
					if (possibleValuesFreqs == null && possibleValues.size == 1) {
						// the writer stores per-value frequencies only for 2+ values; one value has the subtype total
						freq = subType.frequency
					}
					var topValue = byKey[fullKey]
					if (topValue == null) {
						topValue = SpatialPoiType(subType, topValueName, fullKey, ids.incrementAndGet() - 1)
						if (wikidataId.isNotEmpty()) {
							val otherNames = WorldRegion.splitLikeJava(wikidataId, ";")
							topValue.wikidataId = otherNames[0]
							for (ts in 1 until otherNames.size) {
								topValue.addName(otherNames[ts])
							}
						}
						addToIndex(topValueName, topValue)
					} else if (wikidataId.isNotEmpty()) {
						updateNamesWikidataId(topValue, wikidataId)
					}
					val fit = poiFrequencies[topValue.key]
					if (fit != null) {
						freq += fit
					}
					poiFrequencies[topValue.key] = freq
				}
			}
			val indSubType = byKey[subType.name!!] ?: continue // skip top level additional
			poiFrequencies[indSubType.key] = subType.frequency
		}
	}

	private class PoiCatSearch(
		val pt: SpatialPoiType,
		val tokens: MutableList<SpatialSearchToken>,
		val atoms: MutableList<NameIndexAtom>,
		val freq: Int
	) : Comparable<PoiCatSearch> {

		override fun compareTo(other: PoiCatSearch): Int {
			val i1 = -tokens.size.compareTo(other.tokens.size)
			if (i1 != 0) {
				return i1
			}
			return -freq.compareTo(other.freq)
		}
	}

	private fun addPoiCategoryMatch(
		ctx: SpatialSearchContext, a: SpatialPoiType, t: SpatialSearchToken,
		res: MutableMap<SpatialPoiType, PoiCatSearch>
	) {
		val total = getCategoryFrequency(ctx, a)
		var cs = res[a]
		if (cs == null) {
			cs = PoiCatSearch(a, ArrayList(), ArrayList(), total)
			res[a] = cs
		}
		if (cs.tokens.contains(t)) {
			return
		}

		val atom = NameIndexAtom(a.key, a.id.toLong(), total)
		cs.atoms.add(atom)
		cs.tokens.add(t)
		if (typedInFull(t, a)) {
			t.addPoiCategoryMatch(a.id)
		}
	}

	fun getCategoryFrequency(ctx: SpatialSearchContext, categoryKey: String): Int {
		val a = getByKey(categoryKey) ?: return 0
		return getCategoryFrequency(ctx, a)
	}

	private fun getCategoryFrequency(ctx: SpatialSearchContext, a: SpatialPoiType): Int {
		var total = 0
		for (l in ctx.internalFile) {
			val poiFrequencies = l.poiFrequencies
			if (poiFrequencies != null) {
				var freq = poiFrequencies[a.key]
				if (freq != null) {
					total += freq
				}
				val singleType = a.singleType
				if (singleType is PoiFilter) {
					for (p in singleType.getPoiTypes()) {
						freq = poiFrequencies[p.getKeyName()]
						if (freq != null) {
							total += freq
						}
					}
				}
			}
		}
		return total
	}

	fun processPoiCategories(ctx: SpatialSearchContext, tokens: List<SpatialSearchToken>) {
		val res = LinkedHashMap<SpatialPoiType, PoiCatSearch>()
		for (t in tokens) {
			val poiTypes = synchronized(poiTypesIndexLock) {
				poiTypesIndex.match(t.getPrefixMatcher(ctx.stats))
			}
			for (a in poiTypes) {
				var match = false
				for (n in a.names) {
					if (t.getMainCollator().matches(n)) {
						match = true
						break
					}
				}
				if (match) {
					addPoiCategoryMatch(ctx, a, t, res)
					val wikidataId = a.wikidataId
					if (wikidataId != null) {
						val otherTypes = byWikidataKey[wikidataId]!!
						for (otherA in otherTypes) {
							if (otherA.id != a.id) {
								addPoiCategoryMatch(ctx, otherA, t, res)
							}
						}
					}
				}
			}
		}

		val finalRes = ArrayList(res.values)
		finalRes.sort()
		val uncoveredTokens = HashSet<SpatialSearchToken>()
		for (pc in finalRes) {
			uncoveredTokens.addAll(pc.tokens)
		}
		var free = ctx.settings.LIMIT_POI_CATEGORY_BY_FREQ
		for (pc in finalRes) {
			val coversNewToken = uncoveredTokens.removeAll(pc.tokens)
			if (free == 0 || (!coversNewToken && free <= uncoveredTokens.size)) {
				continue
			}
			free--
			for (i in pc.tokens.indices) {
				val token = pc.tokens[i]
				val atom = pc.atoms[i]
				token.addAtom(atom)
				if (!typedInFull(token, pc.pt)) {
					token.poiCategoryKeysToAutocomplete.remove(atom.name)
					token.poiCategoryIds.remove(atom.id.toInt())
				}
			}
			// Problem "Helipad 32" (doesn't list object because no 32 ref is found"
			// Categories are not needed if exact result is found (there is always option to go in category and filter later)
		}
	}

	fun getById(id: Int): SpatialPoiType? = byId[id]

	fun getByKey(key: String): SpatialPoiType? = byKey[key]

	private fun filterByRadius(
		l: KLatLon, rad: Int, oFiles: MutableList<BinaryMapIndexReader>,
		res: MutableList<BinaryMapIndexReader>
	): MutableList<BinaryMapIndexReader> {
		val rect = KMapUtils.calculate31BboxUsingRhumb(rad, l)
		val it = oFiles.iterator()
		while (it.hasNext()) {
			val next = it.next()
			if (next.containsPoiData(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())) {
				res.add(next)
				it.remove()
			}
		}
		return res
	}

	fun loadPOIObjects(ctx: SpatialSearchContext, spt: SpatialPoiType?, r: KQuadRect, zoom: Int, limit: Int): List<Amenity> {
		val results = ArrayList<Amenity>()
		if (spt != null) {
			val alimit = intArrayOf(limit)
			val req = prepareRequest(
				spt, results, KMapUtils.get31TileNumberX(r.left),
				KMapUtils.get31TileNumberY(r.top), KMapUtils.get31TileNumberX(r.right),
				KMapUtils.get31TileNumberY(r.bottom), zoom, alimit
			)
			iterateSearch(ctx, req, ctx.files, true)
		}
		return filterByZoomTile(ctx, results)
	}

	private fun filterByZoomTile(ctx: SpatialSearchContext, amenities: List<Amenity>): List<Amenity> {
		val z = 16 - ctx.settings.SEARCH_POI_BY_CATEGORY_ZOOM
		if (z < 0) {
			return amenities // dedup disabled at this zoom, keep everything
		}
		val tiles = KTLongHashSet()
		val res = ArrayList<Amenity>(amenities.size)
		for (a in amenities) {
			val location = a.getLocation()!!
			val x16 = KMapUtils.get31TileNumberX(location.longitude) shr 15
			val y16 = KMapUtils.get31TileNumberY(location.latitude) shr 15
			val tileId = KMapUtils.interleaveBits((x16 shr z).toLong(), (y16 shr z).toLong())
			val elo = a.getTravelEloNumber() > Amenity.DEFAULT_ELO
			if (tiles.contains(tileId) && !elo) {
				continue
			}
			tiles.add(tileId)
			res.add(a)
		}
		return res
	}

	fun loadPOIObjects(ctx: SpatialSearchContext, spt: SpatialPoiType?, latLon: KLatLon, radMeters: Int, limit: Int): List<Amenity> {
		val results = ArrayList<Amenity>()
		if (spt != null) {
			val alimit = intArrayOf(limit)
			val rect = KMapUtils.calculate31BboxUsingRhumb(radMeters, latLon)
			val req = prepareRequest(
				spt, results, rect.left.toInt(), rect.top.toInt(), rect.right.toInt(),
				rect.bottom.toInt(), -1, alimit
			)
			val oFiles = ArrayList(ctx.files)
			iterateSearch(ctx, req, filterByRadius(latLon, 5_000, oFiles, ArrayList()), false)
			if (alimit[0] != 0 && radMeters > 5_000) {
				iterateSearch(ctx, req, filterByRadius(latLon, 50_000, oFiles, ArrayList()), false)
			}
			if (alimit[0] != 0 && radMeters > 50_000) {
				iterateSearch(ctx, req, oFiles, false)
			}
		}
		return results
	}

	private fun prepareRequest(
		spt: SpatialPoiType, results: MutableList<Amenity>, sleft: Int, stop: Int, sright: Int,
		sbottom: Int, zoom: Int, alimit: IntArray
	): SearchRequest<Amenity> {
		val groupTypes = groupChildTypes(spt)
		val typeFilter = if (spt.poiAdditional != null) null else object : SearchPoiTypeFilter {

			override fun accept(type: PoiCategory?, subcategory: String): Boolean {
				if (spt.key == type!!.getKeyName() || spt.key == subcategory) {
					return true
				}
				if (groupTypes.contains(subcategory)) {
					return true
				}
				val parentTypes = spt.parentTypes
				if (parentTypes != null) {
					for (a in parentTypes) {
						if (a.getKeyName() == type.getKeyName() || a.getKeyName() == subcategory) {
							return true
						}
					}
				}
				return false
			}

			override fun isEmpty(): Boolean = false
		}
		val addFilter = if (spt.poiAdditional == null) null else object : SearchPoiAdditionalFilter {

			override fun getName(): String = spt.names[0]

			override fun getIconResource(): String? = null

			override fun accept(poiSubType: PoiSubType, value: String): Boolean {
				if (spt.poiAdditional == value) {
					return true
				}
				return false
			}
		}
		val matcher = object : ResultMatcher<Amenity> {

			override fun publish(obj: Amenity): Boolean {
				if (spt.parentTypes != null) {
					val match = obj.getAdditionalInfo(spt.key) != null
					if (!match) {
						return false
					}
				}
				if (alimit[0] > 0) {
					alimit[0]--
				}
				results.add(obj)
				return false
			}

			override fun isCancelled(): Boolean {
				return false // allow to read all for proper sorting
			}
		}
		return SearchRequest.buildSearchPoiRequest(sleft, sright, stop, sbottom, zoom, typeFilter, addFilter, matcher)
	}

	private fun iterateSearch(
		ctx: SpatialSearchContext, req: SearchRequest<Amenity>, res: List<BinaryMapIndexReader>,
		check: Boolean
	) {
		for (bir in res) {
			if (check && !bir.containsPoiData(req.getLeft(), req.getTop(), req.getRight(), req.getBottom())) {
				continue
			}
			ctx.stats.poiByTypeTime.start()
			val br = bir.getBytesRead()
			bir.searchPoi(req)
			ctx.stats.poiByTypeBytes += (bir.getBytesRead() - br)
			ctx.stats.poiByTypeTime.finish()
			ctx.stats.poiByTypeBboxes += req.numberOfReadSubtrees.toLong()
		}
	}

	companion object {

		private fun getNames(translation: String?, synonyms: String?): List<String> {
			val names = ArrayList<String>()
			for (name in getOriginalNames(translation, synonyms)) {
				names.add(KSearchAlgorithms.alignChars(name))
			}
			return names
		}

		// translation (only the first name) and synonyms
		private fun getOriginalNames(translation: String?, synonyms: String?): List<String> {
			val all = (if (KAlgorithms.isEmpty(translation)) "" else WorldRegion.splitLikeJava(translation!!, ";")[0]) + ";" +
					(synonyms ?: "")
			val names = ArrayList<String>()
			for (name in WorldRegion.splitLikeJava(all, ";")) {
				if (name.trim().isNotEmpty()) {
					names.add(name.trim())
				}
			}
			return names
		}

		// a category matched by a word still being typed is only a row: "ca" does not list every catholic church
		private fun typedInFull(t: SpatialSearchToken, a: SpatialPoiType): Boolean {
			if (!t.incomplete) {
				return true
			}
			for (n in a.names) {
				for (w in KSearchAlgorithms.splitAndNormalize(n, false)) {
					if (KSearchAlgorithms.alignChars(w) == t.wordNoDot) {
						return true
					}
				}
			}
			return false
		}

		private fun groupChildTypes(spt: SpatialPoiType): Set<String> {
			val singleType = spt.singleType
			if (singleType is PoiFilter && singleType !is PoiCategory) {
				val types = HashSet<String>()
				for (p in singleType.getPoiTypes()) {
					types.add(p.getKeyName())
				}
				return types
			}
			return emptySet()
		}
	}
}
