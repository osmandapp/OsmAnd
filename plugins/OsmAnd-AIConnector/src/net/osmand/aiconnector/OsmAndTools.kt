package net.osmand.aiconnector

import android.util.Base64
import android.view.KeyEvent
import net.osmand.aidlapi.IOsmAndAidlCallback
import net.osmand.aidlapi.contextmenu.AContextMenuButton
import net.osmand.aidlapi.customization.AProfile
import net.osmand.aidlapi.customization.PreferenceParams
import net.osmand.aidlapi.customization.SelectProfileParams
import net.osmand.aidlapi.favorite.AFavorite
import net.osmand.aidlapi.favorite.AddFavoriteParams
import net.osmand.aidlapi.gpx.AGpxBitmap
import net.osmand.aidlapi.gpx.AGpxFile
import net.osmand.aidlapi.gpx.GpxPointsParams
import net.osmand.aidlapi.gpx.GpxSearchParams
import net.osmand.aidlapi.gpx.ASelectedGpxFile
import net.osmand.aidlapi.gpx.HideGpxParams
import net.osmand.aidlapi.gpx.ShowGpxParams
import net.osmand.aidlapi.gpx.StartGpxRecordingParams
import net.osmand.aidlapi.gpx.StopGpxRecordingParams
import net.osmand.aidlapi.logcat.OnLogcatMessageParams
import net.osmand.aidlapi.info.AMapWidgetValue
import net.osmand.aidlapi.map.MapScreenshotParams
import net.osmand.aidlapi.map.SetMapCameraParams
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ToolError(message: String) : Exception(message)

/** A tool result shown to the assistant as a picture. */
class ToolImage(val jpeg: ByteArray, val caption: String)

/** MCP tool definitions and their implementation on top of OsmAnd's AIDL API v2. */
class OsmAndTools(private val bridge: OsmAndBridge) {

	private class Tool(
		val name: String,
		val description: String,
		val schema: JSONObject,
		val run: (JSONObject) -> Any
	)

	/** The OsmAnd app the connector drives, as the user sees it in the launcher. */
	private fun osmandApp(): String {
		val pack = bridge.boundPackage ?: bridge.preferredPackage
		return "${ConnectorSettings.OSMAND_PACKAGES[pack] ?: "OsmAnd"} ($pack)"
	}

