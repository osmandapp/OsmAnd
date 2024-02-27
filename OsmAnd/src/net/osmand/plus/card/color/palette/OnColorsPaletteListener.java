package net.osmand.plus.card.color.palette;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.card.color.palette.data.PaletteColor;

public interface OnColorsPaletteListener {

	void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor);

	default void onColorAddedToPalette(@Nullable PaletteColor oldColor, @NonNull PaletteColor newColor) {
	}

}
