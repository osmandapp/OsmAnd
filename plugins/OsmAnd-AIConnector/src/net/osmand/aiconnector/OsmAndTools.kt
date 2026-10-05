package net.osmand.aiconnector

import android.view.KeyEvent
import net.osmand.aidlapi.IOsmAndAidlCallback
import net.osmand.aidlapi.contextmenu.AContextMenuButton
import net.osmand.aidlapi.customization.AProfile
import net.osmand.aidlapi.customization.PreferenceParams
import net.osmand.aidlapi.customization.SelectProfileParams
import net.osmand.aidlapi.favorite.AFavorite
import net.osmand.aidlapi.favorite.AddFavoriteParams
import net.osmand.aidlapi.gpx.AGpxBitmap
import net.osmand.aidlapi.gpx.ASelectedGpxFile
import net.osmand.aidlapi.gpx.HideGpxParams
import net.osmand.aidlapi.gpx.ShowGpxParams
import net.osmand.aidlapi.logcat.OnLogcatMessageParams
import net.osmand.aidlapi.map.SetMapLocationParams
import net.osmand.aidlapi.navigation.ADirectionInfo
import net.osmand.aidlapi.navigation.NavigateParams
import net.osmand.aidlapi.navigation.NavigateSearchParams
import net.osmand.aidlapi.navigation.OnVoiceNavigationParams
import net.osmand.aidlapi.navigation.StopNavigationParams
import net.osmand.aidlapi.quickaction.QuickActionInfoParams
import net.osmand.aidlapi.quickaction.QuickActionParams
import net.osmand.aidlapi.search.SearchParams
import net.osmand.aidlapi.search.SearchResult
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ToolError(message: String) : Exception(message)

/** MCP tool definitions and their implementation on top of OsmAnd's AIDL API v2. */
class OsmAndTools(private val bridge: OsmAndBridge) {

	private class Tool(
		val name: String,
		val description: String,
		val schema: JSONObject,
		val run: (JSONObject) -> Any
	)

	private val refused = "OsmAnd refused the call. Most likely OsmAnd AI Connector has no permission for it: " +
			"open OsmAnd > Menu > Plugins > OsmAnd AI Connector and turn on the needed group (Navigation, Favorites, " +
			"Tracks...), or tap Allow access in the AI Connector app. The arguments may also be invalid."

