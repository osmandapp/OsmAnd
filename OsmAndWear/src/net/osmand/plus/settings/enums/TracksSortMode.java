package net.osmand.plus.settings.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.util.Algorithms;

public enum TracksSortMode {

	NEAREST(R.string.shared_string_nearest, R.drawable.ic_action_nearby),
	LAST_MODIFIED(R.string.sort_last_modified, R.drawable.ic_action_time),
	NAME_ASCENDING(R.string.sort_name_ascending, R.drawable.ic_action_sort_by_name_ascending),
	NAME_DESCENDING(R.string.sort_name_descending, R.drawable.ic_action_sort_by_name_descending),
	DATE_ASCENDING(R.string.sort_date_ascending, R.drawable.ic_action_sort_date_1),
	DATE_DESCENDING(R.string.sort_date_descending, R.drawable.ic_action_sort_date_31),
	DISTANCE_DESCENDING(R.string.sort_distance_descending, R.drawable.ic_action_sort_long_to_short),
	DISTANCE_ASCENDING(R.string.sort_distance_ascending, R.drawable.ic_action_sort_short_to_long),
	DURATION_DESCENDING(R.string.sort_duration_descending, R.drawable.ic_action_sort_duration_long_to_short),
	DURATION_ASCENDING(R.string.sort_duration_ascending, R.drawable.ic_action_sort_duration_short_to_long);

	@StringRes
	private final int nameId;
	@DrawableRes
	private final int iconId;

	TracksSortMode(@StringRes int nameId, @DrawableRes int iconId) {
		this.nameId = nameId;
		this.iconId = iconId;
	}

	@StringRes
	public int getNameId() {
		return nameId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public static TracksSortMode getDefaultSortMode() {
		return LAST_MODIFIED;
	}

	@NonNull
	public static TracksSortMode getByValue(@NonNull String name) {
		for (TracksSortMode sortMode : values()) {
			if (Algorithms.stringsEqual(sortMode.name(), name)) {
				return sortMode;
			}
		}
		return getDefaultSortMode();
	}
}
