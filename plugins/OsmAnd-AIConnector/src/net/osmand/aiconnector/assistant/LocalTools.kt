package net.osmand.aiconnector.assistant

import android.util.Base64
import net.osmand.aiconnector.OsmAndTools
import org.json.JSONArray
import org.json.JSONObject

/** What a tool gave back: text for the model and, for a screenshot, the picture. */
class ToolResult(val text: String, val failed: Boolean, val image: ByteArray? = null)

/**
 * The tools a small on-device model gets: fewer than the MCP server offers and with short descriptions.
 * On the desktop test (20 requests) Qwen3 1.7B went from 16 to 18 correct with this set, and the prompt
 * shrank from ~3,300 to ~1,350 tokens, which is what the phone spends most of its time on.
 */
class LocalTools(private val tools: OsmAndTools, private val settings: SettingsCatalog) {

	private class Entry(val description: String?, val params: List<String>? = null)

	private val entries = linkedMapOf(
		"osmand_status" to Entry("Connected OsmAnd app, current profile and profiles."),
		"osmand_select_profile" to Entry("Switch profile: default (Browse map), car, bicycle, pedestrian, public_transport."),
		"osmand_set_preference" to Entry("Change a setting. Find its pref_id and values with osmand_find_setting " +
				"first, unless it is daynight_mode (DAY, NIGHT, AUTO) or renderer (OsmAnd, Topo, Touring view, Offroad).",
			listOf("pref_id", "value")),
		"osmand_set_camera" to Entry("Move or tilt the map: lat, lon, zoom, rotation, elevation_angle (90 flat, 30-50 3D)."),
		"osmand_search" to Entry("Search places, POI and addresses near lat, lon. Returns names and coordinates."),
		"osmand_navigate_search" to Entry("Navigate to a place or address by text, searched near search_lat, search_lon."),
		"osmand_navigate" to Entry("Navigate to coordinates, e.g. a result of osmand_search."),
		"osmand_stop_navigation" to Entry(null),
		"osmand_add_favorite" to Entry("Save a point as a favorite.", listOf("lat", "lon", "name", "category")),
		"osmand_list_tracks" to Entry("Find the user's tracks. Filters: activity (e.g. skiing, hiking, road_cycling), " +
				"min_km, max_km, from, to (YYYY-MM-DD); sort: newest, oldest, longest. Replies how many match and lists the first.",
			listOf("query", "activity", "from", "to", "min_km", "max_km", "sort")),
		"osmand_show_gpx" to Entry("Show a track file on the map."),
		"osmand_track_stats" to Entry("Distance, time, speed and elevation of a track file."),
		"osmand_start_recording" to Entry(null),
		"osmand_recording_status" to Entry(null),
		"osmand_stop_recording" to Entry(null),
		"osmand_list_quick_actions" to Entry(null),
		"osmand_execute_quick_action" to Entry(null)
	)

	/** Tools of the assistant itself, not of the MCP server. */
	private val local = JSONArray()
		.put(declaration("osmand_find_setting", "Find OsmAnd settings and map style options (what the map shows, " +
				"units, voice, language, 3D...) by words. Returns pref_id, allowed values and the current value.",
			"query", "Words that describe the setting, e.g. contour lines, hide buildings, miles"))
		.put(declaration("osmand_look_at_screen", "Look at the OsmAnd screen as the user sees it now: map, " +
				"widgets, buttons. Only for questions about what the screen shows, never to change settings.",
			"question", "What to look for"))

	private fun declaration(name: String, description: String, param: String, about: String) = JSONObject()
		.put("name", name).put("description", description)
		.put("parameters", JSONObject().put("type", "object")
			.put("properties", JSONObject().put(param, JSONObject().put("type", "string").put("description", about)))
			.put("required", JSONArray().put(param)))

	/** Pictures go only to models that see them. */
	var vision = false

	/** Parameters whose name says enough: their descriptions only cost prompt tokens. */
	private val obviousParams = setOf("lat", "lon", "zoom", "name", "query", "file", "number", "category")

