package net.osmand.plus.onlinerouting;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class VehicleType {

	private final String key;
	@StringRes
	private final int titleId;

	public VehicleType(@NonNull String key,
	                   @StringRes int titleId) {
		this.key = key;
		this.titleId = titleId;
	}

	@NonNull
	public String getKey() {
		return key;
	}

	@NonNull
	public String getTitle(@NonNull Context ctx) {
		return ctx.getString(titleId);
	}
}
