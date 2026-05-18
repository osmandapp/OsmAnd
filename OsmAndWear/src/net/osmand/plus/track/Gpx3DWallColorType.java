package net.osmand.plus.track;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum Gpx3DWallColorType {

	NONE("none", R.string.shared_string_none),
	SOLID("solid", R.string.track_coloring_solid),
	DOWNWARD_GRADIENT("downward_gradient", R.string.downward_gradient),
	UPWARD_GRADIENT("upward_gradient", R.string.upward_gradient),
	ALTITUDE("altitude", R.string.altitude),
	SLOPE("slope", R.string.shared_string_slope),
	SPEED("speed", R.string.shared_string_speed);

	private final String typeName;
	private final int displayNameResId;

	Gpx3DWallColorType(@NonNull String typeName, @StringRes int displayNameResId) {
		this.typeName = typeName;
		this.displayNameResId = displayNameResId;
	}

	@NonNull
	public static Gpx3DWallColorType get3DWallColorType(@Nullable String typeName) {
		for (Gpx3DWallColorType type : values()) {
			if (type.typeName.equalsIgnoreCase(typeName)) {
				return type;
			}
		}
		return NONE;
	}

	@NonNull
	public String getTypeName() {
		return typeName;
	}

	@StringRes
	public int getDisplayNameResId() {
		return displayNameResId;
	}

	public boolean isGradient() {
		return this == ALTITUDE || this == SLOPE || this == SPEED;
	}

	public boolean isVerticalGradient() {
		return this == UPWARD_GRADIENT || this == DOWNWARD_GRADIENT;
	}
}