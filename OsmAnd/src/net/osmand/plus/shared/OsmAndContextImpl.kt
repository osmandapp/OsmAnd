package net.osmand.plus.shared

import net.osmand.CollatorStringMatcher
import net.osmand.IndexConstants.GPX_IMPORT_DIR
import net.osmand.IndexConstants.GPX_INDEX_DIR
import net.osmand.IndexConstants.GPX_RECORDED_INDEX_DIR
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks.CITY_TOWN_TYPE
import net.osmand.binary.BinaryMapIndexReader
import net.osmand.data.City
import net.osmand.data.LatLon
import net.osmand.data.QuadRect
import net.osmand.data.QuadTree
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.search.core.SearchPhrase
import net.osmand.search.core.SearchPhrase.SearchPhraseDataType.ADDRESS
import net.osmand.shared.api.CityNameCallback
import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.data.KLatLon
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxTrackAnalysis.TrackPointsAnalyser
import net.osmand.shared.io.KFile
import net.osmand.shared.settings.enums.AltitudeMetrics
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.shared.settings.enums.SpeedConstants
import net.osmand.shared.util.KLock
import net.osmand.shared.util.KStringMatcher
import net.osmand.shared.util.synchronized
import net.osmand.util.Algorithms
import net.osmand.util.MapUtils
import java.io.IOException

class OsmAndContextImpl(private val app: OsmandApplication) : OsmAndContext {

	companion object {
		private const val CITY_SEARCH_RADIUS: Int = 50 * 1000
	}

	private val settings: SettingsAPIImpl = SettingsAPIImpl(app)

	private val townCitiesLock = KLock()
	private val townCitiesInit = mutableSetOf<String>()
	private val townCitiesQR: QuadTree<City> = QuadTree(QuadRect(0.0, 0.0, Int.MAX_VALUE.toDouble(), Int.MAX_VALUE.toDouble()), 12, 0.55f)

	override fun getAppDir(): KFile = app.getAppPathKt(null)

	override fun getCacheDir(): KFile = app.cacheDirKt

	override fun getGpxDir(): KFile = app.getAppPathKt(GPX_INDEX_DIR)

	override fun getGpxImportDir(): KFile = app.getAppPathKt(GPX_IMPORT_DIR)

	override fun getGpxRecordedDir(): KFile = app.getAppPathKt(GPX_RECORDED_INDEX_DIR)

	override fun getSettings(): SettingsAPI = settings

	override fun getSpeedSystem(): SpeedConstants? = app.settings.SPEED_SYSTEM.get()

	override fun getMetricSystem(): MetricsConstants? = app.settings.METRIC_SYSTEM.get()

	override fun getAltitudeMetric(): AltitudeMetrics? = app.settings.ALTITUDE_METRIC.get()

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

	override fun getTrackPointsAnalyser(): TrackPointsAnalyser? =
		PluginsHelper.getTrackPointsAnalyser()

	override fun searchNearestCityName(latLon: KLatLon, callback: CityNameCallback) {
		while (app.isApplicationInitializing) {
			Thread.sleep(50)
		}
		val jLatLon = SharedUtil.jLatLon(latLon)
		val rect = SearchPhrase.calculateBbox(CITY_SEARCH_RADIUS, jLatLon)
		val offlineIndexes = app.resourceManager.getQuickSearchFiles(null).toList()
		val iterator = SearchPhrase.getOfflineIndexes(rect, ADDRESS, offlineIndexes)

		val cities = searchNearestCities(rect, iterator)
		if (cities.isNotEmpty()) {
			sortCities(cities, jLatLon)
			callback(cities.first().name)
		} else {
			callback("")
		}
	}

	private fun searchNearestCities(rect: QuadRect, iterator: Iterator<BinaryMapIndexReader>): MutableList<City> = synchronized(townCitiesLock) {
		while (iterator.hasNext()) {
			val reader = iterator.next()
			if (townCitiesInit.add(reader.regionName)) {
				val cities = reader.getCities(null, CITY_TOWN_TYPE, null, null)
				for (city in cities) {
					val bbox31 = city.bbox31
					val cityRect = if (bbox31 != null) {
						QuadRect(
							bbox31[0].toDouble(),
							bbox31[1].toDouble(),
							bbox31[2].toDouble(),
							bbox31[3].toDouble()
						)
					} else {
						val location = city.location
						val y = MapUtils.get31TileNumberY(location.latitude)
						val x = MapUtils.get31TileNumberX(location.longitude)
						QuadRect(x.toDouble(), y.toDouble(), x.toDouble(), y.toDouble())
					}
					townCitiesQR.insert(city, cityRect)
				}
			}
		}
		townCitiesQR.queryInBox(rect, ArrayList<City>())
	}

	private fun sortCities(cities: MutableList<City>, jLatLon: LatLon) {
		cities.sortWith { c1, c2 ->
			val rad1 = c1.type.radius.toDouble().let { if (it > 0) it else 1000.0 }
			val rad2 = c2.type.radius.toDouble().let { if (it > 0) it else 1000.0 }
			val d1 = MapUtils.getDistance(jLatLon, c1.location) / rad1
			val d2 = MapUtils.getDistance(jLatLon, c2.location) / rad2
			d1.compareTo(d2)
		}
	}
}
