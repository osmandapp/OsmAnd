package net.osmand.plus.card.color.palette;

import androidx.annotation.ColorInt;

public interface IColorsPalette {
	void updatePalette();
	void updatePaletteSelection(@ColorInt Integer oldColor, @ColorInt int newColor);
}
