package net.osmand.aiconnector.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * OsmAnd settings and map style properties with their names in English and Russian (assets/assistant/
 * settings.json, built from the style files and OsmAnd's strings). A small model does not know setting
 * ids; it finds them here by the words of the request.
 */
class SettingsCatalog(ctx: Context) {

	private class Entry(val json: JSONObject, val words: List<String>)

	private val entries: List<Entry> = ctx.assets.open("assistant/settings.json").use { input ->
		val all = JSONArray(input.readBytes().toString(Charsets.UTF_8))
		(0 until all.length()).map { i ->
			val o = all.getJSONObject(i)
			// values too: "high contrast" finds roadStyle by its value highContrastRoads
			val text = listOf("id", "title", "ru", "about", "ru_about", "values").joinToString(" ") { o.optString(it) }
			Entry(o, words(splitCamel(text)))
		}
	}

	fun byId(id: String): JSONObject? = entries.firstOrNull { it.json.getString("id") == id }?.json

	/** The values a setting takes when they are a closed list (true/false, enum names), otherwise null. */
	fun allowedValues(entry: JSONObject): List<String>? {
		val values = entry.getString("values")
		if (!values.matches(Regex("[A-Za-z0-9_\\-]+(, [A-Za-z0-9_\\-]+)+"))) return null
		return values.split(", ")
	}

	/** The best matches for the query, best first. */
	fun find(query: String, limit: Int = 5): List<JSONObject> {
		val q = words(query).filter { it !in stopWords }
		if (q.isEmpty()) return emptyList()
		return entries.map { e -> e to q.count { w -> e.words.any { matches(w, it) } } }
			.filter { it.second > 0 }
			.sortedByDescending { it.second }
			.take(limit)
			.map { it.first.json }
	}

	/** Russian and English words change their endings: compare the first letters only. */
	private fun matches(queryWord: String, word: String): Boolean {
		val n = minOf(5, queryWord.length)
		return word.length >= n && word.regionMatches(0, queryWord, 0, n)
	}

	private fun words(text: String) = text.lowercase(Locale.ROOT).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 3 }

	private fun splitCamel(text: String) = text.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('_', ' ')

	private val stopWords = setOf("show", "hide", "the", "map", "turn", "set", "change", "make", "and", "with", "setting",
		"settings", "карта", "карте", "карту", "включи", "выключи", "показать", "покажи", "скрой", "убери", "настройка",
		"настройку", "nrenderer", "true", "false", "enable", "disable", "how", "where", "что", "как", "где")
}
