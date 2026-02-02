package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiContext;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;

public enum ScreenLayoutMode {

	PORTRAIT(R.string.map_orientation_portrait),
	LANDSCAPE(R.string.map_orientation_landscape);

	@StringRes
	private final int nameId;

	ScreenLayoutMode(@StringRes int nameId) {
		this.nameId = nameId;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(nameId);
	}

	@Nullable
	public static ScreenLayoutMode getDefault(@NonNull @UiContext Context ctx) {
		OsmandApplication app = AndroidUtils.getApp(ctx);
		if (app.getSettings().USE_SEPARATE_LAYOUTS.get()) {
			return AndroidUiHelper.isPortrait(ctx) ? PORTRAIT : LANDSCAPE;
		}
		return null;
	}

	public boolean isPortrait() {
		return this == PORTRAIT;
	}
}