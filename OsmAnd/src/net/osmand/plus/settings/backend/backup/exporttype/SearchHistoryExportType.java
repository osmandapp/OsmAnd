package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class SearchHistoryExportType extends ExportType {

	public SearchHistoryExportType() {
		super(R.string.shared_string_search_history, R.drawable.ic_action_history, SettingsItemType.SEARCH_HISTORY);
	}

	@NonNull
	@Override
	public String getId() {
		return "SEARCH_HISTORY";
	}
}
