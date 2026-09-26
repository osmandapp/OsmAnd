package net.osmand.shared.search.core

import co.touchlab.stately.concurrency.AtomicInt
import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.api.KStringMatcherMode.CHECK_EQUALS
import net.osmand.shared.api.KStringMatcherMode.CHECK_ONLY_STARTS_WITH
import net.osmand.shared.api.KStringMatcherMode.CHECK_STARTS_FROM_SPACE
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.CityBlocks
import net.osmand.shared.binary.CommonWords
import net.osmand.shared.binary.ObfConstants.isTagIndexedForSearchAsId
import net.osmand.shared.binary.ObfConstants.isTagIndexedForSearchAsName
import net.osmand.shared.binary.ObfConstants.isTagNonIndexedForSearchAsName
import net.osmand.shared.binary.PoiSubType
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.binary.SearchPoiAdditionalFilter
import net.osmand.shared.binary.SearchPoiTypeFilter
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.Amenity.Companion.POPULATION
import net.osmand.shared.data.Building
import net.osmand.shared.data.City
import net.osmand.shared.data.CityType
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.data.KQuadTree
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.map.WorldRegion
import net.osmand.shared.osm.AbstractPoiType
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.MapPoiTypes.Companion.OSM_WIKI_CATEGORY
import net.osmand.shared.osm.MapPoiTypes.Companion.WIKI_PLACE
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.osm.PoiType
import net.osmand.shared.search.SearchUICore.SearchResultMatcher
import net.osmand.shared.search.core.ObjectType.POI
import net.osmand.shared.search.core.SearchPhrase.SearchPhraseDataType
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollatorStringMatcher
import net.osmand.shared.util.KGeoParsedPoint
import net.osmand.shared.util.KGeoPointParserUtil
import net.osmand.shared.util.KLocationParser
import net.osmand.shared.util.KLocationParser.ParsedOpenLocationCode
import net.osmand.shared.util.KLocationParser.parseOpenLocationCode
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.KSearchAlgorithms.splitAndNormalize
import net.osmand.shared.util.PlatformUtil
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.math.abs

/**
 * The search apis: settlements, streets and houses by name, pois by name and by type, poi types,
 * streets of a settlement, houses and crossings of a street, and a location typed or pasted as a
 * link; with their constants and the cache of settlements the address search keeps.
 *
 * A copy of `SearchCoreFactory` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS. The statistics of the reader are not kept, so where java hands a result to the reader
 * and answers whether statistics are kept, the copy answers false, as java does without them.
 */
object SearchCoreFactory {

	const val PREFERRED_STREET_ZOOM = 17
	const val PREFERRED_INDEX_ITEM_ZOOM = 17
	const val PREFERRED_BUILDING_ZOOM = 16
	const val PREFERRED_COUNTRY_ZOOM = 7
	const val PREFERRED_CITY_ZOOM = 13
	const val PREFERRED_POI_ZOOM = 16
	const val PREFERRED_NEARBY_POINT_ZOOM = 16
	const val PREFERRED_WPT_ZOOM = 16
	const val PREFERRED_GPX_FILE_ZOOM = 17
	const val PREFERRED_DEFAULT_RECENT_ZOOM = 17
	const val PREFERRED_FAVORITES_GROUP_ZOOM = 17
	const val PREFERRED_FAVORITE_ZOOM = 16
	const val PREFERRED_STREET_INTERSECTION_ZOOM = 16
	const val PREFERRED_REGION_ZOOM = 6
	const val PREFERRED_DEFAULT_ZOOM = 15
	@JvmField
	var DISPLAY_DEFAULT_POI_TYPES = false
	const val MAX_DEFAULT_SEARCH_RADIUS = 7
	const val SEARCH_MAX_PRIORITY = Int.MAX_VALUE

	//////////////// CONSTANTS //////////
	const val SEARCH_REGION_API_PRIORITY = 300
	const val SEARCH_REGION_OBJECT_PRIORITY = 1000

	// context less
	const val SEARCH_LOCATION_PRIORITY = 0
	const val SEARCH_AMENITY_TYPE_PRIORITY = 100
	const val SEARCH_AMENITY_TYPE_API_PRIORITY = 100

	// context
	const val SEARCH_STREET_BY_CITY_PRIORITY = 200
	const val SEARCH_BUILDING_BY_CITY_PRIORITY = 300
	const val SEARCH_BUILDING_BY_STREET_PRIORITY = 100
	const val SEARCH_AMENITY_BY_TYPE_PRIORITY = 300

	// context less (slow)
	const val SEARCH_ADDRESS_BY_NAME_API_PRIORITY = 500
	const val SEARCH_ADDRESS_BY_NAME_API_PRIORITY_RADIUS2 = 500
	// results priority
	const val SEARCH_ADDRESS_BY_NAME_PRIORITY = 500
	const val SEARCH_ADDRESS_BY_NAME_PRIORITY_RADIUS2 = 500

	// context less (slower)
	const val SEARCH_AMENITY_BY_NAME_PRIORITY = 500
	// api priority
	const val SEARCH_AMENITY_BY_NAME_API_PRIORITY_IF_3_CHAR = 600
	const val SEARCH_ADDRESS_BY_NAME_LONG_API_PRIORITY = 700

	private const val SEARCH_ADDRESS_BY_NAME_CITY_PRIORITY_DISTANCE = 0.4
	private const val SEARCH_AMENITY_BY_NAME_CITY_PRIORITY_DISTANCE = 0.5
	private const val SEARCH_AMENITY_BY_NAME_TOWN_PRIORITY_DISTANCE = 0.7

	const val SEARCH_OLC_WITH_CITY_PRIORITY = 8
	const val SEARCH_OLC_WITH_CITY_TOTAL_LIMIT = 500

	abstract class SearchBaseAPI protected constructor(vararg searchTypes: ObjectType) : SearchCoreAPI {

		private val searchTypes: Array<out ObjectType> = searchTypes

		override fun isSearchAvailable(p: SearchPhrase): Boolean {
			val typesToSearch = p.getSearchTypes()
			val exclusiveSearchType = p.getExclusiveSearchType()
			if (exclusiveSearchType != null) {
				return searchTypes.size == 1 && searchTypes[0] == exclusiveSearchType
			} else if (typesToSearch == null) {
				return true
			} else {
				for (type in searchTypes) {
					for (ts in typesToSearch) {
						if (type == ts) {
							return true
						}
					}
				}
				return false
			}
		}

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			return true
		}

		override fun getSearchPriority(p: SearchPhrase): Int {
			return 1
		}

		override fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean {
			return phrase.getRadiusLevel() < MAX_DEFAULT_SEARCH_RADIUS
		}

		override fun getMinimalSearchRadius(phrase: SearchPhrase): Int {
			return 0
		}

		override fun getNextSearchRadius(phrase: SearchPhrase): Int {
			return 0
		}

		protected fun subSearchApiOrPublish(
			phrase: SearchPhrase, resultMatcher: SearchResultMatcher, res: SearchResult, api: SearchBaseAPI?
		): SearchPhrase? {
			return subSearchApiOrPublish(phrase, resultMatcher, res, api, null, true)
		}

		protected fun subSearchApiOrPublish(
			phrase: SearchPhrase, resultMatcher: SearchResultMatcher, res: SearchResult, api: SearchBaseAPI?,
			setParentSearchResult: SearchResult?, publish: Boolean
		): SearchPhrase? {
			phrase.countUnknownWordsMatchMainResult(res)
			var leftUnknownSearchWords = res.filterUnknownSearchWord(null)
			if (setParentSearchResult != null) {
				phrase.countUnknownWordsMatchMainResult(setParentSearchResult)
				leftUnknownSearchWords = setParentSearchResult.filterUnknownSearchWord(leftUnknownSearchWords)
			}
			// publish result to set parentSearchResult before search
			if (publish) {
				if (setParentSearchResult != null) {
					val prev = resultMatcher.setParentSearchResult(setParentSearchResult)
					resultMatcher.publish(res)
					resultMatcher.setParentSearchResult(prev)
				} else {
					resultMatcher.publish(res)
				}
			}
			if (!leftUnknownSearchWords.isEmpty() && api != null && api.isSearchAvailable(phrase)) {
				val nphrase = phrase.selectWord(
					res, leftUnknownSearchWords,
					phrase.isLastUnknownSearchWordComplete() ||
							!leftUnknownSearchWords.contains(phrase.getLastUnknownSearchWord())
				)
				val prev = resultMatcher.setParentSearchResult(
					if (publish) res else resultMatcher.getParentSearchResult()
				)
				api.search(nphrase, resultMatcher)

				resultMatcher.setParentSearchResult(prev)
				return nphrase
			}
			return null
		}

		internal fun matchAddressName(phrase: SearchPhrase, prevRes: SearchResult?, res: SearchResult, fullMatch: Boolean): Boolean {
			var match = false
			if (prevRes != null) {
				// remove braces to not count them
				val backup = prevRes.stripBracesNames()
				phrase.countUnknownWordsMatchMainResult(prevRes)
				prevRes.restoreBraceNames(backup)
				val leftUnknownSearchWords = prevRes.filterUnknownSearchWord(null)
				val nphrase = phrase.selectWord(
					prevRes, leftUnknownSearchWords,
					phrase.isLastUnknownSearchWordComplete()
							|| !leftUnknownSearchWords.contains(phrase.getLastUnknownSearchWord())
				)
//				NameStringMatcher unknownNameStringMatcher = nphrase.getMainUnknownNameStringMatcher();
				for (otherName in res.otherNames!!) {
					// for now do full equals (in future we could count with matcher
					// 	if (unknownNameStringMatcher.matches(otherName)) {
					if (phrase.getCollator().equals(nphrase.getUnknownWordToSearch(), otherName)) {
						return true
					}
				}
			}
			if (!phrase.isUnknownSearchWordPresent()) {
				return false
			}
			// res only used as container to count matches
//			phrase.countUnknownWordsMatchMainResult(res);

			val nm = phrase.getMainUnknownNameStringMatcher()
			val localeName = SearchPhrase.stripBraces(res.localeName)
			val otherNames = res.otherNames
			// quick check
			if (!fullMatch && (nm.matchesName(localeName) || nm.matches(otherNames))) {
				return true
			}
			val localeNames = SearchPhrase.splitWords(localeName, ArrayList(), SearchPhrase.ALLDELIMITERS)
			if (prevRes == null || !prevRes.firstUnknownWordMatches) {
				val it = localeNames.iterator()
				while (it.hasNext()) {
					val lName = it.next()
					if (phrase.getFirstUnknownNameStringMatcher().matches(lName)) {
						it.remove()
					}
				}
			}
			val leftUnknownSearchWords = if (prevRes == null) phrase.getUnknownSearchWords() else prevRes.filterUnknownSearchWord(null)
			val unknownSearchWords = phrase.getUnknownSearchWords()
			var i = 0
			while (i < unknownSearchWords.size && !match) {
				val leftUnknownSearchWord = unknownSearchWords[i]
				if (!leftUnknownSearchWords.contains(leftUnknownSearchWord)) {
					i++
					continue
				}
				val it = localeNames.iterator()
				while (it.hasNext()) {
					val lName = it.next()
					if (phrase.getUnknownNameStringMatcher(i).matches(lName)) {
						it.remove()
					}
				}
				i++
			}
			if (localeNames.size == 0) {
				return true
			}
			return false
		}

