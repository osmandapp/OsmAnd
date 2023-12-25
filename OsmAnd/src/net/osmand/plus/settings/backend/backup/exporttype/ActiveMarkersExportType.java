package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class ActiveMarkersExportType extends ExportType {

	public ActiveMarkersExportType() {
		super(R.string.map_markers, R.drawable.ic_action_flag, SettingsItemType.ACTIVE_MARKERS);
	}

	@NonNull
	@Override
	public String getId() {
		return "ACTIVE_MARKERS";
	}
}
