package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.util.Collections;
import java.util.List;

class VoiceExportType extends AbstractVoiceExportType {

	@Override
	public int getTitleId() {
		return R.string.local_indexes_cat_voice;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_volume_up;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.singletonList(FileSubtype.VOICE);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return LocalItemType.VOICE_DATA;
	}
}
