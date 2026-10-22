package net.osmand.shared.search.core.spatial

import net.osmand.shared.data.KQuadRect
import net.osmand.shared.util.KMapUtils

/**
 * The spatial text search: the words of a query matched against the name indexes of the maps and
 * the objects their atoms make together.
 *
 * A copy of `SpatialTextSearch` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS. So far only its settings, which the words of a query read; the search itself comes
 * in a later change.
 */
class SpatialTextSearch private constructor() {

	class SpatialTextSearchSettings private constructor() {

		///////////// GENERAL SETTINGS //////////

		var SEARCH_SUGGESTION = false // incomplete to add '.' in the end
		// not used in search as maps provided (web could multiply by 1.5x or adjust bbox)
		var SUGGESTED_SEARCH_RADIUS_KM = 400
		// "did you mean" the query with a misspelled word corrected (SpatialTypoSuggestions)
		var TYPO_SUGGESTION = true

		// lang to deduplicate results
		var LANG_DEDUPLICATE = ""

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
}
