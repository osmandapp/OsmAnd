package net.osmand.plus.settings.enums;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum TrackApproximationType {

	MANUAL(R.string.ask_every_time),
	AUTOMATIC(R.string.shared_string_always);

	@StringRes
	private final int nameRes;

	TrackApproximationType(@StringRes int nameRes) {
		this.nameRes = nameRes;
	}

	@StringRes
	public int getNameRes() {
		return nameRes;
	}
}
