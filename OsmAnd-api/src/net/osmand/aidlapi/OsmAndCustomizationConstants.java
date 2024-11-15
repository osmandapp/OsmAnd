package net.osmand.aidlapi;

public interface OsmAndCustomizationConstants {

	// Navigation Drawer:
	String DRAWER_ITEM_ID_SCHEME = "drawer.action.";
	String DRAWER_SWITCH_PROFILE_ID = DRAWER_ITEM_ID_SCHEME + "switch_profile";
	String DRAWER_CONFIGURE_PROFILE_ID = DRAWER_ITEM_ID_SCHEME + "configure_profile";
	String DRAWER_DASHBOARD_ID = DRAWER_ITEM_ID_SCHEME + "dashboard";
	String DRAWER_MAP_MARKERS_ID = DRAWER_ITEM_ID_SCHEME + "map_markers";
	String DRAWER_MY_PLACES_ID = DRAWER_ITEM_ID_SCHEME + "my_places";
	String DRAWER_FAVORITES_ID = DRAWER_ITEM_ID_SCHEME + "favorites";
	String DRAWER_TRACKS_ID = DRAWER_ITEM_ID_SCHEME + "tracks";
	String DRAWER_AV_NOTES_ID = DRAWER_ITEM_ID_SCHEME + "av_notes";
	String DRAWER_OSM_EDITS_ID = DRAWER_ITEM_ID_SCHEME + "osm_edits";
	String DRAWER_WEATHER_FORECAST_ID = DRAWER_ITEM_ID_SCHEME + "weather_forecast";
	String DRAWER_ANT_PLUS_ID = DRAWER_ITEM_ID_SCHEME + "ant_plus";
	String DRAWER_BACKUP_RESTORE_ID = DRAWER_ITEM_ID_SCHEME + "backup_restore";
	String DRAWER_SEARCH_ID = DRAWER_ITEM_ID_SCHEME + "search";
	String DRAWER_DIRECTIONS_ID = DRAWER_ITEM_ID_SCHEME + "directions";
	String DRAWER_TRIP_RECORDING_ID = DRAWER_ITEM_ID_SCHEME + "trip_recording";
	String DRAWER_CONFIGURE_MAP_ID = DRAWER_ITEM_ID_SCHEME + "configure_map";
	String DRAWER_DOWNLOAD_MAPS_ID = DRAWER_ITEM_ID_SCHEME + "download_maps";
	String DRAWER_LIVE_UPDATES_ID = DRAWER_ITEM_ID_SCHEME + "live_updates";
	String DRAWER_OSMAND_LIVE_ID = DRAWER_ITEM_ID_SCHEME + "osmand_live";
	String DRAWER_TRAVEL_GUIDES_ID = DRAWER_ITEM_ID_SCHEME + "travel_guides";
	String DRAWER_MEASURE_DISTANCE_ID = DRAWER_ITEM_ID_SCHEME + "measure_distance";
	String DRAWER_CONFIGURE_SCREEN_ID = DRAWER_ITEM_ID_SCHEME + "configure_screen";
	String DRAWER_PLUGINS_ID = DRAWER_ITEM_ID_SCHEME + "plugins";
	String DRAWER_SETTINGS_ID = DRAWER_ITEM_ID_SCHEME + "settings.new";
	String DRAWER_HELP_ID = DRAWER_ITEM_ID_SCHEME + "help";
	String DRAWER_BUILDS_ID = DRAWER_ITEM_ID_SCHEME + "builds";
	String DRAWER_DIVIDER_ID = DRAWER_ITEM_ID_SCHEME + "divider";
	String DRAWER_OSMAND_VERSION_ID = DRAWER_ITEM_ID_SCHEME + "osmand_version";
	String DRAWER_VEHICLE_METRICS_ID = DRAWER_ITEM_ID_SCHEME + "vehicle_metrics";

	// Configure Map:
	String CONFIGURE_MAP_ITEM_ID_SCHEME = "map.configure.";
	String APP_PROFILES_ID = CONFIGURE_MAP_ITEM_ID_SCHEME + "app_profiles";

