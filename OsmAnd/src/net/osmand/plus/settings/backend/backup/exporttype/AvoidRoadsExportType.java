package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class AvoidRoadsExportType extends ExportType {

	public AvoidRoadsExportType() {
		super(R.string.avoid_road, R.drawable.ic_action_alert, SettingsItemType.AVOID_ROADS);
	}

	@NonNull
	@Override
	public String getId() {
		return "AVOID_ROADS";
	}
}
