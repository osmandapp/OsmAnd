package net.osmand.plus.download.local;

import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;
import static net.osmand.plus.download.local.LocalItemType.ACTIVE_MARKERS;
import static net.osmand.plus.download.local.LocalItemType.CACHE;
import static net.osmand.plus.download.local.LocalItemType.MULTIMEDIA_NOTES;
import static net.osmand.plus.download.local.LocalItemType.OSM_EDITS;
import static net.osmand.plus.download.local.LocalItemType.OSM_NOTES;
import static net.osmand.plus.download.local.LocalItemType.PROFILES;
import static net.osmand.plus.download.local.LocalItemType.RENDERING_STYLES;
import static net.osmand.plus.download.local.LocalItemType.TILES_DATA;
import static net.osmand.plus.download.local.LocalItemType.TRACKS;
import static net.osmand.plus.download.local.LocalItemType.TTS_VOICE_DATA;
import static net.osmand.plus.download.local.LocalItemType.VOICE_DATA;
import static net.osmand.plus.download.local.LocalItemUtils.getFormattedDate;
import static net.osmand.plus.settings.backend.OsmandSettings.CUSTOM_SHARED_PREFERENCES_PREFIX;
import static net.osmand.plus.settings.backend.OsmandSettings.SHARED_PREFERENCES_NAME;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.map.OsmandRegions;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Date;

public class LocalItem implements Comparable<LocalItem> {

	private final File file;
	private final LocalItemType type;
	private final String path;
	private final String fileName;
	private final long size;
	private final boolean backuped;

	@Nullable
	private Object attachedObject;
	private long lastModified;


	public LocalItem(@NonNull File file, @NonNull LocalItemType type) {
		this.file = file;
		this.type = type;
		this.fileName = file.getName();
		this.path = file.getAbsolutePath();
		this.size = file.length();
		this.backuped = path.contains(BACKUP_INDEX_DIR);
		this.lastModified = file.lastModified();
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@NonNull
	public LocalItemType getType() {
		return type;
	}

	@NonNull
	public String getPath() {
		return path;
	}

	@NonNull
	public String getFileName() {
		return fileName;
	}

	public long getSize() {
		return size;
	}

	public boolean isBackuped() {
		return backuped;
	}

	@Nullable
	public Object getAttachedObject() {
		return attachedObject;
	}

	public void setAttachedObject(@Nullable Object object) {
		this.attachedObject = object;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	@NonNull
	public CharSequence getName(@NonNull Context context) {
		if (Algorithms.equalsToAny(type, OSM_EDITS, OSM_NOTES, ACTIVE_MARKERS)) {
			return type.toHumanString(context);
		} else if (type == CACHE) {
			if (fileName.startsWith("heightmap")) {
				return context.getString(R.string.relief_3d);
			} else if (fileName.startsWith("hillshade")) {
				return context.getString(R.string.shared_string_hillshade);
			} else if (fileName.startsWith("slope")) {
				return context.getString(R.string.shared_string_slope);
			} else if (fileName.equals("weather_tiffs.db")) {
				return context.getString(R.string.weather_online);
			}
		} else if (Algorithms.equalsToAny(type, VOICE_DATA, TTS_VOICE_DATA)) {
			return FileNameTranslationHelper.getVoiceName(context, fileName);
		} else if (type == MULTIMEDIA_NOTES) {
			if (attachedObject instanceof Recording) {
				return ((Recording) attachedObject).getName(context, true);
			}
		} else if (type == TRACKS) {
			return GpxUiHelper.getGpxTitle(fileName);
		} else if (type == PROFILES) {
			String key = Algorithms.getFileNameWithoutExtension(fileName);
			if (Algorithms.equalsToAny(key, SHARED_PREFERENCES_NAME, CUSTOM_SHARED_PREFERENCES_PREFIX)) {
				return context.getString(R.string.osmand_settings);
			} else if (attachedObject instanceof ApplicationMode) {
				return ((ApplicationMode) attachedObject).toHumanString();
			}
		} else if (type == RENDERING_STYLES) {
			if (attachedObject instanceof String) {
				return RendererRegistry.getRendererName(context, (String) attachedObject);
			}
		}
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandSettings settings = app.getSettings();
		OsmandRegions regions = app.getResourceManager().getOsmandRegions();
		boolean reversed = !settings.LOCAL_MAPS_SORT_MODE.get().isCountryMode();

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
	public String getDescription(@NonNull Context context) {
		String size = AndroidUtils.formatSize(context, file.length());
		if (type == CACHE) {
			return size;
		} else {
			String formattedDate = getFormattedDate(new Date(lastModified));
			return context.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, formattedDate);
		}
	}

	public boolean isLoaded(@NonNull OsmandApplication app) {
		return !isBackuped() && app.getResourceManager().getIndexFileNames().containsKey(fileName);
	}

	public boolean isCorrupted() {
		if (type == TILES_DATA) {
			return file.isDirectory() && !TileSourceManager.isTileSourceMetaInfoExist(file);
		}
		return false;
	}

	@Override
	public int compareTo(LocalItem item) {
		return fileName.compareTo(item.fileName);
	}

	@NonNull
	@Override
	public String toString() {
		return fileName;
	}
}