	/** Function declarations in the shape chat templates expect: name, description, parameters. */
	fun declarations(): JSONArray {
		val out = JSONArray()
		val all = tools.list()
		for (i in 0 until all.length()) {
			val t = all.getJSONObject(i)
			val entry = entries[t.getString("name")] ?: continue
			val schema = JSONObject(t.getJSONObject("inputSchema").toString())
			val props = schema.optJSONObject("properties") ?: JSONObject()
			for (key in props.keys().asSequence().toList()) {
				if (entry.params != null && key !in entry.params) props.remove(key)
				else if (key in obviousParams) props.getJSONObject(key).remove("description")
			}
			schema.optJSONArray("required")?.let { req ->
				val kept = JSONArray()
				for (j in 0 until req.length()) if (props.has(req.getString(j))) kept.put(req.getString(j))
				schema.put("required", kept)
			}
			out.put(JSONObject().put("name", t.getString("name"))
				.put("description", entry.description ?: t.getString("description"))
				.put("parameters", schema))
		}
		for (i in 0 until local.length()) {
			val d = local.getJSONObject(i)
			if (vision || d.getString("name") != "osmand_look_at_screen") out.put(d)
		}
		return out
	}

	fun isKnown(name: String) = entries.containsKey(name) || name == "osmand_find_setting" ||
			(vision && name == "osmand_look_at_screen")

	/** A tool result in full, for the assistant's own use (not shown to the model as is). */
	fun read(name: String, args: JSONObject = JSONObject()): JSONObject? = try {
		val result = tools.call(name, args)
		if (result.optBoolean("isError")) null
		else JSONObject(result.getJSONArray("content").getJSONObject(0).getString("text"))
	} catch (e: Exception) {
		null
	}

	/** Tools that only read: they run even in a dry run. */
	private val readOnly = setOf("osmand_find_setting", "osmand_look_at_screen", "osmand_status", "osmand_search", "osmand_list_tracks", "osmand_track_stats",
		"osmand_recording_status", "osmand_list_quick_actions")

	/**
	 * Runs a tool; the text the model reads back and whether it failed.
	 * @param dryRun tools that change OsmAnd are not run, the model reads that they were done
	 */
	fun call(call: ToolCall, dryRun: Boolean = false): ToolResult {
		if (!isKnown(call.name)) return ToolResult("Unknown tool ${call.name}", true)
		if (call.name == "osmand_set_preference") checkSetting(call.args)?.let { return ToolResult(it, true) }
		if (dryRun && call.name !in readOnly) return ToolResult("Done (dry run, OsmAnd was not changed)", false)
		return when (call.name) {
			"osmand_find_setting" -> findSetting(call.args.optString("query"))
			"osmand_look_at_screen" -> screenshot()
			else -> callServer(call.name, call.args)
		}
	}

	/** Any tool of the MCP server, also those the model is not given (debug runs). */
	fun callServer(name: String, args: JSONObject): ToolResult {
		val result = tools.call(name, args)
		val content = result.optJSONArray("content")
		val text = (0 until (content?.length() ?: 0))
			.map { content!!.getJSONObject(it) }
			.filter { it.optString("type") == "text" }
			.joinToString("\n") { it.optString("text") }
		// long results (track lists, search) would crowd out the small context window
		return ToolResult(compact(name, text).take(maxResultChars), result.optBoolean("isError"))
	}

	/**
	 * Small models guess setting ids and values ("hide_buildings", contour lines "true"); OsmAnd would
	 * store them without effect. A guess goes back to the model as an error that says what to do.
	 */
	private fun checkSetting(args: JSONObject): String? {
		val id = args.optString("pref_id")
		val entry = settings.byId(id) ?: return "Unknown pref_id \"$id\". Call osmand_find_setting to get the right pref_id."
		val allowed = settings.allowedValues(entry) ?: return null
		val value = args.optString("value")
		val match = allowed.firstOrNull { it.equals(value, ignoreCase = true) }
			?: return "Wrong value \"$value\" for $id. Use one of: ${allowed.joinToString(", ")}."
		args.put("value", match)
		return null
	}

