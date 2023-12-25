package net.osmand.plus.settings.backend.backup.exporttype;

import static net.osmand.plus.settings.backend.backup.SettingsHelper.OLD_OFFLINE_MAPS_EXPORT_TYPE_KEY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExportTypes {
	private static final List<ExportType> values = new ArrayList<>();

	public static final ExportType PROFILE = register(new ProfileExportType());
	public static final ExportType GLOBAL = register(new GlobalExportType());
	public static final ExportType QUICK_ACTIONS = register(new QuickActionsExportType());
	public static final ExportType POI_TYPES = register(new PoiTypesExportType());
	public static final ExportType AVOID_ROADS = register(new AvoidRoadsExportType());
	public static final ExportType FAVORITES = register(new FavoritesExportType());
	public static final ExportType FAVORITES_BACKUP = register(new FavoritesBackupExportType());
	public static final ExportType TRACKS = register(new TracksExportType());
	public static final ExportType OSM_NOTES = register(new OsmNotesExportType());
	public static final ExportType OSM_EDITS = register(new OsmEditsExportType());
	public static final ExportType MULTIMEDIA_NOTES = register(new MultimediaNotesExportType());
	public static final ExportType ACTIVE_MARKERS = register(new ActiveMarkersExportType());
	public static final ExportType HISTORY_MARKERS = register(new HistoryMarkersExportType());
	public static final ExportType SEARCH_HISTORY = register(new SearchHistoryExportType());
	public static final ExportType NAVIGATION_HISTORY = register(new NavigationHistoryExportType());
	public static final ExportType CUSTOM_RENDER_STYLE = register(new CustomRenderStyleExportType());
	public static final ExportType CUSTOM_ROUTING = register(new CustomRoutingExportType());
	public static final ExportType MAP_SOURCES = register(new MapSourcesExportType());
	public static final ExportType STANDARD_MAPS = register(new StandardMapsExportType());
	public static final ExportType ROAD_MAPS = register(new RoadMapsExportType());
	public static final ExportType WIKI_AND_TRAVEL = register(new WikiAndTravelExportType());
	public static final ExportType TERRAIN_DATA = register(new TerrainDataExportType());
	public static final ExportType DEPTH_DATA = register(new DepthDataExportType());
	public static final ExportType TTS_VOICE = register(new TtsVoiceExportType());
	public static final ExportType VOICE = register(new VoiceExportType());
	public static final ExportType ONLINE_ROUTING_ENGINES = register(new OnlineRoutingEnginesExportType());
	public static final ExportType ITINERARY_GROUPS = register(new ItineraryGroupsExportType());

	@Nullable
	public static ExportType findBy(@NonNull RemoteFile remoteFile) {
		if (remoteFile.item != null) {
			return findBy(remoteFile.item);
		}
		for (ExportType exportType : values()) {
			String type = remoteFile.getType();
			if (Objects.equals(exportType.getSettingsItemType().name(), type)) {
				if (SettingsItemType.FILE.name().equals(type)) {
					FileSubtype subtype = FileSubtype.getSubtypeByFileName(remoteFile.getName());
					return findBy(subtype);
				} else {
					return exportType;
				}
			}
		}
		return null;
	}

	@Nullable
	public static ExportType findBy(@Nullable SettingsItem item) {
		if (item == null) return null;

		for (ExportType exportType : values()) {
			if (exportType.getSettingsItemType().equals(item.getType())) {
				if (item.getType() == SettingsItemType.FILE) {
					FileSettingsItem fileItem = (FileSettingsItem) item;
					return findBy(fileItem.getSubtype());
				} else {
					return exportType;
				}
			}
		}
		return null;
	}

	@Nullable
	public static ExportType findBy(@NonNull FileSubtype subtype) {
		if (subtype == FileSubtype.RENDERING_STYLE) {
			return CUSTOM_RENDER_STYLE;
		} else if (subtype == FileSubtype.ROUTING_CONFIG) {
			return CUSTOM_ROUTING;
		} else if (subtype == FileSubtype.MULTIMEDIA_NOTES) {
			return MULTIMEDIA_NOTES;
		} else if (subtype == FileSubtype.GPX) {
			return TRACKS;
		} else if (subtype == FileSubtype.OBF_MAP) {
			return STANDARD_MAPS;
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
	public static ExportType findBy(@NonNull LocalItemType localItemType) {
		if (localItemType == LocalItemType.MAP_DATA) {
			return STANDARD_MAPS;
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
		} else if (localItemType == LocalItemType.TILES_DATA) {
			return MAP_SOURCES;
		} else if (localItemType == LocalItemType.RENDERING_STYLES) {
			return CUSTOM_RENDER_STYLE;
		}
		return null;
	}

	@NonNull
	public static List<ExportType> settingsValues() {
		return Arrays.asList(
				PROFILE, GLOBAL, QUICK_ACTIONS, POI_TYPES, AVOID_ROADS
		);
	}

	@NonNull
	public static List<ExportType> myPlacesValues() {
		return Arrays.asList(
				FAVORITES, TRACKS, OSM_EDITS, OSM_NOTES,
				MULTIMEDIA_NOTES, ACTIVE_MARKERS, HISTORY_MARKERS,
				SEARCH_HISTORY, ITINERARY_GROUPS, NAVIGATION_HISTORY
		);
	}

	@NonNull
	public static List<ExportType> resourcesValues() {
		return Arrays.asList(
				CUSTOM_RENDER_STYLE, CUSTOM_ROUTING, MAP_SOURCES,
				STANDARD_MAPS, ROAD_MAPS, WIKI_AND_TRAVEL,
				TERRAIN_DATA, DEPTH_DATA, VOICE, TTS_VOICE,
				ONLINE_ROUTING_ENGINES, FAVORITES_BACKUP
		);
	}

	@NonNull
	public static List<ExportType> availableInFreeValues() {
		return Arrays.asList(
				PROFILE, GLOBAL, FAVORITES, OSM_NOTES, OSM_EDITS
		);
	}

	@NonNull
	public static List<ExportType> mapValues() {
		return Arrays.asList(STANDARD_MAPS, ROAD_MAPS, WIKI_AND_TRAVEL, TERRAIN_DATA, DEPTH_DATA);
	}

	@NonNull
	public static List<ExportType> enabledValues() {
		List<ExportType> result = new ArrayList<>(values());
		if (!PluginsHelper.isActive(OsmEditingPlugin.class)) {
			result.remove(OSM_EDITS);
			result.remove(OSM_NOTES);
		}
		if (!PluginsHelper.isActive(AudioVideoNotesPlugin.class)) {
			result.remove(MULTIMEDIA_NOTES);
		}
		return result;
	}

	@NonNull
	public static List<ExportType> valuesOfKeys(@NonNull List<String> typeKeys) {
		List<ExportType> result = new ArrayList<>();
		for (String key : typeKeys) {
			if (Objects.equals(OLD_OFFLINE_MAPS_EXPORT_TYPE_KEY, key)) {
				Algorithms.addAllIfNotContains(result, mapValues());
			} else {
				Algorithms.addIfNotContains(result, valueOf(key));
			}
		}
		return result;
	}

	@Nullable
	public static ExportType valueOf(@NonNull String key) {
		for (ExportType exportType : values()) {
			if (Objects.equals(key, exportType.getId())) {
				return exportType;
			}
		}
		return null;
	}

	@NonNull
	public static List<ExportType> values() {
		return values;
	}

	@NonNull
	private static ExportType register(@NonNull ExportType exportType) {
		values.add(exportType);
		return exportType;
	}
}
