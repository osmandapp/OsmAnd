package net.osmand.shared.search.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.City
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.data.MapObject
import net.osmand.shared.map.OsmandRegions
import net.osmand.shared.map.WorldRegion
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import okio.IOException
import kotlin.jvm.JvmStatic

/**
 * The settings a search runs with: where, in which languages, how far, over which files and for
 * which types. Immutable object: the setters return a changed copy.
 *
 * A copy of `SearchSettings` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. The statistics of the reader (`stat`) are not copied, as they are not in the shared
 * reader, and neither is `toJSON`, which only the server calls. [parseJSON] reads the settings of
 * the search tests; as with org.json in java, numbers and booleans may also be given as strings.
 */
class SearchSettings {

	enum class SortType {
		BY_RELEVANCE,
		ONLY_BY_DISTANCE,
		IGNORE_DISTANCE
	}

	private var originalLocation: KLatLon? = null
	private var regions: OsmandRegions? = null
	private var regionLang: String? = null
	private var offlineIndexes: List<BinaryMapIndexReader> = ArrayList()
	private var radiusLevel = 1
	private var totalLimit = -1
	private var appLang: String? = null
	private var mapLang: String? = null
	private var transliterateIfMissing = false
	private var searchTypes: Array<out ObjectType>? = null
	private var emptyQueryAllowed = false
	private var sortByName = false
	private var searchBBox31: KQuadRect? = null
	private var exportSettings: SearchExportSettings? = null // = new SearchExportSettings(true, true, -1);
	private var exportedObjects: MutableList<MapObject>? = null
	private var exportedCities: MutableList<City>? = null
	private var sortType: SortType? = null
	private var regionPriorityProvider: RegionPriorityProvider? = null

	constructor(s: SearchSettings?) {
		if (s != null) {
			this.radiusLevel = s.radiusLevel
			this.appLang = s.appLang
			this.mapLang = s.mapLang
			this.transliterateIfMissing = s.transliterateIfMissing
			this.totalLimit = s.totalLimit
			this.offlineIndexes = s.offlineIndexes
			this.originalLocation = s.originalLocation
			this.searchBBox31 = s.searchBBox31
			this.regions = s.regions
			this.regionLang = s.regionLang
			this.searchTypes = s.searchTypes
			this.emptyQueryAllowed = s.emptyQueryAllowed
			this.sortByName = s.sortByName
			this.exportSettings = s.exportSettings
			this.sortType = s.sortType
		}
	}

	constructor(offlineIndexes: List<BinaryMapIndexReader>) {
		this.offlineIndexes = offlineIndexes
	}

	fun getExportedObjects(): MutableList<MapObject>? = exportedObjects

	fun setExportedObjects(exportedObjects: MutableList<MapObject>?) {
		if (exportedObjects == null) {
			this.exportedObjects = null
			return
		}

		val current = this.exportedObjects
		if (current == null)
			this.exportedObjects = exportedObjects
		else
			current.addAll(exportedObjects)
	}

	fun getExportedCities(): MutableList<City>? = exportedCities

	fun setExportedCities(exportedCities: MutableList<City>?) {
		if (exportedCities == null) {
			this.exportedCities = null
			return
		}

		val current = this.exportedCities
		if (current == null)
			this.exportedCities = exportedCities
		else
			current.addAll(exportedCities)
	}

	fun getOfflineIndexes(): List<BinaryMapIndexReader> = offlineIndexes

	fun setOfflineIndexes(offlineIndexes: List<BinaryMapIndexReader>) {
		this.offlineIndexes = offlineIndexes
	}

	fun getRadiusLevel(): Int = radiusLevel

	fun getAppLang(): String? = appLang

	fun getLang(): String? = mapLang

	fun setLang(lang: String?, transliterateIfMissing: Boolean): SearchSettings {
		return setLangs(lang, lang, transliterateIfMissing)
	}