	private val tools = listOf(
		Tool(
			"osmand_status",
			"Which OsmAnd package is connected, the current profile, profiles list and whether a screen or context menu is open.",
			schema()
		) { status() },
		Tool(
			"osmand_get_profiles",
			"List OsmAnd profiles (app modes): key, name, parent, routing profile.",
			schema()
		) { profiles() },
		Tool(
			"osmand_select_profile",
			"Switch the current OsmAnd profile. Use a key from osmand_get_profiles, e.g. default (Browse map), car, bicycle, pedestrian, public_transport.",
			schema(prop("profile", "string", "Profile key"), required = listOf("profile"))
		) { a ->
			check(bridge.get().selectProfile(SelectProfileParams(a.getString("profile"))))
			"Profile switched to ${a.getString("profile")}"
		},
		Tool(
			"osmand_get_preference",
			"Read OsmAnd settings by id (the keys OsmAnd uses in its settings export). " +
					"Pass profile to read the value of a specific profile, otherwise the current one is used. $PREF_HINT",
			schema(
				prop("pref_ids", "array", "Preference ids", items = "string"),
				prop("profile", "string", "Optional profile key"),
				required = listOf("pref_ids")
			)
		) { a -> getPreferences(a) },
		Tool(
			"osmand_set_preference",
			"Change one OsmAnd setting. Value is a string as in OsmAnd's settings export: " +
					"true/false, numbers, enum names. Pass profile to change a specific profile, otherwise the current one. $PREF_HINT",
			schema(
				prop("pref_id", "string", "Preference id"),
				prop("value", "string", "New value as a string"),
				prop("profile", "string", "Optional profile key"),
				required = listOf("pref_id", "value")
			)
		) { a -> setPreference(a) },
		Tool(
			"osmand_set_map_location",
			"Move the map to a location.",
			schema(
				prop("lat", "number", "Latitude"),
				prop("lon", "number", "Longitude"),
				prop("zoom", "integer", "Zoom 1-22, 0 keeps the current zoom"),
				prop("animated", "boolean", "Animate the move"),
				required = listOf("lat", "lon")
			)
		) { a ->
			check(
				bridge.get().setMapLocation(
					SetMapLocationParams(
						a.getDouble("lat"), a.getDouble("lon"), a.optInt("zoom", 0), Float.NaN,
						a.optBoolean("animated", true)
					)
				)
			)
			"Map moved"
		},
		Tool(
			"osmand_search",
			"Search OsmAnd offline data (POI and/or addresses) around a point. Returns name, type and coordinates.",
			schema(
				prop("query", "string", "Search text"),
				prop("lat", "number", "Latitude of the search center"),
				prop("lon", "number", "Longitude of the search center"),
				prop("type", "string", "poi, address or all (default)"),
				prop("limit", "integer", "Max results, default 10"),
				required = listOf("query", "lat", "lon")
			)
		) { a -> search(a) },
		Tool(
			"osmand_navigate",
			"Build a route to coordinates and start navigation. Without start the current location is used.",
			schema(
				prop("dest_lat", "number", "Destination latitude"),
				prop("dest_lon", "number", "Destination longitude"),
				prop("dest_name", "string", "Destination name"),
				prop("start_lat", "number", "Optional start latitude"),
				prop("start_lon", "number", "Optional start longitude"),
				prop("profile", "string", "Profile key, e.g. car, bicycle, pedestrian"),
				required = listOf("dest_lat", "dest_lon")
			)
		) { a ->
			val hasStart = a.has("start_lat") && a.has("start_lon")
			check(
				bridge.get().navigate(
					NavigateParams(
						if (hasStart) "Start" else null,
						a.optDouble("start_lat", 0.0), a.optDouble("start_lon", 0.0),
						a.optString("dest_name", "Destination"),
						a.getDouble("dest_lat"), a.getDouble("dest_lon"),
						a.optString("profile", "car"), true, true
					)
				)
			)
			"Navigation requested"
		},
		Tool(
			"osmand_navigate_search",
			"Search for a place by text near a point and navigate to the best match.",
			schema(
				prop("query", "string", "What to search, e.g. an address or place name"),
				prop("search_lat", "number", "Latitude to search around"),
				prop("search_lon", "number", "Longitude to search around"),
				prop("profile", "string", "Profile key, e.g. car, bicycle, pedestrian"),
				required = listOf("query", "search_lat", "search_lon")
			)
		) { a ->
			check(
				bridge.get().navigateSearch(
					NavigateSearchParams(
						null, 0.0, 0.0, a.getString("query"),
						a.getDouble("search_lat"), a.getDouble("search_lon"),
						a.optString("profile", "car"), true, true
					)
				)
			)
			"Navigation search requested"
		},
		Tool("osmand_stop_navigation", "Stop the current navigation.", schema()) {
			check(bridge.get().stopNavigation(StopNavigationParams()))
			"Navigation stopped"
		},
		Tool(
			"osmand_add_favorite",
			"Add a favorite point.",
			schema(
				prop("lat", "number", "Latitude"),
				prop("lon", "number", "Longitude"),
				prop("name", "string", "Name"),
				prop("category", "string", "Favorites group, default 'MCP'"),
				prop("description", "string", "Description"),
				prop("color", "string", "Color name, e.g. red, orange, yellow, lightgreen, green, lightblue, blue, purple"),
				required = listOf("lat", "lon", "name")
			)
		) { a ->
			val fav = AFavorite(
				a.getDouble("lat"), a.getDouble("lon"), a.getString("name"), a.optString("description", ""),
				"", a.optString("category", "MCP"), a.optString("color", "red"), true
			)
			check(bridge.get().addFavorite(AddFavoriteParams(fav)))
			"Favorite added"
		},
		Tool(
			"osmand_refresh_map",
			"Redraw the map, e.g. after changing settings that a layer does not pick up by itself.",
			schema()
		) {
			check(bridge.get().refreshMap())
			"Map refreshed"
		},
		Tool(
			"osmand_get_active_gpx",
			"List tracks (GPX) currently shown on the map: file name relative to the tracks folder, size, modified time.",
			schema()
		) {
			val list = ArrayList<ASelectedGpxFile>()
			check(bridge.get().getActiveGpx(list))
			JSONArray(list.map {
				JSONObject().put("file", it.fileName).put("size", it.fileSize).put("modified", it.modifiedTime)
			})
		},
		Tool(
			"osmand_show_gpx",
			"Show a track on the map. File name relative to the tracks folder, e.g. rec/2026-09-27_13-43_Sun.gpx or a file from tracks/import.",
			schema(prop("file", "string", "Track file name"), required = listOf("file"))
		) { a ->
			check(bridge.get().showGpx(ShowGpxParams(a.getString("file"))))
			"Track shown"
		},
		Tool(
			"osmand_hide_gpx",
			"Hide a track from the map (the file is kept). Use a file name from osmand_get_active_gpx.",
			schema(prop("file", "string", "Track file name"), required = listOf("file"))
		) { a ->
			if (!bridge.get().hideGpx(HideGpxParams(a.getString("file")))) {
				throw ToolError("Track '${a.getString("file")}' is not shown on the map")
			}
			"Track hidden"
		},
		Tool("osmand_list_quick_actions", "List configured quick actions with their numbers.", schema()) {
			val list = ArrayList<QuickActionInfoParams>()
			check(bridge.get().getQuickActionsInfo(list))
			JSONArray(list.mapIndexed { i, q ->
				JSONObject().put("number", i + 1).put("id", q.actionId).put("name", q.name)
					.put("type", q.actionType).put("params", q.params)
			})
		},
		Tool(
			"osmand_execute_quick_action",
			"Run a quick action by its number from osmand_list_quick_actions.",
			schema(prop("number", "integer", "Quick action number"), required = listOf("number"))
		) { a ->
			check(bridge.get().executeQuickAction(QuickActionParams(a.getInt("number"))))
			"Quick action executed"
		}
	).associateBy { it.name }

