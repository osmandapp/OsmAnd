package net.osmand.plus.settings.enums;

import android.content.pm.ActivityInfo;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum ScreenOrientation {

	PORTRAIT(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
			R.string.map_orientation_portrait,
			R.drawable.ic_action_phone_portrait_orientation),

	PORTRAIT_INVERTED(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
			R.string.map_orientation_portrait_inverted,
			R.drawable.ic_action_phone_portrait_orientation_inverted),

	LANDSCAPE(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
			R.string.map_orientation_landscape,
			R.drawable.ic_action_phone_landscape_orientation),

	LANDSCAPE_INVERTED(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
			R.string.map_orientation_landscape_inverted,
			R.drawable.ic_action_phone_landscape_orientation_inverted),

	DEFAULT(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
			R.string.map_orientation_default,
			R.drawable.ic_action_phone_device_orientation);

	private final int value;
	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	ScreenOrientation(int value, @StringRes int titleId, @DrawableRes int iconId) {
		this.value = value;
		this.titleId = titleId;
		this.iconId = iconId;
	}

	public int getValue() {
		return value;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public static ScreenOrientation fromValue(int value) {
		for (ScreenOrientation o : values()) {
			if (o.value == value) return o;
		}
		return DEFAULT;
	}
}

