package net.osmand.plus.shared

import net.osmand.CollatorStringMatcher
import net.osmand.IndexConstants.GPX_IMPORT_DIR
import net.osmand.IndexConstants.GPX_INDEX_DIR
import net.osmand.IndexConstants.GPX_RECORDED_INDEX_DIR
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter
import net.osmand.data.Amenity
import net.osmand.data.City
import net.osmand.osm.PoiCategory
import net.osmand.plus.AppInitializeListener
import net.osmand.plus.AppInitializer
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.shared.api.CityNameCallback
import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.data.KLatLon
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxTrackAnalysis.TrackPointsAnalyser
import net.osmand.shared.io.KFile
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.shared.settings.enums.SpeedConstants
import net.osmand.shared.util.KStringMatcher
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils
import java.io.IOException

class OsmAndContextImpl(private val app: OsmandApplication) : OsmAndContext {

	companion object {
		private const val CITY_SEARCH_RADIUS: Int = 50 * 1000
	}

	private val settings: SettingsAPIImpl = SettingsAPIImpl(app)

	override fun getAppDir(): KFile = app.getAppPathKt(null)

	override fun getGpxDir(): KFile = app.getAppPathKt(GPX_INDEX_DIR)

	override fun getGpxImportDir(): KFile = app.getAppPathKt(GPX_IMPORT_DIR)

	override fun getGpxRecordedDir(): KFile = app.getAppPathKt(GPX_RECORDED_INDEX_DIR)

	override fun getSettings(): SettingsAPI = settings

	override fun getSpeedSystem(): SpeedConstants? = app.settings.SPEED_SYSTEM.get()

	override fun getMetricSystem(): MetricsConstants? = app.settings.METRIC_SYSTEM.get()

	override fun isGpxFileVisible(path: String): Boolean =
		app.selectedGpxHelper.getSelectedFileByPath(path) != null

	override fun getSelectedFileByPath(path: String): GpxFile? =
		app.selectedGpxHelper.getSelectedFileByPath(path)?.gpxFile

	override fun getAssetAsString(name: String): String? {
		var res: String? = null
		try {
			val inputStream = app.assets.open(name)
			res = Algorithms.readFromInputStream(inputStream).toString()
		} catch (_: IOException) {
		}
		return res
	}

	override fun getNameStringMatcher(name: String, mode: KStringMatcherMode): KStringMatcher {
		return object : KStringMatcher {
			private val sm: CollatorStringMatcher =
				CollatorStringMatcher(name, getStringMatcherMode(mode))

			private fun getStringMatcherMode(mode: KStringMatcherMode): CollatorStringMatcher.StringMatcherMode =
				when (mode) {
					KStringMatcherMode.CHECK_ONLY_STARTS_WITH -> CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH
					KStringMatcherMode.CHECK_STARTS_FROM_SPACE -> CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE
					KStringMatcherMode.CHECK_STARTS_FROM_SPACE_NOT_BEGINNING -> CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE_NOT_BEGINNING
					KStringMatcherMode.CHECK_EQUALS_FROM_SPACE -> CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS_FROM_SPACE
					KStringMatcherMode.CHECK_CONTAINS -> CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS
					KStringMatcherMode.CHECK_EQUALS -> CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS
				}

			override fun matches(name: String): Boolean = sm.matches(name)
		}
	}

	override fun getTrackPointsAnalyser(): TrackPointsAnalyser =
		PluginsHelper.getTrackPointsAnalyser()

	override fun searchNearestCityName(latLon: KLatLon, callback: CityNameCallback) {
		if (app.isApplicationInitializing) {
			app.appInitializer.addListener(object : AppInitializeListener {
				override fun onFinish(init: AppInitializer) {
					searchNearestCity(latLon, callback)
				}
			})
		} else {
			searchNearestCity(latLon, callback)
		}
	}

	private fun searchNearestCity(latLon: KLatLon, callback: CityNameCallback) {
		val cityTypes = City.CityType.entries.associateBy { it.name.lowercase() }
		val rect = MapUtils.calculateLatLonBbox(latLon.latitude, latLon.longitude, CITY_SEARCH_RADIUS)
		val cities = app.resourceManager.searchAmenities(object : SearchPoiTypeFilter {
			override fun accept(type: PoiCategory, subcategory: String): Boolean {
				return cityTypes.containsKey(subcategory)
			}

			override fun isEmpty(): Boolean {
				return false
			}
		}, rect, false)

		if (cities.isNotEmpty()) {
			sortAmenities(cities, cityTypes, latLon)
			callback(cities.first().name)
		} else {
			callback("")
		}
	}

	private fun sortAmenities(
		amenities: MutableList<Amenity>,
		cityTypes: Map<String, City.CityType>,
		latLon: KLatLon
	) {
		val jLatLon = SharedUtil.jLatLon(latLon)
		amenities.sortWith { o1, o2 ->
			val rad1 = cityTypes[o1.subType]?.radius ?: 1000.0
			val rad2 = cityTypes[o2.subType]?.radius ?: 1000.0
			val distance1 = MapUtils.getDistance(jLatLon, o1.location) / rad1
			val distance2 = MapUtils.getDistance(jLatLon, o2.location) / rad2
			distance1.compareTo(distance2)
		}
	}
}