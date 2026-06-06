package net.osmand.plus.plugins.audionotes;

public enum AVActionType {
	REC_AUDIO,
	REC_VIDEO,
	REC_PHOTO;

	public boolean isPhoto() {
		return this == REC_PHOTO;
	}

	public boolean isAudio() {
		return this == REC_AUDIO;
	}

	public boolean isVideo() {
		return this == REC_VIDEO;
	}
}
