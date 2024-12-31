package net.osmand.plus.download.local;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum CategoryType {

	RESOURCES(R.string.shared_string_resources, R.color.local_resources_color),
	MY_PLACES(R.string.shared_string_my_places, R.color.local_my_places_color),
	SETTINGS(R.string.shared_string_settings, R.color.local_settings_color);

	@StringRes
	private final int titleId;
	@ColorRes
	private final int colorId;

	CategoryType(@StringRes int titleId, @ColorRes int colorId) {
		this.titleId = titleId;
		this.colorId = colorId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@ColorRes
	public int getColorId() {
		return colorId;
	}
}
