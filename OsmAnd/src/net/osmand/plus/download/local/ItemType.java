package net.osmand.plus.download.local;


import static net.osmand.IndexConstants.BINARY_DEPTH_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.FONT_INDEX_EXT;
import static net.osmand.IndexConstants.GEOTIFF_SQLITE_CACHE_DIR;
import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.HEIGHTMAP_SQLITE_EXT;
import static net.osmand.IndexConstants.RENDERER_INDEX_EXT;
import static net.osmand.IndexConstants.ROUTING_FILE_EXT;
import static net.osmand.IndexConstants.ROUTING_PROFILES_DIR;
import static net.osmand.IndexConstants.TIF_EXT;
import static net.osmand.IndexConstants.TILES_INDEX_DIR;
import static net.osmand.IndexConstants.VOICE_INDEX_DIR;
import static net.osmand.IndexConstants.WEATHER_EXT;
import static net.osmand.IndexConstants.WEATHER_FORECAST_DIR;
import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.plus.mapmarkers.MapMarkersDbHelper.DB_NAME;
import static net.osmand.plus.myplaces.favorites.FavouritesFileHelper.FAV_FILE_PREFIX;
import static net.osmand.plus.myplaces.favorites.FavouritesFileHelper.LEGACY_FAV_FILE_PREFIX;
import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.IMG_EXTENSION;
import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.MPEG4_EXTENSION;
import static net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.THREEGP_EXTENSION;
import static net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapsDbHelper.OPENSTREETMAP_DB_NAME;
import static net.osmand.plus.plugins.osmedit.helpers.OsmBugsDbHelper.OSMBUGS_DB_NAME;
import static net.osmand.plus.settings.backend.OsmandSettings.CUSTOM_SHARED_PREFERENCES_PREFIX;
import static net.osmand.plus.settings.backend.OsmandSettings.SHARED_PREFERENCES_NAME;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.mapmarkers.ItineraryDataHelper;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.voice.JsMediaCommandPlayer;
import net.osmand.plus.voice.JsTtsCommandPlayer;
import net.osmand.util.Algorithms;

import java.io.File;

public enum ItemType {

	REGULAR_MAPS(R.string.standard_maps, R.drawable.ic_map),
	TERRAIN_MAPS(R.string.terrain_maps, R.drawable.ic_action_terrain),
	WIKI_AND_TRAVEL_MAPS(R.string.wikipedia_and_travel_maps, R.drawable.ic_action_wikipedia),
	NAUTICAL_MAPS(R.string.nautical_maps, R.drawable.ic_action_anchor),
	WEATHER_MAPS(R.string.shared_string_weather, R.drawable.ic_action_umbrella),
	MAP_SOURCES(R.string.quick_action_map_source_title, R.drawable.ic_action_layers),
	RENDERING_STYLES(R.string.rendering_styles, R.drawable.ic_action_map_style),
	ROUTING(R.string.shared_string_routing, R.drawable.ic_action_file_routing),
	TTS_VOICE_DATA(R.string.local_indexes_cat_tts, R.drawable.ic_action_volume_up),
	VOICE_DATA(R.string.local_indexes_cat_voice, R.drawable.ic_action_volume_up),
	FONT_DATA(R.string.fonts_header, R.drawable.ic_action_map_language),
	CACHE(R.string.shared_string_cache, R.drawable.ic_action_storage),


	FAVORITES(R.string.favourites, R.drawable.ic_action_favorite),
	TRACKS(R.string.shared_string_tracks, R.drawable.ic_action_polygom_dark),
	OSM_NOTES(R.string.osm_notes, R.drawable.ic_action_openstreetmap_logo),
	OSM_EDITS(R.string.osm_edits, R.drawable.ic_action_openstreetmap_logo),
	MULTIMEDIA_NOTES(R.string.notes, R.drawable.ic_action_folder_av_notes),
	ACTIVE_MARKERS(R.string.map_markers, R.drawable.ic_action_flag_stroke),
	HISTORY_MARKERS(R.string.shared_string_history, R.drawable.ic_action_history),
	ITINERARY_GROUPS(R.string.shared_string_itinerary, R.drawable.ic_action_flag_stroke),

	PROFILES(R.string.shared_string_profiles, R.drawable.ic_action_manage_profiles),
	OTHER(R.string.shared_string_other, R.drawable.ic_action_settings);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	ItemType(@StringRes int titleId, @DrawableRes int iconId) {
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
		return Algorithms.equalsToAny(this, PROFILES, OTHER);
	}

