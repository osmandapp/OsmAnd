package net.osmand.plus.settings.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum TracksSortByMode {
	BY_DATE(R.string.sort_last_modified, R.drawable.ic_action_time_start),
	BY_NAME_ASCENDING(R.string.sort_name_ascending, R.drawable.ic_action_sort_by_name_ascending),
	BY_NAME_DESCENDING(R.string.sort_name_descending, R.drawable.ic_action_sort_by_name_descending);

	private final int iconId;
	private final int nameId;

	TracksSortByMode(int nameId, int iconId) {
		this.nameId = nameId;
		this.iconId = iconId;
	}

	public boolean isByName() {
		return this == BY_NAME_ASCENDING || this == BY_NAME_DESCENDING;
	}

	public boolean isByDate() {
		return this == BY_DATE;
	}

	@StringRes
	public int getNameId() {
		return nameId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}
}
