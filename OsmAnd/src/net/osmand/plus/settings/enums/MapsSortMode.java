package net.osmand.plus.settings.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

public enum MapsSortMode {

	NAME_ASCENDING(R.string.sort_name_ascending, R.drawable.ic_action_sort_by_name_ascending),
	NAME_DESCENDING(R.string.sort_name_descending, R.drawable.ic_action_sort_by_name_descending),
	COUNTRY_NAME_ASCENDING(R.string.sort_country_name_ascending, R.drawable.ic_action_sort_by_name_ascending),
	COUNTRY_NAME_DESCENDING(R.string.sort_country_name_descending, R.drawable.ic_action_sort_by_name_descending),
	DATE_ASCENDING(R.string.sort_date_ascending, R.drawable.ic_action_sort_date_1),
	DATE_DESCENDING(R.string.sort_date_descending, R.drawable.ic_action_sort_date_31),
	SIZE_DESCENDING(R.string.sort_size_descending, R.drawable.ic_action_sort_duration_long_to_short),
	SIZE_ASCENDING(R.string.sort_size_ascending, R.drawable.ic_action_sort_duration_short_to_long);

	@StringRes
	private final int nameId;
	@DrawableRes
	private final int iconId;

	MapsSortMode(@StringRes int nameId, @DrawableRes int iconId) {
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
	public static MapsSortMode getDefaultSortMode() {
		return COUNTRY_NAME_ASCENDING;
	}

	@NonNull
	public static MapsSortMode getByValue(@NonNull String name) {
		for (MapsSortMode sortMode : values()) {
			if (Algorithms.stringsEqual(sortMode.name(), name)) {
				return sortMode;
			}
		}
		return getDefaultSortMode();
	}

	public boolean isCountryMode() {
		return CollectionUtils.equalsToAny(this, COUNTRY_NAME_ASCENDING, COUNTRY_NAME_DESCENDING);
	}
}