		override fun toString(): String {
			return this::class.simpleName ?: ""
		}
	}

	open class SearchRegionByNameAPI : SearchBaseAPI(ObjectType.REGION) {

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			for (bmir in phrase.getOfflineIndexes()) {
				if (bmir.getRegionCenter() != null) {
					val sr = SearchResult(phrase)
					sr.localeName = bmir.getRegionName()
					sr.`object` = bmir
					sr.file = bmir
					sr.priority = SEARCH_REGION_OBJECT_PRIORITY.toDouble()
					sr.objectType = ObjectType.REGION
					sr.location = bmir.getRegionCenter()
					sr.preferredZoom = PREFERRED_REGION_ZOOM
					if (phrase.getFullSearchPhrase().length <= 1 && phrase.isNoSelectedType()) {
						resultMatcher.publish(sr)
					} else if (phrase.getFirstUnknownNameStringMatcher().matchesName(sr.localeName)) {
						resultMatcher.publish(sr)
					}
				}
			}
			return true
		}

		override fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean {
			return false
		}

		override fun getSearchPriority(p: SearchPhrase): Int {
			if (!p.isNoSelectedType()) {
				return -1
			}
			return SEARCH_REGION_API_PRIORITY
		}
	}

	open class SearchAddressByNameAPI(
		private val streetsApi: SearchBuildingAndIntersectionsByStreetAPI,
		private val cityApi: SearchStreetByCityAPI,
		private val longDistance: Boolean,
		private val townCitiesCache: TownCitiesCache
	) : SearchBaseAPI(
		ObjectType.CITY, ObjectType.VILLAGE, ObjectType.BOUNDARY, ObjectType.POSTCODE,
		ObjectType.STREET, ObjectType.HOUSE, ObjectType.STREET_INTERSECTION
	) {

		private val MAX_ADRESS_RESULTS_BY_REGIONS = 1000

		override fun getSearchPriority(p: SearchPhrase): Int {
			if (!p.isNoSelectedType() && p.getRadiusLevel() == 1) {
				return -1
			}
			if (p.isLastWord(ObjectType.POI) || p.isLastWord(ObjectType.POI_TYPE)) {
				return -1
			}
			if (longDistance) {
				return SEARCH_ADDRESS_BY_NAME_LONG_API_PRIORITY
			}
			if (p.isNoSelectedType()) {
				return SEARCH_ADDRESS_BY_NAME_API_PRIORITY
			}
			return SEARCH_ADDRESS_BY_NAME_API_PRIORITY_RADIUS2
		}

		override fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean {
			// case when street is not found for given city is covered by SearchStreetByCityAPI
			return getSearchPriority(phrase) != -1 && super.isSearchMoreAvailable(phrase)
		}

		override fun getMinimalSearchRadius(phrase: SearchPhrase): Int {
			return phrase.getRadiusSearch(DEFAULT_ADDRESS_BBOX_RADIUS)
		}

		override fun getNextSearchRadius(phrase: SearchPhrase): Int {
			return phrase.getNextRadiusSearch(DEFAULT_ADDRESS_BBOX_RADIUS)
		}

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			if (!phrase.isUnknownSearchWordPresent() && !phrase.isEmptyQueryAllowed()) {
				return false
			}
			// phrase.isLastWord(ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE) || phrase.isLastWord(ObjectType.REGION)
			if (phrase.isNoSelectedType() || phrase.isLastWord(ObjectType.BOUNDARY, ObjectType.REGION)
				|| phrase.isLastWord(ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE)
				|| phrase.getRadiusLevel() >= 2
			) {
				initAndSearchCities(phrase, resultMatcher)
				// not publish results (let it sort)
				// resultMatcher.apiSearchFinished(this, phrase);
				searchByName(phrase, resultMatcher)
			}
			return true
		}

		private fun initAndSearchCities(phrase: SearchPhrase, resultMatcher: SearchResultMatcher) {
			val bbox: KQuadRect?
			val offlineIndexes: Iterator<BinaryMapIndexReader>
			val longRadius = LONG_ADDRESS_BBOX_RADIUS * 5
			val defRadius = DEFAULT_ADDRESS_BBOX_RADIUS * 5
			if (longDistance) {
				bbox = phrase.getRadiusBBoxToSearch(longRadius)
				offlineIndexes = phrase.getRadiusOfflineIndexes(defRadius, longRadius, SearchPhraseDataType.ADDRESS)
			} else {
				bbox = phrase.getRadiusBBoxToSearch(defRadius)
				offlineIndexes = phrase.getRadiusOfflineIndexes(0, defRadius, SearchPhraseDataType.ADDRESS)
			}
			while (offlineIndexes.hasNext()) {
				val r = offlineIndexes.next()
				if (!townCitiesCache.contains(r)) {
					var l = r.getCities(null, CityBlocks.CITY_TOWN_TYPE, null)
					townCitiesCache.add(r)
					for (c in l) {
						if (phrase.getSettings()!!.isExportObjects()) {
							resultMatcher.exportCity(phrase, c)
						}
						c.setReferenceFile(r)
						val cl = c.getLocation()!!

						val y = KMapUtils.get31TileNumberY(cl.latitude)
						val x = KMapUtils.get31TileNumberX(cl.longitude)
						var qr = KQuadRect(x.toDouble(), y.toDouble(), x.toDouble(), y.toDouble())
						val bbox31 = c.getBbox31()
						if (bbox31 != null) {
							qr = KQuadRect(bbox31[0].toDouble(), bbox31[1].toDouble(), bbox31[2].toDouble(), bbox31[3].toDouble())
						}
						townCitiesCache.insertCityQR(c, qr)
					}
					l = r.getCities(null, CityBlocks.BOUNDARY_TYPE, null)
					for (c in l) {
						if (phrase.getSettings()!!.isExportObjects()) {
							resultMatcher.exportCity(phrase, c)
						}
						c.setReferenceFile(r)
						val cl = c.getLocation()!!
						val y = KMapUtils.get31TileNumberY(cl.latitude)
						val x = KMapUtils.get31TileNumberX(cl.longitude)
						var qr = KQuadRect(x.toDouble(), y.toDouble(), x.toDouble(), y.toDouble())
						val bbox31 = c.getBbox31()
						if (bbox31 != null) {
							qr = KQuadRect(bbox31[0].toDouble(), bbox31[1].toDouble(), bbox31[2].toDouble(), bbox31[3].toDouble())
						}
						townCitiesCache.insertBoundaryQR(c, qr)
					}
				}
			}
			if (phrase.isNoSelectedType() && bbox != null
				&& (phrase.isUnknownSearchWordPresent() || phrase.isEmptyQueryAllowed())
				&& phrase.isSearchTypeAllowed(ObjectType.CITY)
			) {
				val nm = phrase.getMainUnknownNameStringMatcher()
				val cacheResArray = townCitiesCache.queryCities(bbox)
				var limit = 0
				for (c in cacheResArray) {
					val res = SearchResult(phrase)
					res.`object` = c
					res.file = c.getReferenceFile() as BinaryMapIndexReader?
					res.localeName = c.getName(phrase.getSettings()!!.getLang(), phrase.getSettings()!!.isTransliterate())
					res.otherNames = c.getOtherNames(true, res.localeName)
					res.localeRelatedObjectName = res.file!!.getRegionName()
					res.relatedObject = res.file
					res.location = c.getLocation()
					res.priority = SEARCH_ADDRESS_BY_NAME_PRIORITY.toDouble()
					res.priorityDistance = SEARCH_ADDRESS_BY_NAME_CITY_PRIORITY_DISTANCE
					res.objectType = ObjectType.CITY
					if (phrase.isEmptyQueryAllowed() && phrase.isEmpty()) {
						resultMatcher.publish(res)
					} else if (nm.matchesName(res.localeName) || nm.matches(res.otherNames)) {
						subSearchApiOrPublish(phrase, resultMatcher, res, cityApi)
						// No failed cases found yet - city / town should have exact boundary that street belongs to

						// // Require exact name matching to search street by name (not attached to city)
						// It will be very inefficient too
//						if (matchAddressName(phrase, null, res, true)) {
//							subSearchApiOrPublish(phrase, resultMatcher, res, this);
//						}
					}
					if (limit++ > LIMIT * phrase.getRadiusLevel()) {
						break
					}
				}
			}
		}

		internal fun hasNonNumericLeftUnknownSearchWord(res: SearchResult): Boolean {
			for (leftUnknownSearchWord in res.filterUnknownSearchWord(null)) {
				if (!KSearchAlgorithms.isNumber2Letters(leftUnknownSearchWord)) {
					return true
				}
			}
			return false
		}

		private fun searchByName(phrase: SearchPhrase, resultMatcher: SearchResultMatcher) {
			if (phrase.getRadiusLevel() > 1 || phrase.getUnknownWordToSearch().length > 3 ||
				phrase.hasMoreThanOneUnknownSearchWord() || phrase.isSearchTypeAllowed(ObjectType.POSTCODE, true)
			) {
				val locSpecified = phrase.getLastTokenLocation() != null
				val loc = phrase.getLastTokenLocation()
				val immediateResults: MutableList<SearchResult> = ArrayList()
				val searchRadius = if (longDistance) LONG_ADDRESS_BBOX_RADIUS else DEFAULT_ADDRESS_BBOX_RADIUS
				val postcodeBbox = phrase.getRadiusBBoxToSearch(searchRadius * 5)
				val villagesBbox = phrase.getRadiusBBoxToSearch(searchRadius * 3)
				val cityBbox = phrase.getRadiusBBoxToSearch(searchRadius * 5) // covered by separate radius before
				val priority = if (phrase.isNoSelectedType())
					SEARCH_ADDRESS_BY_NAME_PRIORITY else SEARCH_ADDRESS_BY_NAME_PRIORITY_RADIUS2
				val currentFile = arrayOfNulls<BinaryMapIndexReader>(1)

				val rm = object : ResultMatcher<MapObject> {
					var limit = 0

					override fun publish(obj: MapObject): Boolean {
						if (isCancelled()) {
							return false
						}
						val settings = phrase.getSettings()!!
						val sr = SearchResult(phrase)
						sr.`object` = obj
						sr.file = currentFile[0]
						sr.localeName = obj.getName(settings.getLang(), settings.isTransliterate())
						sr.otherNames = obj.getOtherNames(true, sr.localeName)
						sr.localeRelatedObjectName = sr.file!!.getRegionName()
						sr.relatedObject = sr.file
						sr.location = obj.getLocation()
						sr.priorityDistance = 1.0
						sr.priority = priority.toDouble()
						val y = KMapUtils.get31TileNumberY(obj.getLocation()!!.latitude)
						val x = KMapUtils.get31TileNumberX(obj.getLocation()!!.longitude)
						var closestCities: List<City>? = null
						if (obj is Street) {
							// remove limitation by location
							if (  //(locSpecified && !streetBbox.contains(x, y, x, y)) ||
								!phrase.isSearchTypeAllowed(ObjectType.STREET)
							) {
								return false
							}
							if (obj.getName().startsWith("<")) {
								return false
							}
							sr.objectType = ObjectType.STREET
							sr.localeRelatedObjectName = obj.getCity()!!.getName(settings.getLang(), settings.isTransliterate())
							sr.relatedObject = obj.getCity()
						} else if (obj is City && !isLastWordCityGroup(phrase)) {
							val type = obj.getType()
							if (type == CityType.CITY || type == CityType.TOWN) {
								if (phrase.isNoSelectedType()) {
									// ignore city/town
									return false
								}
								if ((locSpecified && !contains(cityBbox!!, x, y))
									|| !phrase.isSearchTypeAllowed(ObjectType.CITY)
								) {
									return false
								}
								sr.objectType = ObjectType.CITY
								sr.priorityDistance = SEARCH_ADDRESS_BY_NAME_CITY_PRIORITY_DISTANCE
							} else if (type == CityType.POSTCODE) {
								if ((locSpecified && !contains(postcodeBbox!!, x, y))
									|| !phrase.isSearchTypeAllowed(ObjectType.POSTCODE)
								) {
									return false
								}
								sr.objectType = ObjectType.POSTCODE
								sr.priorityDistance = 0.0
							} else if (type == CityType.BOUNDARY) {
								if ((locSpecified && !contains(villagesBbox!!, x, y))
									|| !phrase.isSearchTypeAllowed(ObjectType.BOUNDARY)
								) {
									return false
								}
								sr.objectType = ObjectType.BOUNDARY
								sr.priorityDistance = 0.0
								phrase.countUnknownWordsMatchMainResult(sr)
							} else if (type == CityType.HAMLET || type == CityType.SUBURB ||
								type == CityType.VILLAGE
							) {
								if ((locSpecified && !contains(villagesBbox!!, x, y))
									|| !phrase.isSearchTypeAllowed(ObjectType.VILLAGE)
								) {
									return false
								}
								var closestCity: City? = null
								if (closestCities == null) {
									// cities are loaded only from files near the user: a village from a file beyond that keeps the region name
									closestCities = if (townCitiesCache.contains(currentFile[0]!!))
										townCitiesCache.queryCities(villagesBbox!!) else emptyList()
								}
								var minDist = -1.0
								var pDist = -1.0
								for (city in closestCities) {
									val ll = KMapUtils.getDistance(city.getLocation()!!, obj.getLocation()!!)
									val pd = if (city.getType() == CityType.CITY) ll else ll * 10
									if (minDist == -1.0 || pd < pDist) {
										closestCity = city
										minDist = ll
										pDist = pd
									}
								}
								if (closestCity != null) {
									sr.localeRelatedObjectName = closestCity.getName(settings.getLang(), settings.isTransliterate())
									sr.relatedObject = closestCity
									sr.distRelatedObjectName = minDist
								}
								sr.objectType = ObjectType.VILLAGE
							}
						} else {
							return false
						}
						limit++
						immediateResults.add(sr)
						// java: whether statistics are kept
						return false
					}

					override fun isCancelled(): Boolean {
						return limit > LIMIT * phrase.getRadiusLevel() ||
								resultMatcher.isCancelled()
					}
				}

				var rawDataCollector: ResultMatcher<MapObject>? = null
				if (phrase.getSettings()!!.isExportObjects()) {
					rawDataCollector = object : ResultMatcher<MapObject> {
						override fun publish(obj: MapObject): Boolean {
							resultMatcher.exportObject(phrase, obj)
							return true
						}

						override fun isCancelled(): Boolean {
							return false
						}
					}
				}

				val lastWord = phrase.getLastSelectedWord()
				var minRadius = 0
				var maxRadius = DEFAULT_ADDRESS_BBOX_RADIUS * 5
				if (longDistance) {
					minRadius = maxRadius
					maxRadius = LONG_ADDRESS_BBOX_RADIUS * 5
				}
				var offlineIterator = phrase.getRadiusOfflineIndexes(minRadius, maxRadius, SearchPhraseDataType.ADDRESS)
				var wordToSearch = phrase.getUnknownWordToSearch()
				val wordToSearchSplit = splitAndNormalize(wordToSearch, true)
				if (wordToSearchSplit.size > 1) {
					wordToSearch = SearchPhrase.selectMainUnknownWordToSearch(ArrayList(wordToSearchSplit))
				}
				val req = SearchRequest.buildAddressByNameRequest(
					rm, rawDataCollector, wordToSearch.lowercase(),
					if (phrase.isMainUnknownSearchWordComplete()) KStringMatcherMode.CHECK_EQUALS_FROM_SPACE
					else KStringMatcherMode.CHECK_STARTS_FROM_SPACE
				)
				if (locSpecified) {
					val rect: KQuadRect
					val c = lastWord?.getResult()?.`object`
					if (c is City) {
						val x31 = KMapUtils.get31TileNumberX(c.getLocation()!!.longitude)
						val y31 = KMapUtils.get31TileNumberY(c.getLocation()!!.latitude)
						val bb = c.getBbox31()
						if (bb != null && bb.size >= 4) {
							val w = (bb[2] - bb[0]) / 3
							val h = (bb[3] - bb[1]) / 3 // enlarge for 1234 Golden Pond Road Woodhull
							val left = bb[0] - w
							val top = bb[1] - h
							val right = bb[2] + w
							val bottom = bb[3] + h
							rect = KQuadRect(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
							req.setBBox(x31, y31, left, top, right, bottom)
						} else {
							val radius = c.getType().getRadius().toInt() * 3
							rect = KMapUtils.calculate31BboxUsingRhumb(radius, c.getLocation()!!)
							req.setBBoxRadius(c.getLocation()!!.latitude, c.getLocation()!!.longitude, radius)
						}
					} else {
						val radius = phrase.getRadiusSearch(maxRadius)
						rect = KMapUtils.calculate31BboxUsingRhumb(radius, loc!!)
						req.setBBoxRadius(loc.latitude, loc.longitude, radius)
					}
					offlineIterator = phrase.getOfflineIndexes(rect, SearchPhraseDataType.ADDRESS)
				}

				var lastRegionPriority = 0
				val lastResultCount = resultMatcher.getCount()
				while (offlineIterator.hasNext() && wordToSearch.length > 0) {
					val r = offlineIterator.next()
					currentFile[0] = r
					immediateResults.clear()

					r.searchAddressDataByName(req)
					for (res in immediateResults) {
						if (res.objectType == ObjectType.STREET) {
							var newParentSearchResult: SearchResult? = null
							val street = res.`object`
							if (res.parentSearchResult == null && resultMatcher.getParentSearchResult() == null &&
								street is Street && street.getCity() != null
							) {
								val ct = street.getCity()!!
								val settings = phrase.getSettings()!!
								val cityResult = SearchResult(phrase)
								cityResult.`object` = ct
								cityResult.objectType = ObjectType.CITY
								cityResult.localeName = ct.getName(settings.getLang(), settings.isTransliterate())
								cityResult.otherNames = ct.getOtherNames(true)
								cityResult.location = ct.getLocation()
								cityResult.localeRelatedObjectName = res.file!!.getRegionName()
								cityResult.file = res.file
								// include parent search result even if it is empty
								// for street-city don't require exact matching
								val match = matchAddressName(phrase, res, cityResult, true)
								if (match) {
									newParentSearchResult = cityResult
								} else if (hasNonNumericLeftUnknownSearchWord(res)) { // speed up
									val bbox = KMapUtils.calculate31BboxUsingRhumb(1000, res.location!!)
									val cacheResArray = townCitiesCache.queryBoundaries(bbox)
									for (boundary in cacheResArray) {
										val bb = boundary.getBbox31() ?: continue
										val boundBox = KQuadRect(bb[0].toDouble(), bb[1].toDouble(), bb[2].toDouble(), bb[3].toDouble())
										if (!KQuadRect.intersects(boundBox, bbox)) {
											continue
										}
										// cityResult.object = boundary; // keep city the same
										cityResult.localeName = boundary.getName(settings.getLang(), settings.isTransliterate())
										cityResult.otherNames = boundary.getOtherNames(true)
										// for another city require exact matching
										if (matchAddressName(phrase, res, cityResult, true)) {
											cityResult.`object` = boundary
											cityResult.location = boundary.getLocation()
											newParentSearchResult = cityResult
											break
										}
									}
								}

							}
							subSearchApiOrPublish(phrase, resultMatcher, res, streetsApi, newParentSearchResult, true)
						} else if (res.objectType == ObjectType.BOUNDARY) {
							// require exact matching to speed up
							if (matchAddressName(phrase, null, res, true)) {
								subSearchApiOrPublish(phrase, resultMatcher, res, this)
							}
						} else {

							subSearchApiOrPublish(phrase, resultMatcher, res, cityApi)
							// if subsearch by cityApi we could avoid calling subsearch by boundary
							// but it's tricky to check how good matching results (case Hohlmaier 1 Breuningsweiler)

							// require exact matching to search street by name (not attached to city)
							if (matchAddressName(phrase, null, res, true)) {
								subSearchApiOrPublish(phrase, resultMatcher, res, this)
							}
						}
					}
					resultMatcher.apiSearchRegionFinished(this, r, phrase)
					val regionPriority = phrase.getRegionPriority(r)
					val cnt = resultMatcher.getCount() - lastResultCount
					if (cnt > MAX_ADRESS_RESULTS_BY_REGIONS && regionPriority > lastRegionPriority) {
						break
					}
					lastRegionPriority = regionPriority
				}
			}
		}

		private companion object {
			private const val DEFAULT_ADDRESS_BBOX_RADIUS = 40 * 1000
			private const val LONG_ADDRESS_BBOX_RADIUS = 100 * 1000
			private const val LIMIT = 10000
		}
	}


	open class SearchAmenityByNameAPI : SearchBaseAPI(ObjectType.POI) {

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			if (!phrase.isUnknownSearchWordPresent()) {
				return false
			}
			if (!phrase.isNoSelectedType()) {
				// don't search by name when type is selected or poi type is part of name
				return false
			}
			// Take into account POI [bar] - 'Hospital 512'
			// BEFORE: it was searching exact match of whole phrase.getUnknownSearchPhrase() [ Check feedback ]

			val currentFile = arrayOfNulls<BinaryMapIndexReader>(1)
			// ?LONG? 2 radiuses !!!
			val offlineIterator = phrase.getRadiusOfflineIndexes(
				BBOX_RADIUS,
				SearchPhraseDataType.POI
			)
			val searchWord = phrase.getUnknownWordToSearch()
			val nm = phrase.getMainUnknownNameStringMatcher()
			val bbox = (if (phrase.getFileRequest() != null) phrase.getRadiusBBoxToSearch(BBOX_RADIUS_POI_IN_CITY) else phrase.getRadiusBBoxToSearch(BBOX_RADIUS_INSIDE))!!
			val ids: MutableSet<String> = HashSet()

			var rawDataCollector: ResultMatcher<Amenity>? = null
			if (phrase.getSettings()!!.isExportObjects()) {
				rawDataCollector = object : ResultMatcher<Amenity> {
					override fun publish(obj: Amenity): Boolean {
						resultMatcher.exportObject(phrase, obj)
						return true
					}

					override fun isCancelled(): Boolean {
						return false
					}
				}
			}
			val matcher = object : ResultMatcher<Amenity> {
				var limit = 0

				override fun publish(obj: Amenity): Boolean {
					val settings = phrase.getSettings()!!
					if (settings.isExportObjects()) {
						resultMatcher.exportObject(phrase, obj)
					}
					if (limit++ > LIMIT) {
						return false
					}
					val poiID = obj.getType()!!.getKeyName() + "_" + obj.getId()
					if (ids.contains(poiID)) {
						return false
					}
					val sr = SearchResult(phrase)
					sr.localeName = obj.getName(settings.getLang(), settings.isTransliterate())
					sr.otherNames = obj.getOtherNames(true, sr.localeName)
					var matchLocalName = nm.matchesName(sr.localeName)
					if (!matchLocalName) {
						sr.localeName = obj.getName(settings.getLang(), settings.isTransliterate())
						matchLocalName = nm.matchesName(sr.localeName)
					}
					if (!matchLocalName && !nm.matches(sr.otherNames)) {
						for (k in obj.getAdditionalInfoKeys()) {
							if ((isTagIndexedForSearchAsName(k) ||
										isTagNonIndexedForSearchAsName(k) ||
										isTagIndexedForSearchAsId(k))
								&& nm.matchesName(obj.getAdditionalInfo(k))
							) {
								sr.alternateName = obj.getAdditionalInfo(k)
								break
							}
						}
						if (KAlgorithms.isEmpty(sr.alternateName)) {
							return false
						}
					}

					sr.`object` = obj
					sr.preferredZoom = PREFERRED_POI_ZOOM
					sr.file = currentFile[0]
					sr.location = obj.getLocation()
					if (obj.getSubType()!! == "city" || obj.getSubType()!! == "country") {
						sr.priorityDistance = SEARCH_AMENITY_BY_NAME_CITY_PRIORITY_DISTANCE
						sr.preferredZoom = if (obj.getSubType()!! == "country") PREFERRED_COUNTRY_ZOOM else PREFERRED_CITY_ZOOM
					} else if (obj.getSubType()!! == "town") {
						sr.priorityDistance = SEARCH_AMENITY_BY_NAME_TOWN_PRIORITY_DISTANCE
					} else {
						sr.priorityDistance = 1.0
					}
					sr.priority = SEARCH_AMENITY_BY_NAME_PRIORITY.toDouble()
					phrase.countUnknownWordsMatchMainResult(sr)
					sr.cityName = obj.getCityFromTagGroups(settings.getLang())
					sr.objectType = ObjectType.POI
					resultMatcher.publish(sr)
					ids.add(poiID)
					// java: whether statistics are kept
					return false
				}

				override fun isCancelled(): Boolean {
					return resultMatcher.isCancelled() && (limit < LIMIT)
				}
			}

			// java's overload of these arguments passes no raw data collector on
			val req = SearchRequest.buildSearchPoiRequest(
				bbox.centerX().toInt(), bbox.centerY().toInt(), searchWord,
				bbox.left.toInt(), bbox.right.toInt(), bbox.top.toInt(), bbox.bottom.toInt(),
				null, matcher, null
			)
//			req.setMatcherMode(nm.getStringMatcher().getMode()); // enable it once tested

			val reqUnlimited = SearchRequest.buildSearchPoiRequest(
				bbox.centerX().toInt(), bbox.centerY().toInt(), searchWord,
				0, Int.MAX_VALUE, 0, Int.MAX_VALUE,
				null, matcher, null
			)
//			reqUnlimited.setMatcherMode(nm.getStringMatcher().getMode()); // enable it once tested

			val fileRequest = phrase.getFileRequest()
			if (fileRequest != null) {
				fileRequest.searchPoiByName(req)
				resultMatcher.apiSearchRegionFinished(this, fileRequest, phrase)
			} else {
				var lastRegionPriority = 0
				val lastResultCount = resultMatcher.getCount()
				while (offlineIterator.hasNext()) {
					val r = offlineIterator.next()
					currentFile[0] = r
					r.searchPoiByName(if (r.isBasemap()) reqUnlimited else req)
					resultMatcher.apiSearchRegionFinished(this, r, phrase)
					val regionPriority = phrase.getRegionPriority(r)
					val cnt = resultMatcher.getCount() - lastResultCount
					if (cnt > MAX_POI_RESULTS_BY_REGIONS && regionPriority > lastRegionPriority) {
						break
					}
					lastRegionPriority = regionPriority
				}
			}
			return true
		}

		override fun getSearchPriority(p: SearchPhrase): Int {
			if (p.hasObjectType(ObjectType.POI) ||
				!p.isUnknownSearchWordPresent()
			) {
				return -1
			}
			if (p.hasObjectType(ObjectType.POI_TYPE)) {
				return -1
			}
			if (p.getUnknownWordToSearch().length >= FIRST_WORD_MIN_LENGTH || p.isFirstUnknownSearchWordComplete()) {
				return SEARCH_AMENITY_BY_NAME_API_PRIORITY_IF_3_CHAR
			}
			return -1
		}

		override fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean {
			return super.isSearchMoreAvailable(phrase) && getSearchPriority(phrase) != -1
		}

		override fun getMinimalSearchRadius(phrase: SearchPhrase): Int {
			return phrase.getRadiusSearch(BBOX_RADIUS)
		}

		override fun getNextSearchRadius(phrase: SearchPhrase): Int {
			return phrase.getNextRadiusSearch(BBOX_RADIUS)
		}

		private companion object {
			private const val LIMIT = 10000
			private const val MAX_POI_RESULTS_BY_REGIONS = 1000
			private const val BBOX_RADIUS = 500 * 1000
			private const val BBOX_RADIUS_INSIDE = 5600 * 1000 // 5600 is the minimum to pass test [14: hisar]
			private const val BBOX_RADIUS_POI_IN_CITY = 25 * 1000
			private const val FIRST_WORD_MIN_LENGTH = 3
		}
	}

	open class PoiTypeResult {
		@JvmField
		var pt: AbstractPoiType? = null
		@JvmField
		var foundWords: MutableSet<String> = LinkedHashSet()
	}

	open class SearchAmenityTypesAPI(private val types: MapPoiTypes) : SearchBaseAPI(ObjectType.POI_TYPE) {

		private var translatedNames: Map<String, PoiType> = LinkedHashMap()
		private var topVisibleFilters: MutableList<AbstractPoiType>? = null
		private var categories: List<PoiCategory>? = null
		private val customPoiFilters: MutableList<CustomSearchPoiFilter> = ArrayList()
		private val activePoiFilters: MutableMap<String?, Int> = HashMap()
		private val poiAdditionalTopIndexCache: MutableMap<BinaryMapIndexReader, MutableSet<String>> = HashMap()

		fun clearCustomFilters() {
			this.customPoiFilters.clear()
			this.activePoiFilters.clear()
		}

		fun addCustomFilter(poiFilter: CustomSearchPoiFilter, priority: Int) {
			this.customPoiFilters.add(poiFilter)
			if (priority > 0) {
				this.activePoiFilters[poiFilter.getFilterId()] = priority
			}
		}

		fun setActivePoiFiltersByOrder(filterOrder: List<String>) {
			for (i in filterOrder.indices) {
				this.activePoiFilters[filterOrder[i]] = i
			}
		}

		fun getPoiTypeResults(nm: NameStringMatcher, nmAdditional: NameStringMatcher?): MutableMap<String, PoiTypeResult> {
			val results: MutableMap<String, PoiTypeResult> = LinkedHashMap()
			for (pf in topVisibleFilters!!) {
				val res = checkPoiType(nm, pf)
				if (res != null) {
					results[res.pt!!.getKeyName()] = res
				}
			}
			// don't spam results with unsearchable additionals like 'description', 'email', ...
			// if (nmAdditional != null) {
			//	addAditonals(nmAdditional, results, types.getOtherMapCategory());
			// }
			for (c in categories!!) {
				val res = checkPoiType(nm, c)
				if (res != null) {
					results[res.pt!!.getKeyName()] = res
				}
				if (nmAdditional != null) {
					addAditonals(nmAdditional, results, c)
				}
				for (pf in c.getPoiFilters()) {
					val filtRes = checkPoiType(nm, pf)
					if (filtRes != null) {
						results[filtRes.pt!!.getKeyName()] = filtRes
					}
				}
			}
			val additionals: MutableMap<String, PoiTypeResult> = LinkedHashMap()
			val it = translatedNames.entries.iterator()
			while (it.hasNext()) {
				val e = it.next()
				val pt = e.value
				if (pt.getCategory() !== types.getOtherMapCategory() && !pt.isReference()) {
					val res = checkPoiType(nm, pt)
					if (res != null) {
						results[res.pt!!.getKeyName()] = res
					}
					if (nmAdditional != null) {
						addAditonals(nmAdditional, additionals, pt)
					}
				}
			}
			results.putAll(additionals) // results ordered by: top, categories, types, additional
			return results
		}

		private fun addAditonals(nm: NameStringMatcher, results: MutableMap<String, PoiTypeResult>, pt: AbstractPoiType) {
			val additionals = pt.getPoiAdditionals()
			for (a in additionals) {
				if (a.getReferenceType() != null) {
					// ignore reference types as duplicates
					continue
				}
				val existingResult = results[a.getKeyName()]
				if (existingResult != null) {
					val f: PoiAdditionalCustomFilter
					val existing = existingResult.pt
					if (existing is PoiAdditionalCustomFilter) {
						f = existing
					} else {
						f = PoiAdditionalCustomFilter(types, existing as PoiType)
					}
					if (!f.additionalPoiTypes.contains(a)) {
						f.additionalPoiTypes.add(a)
					}
					existingResult.pt = f
				} else {
					val enTranslation = a.getEnTranslation().lowercase()
					if ("no" != enTranslation) {
						val ptr = checkPoiType(nm, a)
						if (ptr != null && ptr.pt != null && ptr.pt!!.isTopVisible()) {
							results[a.getKeyName()] = ptr
						}
					}
				}
			}
		}

		private fun checkPoiType(nm: NameStringMatcher, pf: AbstractPoiType): PoiTypeResult? {
			var res: PoiTypeResult? = null
			if (nm.matches(pf.getTranslation())) {
				res = addIfMatch(nm, pf.getTranslation(), pf, res)
			}
			if (nm.matches(pf.getEnTranslation())) {
				res = addIfMatch(nm, pf.getEnTranslation(), pf, res)
			}
			if (nm.matches(pf.getKeyName())) {
				res = addIfMatch(nm, pf.getKeyName().replace('_', ' '), pf, res)
			}

			if (nm.matches(pf.getSynonyms())) {
				val synonyms = WorldRegion.splitLikeJava(pf.getSynonyms(), ";")
				for (synonym in synonyms) {
					res = addIfMatch(nm, synonym, pf, res)
				}
			}
			return res
		}

		private fun addIfMatch(nm: NameStringMatcher, s: String, pf: AbstractPoiType, res: PoiTypeResult?): PoiTypeResult? {
			var res = res
			if (nm.matches(s)) {
				if (res == null) {
					res = PoiTypeResult()
					res.pt = pf
				}
				res.foundWords.add(s)

			}
			return res
		}

		internal fun initPoiTypes() {
			if (translatedNames.isEmpty()) {
				translatedNames = types.getAllTranslatedNames(false)
				val topVisibleFilters = types.getTopVisibleFilters().toMutableList()
				this.topVisibleFilters = topVisibleFilters
				types.getOsmwiki()?.let { topVisibleFilters.remove(it) }
				categories = types.getCategories(false)

				if (DISPLAY_DEFAULT_POI_TYPES) {
					val order: MutableList<String> = ArrayList()
					for (p in topVisibleFilters) {
						order.add(getStandardFilterId(p))
					}
					val nearestPois = object : CustomSearchPoiFilter {

						override fun isEmpty(): Boolean {
							return false
						}

						override fun accept(type: PoiCategory?, subcategory: String): Boolean {
							return true
						}

						override fun wrapResultMatcher(matcher: ResultMatcher<Amenity>): ResultMatcher<Amenity> {
							return matcher
						}

						override fun getName(): String {
							return "Neareset POIs"
						}

						override fun getIconResource(): Any? {
							return null
						}

						override fun getFilterId(): String {
							return "nearest_pois"
						}
					}
					setActivePoiFiltersByOrder(order)
					addCustomFilter(nearestPois, 100)
				}
			}
		}

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			val showTopFiltersOnly = !phrase.isUnknownSearchWordPresent()
			val nm = phrase.getFirstUnknownNameStringMatcher()

			initPoiTypes()
			if (showTopFiltersOnly) {
				for (pt in topVisibleFilters!!) {
					val res = SearchResult(phrase)
					res.localeName = pt.getTranslation()
					res.`object` = pt
					addPoiTypeResult(phrase, resultMatcher, showTopFiltersOnly, getStandardFilterId(pt), res)
				}

			} else {
				val includeAdditional = !phrase.hasMoreThanOneUnknownSearchWord()
				val nmAdditional = if (includeAdditional)
					NameStringMatcher(phrase.getFirstUnknownSearchWord(), KStringMatcherMode.CHECK_EQUALS_FROM_SPACE) else null
				var poiTypes = getPoiTypeResults(nm, nmAdditional)
				poiTypes = filterTypes(poiTypes)
				val wikiCategory = poiTypes[OSM_WIKI_CATEGORY]
				val wikiType = poiTypes[WIKI_PLACE]
				if (wikiCategory != null && wikiType != null) {
					poiTypes.remove(WIKI_PLACE)
				}
				for (ptr in poiTypes.values) {
					var match = !phrase.isFirstUnknownSearchWordComplete()
					if (!match) {
						for (foundName in ptr.foundWords) {
							val csm = KCollatorStringMatcher(foundName, CHECK_ONLY_STARTS_WITH)
							match = csm.matches(phrase.getUnknownSearchPhrase())
							if (match) {
								break
							}
						}
					}
					if (match) {
						val res = SearchResult(phrase)
						if (OSM_WIKI_CATEGORY == ptr.pt!!.getKeyName()) {
							res.localeName = ptr.pt!!.getTranslation() + " (" + types.getAllLanguagesTranslationSuffix() + ")"
						} else {
							res.localeName = ptr.pt!!.getTranslation()
						}
						res.`object` = ptr.pt
						addPoiTypeResult(
							phrase, resultMatcher, showTopFiltersOnly, getStandardFilterId(ptr.pt!!),
							res
						)
					}
				}
			}
			for (i in customPoiFilters.indices) {
				val csf = customPoiFilters[i]
				if (showTopFiltersOnly || nm.matchesName(csf.getName())) {
					val res = SearchResult(phrase)
					res.localeName = csf.getName()
					res.`object` = csf
					addPoiTypeResult(phrase, resultMatcher, showTopFiltersOnly, csf.getFilterId(), res)
				}
			}
			searchTopIndexPoiAdditional(phrase, resultMatcher)
			return true
		}

		// filter out types that are not in the category
		private fun filterTypes(poiTypes: Map<String, PoiTypeResult>): MutableMap<String, PoiTypeResult> {
			val filtered: MutableMap<String, PoiTypeResult> = LinkedHashMap()
			for (ptr in poiTypes.values) {
				val pt = ptr.pt
				if (pt is PoiAdditionalCustomFilter) {
					if (pt.getPoiAdditionalCategory() != null) {
						filtered[pt.getKeyName()] = ptr
					} else {
						pt.additionalPoiTypes.forEach { t ->
							if (t.getPoiAdditionalCategory() != null) {
								filtered[ptr.pt!!.getKeyName()] = ptr
							}
						}
					}
				} else {
					filtered[ptr.pt!!.getKeyName()] = ptr
				}
			}
			return filtered
		}

		private fun addPoiTypeResult(
			phrase: SearchPhrase, resultMatcher: SearchResultMatcher, showTopFiltersOnly: Boolean,
			stdFilterId: String?, res: SearchResult
		) {
			res.priorityDistance = 0.0
			res.objectType = ObjectType.POI_TYPE
			res.firstUnknownWordMatches = true
			if (showTopFiltersOnly) {
				if (activePoiFilters.containsKey(stdFilterId)) {
					res.priority = getPoiTypePriority(stdFilterId).toDouble()
					resultMatcher.publish(res)
				}
			} else {
				phrase.countUnknownWordsMatchMainResult(res)
				res.priority = SEARCH_AMENITY_TYPE_PRIORITY.toDouble()
				resultMatcher.publish(res)
			}
		}

		private fun getPoiTypePriority(stdFilterId: String?): Int {
			val i = activePoiFilters[stdFilterId] ?: return SEARCH_AMENITY_TYPE_PRIORITY
			return SEARCH_AMENITY_TYPE_PRIORITY + i
		}


		fun getStandardFilterId(poi: AbstractPoiType): String {
			return STD_POI_FILTER_PREFIX + poi.getKeyName()
		}

		override fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean {
			return false
		}

		override fun getSearchPriority(p: SearchPhrase): Int {
			if (p.hasObjectType(ObjectType.POI) || p.hasObjectType(ObjectType.POI_TYPE)) {
				return -1
			}
			if (!p.isNoSelectedType() && !p.isUnknownSearchWordPresent()) {
				return -1
			}
			val lastSelectedWord = p.getLastSelectedWord()
			if (lastSelectedWord != null && ObjectType.isAddress(lastSelectedWord.getType())) {
				return -1
			}
			return SEARCH_AMENITY_TYPE_API_PRIORITY
		}

		private fun initPoiAdditionalTopIndex(r: BinaryMapIndexReader) {
			if (poiAdditionalTopIndexCache.containsKey(r)) {
				return
			}
			val poiSubTypes = r.getTopIndexSubTypes()
			if (poiSubTypes.size == 0) {
				return
			}
			val names: MutableSet<String> = HashSet()
			for (subType in poiSubTypes) {
				val possibleValues = subType.possibleValues ?: continue
				names.addAll(possibleValues)
			}
			val translation: MutableList<String> = ArrayList()
			for (v in names) {
				val translate = getTopIndexTranslation(v)
				translation.add(translate)
			}
			names.addAll(translation)
			if (names.size > 0) {
				poiAdditionalTopIndexCache[r] = names
			}
		}

		fun searchTopIndexPoiAdditional(phrase: SearchPhrase, resultMatcher: SearchResultMatcher) {
			if (phrase.isEmpty()) {
				return
			}
			val offlineIndexes = phrase.getRadiusOfflineIndexes(BBOX_RADIUS, SearchPhraseDataType.POI)
			val nm = phrase.getMainUnknownNameStringMatcher()
			val matchedValues: MutableMap<String?, HashSet<String>> = HashMap()
			while (offlineIndexes.hasNext()) {
				val r = offlineIndexes.next()
				initPoiAdditionalTopIndex(r)
				if (!poiAdditionalTopIndexCache.containsKey(r)) {
					continue
				}
				if (nm.matches(poiAdditionalTopIndexCache[r])) {
					val match = matchTopIndex(r, phrase)
					if (match != null) {
						if (matchedValues.containsKey(match.subType.name) && matchedValues[match.subType.name]!!.contains(match.value)) {
							continue
						}
						val res = SearchResult(phrase)
						res.localeName = match.translatedValue
						res.`object` = TopIndexFilter(match.subType, types, match.value)
						addPoiTypeResult(phrase, resultMatcher, false, null, res)
						val values = matchedValues.getOrPut(match.subType.name) { HashSet() }
						values.add(match.value)
					}
				}
			}
		}

		private fun matchTopIndex(r: BinaryMapIndexReader, phrase: SearchPhrase): TopIndexMatch? {
			val search = phrase.getUnknownSearchPhrase()
			val complete = phrase.isFirstUnknownSearchWordComplete()
			val poiSubTypes = r.getTopIndexSubTypes()
			val lang = phrase.getSettings()!!.getLang()
			val matches: MutableList<TopIndexMatch> = ArrayList()
			val nm = NameStringMatcher(search, CHECK_ONLY_STARTS_WITH)
			for (subType in poiSubTypes) {
				var topIndexValue: String? = null
				var translate: String? = null
				val possibleValues: MutableList<String> = ArrayList(subType.possibleValues!!)
				possibleValues.sort()
				for (s in possibleValues) {
					translate = getTopIndexTranslation(s)
					val normalizeBrand = s.lowercase()
					if (complete) {
						if (KCollatorStringMatcher.cmatches(search, normalizeBrand, CHECK_ONLY_STARTS_WITH)) {
							topIndexValue = s
							break
						} else {
							if (KCollatorStringMatcher.cmatches(search, translate, CHECK_ONLY_STARTS_WITH)) {
								topIndexValue = s
								break
							}
						}
					} else if (nm.matches(s) || nm.matches(translate)) {
						topIndexValue = s
						break
					}
				}
				if (topIndexValue != null) {
					val topIndexMatch = TopIndexMatch(subType, topIndexValue, translate!!)
					if (!KAlgorithms.isEmpty(lang) && subType.name!!.contains(":$lang")) {
						return topIndexMatch
					}
					matches.add(topIndexMatch)
				}
			}
			for (m in matches) {
				if (!m.subType.name!!.contains(":")) {
					return m
				}
			}
			if (matches.size > 0) {
				return matches[0]
			}
			return null
		}

		private fun getTopIndexTranslation(value: String): String {
			val key = TopIndexFilter.getValueKey(value)
			var translate = types.getPoiTranslation(key)!!
			if (translate.lowercase() == key) {
				translate = value
			}
			return translate
		}

		companion object {
			const val STD_POI_FILTER_PREFIX = "std_"
			private const val BBOX_RADIUS = 10000
		}
	}

	open class SearchAmenityByTypeAPI(
		private val types: MapPoiTypes,
		private val searchAmenityTypesAPI: SearchAmenityTypesAPI?
	) : SearchBaseAPI(ObjectType.POI) {
		private var unselectedPoiType: AbstractPoiType? = null
		private var nameFilter: String? = null

		fun getUnselectedPoiType(): AbstractPoiType? {
			return unselectedPoiType
		}

		fun getNameFilter(): String? {
			return nameFilter
		}

		override fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean {
			return getSearchPriority(phrase) != -1 && super.isSearchMoreAvailable(phrase)
		}

		override fun getMinimalSearchRadius(phrase: SearchPhrase): Int {
			return phrase.getRadiusSearch(BBOX_RADIUS)
		}

		override fun getNextSearchRadius(phrase: SearchPhrase): Int {
			return phrase.getNextRadiusSearch(BBOX_RADIUS)
		}

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			unselectedPoiType = null
			var poiTypeFilter: SearchPoiTypeFilter? = null
			var poiAdditionalFilter: SearchPoiAdditionalFilter? = null
			var nameFilter: String? = null
			var countExtraWords = 0
			val poiAdditionals: MutableSet<String> = LinkedHashSet()
			if (phrase.isLastWord(ObjectType.POI_TYPE)) {
				val obj = phrase.getLastSelectedWord()!!.getResult()!!.`object`
				if (obj is AbstractPoiType) {
					poiTypeFilter = getPoiTypeFilter(obj, poiAdditionals)
				} else if (obj is SearchPoiTypeFilter) {
					poiTypeFilter = obj
				} else if (obj is SearchPoiAdditionalFilter) {
					poiTypeFilter = null
					poiAdditionalFilter = obj
				} else {
					throw UnsupportedOperationException()
				}
				nameFilter = phrase.getUnknownSearchPhrase()
			} else if (searchAmenityTypesAPI != null && phrase.isNoSelectedType() && phrase.getFirstUnknownSearchWord().length > 1) {
				val nm = phrase.getFirstUnknownNameStringMatcher()
				val nmAdditional = NameStringMatcher(phrase.getFirstUnknownSearchWord(), KStringMatcherMode.CHECK_EQUALS_FROM_SPACE)
				searchAmenityTypesAPI.initPoiTypes()
				val poiTypeResults = searchAmenityTypesAPI.getPoiTypeResults(nm, nmAdditional)
				// find first full match only
				for (poiTypeResult in poiTypeResults.values) {
					for (foundName in poiTypeResult.foundWords) {
						val csm = KCollatorStringMatcher(foundName, CHECK_ONLY_STARTS_WITH)
						// matches only completely
						val mwords = SearchPhrase.countWords(foundName)
						if (csm.matches(phrase.getUnknownSearchPhrase()) && countExtraWords < mwords) {
							countExtraWords = SearchPhrase.countWords(foundName)
							val otherSearchWords = phrase.getUnknownSearchWords()
							nameFilter = null
							if (countExtraWords - 1 < otherSearchWords.size) {
								nameFilter = ""
								for (k in countExtraWords - 1 until otherSearchWords.size) {
									if (nameFilter!!.length > 0) {
										nameFilter += SearchPhrase.DELIMITER
									}
									nameFilter += otherSearchWords[k]
								}
							}
							poiTypeFilter = getPoiTypeFilter(poiTypeResult.pt!!, poiAdditionals)
							unselectedPoiType = poiTypeResult.pt
							val wordsInPoiType = SearchPhrase.countWords(foundName)
							val wordsInUnknownPart = SearchPhrase.countWords(phrase.getUnknownSearchPhrase())
							if (wordsInPoiType == wordsInUnknownPart) {
								// store only perfect match
								phrase.setUnselectedPoiType(unselectedPoiType)
							}
						}
					}
				}
			}
			this.nameFilter = nameFilter
			if (poiTypeFilter != null || poiAdditionalFilter != null) {
				var radius = BBOX_RADIUS
				if (poiTypeFilter != null && phrase.getRadiusLevel() == 1 && poiTypeFilter is CustomSearchPoiFilter) {
					val name = poiTypeFilter.getFilterId()
					if ("std_null" == name) {
						radius = BBOX_RADIUS_NEAREST
					}
				}
				val bbox = phrase.getRadiusBBoxToSearch(radius)!!
				val offlineIndexes = phrase.getOfflineIndexes()
				// java's TreeSet, only asked whether it holds an id
				val searchedPois: MutableSet<String> = HashSet()
				for (r in offlineIndexes) {
					var rm = getResultMatcher(
						phrase, poiTypeFilter, resultMatcher, nameFilter, r,
						searchedPois, poiAdditionals, countExtraWords
					)
					if (poiTypeFilter is CustomSearchPoiFilter) {
						rm = poiTypeFilter.wrapResultMatcher(rm)
					}
					val req = SearchRequest.buildSearchPoiRequest(
						bbox.left.toInt(),
						bbox.right.toInt(), bbox.top.toInt(), bbox.bottom.toInt(), -1, poiTypeFilter, poiAdditionalFilter, rm
					)
					r.searchPoi(req)
					resultMatcher.apiSearchRegionFinished(this, r, phrase)
				}
			}
			return true
		}


		private fun getResultMatcher(
			phrase: SearchPhrase, poiTypeFilter: SearchPoiTypeFilter?,
			resultMatcher: SearchResultMatcher, nameFilter: String?,
			selected: BinaryMapIndexReader, searchedPois: MutableSet<String>,
			poiAdditionals: Collection<String>, countExtraWords: Int
		): ResultMatcher<Amenity> {


			val ns = if (nameFilter == null) null else NameStringMatcher(nameFilter, CHECK_STARTS_FROM_SPACE)
			return object : ResultMatcher<Amenity> {

				override fun publish(obj: Amenity): Boolean {
					val settings = phrase.getSettings()!!
					if (settings.isExportObjects()) {
						resultMatcher.exportObject(phrase, obj)
					}
					val res = SearchResult(phrase)
					val poiID = obj.getType()!!.getKeyName() + "_" + obj.getId()
					if (!searchedPois.add(poiID)) {
						return false
					}
					if (obj.isClosed()) {
						return false
					}
					if (!phrase.isAcceptPrivate() && obj.isPrivateAccess()) {
						return false
					}
					if (!poiAdditionals.isEmpty()) {
						var found = false
						for (add in poiAdditionals) {
							if (obj.getAdditionalInfoKeys().contains(add)) {
								found = true
								break
							}
						}
						if (!found) {
							return false
						}
					}
					res.localeName = obj.getName(settings.getLang(), settings.isTransliterate())
					res.otherNames = obj.getOtherNames(true, res.localeName)

					if (KAlgorithms.isEmpty(res.localeName)) {
						if (obj.isRouteTrack()) {
							res.localeName = obj.getAdditionalInfo(Amenity.ROUTE_ID)
						} else if (obj.isRouteArticle()) {
							res.localeName = getMapObjectName(obj, settings)
						}
					}
					if (KAlgorithms.isEmpty(res.localeName)) {
						val st = anyPoiTypeByKey(types, obj.getSubType())
						if (st != null) {
							res.localeName = st.getTranslation()
						} else {
							res.localeName = obj.getSubType()
						}
					}
					if (ns != null) {
						if (ns.matchesName(res.localeName) || ns.matches(res.otherNames)) {
							phrase.countUnknownWordsMatchMainResult(res, countExtraWords)
						} else {
							// Use ref https://github.com/osmandapp/OsmAnd/issues/8319
							val ref = obj.getTagContent(Amenity.REF, null)
							if (ref == null || !ns.matches(ref)) {
								return false
							} else {
								phrase.countUnknownWordsMatchMainResult(res, ref, countExtraWords)
								res.localeName += " $ref"
							}
						}
					} else {
						phrase.countUnknownWordsMatchMainResult(res, countExtraWords)
					}

					res.`object` = obj
					res.cityName = obj.getCityFromTagGroups(settings.getLang())
					res.preferredZoom = PREFERRED_POI_ZOOM
					res.file = selected
					res.location = obj.getLocation()
					res.priority = SEARCH_AMENITY_BY_TYPE_PRIORITY.toDouble()
					res.priorityDistance = 1.0
					res.objectType = POI
					resultMatcher.publish(res)
					return false
				}

				override fun isCancelled(): Boolean {
					return resultMatcher.isCancelled()
				}
			}
		}

		fun getPoiTypeFilter(pt: AbstractPoiType, poiAdditionals: MutableSet<String>): SearchPoiTypeFilter {
			val acceptedTypes: MutableMap<PoiCategory, LinkedHashSet<String>?> = LinkedHashMap()
			pt.putTypes(acceptedTypes)
			poiAdditionals.clear()
			if (pt.isAdditional()) {
				poiAdditionals.add(pt.getKeyName())
			}
			return object : SearchPoiTypeFilter {

				override fun isEmpty(): Boolean {
					return false
				}

				override fun accept(type: PoiCategory?, subcategory: String): Boolean {
					var type = type ?: return true
					if (!types.isRegisteredType(type)) {
						// java asks for a null category too, which it has not
						type = types.getOtherPoiCategory() ?: return false
					}
					if (!acceptedTypes.containsKey(type)) {
						return false
					}
					val set = acceptedTypes[type] ?: return true
					return set.contains(subcategory)
				}
			}
		}

		override fun getSearchPriority(p: SearchPhrase): Int {
			if ((p.isLastWord(ObjectType.POI_TYPE) && p.getLastTokenLocation() != null)
				|| (p.isNoSelectedType())
			) {
				return SEARCH_AMENITY_BY_TYPE_PRIORITY
			}
			return -1
		}

		private companion object {
			private const val BBOX_RADIUS = 10000
			private const val BBOX_RADIUS_NEAREST = 1000
		}
	}

	open class SearchStreetByCityAPI(
		private val streetsAPI: SearchBuildingAndIntersectionsByStreetAPI
	) : SearchBaseAPI(ObjectType.HOUSE, ObjectType.STREET, ObjectType.STREET_INTERSECTION) {

		override fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean {
			// case when street is not found for given city is covered here
			return phrase.getRadiusLevel() == 1 && getSearchPriority(phrase) != -1
		}

		override fun getMinimalSearchRadius(phrase: SearchPhrase): Int {
			return phrase.getRadiusSearch(DEFAULT_ADDRESS_BBOX_RADIUS)
		}

		override fun getNextSearchRadius(phrase: SearchPhrase): Int {
			return phrase.getNextRadiusSearch(DEFAULT_ADDRESS_BBOX_RADIUS)
		}

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			val sw = phrase.getLastSelectedWord()
			if (isLastWordCityGroup(phrase) && sw!!.getResult() != null && sw.getResult()!!.file != null) {
				val settings = phrase.getSettings()!!
				val c = sw.getResult()!!.`object` as City
				if (c.getStreets().isEmpty()) {
					sw.getResult()!!.file!!.preloadStreets(c, null)
				}
				var limit = 0
				for (obj in c.getStreets()) {
					val res = SearchResult(phrase)
					res.localeName = obj.getName(settings.getLang(), settings.isTransliterate())
					res.otherNames = obj.getOtherNames(true, res.localeName)
					res.`object` = obj
					var pub = true
					if (obj.getName().startsWith("<")) {
						// streets related to city
						pub = false
					} else if (phrase.isUnknownSearchWordPresent() && !matchAddressName(phrase, null, res, false)) {
						continue
					}
					res.localeRelatedObjectName = c.getName(settings.getLang(), settings.isTransliterate())
					res.preferredZoom = PREFERRED_STREET_ZOOM
					res.file = sw.getResult()!!.file
					res.location = obj.getLocation()
					res.priority = SEARCH_STREET_BY_CITY_PRIORITY.toDouble()
					//res.priorityDistance = 1;
					res.objectType = ObjectType.STREET
					subSearchApiOrPublish(phrase, resultMatcher, res, streetsAPI, null, pub)
					if (limit++ > LIMIT) {
						break
					}

				}
				return true
			}
			return true
		}

		override fun getSearchPriority(p: SearchPhrase): Int {
			if (isLastWordCityGroup(p)) {
				return SEARCH_STREET_BY_CITY_PRIORITY
			}
			return -1
		}

		private companion object {
			private const val DEFAULT_ADDRESS_BBOX_RADIUS = 100 * 1000
			private const val LIMIT = 10000
		}
	}

	open class SearchBuildingAndIntersectionsByStreetAPI : SearchBaseAPI(ObjectType.HOUSE, ObjectType.STREET_INTERSECTION) {
		internal var cacheBuilding: Street? = null

		override fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean {
			return false
		}

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			var s: Street? = null
			val commonWords = CommonWords.getInstance()
			var priority = SEARCH_BUILDING_BY_STREET_PRIORITY
			if (phrase.isLastWord(ObjectType.STREET)) {
				s = phrase.getLastSelectedWord()!!.getResult()!!.`object` as Street?
			}
			if (isLastWordCityGroup(phrase)) {
				priority = SEARCH_BUILDING_BY_CITY_PRIORITY
				val o = phrase.getLastSelectedWord()!!.getResult()!!.`object`
				if (o is City) {
					val streets = o.getStreets()
					if (streets.size == 1) {
						s = streets[0]
					} else {
						for (st in streets) {
							if (st.getName() == o.getName() ||
								st.getName() == "<" + o.getName() + ">"
							) {
								s = st
								break
							}
						}
					}
				}
			}

			if (s != null) {
				val settings = phrase.getSettings()!!
				val file = phrase.getLastSelectedWord()!!.getResult()!!.file

				if (cacheBuilding !== s) {
					cacheBuilding = s
					if (s.getBuildings().isEmpty() && s.getIntersectedStreets().isEmpty()) {
						val sr = SearchRequest.buildAddressRequest(object : ResultMatcher<Building> {
							override fun publish(obj: Building): Boolean {
								return true
							}

							override fun isCancelled(): Boolean {
								return resultMatcher.isCancelled()
							}
						})
						file!!.preloadBuildings(s, sr)
					}
					s.getBuildings().sortWith(object : Comparator<Building> {
						override fun compare(o1: Building, o2: Building): Int {
							val i1 = KAlgorithms.extractFirstIntegerNumber(o1.getName())
							val i2 = KAlgorithms.extractFirstIntegerNumber(o2.getName())
							if (i1 == i2) {
								return 0
							}
							return i1.compareTo(i2)
						}
					})
				}
				val lw = phrase.getUnknownWordToSearchBuilding()
				val buildingMatch = phrase.getUnknownWordToSearchBuildingNameMatcher()
				val startMatch = NameStringMatcher(lw, CHECK_ONLY_STARTS_WITH)
				val number = KAlgorithms.extractFirstIntegerNumber(lw)

				if (phrase.isSearchTypeAllowed(ObjectType.HOUSE)) {
					for (b in s.getBuildings()) {
						val res = SearchResult(phrase)
						var interpolation = false
						if (b.belongsToInterpolation(lw)) {
							interpolation = true
						} else if (number > 0 && number == KAlgorithms.extractFirstIntegerNumber(b.getName()) &&
							lw.startsWith(b.getName().lowercase())
						) {
							// match by partial name
						} else if (buildingMatch.matches(b.getName())) {
							// match by name
						} else {
							continue
						}
						if (interpolation) {
							res.localeName = lw
							res.location = b.getLocation(b.interpolation(lw))
						} else {
							res.localeName = b.getName(settings.getLang(), settings.isTransliterate())
							res.location = b.getLocation()
						}
						res.otherNames = b.getOtherNames(true, res.localeName)
						res.`object` = b
						res.file = file
						res.priority = priority.toDouble()
						res.priorityDistance = 0.0
						res.firstUnknownWordMatches = startMatch.matchesName(res.localeName)
						// phrase.countUnknownWordsMatchMainResult(res); // same as above
						res.relatedObject = s
						res.localeRelatedObjectName = s.getName(settings.getLang(), settings.isTransliterate())
						res.objectType = ObjectType.HOUSE
						res.preferredZoom = PREFERRED_BUILDING_ZOOM

						resultMatcher.publish(res)
					}
				}
				val streetIntersection = phrase.getUnknownWordToSearch()
				if (KAlgorithms.isEmpty(streetIntersection) ||
					(!streetIntersection[0].isDigit() &&
							commonWords.getCommonSearch(streetIntersection) == -1) &&
					phrase.isSearchTypeAllowed(ObjectType.STREET_INTERSECTION)
				) {
					for (street in s.getIntersectedStreets()) {
						val res = SearchResult(phrase)
						res.otherNames = street.getOtherNames(true)
						res.localeName = street.getName(settings.getLang(), settings.isTransliterate())
						res.`object` = street
						if (!matchAddressName(phrase, null, res, false)) {
							continue
						}
						res.file = file
						res.relatedObject = s
						res.priority = (priority + 1).toDouble()
						res.localeRelatedObjectName = s.getName(settings.getLang(), settings.isTransliterate())
						res.priorityDistance = 0.0
						res.objectType = ObjectType.STREET_INTERSECTION
						res.location = street.getLocation()
						res.preferredZoom = PREFERRED_STREET_INTERSECTION_ZOOM
						phrase.countUnknownWordsMatchMainResult(res)
						resultMatcher.publish(res)
					}
				}
			}
			return true
		}

		override fun getSearchPriority(p: SearchPhrase): Int {
			if (isLastWordCityGroup(p)) {
				return SEARCH_BUILDING_BY_CITY_PRIORITY
			}
			if (!p.isLastWord(ObjectType.STREET)) {
				return -1
			}
			return SEARCH_BUILDING_BY_STREET_PRIORITY
		}
	}

	open class TownCitiesCache {
		private val townCitiesInit: MutableSet<String> = LinkedHashSet()
		private val townCitiesQR = KQuadTree<City>(
			KQuadRect(0.0, 0.0, Int.MAX_VALUE.toDouble(), Int.MAX_VALUE.toDouble()),
			12, 0.55f
		)
		private val boundariesQR = KQuadTree<City>(
			KQuadRect(0.0, 0.0, Int.MAX_VALUE.toDouble(), Int.MAX_VALUE.toDouble()),
			12, 0.55f
		)

		fun contains(reader: BinaryMapIndexReader): Boolean {
			return townCitiesInit.contains(getKey(reader))
		}

		fun add(reader: BinaryMapIndexReader) {
			townCitiesInit.add(getKey(reader))
		}

		// files could share the same region name (ex. old combined German state maps are all "Germany"),
		// so cities must be cached per file, otherwise only the first file of the region is loaded
		private fun getKey(reader: BinaryMapIndexReader): String {
			val name = reader.getFile().absolutePath()
			return name + "_" + reader.getDateCreated()
		}

		fun insertCityQR(c: City, r: KQuadRect) {
			townCitiesQR.insert(c, r)
		}

		fun insertBoundaryQR(c: City, r: KQuadRect) {
			boundariesQR.insert(c, r)
		}

		fun queryCities(bbox: KQuadRect): List<City> {
			val cacheResArray: MutableList<City> = ArrayList()
			townCitiesQR.queryInBox(bbox, cacheResArray)
			return cacheResArray
		}

		fun queryBoundaries(bbox: KQuadRect): List<City> {
			val cacheResArray: MutableList<City> = ArrayList()
			boundariesQR.queryInBox(bbox, cacheResArray)
			return cacheResArray
		}
	}

	open class PoiAdditionalCustomFilter(registry: MapPoiTypes, pt: PoiType) : AbstractPoiType(pt.getKeyName(), registry) {

		private val poiType: PoiType = pt
		@JvmField
		var additionalPoiTypes: MutableList<PoiType> = ArrayList()

		init {
			additionalPoiTypes.add(pt)
		}

		override fun isAdditional(): Boolean {
			return true
		}

		override fun putTypes(
			acceptedTypes: MutableMap<PoiCategory, LinkedHashSet<String>?>
		): MutableMap<PoiCategory, LinkedHashSet<String>?> {
			for (p in additionalPoiTypes) {
				if (p.getParentType() === registry.getOtherMapCategory()) {
					for (c in registry.getCategories(false)) {
						c.putTypes(acceptedTypes)
					}
				} else {
					p.getParentType()!!.putTypes(acceptedTypes)
				}

			}
			return acceptedTypes
		}

		override fun getParentTypeName(): String {
			return poiType.getParentTypeName()
		}

		override fun equals(other: Any?): Boolean {
			if (super.equals(other)) {
				if (other !is PoiAdditionalCustomFilter) {
					return false
				}
				return this.additionalPoiTypes == other.additionalPoiTypes
			}
			return false
		}

	}

	open class SearchLocationAndUrlAPI(
		private val amenitiesApi: SearchAmenityByNameAPI,
		internetConnectionAvailable: (() -> Boolean)?
	) : SearchBaseAPI(ObjectType.LOCATION, ObjectType.PARTIAL_LOCATION) {

		private var olcPhraseHash = 0
		private var olcPhraseLocation: KLatLon? = null
		private var cachedParsedCode: ParsedOpenLocationCode? = null
//		private final List<String> citySubTypes = Arrays.asList("city", "town", "village");

		private val internetConnectionAvailable: () -> Boolean = internetConnectionAvailable
			?: DEFAULT_INTERNET_CONNECTION_AVAILABLE

		constructor(amenitiesApi: SearchAmenityByNameAPI) : this(amenitiesApi, DEFAULT_INTERNET_CONNECTION_AVAILABLE)

		override fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean {
			return false
		}

		override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			if (!phrase.isUnknownSearchWordPresent()) {
				return false
			}
			val parseUrl = parseUrl(phrase, resultMatcher)
			if (!parseUrl) {
				parseLocation(phrase, resultMatcher)
			}
			return super.search(phrase, resultMatcher)
		}

		internal fun parsePartialLocation(s: String): KLatLon? {
			val s = s.trim { it <= ' ' }
			if (s.length == 0 || !(s[0] == '-' || s[0].isDigit()
						|| s[0] == 'S' || s[0] == 's'
						|| s[0] == 'N' || s[0] == 'n'
						|| s.contains("://"))
			) {
				return null
			}
			val partial = booleanArrayOf(false)
			val d: MutableList<Double> = ArrayList()
			val all: MutableList<Any> = ArrayList()
			val strings: MutableList<String> = ArrayList()
			KLocationParser.splitObjects(s, d, all, strings, partial)
			if (partial[0]) {
				val lat = KLocationParser.parse1Coordinate(all, 0, all.size)
				return KLatLon(lat, 0.0)
			}
			return null
		}

		private fun parseLocation(phrase: SearchPhrase, resultMatcher: SearchResultMatcher) {
			val lw = phrase.getUnknownSearchPhrase()
			// Detect OLC
			var parsedCode = cachedParsedCode
			if (parsedCode == null) {
				parsedCode = parseOpenLocationCode(lw)
			}
			if (parsedCode != null) {
				var latLon = parsedCode.getLatLon()
				// do we have local code with locality
				if (!parsedCode.isFull() && !KAlgorithms.isEmpty(parsedCode.getPlaceName())) {
					val cityLocation = searchOLCLocation(phrase, resultMatcher)
					if (cityLocation != null) {
						latLon = parsedCode.recover(cityLocation)
					}
				}
				if (latLon == null && !parsedCode.isFull()) {
					latLon = parsedCode.recover(phrase.getSettings()!!.getOriginalLocation()!!)
				}
				if (latLon != null) {
					publishLocation(phrase, resultMatcher, lw, latLon)
				}
			} else {
				val l = KLocationParser.parseLocation(lw)
				if (l != null && phrase.isSearchTypeAllowed(ObjectType.LOCATION)) {
					publishLocation(phrase, resultMatcher, lw, l)
				} else if (l == null && phrase.isNoSelectedType() && phrase.isSearchTypeAllowed(ObjectType.PARTIAL_LOCATION)) {
					val ll = parsePartialLocation(lw)
					if (ll != null) {
						val sp = SearchResult(phrase)
						sp.priority = SEARCH_LOCATION_PRIORITY.toDouble()

						sp.location = ll
						sp.`object` = sp.location
						sp.localeName = formatLatLon(sp.location!!.latitude) + ", <input> "
						sp.objectType = ObjectType.PARTIAL_LOCATION
						resultMatcher.publish(sp)
					}
				}
			}
		}

		private fun searchOLCLocation(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): KLatLon? {
			val unknownWords = phrase.getUnknownSearchWords()
			val text = if (!unknownWords.isEmpty()) unknownWords[0] else phrase.getUnknownWordToSearch()

			val allowedTypes = listOf("village", "town", "city") // ascending priority
			val searchBBox31 = KQuadRect(0.0, 0.0, Int.MAX_VALUE.toDouble(), Int.MAX_VALUE.toDouble())
			val nm = NameStringMatcher(text, CHECK_STARTS_FROM_SPACE)
			val lang = phrase.getSettings()!!.getLang()
			val transliterate = phrase.getSettings()!!.isTransliterate()

			var settings = phrase.getSettings()!!.setSearchBBox31(searchBBox31)
			settings = settings.setSortByName(false)
			settings = settings.setEmptyQueryAllowed(true)

			val olcPhrase = phrase.generateNewPhrase(text, settings)
			val result: MutableList<SearchResult> = ArrayList()

			val matcher = object : ResultMatcher<SearchResult> {
				var count = 0

				override fun publish(obj: SearchResult): Boolean {
					if (count > SEARCH_OLC_WITH_CITY_TOTAL_LIMIT) {
						return false
					}
					var amenity: Amenity? = null
					if (obj.objectType == POI) {
						amenity = obj.`object` as Amenity
					}

					if (amenity == null) {
						return false
					}

					val subType = amenity.getSubType()
					val localeName = amenity.getName(lang, transliterate)
					val otherNames = obj.otherNames

					if (!allowedTypes.contains(subType) || (!nm.matches(localeName) && !nm.matches(otherNames))) {
						return false
					}
					result.add(obj)
					count++
					return true
				}

				override fun isCancelled(): Boolean {
					return count > SEARCH_OLC_WITH_CITY_TOTAL_LIMIT || resultMatcher.isCancelled()
				}
			}

			val rm = SearchResultMatcher(matcher, olcPhrase, 0, AtomicInt(0), SEARCH_OLC_WITH_CITY_TOTAL_LIMIT)
			amenitiesApi.search(olcPhrase, rm)

			val nmEquals = NameStringMatcher(text, CHECK_EQUALS)

			result.sortWith(object : Comparator<SearchResult> {
				override fun compare(sr1: SearchResult, sr2: SearchResult): Int {
					if (sr1.objectType != POI || sr2.objectType != POI) {
						return 0
					}
					val a1 = sr1.`object` as Amenity
					val a2 = sr2.`object` as Amenity

					val i1 = getIndex(a1)
					val i2 = getIndex(a2)
					val priorityDiff = i2.compareTo(i1)
					if (priorityDiff != 0) {
						return priorityDiff
					}
					val p1 = a1.getAdditionalInfo(POPULATION)
					val p2 = a2.getAdditionalInfo(POPULATION)
					val pop1 = KAlgorithms.parseLongSilently(p1, -1)
					val pop2 = KAlgorithms.parseLongSilently(p2, -1)

					return pop2.compareTo(pop1) // descending order
				}

				private fun getIndex(poi: Amenity): Int {
					var res = 0
					val poiTypeIndex = allowedTypes.indexOf(poi.getSubType())
					if (poiTypeIndex != -1) {
						res += poiTypeIndex
						if (nmEquals.matches(poi.getName()) || nmEquals.matches(poi.getOtherNames())) {
							res += SEARCH_OLC_WITH_CITY_PRIORITY
						}
					}
					return res
				}
			})

			return if (!result.isEmpty()) result[0].location else null
		}


		private fun publishLocation(phrase: SearchPhrase, resultMatcher: SearchResultMatcher, lw: String, l: KLatLon) {
			val sp = SearchResult(phrase)
			sp.priority = SEARCH_LOCATION_PRIORITY.toDouble()
			sp.location = l
			sp.`object` = sp.location
			sp.localeName = formatLatLon(sp.location!!.latitude) + ", " + formatLatLon(sp.location!!.longitude)
			sp.objectType = ObjectType.LOCATION
			sp.wordsSpan = lw
			resultMatcher.publish(sp)
		}

		private fun parseUrl(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
			val lines = phrase.getUnknownSearchPhrase().replace("\r\n", "\n")

			var pnt: KGeoParsedPoint? = null
			for (text in WorldRegion.splitLikeJava(lines, "\n")) {
				pnt = KGeoPointParserUtil.parse(text)
				if (pnt == null && KGeoPointParserUtil.isGooGlUrl(text)) {
					val resolvedUrl = resolveRedirectUrl(text)
					if (resolvedUrl != null) {
						pnt = KGeoPointParserUtil.parse(resolvedUrl)
					}
				}
				if (pnt != null) {
					break
				}
			}

			if (pnt != null && pnt.isGeoPoint() && phrase.isSearchTypeAllowed(ObjectType.LOCATION)) {
				val sp = SearchResult(phrase)
				sp.priority = 0.0
				sp.`object` = pnt
				sp.wordsSpan = lines
				sp.setImpreciseCoordinates(pnt.hasImpreciseCoordinates())
				sp.location = KLatLon(pnt.getLatitude(), pnt.getLongitude())
				sp.localeName = formatLatLon(pnt.getLatitude()) + ", " + formatLatLon(pnt.getLongitude())
				if (pnt.getZoom() > 0) {
					sp.preferredZoom = pnt.getZoom()
				}
				sp.objectType = ObjectType.LOCATION
				resultMatcher.publish(sp)
				return true
			}
			return false
		}

		private fun resolveRedirectUrl(url: String): String? {
			val uri = KGeoPointParserUtil.createUri(url)
			if (uri != null && internetConnectionAvailable()) {
				return PlatformUtil.getNetworkAPI().resolveRedirectUrl(uri.original)
			}
			return null
		}

		override fun getSearchPriority(p: SearchPhrase): Int {
			if (!p.isNoSelectedType() || !p.isUnknownSearchWordPresent()) {
				return -1
			}
			var olcPhraseHash = p.getUnknownSearchPhrase().hashCode()
			if (this.olcPhraseHash == olcPhraseHash && this.olcPhraseLocation != null) {
				val distance = KMapUtils.getDistance(p.getSettings()!!.getOriginalLocation()!!, this.olcPhraseLocation!!)
				if (distance > OLC_RECALC_DISTANCE_THRESHOLD) {
					olcPhraseHash++
				}
			}
			if (this.olcPhraseHash != olcPhraseHash) {
				this.olcPhraseHash = olcPhraseHash
				this.olcPhraseLocation = p.getSettings()!!.getOriginalLocation()
				cachedParsedCode = parseOpenLocationCode(p.getUnknownSearchPhrase())
			}
			return SEARCH_LOCATION_PRIORITY
		}

