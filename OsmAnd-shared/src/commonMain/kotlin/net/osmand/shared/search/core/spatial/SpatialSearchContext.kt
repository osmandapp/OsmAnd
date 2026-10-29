package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.AddressNameIndexDataAtom
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.CityBlocks
import net.osmand.shared.binary.NameIndexReader
import net.osmand.shared.binary.NameIndexReader.PrefixNameValue
import net.osmand.shared.binary.OsmAndPoiNameIndexDataAtom
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.data.City
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.MapObject
import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.map.WorldRegion
import net.osmand.shared.search.core.HashSkipTileQuadTree
import net.osmand.shared.search.core.HashSkipTileQuadTreeJoiner
import net.osmand.shared.search.core.TopIndexFilter
import net.osmand.shared.search.core.spatial.SpatialPoiSearch.SpatialPoiType
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtom
import net.osmand.shared.search.core.spatial.SpatialSearchToken.NameIndexAtomXY
import net.osmand.shared.search.core.spatial.SpatialSearchToken.PartialMatch
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialSearchFileCache
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialSearchGlobalCache
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import net.osmand.shared.util.JavaFormat
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTLongHashSet
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.math.abs
import kotlin.math.max

/**
 * The state of one spatial text search: the maps, the words of the query and their atoms.
 *
 * A copy of `SpatialSearchContext` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS.
 */
