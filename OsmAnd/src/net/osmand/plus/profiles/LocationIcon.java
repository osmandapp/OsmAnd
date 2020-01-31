package net.osmand.plus.profiles;

import android.support.annotation.DrawableRes;

import net.osmand.plus.R;

public enum LocationIcon {
	DEFAULT(R.drawable.map_location_default, R.drawable.map_location_default_view_angle),
	CAR(R.drawable.map_location_car, R.drawable.map_location_car_view_angle),
	BICYCLE(R.drawable.map_location_bicycle, R.drawable.map_location_bicycle_view_angle);

	LocationIcon(@DrawableRes int iconId, @DrawableRes int headingIconId) {
		this.iconId = iconId;
		this.headingIconId = headingIconId;
	}

	@DrawableRes
	private final int iconId;
	@DrawableRes
	private final int headingIconId;

	public int getIconId() {
		return iconId;
	}

	public int getHeadingIconId() {
		return headingIconId;
	}
}