package net.osmand.shared.palette.data.gradient

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil

class GradientSettingsHelper(
	private val settingsAPI: SettingsAPI = PlatformUtil.getOsmAndContext().getSettings()
) {

	companion object {
		private const val GRADIENT_COLOR_PALETTES_PREF = "gradient_color_palettes"

		private val LOG = LoggerFactory.getLogger("GradientSettingsHelper")
	}

	private val json = Json {
		isLenient = true
		ignoreUnknownKeys = true
		encodeDefaults = true
	}

	init {
		settingsAPI.registerPreference(
			GRADIENT_COLOR_PALETTES_PREF,
			defValue = "",
			global = true,
			shared = true
		)
	}

	fun getItems(categoryKey: String): List<GradientSettingsItem> {
		val jsonString = settingsAPI.getStringPreference(GRADIENT_COLOR_PALETTES_PREF)

		if (jsonString.isNullOrEmpty()) {
			return emptyList()
		}

		val result = ArrayList<GradientSettingsItem>()

		try {
			val rootElement = json.parseToJsonElement(jsonString)
			val rootObject = rootElement.jsonObject

			val categoryArray = rootObject[categoryKey]?.jsonArray ?: return emptyList()

			for (element in categoryArray) {
				try {
					val item = json.decodeFromJsonElement<GradientSettingsItem>(element)
					result.add(item)
				} catch (e: Exception) {
					 LOG.debug("Error while reading a single gradient color info from JSON $e")
				}
			}

		} catch (e: Exception) {
			 LOG.debug("Error parsing global gradient settings JSON $e")
		}

		return result
	}

	fun saveItems(categoryKey: String, items: List<GradientSettingsItem>) {
		val currentJsonString = settingsAPI.getStringPreference(GRADIENT_COLOR_PALETTES_PREF)

		val rootMap = HashMap<String, JsonElement>()

		if (!currentJsonString.isNullOrEmpty()) {
			try {
				val rootElement = json.parseToJsonElement(currentJsonString).jsonObject
				rootMap.putAll(rootElement)
			} catch (e: Exception) {
				 LOG.debug("Error parsing global settings JSON $e")
			}
		}

		try {
			val newCategoryElement = json.encodeToJsonElement(items)
			rootMap[categoryKey] = newCategoryElement

			val newJsonString = json.encodeToString(rootMap)
			settingsAPI.setStringPreference(GRADIENT_COLOR_PALETTES_PREF, newJsonString)
		} catch (e: Exception) {
			 LOG.debug("Error saving gradient settings $e")
		}
	}
}