package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.avoidroads.AvoidRoadInfo;
import net.osmand.plus.avoidroads.AvoidSpecificRoads;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.AvoidRoadsSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class AvoidRoadsExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.avoid_road;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_alert;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		AvoidSpecificRoads avoidSpecificRoads = app.getAvoidSpecificRoads();
		return new ArrayList<>(avoidSpecificRoads.getImpassableRoads());
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		AvoidRoadsSettingsItem avoidRoadsItem = (AvoidRoadsSettingsItem) settingsItem;
		if (importCompleted) {
			return avoidRoadsItem.getAppliedItems();
		} else {
			return avoidRoadsItem.getItems();
		}
	}

	@Override
	public boolean isRelatedObject(@NonNull OsmandApplication app, @NonNull Object object) {
		return object instanceof AvoidRoadInfo;
	}

	@NonNull
	@Override
	public ExportCategory getRelatedExportCategory() {
		return ExportCategory.SETTINGS;
	}

	@NonNull
	@Override
	public SettingsItemType getRelatedSettingsItemType() {
		return SettingsItemType.AVOID_ROADS;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return null;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> getRelatedPluginClass() {
		return null;
	}
}