//		private boolean isSearchDone(SearchPhrase phrase) {
//			return cachedParsedCode != null;
//		}

		/**
		 * What java's `DecimalFormat("0.0####")` writes in the US locale: one to five fraction
		 * digits, no grouping, half even rounding of the exact value of [latLon]. The JDK rounds
		 * the double nearest to 0.000005 down, though it is above it; this rounds it up.
		 */
		internal fun formatLatLon(latLon: Double): String {
			if (latLon.isNaN()) {
				return "NaN"
			}
			if (latLon.isInfinite()) {
				return if (latLon > 0) "\u221E" else "-\u221E"
			}
			val value = abs(latLon)
			// the digits of the text of the value, and where its point is among them
			val text = value.toString()
			val e = text.indexOfFirst { it == 'E' || it == 'e' }
			val mantissa = if (e >= 0) text.substring(0, e) else text
			val dot = mantissa.indexOf('.')
			var digits = if (dot >= 0) mantissa.substring(0, dot) + mantissa.substring(dot + 1) else mantissa
			var point = (if (dot >= 0) dot else mantissa.length) + (if (e >= 0) text.substring(e + 1).toInt() else 0)
			val lead = digits.indexOfFirst { it != '0' }
			if (lead < 0) {
				digits = ""
				point = 0
			} else {
				digits = digits.substring(lead)
				point -= lead
			}
			// the integer part and five fraction digits stay
			val keep = point + 5
			val units = StringBuilder()
			for (i in 0 until keep) {
				units.append(if (i < digits.length) digits[i] else '0')
			}
			if (keep >= 0 && keep < digits.length) {
				val first = digits[keep]
				val up = when {
					first > '5' -> true
					first < '5' -> false
					digits.substring(keep + 1).any { it != '0' } -> true
					else -> {
						// the text is half way: the exact value decides, and a tie goes to the even digit
						val c = compareToMillionths(value, digits.substring(0, keep + 1).toLong())
						c > 0 || (c == 0 && units.isNotEmpty() && (units[units.length - 1] - '0') % 2 == 1)
					}
				}
				if (up) {
					var i = units.length - 1
					while (i >= 0 && units[i] == '9') {
						units[i] = '0'
						i--
					}
					if (i >= 0) {
						units[i] = units[i] + 1
					} else {
						units.insert(0, '1')
					}
				}
			}
			while (units.length < 6) {
				units.insert(0, '0')
			}
			val integer = units.substring(0, units.length - 5).trimStart('0').ifEmpty { "0" }
			val fraction = units.substring(units.length - 5).trimEnd('0').ifEmpty { "0" }
			// the sign stays on -0.0 and on what rounds to zero
			return (if (latLon.toRawBits() < 0) "-" else "") + integer + "." + fraction
		}

		private companion object {
			private const val OLC_RECALC_DISTANCE_THRESHOLD = 100000 // 100 km
			private val DEFAULT_INTERNET_CONNECTION_AVAILABLE: () -> Boolean = { true }

			/** The sign of [value] - [n] / 10^6, exactly, for a positive finite [value]. */
			private fun compareToMillionths(value: Double, n: Long): Int {
				val bits = value.toRawBits()
				val exponent = ((bits ushr 52) and 0x7FF).toInt()
				val fraction = bits and 0xFFFFFFFFFFFFFL
				val m = if (exponent == 0) fraction else fraction or (1L shl 52)
				// value * 10^6 = m * 15625 * 2^shift
				val shift = (if (exponent == 0) -1074 else exponent - 1075) + 6
				if (shift >= 0) {
					// m * 15625 alone is past any n
					return 1
				}
				val lo = ((m and 0xFFFFFFFFL) * 15625).toULong()
				val hi = ((m ushr 32) * 15625).toULong()
				val xLo = (hi shl 32) + lo
				val xHi = (hi shr 32) + (if (xLo < lo) 1uL else 0uL)
				val t = -shift
				val qHi: ULong
				val qLo: ULong
				val rest: Boolean
				if (t >= 128) {
					qHi = 0uL
					qLo = 0uL
					rest = true
				} else if (t >= 64) {
					val u = t - 64
					qHi = 0uL
					qLo = if (u == 0) xHi else xHi shr u
					rest = xLo != 0uL || (u > 0 && (xHi and ((1uL shl u) - 1uL)) != 0uL)
				} else {
					qHi = xHi shr t
					qLo = (xLo shr t) or (xHi shl (64 - t))
					rest = (xLo and ((1uL shl t) - 1uL)) != 0uL
				}
				if (qHi != 0uL) {
					return 1
				}
				val target = n.toULong()
				return when {
					qLo > target -> 1
					qLo < target -> -1
					rest -> 1
					else -> 0
				}
			}
		}
	}


	@JvmStatic
	fun isLastWordCityGroup(p: SearchPhrase): Boolean {
		return p.isLastWord(ObjectType.CITY) || p.isLastWord(ObjectType.POSTCODE) ||
				p.isLastWord(ObjectType.VILLAGE) || p.isLastWord(ObjectType.BOUNDARY)
	}

	@JvmStatic
	fun createSearchResult(amenity: Amenity, phrase: SearchPhrase, poiTypes: MapPoiTypes): SearchResult {
		val result = SearchResult(phrase)
		result.`object` = amenity
		result.objectType = POI
		result.location = amenity.getLocation()
		result.preferredZoom = PREFERRED_POI_ZOOM

		val settings = phrase.getSettings()!!
		result.otherNames = amenity.getOtherNames(true, result.localeName)
		result.cityName = amenity.getCityFromTagGroups(settings.getLang())
		result.localeName = amenity.getName(settings.getLang(), settings.isTransliterate())
		if (KAlgorithms.isEmpty(result.localeName)) {
			val poiType = anyPoiTypeByKey(poiTypes, amenity.getSubType())
			if (poiType != null) {
				result.localeName = poiType.getTranslation()
			} else {
				result.localeName = amenity.getSubType()
			}
		}

		return result
	}

	// java looks a null key up too, and finds nothing
	private fun anyPoiTypeByKey(types: MapPoiTypes, key: String?): AbstractPoiType? =
		if (key == null) null else types.getAnyPoiTypeByKey(key)

	private fun getMapObjectName(mapObject: MapObject, settings: SearchSettings): String {
		return getMapObjectName(mapObject, settings.getLang(), settings.getAppLang(), settings.isTransliterate())
	}

	private fun getMapObjectName(mapObject: MapObject, mapLang: String?, appLang: String?, transliterate: Boolean): String {
		var name = mapObject.getName(mapLang, transliterate)
		if (KAlgorithms.isEmpty(name) && !KAlgorithms.stringsEqual(appLang, mapLang)) {
			name = mapObject.getName(appLang, transliterate)
		}
		return name
	}

	private fun contains(box: KQuadRect, x: Int, y: Int): Boolean =
		box.contains(x.toDouble(), y.toDouble(), x.toDouble(), y.toDouble())

	private class TopIndexMatch(
		@JvmField val subType: PoiSubType,
		@JvmField val value: String,
		@JvmField val translatedValue: String
	)
}
