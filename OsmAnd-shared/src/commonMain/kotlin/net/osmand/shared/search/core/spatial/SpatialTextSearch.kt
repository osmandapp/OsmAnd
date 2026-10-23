package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.NameIndexReader
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiType
import net.osmand.shared.search.core.spatial.SpatialPoiSearch.SpatialPoiType
import net.osmand.shared.search.core.spatial.SpatialSearchContext.SpatialSearchStats
import net.osmand.shared.search.rules.SearchModRules
import net.osmand.shared.util.JavaFormat
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollatorStringMatcher
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.collections.KBitSet
import net.osmand.shared.util.collections.KTLongHashSet
import kotlin.math.max

//////////////// SEARCH ALGORITHM //////////////////
// 1. Init files + read caches
// 2. Split tokens
// 3. Read tokens -> atoms (
// 4. Sort tokens to do combinations
// 5. Find combinations
// 6. Sort results, filter results
// 7. Expand poi categories if needed

////////////// FUTURE OPTIMIZATIONS ////////////////
// 1. PARTIAL SEARCH. Perform equals search and then with '.'
// 2. MAPS. Do search first with closest maps and then with others
// 3. ALL COMBINATIONS. Stop on one combination or find all
// 4. POI CATEGORIES. -?
// 5. READ_ALL. Switch ALWAYS_READ_COMMON_WORDS_ATOMS=true (new results + school intersections)
// 6. OPTIMIZE POI READ. Read only 1 POI in block
////////////////////////////////////////////////////

/**
 * The spatial text search: the words of a query matched against the name indexes of the maps and
 * the objects their atoms make together.
 *
 * A copy of `SpatialTextSearch` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS. The search rules are read from [searchRulesDir], the directory of `rules*.xml`, where
 * java reads them from its classpath. Without `mainTest`, `initFile` and `searchTest`, which only the
 * diagnostics of java call, and `findObjCombinationsSimpleIteration`, which nothing calls.
 */
class SpatialTextSearch(searchRulesDir: String) {

	class SpatialTextSearchSettings private constructor() {

		///////////// GENERAL SETTINGS //////////

		var SEARCH_SUGGESTION = false // incomplete to add '.' in the end
		// not used in search as maps provided (web could multiply by 1.5x or adjust bbox)
		var SUGGESTED_SEARCH_RADIUS_KM = 400
		// "did you mean" the query with a misspelled word corrected (SpatialTypoSuggestions)
		var TYPO_SUGGESTION = true

		// lang to deduplicate results
		var LANG_DEDUPLICATE: String? = ""

		var SEARCH_ADDR = true
		var SEARCH_POI = true
		var SEARCH_BUILDINGS = true
		var SEARCH_STREET_INTERSECTIONS = true
		var SEARCH_POI_INTERSECTIONS = true
		var SEARCH_POI_CATEGORIES = true
		var SEARCH_POI_REF = true
		var SUGGEST_SEARCH_POI_CATEGORY_WITH_REF = true

		// replacement to search by category via name index
		var SEARCH_POI_BY_CATEGORY_ONLY = false
		var SEARCH_POI_BY_CATEGORY_ZOOM = 17 // 17+ no filter
		var SEARCH_POI_BY_CATEGORY_BBOX: IntArray? = null

		// max prefixes for each name reader
		var AUTO_CLEAR_PREFIX_CACHE_LIMIT = 1000
		// max bytes of name index blocks kept in all files during and between searches
		var AUTO_CLEAR_PREFIX_CACHE_BYTES = 64L shl 20

		// Deduplicate results in the end by checking osm id of the first object in combination
		var DEDUPLICATE_RES = true

		///////////// FINE TUNE SETTINGS ///////////

		// display only top categories maximum
		var LIMIT_POI_CATEGORY_BY_FREQ = 5

		// performance tested (we need to turn on for <POI + Address> search)
		var ALLOW_HOUSE_POI_TYPE_INTERSECTION = true
		// no intersection recorded but streets are nearby
		var ALLOW_VIRTUAL_STREET_INTERSECTIONS = true
		// SHOULD BE false: '<NAME>' inside '<NAME_AREA>', then <NAME_AREA> be part of Address section not POI
		var USE_POI_BBOX = false