class SpatialSearchContext(
	settings: SpatialTextSearchSettings, files: List<BinaryMapIndexReader>,
	poiSearch: SpatialPoiSearch, location: KLatLon?
) {

	internal val files: List<BinaryMapIndexReader> = files
	internal val internalFile = ArrayList<SpatialSearchFileCache>()
	internal val location: KLatLon? = location // could be null
	internal val limitLocationBboxes: Array<IntArray>

	internal val poiSearch: SpatialPoiSearch = poiSearch
	internal val settings: SpatialTextSearchSettings = settings
	internal val stats = SpatialSearchStats()
	internal val typos = SpatialTypoSuggestions(this)

	// flags of the words met in the map being read, files are read one after another
	private var wordFlagsReader: NameIndexReader? = null
	private val wordFlags = HashMap<String, Int>()

	var resultMatcher: ResultMatcher<SpatialSearchResult>? = null

	val ranking: SpatialSearchRanking? = if (settings.SCORE_RANKING) SpatialSearchRanking() else null

	fun isCancelled(): Boolean = resultMatcher?.isCancelled() == true

	internal var tokens: MutableList<SpatialSearchToken>? = null // non initiatilized
	internal var commonlyUsedWords: MutableSet<String> = HashSet()

	class SpatialSearchStats {
		var requestTime = Timer()
		var step1Atoms = Timer()
		var sub1FileAtomsTime = Timer()
		var sub1MatchTime = Timer()
		var sub1PartMatchTime = sub1MatchTime // new Timer();
		var sub1PoiNameBoundaryTime = Timer()
		var tokenObjs = 0

		var step2Compute = Timer()
		var sub2LoadObjectsBldTime = Timer()
		var sub2ReadObjTime = Timer()
		var maxCombinations = 0

		var step3Sort = Timer()

		var readTableBytes = 0L
		var readAtomsBytes = 0L
		var readObjsBytes = 0L
		var skipTableBytes = 0L
		var skipAtomsBytes = 0L

		// separate api
		var poiByTypeTime = Timer()
		var poiByTypeBytes = 0L
		var poiByTypeBboxes = 0L

		var doTiming = true
		var printLogs = true

		inner class Timer {
			var time = 0L
			var intervals = 0
			var endTime = nanoTime()

			fun start() {
				intervals++
				if (doTiming) {
					time -= nanoTime()
				}
			}

			fun finish() {
				if (doTiming) {
					time += nanoTime()
				}
			}

			fun ms(): Double = time / 1e6
		}

		override fun toString(): String =
			"Search Stats ${f(requestTime.ms())} ms (read ${g((readTableBytes + readAtomsBytes + readObjsBytes) / 1024)} KB): " +
					"1. Atoms ${f(step1Atoms.ms())} ms ${g(tokenObjs.toLong())} (read ${f(sub1FileAtomsTime.ms())} " +
					"${sub1FileAtomsTime.intervals}, match ${f(sub1MatchTime.ms())} ${g(sub1MatchTime.intervals.toLong())}, " +
					"poi ${f(sub1PoiNameBoundaryTime.ms())}), " +
					"2. Compute ${f(step2Compute.ms())} ms ${g(maxCombinations.toLong())} " +
					"(validate ${f(sub2LoadObjectsBldTime.ms())} > read ${f(sub2ReadObjTime.ms())})"

		private fun f(ms: Double): String = JavaFormat.fixed(ms, 1)

		private fun g(value: Long): String = JavaFormat.grouped(value)
	}

	init {
		// SpatialPoiSearch will be passed as parameter
		val loc = getLimitLocationFromFiles(files, location)
		limitLocationBboxes = Array(settings.OPTIM_LIMIT_RADIUS.size) { k -> calculateBbox(settings.OPTIM_LIMIT_RADIUS[k], loc) }
	}

	private fun getLimitLocationFromFiles(files: List<BinaryMapIndexReader>, location: KLatLon?): KLatLon {
		var limitLocation = location
		if (limitLocation == null) {
			for (f in files) {
				limitLocation = f.getRegionCenter()
				if (limitLocation != null) {
					break
				}
			}
			if (limitLocation == null) {
				limitLocation = KLatLon(0.0, 0.0)
			}
		}
		return limitLocation
	}

	fun getStats(): SpatialSearchStats = stats

	fun initFiles(cache: SpatialSearchGlobalCache) {
		var indexInd = 0
		var fileInd = 0
		for (bir in files) {
			var fc = cache.filesCache[bir.getFile().name()]
			if (fc == null || !fc.test(bir)) {
				fc = SpatialSearchFileCache(bir, cache.searchRules)
			}
			cache.filesCache[fc.file] = fc
			fc.indexInd = indexInd
			fc.fileInd = fileInd
			this.internalFile.add(fc)
			indexInd += fc.indexReaders.size
			fileInd++
			var initPoi = false
			if (fc.poiFrequencies != null || fc.poiSearch !== poiSearch) {
				fc.poiFrequencies = HashMap()
				fc.poiSearch = poiSearch
				initPoi = true
			}
			for (r in fc.indexReaders) {
				val poiRegion = r.poiRegion
				if (poiRegion != null) {
					val bRead = bir.getBytesRead()
					bir.initCategories(poiRegion)
					if (initPoi) {
						poiSearch.init(cache, fc, bir, poiRegion)
					}
					stats.readAtomsBytes += (bir.getBytesRead() - bRead)
				}
				r.gcPrefixes(settings.AUTO_CLEAR_PREFIX_CACHE_LIMIT)
			}
		}
	}

	fun setTokens(tokens: MutableList<SpatialSearchToken>) {
		this.tokens = tokens
		typos.clear()
	}

	internal fun processPoiCategories() {
		if (settings.SEARCH_POI_CATEGORIES) {
			poiSearch.processPoiCategories(this, tokens!!)
		}
	}

	/** a word still being typed is matched whole when its index blocks are larger than the limit */
	private fun readAndCheckBroadIncompleteWords() {
		for (t in tokens!!) {
			if (!t.incomplete || t.isOnlyFullMatch() || settings.LIMIT_INCOMPLETE_BYTES <= 0) {
				continue
			}
			var bytes = 0L
			for (fileInd in files.indices) {
				for (indx in internalFile[fileInd].indexReaders) {
					var prefixes = indx.getMatchedPrefixes(t.word)
					if (prefixes == null) {
						stats.sub1FileAtomsTime.start()
						prefixes = files[fileInd].readFullNameIndex(
							indx.setQuery(t.word, t.getPrefixMatcher(stats, internalFile[fileInd].locale))
						)
						stats.sub1FileAtomsTime.finish()
					}
					for (p in prefixes ?: emptyList()) {
						bytes += p.data?.size ?: 0
					}
				}
			}
			t.broad = bytes > settings.LIMIT_INCOMPLETE_BYTES
			t.localeRules.clear()
			t.noRules.fastPrefMatchCheck.clear()
		}
	}

	internal fun readAtoms() {
		for (c in internalFile) {
			for (indx in c.indexReaders) {
				indx.resetBytesStat() // before readAndCheckBroadIncompleteWords, it reads too
			}
		}
		readAndCheckBroadIncompleteWords()
		val tokens = tokens!!
		var indxInd = 0
		var cachedBytes = 0L
		for (fileInd in files.indices) {
			val iCache = internalFile[fileInd]
			val b = files[fileInd]
			for (indx in iCache.indexReaders) {
				readAtoms(tokens, b, indx, indxInd, iCache.locale)
				indxInd++
				// the matched atoms are in the tokens now, the parsed blocks are only a cache for the next search
				cachedBytes += indx.getCachedBytes()
				if (cachedBytes > settings.AUTO_CLEAR_PREFIX_CACHE_BYTES) {
					cachedBytes -= indx.getCachedBytes()
					indx.clearPrefixes()
				}
				val bytesStat = indx.getBytesStat()
				stats.readAtomsBytes += bytesStat.readAtomBytes
				stats.skipAtomsBytes += bytesStat.skipAtomBytes
				stats.readTableBytes += (bytesStat.readTableBytes - bytesStat.skipTableBytes)
				stats.skipTableBytes += bytesStat.skipTableBytes
			}
		}
		// add partial once we read all files
		for (t in tokens) {
			if (settings.OPTIM_READ_COMMON_WITH_OTH_NON_FOUND_ATOMS || settings.OPTIM_READ_POI_CATEGORY_WORD_ATOMS) {
				addPartialMatch(t, t.getPartialExactMatch())
				addPartialMatch(t, t.getPartialMatch())
				t.clearPartialAtoms()
			}
		}
		if (settings.OPTIM_DELETE_EMBEDDED_BOUNDARIES) {
			stats.sub1PoiNameBoundaryTime.start()
			val boundaries = filterEmbeddedBoundaries(tokens)
			if (settings.OPTIM_FLAG_POI_SAME_AS_CITY_STREET) {
				assignPoiFlagGeo(boundaries, tokens)
			}
			stats.sub1PoiNameBoundaryTime.finish()
		}

		// after the partial matches: only a word that still names no object is looked up with a typo in its key
		typos.readKeyNeighbours()
	}

	private fun addPartialMatch(t: SpatialSearchToken, partialAtoms: List<PartialMatch>) {
		// 'haupstrasse' vs 'haupstrasse <specifier>'
		val partialAreSameFreq = partialAtoms.size < t.atoms.size / 2
		var nearbyLimit = Int.MAX_VALUE
		if (!partialAreSameFreq) {
			val cnts = IntArray(settings.OPTIM_LIMIT_RADIUS.size + 1)
			val set = KTLongHashSet()
			for (a in partialAtoms) {
				if (set.add(a.atom.id)) {
					cnts[a.atom.nearbyRadius]++
				}
			}
			nearbyLimit = 0
			var cnt = t.atoms.size
			while (nearbyLimit < cnts.size &&
				(cnts[nearbyLimit] + cnt < settings.OPTIM_READ_COMMON_WORDS_LIMIT || settings.DEV_USE_PIPELINE)
			) {
				cnt += cnts[nearbyLimit]
				nearbyLimit++
			}
		}
		for (ind in partialAtoms.indices) {
			val pm = partialAtoms[ind]
			val atom = pm.atom
			if (atom.nearbyRadius >= nearbyLimit && atom.elo <= settings.MIN_ELO_RATING_TO_KEEP_IN_ATOM) {
				continue
			}
			val otherTokens = pm.other
			val added = t.addAtom(atom)
			if (added) {
				if (otherTokens != null) {
					for (otherToken in otherTokens) {
						otherToken.addAtom(NameIndexAtom(atom))
					}
				}
				addBuildingRefAtoms(t, tokens!!, otherTokens, pm.nonNumericMatch, atom)
			}
		}
	}

	private fun assignPoiFlagGeo(cities: Map<KTIntArrayList, List<AtomByTokens>>, tokens: List<SpatialSearchToken>) {
		val streets = groupAtomsByTokens(tokens) { it.isStreet() } // check performance
		val pois = groupAtomsByTokens(tokens) { it.isPOI() }
		if (settings.DEV_FLAG_POI_SAME_AS_CITY_TREE) {
			for ((keySet, value) in pois) {
				val areasTree = HashSkipTileQuadTree<NameIndexAtom>()
				buildTree(areasTree, cities[keySet])
				buildTree(areasTree, streets[keySet])
				if (!areasTree.isEmpty()) {
					areasTree.build()
					val poiTree = buildTree(HashSkipTileQuadTree(), value)
					poiTree.build()
					val joiner = HashSkipTileQuadTreeJoiner(areasTree, poiTree)
					val ids = KTLongHashSet()
					joiner.joinAllBuckets { area, poi ->
						if (area.obj.coords.contains(poi.obj.coords) && !ids.contains(poi.obj.id)) {
							ids.add(poi.obj.id)
							// delete completely not correct for new york the plaza
							// tokens.get(indx).removeAtom(poi.obj);
							// mark to not intersect
							val keyIt = keySet.iterator()
							while (keyIt.hasNext()) {
								val indx = keyIt.next()
								val atomSet = tokens[indx].getAtomToken(poi.obj)!!
								atomSet.sameNameAreaObj = area.obj
							}
						}
					}
				}
			}
		} else {
			for ((lst, value) in pois) {
				val cityNames = cities[lst]
				if (cityNames != null) {
					for (poi in value) {
						markPOIAsArea(poi, cityNames, lst, tokens)
					}
				}
				val streetNames = streets[lst]
				if (streetNames != null) {
					for (poi in value) {
						markPOIAsArea(poi, streetNames, lst, tokens)
					}
				}
			}
		}
	}

	private fun buildTree(objTree: HashSkipTileQuadTree<NameIndexAtom>, objs: List<AtomByTokens>?): HashSkipTileQuadTree<NameIndexAtom> {
		if (objs == null) {
			return objTree
		}
		for (largeArea in objs) {
			val bbox31 = largeArea.obj.coords.bbox31
			if (bbox31 != null) {
				objTree.addObject(largeArea.obj, bbox31)
			}
		}
		return objTree
	}

	private fun markPOIAsArea(poi: AtomByTokens, cityNames: List<AtomByTokens>, indxs: KTIntArrayList, tokens: List<SpatialSearchToken>) {
		for (largeArea in cityNames) {
			if (largeArea.obj.coords.contains(poi.obj.coords)) {
				val it = indxs.iterator()
				while (it.hasNext()) {
					val indx = it.next()
					// delete completely not correct for new york the plaza
					// tokens.get(indx).removeAtom(poi.obj);
					// mark to not intersect
					val atomSet = tokens[indx].getAtomToken(poi.obj)!!
					atomSet.sameNameAreaObj = largeArea.obj
				}
				return
			}
		}
	}

	internal class AtomByTokens(val obj: NameIndexAtom, val lstTokens: KTIntArrayList)

	private fun filterEmbeddedBoundaries(tokens: List<SpatialSearchToken>): Map<KTIntArrayList, List<AtomByTokens>> {
		val group = groupAtomsByTokens(tokens) { it.isCityVillage() || it.isBoundary() }
		// 3. find the largest boundary and delete embedded
		for ((lst, collection) in group) {
			if (lst.size() >= tokens.size - 1) {
				// do not delete full match tokens
				continue
			}
			var k = 0
			while (k < collection.size) {
				val aBoundary = collection[k]
				var toDelete: AtomByTokens? = null
				for (l in collection.indices) {
					if (k == l) {
						continue
					}
					val bBoundary = collection[l]
					if (bBoundary.obj.coords.contains(aBoundary.obj.coords) &&
						bBoundary.obj.otherWordsCnt <= aBoundary.obj.otherWordsCnt
					) {
						toDelete = aBoundary
						break
					}
				}
				if (toDelete != null) {
					collection.removeAt(k)
					for (token in lst.toArray()) {
						tokens[token].removeAtom(toDelete.obj)
					}
				} else {
					k++
				}
			}
		}
		return group
	}

	private fun groupAtomsByTokens(
		tokens: List<SpatialSearchToken>, filter: (NameIndexAtom) -> Boolean
	): MutableMap<KTIntArrayList, MutableList<AtomByTokens>> {
		val boundaries = KTLongObjectMap<AtomByTokens>()
		// in the order the words found them, where java walks a hash map: of two boundaries with as many other words
		// that contain each other, the one met first is deleted
		val ordered = ArrayList<AtomByTokens>()
		// 1. index boundaries by tokens
		for (tokenOrder in tokens.indices) {
			val token = tokens[tokenOrder]
			for (a in token.atoms) {
				if (filter(a)) {
					var b = boundaries[a.id]
					if (b == null) {
						b = AtomByTokens(a, KTIntArrayList(5))
						boundaries.put(a.id, b)
						ordered.add(b)
					}
					b.lstTokens.add(tokenOrder)
				}
			}
		}
		// 2. combine boundaries by same tokens to find the largest boundary
		val regroup = LinkedHashMap<KTIntArrayList, MutableList<AtomByTokens>>()
		for (b in ordered) {
			regroup.getOrPut(b.lstTokens) { ArrayList() }.add(b)
		}
		return regroup
	}

	private fun readAtoms(
		tokens: MutableList<SpatialSearchToken>, b: BinaryMapIndexReader, indx: NameIndexReader, indxInd: Int,
		locale: String
	) {
		// sort to assign tokens to '2nd street 2' first instead '2 2nd street'
		tokens.sortWith { o1, o2 ->
			val cm = KSearchAlgorithms.isNumber2Letters(o1.word).compareTo(KSearchAlgorithms.isNumber2Letters(o2.word))
			if (cm != 0) cm else o1.originalOrder.compareTo(o2.originalOrder)
		}
		for (t in tokens) {
			if (settings.SEARCH_POI_BY_CATEGORY_ONLY && indx.poiRegion == null) {
				continue
			} else if (!settings.SEARCH_POI && indx.poiRegion != null) {
				continue
			} else if (!settings.SEARCH_ADDR && indx.addressRegion != null) {
				continue
			}
			val query = if (t.broad) t.word + " " else t.word // a broad word reads other keys
			var matchedPrefixes = indx.getMatchedPrefixes(query)
			if (matchedPrefixes == null) {
				stats.sub1FileAtomsTime.start()
				matchedPrefixes = b.readFullNameIndex(indx.setQuery(query, t.getPrefixMatcher(stats, locale)))
				stats.sub1FileAtomsTime.finish()
				if (matchedPrefixes == null) {
					continue
				}
			}
			for (prefix in matchedPrefixes) {
				parseAtomSuffixes(t, indxInd, indx, prefix, tokens, locale)
			}
			typos.countNeighbours(t, matchedPrefixes)
		}
	}

	private fun makeAddrId(fileInd: Int, shiftToIndex: Long): Long {
		if (fileInd > 1 shl SHIFT_FILE_IND) {
			throw IllegalStateException()
		}
		return (shiftToIndex shl SHIFT_FILE_IND) + fileInd
	}

	private fun makePoiId(fileInd: Int, shiftToIndex: Long, poiInd: Int): Long {
		if (fileInd > 1 shl SHIFT_FILE_IND) {
			throw IllegalStateException()
		}
		if (poiInd > 1 shl SHIFT_POI_IND) {
			throw IllegalStateException()
		}
		return (((shiftToIndex shl SHIFT_POI_IND) + poiInd) shl SHIFT_FILE_IND) + fileInd
	}

	private fun parseAtomSuffixes(
		t: SpatialSearchToken, indInd: Int, indx: NameIndexReader, prefix: PrefixNameValue,
		allTokens: List<SpatialSearchToken>, locale: String
	) {
		var curSuffix: String? = null
		val suffixes = ArrayList<String>()
		val commonSuffixes = ArrayList<String>()
		val addrData = prefix.getAddr()
		val poiData = if (addrData == null) prefix.getPoi() else null
		val addr = addrData != null
		for (s in addrData?.getSuffixesDictionaryList() ?: poiData!!.getSuffixesDictionaryList()) {
			curSuffix = KSearchAlgorithms.nameIndexDecodeDictionarySuffix(curSuffix, s)
			suffixes.add(prefix.key + curSuffix)
		}
		for (i in addrData?.getSuffixesCommonDictionaryList() ?: poiData!!.getSuffixesCommonDictionaryList()) {
			commonSuffixes.add(indx.getCommonIndexed(i))
		}
		if (addrData != null && settings.SEARCH_ADDR) {
			for (a in addrData.getAtomList()) {
				val lid = makeAddrId(indInd, prefix.shift - a.getShiftToIndex(0))
				var pid = 0L
				if (a.getType() == CityBlocks.STREET_TYPE.index) {
					pid = makeAddrId(indInd, prefix.shift - a.getShiftToCityIndex(0))
				} else if (a.getType() != CityBlocks.BOUNDARY_TYPE.index && a.getType() != CityBlocks.CITY_TOWN_TYPE.index
					&& a.getType() != CityBlocks.VILLAGES_TYPE.index && a.getType() != CityBlocks.POSTCODES_TYPE.index
				) {
					continue
				}
				val obj: MapObject? = null
				parseSuffixes(t, indx, suffixes, commonSuffixes, a, null, lid, pid, obj, allTokens, locale)
			}
		} else if (!addr && settings.SEARCH_POI) {
			for (a in poiData!!.getAtomsList()) {
				if (a.getPoiIndInBlockCount() == 0) {
					// intermediate version ignore
					continue
				}
				val lid = makePoiId(
					indInd, BinaryMapIndexReader.convertFixed32ToRef(a.getShiftTo()).toLong(),
					a.getPoiIndInBlock(0)
				)
				val amenity: MapObject? = null
				if (settings.SEARCH_POI_BY_CATEGORY_ONLY && skipFilteredZoomObject(t, a.getX(), a.getY(), a.getEloRatingCount() > 0, false)) {
					continue
				}
				parseSuffixes(t, indx, suffixes, commonSuffixes, null, a, lid, 0, amenity, allTokens, locale)
			}
		}
	}

	private fun skipFilteredZoomObject(t: SpatialSearchToken, x16: Int, y16: Int, elo: Boolean, add: Boolean): Boolean {
		val bbox = settings.SEARCH_POI_BY_CATEGORY_BBOX
		if (bbox != null) {
			if (x16 < bbox[0] || x16 > bbox[2] || y16 < bbox[1] || y16 > bbox[3]) {
				return true
			}
		}
		val z = 16 - settings.SEARCH_POI_BY_CATEGORY_ZOOM
		if (z < 0) {
			return false
		}
		val tileId = KMapUtils.interleaveBits((x16 shr z).toLong(), (y16 shr z).toLong())
		if (t.cacheCategoryFilterObjects.contains(tileId) && !elo) {
			return true
		}
		if (add) {
			t.cacheCategoryFilterObjects.add(tileId)
		}
		return false
	}

	fun readPOIBboxes(indInd: Int, tiles: KTLongHashSet) {
		var nameIndex: NameIndexReader? = null
		var c: SpatialSearchFileCache? = null
		for (k in internalFile.indices) {
			c = internalFile[k]
			if (indInd < c.indexInd + c.indexReaders.size) {
				nameIndex = c.indexReaders[indInd - c.indexInd]
				break
			}
		}
		stats.sub2ReadObjTime.start()
		val bmir = files[c!!.fileInd]
		val bytesRead = bmir.getBytesRead()
		bmir.readAmenityBboxes(nameIndex!!.poiRegion!!, tiles)
		stats.readObjsBytes += (bmir.getBytesRead() - bytesRead)
		stats.sub2ReadObjTime.finish()
	}

	fun getFileInd(id: Long): Int = (id and ((1L shl SHIFT_FILE_IND) - 1)).toInt()

	fun readPoiObject(id: Long, cache: KTLongObjectMap<MapObject>?): MapObject? = readPoiObject(id, cache, null)

	fun readPoiObject(objectId: Long, cache: KTLongObjectMap<MapObject>?, wanted: KTLongHashSet?): MapObject? {
		var id = objectId and ((1L shl SHIFT_ALT_NAME) - 1) // the alternative name variant reads the same object
		if (cache != null) {
			val mapObject = cache[id]
			if (mapObject != null) {
				return mapObject
			}
		}
		val oid = id
		val indInd = (id and ((1L shl SHIFT_FILE_IND) - 1)).toInt()
		id = id shr SHIFT_FILE_IND
		val poiInd = (id and ((1L shl SHIFT_POI_IND) - 1)).toInt()
		id = id shr SHIFT_POI_IND
		val shift = id

		var nameIndex: NameIndexReader? = null
		var c: SpatialSearchFileCache? = null
		for (k in internalFile.indices) {
			c = internalFile[k]
			if (indInd < c.indexInd + c.indexReaders.size) {
				nameIndex = c.indexReaders[indInd - c.indexInd]
				break
			}
		}

		stats.sub2ReadObjTime.start()
		val bmir = files[c!!.fileInd]
		val bytesRead = bmir.getBytesRead()
		val lst = bmir.readAmenityBlock(nameIndex!!.poiRegion!!, shift, poiInd)
		if (cache != null) {
			val ofirstid = oid - (poiInd shl SHIFT_FILE_IND)
			for (i in lst.indices) {
				val bid = ofirstid + (i shl SHIFT_FILE_IND)
				if (wanted == null || wanted.contains(bid)) {
					cache.put(bid, lst[i])
				}
			}
		}
		if (poiInd >= lst.size) {
			LOG.error("Error reading $shift - $poiInd object from ${c.file} ")
			return null
		}
		val amenity = lst[poiInd]
		stats.readObjsBytes += (bmir.getBytesRead() - bytesRead)
		stats.sub2ReadObjTime.finish()
		return amenity
	}

	fun readAddrObject(objectId: Long, parentId: Long, cache: KTLongObjectMap<MapObject>?): MapObject? {
		if (cache != null) {
			val obj = cache[objectId]
			if (obj != null) {
				return obj
			}
		}
		var pid = parentId
		val opid = pid
		var id = objectId and ((1L shl SHIFT_ALT_NAME) - 1) // the alternative name variant reads the same object
		val indInd = (id and ((1L shl SHIFT_FILE_IND) - 1)).toInt()
		id = id shr SHIFT_FILE_IND
		val shift = id

		var nameIndex: NameIndexReader? = null
		var c: SpatialSearchFileCache? = null
		for (k in internalFile.indices) {
			c = internalFile[k]
			if (indInd < c.indexInd + c.indexReaders.size) {
				nameIndex = c.indexReaders[indInd - c.indexInd]
				break
			}
		}
		val bmir = files[c!!.fileInd]
		val bytesRead = bmir.getBytesRead()
		stats.sub2ReadObjTime.start()
		val addressRegion = nameIndex!!.addressRegion!!
		val obj: MapObject?
		if (pid != 0L) {
			val pIndInd = (pid and ((1L shl SHIFT_FILE_IND) - 1)).toInt()
			pid = pid shr SHIFT_FILE_IND
			val pshift = pid
			if (pIndInd != indInd) {
				throw UnsupportedOperationException()
			}
			var city: City? = null
			if (cache != null) {
				city = cache[opid] as City?
			}
			if (city == null) {
				city = bmir.readCityObject(addressRegion, pshift)!!
				city.setReferenceFile(bmir)
			}
			obj = bmir.readStreetObject(addressRegion, city, shift)
		} else {
			obj = bmir.readCityObject(addressRegion, shift)
		}
		if (obj is City) {
			obj.setReferenceFile(bmir)
		}
		stats.readObjsBytes += (bmir.getBytesRead() - bytesRead)
		stats.sub2ReadObjTime.finish()
		return obj
	}

	private fun parseSuffixes(
		t: SpatialSearchToken, indx: NameIndexReader, suffixes: List<String>,
		commonSuffixes: List<String>, a: AddressNameIndexDataAtom?, b: OsmAndPoiNameIndexDataAtom?, cid: Long, pid: Long,
		obj: MapObject?, allTokens: List<SpatialSearchToken>, locale: String
	) {
		val cnt = a?.getSuffixesBitsetIndexCount() ?: b!!.getSuffixesBitsetIndexCount()
		var name = ""
		var wordInd = 0
		val type = a?.getType() ?: SpatialSearchToken.POI_TYPE
		var poiTypes: KTIntArrayList? = null
		var elo = 0
		if (b != null) {
			if (b.getEloRatingCount() > 0) {
				elo = b.getEloRating(0)
			}
			if (indx.poiRegion!!.isBasemap()) {
				elo = max(settings.WORLD_ELO_RATING, elo)
			}
			poiTypes = parsePoiTypes(indx, b, poiTypes)
		}
		val cmnWord = BooleanArray(2) // common, kind word
		for (i in 0 until cnt) {
			val suffBit = a?.getSuffixesBitsetIndex(i) ?: b!!.getSuffixesBitsetIndex(i)
			if (suffBit % 2 == 0) {
				val ind = suffBit / 2 - 1
				if (ind == -1) {
					if (a != null && wordInd < a.getExtraSuffixCount()) {
						name += a.getExtraSuffix(wordInd)
					} else if (b != null && wordInd < b.getExtraSuffixCount()) {
						name += b.getExtraSuffix(wordInd)
					}
					var accepted = matchName(indx, t, name, poiTypes, cmnWord, locale, type)
					if (!accepted) {
						val partName = matchPartName(t, name, allTokens, locale, type)
						if (partName != null) {
							name = partName
							accepted = true
						}
					}
					if (accepted) {
						val other = if (a != null) {
							if (wordInd < a.getOtherWordsCountCount()) a.getOtherWordsCount(wordInd) else 0
						} else {
							if (wordInd < b!!.getOtherWordsCountCount()) b.getOtherWordsCount(wordInd) else 0
						}
						addObject(
							t, indx, name, type, cid, pid, obj, other, poiTypes, elo,
							NameIndexAtomXY(a, b, settings), allTokens, cmnWord, locale
						)
					}
					wordInd++
					name = ""
				} else if (ind < suffixes.size) {
					name += suffixes[ind]
				} else {
					// common suffix
					name += " " + commonSuffixes[ind - suffixes.size]
				}
			} else {
				if (suffBit % 4 == 1) {
					// separated number
					name += " " + (suffBit shr 2)
				} else {
					// partial
					name += (suffBit shr 2)
				}
			}
		}
		if (a != null && wordInd < a.getExtraSuffixCount()) {
			name += a.getExtraSuffix(wordInd)
		} else if (b != null && wordInd < b.getExtraSuffixCount()) {
			name += b.getExtraSuffix(wordInd)
		}
		if (name.isNotEmpty()) {
			var accepted = matchName(indx, t, name, poiTypes, cmnWord, locale, type)
			if (!accepted) {
				val partName = matchPartName(t, name, allTokens, locale, type)
				if (partName != null) {
					name = partName
					accepted = true
				}
			}
			if (accepted) {
				val other = if (a != null) {
					if (wordInd < a.getOtherWordsCountCount()) a.getOtherWordsCount(wordInd) else 0
				} else {
					if (wordInd < b!!.getOtherWordsCountCount()) b.getOtherWordsCount(wordInd) else 0
				}
				// object will be added once it's read rare word
				// disabled for now as it could only have effect for frequent words in index
				addObject(
					t, indx, name, type, cid, pid, obj, other, poiTypes, elo, NameIndexAtomXY(a, b, settings),
					allTokens, cmnWord, locale
				)
			}
		}
	}

	private fun parsePoiTypes(indx: NameIndexReader, b: OsmAndPoiNameIndexDataAtom, types: KTIntArrayList?): KTIntArrayList? {
		var poiTypes = types
		if (b.getPoiCategoriesCount() > 0) {
			poiTypes = KTIntArrayList()
			val poiRegion = indx.poiRegion!!
			for (k in 0 until b.getPoiCategoriesCount()) {
				var spatialType: SpatialPoiType? = null
				val catFile = b.getPoiCategories(k)
				val subType = StringBuilder()
				if (catFile % 2 == 0) {
					val pc = poiRegion.decodePoiType(catFile / 2, subType)
					if (subType.isNotEmpty()) {
						spatialType = poiSearch.getByKey(subType.toString())
					}
					if (pc != null && spatialType == null) {
						spatialType = poiSearch.getByKey(pc.getKeyName())
					}
				} else {
					val st = poiRegion.getSubtypeFromId(catFile / 2, subType)
					if (st != null) {
						var fullKey = st.name!!
						if (st.isTopIndex()) {
							fullKey = st.name + "_" + TopIndexFilter.getValueKey(subType.toString())
						}
						spatialType = poiSearch.getByKey(fullKey)
					}
				}
				if (spatialType != null) {
					poiTypes.add(spatialType.id)
				}
			}
		}
		return poiTypes
	}

	private fun matchName(
		indx: NameIndexReader, t: SpatialSearchToken, name: String,
		poiTypes: KTIntArrayList?, commonWord: BooleanArray?, locale: String, type: Int
	): Boolean {
		stats.sub1MatchTime.start()
		val iSpace = name.indexOf(' ')
		var mname = if (iSpace >= 0) name.substring(0, iSpace) else name
		var acceptName = t.matchName(mname, poiTypes, locale, type)
		if (!acceptName && iSpace >= 0) {
			val split = WorldRegion.splitLikeJava(name, " ")
			for (k in 1 until split.size) {
				val combiName = mname + split[k]
				if (t.matchName(combiName, null, locale, type)) {
					// query 'weberstrasse' matches 'weber straße': works for popular suffixes
					mname = combiName
					acceptName = true
					break
				} else if (KSearchAlgorithms.startsWithDigit(split[k]) && t.matchName(mname + "-" + split[k], null, locale, type)) {
					// "us 15" match "us-15" (as we don't split before numbers)
					mname = mname + "-" + split[k]
					acceptName = true
					break
				}
			}
		}
		if (commonWord != null) {
			commonWord[0] = isWordCommonlyUsed(indx, mname)
			if (commonWord.size > 1) {
				commonWord[1] = isKindWord(indx, mname)
			}
		}
		stats.sub1MatchTime.finish()
		return acceptName
	}

	private fun matchPartName(
		t: SpatialSearchToken, name: String, allTokens: List<SpatialSearchToken>,
		locale: String, type: Int
	): String? {
		stats.sub1MatchTime.start()
		val res = t.matchSplitName(name)
		var resName: String? = null
		if (res != null) {
			for (st in allTokens) {
				if (st !== t && st.matchName(res[1], null, locale, type)) {
					resName = res[0] + " " + res[1]
					break
				}
			}
		}
		stats.sub1MatchTime.finish()
		return resName
	}

	private fun addObject(
		t: SpatialSearchToken, indx: NameIndexReader, name: String, type: Int, objectId: Long, pid: Long,
		obj: MapObject?, otherWords: Int, poiTypes: KTIntArrayList?, elo: Int, coords: NameIndexAtomXY,
		allTokens: List<SpatialSearchToken>, cmnWord: BooleanArray?, locale: String
	) {
		var lid = objectId
		var other = otherWords
		var otherTokens: MutableList<SpatialSearchToken>? = null
		var streetCity = false
		var ruleSpelling = false
		var numericNotMatch = false
		var altVariant = 0
		// the word that found the object: common in this map ("avenue", "rue") names no object
		var distinct = if (cmnWord != null && cmnWord.size > 1 && cmnWord[1]) 0 else 1
		var split: List<String>? = null
		if (name.indexOf(' ') != -1) {
			split = KSearchAlgorithms.splitAndNormalize(name, false)
		}
		// split '-' to allow search 'M-42' as 'M 42'
		if (name.indexOf('-') != -1 && !t.word.contains("-")) {
			split = KSearchAlgorithms.splitAndNormalize(name.replace('-', ' '), false)
		}
		// '2.Sokak'
		if (name.indexOf('.') != -1) {
			split = KSearchAlgorithms.splitAndNormalize(name.replace('.', ' '), false)
		}
		if (poiTypes != null && poiTypes.size() > 0) {
			for (token in allTokens) {
				if (t !== token && token.matchPoiCategoryKeys(poiTypes) && (otherTokens == null || !otherTokens.contains(token))) {
					if (otherTokens == null) {
						otherTokens = ArrayList(3)
					}
					otherTokens.add(token)
				}
			}
		}
		var nameFound = 0
		if (split != null) {
			for (k in 1 until split.size) {
				val otherName = split[k]
				val numeric = KSearchAlgorithms.isNumber2Letters(otherName)
				if (otherName.equals(NameIndexReader.CITY_AS_STREET_COMMON, ignoreCase = true)) {
					streetCity = true
					continue
				}
				if (otherName.equals(NameIndexReader.RULE_SPELLING_COMMON, ignoreCase = true)) {
					ruleSpelling = true
					continue
				}
				val marker = NameIndexReader.altNameVariant(otherName)
				if (marker > 0) {
					altVariant = marker
					continue
				}
				var matched = false
				for (tk in allTokens) {
					var token = tk
					if (t !== token && matchName(indx, token, otherName, poiTypes, null, locale, type)
						&& (otherTokens == null || !otherTokens.contains(token))
					) {
						if (otherTokens == null) {
							otherTokens = ArrayList(3)
						}
						token = nearestCopy(allTokens, t, token, otherTokens)
						otherTokens.add(token)
						matched = true
						nameFound++
						if (!isKindWord(indx, otherName)) {
							distinct++
						}
						break
					}
				}
				if (!matched) {
					if (numeric) {
						numericNotMatch = !t.word.contains(otherName) // "us 15" data, "us-15" token
					}
					if (!t.globalRules.dictionary(locale).isCommonSkipOtherCnt(otherName, t.ruleOwner(type)) &&
						!isWordCommonlyUsed(indx, otherName)
					) { // To choose Tour eiffel or onlyWest / North !
						other++
					}
				}
			}
		}
		// the query word only names this object's category ('hotel' in 'Hotel Sacher') and every other query word is already
		// explained by the category: the category atom finds the object without the unmatched name words
		if (other > 0 && nameFound == 0 && (otherTokens?.size ?: 0) == allTokens.size - 1
			&& poiTypes != null && t.matchPoiCategoryKeys(poiTypes) && (split == null || t.matchName(split[0], null, locale, type))
		) {
			return
		}
		val otherFound = otherTokens?.size ?: 0
		var nearByType = 0
		while (nearByType < limitLocationBboxes.size) {
			if (coords.intersects(limitLocationBboxes[nearByType])) {
				break
			}
			nearByType++
		}
		if (altVariant > 0) {
			// an alternative name with its own marker is its own object: its words never take the slots of the main name
			lid += altVariant.toLong() shl SHIFT_ALT_NAME
		}
		val atom = NameIndexAtom(
			name, type, lid, pid, obj, streetCity, other, otherFound, coords,
			nearByType, -1
		)
		atom.locale = locale
		atom.ruleSpelling = ruleSpelling
		atom.distinctFoundCnt = distinct
		atom.poiTypes = poiTypes
		atom.elo = elo
		if (settings.SEARCH_POI_BY_CATEGORY_ONLY) {
			skipFilteredZoomObject(t, atom.coords.x16, atom.coords.y16, atom.elo > 0, true)
		}
		// for all common always false, for some frequent could be optimization
		if (settings.OPTIM_READ_COMMON_WITH_OTH_NON_FOUND_ATOMS && cmnWord!![0]) {
			// name 'ru de rue' could match 'rue' it's because of prefix & suffixes

			if (other > 0) {
				// skip rare words to be added specifically
				t.addPartialOtherAtom(atom, otherTokens, numericNotMatch)
				return
			} else {
				// consists only of common words
				t.addPartialCommonAtom(atom, otherTokens, numericNotMatch)
				return
			}
		}
		if (settings.OPTIM_READ_POI_CATEGORY_WORD_ATOMS && t.hasPoiCategoryKeys() && atom.isPOI()) {
			// we always add to partial so if we word overloaded we don't display it
			// doesn't matter if we read token by name "cafe" or "#^cafe" the word associated with category
			t.addPartialCommonAtom(atom, otherTokens, numericNotMatch)
			return
		}
		val added = t.addAtom(atom)
		if (added) {
			if (otherTokens != null) {
				for (otherToken in otherTokens) {
					otherToken.addAtom(NameIndexAtom(atom))
				}
			}
			addBuildingRefAtoms(t, allTokens, otherTokens, numericNotMatch, atom)
		}
	}

	/**
	 * A word the query has twice belongs to the name next to it: in "travessa de santo antónio rua joaquim ribeiro de
	 * carvalho" the street found by "antónio" takes the first "de" and the street found by "ribeiro" the second.
	 * Copies side by side ("w w" of "W&W") are one name and keep their order.
	 */
	private fun nearestCopy(
		allTokens: List<SpatialSearchToken>, found: SpatialSearchToken,
		copyToken: SpatialSearchToken, used: List<SpatialSearchToken>
	): SpatialSearchToken {
		var copy = copyToken
		val f = allTokens.indexOf(found)
		for (other in allTokens) {
			if (other === found || other === copy || other.word != copy.word || used.contains(other)) {
				continue
			}
			val o = allTokens.indexOf(other)
			val c = allTokens.indexOf(copy)
			if (abs(o - c) > 1 && abs(o - f) < abs(c - f)) {
				copy = other
			}
		}
		return copy
	}

	/** a word the map mostly leaves unindexed says what an object is ("avenue", "вулиця", "calle"),
	 *  not which one: measured on the map, like the common words, never listed by hand */
	internal fun isKindWord(indx: NameIndexReader, word: String?): Boolean =
		word != null && (wordFlags(indx, word) and WORD_KIND) != 0

	private fun isWordCommonlyUsed(indx: NameIndexReader, mainWord: String): Boolean =
		(wordFlags(indx, mainWord) and WORD_COMMON) != 0

	/** matchName and addObject ask about the same few words for every atom they read: decide once per word */
	private fun wordFlags(indx: NameIndexReader, word: String): Int {
		// do not store commonlyUsedWords across all files!
		// it creates bug "united states" not common for world, regions.ocbf (apple test case - appletree)
		if (indx !== wordFlagsReader) {
			wordFlagsReader = indx
			wordFlags.clear()
		}
		val cached = wordFlags[word]
		if (cached != null) {
			return cached
		}
		var flags = 0
		if (!KSearchAlgorithms.isNumber2Letters(word)) {
			val stats = indx.getCommonWordsStats()
			val v = stats?.get(KSearchAlgorithms.alignChars(word))
			if (v != null) {
				flags = flags or WORD_COMMON
				// nonindexed = freq - extra; "avenue" 12662 of 19992 in New York, "york" 0 of 170
				if (v.freq > 0 && (v.freq - v.extra) >= v.freq * settings.KIND_WORD_NONINDEXED_SHARE) {
					flags = flags or WORD_KIND
				}
			}
		}
		wordFlags[word] = flags
		return flags
	}

	internal fun addBuildingRefAtoms(
		t: SpatialSearchToken, allTokens: List<SpatialSearchToken>,
		otherTokens: List<SpatialSearchToken>?, numericNotMatchObject: Boolean, atom: NameIndexAtom
	) {
		val street = atom.type == SpatialSearchToken.STREET_TYPE
		val poi = atom.type == SpatialSearchToken.POI_CATEGORY_TYPE || atom.type == SpatialSearchToken.POI_TYPE
		// numericNotMatch object name contains numeric - require full street (poi) name match to assign buildings
		if (numericNotMatchObject) {
			return
		}
		if (!(street && settings.SEARCH_BUILDINGS) && !(poi && settings.SEARCH_POI_REF)) {
			return
		}
		val typeToAdd = if (street) SpatialSearchToken.BUILDING_TYPE else SpatialSearchToken.POI_REF_TYPE
		for (token in allTokens) {
			// assign building to word token isNumber2Letters (number + 1 char) + possible
			if (t !== token && (otherTokens == null || !otherTokens.contains(token))) {
				if ((street && token.likelyPartOfBuilding(atom.locale)) || (poi && token.likelyRef())) {
					val atomB = NameIndexAtom(
						atom.name, typeToAdd, atom.id,
						atom.parentid, atom.`object`, atom.cityAsStreet, atom.otherWordsCnt, atom.otherFoundCnt,
						atom.coords, atom.nearbyRadius, t.originalOrder
					)
					atomB.distinctFoundCnt = atom.distinctFoundCnt // the house number names no street
					atomB.locale = atom.locale
					atomB.ruleSpelling = atom.ruleSpelling
					token.addAtom(atomB)
				}
			}
		}
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("SpatialSearchContext")

		private const val SHIFT_FILE_IND = 14 // maxism files 16K
		private const val SHIFT_POI_IND = 10 // maximum poi 1024
		// alternative name variant 1..7 (NameIndexReader.ALT_NAME_VARIANTS) in bits 56-58 of an atom id: an address id takes up to
		// 45 bits (31-bit file offset + file index), a poi id up to 55 bits (31-bit shift + poi index + file index)
		private const val SHIFT_ALT_NAME = 56

		private const val WORD_COMMON = 1
		private const val WORD_KIND = 2

		fun calculateBbox(radiusMeters: Int, l: KLatLon): IntArray {
			val qr = KMapUtils.calculate31BboxUsingRhumb(radiusMeters, l)
			val bbox31 = IntArray(4)
			bbox31[1] = qr.top.toInt()
			bbox31[0] = qr.left.toInt()
			bbox31[3] = qr.bottom.toInt()
			bbox31[2] = qr.right.toInt()
			return bbox31
		}

		internal fun checkPoiTypeId(poiTypeId: Int): Boolean {
			// first 2^8 - 256 bytes not possible to be used by file
			if (poiTypeId > (1 shl (SHIFT_FILE_IND + 8))) {
				throw IllegalStateException("Possible overlap with addr / poi id")
			}
			return true
		}

		fun poiObjectId(id: Long): Long = id and ((1L shl SHIFT_ALT_NAME) - 1)
	}
}
