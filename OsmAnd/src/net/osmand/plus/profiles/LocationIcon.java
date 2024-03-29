package net.osmand.plus.profiles;

import androidx.annotation.DrawableRes;

import net.osmand.plus.R;

public enum LocationIcon {

	DEFAULT(R.drawable.map_location_default, R.drawable.map_location_default_view_angle),
	CAR(R.drawable.map_location_car, R.drawable.map_location_car_view_angle),
	BICYCLE(R.drawable.map_location_bicycle, R.drawable.map_location_bicycle_view_angle);

	@DrawableRes
	private final int iconId;
	@DrawableRes
	private final int headingIconId;

	LocationIcon(@DrawableRes int iconId, @DrawableRes int headingIconId) {
		this.iconId = iconId;
		this.headingIconId = headingIconId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@DrawableRes
	public int getHeadingIconId() {
		return headingIconId;
	}
}