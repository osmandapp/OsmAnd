package net.osmand.plus.settings.backend;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum ExportCategory {

	SETTINGS(R.string.shared_string_settings),
	MY_PLACES(R.string.shared_string_my_places),
	RESOURCES(R.string.shared_string_resources);

	@StringRes
	private final int titleId;

	ExportCategory(@StringRes int titleId) {
		this.titleId = titleId;
	}

	public int getTitleId() {
		return titleId;
	}
}
