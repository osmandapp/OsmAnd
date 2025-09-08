package net.osmand.plus.settings.fragments.voice;

import static net.osmand.plus.download.DownloadResourceGroupType.OTHER_GROUP;
import static net.osmand.plus.download.DownloadResourceGroupType.VOICE_HEADER_REC;
import static net.osmand.plus.download.DownloadResourceGroupType.VOICE_HEADER_TTS;
import static net.osmand.plus.download.DownloadResourceGroupType.VOICE_REC;
import static net.osmand.plus.download.DownloadResourceGroupType.VOICE_TTS;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

enum VoiceType {

	TTS(R.string.tts_title, R.string.tts_description, OTHER_GROUP.getDefaultId()
			+ "#" + VOICE_TTS.getDefaultId() + "#" + VOICE_HEADER_TTS.getDefaultId()),
	RECORDED(R.string.shared_string_recorded, R.string.recorded_description, OTHER_GROUP.getDefaultId()
			+ "#" + VOICE_REC.getDefaultId() + "#" + VOICE_HEADER_REC.getDefaultId());

	@StringRes
	public final int titleRes;
	@StringRes
	public final int descriptionRes;
	public final String indexGroupName;

	VoiceType(int titleRes, int descriptionRes, String indexGroupName) {
		this.titleRes = titleRes;
		this.descriptionRes = descriptionRes;
		this.indexGroupName = indexGroupName;
	}
}
