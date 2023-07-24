package net.osmand.plus.settings.fragments;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

enum SpeedSliderType {

	DEFAULT_SPEED_ONLY(R.string.default_speed_setting_title, R.id.min_speed_layout),
	DEFAULT_SPEED(R.string.default_speed_setting_title, R.id.default_speed_layout),
	MIN_SPEED(R.string.shared_string_min_speed, R.id.min_speed_layout),
	MAX_SPEED(R.string.shared_string_max_speed, R.id.max_speed_layout);

	@StringRes
	final int titleId;
	@LayoutRes
	final int layoutId;

	SpeedSliderType(@StringRes int titleId, @LayoutRes int layoutId) {
		this.titleId = titleId;
		this.layoutId = layoutId;
	}
}
