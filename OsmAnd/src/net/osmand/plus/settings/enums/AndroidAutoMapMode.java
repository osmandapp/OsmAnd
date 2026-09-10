package net.osmand.plus.settings.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum AndroidAutoMapMode {
	DAY(R.string.daynight_mode_day, R.drawable.ic_action_map_day),
	NIGHT(R.string.daynight_mode_night, R.drawable.ic_action_map_night),
	AUTOMATIC(R.string.shared_string_automatic, R.drawable.ic_action_map_sunset);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	AndroidAutoMapMode(@StringRes int titleId, @DrawableRes int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}
}