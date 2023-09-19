package net.osmand.plus.download.local;

import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;
import static net.osmand.plus.download.local.ItemType.ACTIVE_MARKERS;
import static net.osmand.plus.download.local.ItemType.CACHE;
import static net.osmand.plus.download.local.ItemType.MAP_SOURCES;
import static net.osmand.plus.download.local.ItemType.MULTIMEDIA_NOTES;
import static net.osmand.plus.download.local.ItemType.OSM_EDITS;
import static net.osmand.plus.download.local.ItemType.OSM_NOTES;
import static net.osmand.plus.download.local.ItemType.PROFILES;
import static net.osmand.plus.download.local.ItemType.REGULAR_MAPS;
import static net.osmand.plus.download.local.ItemType.RENDERING_STYLES;
import static net.osmand.plus.download.local.ItemType.TRACKS;
import static net.osmand.plus.download.local.ItemType.VOICE_DATA;
import static net.osmand.plus.settings.backend.OsmandSettings.SHARED_PREFERENCES_NAME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

public class LocalItem {

	private static final Log log = PlatformUtil.getLog(LocalItem.class);

	private final File file;
	private final ItemType type;
	private final long size;

	private CharSequence name;
	private String description;
	@Nullable
	private Object attachedObject;


	public LocalItem(@NonNull OsmandApplication app, @NonNull File file, @NonNull ItemType type) {
		this.file = file;
		this.type = type;
		this.size = file.length();
		init(app);
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@NonNull
	public ItemType getType() {
		return type;
	}

	public long getSize() {
		return size;
	}

	@NonNull
	public CharSequence getName() {
		return name;
	}

	@NonNull
	public String getDescription() {
		return description;
	}

	@Nullable
	public Object getAttachedObject() {
		return attachedObject;
	}

	public boolean isBackupedData(@NonNull OsmandApplication app) {
		File backupDir = app.getAppPath(BACKUP_INDEX_DIR);
		return file.getAbsolutePath().startsWith(backupDir.getAbsolutePath());
	}

	private void init(@NonNull OsmandApplication app) {
		String fileName = file.getName();
		if (type == MULTIMEDIA_NOTES) {
			attachedObject = new Recording(file);
			name = ((Recording) attachedObject).getName(app, true);
		} else if (type == TRACKS) {
			attachedObject = app.getGpxDbHelper().getItem(file, item -> attachedObject = item);
			name = GpxUiHelper.getGpxTitle(fileName);
		} else if (type == RENDERING_STYLES) {
			Map<String, String> renderers = app.getRendererRegistry().getRenderers(true);
			for (Map.Entry<String, String> entry : renderers.entrySet()) {
				if (Algorithms.stringsEqual(entry.getValue(), fileName)) {
					attachedObject = entry.getKey();
					name = RendererRegistry.getRendererName(app, (String) attachedObject);
				}
			}
		} else if (type == VOICE_DATA) {
			name = FileNameTranslationHelper.getVoiceName(app, fileName);
		} else if (type == PROFILES) {
			String key = Algorithms.getFileNameWithoutExtension(fileName);
			if (key.equals(SHARED_PREFERENCES_NAME)) {
				name = app.getString(R.string.osmand_settings);
			}
			int index = key.lastIndexOf('.');
			if (index != -1) {
				key = key.substring(index + 1);
			}
			attachedObject = ApplicationMode.valueOfStringKey(key, null);
			if (attachedObject != null) {
				name = ((ApplicationMode) attachedObject).toHumanString();
			}
		} else if (type == MAP_SOURCES) {
			if (file.isDirectory() && TileSourceManager.isTileSourceMetaInfoExist(file)) {
				attachedObject = TileSourceManager.createTileSourceTemplate(file);
			} else if (file.isFile() && fileName.endsWith(SQLiteTileSource.EXT)) {
				attachedObject = new SQLiteTileSource(app, file, TileSourceManager.getKnownSourceTemplates());
			}
		} else if (Algorithms.equalsToAny(type, OSM_EDITS, OSM_NOTES, ACTIVE_MARKERS)) {
			name = app.getString(type.getTitleId());
		} else if (type == CACHE) {
			if (fileName.startsWith("heightmap")) {
				name = app.getString(R.string.relief_3d);
			} else if (fileName.startsWith("hillshade")) {
				name = app.getString(R.string.shared_string_hillshade);
			} else if (fileName.startsWith("slope")) {
				name = app.getString(R.string.shared_string_slope);
			} else if (fileName.equals("weather_tiffs.db")) {
				name = app.getString(R.string.weather_online);
			}
		}
		if (Algorithms.isEmpty(name)) {
			OsmandRegions regions = app.getResourceManager().getOsmandRegions();
			String name = FileNameTranslationHelper.getFileName(app, regions, fileName, true, true);
			this.name = name != null ? name : fileName;
		}
		String formattedSize = AndroidUtils.formatSize(app, size);
		if (type == CACHE) {
			description = formattedSize;
		} else {
			String descr = getInstalledDate(app);
			description = app.getString(R.string.ltr_or_rtl_combine_via_bold_point, formattedSize, descr);
		}
	}

	@NonNull
	private String getInstalledDate(@NonNull OsmandApplication app) {
		if (type == REGULAR_MAPS) {
			Map<String, String> fileNames = app.getResourceManager().getIndexFileNames();
			String fileModifiedDate = fileNames.get(file.getName());
			if (fileModifiedDate != null) {
				try {
					Date date = app.getResourceManager().getDateFormat().parse(fileModifiedDate);
					if (date != null) {
						return getInstalledDate(date);
					}
				} catch (Exception e) {
					log.error(e);
				}
			}
		}
		return getInstalledDate(new Date(file.lastModified()));
	}

	@NonNull
	private String getInstalledDate(@NonNull Date date) {
		return DateFormat.getDateInstance(DateFormat.SHORT).format(date);
	}
}
