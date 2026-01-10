package net.osmand.plus.plugins.astro

import net.osmand.plus.settings.backend.preferences.CommonPreference
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

class StarWatcherSettings(private val settingsPref: CommonPreference<String>) {

	companion object {
		private const val KEY_COMMON = "common"
		private const val KEY_SHOW_STAR_MAP = "showStarMap"
		private const val KEY_SHOW_STAR_CHART = "showStarChart"
		private const val KEY_IS_2D_MODE = "is2DMode"

		private const val KEY_STAR_MAP = "star_map"
		private const val KEY_STAR_CHART = "star_chart"

		private const val KEY_SHOW_AZIMUTHAL = "showAzimuthalGrid"
		private const val KEY_SHOW_EQUATORIAL = "showEquatorialGrid"
		private const val KEY_SHOW_ECLIPTIC = "showEclipticLine"
		private const val KEY_SHOW_SUN = "showSun"
		private const val KEY_SHOW_MOON = "showMoon"
		private const val KEY_SHOW_PLANETS = "showPlanets"

		private const val KEY_SHOW_CONSTELLATIONS = "showConstellations"

		private const val KEY_SHOW_STARS = "showStars"
		private const val KEY_SHOW_GALAXIES = "showGalaxies"
		private const val KEY_SHOW_NEBULAE = "showNebulae"
		private const val KEY_SHOW_OPEN_CLUSTERS = "showOpenClusters"
		private const val KEY_SHOW_GLOBULAR_CLUSTERS = "showGlobularClusters"
		private const val KEY_SHOW_GALAXY_CLUSTERS = "showGalaxyClusters"
		private const val KEY_SHOW_BLACK_HOLES = "showBlackHoles"
		private const val KEY_SHOW_MAGNITUDE_FILTER = "showMagnitudeFilter"
		private const val KEY_MAGNITUDE_FILTER = "magnitudeFilter"

		private const val KEY_ITEMS = "items"
		private const val KEY_ID = "id"
		private const val KEY_VISIBLE = "visible"

		private val DEFAULT_VISIBLE_IDS = setOf(
			"sun", "moon", "mercury", "venus", "mars",
			"jupiter", "saturn", "uranus", "neptune"
		)
	}

	data class SkyObjectConfig(
		val id: String,
		val isVisible: Boolean
	)

	data class CommonConfig(
		val showStarMap: Boolean,
		val showStarChart: Boolean
	)

	data class StarMapConfig(
		val showAzimuthalGrid: Boolean,
		val showEquatorialGrid: Boolean,
		val showEclipticLine: Boolean,
		val showSun: Boolean,
		val showMoon: Boolean,
		val showPlanets: Boolean,
		val showConstellations: Boolean,
		val showStars: Boolean,
		val showGalaxies: Boolean,
		val showNebulae: Boolean,
		val showOpenClusters: Boolean,
		val showGlobularClusters: Boolean,
		val showGalaxyClusters: Boolean,
		val showBlackHoles: Boolean,
		val is2DMode: Boolean,
		val showMagnitudeFilter: Boolean,
		val magnitudeFilter: Double?,
		val items: List<SkyObjectConfig>
	)

	data class StarChartConfig(
		val items: List<SkyObjectConfig>
	)

	private fun getSettingsJson(): JSONObject {
		val str = settingsPref.get()
		if (str.isNullOrEmpty()) {
			return JSONObject()
		}
		return try {
			JSONObject(str)
		} catch (_: Exception) {
			JSONObject()
		}
	}

	private fun setSettingsJson(json: JSONObject) {
		settingsPref.set(json.toString())
	}

	private fun parseItems(json: JSONObject?): List<SkyObjectConfig> {
		val itemsList = mutableListOf<SkyObjectConfig>()
		val itemsJson = json?.optJSONArray(KEY_ITEMS)

		if (itemsJson != null) {
			for (i in 0 until itemsJson.length()) {
				val itemObj = itemsJson.optJSONObject(i)
				val id = itemObj?.optString(KEY_ID)
				if (!id.isNullOrEmpty()) {
					val visible = itemObj.optBoolean(KEY_VISIBLE, true)
					itemsList.add(SkyObjectConfig(id, visible))
				}
			}
		} else {
			DEFAULT_VISIBLE_IDS.forEach { id -> itemsList.add(SkyObjectConfig(id, true)) }
		}

		return itemsList
	}

	private fun serializeItems(items: List<SkyObjectConfig>): JSONArray {
		val array = JSONArray()
		items.forEach { item ->
			val obj = JSONObject()
			obj.put(KEY_ID, item.id)
			obj.put(KEY_VISIBLE, item.isVisible)
			array.put(obj)
		}
		return array
	}

	fun getCommonConfig(): CommonConfig {
		val root = getSettingsJson()
		val settings = root.optJSONObject(KEY_COMMON)

		val showStarMap = settings?.optBoolean(KEY_SHOW_STAR_MAP, true) ?: true
		val showStarChart = settings?.optBoolean(KEY_SHOW_STAR_CHART, false) ?: false

		return CommonConfig(showStarMap, showStarChart)
	}

	fun setCommonConfig(config: CommonConfig) {
		val root = getSettingsJson()
		val settings = root.optJSONObject(KEY_COMMON) ?: JSONObject()

		settings.put(KEY_SHOW_STAR_MAP, config.showStarMap)
		settings.put(KEY_SHOW_STAR_CHART, config.showStarChart)

		root.put(KEY_COMMON, settings)
		setSettingsJson(root)
	}