		// Enlarge boundaries in case result is not found
		// > 300 km - x0, for 50km-300km - x0.5, 10-50km - x1.5, 10km - x3sorted!
		// in the order of the keys, as java's TreeMap keeps them
		var ENLARGE_BOUNDARIES: Map<Int, Double> = linkedMapOf(-300_000 to 0.2, -100_000 to 0.5, -10_000 to 1.0, -1_000 to 20.0)

		// Hide results under SHOW MORE
		// the ladder widens and ends: the last level holds the rest, within the limits of the search itself
		var SHOW_MORE_WORDS_COUNT = intArrayOf(3, 10, 30, 100, 300, 1000)
		// with score ranking a level also ends where the score drops this much below its first row
		var SHOW_MORE_SCORE_DROP = 0.5
		// ... or holds this many times its minimum: hundreds of rows with one score give no drop to cut at
		var SHOW_MORE_MAX_LEVEL_TIMES = 3

		// only do incomplete search with 2+ chars
		var MIN_CHARACTERS_INCOMPLETE = 2

		var SCORE_RANKING = true // false - old lexicographic ladder
		// one-word query: POIs found by category only and unrated, read nearest first ("restaurant" finds 180K; 0 - all)
		var LIMIT_READ_SINGLE_OBJECTS = 1500
		// objects read for one stage, the best by SpatialSearchRanking.prescore (0 - all)
		var LIMIT_READ_OBJECTS = 1000
		// a word still being typed whose index blocks are larger is matched whole ('sch' 14 MB; 0 - never)
		var LIMIT_INCOMPLETE_BYTES = 5 shl 20

		var MIN_ELO_RATING = 1400 // see SearchResult.MIN_ELO_RATING
		var WORLD_ELO_RATING = 1500 // from world map by default

		// no need to find 3 street intersection or 3 POI intersection
		var LIMIT_ATOMIC_OBJECTS = 2

		// share of a common word left unindexed from which it names a kind, not an object.
		// Address index of the 2026-09 maps: "street", "avenue", "straße" 0.90-1.00, but street names reach
		// 0.2 too - "john" 0.31, "lange" 0.33, "hohe" 0.26 - and 0.2 sent "707 John Street" below Johnson Street
		var KIND_WORD_NONINDEXED_SHARE = 0.5

		// Create default bboxes for points POI / Address objects
		var POI_DEFAULT_RADIUS = 50
		var ADDR_DEFAULT_RADIUS = 1000
		var POI_HOUSE_DEFAULT_RADIUS = 300

		///////////// DEV FEATURES ///////////

		// FEATURE #1. Use tileId as flat (incomplete) or skip hash tree complete solution
		var DEV_USE_SKIP_HASH_TREE = true

		// FEATURE #2. PIPELINE vs INTERSECTIONS algorithm
		// Use mechanism to smart selection results intead of all word x word intersections
		var DEV_USE_PIPELINE = true

		// print some poi cat - to be deleted once web/android completed
		var TEST_PRINT_POI_CAT_LIMIT = 0 // 10
		var TEST_PRINT_POI_CAT_RADIUS_KM = 10

		///////////// OPTIMIZATIONS ///////////

		// OPTIMIZATION #1
		// produces x10 less intersection and maintains x2-x4 ratio for DEDUPLICATE_RES
		// by deleting embedded or duplicate boundaries in each other
		var OPTIM_DELETE_EMBEDDED_BOUNDARIES = true

		// OPTIMIZATION #2 (IMPORTANT for results - public transport stops)
		// In case POI is called 'Bratislava' it will be not allowed to be searched as POIxPOI, POIxStreet
		// Related frequent POIs like "City&Bike 4th Street..." or public transport stops
		var OPTIM_FLAG_POI_SAME_AS_CITY_STREET = true
		var DEV_FLAG_POI_SAME_AS_CITY_TREE = true

