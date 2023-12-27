package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.OnlineRoutingSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.Collections;
import java.util.List;

class OnlineRoutingEnginesExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.online_routing_engines;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_world_globe_dark;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		return app.getOnlineRoutingHelper().getOnlyCustomEngines();
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		OnlineRoutingSettingsItem onlineRoutingSettingsItem = (OnlineRoutingSettingsItem) settingsItem;
		return onlineRoutingSettingsItem.getItems();
	}

	@NonNull
	@Override
	public ExportCategory relatedExportCategory() {
		return ExportCategory.RESOURCES;
	}

	@NonNull
	@Override
	public SettingsItemType relatedSettingsItemType() {
		return SettingsItemType.ONLINE_ROUTING_ENGINES;
	}

	@NonNull
	@Override
	public List<FileSubtype> relatedFileSubtypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public LocalItemType relatedLocalItemType() {
		return null;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> relatedPluginClass() {
		return null;
	}
}