package net.osmand.plus.card.color.palette.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.card.color.palette.main.data.PaletteColor;

public interface OnColorsPaletteListener {

	void onColorSelectedFromPalette(@NonNull PaletteColor paletteColor);

	default void onColorAddedToPalette(@Nullable PaletteColor oldColor, @NonNull PaletteColor newColor) {
	}

}
