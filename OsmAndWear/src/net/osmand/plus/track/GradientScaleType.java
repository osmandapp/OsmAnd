package net.osmand.plus.track;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.shared.routing.RouteColorize.ColorizationType;

public enum GradientScaleType {

	SPEED("speed", "gradient_speed_color", R.string.shared_string_speed, R.drawable.ic_action_speed),
	ALTITUDE("altitude", "gradient_altitude_color", R.string.altitude, R.drawable.ic_action_altitude_average),
	SLOPE("slope", "gradient_slope_color", R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent);

	private final String typeName;
	private final String colorTypeName;
	@StringRes
	private final int resId;
	@DrawableRes
	private final int iconId;

	GradientScaleType(@NonNull String typeName, @NonNull String colorTypeName, @StringRes int resId, @DrawableRes int iconId) {
		this.typeName = typeName;
		this.colorTypeName = colorTypeName;
		this.resId = resId;
		this.iconId = iconId;
	}

	public String getTypeName() {
		return typeName;
	}

	public String getColorTypeName() {
		return colorTypeName;
	}

	public int getIconId() {
		return iconId;
	}

	public String getHumanString(@NonNull Context ctx) {
		return ctx.getString(resId);
	}

	public ColorizationType toColorizationType() {
		if (this == SPEED) {
			return ColorizationType.SPEED;
		} else if (this == ALTITUDE) {
			return ColorizationType.ELEVATION;
		} else if (this == SLOPE) {
			return ColorizationType.SLOPE;
		} else {
			return ColorizationType.NONE;
		}
	}

	@Nullable
	public static GradientScaleType getGradientTypeByName(@Nullable String name) {
		for (GradientScaleType scaleType : values()) {
			if (scaleType.name().equalsIgnoreCase(name)) {
				return scaleType;
			}
		}
		return null;
	}
}