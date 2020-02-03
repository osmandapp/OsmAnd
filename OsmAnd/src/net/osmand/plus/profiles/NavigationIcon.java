package net.osmand.plus.profiles;

import android.support.annotation.DrawableRes;

import net.osmand.plus.R;

public enum NavigationIcon {
	DEFAULT(R.drawable.map_navigation_default),
	NAUTICAL(R.drawable.map_navigation_nautical),
	CAR(R.drawable.map_navigation_car);

	NavigationIcon(@DrawableRes int iconId) {
		this.iconId = iconId;
	}

	@DrawableRes
	private final int iconId;

	public int getIconId() {
		return iconId;
	}
}