package net.osmand.plus.card.color.palette.main.data;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class PredefinedPaletteColor extends PaletteColor {

	@StringRes
	private final int nameId;
	private final String name;

	public PredefinedPaletteColor(@NonNull String id, @ColorInt int color, @StringRes int nameId) {
		this(id, color, nameId, null);
	}

	public PredefinedPaletteColor(@NonNull String id, @ColorInt int color, @NonNull String name) {
		this(id, color, -1, name);
	}

	private PredefinedPaletteColor(@NonNull String id, @ColorInt int color,
	                               @StringRes int nameId, @Nullable String name) {
		super(id, color, 0);
		this.nameId = nameId;
		this.name = name;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return name != null ? name : context.getString(nameId);
	}
}