	public boolean isMyPlacesCategory() {
		return Algorithms.equalsToAny(this, FAVORITES, TRACKS, OSM_EDITS, OSM_NOTES,
				MULTIMEDIA_NOTES, ACTIVE_MARKERS, HISTORY_MARKERS, ITINERARY_GROUPS);
	}

	public boolean isResourcesCategory() {
		return Algorithms.equalsToAny(this, REGULAR_MAPS, TERRAIN_MAPS, WIKI_AND_TRAVEL_MAPS,
				NAUTICAL_MAPS, WEATHER_MAPS, MAP_SOURCES, RENDERING_STYLES, ROUTING, TTS_VOICE_DATA,
				VOICE_DATA, FONT_DATA, CACHE);
	}

	@Nullable
	public static ItemType getItemType(@NonNull OsmandApplication app, @NonNull File file) {
		String name = file.getName();
		String path = file.getAbsolutePath();

		if (name.endsWith(GPX_FILE_EXT) || name.endsWith(GPX_FILE_EXT + ZIP_EXT)) {
			if (ItineraryDataHelper.FILE_TO_SAVE.equals(name)) {
				return ITINERARY_GROUPS;
			} else if (name.startsWith(FAV_FILE_PREFIX) || name.startsWith(LEGACY_FAV_FILE_PREFIX)) {
				return FAVORITES;
			}
			return TRACKS;
		} else if (name.endsWith(RENDERER_INDEX_EXT)) {
			return RENDERING_STYLES;
		} else if (name.endsWith(WEATHER_EXT)) {
			return WEATHER_MAPS;
		} else if (name.endsWith(BINARY_DEPTH_MAP_INDEX_EXT)) {
			return NAUTICAL_MAPS;
		} else if (name.endsWith(TIF_EXT) || SrtmDownloadItem.isSrtmFile(name)) {
			return TERRAIN_MAPS;
		} else if (path.endsWith("databases/" + OSMBUGS_DB_NAME)) {
			return OSM_NOTES;
		} else if (path.endsWith("databases/" + OPENSTREETMAP_DB_NAME)) {
			return OSM_EDITS;
		} else if (path.endsWith("databases/" + DB_NAME)) {
			return ACTIVE_MARKERS;
		} else if (path.contains(VOICE_INDEX_DIR)) {
			if (file.isDirectory()) {
				if (JsTtsCommandPlayer.isMyData(file)) {
					return TTS_VOICE_DATA;
				}
				if (JsMediaCommandPlayer.isMyData(file)) {
					return VOICE_DATA;
				}
			}
			return null;
		} else if (name.endsWith(FONT_INDEX_EXT) || name.endsWith(".ttf")) {
			return FONT_DATA;
		} else if (name.endsWith(THREEGP_EXTENSION) || name.endsWith(MPEG4_EXTENSION) || name.endsWith(IMG_EXTENSION)) {
			return MULTIMEDIA_NOTES;
		} else if (path.contains(TILES_INDEX_DIR)) {
			if (name.endsWith(SQLiteTileSource.EXT) || name.endsWith(HEIGHTMAP_SQLITE_EXT)) {
				return MAP_SOURCES;
			}
			if (file.isDirectory()) {
				File parent = file.getParentFile();
				String parentName = parent != null ? parent.getName() : null;

				if (Algorithms.stringsEqual(TILES_INDEX_DIR.replace("/", ""), parentName)) {
					return MAP_SOURCES;
				}
			}
			return null;
		} else if ((path.contains(SHARED_PREFERENCES_NAME) || path.contains(CUSTOM_SHARED_PREFERENCES_PREFIX)) && name.endsWith(ROUTING_FILE_EXT)) {
			return PROFILES;
		} else if (path.contains(ROUTING_PROFILES_DIR) && name.endsWith(ROUTING_FILE_EXT)) {
			return ROUTING;
		} else if (name.endsWith(BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT) || name.endsWith(BINARY_WIKI_MAP_INDEX_EXT)) {
			return WIKI_AND_TRAVEL_MAPS;
		} else if (name.endsWith(BINARY_MAP_INDEX_EXT)) {
			return REGULAR_MAPS;
		} else if (path.startsWith(app.getCacheDir().getAbsolutePath()) && (path.contains(WEATHER_FORECAST_DIR)
				|| path.contains(GEOTIFF_SQLITE_CACHE_DIR))) {
			return file.isFile() ? CACHE : null;
		}
		return file.isFile() ? OTHER : null;
	}
}