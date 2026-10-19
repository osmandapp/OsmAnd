package net.osmand.plus.settings.enums;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum SunPositionMode {

	SUN_POSITION_MODE(R.string.shared_string_next_event),
	SUNSET_MODE(R.string.shared_string_sunset),
	SUNRISE_MODE(R.string.shared_string_sunrise);

	@StringRes
	final int titleId;

	SunPositionMode(int titleId) {
		this.titleId = titleId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}
}
