package net.osmand.plus.settings.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.util.Algorithms;

public enum FavoritesSortMode {

	SORT_TYPE_DIST(R.string.sort_by_distance, R.drawable.ic_action_list_sort),
	SORT_TYPE_NAME(R.string.sort_by_name, R.drawable.ic_action_sort_by_name),
	SORT_TYPE_CATEGORY(R.string.sort_by_category, R.drawable.ic_action_sort_by_name);

	@StringRes
	private final int nameId;
	@DrawableRes
	private final int iconId;

	FavoritesSortMode(@StringRes int nameId, @DrawableRes int iconId) {
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
	public FavoritesSortMode next() {
		int nextItemIndex = (ordinal() + 1) % values().length;
		return values()[nextItemIndex];
	}

	@NonNull
	public static FavoritesSortMode getDefaultSortMode() {
		return SORT_TYPE_DIST;
	}

	@NonNull
	public static FavoritesSortMode getByValue(@NonNull String name) {
		for (FavoritesSortMode sortMode : values()) {
			if (Algorithms.stringsEqual(sortMode.name(), name)) {
				return sortMode;
			}
		}
		return getDefaultSortMode();
	}
}
