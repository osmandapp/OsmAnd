package net.osmand.plus.card.color.palette;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

public interface OnColorsPaletteListener {

	void onColorSelectedFromPalette(@ColorInt int color);

	void onColorAddedToPalette(@Nullable Integer oldColor, @ColorInt int newColor);

}
