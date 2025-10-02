package net.osmand.plus.settings.enums;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum ExternalDeviceShowMode {

	SENSOR_DATA(R.string.sensor_data),
	BATTERY_LEVEL(R.string.battery_level);

	@StringRes
	final int titleId;

	ExternalDeviceShowMode(int titleId) {
		this.titleId = titleId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public ExternalDeviceShowMode next() {
		int nextItemIndex = (ordinal() + 1) % values().length;
		return values()[nextItemIndex];
	}
}