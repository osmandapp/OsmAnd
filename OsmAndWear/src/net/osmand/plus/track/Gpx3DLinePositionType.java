package net.osmand.plus.track;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum Gpx3DLinePositionType {

	TOP("top", R.string.top),
	BOTTOM("bottom", R.string.bottom),
	TOP_BOTTOM("top_bottom", R.string.top_and_bottom);

	private final String typeName;
	private final int displayNameResId;

	Gpx3DLinePositionType(@NonNull String typeName, @StringRes int displayNameResId) {
		this.typeName = typeName;
		this.displayNameResId = displayNameResId;
	}

	@NonNull
	public static Gpx3DLinePositionType get3DLinePositionType(@Nullable String typeName) {
		for (Gpx3DLinePositionType type : values()) {
			if (type.typeName.equalsIgnoreCase(typeName)) {
				return type;
			}
		}
		return TOP;
	}

	public String getTypeName() {
		return typeName;
	}

	@StringRes
	public int getDisplayNameResId() {
		return displayNameResId;
	}
}