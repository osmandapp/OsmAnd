package net.osmand.plus.mapcontextmenu.other;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

import java.util.Arrays;
import java.util.List;

public enum ShareItem {

	MESSAGE(R.drawable.ic_action_message, R.string.shared_string_send),
	CLIPBOARD(R.drawable.ic_action_copy, R.string.shared_string_copy),
	ADDRESS(R.drawable.ic_action_street_name, R.string.copy_address),
	NAME(R.drawable.ic_action_copy, R.string.copy_poi_name),
	COORDINATES(R.drawable.ic_action_coordinates_location, R.string.copy_coordinates),
	GEO(R.drawable.ic_world_globe_dark, R.string.share_geo);

	@DrawableRes
	private final int iconId;
	@StringRes
	private final int titleId;

	ShareItem(@DrawableRes int iconId, @StringRes int titleId) {
		this.iconId = iconId;
		this.titleId = titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public static List<ShareItem> getNativeShareItems() {
		return Arrays.asList(ADDRESS, NAME, COORDINATES, GEO);
	}
}