	fun list(): JSONArray = JSONArray(tools.values.map {
		JSONObject().put("name", it.name).put("description", it.description).put("inputSchema", it.schema)
	})

	/** Returns MCP CallToolResult. */
	fun call(name: String, args: JSONObject): JSONObject {
		val tool = tools[name] ?: throw IllegalArgumentException("Unknown tool: $name")
		return try {
			val out = tool.run(args)
			val text = when (out) {
				is JSONObject -> out.toString(1)
				is JSONArray -> out.toString(1)
				else -> out.toString()
			}
			result(text, false)
		} catch (e: Exception) {
			result(e.message ?: e.toString(), true)
		}
	}

	private fun result(text: String, isError: Boolean) = JSONObject()
		.put("content", JSONArray().put(JSONObject().put("type", "text").put("text", text)))
		.put("isError", isError)

	private fun check(ok: Boolean) {
		if (!ok) throw ToolError(refused)
	}

	private fun readPref(id: String, profile: String?): String? {
		val p = PreferenceParams(id)
		if (profile != null) p.appModeKey = profile
		return if (bridge.get().getPreference(p)) p.value else null
	}

	private fun profiles(): JSONArray {
		val list = ArrayList<AProfile>()
		check(bridge.get().getProfiles(list))
		return JSONArray(list.map {
			JSONObject().put("key", it.stringKey).put("name", it.userProfileName).put("parent", it.parent)
				.put("routing_profile", it.routingProfile)
		})
	}

	private fun status(): JSONObject {
		val api = bridge.get()
		val o = JSONObject()
			.put("package", bridge.boundPackage)
			.put("installed", JSONArray(bridge.installedPackages()))
		// refreshMap is in the Map group that every connected app gets by default
		if (!api.refreshMap()) {
			o.put("enabled", false).put("hint", refused)
			return o
		}
		o.put("enabled", true)
			.put("screen_open", api.isFragmentOpen)
			.put("context_menu_open", api.isMenuOpen)
		// profiles are read through settings, which needs the Settings group
		val current = readPref("application_mode", null)
		if (current == null) {
			return o.put("settings_access", false)
				.put("settings_hint", "Profiles and settings need the Settings group in OsmAnd > Menu > Plugins > OsmAnd AI Connector.")
		}
		return o.put("settings_access", true)
			.put("current_profile", current)
			.put("profiles", profiles())
	}