	String SHOW_ITEMS_ID_SCHEME = CONFIGURE_MAP_ITEM_ID_SCHEME + "show.";
	String SHOW_CATEGORY_ID = SHOW_ITEMS_ID_SCHEME + "category";
	String FAVORITES_ID = SHOW_ITEMS_ID_SCHEME + "favorites";
	String POI_OVERLAY_ID = SHOW_ITEMS_ID_SCHEME + "poi_overlay";
	String POI_OVERLAY_LABELS_ID = SHOW_ITEMS_ID_SCHEME + "poi_overlay_labels";
	String TRANSPORT_ID = SHOW_ITEMS_ID_SCHEME + "transport";
	String GPX_FILES_ID = SHOW_ITEMS_ID_SCHEME + "gpx_files";
	String WIKIPEDIA_ID = SHOW_ITEMS_ID_SCHEME + "wikipedia";
	String MAP_MARKERS_ID = SHOW_ITEMS_ID_SCHEME + "map_markers";
	String MAP_SOURCE_ID = SHOW_ITEMS_ID_SCHEME + "map_source";
	String MAP_BORDERS_ID = SHOW_ITEMS_ID_SCHEME + "map_borders";
	String RECORDING_LAYER = SHOW_ITEMS_ID_SCHEME + "recording_layer";
	String SHOW_DEPTH_CONTOURS = SHOW_ITEMS_ID_SCHEME + "depth_contours";
	String MAPILLARY = SHOW_ITEMS_ID_SCHEME + "mapillary";
	String OVERLAY_MAP = SHOW_ITEMS_ID_SCHEME + "overlay_map";
	String UNDERLAY_MAP = SHOW_ITEMS_ID_SCHEME + "underlay_map";
	String WEATHER_ID = SHOW_ITEMS_ID_SCHEME + "weather";

	String TERRAIN_ITEMS_ID_SCHEME = CONFIGURE_MAP_ITEM_ID_SCHEME + "terrain.";
	String TERRAIN_ID = TERRAIN_ITEMS_ID_SCHEME + "terrain";
	String TERRAIN_PROMO_ID = TERRAIN_ITEMS_ID_SCHEME + "promo";
	String TERRAIN_CATEGORY_ID = TERRAIN_ITEMS_ID_SCHEME + "category";
	String TERRAIN_DESCRIPTION_ID = TERRAIN_ITEMS_ID_SCHEME + "description";
	String CONTOUR_LINES = TERRAIN_ITEMS_ID_SCHEME + "contour_lines";
	String RELIEF_3D_ID = TERRAIN_ITEMS_ID_SCHEME + "relief_3d";
	String TERRAIN_DEPTH_CONTOURS = TERRAIN_ITEMS_ID_SCHEME + "depth_contours";

	String OPEN_STREET_MAP_ITEMS_ID_SCHEME = CONFIGURE_MAP_ITEM_ID_SCHEME + "open_street_map.";
	String OSM_NOTES = OPEN_STREET_MAP_ITEMS_ID_SCHEME + "osm_notes";
	String OSM_EDITS = OPEN_STREET_MAP_ITEMS_ID_SCHEME + "osm_edits";
	String OPEN_STREET_MAP_CATEGORY_ID = OPEN_STREET_MAP_ITEMS_ID_SCHEME + "category";

	String ROUTES_ITEMS_ID_SCHEME = CONFIGURE_MAP_ITEM_ID_SCHEME + "routes.";
	String ROUTES_CATEGORY_ID = ROUTES_ITEMS_ID_SCHEME + "category";

	String RENDERING_ITEMS_ID_SCHEME = CONFIGURE_MAP_ITEM_ID_SCHEME + "rendering.";
	String CUSTOM_RENDERING_ITEMS_ID_SCHEME = RENDERING_ITEMS_ID_SCHEME + "custom.";
	String MAP_RENDERING_CATEGORY_ID = RENDERING_ITEMS_ID_SCHEME + "category";
	String MAP_STYLE_ID = RENDERING_ITEMS_ID_SCHEME + "map_style";
	String MAP_MODE_ID = RENDERING_ITEMS_ID_SCHEME + "map_mode";
	String MAP_MAGNIFIER_ID = RENDERING_ITEMS_ID_SCHEME + "map_magnifier";
	String ROAD_STYLE_ID = RENDERING_ITEMS_ID_SCHEME + "road_style";
	String TEXT_SIZE_ID = RENDERING_ITEMS_ID_SCHEME + "text_size";
	String MAP_LANGUAGE_ID = RENDERING_ITEMS_ID_SCHEME + "map_language";
	String DETAILS_ID = RENDERING_ITEMS_ID_SCHEME + "details";
	String HIDE_ID = RENDERING_ITEMS_ID_SCHEME + "hide";

