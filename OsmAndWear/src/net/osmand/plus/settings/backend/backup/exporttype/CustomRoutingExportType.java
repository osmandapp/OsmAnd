package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class CustomRoutingExportType extends AbstractFileExportType {

	@Override
	public int getTitleId() {
		return R.string.shared_string_routing;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_route_distance;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		List<File> routingProfiles = new ArrayList<>();
		File folder = app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
		if (folder.exists() && folder.isDirectory()) {
			File[] fl = folder.listFiles();
			if (fl != null && fl.length > 0) {
				routingProfiles.addAll(Arrays.asList(fl));
			}
		}
		return routingProfiles;
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		FileSettingsItem fileSettingsItem = (FileSettingsItem) settingsItem;
		return Collections.singletonList(fileSettingsItem.getFile());
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
		return Collections.singletonList(FileSubtype.ROUTING_CONFIG);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return LocalItemType.ROUTING;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> getRelatedPluginClass() {
		return null;
	}
}