		// OPTIMIZATION #3. IMPORTANT to filter results by words popular words (not effective in corner cases like paterson)
		// 1. If object does have rare words and they are not in query - skip it
		//    Automatically implemented for common via index, for frequent disabled for now
		// 2. If object does have other common words and they are not in query - skip it
		// Problem search: School On Street - some schools have specifiers and some don't
		// Below limit add all possible objects
		var OPTIM_READ_COMMON_WORDS_LIMIT = 3000
		var OPTIM_READ_COMMON_WITH_OTH_NON_FOUND_ATOMS = true
		var OPTIM_READ_POI_CATEGORY_WORD_ATOMS = true
		// do not filter objects with such rating from results
		var MIN_ELO_RATING_TO_KEEP_IN_ATOM = 0

		//////// INTERSECTION ALGORITHM ////////
		var OPTIM_LIMIT_RADIUS = intArrayOf(10_000, 30_000, 80_000, 200_000)
		var OPTIM_LIMIT_INTERSECTIONS = 30_000 // 10K (fast enough) or 50K (slow) - in new york  26,630 (3) -> 2,502 unique

		// Limit evaluation intersection for unique objects
		var LIMIT_STOP_GOALS_ANY_LEVEL_WHEN_REACHED_RES = 1000
		// if there are >= 10 results matching 5 words, 4 words match won't be considered
		var LIMIT_STOP_GOALS_LEVEL_1__WHEN_REACHED_RES = 1 // could be 3
		// overall max without results (evaluate maximum 3 missing words)
		var MAX_TOTAL_LIMIT_GOAL_LEVEL = 3
		////////////////////////////////////////

		//////// PIPELINE ALGORITHM ////////
		var PIPELINE_MAX_STEPS = 8 // 0 - fully covered 1 object, 1 - 2 objects, 2 - 3 objects ...
		// {100, 500} - STOP EVALUATION - if at least 100 fully covered or 500 2 objects
		var MAX_PIPELINE_ANY_RES = 10000
		var MAX_PIPELINE_RES_TO_STOP = intArrayOf(50, 3, 1)
		var PIPELINE_FREQUENT_OBJECTS_THRESHOLD = 5000
		var PIPELINE_MAX_VIRTUAL_MASKS = 7

		fun evalEnlargeBoundary(mp: Map<Int, Double>, dim: Double): Double {
			var v = 0.0
			for ((key, value) in mp) {
				if (dim > -key) {
					break
				}
				v = value
			}
			return v
		}

		companion object {

			fun defaultSettings(): SpatialTextSearchSettings = SpatialTextSearchSettings()

			fun searchPoiByCategorySettings(zoom: Int, r: KQuadRect?): SpatialTextSearchSettings {
				val shift = 3 // test 3, 4, 5, 6
				val settings = SpatialTextSearchSettings()
				settings.ALLOW_HOUSE_POI_TYPE_INTERSECTION = false
				settings.SEARCH_ADDR = false
				settings.SEARCH_POI = true
				settings.SEARCH_POI_CATEGORIES = false
				settings.OPTIM_READ_COMMON_WITH_OTH_NON_FOUND_ATOMS = false
				settings.OPTIM_READ_POI_CATEGORY_WORD_ATOMS = false

				settings.SEARCH_POI_BY_CATEGORY_ONLY = true
				settings.SEARCH_POI_BY_CATEGORY_ZOOM = zoom + shift
				if (r != null) {
					settings.SEARCH_POI_BY_CATEGORY_BBOX = categoryBbox(r)
				}
				return settings
			}

			fun searchPoiCategoriesSettings(zoom: Int, r: KQuadRect?): SpatialTextSearchSettings {
				val settings = SpatialTextSearchSettings()
				settings.SEARCH_ADDR = false
				settings.SEARCH_POI = false
				settings.SEARCH_POI_CATEGORIES = true
				if (r != null) {
					settings.SEARCH_POI_BY_CATEGORY_BBOX = categoryBbox(r)
				}
				return settings
			}

			fun suggestionSettings(): SpatialTextSearchSettings {
				val settings = SpatialTextSearchSettings()
				settings.SEARCH_SUGGESTION = true
				settings.SUGGESTED_SEARCH_RADIUS_KM = 300
				return settings
			}

			// the tiles of zoom 16 of the bbox
			private fun categoryBbox(r: KQuadRect): IntArray = intArrayOf(KMapUtils.get31TileNumberX(r.left) shr 15,
				KMapUtils.get31TileNumberY(r.top) shr 15, KMapUtils.get31TileNumberX(r.right) shr 15,
				KMapUtils.get31TileNumberY(r.bottom) shr 15)
		}
	}

