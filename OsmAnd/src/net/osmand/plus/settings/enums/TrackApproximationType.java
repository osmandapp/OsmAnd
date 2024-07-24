package net.osmand.plus.settings.enums;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum TrackApproximationType {

	MANUAL(R.string.ask_every_time, R.string.ask_every_time),
	AUTOMATIC(R.string.shared_string_always, R.string.shared_string_automatically);

	@StringRes
	private final int nameRes;

	@StringRes
	private final int actionRes;

	TrackApproximationType(@StringRes int nameRes, @StringRes int actionRes) {
		this.nameRes = nameRes;
		this.actionRes = actionRes;
	}

	@StringRes
	public int getNameRes() {
		return nameRes;
	}

	@StringRes
	public int getActionRes() {
		return actionRes;
	}
}
