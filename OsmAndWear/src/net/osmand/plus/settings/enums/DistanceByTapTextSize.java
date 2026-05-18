package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum DistanceByTapTextSize {

	NORMAL(R.string.shared_string_normal, R.dimen.distance_by_tap_normal_text_size),
	LARGE(R.string.shared_string_large, R.dimen.map_widget_text_size);

	@StringRes
	private final int key;
	@DimenRes
	private final int textSizeId;

	DistanceByTapTextSize(@StringRes int key, @DimenRes int textSizeId) {
		this.key = key;
		this.textSizeId = textSizeId;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(key);
	}

	@DimenRes
	public int getTextSizeId() {
		return textSizeId;
	}
}