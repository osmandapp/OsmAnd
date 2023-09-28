package net.osmand.plus.download.local;

import static net.osmand.plus.download.local.LocalItemType.ACTIVE_MARKERS;
import static net.osmand.plus.download.local.LocalItemType.CACHE;
import static net.osmand.plus.download.local.LocalItemType.MAP_DATA;
import static net.osmand.plus.download.local.LocalItemType.MULTIMEDIA_NOTES;
import static net.osmand.plus.download.local.LocalItemType.OSM_EDITS;
import static net.osmand.plus.download.local.LocalItemType.OSM_NOTES;
import static net.osmand.plus.download.local.LocalItemType.PROFILES;
import static net.osmand.plus.download.local.LocalItemType.RENDERING_STYLES;
import static net.osmand.plus.download.local.LocalItemType.ROAD_DATA;
import static net.osmand.plus.download.local.LocalItemType.TILES_DATA;
import static net.osmand.plus.download.local.LocalItemType.TRACKS;
import static net.osmand.plus.download.local.LocalItemType.TTS_VOICE_DATA;
import static net.osmand.plus.download.local.LocalItemType.VOICE_DATA;
import static net.osmand.plus.settings.backend.OsmandSettings.CUSTOM_SHARED_PREFERENCES_PREFIX;
import static net.osmand.plus.settings.backend.OsmandSettings.SHARED_PREFERENCES_NAME;
import static java.text.DateFormat.SHORT;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.OsmandRegions;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
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
}