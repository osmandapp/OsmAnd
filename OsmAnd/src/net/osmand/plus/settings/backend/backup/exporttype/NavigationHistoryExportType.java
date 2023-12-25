package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class NavigationHistoryExportType extends ExportType {

	public NavigationHistoryExportType() {
		super(R.string.navigation_history, R.drawable.ic_action_gdirections_dark, SettingsItemType.NAVIGATION_HISTORY);
	}

	@NonNull
	@Override
	public String getId() {
		return "NAVIGATION_HISTORY";
	}
}