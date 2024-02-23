package net.osmand.plus.card.color.palette.data;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class DefaultPaletteColor extends PaletteColor {

	@StringRes
	private final int nameId;

	public DefaultPaletteColor(@NonNull String id, @ColorInt int color, @StringRes int nameId) {
		super(id, color, 0);
		this.nameId = nameId;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(nameId);
	}
}
