package net.osmand.shared.search.core.spatial

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.Building
import net.osmand.shared.data.City
import net.osmand.shared.data.CityType
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiAdditionalLabels
import net.osmand.shared.search.SearchUICore.SearchResultMatcher
import net.osmand.shared.search.core.ObjectType
import net.osmand.shared.search.core.SearchCoreFactory.PREFERRED_BUILDING_ZOOM
import net.osmand.shared.search.core.SearchCoreFactory.PREFERRED_CITY_ZOOM
import net.osmand.shared.search.core.SearchCoreFactory.PREFERRED_DEFAULT_ZOOM
import net.osmand.shared.search.core.SearchCoreFactory.PREFERRED_POI_ZOOM
import net.osmand.shared.search.core.SearchCoreFactory.PREFERRED_REGION_ZOOM
import net.osmand.shared.search.core.SearchCoreFactory.PREFERRED_STREET_INTERSECTION_ZOOM
import net.osmand.shared.search.core.SearchCoreFactory.PREFERRED_STREET_ZOOM
import net.osmand.shared.search.core.SearchCoreFactory.SEARCH_ADDRESS_BY_NAME_PRIORITY
import net.osmand.shared.search.core.SearchCoreFactory.SearchBaseAPI
import net.osmand.shared.search.core.SearchPhrase
import net.osmand.shared.search.core.SearchPhrase.SearchPhraseDataType
import net.osmand.shared.search.core.SearchResult
import net.osmand.shared.search.core.TopIndexFilter
import net.osmand.shared.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory

/**
 * The spatial text search as an api of [net.osmand.shared.search.SearchUICore]: the whole phrase
 * searched by [SpatialTextSearch] over the maps around its location, and its results made results
 * of the search; and the amenities of a poi category read from the name index.
 *
 * A copy of `SpatialTextSearchAPI` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS. The search rules are read from [searchRulesDir], the directory of `rules*.xml`,
 * where java reads them from its classpath. The search does not time itself here: the statistics
 * of the reader java turns timing on by are not in the copy of the settings.
 */
