package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.Collections;
import java.util.List;

class FavoritesBackupExportType extends AbstractFileExportType {

	@Override
	public int getTitleId() {
		return R.string.favorites_backup;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_folder_favorites;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		if (PluginsHelper.isEnabled(OsmandDevelopmentPlugin.class) && offlineBackup) {
			return app.getFavoritesHelper().getFileHelper().getBackupFiles();
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
		return ExportCategory.RESOURCES;
	}

	@NonNull
	@Override
	public SettingsItemType getRelatedSettingsItemType() {
		return SettingsItemType.FILE;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.singletonList(FileSubtype.FAVORITES_BACKUP);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return null;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> getRelatedPluginClass() {
		return OsmandDevelopmentPlugin.class;
	}
}