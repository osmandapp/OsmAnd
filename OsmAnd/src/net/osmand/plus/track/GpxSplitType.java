package net.osmand.plus.track;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum GpxSplitType {

	NO_SPLIT(-1, R.string.shared_string_none),
	DISTANCE(1, R.string.distance),
	TIME(2, R.string.shared_string_time);

	private int type;
	@StringRes
	private int resId;

	GpxSplitType(int type, @StringRes int resId) {
		this.type = type;
		this.resId = resId;
	}

	public int getType() {
		return type;
	}

	public String getHumanString(@NonNull Context ctx) {
		return ctx.getString(resId);
	}
}