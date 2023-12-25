package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class GlobalExportType extends ExportType {

	public GlobalExportType() {
		super(R.string.osmand_settings, R.drawable.ic_action_settings, SettingsItemType.GLOBAL);
	}

	@NonNull
	@Override
	public String getId() {
		return "GLOBAL";
	}
}
