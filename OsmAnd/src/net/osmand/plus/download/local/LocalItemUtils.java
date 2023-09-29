package net.osmand.plus.download.local;

import static net.osmand.IndexConstants.BINARY_DEPTH_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.FONT_INDEX_EXT;
import static net.osmand.IndexConstants.GEOTIFF_SQLITE_CACHE_DIR;
import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.HEIGHTMAP_SQLITE_EXT;
import static net.osmand.IndexConstants.LIVE_INDEX_DIR;
import static net.osmand.IndexConstants.RENDERER_INDEX_EXT;
import static net.osmand.IndexConstants.ROUTING_FILE_EXT;
import static net.osmand.IndexConstants.ROUTING_PROFILES_DIR;
import static net.osmand.IndexConstants.TIF_EXT;
import static net.osmand.IndexConstants.TILES_INDEX_DIR;
import static net.osmand.IndexConstants.VOICE_INDEX_DIR;
import static net.osmand.IndexConstants.WEATHER_EXT;
import static net.osmand.IndexConstants.WEATHER_FORECAST_DIR;
import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.plus.download.local.LocalItemType.ACTIVE_MARKERS;
import static net.osmand.plus.download.local.LocalItemType.CACHE;
import static net.osmand.plus.download.local.LocalItemType.DEPTH_DATA;
import static net.osmand.plus.download.local.LocalItemType.FAVORITES;
import static net.osmand.plus.download.local.LocalItemType.FONT_DATA;
import static net.osmand.plus.download.local.LocalItemType.ITINERARY_GROUPS;
import static net.osmand.plus.download.local.LocalItemType.LIVE_UPDATES;
import static net.osmand.plus.download.local.LocalItemType.MAP_DATA;
import static net.osmand.plus.download.local.LocalItemType.MULTIMEDIA_NOTES;
import static net.osmand.plus.download.local.LocalItemType.OSM_EDITS;
import static net.osmand.plus.download.local.LocalItemType.OSM_NOTES;
import static net.osmand.plus.download.local.LocalItemType.OTHER;
import static net.osmand.plus.download.local.LocalItemType.PROFILES;
import static net.osmand.plus.download.local.LocalItemType.RENDERING_STYLES;
import static net.osmand.plus.download.local.LocalItemType.ROAD_DATA;
import static net.osmand.plus.download.local.LocalItemType.ROUTING;
import static net.osmand.plus.download.local.LocalItemType.TERRAIN_DATA;
import static net.osmand.plus.download.local.LocalItemType.TILES_DATA;
import static net.osmand.plus.download.local.LocalItemType.TRACKS;
import static net.osmand.plus.download.local.LocalItemType.TTS_VOICE_DATA;
import static net.osmand.plus.download.local.LocalItemType.VOICE_DATA;
import static net.osmand.plus.download.local.LocalItemType.WEATHER_DATA;
import static net.osmand.plus.download.local.LocalItemType.WIKI_AND_TRAVEL_MAPS;
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
import static java.text.DateFormat.SHORT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.OsmandRegions;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.mapmarkers.ItineraryDataHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.voice.JsMediaCommandPlayer;
import net.osmand.plus.voice.JsTtsCommandPlayer;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LocalItemUtils {

	private static final Log log = PlatformUtil.getLog(LocalItemUtils.class);

	@NonNull
	public static String getFormattedDate(@NonNull Date date) {
		return DateFormat.getDateInstance(SHORT).format(date);
	}

	public static void updateItem(@NonNull OsmandApplication app, @NonNull LocalItem item) {
		File file = item.getFile();
		String fileName = file.getName();
		LocalItemType type = item.getType();

		Date date = getInstalledDate(app, file, type);
		item.setLastModified(date.getTime());

		OsmandRegions regions = app.getResourceManager().getOsmandRegions();
		String localeName = FileNameTranslationHelper.getFileName(app, regions, fileName, true, true);
		if (!Algorithms.isEmpty(localeName)) {
			item.setName(localeName);
		}
		String size = AndroidUtils.formatSize(app, file.length());
		if (type == CACHE) {
			item.setDescription(size);
		} else {
			String formattedDate = getFormattedDate(date);
			item.setDescription(app.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, formattedDate));
		}
		if (type == MULTIMEDIA_NOTES) {
			Recording recording = new Recording(file);
			item.setName(recording.getName(app, true));
			item.setAttachedObject(recording);
		} else if (type == TRACKS) {
			item.setName(GpxUiHelper.getGpxTitle(fileName));
			item.setAttachedObject(app.getGpxDbHelper().getItem(file, item::setAttachedObject));
		} else if (type == TILES_DATA) {
			ITileSource template = null;
			if (file.isDirectory() && TileSourceManager.isTileSourceMetaInfoExist(file)) {
				template = TileSourceManager.createTileSourceTemplate(file);
			} else if (file.isFile() && fileName.endsWith(SQLiteTileSource.EXT)) {
				template = new SQLiteTileSource(app, file, TileSourceManager.getKnownSourceTemplates());
			}
			if (template != null) {
				item.setAttachedObject(template);
			}
		} else if (type == PROFILES) {
			String key = Algorithms.getFileNameWithoutExtension(fileName);
			if (Algorithms.equalsToAny(key, SHARED_PREFERENCES_NAME, CUSTOM_SHARED_PREFERENCES_PREFIX)) {
				item.setName(app.getString(R.string.osmand_settings));
			} else {
				int index = key.lastIndexOf('.');
				if (index != -1) {
					key = key.substring(index + 1);
				}
				ApplicationMode mode = ApplicationMode.valueOfStringKey(key, null);
				if (mode != null) {
					item.setAttachedObject(mode);
					item.setName(mode.toHumanString());
				}
			}
		} else if (type == RENDERING_STYLES) {
			Map<String, String> renderers = app.getRendererRegistry().getRenderers(true);
			for (Map.Entry<String, String> entry : renderers.entrySet()) {
				if (Algorithms.stringsEqual(entry.getValue(), fileName)) {
					String key = entry.getKey();
					item.setAttachedObject(key);
					item.setName(RendererRegistry.getRendererName(app, key));
					break;
				}
			}
		} else if (Algorithms.equalsToAny(type, OSM_EDITS, OSM_NOTES, ACTIVE_MARKERS)) {
			item.setName(type.toHumanString(app));
		} else if (type == CACHE) {
			if (fileName.startsWith("heightmap")) {
				item.setName(app.getString(R.string.relief_3d));
			} else if (fileName.startsWith("hillshade")) {
				item.setName(app.getString(R.string.shared_string_hillshade));
			} else if (fileName.startsWith("slope")) {
				item.setName(app.getString(R.string.shared_string_slope));
			} else if (fileName.equals("weather_tiffs.db")) {
				item.setName(app.getString(R.string.weather_online));
			}
		} else if (Algorithms.equalsToAny(type, VOICE_DATA, TTS_VOICE_DATA)) {
			item.setName(FileNameTranslationHelper.getVoiceName(app, fileName));
		}
	}

	@NonNull
	public static Date getInstalledDate(@NonNull OsmandApplication app, @NonNull File file, @NonNull LocalItemType type) {
		if (Algorithms.equalsToAny(type, MAP_DATA, ROAD_DATA)) {
			ResourceManager resourceManager = app.getResourceManager();
			String fileModifiedDate = resourceManager.getIndexFileNames().get(file.getName());
			if (fileModifiedDate != null) {
				try {
					Date date = resourceManager.getDateFormat().parse(fileModifiedDate);
					if (date != null) {
						return date;
					}
				} catch (Exception e) {
					log.error(e);
				}
			}
		}
		return new Date(file.lastModified());
	}

	public static boolean addUnique(@NonNull List<LocalItem> results, @NonNull List<LocalItem> items) {
		int size = results.size();
		for (LocalItem item : items) {
			boolean needAdd = true;
			for (LocalItem result : results) {
				if (result.getName().equals(item.getName())) {
					needAdd = false;
					break;
				}
			}
			if (needAdd) {
				results.add(item);
			}
		}
		return size != results.size();
	}

	@Nullable
	public static LocalItemType getItemType(@NonNull OsmandApplication app, @NonNull File file) {
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
			return WEATHER_DATA;
		} else if (name.endsWith(BINARY_DEPTH_MAP_INDEX_EXT)) {
			return DEPTH_DATA;
		} else if (name.endsWith(TIF_EXT) || SrtmDownloadItem.isSrtmFile(name)) {
			return TERRAIN_DATA;
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
				return TILES_DATA;
			}
			if (file.isDirectory()) {
				File parent = file.getParentFile();
				String parentName = parent != null ? parent.getName() : null;

				if (Algorithms.stringsEqual(TILES_INDEX_DIR.replace("/", ""), parentName)) {
					return TILES_DATA;
				}
			}
			return null;
		} else if ((path.contains(SHARED_PREFERENCES_NAME) || path.contains(CUSTOM_SHARED_PREFERENCES_PREFIX)) && name.endsWith(ROUTING_FILE_EXT)) {
			return PROFILES;
		} else if (path.contains(ROUTING_PROFILES_DIR) && name.endsWith(ROUTING_FILE_EXT)) {
			return ROUTING;
		} else if (name.endsWith(BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT) || name.endsWith(BINARY_WIKI_MAP_INDEX_EXT)) {
			return WIKI_AND_TRAVEL_MAPS;
		} else if (path.contains(LIVE_INDEX_DIR)) {
			return LIVE_UPDATES;
		} else if (name.endsWith(BINARY_MAP_INDEX_EXT)) {
			return name.endsWith(BINARY_ROAD_MAP_INDEX_EXT) ? ROAD_DATA : MAP_DATA;
		} else if (path.startsWith(app.getCacheDir().getAbsolutePath()) && (path.contains(WEATHER_FORECAST_DIR)
				|| path.contains(GEOTIFF_SQLITE_CACHE_DIR))) {
			return file.isFile() ? CACHE : null;
		}
		return file.isFile() ? OTHER : null;
	}
}