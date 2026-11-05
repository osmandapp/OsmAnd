package net.osmand.plus.settings.enums;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public enum SpeedLimitWarningState {

	ALWAYS(R.string.shared_string_always),
	WHEN_EXCEEDED(R.string.when_exceeded);

	@StringRes
	private final int titleId;

	SpeedLimitWarningState(@StringRes int titleId) {
		this.titleId = titleId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public String toHumanString(@NonNull OsmandApplication app) {
		return app.getString(titleId);
	}
}