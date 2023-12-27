package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.enums.HistorySource;

import java.util.Collections;
import java.util.List;

class NavigationHistoryExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.navigation_history;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_gdirections_dark;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		boolean onlyPoints = false;
		SearchHistoryHelper helper = SearchHistoryHelper.getInstance(app);
		return helper.getHistoryEntries(HistorySource.NAVIGATION, onlyPoints);
	}

	@NonNull
	@Override
	public ExportCategory relatedExportCategory() {
		return ExportCategory.MY_PLACES;
	}

	@NonNull
	@Override
	public SettingsItemType relatedSettingsItemType() {
		return SettingsItemType.NAVIGATION_HISTORY;
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