	class SpatialSearchFileCache(r: BinaryMapIndexReader, searchRules: SearchModRules) {
		var fileInd = -1 // changing each session - not concurrent !!!
		var indexInd = -1 // changing each session - not concurrent !!!
		val file: String = r.getFile().name()
		val length: Long = r.getFile().length()
		val edition: Long = r.getDateCreated()
		val indexReaders: MutableList<NameIndexReader> = ArrayList()
		// the same indexes read for typo neighbours only, so their blocks never push out the blocks of the query
		val typoReaders: MutableList<NameIndexReader> = ArrayList()
		var poiFrequencies: MutableMap<String, Int>? = null
		var poiSearch: SpatialPoiSearch? = null
		// rules locale of the data of the map (en_US, de_CH...), whatever the language of the user is
		val locale: String

		init {
			var region: String? = file
			for (a in r.getAddressIndexes()) {
				indexReaders.add(NameIndexReader(a).setCacheRawBlocks(true))
				typoReaders.add(NameIndexReader(a).setCacheRawBlocks(true))
				region = a.getName()
			}
			for (a in r.getPoiIndexes()) {
				indexReaders.add(NameIndexReader(a).setCacheRawBlocks(true))
				typoReaders.add(NameIndexReader(a).setCacheRawBlocks(true))
				region = if (r.getAddressIndexes().isEmpty()) a.getName() else region
			}
			// the region name keeps a subregion with its own locale: "Switzerland_ticino"
			locale = searchRules.locales().forMap(region)
		}

		fun test(r: BinaryMapIndexReader): Boolean =
			r.getFile().name() == file && r.getFile().length() == length && r.getDateCreated() == edition
	}

	/**
	 * What the search keeps between searches: the caches of the maps and the search rules, read from
	 * [searchRulesDir], the directory of `rules*.xml`, where java reads them from its classpath.
	 */
	class SpatialSearchGlobalCache(searchRulesDir: String) {

		val filesCache: MutableMap<String, SpatialSearchFileCache> = HashMap()

		// read once: rules.xml and then the rules of a locale on the first map of that locale
		val searchRules: SearchModRules = SearchModRules.forSearch(searchRulesDir)
	}

	class SpatialSearchResults {

		var input: String? = null

		var tokens: MutableList<SpatialSearchToken>? = null

		var mainResults: MutableList<SpatialSearchResult>? = null

		var combinations: List<SpatialSearchResultsList>? = null

		// the input with one misspelled word corrected, null when every word is found
		var typoSuggestion: String? = null

		var stats: SpatialSearchStats? = null

		fun getFirstResult(): SpatialSearchResult? {
			val main = mainResults
			return if (main == null || main.isEmpty()) null else main[0]
		}
	}

	internal val cache = SpatialSearchGlobalCache(searchRulesDir) // reusable between sessions

	private fun sortTokens(tokens: MutableList<SpatialSearchToken>) {
		// sort from least atoms to do combinations as the most efficient
		tokens.sortWith { o1, o2 ->
			val c1 = o1.atoms.size
			val c2 = o2.atoms.size
			if (c1 != c2) {
				return@sortWith c1.compareTo(c2)
			}
			val res = o1.word.compareTo(o2.word)
			if (res != 0) {
				return@sortWith res
			}
			o1.originalOrder.compareTo(o2.originalOrder)
		}
		for (i in tokens.indices) {
			tokens[i].sortedOrder = i
		}
	}

