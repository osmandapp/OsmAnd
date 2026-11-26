package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum LocationSource {

	GOOGLE_PLAY_SERVICES(R.string.google_play_services),
	ANDROID_API(R.string.android_api);

	@StringRes
	public final int nameId;

	LocationSource(@StringRes int nameId) {
		this.nameId = nameId;
	}

	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(nameId);
	}
}
