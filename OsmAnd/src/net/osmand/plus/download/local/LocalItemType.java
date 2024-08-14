package net.osmand.plus.download.local;


import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.util.CollectionUtils;

public enum LocalItemType {

	MAP_DATA(R.string.standard_maps, R.drawable.ic_map),
	ROAD_DATA(R.string.download_roads_only_maps, R.drawable.ic_map),
	LIVE_UPDATES(R.string.download_live_updates, R.drawable.ic_map),
	TTS_VOICE_DATA(R.string.local_indexes_cat_tts, R.drawable.ic_action_volume_up),
	VOICE_DATA(R.string.local_indexes_cat_voice, R.drawable.ic_action_volume_up),
	FONT_DATA(R.string.fonts_header, R.drawable.ic_action_map_language),
	TERRAIN_DATA(R.string.topography_maps, R.drawable.ic_action_terrain),
	DEPTH_DATA(R.string.nautical_maps, R.drawable.ic_action_anchor),
	WIKI_AND_TRAVEL_MAPS(R.string.wikipedia_and_travel_maps, R.drawable.ic_action_wikipedia),
	TILES_DATA(R.string.quick_action_map_source_title, R.drawable.ic_action_layers),
	WEATHER_DATA(R.string.shared_string_weather, R.drawable.ic_action_umbrella),
	RENDERING_STYLES(R.string.rendering_styles, R.drawable.ic_action_map_style),
	ROUTING(R.string.shared_string_routing, R.drawable.ic_action_file_routing),
	CACHE(R.string.shared_string_cache, R.drawable.ic_action_storage),
	FAVORITES(R.string.favourites, R.drawable.ic_action_favorite),
	TRACKS(R.string.shared_string_tracks, R.drawable.ic_action_polygom_dark),
	OSM_NOTES(R.string.osm_notes, R.drawable.ic_action_openstreetmap_logo),
	OSM_EDITS(R.string.osm_edits, R.drawable.ic_action_openstreetmap_logo),
	MULTIMEDIA_NOTES(R.string.notes, R.drawable.ic_action_folder_av_notes),
	ACTIVE_MARKERS(R.string.map_markers, R.drawable.ic_action_flag_stroke),
	HISTORY_MARKERS(R.string.shared_string_history, R.drawable.ic_action_history),
	ITINERARY_GROUPS(R.string.shared_string_itinerary, R.drawable.ic_action_flag_stroke),
	COLOR_DATA(R.string.shared_string_colors, R.drawable.ic_action_file_color_palette),
	PROFILES(R.string.shared_string_profiles, R.drawable.ic_action_manage_profiles),
	OTHER(R.string.shared_string_other, R.drawable.ic_action_settings);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	LocalItemType(@StringRes int titleId, @DrawableRes int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(titleId);
	}

	@NonNull
	public CategoryType getCategoryType() {
		if (isSettingsCategory()) {
			return CategoryType.SETTINGS;
		} else if (isMyPlacesCategory()) {
			return CategoryType.MY_PLACES;
		} else if (isResourcesCategory()) {
			return CategoryType.RESOURCES;
		}
		throw new IllegalArgumentException("LocalItemType + " + name() + " don`t have category");
	}

	public boolean isSettingsCategory() {
		return CollectionUtils.equalsToAny(this, COLOR_DATA, PROFILES, OTHER);
	}

	public boolean isMyPlacesCategory() {
		return CollectionUtils.equalsToAny(this, FAVORITES, TRACKS, OSM_EDITS, OSM_NOTES,
				MULTIMEDIA_NOTES, ACTIVE_MARKERS, HISTORY_MARKERS, ITINERARY_GROUPS);
	}

	public boolean isResourcesCategory() {
		return CollectionUtils.equalsToAny(this, MAP_DATA, ROAD_DATA, LIVE_UPDATES, TERRAIN_DATA,
				WIKI_AND_TRAVEL_MAPS, DEPTH_DATA, WEATHER_DATA, TILES_DATA, RENDERING_STYLES,
				ROUTING, TTS_VOICE_DATA, VOICE_DATA, FONT_DATA, CACHE);
	}

	public boolean isDownloadType() {
		return CollectionUtils.equalsToAny(this, MAP_DATA, ROAD_DATA, TILES_DATA, TERRAIN_DATA,
				DEPTH_DATA, WIKI_AND_TRAVEL_MAPS, WEATHER_DATA, TTS_VOICE_DATA, VOICE_DATA, FONT_DATA);
	}

	public boolean isUpdateSupported() {
		return this != TILES_DATA && isDownloadType();
	}

	public boolean isDeletionSupported() {
		return isDownloadType() || this == LIVE_UPDATES || this == CACHE
				|| ExportType.findBy(this) != null && this != PROFILES;
	}

	public boolean isBackupSupported() {
		return CollectionUtils.equalsToAny(this, MAP_DATA, ROAD_DATA, WIKI_AND_TRAVEL_MAPS, TERRAIN_DATA, DEPTH_DATA);
	}

	public boolean isRenamingSupported() {
		return this != TILES_DATA && isDownloadType();
	}

	public boolean isSortingSupported() {
		return isMyPlacesCategory() || isResourcesCategory();
	}

	public boolean isSortingByCountrySupported() {
		return CollectionUtils.equalsToAny(this, MAP_DATA, ROAD_DATA);
	}

	@Nullable
	public static LocalItemType getByName(@Nullable String name) {
		for (LocalItemType type : values()) {
			if (type.name().equalsIgnoreCase(name)) {
				return type;
			}
		}
		return null;
	}
}