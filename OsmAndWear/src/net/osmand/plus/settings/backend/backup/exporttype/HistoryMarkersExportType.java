package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.mapmarkers.ItineraryType;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.HistoryMarkersSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.Collections;
import java.util.List;

class HistoryMarkersExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.markers_history;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_flag;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		List<MapMarker> markersHistory = app.getMapMarkersHelper().getMapMarkersHistory();
		if (!markersHistory.isEmpty()) {
			String name = app.getString(R.string.shared_string_history);
			String groupId = ExportType.HISTORY_MARKERS.name();
			MapMarkersGroup markersGroup = new MapMarkersGroup(groupId, name, ItineraryType.MARKERS);
			markersGroup.setMarkers(markersHistory);
			return Collections.singletonList(markersGroup);
		}
		return Collections.emptyList();
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		HistoryMarkersSettingsItem historyMarkersSettingsItem = (HistoryMarkersSettingsItem) settingsItem;
		return Collections.singletonList(historyMarkersSettingsItem.getMarkersGroup());
	}

	@Override
	public boolean isRelatedObject(@NonNull OsmandApplication app, @NonNull Object object) {
		if (object instanceof MapMarker) {
			return ((MapMarker) object).history;
		}
		return false;
	}

	@NonNull
	@Override
	public ExportCategory getRelatedExportCategory() {
		return ExportCategory.MY_PLACES;
	}

	@NonNull
	@Override
	public SettingsItemType getRelatedSettingsItemType() {
		return SettingsItemType.HISTORY_MARKERS;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return LocalItemType.HISTORY_MARKERS;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> getRelatedPluginClass() {
		return null;
	}
}