	fun setLangs(appLang: String?, mapLang: String?, transliterateIfMissing: Boolean): SearchSettings {
		val s = SearchSettings(this)
		s.appLang = appLang
		s.mapLang = mapLang
		s.transliterateIfMissing = transliterateIfMissing
		return s
	}

	fun setRadiusLevel(radiusLevel: Int): SearchSettings {
		val s = SearchSettings(this)
		s.radiusLevel = radiusLevel
		return s
	}

	fun getTotalLimit(): Int = totalLimit

	fun setTotalLimit(totalLimit: Int): SearchSettings {
		val s = SearchSettings(this)
		s.totalLimit = totalLimit
		return s
	}

	fun getOriginalLocation(): KLatLon? = originalLocation

	fun setOriginalLocation(l: KLatLon?): SearchSettings {
		val s = SearchSettings(this)
		val originalLocation = this.originalLocation
		val distance = if (originalLocation == null) -1.0 else KMapUtils.getDistance(l!!, originalLocation)
		s.regionLang = if (distance > MIN_DISTANCE_REGION_LANG_RECALC || distance == -1.0 || this.regionLang == null) calculateRegionLang(l) else this.regionLang
		s.originalLocation = l
		return s
	}

	fun getSearchBBox31(): KQuadRect? = searchBBox31

	fun setSearchBBox31(searchBBox31: KQuadRect?): SearchSettings {
		val s = SearchSettings(this)
		s.searchBBox31 = searchBBox31
		return s
	}

	fun isTransliterate(): Boolean = transliterateIfMissing

	fun getSearchTypes(): Array<out ObjectType>? = searchTypes

	fun isCustomSearch(): Boolean = searchTypes != null

	fun setSearchTypes(vararg searchTypes: ObjectType): SearchSettings {
		val s = SearchSettings(this)
		s.searchTypes = searchTypes
		return s
	}

	fun updateSearchTypes(vararg searchTypes: ObjectType) {
		this.searchTypes = searchTypes
	}

	fun resetSearchTypes(): SearchSettings {
		val s = SearchSettings(this)
		s.searchTypes = null
		return s
	}

	fun isEmptyQueryAllowed(): Boolean = emptyQueryAllowed

	fun setEmptyQueryAllowed(emptyQueryAllowed: Boolean): SearchSettings {
		val s = SearchSettings(this)
		s.emptyQueryAllowed = emptyQueryAllowed
		return s
	}

	fun setSortByName(sortByName: Boolean): SearchSettings {
		val s = SearchSettings(this)
		s.sortType = if (sortByName) SortType.IGNORE_DISTANCE else SortType.BY_RELEVANCE
		return s
	}

	fun getSortType(): SortType? = sortType

	fun setSortType(sortType: SortType?) {
		this.sortType = sortType
	}

	fun getExportSettings(): SearchExportSettings? = exportSettings

	fun setExportSettings(exportSettings: SearchExportSettings?): SearchSettings {
		val s = SearchSettings(this)
		this.exportSettings = exportSettings
		return s
	}

	fun isExportObjects(): Boolean = exportSettings != null

	fun hasCustomSearchType(type: ObjectType): Boolean {
		val searchTypes = searchTypes
		if (searchTypes != null) {
			for (t in searchTypes) {
				if (t == type) {
					return true
				}
			}
		}
		return false
	}

	fun getRegionLang(): String? = regionLang

	fun getRegions(): OsmandRegions? = regions

	fun setRegions(regions: OsmandRegions?) {
		this.regions = regions
	}

	private fun calculateRegionLang(l: KLatLon?): String? {
		var region: WorldRegion? = null
		try {
			val regions = regions
			if (regions != null) {
				val entry = regions.getSmallestBinaryMapDataObjectAt(l!!)
				if (entry != null) {
					region = entry.key
				}
			}
		} catch (e: IOException) {
			LOG.error(e.message, e)
		}
		if (region != null) {
			return region.getParams().getRegionLang()
		}
		return null
	}

	fun hasRegionPriority(): Boolean = regionPriorityProvider != null

