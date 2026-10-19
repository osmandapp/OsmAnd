package net.osmand.plus.settings.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

public enum LocalSortMode {

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

	LocalSortMode(@StringRes int nameId, @DrawableRes int iconId) {
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
	public static LocalSortMode getDefaultSortMode(@NonNull LocalItemType type) {
		if (type.isSortingByCountrySupported()) {
			return COUNTRY_NAME_ASCENDING;
		} else if (type == LocalItemType.CACHE) {
			return SIZE_DESCENDING;
		}
		return NAME_ASCENDING;
	}

	@NonNull
	public static LocalSortMode[] getSupportedModes(@NonNull LocalItemType type) {
		return type.isSortingByCountrySupported() ? values() : getSimpleModes();
	}

	@NonNull
	public static LocalSortMode[] getSimpleModes() {
		return new LocalSortMode[] {NAME_ASCENDING, NAME_DESCENDING, DATE_ASCENDING, DATE_DESCENDING, SIZE_DESCENDING, SIZE_ASCENDING};
	}

	@NonNull
	public static LocalSortMode getByValue(@NonNull String name) {
		for (LocalSortMode sortMode : values()) {
			if (Algorithms.stringsEqual(sortMode.name(), name)) {
				return sortMode;
			}
		}
		return NAME_ASCENDING;
	}

	public boolean isCountryMode() {
		return CollectionUtils.equalsToAny(this, COUNTRY_NAME_ASCENDING, COUNTRY_NAME_DESCENDING);
	}
}

