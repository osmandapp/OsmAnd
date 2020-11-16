package net.osmand.plus.settings.backend;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum ExportSettingsType {
	PROFILE(R.string.shared_string_profiles, R.drawable.ic_action_manage_profiles),
	QUICK_ACTIONS(R.string.configure_screen_quick_action, R.drawable.ic_quick_action),
	POI_TYPES(R.string.poi_dialog_poi_type, R.drawable.ic_action_info_dark),
	MAP_SOURCES(R.string.quick_action_map_source_title, R.drawable.ic_map),
	CUSTOM_RENDER_STYLE(R.string.shared_string_rendering_style, R.drawable.ic_action_map_style),
	CUSTOM_ROUTING(R.string.shared_string_routing, R.drawable.ic_action_route_distance),
	AVOID_ROADS(R.string.avoid_road, R.drawable.ic_action_alert),
	TRACKS(R.string.shared_string_tracks, R.drawable.ic_action_route_distance),
	MULTIMEDIA_NOTES(R.string.audionotes_plugin_name, R.drawable.ic_action_photo_dark),
	GLOBAL(R.string.general_settings_2, R.drawable.ic_action_settings),
	OSM_NOTES(R.string.osm_notes, R.drawable.ic_action_openstreetmap_logo),
	OSM_EDITS(R.string.osm_edits, R.drawable.ic_action_openstreetmap_logo),
	OFFLINE_MAPS(R.string.shared_string_maps, R.drawable.ic_map),
	FAVORITES(R.string.shared_string_favorites, R.drawable.ic_action_favorite),
	TTS_VOICE(R.string.local_indexes_cat_tts, R.drawable.ic_action_volume_up),
	VOICE(R.string.local_indexes_cat_voice, R.drawable.ic_action_volume_up),
	ACTIVE_MARKERS(R.string.map_markers, R.drawable.ic_action_flag),
	HISTORY_MARKERS(R.string.markers_history, R.drawable.ic_action_flag);

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
		return this == PROFILE || this == GLOBAL || this == QUICK_ACTIONS || this == POI_TYPES;
	}

	public boolean isMyPlacesCategory() {
		return this == FAVORITES || this == TRACKS || this == OSM_EDITS || this == OSM_NOTES
				|| this == MULTIMEDIA_NOTES || this == ACTIVE_MARKERS || this == HISTORY_MARKERS;
	}

	public boolean isResourcesCategory() {
		return this == CUSTOM_RENDER_STYLE || this == CUSTOM_ROUTING || this == MAP_SOURCES
				|| this == OFFLINE_MAPS || this == VOICE || this == TTS_VOICE;
	}
}