	fun updateRegionPriorityProvider(phrase: SearchPhrase) {
		var provider = regionPriorityProvider
		if (provider == null) {
			provider = RegionPriorityProvider(phrase)
			regionPriorityProvider = provider
		}
		provider.checkAndUpdate(phrase)
	}

	fun getRegionPriorityIndexes(): Collection<BinaryMapIndexReader> {
		val provider = regionPriorityProvider
		if (provider != null) {
			return provider.getOfflineIndexes()
		}
		return emptyList()
	}

	fun getRegionPriorityIndexesWithMinRadius(min: Int, max: Int): List<BinaryMapIndexReader> {
		val provider = regionPriorityProvider
		if (provider != null) {
			return provider.getOfflineIndexes(min, max)
		}
		return emptyList()
	}

	fun getRegionPriority(reader: BinaryMapIndexReader?): Int {
		val provider = regionPriorityProvider
		if (provider != null) {
			return provider.getRegionWeight(reader)
		}
		return 0
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("SearchSettings")
		private const val MIN_DISTANCE_REGION_LANG_RECALC = 10000.0

		@JvmStatic
		fun parseJSON(json: JsonObject): SearchSettings {
			val s = SearchSettings(ArrayList<BinaryMapIndexReader>())
			if (json.containsKey("lat") && json.containsKey("lon")) {
				s.originalLocation = KLatLon(getDouble(json, "lat"), getDouble(json, "lon"))
			}
			s.radiusLevel = optInt(json, "radiusLevel", 1)
			s.totalLimit = optInt(json, "totalLimit", -1)
			s.transliterateIfMissing = optBoolean(json, "transliterateIfMissing", false)
			s.emptyQueryAllowed = optBoolean(json, "emptyQueryAllowed", false)
			s.sortByName = optBoolean(json, "sortByName", false)

			if (json.containsKey("lang")) {
				s.mapLang = getString(json["lang"], "lang")
			}
			if (json.containsKey("appLang")) {
				s.appLang = getString(json["appLang"], "appLang")
			}
			if (json.containsKey("regionLang")) {
				s.regionLang = getString(json["regionLang"], "regionLang")
			}
			if (json.containsKey("searchTypes")) {
				val searchTypesArr = json["searchTypes"] as? JsonArray
					?: throw IllegalArgumentException("JSONObject[\"searchTypes\"] is not a JSONArray.")
				val searchTypes = Array(searchTypesArr.size) { i ->
					val name = getString(searchTypesArr[i], "searchTypes")
					ObjectType.valueOf(name)
				}
				s.searchTypes = searchTypes
			}
			return s
		}

		// org.json's getters, over the values kotlinx reads

		private fun getDouble(json: JsonObject, key: String): Double {
			val value = json[key]
			if (value !is JsonPrimitive || value is JsonNull) {
				throw IllegalArgumentException("JSONObject[\"$key\"] is not a number.")
			}
			return value.content.toDouble()
		}

		private fun optInt(json: JsonObject, key: String, defaultValue: Int): Int {
			val value = json[key]
			if (value !is JsonPrimitive || value is JsonNull) {
				return defaultValue
			}
			val content = value.content
			return content.toIntOrNull() ?: content.toDoubleOrNull()?.toInt() ?: defaultValue
		}

		private fun optBoolean(json: JsonObject, key: String, defaultValue: Boolean): Boolean {
			val value = json[key]
			if (value !is JsonPrimitive || value is JsonNull) {
				return defaultValue
			}
			val content = value.content
			if (content.equals("true", ignoreCase = true)) {
				return true
			}
			if (content.equals("false", ignoreCase = true)) {
				return false
			}
			return defaultValue
		}

		private fun getString(value: JsonElement?, key: String): String {
			if (value is JsonPrimitive && value !is JsonNull && value.isString) {
				return value.content
			}
			throw IllegalArgumentException("JSONObject[\"$key\"] not a string.")
		}
	}
}