	private fun refused(group: String) = "OsmAnd refused the call. It needs the '$group' permission group for " +
			"OsmAnd AI Connector in ${osmandApp()}: ask the user to open that app > Menu > Plugins > " +
			"OsmAnd AI Connector and turn on '$group'. Each OsmAnd app on the phone keeps its own permissions. " +
			"The arguments may also be invalid."

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
			check(SETTINGS, bridge.get().selectProfile(SelectProfileParams(a.getString("profile"))))
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
				prop("rotation", "number", "Optional map rotation in degrees, 0 is north up"),
				prop("animated", "boolean", "Animate the move"),
				required = listOf("lat", "lon")
			)
		) { a ->
			check(MAP,
				bridge.get().setMapLocation(
					SetMapLocationParams(
						a.getDouble("lat"), a.getDouble("lon"), a.optInt("zoom", 0), a.optFloat("rotation"),
						a.optBoolean("animated", true)
					)
				)
			)
			"Map moved"
		},
		Tool(
			"osmand_set_camera",
			"Point the map camera: center, fractional zoom, rotation and tilt (3D). Omitted values stay as they are. " +
					"For a 3D view of mountains set elevation_angle to 30-50 and turn on terrain " +
					"(osmand_set_preference: terrain / 3D relief settings).",
			schema(
				prop("lat", "number", "Latitude of the map center"),
				prop("lon", "number", "Longitude of the map center"),
				prop("zoom", "number", "Zoom, may be fractional, e.g. 14.5"),
				prop("rotation", "number", "Map rotation in degrees: the direction that is up on the screen, 0 is north"),
				prop("elevation_angle", "number", "Camera angle above the horizon: 90 is a flat map, 30-50 a tilted 3D view"),
				prop("animated", "boolean", "Animate the move, default false")
			)
		) { a ->
			val hasCenter = a.has("lat") && a.has("lon")
			check(MAP, bridge.get().setMapCamera(
				SetMapCameraParams(
					if (hasCenter) a.getDouble("lat") else Double.NaN, if (hasCenter) a.getDouble("lon") else Double.NaN,
					a.optFloat("zoom"), a.optFloat("rotation"), a.optFloat("elevation_angle"),
					a.optBoolean("animated", false)
				)
			))
			"Camera set"
		},
		Tool(
			"osmand_screenshot",
			"Take a picture of the OsmAnd map screen as the user sees it: map, shown tracks, 3D and widgets. " +
					"OsmAnd must be open on the phone. Use it to check the view or to make cards and pictures for the user.",
			schema(
				prop("map_only", "boolean", "Only the map with tracks and 3D, without widgets and buttons; default false"),
				prop("max_width", "integer", "Max width in pixels, default 1080"),
				prop("quality", "integer", "JPEG quality 1-100, default 80")
			)
		) { a ->
			val params = MapScreenshotParams(a.optInt("max_width", 1080), a.optInt("quality", 80))
			params.isMapOnly = a.optBoolean("map_only", false)
			val shot = bridge.get().getMapScreenshot(params)
				?: throw ToolError(refused(SCREEN) + " OsmAnd also needs to be open with the map on the screen.")
			ToolImage(shot.image, "Map screenshot ${shot.width}x${shot.height}")
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
			check(NAVIGATION,
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
			check(NAVIGATION,
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
			check(NAVIGATION, bridge.get().stopNavigation(StopNavigationParams()))
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
			check(FAVORITES, bridge.get().addFavorite(AddFavoriteParams(fav)))
			"Favorite added"
		},
		Tool(
			"osmand_refresh_map",
			"Redraw the map, e.g. after changing settings that a layer does not pick up by itself.",
			schema()
		) {
			check(MAP, bridge.get().refreshMap())
			"Map refreshed"
		},
		Tool(
			"osmand_get_active_gpx",
			"List tracks (GPX) currently shown on the map: file name relative to the tracks folder, size, modified time.",
			schema()
		) {
			val list = ArrayList<ASelectedGpxFile>()
			check(TRACKS_VIEW, bridge.get().getActiveGpx(list))
			JSONArray(list.map {
				JSONObject().put("file", it.fileName).put("size", it.fileSize).put("modified", it.modifiedTime)
			})
		},
		Tool(
			"osmand_show_gpx",
			"Show a track on the map. File name relative to the tracks folder, e.g. rec/2026-09-27_13-43_Sun.gpx or a file from tracks/import.",
			schema(prop("file", "string", "Track file name"), required = listOf("file"))
		) { a ->
			check(TRACKS_EDIT, bridge.get().showGpx(ShowGpxParams(a.getString("file"))))
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
		Tool(
			"osmand_list_tracks",
			"Find track files (GPX) on the phone with their statistics from OsmAnd's track database: activity, " +
					"start, city, distance, duration, moving time, climb, descent, elevation range, max and average speed. " +
					"Filters can be combined, e.g. ski days: min_elevation_range_m 200, min_descent_m 500, " +
					"min_max_speed_kmh 30, max_max_speed_kmh 80, max_avg_speed_kmh 30. The reply lists the activities in use; " +
					"many recorded tracks have no activity, so filter by the numbers too.",
			schema(
				prop("query", "string", "Part of the file path, e.g. 2026-09 or Bukovel"),
				prop("folder", "string", "Folder in the tracks folder, e.g. rec or import"),
				prop("activity", "string", "Activity id from the reply's activities, e.g. skiing"),
				prop("from", "string", "Start date, YYYY-MM-DD"),
				prop("to", "string", "End date, YYYY-MM-DD (inclusive)"),
				prop("min_km", "number", "Min distance, km"),
				prop("max_km", "number", "Max distance, km"),
				prop("shown_only", "boolean", "Only tracks shown on the map"),
				prop("min_descent_m", "number", "Min total descent, m"),
				prop("min_elevation_range_m", "number", "Min difference between the highest and lowest point, m"),
				prop("min_max_speed_kmh", "number", "Min of the track's max speed, km/h"),
				prop("max_max_speed_kmh", "number", "Max of the track's max speed, km/h"),
				prop("max_avg_speed_kmh", "number", "Upper limit of the average speed over the whole time, km/h"),
				prop("sort", "string", "newest (default), oldest, longest or name"),
				prop("offset", "integer", "Skip this many tracks, for paging"),
				prop("limit", "integer", "Max tracks, default 20, max 100")
			)
		) { a -> listTracks(a) },
		Tool(
			"osmand_track_stats",
			"Full statistics of a track file on the phone: activity, city, start point, distance, times, " +
					"elevation (min, max, average, climb, descent), speed, points, waypoints and sensor data " +
					"(heart rate, power, cadence, sensor speed, temperature) when recorded.",
			schema(prop("file", "string", "Track file from osmand_list_tracks"), required = listOf("file"))
		) { a -> trackStats(a.getString("file")) },
		Tool(
			"osmand_track_points",
			"Track points of a track file on the phone or of the track being recorded (file omitted): " +
					"lat, lon, elevation (m), time (ms), speed (m/s), in pages. Use it to analyse runs, climbs, stops.",
			schema(
				prop("file", "string", "Track file from osmand_list_tracks; omit for the track being recorded"),
				prop("offset", "integer", "Index of the first point, default 0"),
				prop("limit", "integer", "Points per page, default 500, max 2000")
			)
		) { a -> trackPoints(a) },
		Tool(
			"osmand_widgets",
			"Values of the map widgets enabled in the current profile, as shown on the screen " +
					"(speed, altitude, distance to destination, time...).",
			schema()
		) {
			val list = ArrayList<AMapWidgetValue>()
			check(LOCATION, bridge.get().getMapWidgetValues(list))
			JSONArray(list.map {
				JSONObject().put("id", it.id).put("title", it.title).put("value", it.value)
					.put("panel", it.panel).put("visible", it.isVisible)
			})
		},
		Tool(
			"osmand_start_recording",
			"Start trip recording (a new GPX track from the phone's location).",
			schema()
		) {
			if (!bridge.get().startGpxRecording(StartGpxRecordingParams())) throw ToolError(recordingRefused())
			"Trip recording started"
		},
		Tool(
			"osmand_recording_status",
			"Whether OsmAnd records a track now and the current track so far: distance, duration, points, " +
					"time of the last point. Also tells whether the Trip recording plugin is on.",
			schema()
		) { recordingStatus() },
		Tool(
			"osmand_stop_recording",
			"Stop trip recording. The track is saved to the rec folder.",
			schema()
		) {
			if (!bridge.get().stopGpxRecording(StopGpxRecordingParams())) throw ToolError(recordingRefused())
			"Trip recording stopped"
		},
		Tool("osmand_list_quick_actions", "List configured quick actions with their numbers.", schema()) {
			val list = ArrayList<QuickActionInfoParams>()
			check(SETTINGS, bridge.get().getQuickActionsInfo(list))
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
			check(SETTINGS, bridge.get().executeQuickAction(QuickActionParams(a.getInt("number"))))
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
			if (out is ToolImage) {
				return JSONObject().put("isError", false).put("content", JSONArray()
					.put(JSONObject().put("type", "image").put("mimeType", "image/jpeg")
						.put("data", Base64.encodeToString(out.jpeg, Base64.NO_WRAP)))
					.put(JSONObject().put("type", "text").put("text", out.caption)))
			}
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

	private fun check(group: String, ok: Boolean) {
		if (!ok) throw ToolError(refused(group))
	}

	private fun readPref(id: String, profile: String?): String? {
		val p = PreferenceParams(id)
		if (profile != null) p.appModeKey = profile
		return if (bridge.get().getPreference(p)) p.value else null
	}

	private fun profiles(): JSONArray {
		val list = ArrayList<AProfile>()
		check(SETTINGS, bridge.get().getProfiles(list))
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
			.put("osmand_app", osmandApp())
		// refreshMap is in the Map group that every connected app gets by default
		if (!api.refreshMap()) {
			o.put("enabled", false).put("hint", refused(MAP))
			return o
		}
		// groups that have a call without side effects; the others are checked when a tool is called
		o.put("groups", JSONObject()
			.put(MAP, true)
			.put(SETTINGS, readPref("application_mode", null) != null)
			.put(TRACKS_VIEW, api.getActiveGpx(ArrayList()))
			.put(RECORDING, api.gpxRecordingInfo != null)
			.put(LOCATION, api.appInfo != null))
		o.put("enabled", true)
			.put("screen_open", api.isFragmentOpen)
			.put("context_menu_open", api.isMenuOpen)
		// profiles are read through settings, which needs the Settings group
		val current = readPref("application_mode", null)
		if (current == null) {
			return o.put("settings_access", false)
				.put("settings_hint", refused(SETTINGS))
		}
		return o.put("settings_access", true)
			.put("current_profile", current)
			.put("profiles", profiles())
	}

	// OsmAnd also refuses recording when its Trip recording plugin is off
	private fun recordingRefused(): String {
		val info = bridge.get().gpxRecordingInfo
		if (info != null && !info.isPluginEnabled) {
			return "The Trip recording plugin is off in ${osmandApp()}: ask the user to turn it on in Menu > Plugins."
		}
		return refused(RECORDING) + " Recording also needs the Trip recording plugin turned on in OsmAnd > Menu > Plugins."
	}

	private fun recordingStatus(): JSONObject {
		// null when the group is off, or from an OsmAnd without this call
		val info = bridge.get().gpxRecordingInfo
			?: throw ToolError(refused(RECORDING) + " An older OsmAnd cannot report recording at all.")
		return JSONObject().put("recording", info.isRecording).put("plugin_enabled", info.isPluginEnabled)
			.put("distance_m", info.distance.toInt()).put("duration_s", info.duration / 1000)
			.put("points", info.points).put("last_point_time", info.lastPointTime)
	}

	private fun listTracks(a: JSONObject): JSONObject {
		val p = GpxSearchParams()
		p.setQuery(a.optString("query").ifEmpty { null })
		p.setFolder(a.optString("folder").ifEmpty { null })
		p.setActivityType(a.optString("activity").ifEmpty { null })
		p.setTimeRange(dayBound(a.optString("from"), false), dayBound(a.optString("to"), true))
		p.setDistanceRange(a.optDouble("min_km", 0.0) * 1000, a.optDouble("max_km", 0.0) * 1000)
		p.setShownOnly(a.optBoolean("shown_only", false))
		p.setMinDescent(a.optDouble("min_descent_m", 0.0))
		p.setMinElevationRange(a.optDouble("min_elevation_range_m", 0.0))
		p.setMaxSpeedRange(kmh(a, "min_max_speed_kmh"), kmh(a, "max_max_speed_kmh"))
		p.setMaxAvgSpeed(kmh(a, "max_avg_speed_kmh"))
		p.setSort(a.optString("sort").ifEmpty { GpxSearchParams.SORT_NEWEST })
		p.setPage(a.optInt("offset", 0), a.optInt("limit", 20).coerceIn(1, 100))
		val result = bridge.get().searchGpx(p)
			?: throw ToolError(refused(TRACKS_VIEW) + " An older OsmAnd cannot search tracks.")
		return JSONObject().put("total", result.total).put("found", result.found)
			.put("activities", JSONArray(result.activityTypes.orEmpty()))
			.put("tracks", JSONArray(result.files.orEmpty().map { trackJson(it, false) }))
	}

	private fun kmh(a: JSONObject, name: String) = (a.optDouble(name, 0.0) / 3.6).toFloat()

	/** Start of the day in the phone's time zone, or its last millisecond for [end]; 0 when empty. */
	private fun dayBound(date: String, end: Boolean): Long {
		if (date.isEmpty()) return 0
		val day = try {
			LocalDate.parse(date)
		} catch (e: Exception) {
			throw ToolError("Dates are YYYY-MM-DD, got '$date'")
		}
		val start = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
		return if (end) day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1 else start
	}

	// OsmAnd leaves start values (e.g. min elevation 99999) when a track has no times or elevations
	private fun trackJson(file: AGpxFile, full: Boolean): JSONObject {
		val o = JSONObject().put("file", file.relativePath ?: file.fileName)
		file.activityType?.takeIf { it.isNotEmpty() }?.let { o.put("activity", it) }
		file.nearestCityName?.takeIf { it.isNotEmpty() }?.let { o.put("city", it) }
		if (full) {
			o.put("size", file.fileSize).put("modified", file.modifiedTime)
			if (!file.startLatitude.isNaN()) o.put("start_lat", file.startLatitude).put("start_lon", file.startLongitude)
		}
		o.put("shown", file.isActive)
		val d = file.details ?: return o
		o.put("distance_km", round1(d.totalDistance / 1000.0))
		if (d.timeSpan > 0) {
			o.put("start", Instant.ofEpochMilli(d.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime().toString())
				.put("duration_min", d.timeSpan / 60000).put("moving_min", d.timeMoving / 60000)
				.put("speed_max_kmh", round1(d.maxSpeed * 3.6))
				// over the whole time, stops and lifts included
				.put("speed_avg_total_kmh", round1(d.totalDistance / (d.timeSpan / 1000.0) * 3.6))
			if (full) {
				o.put("end", Instant.ofEpochMilli(d.endTime).atZone(ZoneId.systemDefault()).toLocalDateTime().toString())
					.put("distance_moving_km", round1(d.totalDistanceMoving / 1000.0))
					.put("speed_avg_moving_kmh", round1(d.avgSpeed * 3.6))
			}
		}
		if (d.maxElevation >= d.minElevation) {
			o.put("climb_m", d.diffElevationUp.toInt()).put("descent_m", d.diffElevationDown.toInt())
				.put("elevation_range_m", (d.maxElevation - d.minElevation).toInt())
			if (full) {
				o.put("elevation_min_m", d.minElevation.toInt())
					.put("elevation_max_m", d.maxElevation.toInt()).put("elevation_avg_m", d.avgElevation.toInt())
			}
		}
		if (full) {
			o.put("points", d.points).put("segments", d.totalTracks).put("waypoints", d.wptPoints)
			val sensors = JSONObject()
			if (d.maxHeartRate > 0) sensors.put("heart_rate_avg", round1(d.avgHeartRate.toDouble()))
				.put("heart_rate_min", d.minHeartRate).put("heart_rate_max", d.maxHeartRate)
			if (d.maxSensorSpeed > 0) sensors.put("sensor_speed_avg_kmh", round1(d.avgSensorSpeed * 3.6))
				.put("sensor_speed_max_kmh", round1(d.maxSensorSpeed * 3.6))
			if (d.maxPower > 0) sensors.put("power_avg_w", round1(d.avgPower.toDouble())).put("power_max_w", d.maxPower)
			if (d.maxCadence > 0) sensors.put("cadence_avg", round1(d.avgCadence.toDouble()))
				.put("cadence_max", round1(d.maxCadence.toDouble()))
			if (d.maxTemperature != 0) sensors.put("temperature_avg_c", round1(d.avgTemperature.toDouble()))
				.put("temperature_max_c", d.maxTemperature)
			if (sensors.length() > 0) o.put("sensors", sensors)
		}
		return o
	}

	private fun trackStats(file: String): JSONObject {
		val p = GpxSearchParams()
		p.setQuery(file)
		p.setPage(0, 100)
		val result = bridge.get().searchGpx(p)
			?: throw ToolError(refused(TRACKS_VIEW) + " An older OsmAnd cannot search tracks.")
		val track = result.files.orEmpty().firstOrNull { it.relativePath == file || it.fileName == file }
			?: throw ToolError("No track '$file' on the phone; see osmand_list_tracks")
		return trackJson(track, true)
	}

	private fun round1(v: Double) = Math.round(v * 10) / 10.0

	private fun trackPoints(a: JSONObject): JSONObject {
		val file = a.optString("file")
		val offset = a.optInt("offset", 0)
		val limit = a.optInt("limit", 500).coerceIn(1, 2000)
		val page = bridge.get().getGpxPoints(GpxPointsParams(file, offset, limit))
			?: throw ToolError(if (file.isEmpty()) refused(RECORDING) else
				refused(TRACKS_VIEW) + " Or there is no track '$file'; see osmand_list_tracks.")
		// columns keep a page of points small
		return JSONObject().put("total", page.total).put("offset", offset)
			.put("columns", JSONArray(listOf("lat", "lon", "ele_m", "time_ms", "speed_ms")))
			.put("points", JSONArray(page.points.orEmpty().map {
				JSONArray().put(it.latitude).put(it.longitude)
					.put(if (it.elevation.isNaN()) JSONObject.NULL else Math.round(it.elevation * 10) / 10.0)
					.put(it.time).put(Math.round(it.speed * 100) / 100.0)
			}))
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
		check(SEARCH, bridge.get().search(p, callback))
		if (!latch.await(30, TimeUnit.SECONDS)) throw ToolError("Search timed out after 30 s")
		return JSONArray(results.take(limit).map {
			JSONObject().put("name", it.localName).put("type", it.localTypeName)
				.put("lat", it.latitude).put("lon", it.longitude)
				.apply { if (!it.alternateName.isNullOrEmpty()) put("alt_name", it.alternateName) }
		})
	}

	private fun JSONObject.optFloat(name: String): Float = if (has(name)) getDouble(name).toFloat() else Float.NaN

	private fun prop(name: String, type: String, description: String, items: String? = null) =
		name to JSONObject().put("type", type).put("description", description).apply {
			if (items != null) put("items", JSONObject().put("type", items))
		}

	private fun schema(vararg props: Pair<String, JSONObject>, required: List<String> = emptyList()) =
		JSONObject().put("type", "object")
			.put("properties", JSONObject().apply { props.forEach { put(it.first, it.second) } })
			.put("required", JSONArray(required))
}

private const val MAP = "Map"
private const val LOCATION = "My Position"
private const val SCREEN = "Screenshots"
private const val SEARCH = "Search"
private const val NAVIGATION = "Navigation"
private const val FAVORITES = "Favorites"
private const val TRACKS_VIEW = "Tracks: view"
private const val TRACKS_EDIT = "Tracks: edit"
private const val RECORDING = "Trip recording"
private const val SETTINGS = "Settings"

private const val PREF_HINT = "Common ids: application_mode (current profile, global), daynight_mode (DAY, NIGHT, AUTO, SENSOR, APP_THEME), " +
		"renderer (map style, e.g. OsmAnd, Touring view (contrast and details), Topo, UniRS, Nautical, Ski map, Winter and ski, Offroad, Desert), " +
		"map_preferred_locale (map labels language, empty = local names), rotate_map (0 none, 1 bearing, 2 compass, 3 manual), " +
		"auto_zoom_map_on_off, voice_mute, show_routing_alarms, metric_system, driving_region, show_poi_label, map_density, text_scale. " +
		"Render style properties use the nrenderer_ prefix, e.g. nrenderer_contourLines, nrenderer_showCycleRoutes."