	/**
	 * For [1, 2, 3, 4] Tokens evaluate with cache (- no cache, +in cache) longest chain
	 * 1. Goal [1, 2, 3, 4]: -[1, 2], -[1, 2, 3], -[1, 2, 3, 4]
	 * 2. Goal [1, 2, 3]: +[1, 2], +[1, 2, 3]
	 * 3. Goal [1, 2, 4]: +[1, 2], -[1, 2, 4]
	 * 4. Goal [1, 3, 4]: -[1, 3], -[1, 3, 4]
	 * 5. Goal [2, 3, 4]: -[2, 3], -[1, 3, 4]
	 * 6. Goal [1, 2]: +[1, 2]
	 * 7. Goal [1, 3]: -[1, 3] ...
	 * Once goal has enough results whole iteration stopped
	 */
	internal fun findLongestCombinations(ctx: SpatialSearchContext, tokens: List<SpatialSearchToken>): List<SpatialSearchResultsList> {
		val fullResult = ArrayList<SpatialSearchResultsList>()
		val mainGoal = KBitSet()
		mainGoal.set(0, tokens.size)

		val root = SpatialSearchResultsList()

		val cache = HashMap<KBitSet, SpatialSearchResultsList>()

		var ind = 0
		for (t in tokens) {
			val b = KBitSet()
			b.set(ind++)
			cache[b] = SpatialSearchResultsList(ctx, t, root)
			ctx.stats.tokenObjs += t.atoms.size
		}

		val goals = ArrayDeque<KBitSet>()
		val evaluated = HashSet<KBitSet>()
		goals.add(mainGoal)

		var uniqueObjects = 0
		var depth1WithResults = 0
		while (goals.isNotEmpty()) {
			val goal = goals.removeFirst()
			if (!evaluated.add(goal)) {
				continue
			}
			if (ctx.resultMatcher != null && ctx.resultMatcher!!.isCancelled()) {
				break
			} else if (goal.length() < mainGoal.length() - ctx.settings.MAX_TOTAL_LIMIT_GOAL_LEVEL) {
				break
			} else if (uniqueObjects >= ctx.settings.LIMIT_STOP_GOALS_ANY_LEVEL_WHEN_REACHED_RES) {
				break
			} else if (goal.length() < depth1WithResults) {
				break
			}
			var goalRes = cache[goal]
			if (goalRes == null) {
				val eval = KBitSet()
				goalRes = root
				var i = goal.nextSetBit(0)
				while (i >= 0) {
					val token = tokens[i]
					eval.set(i)
					val cached = cache[eval]
					if (cached == null) {
						goalRes = SpatialSearchResultsList(ctx, token, goalRes)
						ctx.stats.maxCombinations = max(ctx.stats.maxCombinations, goalRes.getCombinations())
						cache[eval.copy()] = goalRes
					} else {
						goalRes = cached
					}
					i = goal.nextSetBit(i + 1)
				}
			}
			if (ctx.isCancelled()) {
				break
			}
			goalRes!!.loadObjectsAndCalcBuildings(ctx)
			var res = goalRes.sortResults(ctx, ctx.settings.DEDUPLICATE_RES)
			val uniq = uniqueResults(res)
			if (goal == mainGoal && uniq == 0) {
				goalRes = reevalWithExtendedBoundary(ctx, goal, tokens)
				if (ctx.isCancelled()) {
					break
				}
				goalRes.loadObjectsAndCalcBuildings(ctx)
				res = goalRes.sortResults(ctx, ctx.settings.DEDUPLICATE_RES)
			}
			if (res.isNotEmpty()) {
				val matcher = ctx.resultMatcher
				if (matcher != null) {
					// optional ...
					for (p in res) {
						matcher.publish(p)
					}
				}
				uniqueObjects += uniq
				fullResult.add(goalRes)
			}
			if (uniqueObjects >= ctx.settings.LIMIT_STOP_GOALS_LEVEL_1__WHEN_REACHED_RES && depth1WithResults == 0) {
				depth1WithResults = goal.length()
			}
			if (ctx.isCancelled()) {
				break
			}
			val nextGoal = goal.copy()
			var i = nextGoal.previousSetBit(nextGoal.length() - 1)
			while (i >= 0) {
				nextGoal.set(i, false)
				if (!nextGoal.isEmpty()) {
					goals.add(nextGoal.copy())
				}
				nextGoal.set(i, true)
				i = nextGoal.previousSetBit(i - 1)
			}
		}
		return fullResult
	}

