package net.osmand.plus.mapcontextmenu.builders

import net.osmand.data.Amenity.DESCRIPTION
import net.osmand.data.Amenity.LANG_YES
import net.osmand.data.Amenity.NAME
import net.osmand.osm.AbstractPoiType
import net.osmand.osm.MapPoiTypes
import net.osmand.plus.OsmandApplication
import net.osmand.shared.gpx.GpxFile.Companion.XML_COLON

private val NAME_TAG_PREFIXES = listOf(
	"name", "int_name", "nat_name", "reg_name", "loc_name",
	"old_name", "alt_name", "short_name", "official_name", "lock_name"
)

class MergeLocalizedTagsAlgorithm private constructor(private val app: OsmandApplication) {

	companion object {
		fun execute(
			app: OsmandApplication,
			originalDict: Map<String, String>
		): Map<String, Any> {
			val instance = MergeLocalizedTagsAlgorithm(app)
			return instance.executeImpl(HashMap(originalDict))
		}
	}

	private fun executeImpl(originalDict: Map<String, Any>): Map<String, Any> {
		val resultDict = mutableMapOf<String, Any>()
		val langDict = applyLanguageOverride(originalDict)
		val localizationsDict = mutableMapOf<String, MutableMap<String, Any>>()

		for (key in langDict.keys) {
			processAdditionalTypeWithKey(
				key, convertKey(key), langDict, localizationsDict, resultDict
			)
		}

		val keysToUpdate = findKeysToUpdate(localizationsDict)
		for (baseKey in keysToUpdate) {
			val localizations = localizationsDict[baseKey]
			localizations?.put(baseKey, langDict[baseKey] ?: continue)
		}

		val finalDict = finalizeLocalizationDict(localizationsDict)
		addRemainingEntriesFrom(resultDict, finalDict)
		return finalDict
	}

	private fun applyLanguageOverride(originalDict: Map<String, Any>): MutableMap<String, Any> {
		val processedDict = originalDict.toMutableMap()

		val langKeys = processedDict.keys.filter {
			it.startsWith("$LANG_YES:") && processedDict[it] == "yes"
		}
		if (langKeys.size == 1) {
			val langKey = langKeys.first()
			val langCode = langKey.substringAfter("$LANG_YES:")

			renameTagInMap(processedDict, NAME, "$NAME:$langCode")
			renameTagInMap(processedDict, DESCRIPTION, "$DESCRIPTION:$langCode")
		}
		return processedDict
	}

	private fun renameTagInMap(dict: MutableMap<String, Any>, oldKey: String, newKey: String) {
		val value = dict.remove(oldKey)
		if (value != null) {
			dict[newKey] = value
		}
	}

	private fun processNameTagWithKey(
		key: String,
		convertedKey: String,
		originalDict: Map<String, Any>,
		localizationsDict: MutableMap<String, MutableMap<String, Any>>
	) {
		if (key.contains(":")) {
			val components = convertedKey.split(":")
			if (components.size == 2) {
				val baseKey = components[0]
				val localeKey = "$baseKey:${components[1]}"

				val nameDict = dictionaryForKey("name", localizationsDict)
				nameDict[localeKey] = originalDict[convertedKey] ?: return
			}
		} else {
			val nameDict = dictionaryForKey("name", localizationsDict)
			nameDict[convertedKey] = originalDict[key] ?: return
		}
	}

	private fun processAdditionalTypeWithKey(
		key: String,
		convertedKey: String,
		originalDict: Map<String, Any>,
		localizationsDict: MutableMap<String, MutableMap<String, Any>>,
		resultDict: MutableMap<String, Any>
	) {
		val poiTypes: MapPoiTypes = app.poiTypes
		val poiType: AbstractPoiType? = poiTypes.getAnyPoiAdditionalTypeByKey(convertedKey)
		if (poiType?.lang != null && key.contains(":")) {
			val components = key.split(":")
			if (components.size == 2) {
				val baseKey = components[0]
				val localeKey = "$baseKey:${components[1]}"

				val baseDict = dictionaryForKey(baseKey, localizationsDict)
				baseDict[localeKey] = originalDict[key] ?: return
			}
		} else {
			resultDict[key] = originalDict[key] ?: return
		}
	}

	private fun dictionaryForKey(
		key: String,
		dict: MutableMap<String, MutableMap<String, Any>>
	): MutableMap<String, Any> {
		return dict.getOrPut(key) { mutableMapOf() }
	}

	private fun findKeysToUpdate(localizationsDict: Map<String, MutableMap<String, Any>>): List<String> {
		val keysToUpdate = mutableListOf<String>()
		for (baseKey in localizationsDict.keys) {
			val localizations = localizationsDict[baseKey]
			if (localizations != null && !localizations.containsKey(baseKey)) {
				keysToUpdate.add(baseKey)
			}
		}
		return keysToUpdate
	}

	private fun finalizeLocalizationDict(localizationsDict: Map<String, MutableMap<String, Any>>): MutableMap<String, Any> {
		val finalDict = mutableMapOf<String, Any>()

		for (baseKey in localizationsDict.keys) {
			val entryDict = mutableMapOf<String, Any>()
			val localizations = localizationsDict[baseKey]
			entryDict["localizations"] = localizations ?: continue
			finalDict[baseKey] = entryDict
		}
		return finalDict
	}

	private fun addRemainingEntriesFrom(
		resultDict: Map<String, Any>,
		finalDict: MutableMap<String, Any>
	) {
		for (key in resultDict.keys) {
			finalDict.putIfAbsent(key, resultDict[key] ?: continue)
		}
	}

	fun isNameTag(tag: String): Boolean {
		for (prefix in NAME_TAG_PREFIXES) {
			if (tag.startsWith(prefix)) {
				return true
			}
		}
		return false
	}

	private fun convertKey(key: String): String {
		return key.replace(XML_COLON, ":")
	}
}