	private fun getPreferences(a: JSONObject): JSONObject {
		val ids = a.getJSONArray("pref_ids")
		val profile = a.optString("profile").ifEmpty { null }
		val out = JSONObject()
		for (i in 0 until ids.length()) {
			val id = ids.getString(i)
			out.put(id, readPref(id, profile) ?: JSONObject.NULL)
		}
		return out
	}

	private fun setPreference(a: JSONObject): String {
		val id = a.getString("pref_id")
		// OsmAnd fails per-profile prefs when no profile is given, so default to the current one
		val profile = a.optString("profile").ifEmpty { null }
			?: if (id == "application_mode") null else readPref("application_mode", null)
		val p = PreferenceParams(id)
		p.value = a.getString("value")
		if (profile != null) p.appModeKey = profile
		if (!bridge.get().setPreference(p)) {
			throw ToolError("OsmAnd rejected '$id' = '${p.value}': unknown or non-exportable id, wrong value, or the app is not enabled.")
		}
		// layers such as terrain do not redraw on a pref change by themselves
		bridge.get().refreshMap()
		return "$id = ${readPref(id, profile)}" + (profile?.let { " (profile $it)" } ?: "")
	}

	private fun search(a: JSONObject): JSONArray {
		val type = when (a.optString("type", "all")) {
			"poi" -> SearchParams.SEARCH_TYPE_POI
			"address" -> SearchParams.SEARCH_TYPE_ADDRESS
			else -> SearchParams.SEARCH_TYPE_ALL
		}
		val limit = a.optInt("limit", 10)
		val latch = CountDownLatch(1)
		var results: List<SearchResult> = emptyList()
		val callback = object : IOsmAndAidlCallback.Stub() {
			override fun onSearchComplete(resultSet: MutableList<SearchResult>?) {
				results = resultSet ?: emptyList()
				latch.countDown()
			}

			override fun onUpdate() {}
			override fun onAppInitialized() {}
			override fun onGpxBitmapCreated(bitmap: AGpxBitmap?) {}
			override fun updateNavigationInfo(directionInfo: ADirectionInfo?) {}
			override fun onContextMenuButtonClicked(buttonId: Int, pointId: String?, layerId: String?) {}
			override fun onVoiceRouterNotify(params: OnVoiceNavigationParams?) {}
			override fun onKeyEvent(params: KeyEvent?) {}
			override fun onLogcatMessage(params: OnLogcatMessageParams?) {}
		}
		val p = SearchParams(a.getString("query"), type, a.getDouble("lat"), a.getDouble("lon"), 1, limit)
		check(bridge.get().search(p, callback))
		if (!latch.await(30, TimeUnit.SECONDS)) throw ToolError("Search timed out after 30 s")
		return JSONArray(results.take(limit).map {
			JSONObject().put("name", it.localName).put("type", it.localTypeName)
				.put("lat", it.latitude).put("lon", it.longitude)
				.apply { if (!it.alternateName.isNullOrEmpty()) put("alt_name", it.alternateName) }
		})
	}

	private fun prop(name: String, type: String, description: String, items: String? = null) =
		name to JSONObject().put("type", type).put("description", description).apply {
			if (items != null) put("items", JSONObject().put("type", items))
		}

	private fun schema(vararg props: Pair<String, JSONObject>, required: List<String> = emptyList()) =
		JSONObject().put("type", "object")
			.put("properties", JSONObject().apply { props.forEach { put(it.first, it.second) } })
			.put("required", JSONArray(required))
}

private const val PREF_HINT = "Common ids: application_mode (current profile, global), daynight_mode (DAY, NIGHT, AUTO, SENSOR, APP_THEME), " +
		"renderer (map style, e.g. OsmAnd, Touring view (contrast and details), Topo, UniRS, Nautical, Ski map, Winter and ski, Offroad, Desert), " +
		"map_preferred_locale (map labels language, empty = local names), rotate_map (0 none, 1 bearing, 2 compass, 3 manual), " +
		"auto_zoom_map_on_off, voice_mute, show_routing_alarms, metric_system, driving_region, show_poi_label, map_density, text_scale. " +
		"Render style properties use the nrenderer_ prefix, e.g. nrenderer_contourLines, nrenderer_showCycleRoutes."
