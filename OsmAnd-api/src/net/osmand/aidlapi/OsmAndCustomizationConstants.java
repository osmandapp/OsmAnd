package net.osmand.aidlapi;

public interface OsmAndCustomizationConstants {

	// Navigation Drawer:
	String DRAWER_ITEM_ID_SCHEME = "drawer.action.";
	String DRAWER_DASHBOARD_ID = DRAWER_ITEM_ID_SCHEME + "dashboard";
	String DRAWER_MAP_MARKERS_ID = DRAWER_ITEM_ID_SCHEME + "map_markers";
	String DRAWER_MY_PLACES_ID = DRAWER_ITEM_ID_SCHEME + "my_places";
	String DRAWER_SEARCH_ID = DRAWER_ITEM_ID_SCHEME + "search";
	String DRAWER_DIRECTIONS_ID = DRAWER_ITEM_ID_SCHEME + "directions";
	String DRAWER_CONFIGURE_MAP_ID = DRAWER_ITEM_ID_SCHEME + "configure_map";
	String DRAWER_DOWNLOAD_MAPS_ID = DRAWER_ITEM_ID_SCHEME + "download_maps";
	String DRAWER_OSMAND_LIVE_ID = DRAWER_ITEM_ID_SCHEME + "osmand_live";
	String DRAWER_TRAVEL_GUIDES_ID = DRAWER_ITEM_ID_SCHEME + "travel_guides";
	String DRAWER_MEASURE_DISTANCE_ID = DRAWER_ITEM_ID_SCHEME + "measure_distance";
	String DRAWER_CONFIGURE_SCREEN_ID = DRAWER_ITEM_ID_SCHEME + "configure_screen";
	String DRAWER_PLUGINS_ID = DRAWER_ITEM_ID_SCHEME + "plugins";
	String DRAWER_SETTINGS_ID = DRAWER_ITEM_ID_SCHEME + "settings";
	String DRAWER_HELP_ID = DRAWER_ITEM_ID_SCHEME + "help";
	String DRAWER_BUILDS_ID = DRAWER_ITEM_ID_SCHEME + "builds";
	String DRAWER_DIVIDER_ID = DRAWER_ITEM_ID_SCHEME + "divider";

	// Configure Map:
	String CONFIGURE_MAP_ITEM_ID_SCHEME = "map.configure.";
	String SHOW_ITEMS_ID_SCHEME = CONFIGURE_MAP_ITEM_ID_SCHEME + "show.";
	String RENDERING_ITEMS_ID_SCHEME = CONFIGURE_MAP_ITEM_ID_SCHEME + "rendering.";
	String CUSTOM_RENDERING_ITEMS_ID_SCHEME = RENDERING_ITEMS_ID_SCHEME + "custom.";

	String APP_PROFILES_ID = CONFIGURE_MAP_ITEM_ID_SCHEME + "app_profiles";

	String SHOW_CATEGORY_ID = SHOW_ITEMS_ID_SCHEME + "category";
	String FAVORITES_ID = SHOW_ITEMS_ID_SCHEME + "favorites";
	String POI_OVERLAY_ID = SHOW_ITEMS_ID_SCHEME + "poi_overlay";
	String POI_OVERLAY_LABELS_ID = SHOW_ITEMS_ID_SCHEME + "poi_overlay_labels";
	String TRANSPORT_ID = SHOW_ITEMS_ID_SCHEME + "transport";
	String GPX_FILES_ID = SHOW_ITEMS_ID_SCHEME + "gpx_files";
	String MAP_MARKERS_ID = SHOW_ITEMS_ID_SCHEME + "map_markers";
	String MAP_SOURCE_ID = SHOW_ITEMS_ID_SCHEME + "map_source";
	String RECORDING_LAYER = SHOW_ITEMS_ID_SCHEME + "recording_layer";
	String MAPILLARY = SHOW_ITEMS_ID_SCHEME + "mapillary";
	String OSM_NOTES = SHOW_ITEMS_ID_SCHEME + "osm_notes";
	String OVERLAY_MAP = SHOW_ITEMS_ID_SCHEME + "overlay_map";
	String UNDERLAY_MAP = SHOW_ITEMS_ID_SCHEME + "underlay_map";
	String CONTOUR_LINES = SHOW_ITEMS_ID_SCHEME + "contour_lines";
	String HILLSHADE_LAYER = SHOW_ITEMS_ID_SCHEME + "hillshade_layer";

