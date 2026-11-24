package net.osmand.aiconnector.assistant

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import android.util.Xml
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.util.Locale
import java.util.zip.ZipFile

/**
 * The OsmAnd settings and map style properties a small model can find by words. Everything is read from
 * the installed OsmAnd app, so it follows the app's version: the map style properties from the style files
 * inside its APK, the names and descriptions from its strings, in English (the model searches with English
 * words) and in the phone's language.
 */
class SettingsCatalog(private val ctx: Context, private val osmandPackage: String) {

	private class Entry(val json: JSONObject, val words: List<String>)

	/** Read on the first search: the styles are large and OsmAnd may be chosen again before that. */
	private val entries: List<Entry> by lazy {
		try {
			load()
		} catch (e: Exception) {
			Log.w("OsmAndAssistant", "No settings from $osmandPackage: $e")
			emptyList()
		}
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

	fun byId(id: String): JSONObject? = entries.firstOrNull { it.json.getString("id") == id }?.json

	/** The values a setting takes when they are a closed list (true/false, enum names), otherwise null. */
	fun allowedValues(entry: JSONObject): List<String>? {
		val values = entry.getString("values")
		if (!values.matches(Regex("[A-Za-z0-9_\\-]+(, [A-Za-z0-9_\\-]+)+"))) return null
		return values.split(", ")
	}

	private fun load(): List<Entry> {
		val osmand = ctx.createPackageContext(osmandPackage, 0)
		val local = osmand.resources
		val english = osmand.createConfigurationContext(Configuration(local.configuration).apply {
			setLocale(Locale.ENGLISH)
		}).resources
		val out = mutableListOf<Entry>()
		fun add(id: String, title: String?, about: String?, values: String, localTexts: List<String?>) {
			if (title.isNullOrEmpty()) return
			val json = JSONObject().put("id", id).put("title", title).put("about", about ?: "").put("values", values)
			val text = (listOf(id, title, about, values) + localTexts).filterNotNull().joinToString(" ")
			out.add(Entry(json, words(splitCamel(text))))
		}

		// map style properties: the style files are Java resources in OsmAnd's APK
		val seen = mutableSetOf<String>()
		ZipFile(osmand.applicationInfo.sourceDir).use { apk ->
			for (style in STYLES) {
				val entry = apk.getEntry("net/osmand/render/$style") ?: continue
				apk.getInputStream(entry).use { input ->
					val parser = Xml.newPullParser()
					parser.setInput(input, "UTF-8")
					while (parser.next() != XmlPullParser.END_DOCUMENT) {
						if (parser.eventType != XmlPullParser.START_TAG || parser.name != "renderingProperty") continue
						val attr = parser.getAttributeValue(null, "attr") ?: continue
						if (attr in SKIP || parser.getAttributeValue(null, "category") == "ui_hidden" || !seen.add(attr)) continue
						val values = if (parser.getAttributeValue(null, "type") == "boolean") "true, false"
						else (parser.getAttributeValue(null, "possibleValues") ?: "").split(',').filter { it.isNotEmpty() }
							.joinToString(", ")
						add("nrenderer_$attr",
							string(english, "rendering_attr_${attr}_name") ?: parser.getAttributeValue(null, "name"),
							string(english, "rendering_attr_${attr}_description") ?: parser.getAttributeValue(null, "description"),
							values,
							listOf(string(local, "rendering_attr_${attr}_name"), string(local, "rendering_attr_${attr}_description")))
					}
				}
			}
		}

		// app settings: the id and values from OsmAnd's settings, the name from the string its settings screen shows
		for (s in APP_SETTINGS) {
			add(s.id, string(english, s.title), s.about, s.values, listOf(string(local, s.title)))
		}
		return out
	}

	private fun string(res: Resources, name: String): String? {
		val id = res.getIdentifier(name, "string", osmandPackage)
		return if (id == 0) null else res.getString(id)
	}

	/** Words change their endings (contour, contours): compare the first letters only. */
	private fun matches(queryWord: String, word: String): Boolean {
		val n = minOf(5, queryWord.length)
		return word.length >= n && word.regionMatches(0, queryWord, 0, n)
	}

	/** Short words carry no meaning, except ones like 3d. */
	private fun words(text: String) = text.lowercase(Locale.ROOT).split(Regex("[^\\p{L}\\p{N}]+"))
		.filter { it.length >= 3 || it.any(Char::isDigit) }

	private fun splitCamel(text: String) = text.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('_', ' ')

	private val stopWords = setOf("show", "hide", "the", "map", "turn", "set", "change", "make", "and", "with",
		"setting", "settings", "nrenderer", "true", "false", "enable", "disable", "how", "where", "what")

	/** @param title the OsmAnd string its settings screen shows; @param about what the name alone does not say */
	private class AppSetting(val id: String, val title: String, val values: String, val about: String? = null)

	private companion object {
		/** The default style and the addons OsmAnd puts on top of every style. */
		val STYLES = listOf("default.render.xml", "contourlines.addon.render.xml", "routes.addon.render.xml")

		/** Properties OsmAnd sets itself or does not show. */
		val SKIP = setOf("appMode", "baseAppMode", "engine_v1", "currentTrackColor", "currentTrackWidth",
			"useDefaultBuildingColor", "showRoadMaps", "contourLabelDirection")

		/** Ids and values from OsmandSettings and its enums (MetricsConstants, SpeedConstants, AltitudeMetrics). */
		val APP_SETTINGS = listOf(
			AppSetting("daynight_mode", "map_mode", "DAY, NIGHT, AUTO, SENSOR, APP_THEME", "day or night colors of the map"),
			AppSetting("renderer", "map_widget_renderer", "OsmAnd, Touring view (contrast and details), Topo, UniRS, " +
					"LightRS, Nautical, Ski map, Winter and ski, Offroad, Desert, OSM-carto"),
			AppSetting("map_preferred_locale", "map_locale", "a language code such as en or de; empty = local names"),
			AppSetting("preferred_locale", "preferred_locale", "a language code; empty = the phone's language"),
			AppSetting("rotate_map", "rotate_map_to", "0, 1, 2, 3", "0 north is up, 1 movement direction, 2 compass, 3 manual"),
			AppSetting("auto_zoom_map_on_off", "auto_zoom_map", "true, false"),
			AppSetting("voice_mute", "voice_announces", "true, false", "true mutes the voice prompts"),
			AppSetting("default_metric_system", "shared_string_length", "KILOMETERS_AND_METERS, MILES_AND_FEET, " +
					"MILES_AND_METERS, MILES_AND_YARDS, NAUTICAL_MILES_AND_METERS, NAUTICAL_MILES_AND_FEET", "units of distance"),
			AppSetting("default_speed_system", "shared_string_speed", "KILOMETERS_PER_HOUR, MILES_PER_HOUR, " +
					"METERS_PER_SECOND, MINUTES_PER_MILE, MINUTES_PER_KILOMETER, NAUTICALMILES_PER_HOUR", "units of speed"),
			AppSetting("altitude_metrics", "altitude", "METERS, FEET", "units of altitude and height"),
			AppSetting("text_scale", "text_size", "0.75 to 3, 1 = normal"),
			AppSetting("map_density_n", "map_magnifier", "0.25 to 4, 1 = normal"),
			AppSetting("show_routing_alarms", "screen_alerts", "true, false"),
			AppSetting("show_speed_limit_warnings", "traffic_warning_speed_limit", "true, false", "warning"),
			AppSetting("show_cameras", "show_cameras", "true, false", "warning"),
			AppSetting("show_traffic_warnings", "show_traffic_warnings", "true, false"),
			AppSetting("speed_limit_exceed", "speed_limit_exceed", "a number, e.g. 5"),
			AppSetting("keep_informing", "keep_informing", "minutes, 0 = only when needed"),
			AppSetting("turn_screen_on_time_int", "wake_time", "seconds, 0 = off", "turn the screen on for a prompt"),
			AppSetting("speak_street_names", "speak_street_names", "true, false"),
			AppSetting("announce_wpt", "shared_string_gpx_waypoints", "true, false", "announce them"),
			AppSetting("snap_to_road", "snap_to_road", "true, false"),
			AppSetting("enable_3d_map_objects", "enable_3d_objects", "true, false"),
			AppSetting("terrain_layer", "shared_string_terrain", "true, false", "hillshade, slope"),
			AppSetting("show_favorites", "favorites_item", "true, false", "on the map"),
			AppSetting("show_coordinates_grid", "layer_coordinates_grid", "true, false"),
			AppSetting("animate_my_location", "position_animation", "true, false")
		)
	}
}
