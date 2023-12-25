package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class TtsVoiceExportType extends ExportType {

	public TtsVoiceExportType() {
		super(R.string.local_indexes_cat_tts, R.drawable.ic_action_volume_up, SettingsItemType.FILE);
	}

	@NonNull
	@Override
	public String getId() {
		return "TTS_VOICE";
	}
}