	// Map Controls:
	String HUD_BTN_ID_SCHEME = "map.view.";
	String LAYERS_HUD_ID = HUD_BTN_ID_SCHEME + "layers";
	String COMPASS_HUD_ID = HUD_BTN_ID_SCHEME + "compass";
	String QUICK_SEARCH_HUD_ID = HUD_BTN_ID_SCHEME + "quick_search";
	String BACK_TO_LOC_HUD_ID = HUD_BTN_ID_SCHEME + "back_to_loc";
	String MAP_3D_HUD_ID = HUD_BTN_ID_SCHEME + "map_3d";
	String MENU_HUD_ID = HUD_BTN_ID_SCHEME + "menu";
	String ROUTE_PLANNING_HUD_ID = HUD_BTN_ID_SCHEME + "route_planning";
	String ZOOM_IN_HUD_ID = HUD_BTN_ID_SCHEME + "zoom_id";
	String ZOOM_OUT_HUD_ID = HUD_BTN_ID_SCHEME + "zoom_out";
	String QUICK_ACTION_HUD_ID = HUD_BTN_ID_SCHEME + "quick_action";

	//Map Context Menu Actions:
	String MAP_CONTEXT_MENU_ACTIONS = "point.actions.";
	String MAP_CONTEXT_MENU_ADD_ID = MAP_CONTEXT_MENU_ACTIONS + "add";
	String MAP_CONTEXT_MENU_MARKER_ID = MAP_CONTEXT_MENU_ACTIONS + "marker";
	String MAP_CONTEXT_MENU_SHARE_ID = MAP_CONTEXT_MENU_ACTIONS + "share";
	String MAP_CONTEXT_MENU_MORE_ID = MAP_CONTEXT_MENU_ACTIONS + "more";
	String MAP_CONTEXT_MENU_DIRECTIONS_FROM_ID = MAP_CONTEXT_MENU_ACTIONS + "directions_from";
	String MAP_CONTEXT_MENU_SEARCH_NEARBY = MAP_CONTEXT_MENU_ACTIONS + "search_nearby";
	String MAP_CONTEXT_MENU_CHANGE_MARKER_POSITION = MAP_CONTEXT_MENU_ACTIONS + "change_m_position";
	String MAP_CONTEXT_MENU_MARK_AS_PARKING_LOC = MAP_CONTEXT_MENU_ACTIONS + "mark_as_parking";
	String MAP_CONTEXT_MENU_MEASURE_DISTANCE = MAP_CONTEXT_MENU_ACTIONS + "measure_distance";
	String MAP_CONTEXT_MENU_AVOID_ROAD = MAP_CONTEXT_MENU_ACTIONS + "avoid_road";
	String MAP_CONTEXT_MENU_EDIT_GPX_WP = MAP_CONTEXT_MENU_ACTIONS + "edit_gpx_waypoint";
	String MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT = MAP_CONTEXT_MENU_ACTIONS + "add_gpx_waypoint";
	String MAP_CONTEXT_MENU_UPDATE_MAP = MAP_CONTEXT_MENU_ACTIONS + "update_map";
	String MAP_CONTEXT_MENU_DOWNLOAD_MAP = MAP_CONTEXT_MENU_ACTIONS + "download_map";
	String MAP_CONTEXT_MENU_MODIFY_POI = MAP_CONTEXT_MENU_ACTIONS + "modify_poi";
	String MAP_CONTEXT_MENU_MODIFY_OSM_CHANGE = MAP_CONTEXT_MENU_ACTIONS + "modify_osm_change";
	String MAP_CONTEXT_MENU_CREATE_POI = MAP_CONTEXT_MENU_ACTIONS + "create_poi";
	String MAP_CONTEXT_MENU_MODIFY_OSM_NOTE = MAP_CONTEXT_MENU_ACTIONS + "modify_osm_note";
	String MAP_CONTEXT_MENU_OPEN_OSM_NOTE = MAP_CONTEXT_MENU_ACTIONS + "open_osm_note";
	String MAP_CONTEXT_MENU_AUDIO_NOTE = MAP_CONTEXT_MENU_ACTIONS + "audio_note";
	String MAP_CONTEXT_MENU_VIDEO_NOTE = MAP_CONTEXT_MENU_ACTIONS + "video_note";
	String MAP_CONTEXT_MENU_PHOTO_NOTE = MAP_CONTEXT_MENU_ACTIONS + "photo_note";

