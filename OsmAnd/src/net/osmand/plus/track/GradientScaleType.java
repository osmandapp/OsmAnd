package net.osmand.plus.track;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum GradientScaleType {

	SPEED("gradient_speed_color", R.string.map_widget_speed, R.drawable.ic_action_speed),
	ALTITUDE("gradient_altitude_color", R.string.altitude, R.drawable.ic_action_altitude_average),
	SLOPE("gradient_slope_color", R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent);

	private String typeName;
	@StringRes
	private int resId;
	@DrawableRes
	private int iconId;

	GradientScaleType(@NonNull String typeName, @StringRes int resId, @DrawableRes int iconId) {
		this.typeName = typeName;
		this.resId = resId;
		this.iconId = iconId;
	}

	public String getTypeName() {
		return typeName;
	}

	public int getIconId() {
		return iconId;
	}

	public String getHumanString(@NonNull Context ctx) {
		return ctx.getString(resId);
	}
}
