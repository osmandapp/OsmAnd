package net.osmand.plus.track;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum GpxSplitType {

	NO_SPLIT("no_split", -1, R.string.shared_string_none),
	DISTANCE("distance", 1, R.string.distance),
	TIME("time", 2, R.string.shared_string_time);

	private final String typeName;
	private final int type;
	@StringRes
	private final int resId;

	GpxSplitType(@NonNull String typeName, int type, @StringRes int resId) {
		this.typeName = typeName;
		this.type = type;
		this.resId = resId;
	}

	public int getType() {
		return type;
	}

	public String getTypeName() {
		return typeName;
	}

	public String getHumanString(@NonNull Context ctx) {
		return ctx.getString(resId);
	}

	@NonNull
	public static GpxSplitType getSplitTypeByName(@Nullable String name) {
		for (GpxSplitType splitType : values()) {
			if (splitType.name().equalsIgnoreCase(name)) {
				return splitType;
			}
		}
		return NO_SPLIT;
	}

	@NonNull
	public static GpxSplitType getSplitTypeByTypeId(int typeId) {
		for (GpxSplitType splitType : values()) {
			if (splitType.type == typeId) {
				return splitType;
			}
		}
		return NO_SPLIT;
	}
}