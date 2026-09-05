package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MultimediaNotesExportType extends AbstractFileExportType {

	@Override
	public int getTitleId() {
		return R.string.notes;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_grouped_by_type;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		AudioVideoNotesPlugin plugin = PluginsHelper.getActivePlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			List<File> files = new ArrayList<>();
			for (Recording rec : plugin.getAllRecordings()) {
				File file = rec.getFile();
				if (file != null && file.exists()) {
					files.add(file);
				}
			}
			return files;
		}
		return Collections.emptyList();
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		FileSettingsItem fileSettingsItem = (FileSettingsItem) settingsItem;
		return Collections.singletonList(fileSettingsItem);
	}

	@NonNull
	@Override
	public ExportCategory getRelatedExportCategory() {
		return ExportCategory.MY_PLACES;
	}

	@NonNull
	@Override
	public SettingsItemType getRelatedSettingsItemType() {
		return SettingsItemType.FILE;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.singletonList(FileSubtype.MULTIMEDIA_NOTES);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return LocalItemType.MULTIMEDIA_NOTES;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> getRelatedPluginClass() {
		return AudioVideoNotesPlugin.class;
	}
}