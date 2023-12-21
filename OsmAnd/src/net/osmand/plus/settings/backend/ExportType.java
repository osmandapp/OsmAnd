package net.osmand.plus.settings.backend;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ExportType {

	PROFILE(R.string.shared_string_profiles, R.drawable.ic_action_manage_profiles, SettingsItemType.PROFILE, true),
	GLOBAL(R.string.osmand_settings, R.drawable.ic_action_settings, SettingsItemType.GLOBAL, true),
	QUICK_ACTIONS(R.string.configure_screen_quick_action, R.drawable.ic_quick_action, SettingsItemType.QUICK_ACTIONS),
	POI_TYPES(R.string.poi_dialog_poi_type, R.drawable.ic_action_info_dark, SettingsItemType.POI_UI_FILTERS),
	AVOID_ROADS(R.string.avoid_road, R.drawable.ic_action_alert, SettingsItemType.AVOID_ROADS),
	FAVORITES(R.string.shared_string_favorites, R.drawable.ic_action_favorite, SettingsItemType.FAVOURITES, true),
	FAVORITES_BACKUP(R.string.favorites_backup, R.drawable.ic_action_folder_favorites, SettingsItemType.FILE),
	TRACKS(R.string.shared_string_tracks, R.drawable.ic_action_polygom_dark, SettingsItemType.GPX),
	OSM_NOTES(R.string.osm_notes, R.drawable.ic_action_openstreetmap_logo, SettingsItemType.OSM_NOTES, true),
	OSM_EDITS(R.string.osm_edits, R.drawable.ic_action_openstreetmap_logo, SettingsItemType.OSM_EDITS, true),
	MULTIMEDIA_NOTES(R.string.notes, R.drawable.ic_grouped_by_type, SettingsItemType.FILE),
	ACTIVE_MARKERS(R.string.map_markers, R.drawable.ic_action_flag, SettingsItemType.ACTIVE_MARKERS),
	HISTORY_MARKERS(R.string.markers_history, R.drawable.ic_action_flag, SettingsItemType.HISTORY_MARKERS),
	SEARCH_HISTORY(R.string.shared_string_search_history, R.drawable.ic_action_history, SettingsItemType.SEARCH_HISTORY),
	NAVIGATION_HISTORY(R.string.navigation_history, R.drawable.ic_action_gdirections_dark, SettingsItemType.NAVIGATION_HISTORY),
	CUSTOM_RENDER_STYLE(R.string.shared_string_rendering_style, R.drawable.ic_action_map_style, SettingsItemType.FILE),
	CUSTOM_ROUTING(R.string.shared_string_routing, R.drawable.ic_action_route_distance, SettingsItemType.FILE),
	MAP_SOURCES(R.string.quick_action_map_source_title, R.drawable.ic_action_layers, SettingsItemType.MAP_SOURCES),
	OFFLINE_MAPS(R.string.standard_maps, R.drawable.ic_map, SettingsItemType.FILE),
	ROAD_MAPS(R.string.download_roads_only_maps, R.drawable.ic_map, SettingsItemType.FILE),
	WIKI_AND_TRAVEL(R.string.wikipedia_and_travel_maps, R.drawable.ic_action_wikipedia, SettingsItemType.FILE),
	TERRAIN_DATA(R.string.topography_maps, R.drawable.ic_action_terrain, SettingsItemType.FILE),
	DEPTH_DATA(R.string.nautical_maps, R.drawable.ic_action_anchor, SettingsItemType.FILE),
	TTS_VOICE(R.string.local_indexes_cat_tts, R.drawable.ic_action_volume_up, SettingsItemType.FILE),
	VOICE(R.string.local_indexes_cat_voice, R.drawable.ic_action_volume_up, SettingsItemType.FILE),
	ONLINE_ROUTING_ENGINES(R.string.online_routing_engines, R.drawable.ic_world_globe_dark, SettingsItemType.ONLINE_ROUTING_ENGINES),
	ITINERARY_GROUPS(R.string.shared_string_itinerary, R.drawable.ic_action_flag, SettingsItemType.ITINERARY_GROUPS);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int drawableRes;
	private final String itemName;
	private final boolean allowedInFreeVersion;


	ExportType(@StringRes int titleId, @DrawableRes int drawableRes, @NonNull SettingsItemType itemType) {
		this(titleId, drawableRes, itemType, false);
	}
	
	ExportType(@StringRes int titleId, @DrawableRes int drawableRes, @NonNull SettingsItemType itemType, boolean allowedInFreeVersion) {
		this.titleId = titleId;
		this.drawableRes = drawableRes;
		this.itemName = itemType.name();
		this.allowedInFreeVersion = allowedInFreeVersion;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconRes() {
		return drawableRes;
	}

	public String getItemName() {
		return itemName;
	}

	public boolean isSettingsCategory() {
		return this == PROFILE || this == GLOBAL || this == QUICK_ACTIONS || this == POI_TYPES
				|| this == AVOID_ROADS;
	}

	public boolean isMyPlacesCategory() {
		return this == FAVORITES || this == TRACKS || this == OSM_EDITS || this == OSM_NOTES
				|| this == MULTIMEDIA_NOTES || this == ACTIVE_MARKERS || this == HISTORY_MARKERS
				|| this == SEARCH_HISTORY || this == ITINERARY_GROUPS || this == NAVIGATION_HISTORY;
	}

	public boolean isResourcesCategory() {
		return this == CUSTOM_RENDER_STYLE || this == CUSTOM_ROUTING || this == MAP_SOURCES
				|| this == OFFLINE_MAPS || this == ROAD_MAPS || this == WIKI_AND_TRAVEL
				|| this == TERRAIN_DATA || this == DEPTH_DATA ||this == VOICE || this == TTS_VOICE
				|| this == ONLINE_ROUTING_ENGINES || this == FAVORITES_BACKUP;
	}

	@Nullable
	public static ExportType findByRemoteFile(@NonNull RemoteFile remoteFile) {
		if (remoteFile.item != null) {
			return findBySettingsItem(remoteFile.item);
		}
		for (ExportType exportType : values()) {
			String type = remoteFile.getType();
			if (exportType.getItemName().equals(type)) {
				if (SettingsItemType.FILE.name().equals(type)) {
					FileSubtype subtype = FileSubtype.getSubtypeByFileName(remoteFile.getName());
					return findByFileSubtype(subtype);
				} else {
					return exportType;
				}
			}
		}
		return null;
	}

	@Nullable
	public static ExportType findBySettingsItem(@NonNull SettingsItem item) {
		for (ExportType exportType : values()) {
			if (exportType.getItemName().equals(item.getType().name())) {
				if (item.getType() == SettingsItemType.FILE) {
					FileSettingsItem fileItem = (FileSettingsItem) item;
					return findByFileSubtype(fileItem.getSubtype());
				} else {
					return exportType;
				}
			}
		}
		return null;
	}

	@Nullable
	public static ExportType findByFileSubtype(@NonNull FileSubtype subtype) {
		if (subtype == FileSubtype.RENDERING_STYLE) {
			return CUSTOM_RENDER_STYLE;
		} else if (subtype == FileSubtype.ROUTING_CONFIG) {
			return CUSTOM_ROUTING;
		} else if (subtype == FileSubtype.MULTIMEDIA_NOTES) {
			return MULTIMEDIA_NOTES;
		} else if (subtype == FileSubtype.GPX) {
			return TRACKS;
		} else if (subtype == FileSubtype.OBF_MAP) {
			return OFFLINE_MAPS;
		} else if (subtype == FileSubtype.WIKI_MAP || subtype == FileSubtype.TRAVEL) {
			return WIKI_AND_TRAVEL;
		} else if (subtype == FileSubtype.SRTM_MAP || subtype == FileSubtype.TERRAIN_DATA) {
			return TERRAIN_DATA;
		} else if (subtype == FileSubtype.TILES_MAP) {
			return MAP_SOURCES;
		} else if (subtype == FileSubtype.ROAD_MAP) {
			return ROAD_MAPS;
		} else if (subtype == FileSubtype.NAUTICAL_DEPTH) {
			return DEPTH_DATA;
		} else if (subtype == FileSubtype.TTS_VOICE) {
			return TTS_VOICE;
		} else if (subtype == FileSubtype.VOICE) {
			return VOICE;
		} else if (subtype == FileSubtype.FAVORITES_BACKUP) {
			return FAVORITES_BACKUP;
		}
		return null;
	}

	@Nullable
	public static ExportType findByLocalItemType(@NonNull LocalItemType localItemType) {
		if (localItemType == LocalItemType.MAP_DATA) {
			return OFFLINE_MAPS;
		} else if (localItemType == LocalItemType.ROAD_DATA) {
			return ROAD_MAPS;
		} else if (localItemType == LocalItemType.WIKI_AND_TRAVEL_MAPS) {
			return WIKI_AND_TRAVEL;
		} else if (localItemType == LocalItemType.DEPTH_DATA) {
			return DEPTH_DATA;
		} else if (localItemType == LocalItemType.TERRAIN_DATA) {
			return TERRAIN_DATA;
		} else if (localItemType == LocalItemType.TTS_VOICE_DATA) {
			return TTS_VOICE;
		} else if (localItemType == LocalItemType.VOICE_DATA) {
			return VOICE;
		}
		return null;
	}

	public boolean isAllowedInFreeVersion() {
		return allowedInFreeVersion;
	}

	public static boolean isTypeEnabled(@NonNull ExportType type) {
		return getEnabledTypes().contains(type);
	}

	@NonNull
	public static List<ExportType> getEnabledTypes() {
		List<ExportType> result = new ArrayList<>(Arrays.asList(values()));
		if (!PluginsHelper.isActive(OsmEditingPlugin.class)) {
			result.remove(OSM_EDITS);
			result.remove(OSM_NOTES);
		}
		if (!PluginsHelper.isActive(AudioVideoNotesPlugin.class)) {
			result.remove(MULTIMEDIA_NOTES);
		}
		return result;
	}
}