	private fun uniqueResults(res: List<SpatialSearchResult>): Int {
		var c = 0
		for (r in res) {
			if (!r.isPoiCategory()) {
				c++
			}
		}
		return c
	}

	private fun reevalWithExtendedBoundary(ctx: SpatialSearchContext, goal: KBitSet, tokens: List<SpatialSearchToken>): SpatialSearchResultsList {
		// Extend boundary for united states addresses (use 50 km radius)
		enlargeBoundaries(ctx, tokens)
		var goalRes = SpatialSearchResultsList()
		var i = goal.nextSetBit(0)
		while (i >= 0) {
			val token = tokens[i]
			goalRes = SpatialSearchResultsList(ctx, token, goalRes)
			i = goal.nextSetBit(i + 1)
		}
		return goalRes
	}

	private fun enlargeBoundaries(ctx: SpatialSearchContext, tokens: List<SpatialSearchToken>) {
		var enlarge = 0
		for (t in tokens) {
			for (a in t.atoms) {
				if (a.isGeoArea()) {
					val v = ctx.settings.evalEnlargeBoundary(ctx.settings.ENLARGE_BOUNDARIES, a.coords.dimensionInM())
					if (v > 0) {
						t.enlargeBbox(a, v)
						enlarge++
					}
				}
			}
			if (ctx.settings.DEV_USE_SKIP_HASH_TREE) {
				t.quadTreeSkip.build()
			}
		}
		if (ctx.stats.printLogs) {
			println("Enlarged boundaries $enlarge")
		}
	}

	internal var VERBOSE_RADIUS = -1

	private fun tokenStats(ctx: SpatialSearchContext, tokens: List<SpatialSearchToken>): StringBuilder {
		val s = StringBuilder(" ")
		for (t in tokens) {
			val cnts = IntArray(ctx.settings.OPTIM_LIMIT_RADIUS.size + 1)
			val bcnts = IntArray(ctx.settings.OPTIM_LIMIT_RADIUS.size + 1)
			val set = KTLongHashSet()
			for (a in t.atoms) {
				if (set.add(a.id)) {
					cnts[a.nearbyRadius]++
					if (a.isBoundary() || a.isCityVillage() || a.isPostcode()) {
						bcnts[a.nearbyRadius]++
					}
				}
			}
			val token = "   - '" + t.word + "' (" + (t.originalOrder + 1) + ")"
			s.append("\n" + token.padEnd(25) + " " + cnts.contentToString().padEnd(30) + " " + bcnts.contentToString())
		}
		// very verbose for debugging
		if (VERBOSE_RADIUS >= 0) {
			for (t in tokens) {
				println("---------\nToken " + t.word)
				for (a in t.atoms) {
					if (!a.isPOI()) {
						continue
					}
					println(a)
				}
			}
		}
		return s
	}

	fun initContext(ctx: SpatialSearchContext) {
		ctx.initFiles(cache)
	}

	private fun clearPrefixCacheIfLarge(ctx: SpatialSearchContext) {
		var bytes = 0L
		for (fc in ctx.internalFile) {
			for (r in fc.indexReaders) {
				// the query keeps the tokens with every atom and object read by this search
				r.clearQuery()
				bytes += r.getCachedBytes()
			}
		}
		if (bytes > ctx.settings.AUTO_CLEAR_PREFIX_CACHE_BYTES) {
			for (fc in ctx.internalFile) {
				for (r in fc.indexReaders) {
					r.clearPrefixes()
				}
			}
		}
	}

	fun searchAPI(input: String, ctx: SpatialSearchContext): SpatialSearchResults {
		try {
			return searchAPIInternal(input, ctx)
		} catch (e: Throwable) {
			for (r in ctx.files) {
				r.resetReadLimits()
			}
			throw e
		} finally {
			clearPrefixCacheIfLarge(ctx)
		}
	}