	//Plug-in's IDs:
	String PLUGIN_OSMAND_MONITORING = "osmand.monitoring";
	String PLUGIN_MAPILLARY = "osmand.mapillary";
	String PLUGIN_OSMAND_DEV = "osmand.development";
	String PLUGIN_AUDIO_VIDEO_NOTES = "osmand.audionotes";
	String PLUGIN_NAUTICAL = "nauticalPlugin.plugin";
	String PLUGIN_OSMAND_EDITING = "osm.editing";
	String PLUGIN_PARKING_POSITION = "osmand.parking.position";
	String PLUGIN_RASTER_MAPS = "osmand.rastermaps";
	String PLUGIN_SKI_MAPS = "skimaps.plugin";
	String PLUGIN_SRTM = "osmand.srtm.paid";
	String PLUGIN_ACCESSIBILITY = "osmand.accessibility";
	String PLUGIN_WIKIPEDIA = "osmand.wikipedia";
	String PLUGIN_ANT_PLUS = "osmand.antplus";
	String PLUGIN_VEHICLE_METRICS = "osmand.vehicle.metrics";
	String PLUGIN_WEATHER = "osmand.weather";

	//Settings:
	String SETTINGS_ID = "settings.";
	String SETTINGS_MAIN_ID = SETTINGS_ID + "main_settings";
	String SETTINGS_GLOBAL_ID = SETTINGS_ID + "global_settings";
	String SETTINGS_CONFIGURE_PROFILE_ID = SETTINGS_ID + "configure_profile";
	String SETTINGS_PROXY_ID = SETTINGS_ID + "enable_proxy";
	String SETTINGS_GENERAL_PROFILE_ID = SETTINGS_ID + "general_settings";
	String SETTINGS_NAVIGATION_ID = SETTINGS_ID + "navigation_settings";
	String SETTINGS_COORDINATES_FORMAT_ID = SETTINGS_ID + "coordinates_format";
	String SETTINGS_ROUTE_PARAMETERS_ID = SETTINGS_ID + "route_parameters";
	String SETTINGS_SCREEN_ALERTS_ID = SETTINGS_ID + "show_routing_alarms";
	String SETTINGS_VOICE_ANNOUNCES_ID = SETTINGS_ID + "voice_mute";
	String SETTINGS_VEHICLE_PARAMETERS_ID = SETTINGS_ID + "vehicle_parameters";
	String SETTINGS_MAP_DURING_NAVIGATION_ID = SETTINGS_ID + "map_during_navigation";
	String SETTINGS_TURN_SCREEN_ON_ID = SETTINGS_ID + "screen_control";
	String SETTINGS_DATA_STORAGE_ID = SETTINGS_ID + "external_storage_dir";
	String SETTINGS_DIALOGS_AND_NOTIFICATIONS_ID = SETTINGS_ID + "dialogs_and_notifications";
	String SETTINGS_HISTORY_ID = SETTINGS_ID + "history";
	String SETTINGS_PROFILE_APPEARANCE_ID = SETTINGS_ID + "profile_appearance";
	String SETTINGS_OPEN_STREET_MAP_EDITING_ID = SETTINGS_ID + "open_street_map_editing";
	String SETTINGS_MULTIMEDIA_NOTES_ID = SETTINGS_ID + "multimedia_notes";
	String SETTINGS_MONITORING_ID = SETTINGS_ID + "monitoring_settings";
	String SETTINGS_LIVE_MONITORING_ID = SETTINGS_ID + "live_monitoring";
	String SETTINGS_ACCESSIBILITY_ID = SETTINGS_ID + "accessibility_settings";
	String SETTINGS_OPEN_PLACE_REVIEWS_ID = SETTINGS_ID + "open_place_reviews";
	String SETTINGS_DEVELOPMENT_ID = SETTINGS_ID + "development_settings";
	String SETTINGS_BACKUP_AND_RESTORE_ID = SETTINGS_ID + "backup_and_restore";