	private fun findSetting(query: String): ToolResult {
		val found = settings.find(query)
		if (found.isEmpty()) return ToolResult("No setting matches \"$query\". Try other words.", false)
		val ids = JSONArray().apply { found.forEach { put(it.getString("id")) } }
		val current = read("osmand_get_preference", JSONObject().put("pref_ids", ids)) ?: JSONObject()
		val text = found.joinToString("\n") { s ->
			val id = s.getString("id")
			val about = s.optString("about").takeIf { it.isNotEmpty() && it != s.optString("title") }
			"pref_id $id: ${s.getString("title")}" + (about?.let { " ($it)" } ?: "") +
					"; values: ${s.getString("values")}; now: ${current.optString(id).takeIf { it.isNotEmpty() && it != "null" } ?: "default"}"
		}
		return ToolResult("$text\nUse a pref_id from this list and one of its values exactly as written.", false)
	}

	/**
	 * OsmAnd draws a screenshot only while its map is on the screen, and while the user chats the assistant
	 * is: these bring OsmAnd up for the picture and the chat back after it.
	 */
	var showOsmAnd: (() -> Unit)? = null
	var showChat: (() -> Unit)? = null

	private fun screenshot(): ToolResult {
		val first = takeScreenshot()
		if (first.image != null || showOsmAnd == null) return first
		showOsmAnd?.invoke()
		return takeScreenshot().also { showChat?.invoke() }
	}

	private fun takeScreenshot(): ToolResult {
		val result = tools.call("osmand_screenshot", JSONObject().put("max_width", 768).put("quality", 70))
		val content = result.optJSONArray("content") ?: return ToolResult("No picture", true)
		for (i in 0 until content.length()) {
			val c = content.getJSONObject(i)
			if (c.optString("type") == "image") {
				return ToolResult("The screenshot of OsmAnd is attached.", false, Base64.decode(c.getString("data"), Base64.DEFAULT))
			}
		}
		return ToolResult(content.optJSONObject(0)?.optString("text") ?: "No picture", true)
	}

	/**
	 * A result as the model reads it: JSON without indentation and long lists cut to their first items.
	 * Pretty JSON of 284 tracks filled the model's 4096-token context in three questions.
	 */
	private fun compact(tool: String, text: String): String = try {
		when {
			tool == "osmand_list_tracks" -> trackList(JSONObject(text))
			text.trimStart().startsWith("{") -> shorten(JSONObject(text)).toString()
			text.trimStart().startsWith("[") -> shorten(JSONArray(text)).toString()
			else -> text
		}
	} catch (e: Exception) {
		text
	}

	/**
	 * A track list as text: small models count the items they see instead of reading "found" from JSON,
	 * and miss the activity filter unless the reply names it.
	 */
	private fun trackList(o: JSONObject): String {
		val tracks = o.optJSONArray("tracks") ?: JSONArray()
		val shown = minOf(tracks.length(), maxListItems)
		val sb = StringBuilder("Found ${o.optInt("found")} matching tracks of ${o.optInt("total")} in all.")
		val activities = o.optJSONArray("activities")
		if (activities != null && activities.length() > 0) {
			sb.append(" Activities: ").append((0 until activities.length()).joinToString { activities.getString(it) })
				.append(" (filter with activity).")
		}
		if (shown > 0) sb.append(" The first $shown:")
		for (i in 0 until shown) {
			val t = tracks.getJSONObject(i)
			sb.append("\n- ").append(t.optString("start").take(10)).append(", ").append(t.optDouble("distance_km")).append(" km")
			t.optString("activity").takeIf { it.isNotEmpty() }?.let { sb.append(", ").append(it) }
			t.optString("city").takeIf { it.isNotEmpty() }?.let { sb.append(", ").append(it) }
			sb.append(", file ").append(t.optString("file"))
		}
		return sb.toString()
	}

	private fun shorten(v: Any?): Any? = when (v) {
		is JSONObject -> JSONObject().also { o -> v.keys().forEach { k -> o.put(k, shorten(v.opt(k))) } }
		is JSONArray -> JSONArray().also { a ->
			for (i in 0 until minOf(v.length(), maxListItems)) a.put(shorten(v.opt(i)))
			if (v.length() > maxListItems) a.put("${v.length() - maxListItems} more")
		}
		else -> v
	}

	private val maxListItems = 10
	private val maxResultChars = 1500
}
