package net.osmand.plus.settings.backend;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum ExportSettingsType {
	PROFILE(R.string.shared_string_profiles, R.drawable.ic_action_manage_profiles),
	GLOBAL(R.string.general_settings_2, R.drawable.ic_action_settings),
	QUICK_ACTIONS(R.string.configure_screen_quick_action, R.drawable.ic_quick_action),
	POI_TYPES(R.string.poi_dialog_poi_type, R.drawable.ic_action_info_dark),
	AVOID_ROADS(R.string.avoid_road, R.drawable.ic_action_alert),
	FAVORITES(R.string.shared_string_favorites, R.drawable.ic_action_favorite),
	TRACKS(R.string.shared_string_tracks, R.drawable.ic_action_polygom_dark),
	OSM_NOTES(R.string.osm_notes, R.drawable.ic_action_openstreetmap_logo),
	OSM_EDITS(R.string.osm_edits, R.drawable.ic_action_openstreetmap_logo),
	MULTIMEDIA_NOTES(R.string.notes, R.drawable.ic_grouped_by_type),
	ACTIVE_MARKERS(R.string.map_markers, R.drawable.ic_action_flag),
	HISTORY_MARKERS(R.string.markers_history, R.drawable.ic_action_flag),
	SEARCH_HISTORY(R.string.shared_string_search_history, R.drawable.ic_action_history),
	CUSTOM_RENDER_STYLE(R.string.shared_string_rendering_style, R.drawable.ic_action_map_style),
	CUSTOM_ROUTING(R.string.shared_string_routing, R.drawable.ic_action_route_distance),
	MAP_SOURCES(R.string.quick_action_map_source_title, R.drawable.ic_map),
	OFFLINE_MAPS(R.string.shared_string_maps, R.drawable.ic_map),
	TTS_VOICE(R.string.local_indexes_cat_tts, R.drawable.ic_action_volume_up),
	VOICE(R.string.local_indexes_cat_voice, R.drawable.ic_action_volume_up),
	ONLINE_ROUTING_ENGINES(R.string.online_routing_engines, R.drawable.ic_world_globe_dark);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int drawableRes;

	ExportSettingsType(@StringRes int titleId, @DrawableRes int drawableRes) {
		this.titleId = titleId;
		this.drawableRes = drawableRes;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconRes() {
		return drawableRes;
	}

	public boolean isSettingsCategory() {
		return this == PROFILE || this == GLOBAL || this == QUICK_ACTIONS || this == POI_TYPES
				|| this == AVOID_ROADS;
	}

	public boolean isMyPlacesCategory() {
		return this == FAVORITES || this == TRACKS || this == OSM_EDITS || this == OSM_NOTES
				|| this == MULTIMEDIA_NOTES || this == ACTIVE_MARKERS || this == HISTORY_MARKERS
				|| this == SEARCH_HISTORY;
	}

	public boolean isResourcesCategory() {
		return this == CUSTOM_RENDER_STYLE || this == CUSTOM_ROUTING || this == MAP_SOURCES
				|| this == OFFLINE_MAPS || this == VOICE || this == TTS_VOICE || this == ONLINE_ROUTING_ENGINES;
	}
}