	// ctx is built with SpatialTextSearchSettings.searchPoiByCategorySettings
	fun searchPoiByCategory(ctx: SpatialSearchContext, key: String, bboxLatLon: KQuadRect, poiZoom: Int, limit: Int): List<Amenity> {
		var categoryKey = key
		var spatialType: SpatialPoiType? = null
		if (!categoryKey.startsWith(MapPoiTypes.TOP_INDEX_ADDITIONAL_PREFIX)) {
			spatialType = ctx.poiSearch.getByKey(categoryKey) ?: return emptyList()
			categoryKey = spatialType.getKey()
		}
		val singleType = spatialType?.singleType
		val indexed = spatialType == null || (singleType is PoiType && !singleType.isNonIndx())
		if (!indexed) {
			return ctx.poiSearch.loadPOIObjects(ctx, spatialType, bboxLatLon, poiZoom, limit)
		}
		val res = searchAPI(NameIndexReader.POI_CATEGORY_PREFIX + categoryKey, ctx)
		val amenities = ArrayList<Amenity>()
		val mainResults = res.mainResults
		if (mainResults != null) {
			for (r in mainResults) {
				for (o in r.getObjects()) {
					if (o is Amenity) {
						amenities.add(o)
					}
				}
			}
		}
		return amenities
	}

	private fun searchAPIInternal(text: String, ctx: SpatialSearchContext): SpatialSearchResults {
		var input = text
		ctx.stats.requestTime.start()
		val res = SpatialSearchResults()
		if (ctx.settings.SEARCH_SUGGESTION && !input.endsWith(KCollatorStringMatcher.INCOMPLETE_DOT.toString()) &&
			!input.endsWith(" ")
		) {
			input += KCollatorStringMatcher.INCOMPLETE_DOT
		}
		ctx.initFiles(cache)
		res.input = input

		// 1. prepare tokens
		val tokens = if (ctx.settings.SEARCH_POI_BY_CATEGORY_ONLY) {
			mutableListOf(SpatialSearchToken(cache.searchRules, 0, input, input, 1))
		} else {
			splitWords(ctx, input)
		}
		res.tokens = tokens

		// 2. read atoms & poi categories
		ctx.stats.step1Atoms.start()
		ctx.setTokens(tokens)
		ctx.processPoiCategories()
		ctx.readAtoms()
		res.typoSuggestion = ctx.typos.suggestion(input, tokens)
		ctx.stats.step1Atoms.finish()

		// 3. sort tokens
		sortTokens(tokens)
		if (ctx.stats.printLogs) {
			println("Token stats '" + input + "' (counts, boundaries): " + tokenStats(ctx, tokens).toString())
		}

		// 4. find combinations
		ctx.stats.step2Compute.start()
		val combinations = if (ctx.settings.DEV_USE_PIPELINE) {
			SpatialPipelineSearch(ctx, tokens).runPipeline()
		} else {
			findLongestCombinations(ctx, tokens)
		}
		res.combinations = combinations

		ctx.stats.step2Compute.finish()
		// 5. sort combinations, load objects, objects and filter duplicate
		res.mainResults = ArrayList()
		ctx.stats.step3Sort.start()
		if (combinations.isNotEmpty()) {
			combineSortFilterResults(ctx, res)
		}
		ctx.stats.step3Sort.finish()
		ctx.stats.requestTime.finish()
		res.stats = ctx.stats
		if (ctx.stats.printLogs) {
			println(ctx.stats)
		}
		return res
	}