open class SpatialTextSearchAPI(
	private val poiTypes: MapPoiTypes,
	searchRulesDir: String
) : SearchBaseAPI(
	ObjectType.CITY, ObjectType.VILLAGE, ObjectType.BOUNDARY, ObjectType.POSTCODE,
	ObjectType.STREET, ObjectType.HOUSE, ObjectType.STREET_INTERSECTION, ObjectType.POI,
	ObjectType.POI_TYPE
) {

	private val spatialTextSearch = SpatialTextSearch(searchRulesDir)
	internal val poiSearch = SpatialPoiSearch(poiTypes)

	override fun isSearchAvailable(p: SearchPhrase): Boolean {
		return super.isSearchAvailable(p) && !p.hasCustomSearchType(ObjectType.ONLINE_SEARCH)
	}

	override fun search(phrase: SearchPhrase, resultMatcher: SearchResultMatcher): Boolean {
		// poi categories are matched in the app language and English (poiTypes), the map language (Configure map)
		// and the main language of the search location: "de" in Leipzig, "uk" of "uk,ru"
		val searchSettings = phrase.getSettings()!!
		for (lang in arrayOf(searchSettings.getLang(), searchSettings.getRegionLang())) {
			if (!KAlgorithms.isEmpty(lang)) {
				poiSearch.addLanguage(lang!!.split(",")[0])
			}
		}
		val settings = createSpatialSettings(phrase)
		val files = getSpatialSearchFiles(phrase, settings)
		if (KAlgorithms.isEmpty(files)) {
			return false
		}
		LOG.info("\nStart new spatial search")
		val context = createSpatialContext(phrase, resultMatcher, files, poiSearch, settings)
		LOG.info("Spatial search setting " + (if (context.settings.SEARCH_SUGGESTION) "SUGGESTION" else "Default"))
		LOG.info("Spatial search setting.LANG_DEDUPLICATE " + context.settings.LANG_DEDUPLICATE)
		LOG.info("Spatial search setting.SUGGESTED_SEARCH_RADIUS_KM " + context.settings.SUGGESTED_SEARCH_RADIUS_KM)

		LOG.info("Spatial search start call spatialTextSearch.searchAPI")
		val results = spatialTextSearch.searchAPI(phrase.getFullSearchPhrase(), context)
		LOG.info("Spatial search after call spatialTextSearch.searchAPI")
		val typoSuggestion = results.typoSuggestion
		if (typoSuggestion != null && phrase.isSearchTypeAllowed(ObjectType.SUGGESTION)
			&& !resultMatcher.isCancelled()
		) {
			// "did you mean": the corrected query, searched when the row is tapped
			val suggestion = SearchResult(phrase)
			suggestion.objectType = ObjectType.SUGGESTION
			suggestion.`object` = typoSuggestion
			suggestion.localeName = typoSuggestion
			suggestion.priority = SEARCH_PRIORITY.toDouble()
			resultMatcher.publish(suggestion)
		}
		val mainResults = results.mainResults ?: return true

		var index = 0
		LOG.info("found " + mainResults.size + " mainResults")
		for (spatialResult in mainResults) {
			LOG.info("found mainResult " + spatialResult + ". visible level " + spatialResult.visibleLevel())
			if (index++ == 10) {
				break
			}
		}

		index = 0
		val combinations = results.combinations
		if (combinations != null) {
			LOG.info("found " + combinations.size + " combinations")
			for (combination in combinations) {
				LOG.info("found combination $combination")
				if (index++ == 10) {
					break
				}
			}
		}

		for (spatialResult in mainResults) {
			if (resultMatcher.isCancelled()) {
				return false
			}
			val searchResult = convertResult(phrase, context, spatialResult)
			if (searchResult != null && !phrase.isSearchTypeAllowed(searchResult.objectType!!)) {
				continue
			}
			if (searchResult != null) {
				publishWithParent(resultMatcher, searchResult)
			} else {
				LOG.info("searchResult = null")
			}
		}
		return true
	}

	fun searchPoiByCategory(
		phrase: SearchPhrase, resultMatcher: SearchResultMatcher,
		categoryKey: String, bbox31: KQuadRect
	): List<Amenity> {
		val bboxLatLon = KQuadRect(
			KMapUtils.get31LongitudeX(bbox31.left.toInt()),
			KMapUtils.get31LatitudeY(bbox31.top.toInt()), KMapUtils.get31LongitudeX(bbox31.right.toInt()),
			KMapUtils.get31LatitudeY(bbox31.bottom.toInt())
		)
		// the list needs every object: at this zoom the per-tile thinning of the map layer is off
		val settings = SpatialTextSearchSettings.searchPoiByCategorySettings(PREFERRED_POI_ZOOM, bboxLatLon)
		settings.LIMIT_READ_SINGLE_OBJECTS = 0
		settings.LIMIT_READ_OBJECTS = 0
		val files = ArrayList<BinaryMapIndexReader>()
		val it = phrase.getOfflineIndexes(bbox31, SearchPhraseDataType.POI)
		while (it.hasNext()) {
			val reader = it.next()
			if (hasPoiTypesInNameIndex(reader)) {
				addFile(files, reader)
			}
		}
		val context = createSpatialContext(phrase, resultMatcher, files, poiSearch, settings)
		return spatialTextSearch.searchPoiByCategory(context, categoryKey, bboxLatLon, PREFERRED_POI_ZOOM, Int.MAX_VALUE)
	}

	private fun createSpatialContext(
		phrase: SearchPhrase, resultMatcher: SearchResultMatcher,
		files: List<BinaryMapIndexReader>, poiSearch: SpatialPoiSearch, settings: SpatialTextSearchSettings
	): SpatialSearchContext {
		val context = SpatialSearchContext(settings, files, poiSearch, phrase.getSettings()!!.getOriginalLocation())
		context.resultMatcher = object : ResultMatcher<SpatialSearchResult> {
			override fun publish(obj: SpatialSearchResult): Boolean {
				return !resultMatcher.isCancelled()
			}

			override fun isCancelled(): Boolean = resultMatcher.isCancelled()
		}
		context.stats.doTiming = false
		context.stats.printLogs = true
		return context
	}

	fun getSpatialSearchFiles(phrase: SearchPhrase): List<BinaryMapIndexReader> {
		return getSpatialSearchFiles(phrase, createSpatialSettings(phrase))
	}

	private fun getSpatialSearchFiles(phrase: SearchPhrase, settings: SpatialTextSearchSettings): List<BinaryMapIndexReader> {
		val files = ArrayList<BinaryMapIndexReader>()
		addFiles(files, getSpatialSearchOfflineIndexes(phrase, settings))
		addRegionsFile(files, phrase)
		return files
	}

	private fun getSpatialSearchOfflineIndexes(
		phrase: SearchPhrase, settings: SpatialTextSearchSettings
	): Iterator<BinaryMapIndexReader> {
		val latLon = phrase.getLastTokenLocation()!!
		val rect = KMapUtils.calculate31Bbox(latLon.latitude, latLon.longitude, settings.SUGGESTED_SEARCH_RADIUS_KM * 1000)
		return phrase.getOfflineIndexes(rect, SearchPhraseDataType.MAP)
	}

	private fun addRegionsFile(files: MutableList<BinaryMapIndexReader>, phrase: SearchPhrase) {
		val regions = phrase.getSettings()!!.getRegions()
		if (regions != null) {
			addFile(files, regions.getReader())
		}
	}

	private fun addFiles(files: MutableList<BinaryMapIndexReader>, iterator: Iterator<BinaryMapIndexReader>) {
		while (iterator.hasNext()) {
			addFile(files, iterator.next())
		}
	}

	private fun addFile(files: MutableList<BinaryMapIndexReader>, reader: BinaryMapIndexReader?) {
		if (reader != null && !files.contains(reader)) {
			files.add(reader)
		}
	}

	private fun publishWithParent(resultMatcher: SearchResultMatcher, searchResult: SearchResult) {
		val parent = searchResult.parentSearchResult
		val previousParent = resultMatcher.setParentSearchResult(parent)
		resultMatcher.publish(searchResult)
		resultMatcher.setParentSearchResult(previousParent)
	}

	private fun createSpatialSettings(phrase: SearchPhrase): SpatialTextSearchSettings {
		val settings = if (phrase.isLastUnknownSearchWordComplete())
			SpatialTextSearchSettings.defaultSettings()
		else
			SpatialTextSearchSettings.suggestionSettings()
		settings.LANG_DEDUPLICATE = phrase.getSettings()!!.getLang()
		settings.SEARCH_POI = settings.SEARCH_POI and phrase.isSearchTypeAllowed(ObjectType.POI)
		settings.SEARCH_POI_CATEGORIES = settings.SEARCH_POI_CATEGORIES and phrase.isSearchTypeAllowed(ObjectType.POI_TYPE)
		return settings
	}

	override fun getSearchPriority(p: SearchPhrase): Int {
		// the address tab lists are filtered by SpatialAddressByNameAPI and SpatialStreetByCityAPI
		if (!p.isUnknownSearchWordPresent() || p.isAddressListFilter()
			|| p.isLastWord(ObjectType.POI_TYPE, ObjectType.STREET)
		) {
			return -1
		}
		return SEARCH_PRIORITY
	}

	override fun isSearchMoreAvailable(phrase: SearchPhrase): Boolean {
		return false
	}

	private fun convertResult(phrase: SearchPhrase, context: SpatialSearchContext, ssr: SpatialSearchResult): SearchResult? {
		val result = SearchResult(phrase)

		val location = ssr.getLatLon() // nullable

		val searchSettings = phrase.getSettings()!!
		val lang = searchSettings.getLang()
		val transliterate = searchSettings.isTransliterate()

		if (convertSpatialSearchResult(ssr, result, context.poiSearch, location, lang, transliterate) == null) {
			return null
		}

		phrase.countUnknownWordsMatchMainResult(result)
		return result
	}

	fun convertSpatialSearchResult(
		ssr: SpatialSearchResult, result: SearchResult,
		poiTypeSearch: SpatialPoiSearch, location: KLatLon?,
		lang: String?, transliterate: Boolean
	): SearchResult? {
		val objs = ssr.getObjects()

		result.spatialResult = ssr
		result.location = location // nullable
		result.spatialSearchVisibleLevel = ssr.visibleLevel()

		if (ssr.isPoiCategory()) {
			return convertPoiType(ssr, result, poiTypeSearch)
		} else if (objs.isNotEmpty()) {
			return convertMapObjects(ssr, objs, result, lang, transliterate)
		}

		return null
	}

	private fun convertMapObjects(
		ssr: SpatialSearchResult, mapObjects: List<MapObject>,
		result: SearchResult, lang: String?, transliterate: Boolean
	): SearchResult? {
		val obj = ssr.getMainObject() ?: return null

		val extraNameMatch = ssr.getExtraNameMatch()

		if (obj is Building && obj.isInterpolation() && !extraNameMatch.isNullOrEmpty()) {
			result.localeName = extraNameMatch // interpolated house number
		} else {
			result.localeName = obj.getName(lang, transliterate)
			result.otherNames = obj.getOtherNames(transliterate, result.localeName)
			if (!extraNameMatch.isNullOrEmpty()) {
				result.localeName += " ($extraNameMatch)" // ref
			}
		}

		if (obj is Amenity) {
			result.objectType = ObjectType.POI
			result.cityName = obj.getCityFromTagGroups(lang)
		} else if (obj is Street) {
			result.objectType = if (countStreetRefs(ssr) > 1) ObjectType.STREET_INTERSECTION else ObjectType.STREET
			val city = getSpatialCity(mapObjects)
			if (city != null) {
				result.relatedObject = city
				result.localeRelatedObjectName = city.getName(lang, transliterate)
			}
		} else if (obj is Building) {
			result.objectType = ObjectType.HOUSE
			val street = getSpatialStreet(mapObjects)
			if (street != null) {
				result.relatedObject = street
				result.localeRelatedObjectName = street.getName(lang, transliterate)
			}
			val city = getSpatialCity(mapObjects)
			if (city != null) {
				val parent = SearchResult(result.requiredSearchPhrase)
				parent.relatedObject = city
				parent.localeRelatedObjectName = city.getName(lang, transliterate)
				result.parentSearchResult = parent
			}
		} else if (obj is City) {
			val reader = obj.getReferenceFile()
			if (reader is BinaryMapIndexReader) {
				result.file = reader
				result.relatedObject = reader
				result.localeRelatedObjectName = reader.getRegionName()
			}
			val type = obj.getType()
			result.objectType = if (type == CityType.CITY || type == CityType.TOWN) {
				ObjectType.CITY
			} else if (type == CityType.POSTCODE) {
				ObjectType.POSTCODE
			} else if (type == CityType.BOUNDARY) {
				ObjectType.BOUNDARY
			} else {
				ObjectType.VILLAGE
			}
		} else {
			result.objectType = ObjectType.LOCATION
		}

		result.`object` = obj
		result.priorityDistance = 1.0
		result.priority = SEARCH_PRIORITY.toDouble()
		result.preferredZoom = getPreferredZoom(result.objectType)

		return result
	}

	private fun convertPoiType(ssr: SpatialSearchResult, result: SearchResult, poiTypeSearch: SpatialPoiSearch): SearchResult? {
		val spatialPoiType = ssr.getPoiCategory(poiTypeSearch) ?: return null
		val obj: Any
		val localeName: String
		val poiType = spatialPoiType.singleType
		val poiSubType = spatialPoiType.poiSubType
		if (poiType != null) {
			obj = poiType
			localeName = PoiAdditionalLabels.getSearchName(poiType)
		} else if (poiSubType != null) {
			obj = TopIndexFilter(poiSubType, poiTypes, spatialPoiType.poiAdditional!!)
			localeName = spatialPoiType.names[0]
		} else {
			return null
		}
		result.`object` = obj
		result.objectType = ObjectType.POI_TYPE
		result.localeName = localeName
		result.priority = SEARCH_PRIORITY.toDouble()
		result.priorityDistance = 0.0
		result.preferredZoom = PREFERRED_POI_ZOOM
		return result
	}

	private fun countStreetRefs(spatialResult: SpatialSearchResult): Int {
		var count = 0
		for (ref in spatialResult.objs) {
			if (ref.atom.isStreet() && ref.atom.`object` is Street) {
				count++
			}
		}
		return count
	}

	private fun getPreferredZoom(type: ObjectType?): Int {
		return when (type) {
			ObjectType.HOUSE -> PREFERRED_BUILDING_ZOOM
			ObjectType.STREET -> PREFERRED_STREET_ZOOM
			ObjectType.STREET_INTERSECTION -> PREFERRED_STREET_INTERSECTION_ZOOM
			ObjectType.CITY, ObjectType.VILLAGE -> PREFERRED_CITY_ZOOM
			ObjectType.POSTCODE -> PREFERRED_REGION_ZOOM
			ObjectType.BOUNDARY -> PREFERRED_REGION_ZOOM
			ObjectType.POI -> PREFERRED_POI_ZOOM
			else -> PREFERRED_DEFAULT_ZOOM
		}
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("SpatialTextSearch")

		private const val SEARCH_PRIORITY = SEARCH_ADDRESS_BY_NAME_PRIORITY
		private const val POI_TYPES_IN_NAME_INDEX_EDITION = 1782864000000L // 2026-07-01 UTC

		// maps built before 2026-07 have no poi types in the name index
		fun hasPoiTypesInNameIndex(reader: BinaryMapIndexReader): Boolean {
			return reader.getDateCreated() >= POI_TYPES_IN_NAME_INDEX_EDITION
		}

		private fun getSpatialStreet(objs: List<MapObject>): Street? {
			for (obj in objs) {
				if (obj is Street) {
					return obj
				}
			}
			return null
		}

		private fun getSpatialCity(objs: List<MapObject>): City? {
			for (obj in objs) {
				if (obj is City) {
					return obj
				}
				val city = (obj as? Street)?.getCity()
				if (city != null) {
					return city
				}
			}
			return null
		}
	}
}
