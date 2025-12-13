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

		private const val KEY_STAR_MAP = "star_map"
		private const val KEY_STAR_CHART = "star_chart"

		private const val KEY_SHOW_AZIMUTHAL = "showAzimuthalGrid"
		private const val KEY_SHOW_EQUATORIAL = "showEquatorialGrid"
		private const val KEY_SHOW_ECLIPTIC = "showEclipticLine"
		private const val KEY_SHOW_CONSTELLATIONS = "showConstellations"

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
		val showStarChart: Boolean,
	)

	data class StarMapConfig(
		val showAzimuthalGrid: Boolean,
		val showEquatorialGrid: Boolean,
		val showEclipticLine: Boolean,
		val showConstellations: Boolean,
		val items: List<SkyObjectConfig>
	)

	data class BaseChartConfig(
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
		val showConstellations = mapSettings?.optBoolean(KEY_SHOW_CONSTELLATIONS, false) ?: false

		val items = parseItems(mapSettings)

		return StarMapConfig(showAzimuthal, showEquatorial, showEcliptic, showConstellations, items)
	}

	fun setStarMapConfig(config: StarMapConfig) {
		val root = getSettingsJson()
		val mapSettings = root.optJSONObject(KEY_STAR_MAP) ?: JSONObject()

		mapSettings.put(KEY_SHOW_AZIMUTHAL, config.showAzimuthalGrid)
		mapSettings.put(KEY_SHOW_EQUATORIAL, config.showEquatorialGrid)
		mapSettings.put(KEY_SHOW_ECLIPTIC, config.showEclipticLine)
		mapSettings.put(KEY_SHOW_CONSTELLATIONS, config.showConstellations)

		mapSettings.put(KEY_ITEMS, serializeItems(config.items))

		root.put(KEY_STAR_MAP, mapSettings)
		setSettingsJson(root)
	}

	fun getStarChartConfig(): BaseChartConfig {
		val root = getSettingsJson()
		val chartSettings = root.optJSONObject(KEY_STAR_CHART)
		val items = parseItems(chartSettings)
		return BaseChartConfig(items)
	}

	fun setStarChartConfig(config: BaseChartConfig) {
		val root = getSettingsJson()
		val chartSettings = root.optJSONObject(KEY_STAR_CHART) ?: JSONObject()
		chartSettings.put(KEY_ITEMS, serializeItems(config.items))
		root.put(KEY_STAR_CHART, chartSettings)
		setSettingsJson(root)
	}
}