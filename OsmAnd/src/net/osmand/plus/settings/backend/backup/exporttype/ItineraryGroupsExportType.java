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
import net.osmand.plus.settings.backend.backup.items.ItinerarySettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.Collections;
import java.util.List;

class ItineraryGroupsExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.shared_string_itinerary;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_flag;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		return app.getMapMarkersHelper().getVisibleMapMarkersGroups();
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		ItinerarySettingsItem itinerarySettingsItem = (ItinerarySettingsItem) settingsItem;
		return itinerarySettingsItem.getItems();
	}

	@NonNull
	@Override
	public ExportCategory relatedExportCategory() {
		return ExportCategory.MY_PLACES;
	}

	@NonNull
	@Override
	public SettingsItemType relatedSettingsItemType() {
		return SettingsItemType.ITINERARY_GROUPS;
	}

	@NonNull
	@Override
	public List<FileSubtype> relatedFileSubtypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public LocalItemType relatedLocalItemType() {
		return LocalItemType.ITINERARY_GROUPS;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> relatedPluginClass() {
		return null;
	}
}
