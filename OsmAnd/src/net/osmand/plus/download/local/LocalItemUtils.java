package net.osmand.plus.download.local;

import static net.osmand.IndexConstants.*;
import static net.osmand.plus.download.local.LocalItemType.*;
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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.OsmandRegions;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.download.local.dialogs.LiveGroupItem;
import net.osmand.plus.helpers.ColorsPaletteUtils;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.mapmarkers.ItineraryDataHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.LocalSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.voice.JsMediaCommandPlayer;
import net.osmand.plus.voice.JsTtsCommandPlayer;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LocalItemUtils {

	private static final Log log = PlatformUtil.getLog(LocalItemUtils.class);

	private static final long OTHER_MIN_SIZE = 1024 * 1024; // 1MB

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

		if (type == MULTIMEDIA_NOTES) {
			item.setAttachedObject(new Recording(file));
		} else if (type == TRACKS) {
			item.setAttachedObject(app.getGpxDbHelper().getItem(SharedUtil.kFile(file), item::setAttachedObject));
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
			if (!CollectionUtils.equalsToAny(key, SHARED_PREFERENCES_NAME, CUSTOM_SHARED_PREFERENCES_PREFIX)) {
				int index = key.lastIndexOf('.');
				if (index != -1) {
					key = key.substring(index + 1);
				}
				item.setAttachedObject(ApplicationMode.valueOfStringKey(key, null));
			}
		} else if (type == RENDERING_STYLES) {
			Map<String, String> renderers = app.getRendererRegistry().getRenderers(true);
			for (Map.Entry<String, String> entry : renderers.entrySet()) {
				if (Algorithms.stringsEqual(entry.getValue(), fileName)) {
					item.setAttachedObject(entry.getKey());
					break;
				}
			}
		}
	}

	@NonNull
	public static Date getInstalledDate(@NonNull OsmandApplication app, @NonNull File file, @NonNull LocalItemType type) {
		if (CollectionUtils.equalsToAny(type, MAP_DATA, ROAD_DATA)) {
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
				if (Algorithms.objectEquals(result.getFileName(), item.getFileName())) {
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
		String name = file.getName().toLowerCase();
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
		} else if (name.endsWith(BINARY_DEPTH_MAP_INDEX_EXT) || name.equals("world_seamarks.obf") || (path.contains(NAUTICAL_INDEX_DIR) && name.endsWith(BINARY_MAP_INDEX_EXT))) {
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
			if (name.endsWith(SQLiteTileSource.EXT)) {
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
		} else if (path.contains(LIVE_INDEX_DIR) && path.endsWith(BINARY_MAP_INDEX_EXT)) {
			return LIVE_UPDATES;
		} else if (name.endsWith(BINARY_MAP_INDEX_EXT)) {
			return name.endsWith(BINARY_ROAD_MAP_INDEX_EXT) ? ROAD_DATA : MAP_DATA;
		} else if (path.startsWith(app.getCacheDir().getAbsolutePath()) && (path.contains(WEATHER_FORECAST_DIR)
				|| path.contains(GEOTIFF_SQLITE_CACHE_DIR))) {
			return file.isFile() ? CACHE : null;
		} else if (path.contains(COLOR_PALETTE_DIR) && name.endsWith(TXT_EXT)) {
			return COLOR_DATA;
		}
		if (file.isFile() && file.length() >= OTHER_MIN_SIZE) {
			return OTHER;
		}
		return null;
	}

	@NonNull
	public static CharSequence getItemName(@NonNull Context context, @NonNull LocalItem item) {
		LocalItemType type = item.getType();
		String fileName = item.getFileName();
		Object attachedObject = item.getAttachedObject();

		if (CollectionUtils.equalsToAny(type, OSM_EDITS, OSM_NOTES, ACTIVE_MARKERS)) {
			return type.toHumanString(context);
		} else if (type == CACHE) {
			if (fileName.startsWith("heightmap")) {
				return context.getString(R.string.relief_3d);
			} else if (fileName.startsWith("height_cache")) {
				return context.getString(R.string.altitude);
			} else if (fileName.startsWith("hillshade")) {
				return context.getString(R.string.shared_string_hillshade);
			} else if (fileName.startsWith("slope")) {
				return context.getString(R.string.shared_string_slope);
			} else if (fileName.equals("weather_tiffs.db")) {
				return context.getString(R.string.weather_online);
			}
		} else if (CollectionUtils.equalsToAny(type, VOICE_DATA, TTS_VOICE_DATA)) {
			return FileNameTranslationHelper.getVoiceName(context, fileName);
		} else if (type == MULTIMEDIA_NOTES) {
			if (attachedObject instanceof Recording) {
				return ((Recording) attachedObject).getName(context, true);
			}
		} else if (type == TRACKS) {
			return GpxHelper.INSTANCE.getGpxTitle(fileName);
		} else if (type == PROFILES) {
			String key = Algorithms.getFileNameWithoutExtension(fileName);
			if (CollectionUtils.equalsToAny(key, SHARED_PREFERENCES_NAME, CUSTOM_SHARED_PREFERENCES_PREFIX)) {
				return context.getString(R.string.osmand_settings);
			} else if (attachedObject instanceof ApplicationMode) {
				return ((ApplicationMode) attachedObject).toHumanString();
			}
		} else if (type == RENDERING_STYLES) {
			if (attachedObject instanceof String) {
				return RendererRegistry.getRendererName(context, (String) attachedObject);
			}
		} else if (type == COLOR_DATA) {
			return ColorsPaletteUtils.getPaletteName(item.getFile());
		}
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandRegions regions = app.getResourceManager().getOsmandRegions();
		boolean reversed = !getSortModePref(app, type).get().isCountryMode();

		String divider = ", ";
		String name = FileNameTranslationHelper.getFileName(context, regions, fileName, divider, true, reversed);
		if (!Algorithms.isEmpty(name)) {
			int index = name.indexOf(divider);
			if (index != -1) {
				int color = AndroidUtils.getColorFromAttr(context, android.R.attr.textColorSecondary);
				return UiUtilities.createColorSpannable(name, color, name.substring(index));
			}
			return name;
		}
		return Algorithms.getFileNameWithoutExtension(fileName).replace('_', ' ');
	}

	@NonNull
	public static String getItemDescription(@NonNull Context context, @NonNull LocalItem item) {
		String size = AndroidUtils.formatSize(context, item.getFile().length());
		if (item.getType() == CACHE) {
			return size;
		} else if (item.getType() == COLOR_DATA) {
			return ColorsPaletteUtils.getPaletteTypeName(context, item.getFile());
		} else {
			String formattedDate = getFormattedDate(new Date(item.getLastModified()));
			return context.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, formattedDate);
		}
	}

	@NonNull
	public static List<LocalItem> collectLocalItems(@NonNull Set<BaseLocalItem> items) {
		List<LocalItem> localItems = new ArrayList<>();
		for (BaseLocalItem item : items) {
			if (item instanceof LocalItem) {
				localItems.add((LocalItem) item);
			} else if (item instanceof LiveGroupItem) {
				localItems.addAll(((LiveGroupItem) item).getItems());
			}
		}
		return localItems;
	}

	@NonNull
	public static CommonPreference<LocalSortMode> getSortModePref(@NonNull OsmandApplication app, @NonNull LocalItemType type) {
		OsmandSettings settings = app.getSettings();
		String prefId = "local_" + type.name().toLowerCase(Locale.US) + "_sort_mode";
		LocalSortMode defMode = LocalSortMode.getDefaultSortMode(type);
		LocalSortMode[] supportedModes = LocalSortMode.getSupportedModes(type);
		return settings.registerEnumStringPreference(prefId, defMode, supportedModes, LocalSortMode.class).makeGlobal().makeShared();
	}
}