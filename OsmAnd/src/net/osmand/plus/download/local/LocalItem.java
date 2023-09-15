package net.osmand.plus.download.local;

import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;
import static net.osmand.plus.download.local.ItemType.FAVORITES;
import static net.osmand.plus.download.local.ItemType.MAP_SOURCES;
import static net.osmand.plus.download.local.ItemType.MULTIMEDIA_NOTES;
import static net.osmand.plus.download.local.ItemType.OSM_EDITS;
import static net.osmand.plus.download.local.ItemType.OSM_NOTES;
import static net.osmand.plus.download.local.ItemType.PROFILES;
import static net.osmand.plus.download.local.ItemType.RENDERING_STYLES;
import static net.osmand.plus.download.local.ItemType.TRACKS;
import static net.osmand.plus.download.local.ItemType.VOICE_DATA;
import static net.osmand.plus.settings.backend.OsmandSettings.SHARED_PREFERENCES_NAME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.map.OsmandRegions;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Map;

public class LocalItem {

	private final File file;
	private final ItemType type;
	private final CharSequence name;
	private final long size;

	@Nullable
	private Object attachedObject;


	public LocalItem(@NonNull OsmandApplication app, @NonNull File file, @NonNull ItemType type) {
		this.file = file;
		this.type = type;
		this.size = file.length();
		this.name = acquireNameAndObject(app);
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@NonNull
	public CharSequence getName() {
		return name;
	}

	@NonNull
	public ItemType getType() {
		return type;
	}

	public long getSize() {
		return size;
	}

	@Nullable
	public Object getAttachedObject() {
		return attachedObject;
	}

	public boolean isBackupedData(@NonNull OsmandApplication app) {
		File backupDir = app.getAppPath(BACKUP_INDEX_DIR);
		return file.getAbsolutePath().startsWith(backupDir.getAbsolutePath());
	}

	@NonNull
	private String acquireNameAndObject(@NonNull OsmandApplication app) {
		String fileName = file.getName();
		if (type == MULTIMEDIA_NOTES) {
			attachedObject = new Recording(file);
			return ((Recording) attachedObject).getName(app, true);
		} else if (type == TRACKS) {
			return GpxUiHelper.getGpxTitle(fileName);
		} else if (type == RENDERING_STYLES) {
			Map<String, String> renderers = app.getRendererRegistry().getRenderers(true);
			for (Map.Entry<String, String> entry : renderers.entrySet()) {
				if (Algorithms.stringsEqual(entry.getValue(), fileName)) {
					attachedObject = entry.getKey();
					return RendererRegistry.getRendererName(app, (String) attachedObject);
				}
			}
		} else if (type == VOICE_DATA) {
			return FileNameTranslationHelper.getVoiceName(app, fileName);
		} else if (type == PROFILES) {
			String key = Algorithms.getFileNameWithoutExtension(fileName);
			if (key.equals(SHARED_PREFERENCES_NAME)) {
				return app.getString(R.string.osmand_settings);
			}
			int index = key.lastIndexOf('.');
			if (index != -1) {
				key = key.substring(index + 1);
			}
			attachedObject = ApplicationMode.valueOfStringKey(key, null);
			if (attachedObject != null) {
				return ((ApplicationMode) attachedObject).toHumanString();
			}
		} else if (type == MAP_SOURCES) {
			if (file.isDirectory() && TileSourceManager.isTileSourceMetaInfoExist(file)) {
				attachedObject = TileSourceManager.createTileSourceTemplate(file);
			} else if (file.isFile() && fileName.endsWith(SQLiteTileSource.EXT)) {
				attachedObject = new SQLiteTileSource(app, file, TileSourceManager.getKnownSourceTemplates());
			}
		} else if (type == OSM_EDITS) {
			return app.getString(type.getTitleId());
		} else if (type == OSM_NOTES) {
			return app.getString(type.getTitleId());
		}

		OsmandRegions regions = app.getResourceManager().getOsmandRegions();
		String name = FileNameTranslationHelper.getFileName(app, regions, fileName, true, true);
		return name != null ? name : fileName;
	}
}