	fun getStarMapConfig(): StarMapConfig {
		val root = getSettingsJson()
		val mapSettings = root.optJSONObject(KEY_STAR_MAP)

		val showAzimuthal = mapSettings?.optBoolean(KEY_SHOW_AZIMUTHAL, true) ?: true
		val showEquatorial = mapSettings?.optBoolean(KEY_SHOW_EQUATORIAL, false) ?: false
		val showEcliptic = mapSettings?.optBoolean(KEY_SHOW_ECLIPTIC, false) ?: false

		val showSun = mapSettings?.optBoolean(KEY_SHOW_SUN, true) ?: true
		val showMoon = mapSettings?.optBoolean(KEY_SHOW_MOON, true) ?: true
		val showPlanets = mapSettings?.optBoolean(KEY_SHOW_PLANETS, true) ?: true

		val showConstellations = mapSettings?.optBoolean(KEY_SHOW_CONSTELLATIONS, false) ?: false

		val showStars = mapSettings?.optBoolean(KEY_SHOW_STARS, false) ?: false
		val showGalaxies = mapSettings?.optBoolean(KEY_SHOW_GALAXIES, false) ?: false
		val showNebulae = mapSettings?.optBoolean(KEY_SHOW_NEBULAE, false) ?: false
		val showOpenClusters = mapSettings?.optBoolean(KEY_SHOW_OPEN_CLUSTERS, false) ?: false
		val showGlobularClusters = mapSettings?.optBoolean(KEY_SHOW_GLOBULAR_CLUSTERS, false) ?: false
		val showGalaxyClusters = mapSettings?.optBoolean(KEY_SHOW_GALAXY_CLUSTERS, false) ?: false
		val showBlackHoles = mapSettings?.optBoolean(KEY_SHOW_BLACK_HOLES, false) ?: false

		val is2DMode = mapSettings?.optBoolean(KEY_IS_2D_MODE, false) ?: false

		val showMagnitudeFilter = mapSettings?.optBoolean(KEY_SHOW_MAGNITUDE_FILTER, false) ?: false
		val magnitudeFilter = mapSettings?.optDouble(KEY_MAGNITUDE_FILTER)?.takeIf { !it.isNaN() }

		val items = parseItems(mapSettings)

		return StarMapConfig(
			showAzimuthalGrid = showAzimuthal,
			showEquatorialGrid = showEquatorial,
			showEclipticLine = showEcliptic,
			showSun = showSun,
			showMoon = showMoon,
			showPlanets = showPlanets,
			showConstellations = showConstellations,
			showStars = showStars,
			showGalaxies = showGalaxies,
			showNebulae = showNebulae,
			showOpenClusters = showOpenClusters,
			showGlobularClusters = showGlobularClusters,
			showGalaxyClusters = showGalaxyClusters,
			showBlackHoles = showBlackHoles,
			is2DMode = is2DMode,
			showMagnitudeFilter = showMagnitudeFilter,
			magnitudeFilter = magnitudeFilter,
			items = items
		)
	}

	fun setStarMapConfig(config: StarMapConfig) {
		val root = getSettingsJson()
		val mapSettings = root.optJSONObject(KEY_STAR_MAP) ?: JSONObject()

		mapSettings.put(KEY_SHOW_AZIMUTHAL, config.showAzimuthalGrid)
		mapSettings.put(KEY_SHOW_EQUATORIAL, config.showEquatorialGrid)
		mapSettings.put(KEY_SHOW_ECLIPTIC, config.showEclipticLine)

		mapSettings.put(KEY_SHOW_SUN, config.showSun)
		mapSettings.put(KEY_SHOW_MOON, config.showMoon)
		mapSettings.put(KEY_SHOW_PLANETS, config.showPlanets)

		mapSettings.put(KEY_SHOW_CONSTELLATIONS, config.showConstellations)

		mapSettings.put(KEY_SHOW_STARS, config.showStars)
		mapSettings.put(KEY_SHOW_GALAXIES, config.showGalaxies)
		mapSettings.put(KEY_SHOW_NEBULAE, config.showNebulae)
		mapSettings.put(KEY_SHOW_OPEN_CLUSTERS, config.showOpenClusters)
		mapSettings.put(KEY_SHOW_GLOBULAR_CLUSTERS, config.showGlobularClusters)
		mapSettings.put(KEY_SHOW_GALAXY_CLUSTERS, config.showGalaxyClusters)
		mapSettings.put(KEY_SHOW_BLACK_HOLES, config.showBlackHoles)

		mapSettings.put(KEY_IS_2D_MODE, config.is2DMode)

		mapSettings.put(KEY_SHOW_MAGNITUDE_FILTER, config.showMagnitudeFilter)
		if (config.magnitudeFilter == null) {
			mapSettings.remove(KEY_MAGNITUDE_FILTER)
		} else {
			mapSettings.put(KEY_MAGNITUDE_FILTER, config.magnitudeFilter)
		}

		mapSettings.put(KEY_ITEMS, serializeItems(config.items))

		root.put(KEY_STAR_MAP, mapSettings)
		setSettingsJson(root)
	}

	fun getStarChartConfig(): StarChartConfig {
		val root = getSettingsJson()
		val chartSettings = root.optJSONObject(KEY_STAR_CHART)
		val items = parseItems(chartSettings)
		return StarChartConfig(items)
	}

	fun setStarChartConfig(config: StarChartConfig) {
		val root = getSettingsJson()
		val chartSettings = root.optJSONObject(KEY_STAR_CHART) ?: JSONObject()
		chartSettings.put(KEY_ITEMS, serializeItems(config.items))
		root.put(KEY_STAR_CHART, chartSettings)
		setSettingsJson(root)
	}
}