package net.osmand.plus.profiles;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.plus.R;

public enum NavigationIcon {

	DEFAULT(R.drawable.map_navigation_default),
	NAUTICAL(R.drawable.map_navigation_nautical),
	CAR(R.drawable.map_navigation_car),
	MODEL(R.drawable.map_navigation_default);

	NavigationIcon(@DrawableRes int iconId) {
		this.iconId = iconId;
	}

	@DrawableRes
	private final int iconId;

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	public static boolean isModel(@NonNull String name) {
		return name.startsWith(IndexConstants.MODEL_NAME_PREFIX);
	}

	@NonNull
	public static NavigationIcon fromName(@NonNull String name) {
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