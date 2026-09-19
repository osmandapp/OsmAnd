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
import net.osmand.plus.settings.backend.backup.items.MarkersSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.Collections;
import java.util.List;

class ActiveMarkersExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.map_markers;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_flag;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		List<MapMarker> mapMarkers = app.getMapMarkersHelper().getMapMarkers();
		if (!mapMarkers.isEmpty()) {
			String name = app.getString(getTitleId());
			String groupId = ExportType.ACTIVE_MARKERS.name();
			MapMarkersGroup markersGroup = new MapMarkersGroup(groupId, name, ItineraryType.MARKERS);
			markersGroup.setMarkers(mapMarkers);
			return Collections.singletonList(markersGroup);
		}
		return Collections.emptyList();
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		MarkersSettingsItem markersSettingsItem = (MarkersSettingsItem) settingsItem;
		return Collections.singletonList(markersSettingsItem.getMarkersGroup());
	}

	@Override
	public boolean isRelatedObject(@NonNull OsmandApplication app, @NonNull Object object) {
		if (object instanceof MapMarker) {
			return !((MapMarker) object).history;
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
		return SettingsItemType.ACTIVE_MARKERS;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return LocalItemType.ACTIVE_MARKERS;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> getRelatedPluginClass() {
		return null;
	}
}