	String MAP_RENDERING_CATEGORY_ID = RENDERING_ITEMS_ID_SCHEME + "category";
	String MAP_STYLE_ID = RENDERING_ITEMS_ID_SCHEME + "map_style";
	String MAP_MODE_ID = RENDERING_ITEMS_ID_SCHEME + "map_mode";
	String MAP_MAGNIFIER_ID = RENDERING_ITEMS_ID_SCHEME + "map_magnifier";
	String ROAD_STYLE_ID = RENDERING_ITEMS_ID_SCHEME + "road_style";
	String TEXT_SIZE_ID = RENDERING_ITEMS_ID_SCHEME + "text_size";
	String MAP_LANGUAGE_ID = RENDERING_ITEMS_ID_SCHEME + "map_language";
	String TRANSPORT_RENDERING_ID = RENDERING_ITEMS_ID_SCHEME + "transport";
	String DETAILS_ID = RENDERING_ITEMS_ID_SCHEME + "details";
	String HIDE_ID = RENDERING_ITEMS_ID_SCHEME + "hide";
	String ROUTES_ID = RENDERING_ITEMS_ID_SCHEME + "routes";

	// Map Controls:
	String HUD_BTN_ID_SCHEME = "map.view.";
	String LAYERS_HUD_ID = HUD_BTN_ID_SCHEME + "layers";
	String COMPASS_HUD_ID = HUD_BTN_ID_SCHEME + "compass";
	String QUICK_SEARCH_HUD_ID = HUD_BTN_ID_SCHEME + "quick_search";
	String BACK_TO_LOC_HUD_ID = HUD_BTN_ID_SCHEME + "back_to_loc";
	String MENU_HUD_ID = HUD_BTN_ID_SCHEME + "menu";
	String ROUTE_PLANNING_HUD_ID = HUD_BTN_ID_SCHEME + "route_planning";
	String ZOOM_IN_HUD_ID = HUD_BTN_ID_SCHEME + "zoom_id";
	String ZOOM_OUT_HUD_ID = HUD_BTN_ID_SCHEME + "zoom_out";

	//Map Context Menu Actions:
	String MAP_CONTEXT_MENU_ACTIONS = "point.actions.";
	String MAP_CONTEXT_MENU_DIRECTIONS_FROM_ID = MAP_CONTEXT_MENU_ACTIONS + "directions_from";
	String MAP_CONTEXT_MENU_SEARCH_NEARBY = MAP_CONTEXT_MENU_ACTIONS + "search_nearby";
	String MAP_CONTEXT_MENU_CHANGE_MARKER_POSITION = MAP_CONTEXT_MENU_ACTIONS + "change_m_position";
	String MAP_CONTEXT_MENU_MARK_AS_PARKING_LOC = MAP_CONTEXT_MENU_ACTIONS + "mark_as_parking";
	String MAP_CONTEXT_MENU_MEASURE_DISTANCE = MAP_CONTEXT_MENU_ACTIONS + "measure_distance";
	String MAP_CONTEXT_MENU_EDIT_GPX_WP = MAP_CONTEXT_MENU_ACTIONS + "edit_gpx_waypoint";
	String MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT = MAP_CONTEXT_MENU_ACTIONS + "add_gpx_waypoint";
	String MAP_CONTEXT_MENU_UPDATE_MAP = MAP_CONTEXT_MENU_ACTIONS + "update_map";
	String MAP_CONTEXT_MENU_DOWNLOAD_MAP = MAP_CONTEXT_MENU_ACTIONS + "download_map";
	String MAP_CONTEXT_MENU_MODIFY_POI = MAP_CONTEXT_MENU_ACTIONS + "modify_poi";
	String MAP_CONTEXT_MENU_MODIFY_OSM_CHANGE = MAP_CONTEXT_MENU_ACTIONS + "modify_osm_change";
	String MAP_CONTEXT_MENU_CREATE_POI = MAP_CONTEXT_MENU_ACTIONS + "create_poi";
	String MAP_CONTEXT_MENU_MODIFY_OSM_NOTE = MAP_CONTEXT_MENU_ACTIONS + "modify_osm_note";
	String MAP_CONTEXT_MENU_OPEN_OSM_NOTE = MAP_CONTEXT_MENU_ACTIONS + "open_osm_note";

	//Plug-in's IDs:
	String PLUGIN_OSMAND_MONITOR = "osmand.monitoring";
	String PLUGIN_MAPILLARY = "osmand.mapillary";
	String PLUGIN_OSMAND_DEV = "osmand.development";
	String PLUGIN_AUDIO_VIDEO_NOTES = "osmand.audionotes";
	String PLUGIN_NAUTICAL = "nauticalPlugin.plugin";
	String PLUGIN_OSMAND_EDITING = "osm.editing";
	String PLUGIN_PARKING_POSITION = "osmand.parking.position";
	String PLUGIN_RASTER_MAPS = "osmand.rastermaps";
	String PLUGIN_SKI_MAPS = "skimaps.plugin";
	String PLUGIN_SRTM = "osmand.srtm";
}