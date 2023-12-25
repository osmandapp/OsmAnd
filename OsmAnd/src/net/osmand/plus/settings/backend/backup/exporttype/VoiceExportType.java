package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class VoiceExportType extends ExportType {

	public VoiceExportType() {
		super(R.string.local_indexes_cat_voice, R.drawable.ic_action_volume_up, SettingsItemType.FILE);
	}

	@NonNull
	@Override
	public String getId() {
		return "VOICE";
	}
}
