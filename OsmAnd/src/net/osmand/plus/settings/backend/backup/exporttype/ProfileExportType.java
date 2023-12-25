package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class ProfileExportType extends ExportType {

	public ProfileExportType() {
		super(R.string.shared_string_profiles, R.drawable.ic_action_manage_profiles, SettingsItemType.PROFILE);
	}

	@NonNull
	@Override
	public String getId() {
		return "PROFILE";
	}
}