	//Navigation Options:
	String NAVIGATION_OPTIONS_ID = "navigation.options.";
	String NAVIGATION_OPTIONS_MENU_ID = NAVIGATION_OPTIONS_ID + "options_menu";
	String NAVIGATION_SOUND_ID = NAVIGATION_OPTIONS_ID + "mute_sound";
	String NAVIGATION_ROUTE_SIMULATION_ID = NAVIGATION_OPTIONS_ID + "route_simulation";
	String NAVIGATION_TIME_CONDITIONAL_ID = NAVIGATION_OPTIONS_ID + "time_conditional";
	String NAVIGATION_SHOW_ALONG_THE_ROUTE_ID = NAVIGATION_OPTIONS_ID + "show_along_the_route";
	String NAVIGATION_AVOID_ROADS_ID = NAVIGATION_OPTIONS_ID + "avoid_roads_routing";
	String NAVIGATION_AVOID_PT_TYPES_ID = NAVIGATION_OPTIONS_ID + "avoid_pt_types_routing";
	String NAVIGATION_FOLLOW_TRACK_ID = NAVIGATION_OPTIONS_ID + "follow_track_routing";
	String NAVIGATION_OTHER_SETTINGS_ID = NAVIGATION_OPTIONS_ID + "other_settings_routing";
	String NAVIGATION_CUSTOMIZE_ROUTE_LINE_ID = NAVIGATION_OPTIONS_ID + "customize_route_line_routing";
	String NAVIGATION_INTERRUPT_MUSIC_ID = NAVIGATION_OPTIONS_ID + "interrupt_music_routing";
	String NAVIGATION_VOICE_GUIDANCE_ID = NAVIGATION_OPTIONS_ID + "voice_guidance_routing";
	String NAVIGATION_OTHER_LOCAL_ROUTING_ID = NAVIGATION_OPTIONS_ID + "other_local_routing";
	String NAVIGATION_LOCAL_ROUTING_GROUP_ID = NAVIGATION_OPTIONS_ID + "local_routing_group";
	String NAVIGATION_LOCAL_ROUTING_ID = NAVIGATION_OPTIONS_ID + "local_routing";
	String NAVIGATION_DIVIDER_ID = NAVIGATION_OPTIONS_ID + "divider";
	String NAVIGATION_APP_MODES_OPTIONS_ID = NAVIGATION_OPTIONS_ID + "app_modes_options";
	String NAVIGATION_ROUTE_DETAILS_OPTIONS_ID = NAVIGATION_OPTIONS_ID + "route_details_options";
	String NAVIGATION_ROUTE_CALCULATE_ALTITUDE_ID = NAVIGATION_OPTIONS_ID + "calculate_altitude";

	//Dialogs IDs:
	String FRAGMENT_ID = "fragment.";
	String FRAGMENT_RATE_US_ID = FRAGMENT_ID + "rate_us";
	String FRAGMENT_CRASH_ID = FRAGMENT_ID + "crash";
	String FRAGMENT_RENDER_INIT_ERROR_ID = FRAGMENT_ID + "render_init_error";
	String FRAGMENT_DESTINATION_REACHED_ID = FRAGMENT_ID + "destination_reached";
	String FRAGMENT_WHATS_NEW_ID = FRAGMENT_ID + "whats_new";
	String FRAGMENT_SEND_ANALYTICS_ID = FRAGMENT_ID + "send_analytics";
	String FRAGMENT_DRAWER_ID = FRAGMENT_ID + "drawer";
	String FRAGMENT_TRIPLTEK_PROMO_ID = FRAGMENT_ID + "tripltek_promo";
	String FRAGMENT_HUGEROCK_PROMO_ID = FRAGMENT_ID + "hugerock_promo";

	//Map Context Menu rows:
	String MAP_CONTEXT_MENU_ROWS = "context.menu.rows.";
	String CONTEXT_MENU_LINKS_ID = MAP_CONTEXT_MENU_ROWS + "links";
	String CONTEXT_MENU_PHONE_ID = MAP_CONTEXT_MENU_ROWS + "phone";
	String CONTEXT_MENU_SEARCH_MORE_ID = MAP_CONTEXT_MENU_ROWS + "search_more";
	String CONTEXT_MENU_SHOW_ON_MAP_ID = MAP_CONTEXT_MENU_ROWS + "show_on_map";
	String CONTEXT_MENU_AVOID_ROADS_ID = MAP_CONTEXT_MENU_ROWS + "avoid_roads";
	String CONTEXT_MENU_ONLINE_PHOTOS_ID = MAP_CONTEXT_MENU_ROWS + "online_photos";
}