	private fun combineSortFilterResults(ctx: SpatialSearchContext, res: SpatialSearchResults) {
		val combinations = res.combinations!!
		val main = combinations[0]
		val mainLength = main.getTokenCount()
		var mainResults = res.mainResults!!
		for (m in combinations) {
			val lst = m.getFinalResult() ?: m.sortResults(ctx, ctx.settings.DEDUPLICATE_RES)
			for (r in lst) {
				if (r.isPoiCategory() && m.getTokenCount() < mainLength) {
					continue
				}
				mainResults.add(r)
			}
		}
		mainResults = main.sortResults(ctx, mainResults, ctx.settings.DEDUPLICATE_RES)
		res.mainResults = mainResults
		var limitPoiCat = ctx.settings.TEST_PRINT_POI_CAT_LIMIT
		if (mainResults.isNotEmpty()) {
			val limits = ctx.settings.SHOW_MORE_WORDS_COUNT.copyOf()
			var cKey = SpatialSearchResult.compareKey(mainResults[0])
			var levelScore = mainResults[0].score
			var ind = 0
			var lind = 0
			var level = 0
			for (r in mainResults) {
				if (limitPoiCat > 0) {
					limitPoiCat = printPoiCategory(ctx, limitPoiCat, r)
				}
				val nextKey = SpatialSearchResult.compareKey(r)
				val scoreDrop = ctx.ranking != null && !r.isPoiCategory() &&
						(r.score < levelScore - ctx.settings.SHOW_MORE_SCORE_DROP ||
								lind < limits.size && ind >= ctx.settings.SHOW_MORE_MAX_LEVEL_TIMES * limits[lind])
				if (cKey != nextKey || scoreDrop) {
					if (lind < limits.size && ind >= limits[lind]) {
						level++
						levelScore = r.score
						ind = 0
						lind++
					}
					cKey = nextKey
				}
				r.visibleLevel = level
				if (!r.isPoiCategory()) {
					ind++
				}
			}
		}
	}

	private fun printPoiCategory(ctx: SpatialSearchContext, limit: Int, r: SpatialSearchResult): Int {
		var limitPoiCat = limit
		val type = r.getPoiCategory(ctx.poiSearch)
		if (type != null) {
			val nt = nanoTime()
			println("Loading poi type '" + type.key + "' - limit " + limitPoiCat + "...")
			val latLon = r.getLatLon()
			val interRes = ctx.poiSearch.loadPOIObjects(
				ctx, type,
				(latLon ?: ctx.location)!!, ctx.settings.TEST_PRINT_POI_CAT_RADIUS_KM * 1000, limitPoiCat
			)
			for (a in interRes) {
				val location = ctx.location
				val dist = if (location == null) 0.0 else KMapUtils.getDistance(location, a.getLocation()!!)
				println("\t " + a + " (" + a.getOsmId() + ") " + JavaFormat.fixed(dist / 1000.0, 2) + " km " + a.getLocation() + " ")
			}
			println(
				"... Loaded " + interRes.size + " pois " + JavaFormat.fixed((nanoTime() - nt) / 1e6, 1) + " ms (" +
						JavaFormat.fixed(ctx.stats.poiByTypeTime.ms(), 1) + " ms, " + ctx.stats.poiByTypeBboxes + " tiles, " +
						JavaFormat.grouped(ctx.stats.poiByTypeBytes / 1024) + " KB)"
			)
			limitPoiCat = 0
		}
		return limitPoiCat
	}

	fun splitWords(ctx: SpatialSearchContext, input: String): MutableList<SpatialSearchToken> {
		val owords = ArrayList<String>()
		// split by hyphen as we supposed to index them separately
		val words = KSearchAlgorithms.splitAndNormalize(input, owords, false)
		val tokens = ArrayList<SpatialSearchToken>()
		for (ind in words.indices) {
			val w = words[ind]
			if (w == SpatialSearchToken.DOT_INCOMPLETE_STRING) {
				continue
			}
			val token = SpatialSearchToken(
				cache.searchRules, ctx.settings.MIN_CHARACTERS_INCOMPLETE, w,
				owords[ind], tokens.size
			)
			tokens.add(token)
		}
		for (t in tokens) {
			for (o in tokens) {
				t.numberNamedByOther = t.numberNamedByOther or (t.mainNumber > 0 && o !== t &&
						KSearchAlgorithms.letters(o.wordNoDot) > 0 &&
						KAlgorithms.extractFirstIntegerNumber(o.wordNoDot) == t.mainNumber)
			}
		}
		return tokens
	}
}
