package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
		Map<LatLon, AvoidRoadInfo> impassableRoads = avoidSpecificRoads.getImpassableRoads();
		return new ArrayList<>(impassableRoads.values());
	}

	@NonNull
	@Override
	public ExportCategory relatedExportCategory() {
		return ExportCategory.SETTINGS;
	}

	@NonNull
	@Override
	public SettingsItemType relatedSettingsItemType() {
		return SettingsItemType.AVOID_ROADS;
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
