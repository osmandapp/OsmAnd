package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class HistoryMarkersExportType extends ExportType {

	public HistoryMarkersExportType() {
		super(R.string.markers_history, R.drawable.ic_action_flag, SettingsItemType.HISTORY_MARKERS);
	}

	@NonNull
	@Override
	public String getId() {
		return "HISTORY_MARKERS";
	}
}