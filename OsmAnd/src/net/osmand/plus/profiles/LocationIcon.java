package net.osmand.plus.profiles;

import net.osmand.IndexConstants;
import net.osmand.plus.R;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public enum LocationIcon {

	DEFAULT(R.drawable.map_location_default, R.drawable.map_location_default_view_angle),
	CAR(R.drawable.map_location_car, R.drawable.map_location_car_view_angle),
	BICYCLE(R.drawable.map_location_bicycle, R.drawable.map_location_bicycle_view_angle),
	MODEL(R.drawable.map_location_default, R.drawable.map_location_car_view_angle);

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

	public static boolean isModel(@NonNull String name) {
		return name.startsWith(IndexConstants.MODEL_NAME_PREFIX);
	}

	@NonNull
	public static LocationIcon fromName(@NonNull String name) {
		if (isModel(name)) {
			return MODEL;
		}
		try {
			return valueOf(name);
		} catch (IllegalArgumentException e) {
			return DEFAULT;
		}
	}
}