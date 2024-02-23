package net.osmand.plus.card.color.palette;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.card.color.palette.data.PaletteColor;

public interface IColorsPalette {
	void updatePaletteColors(@Nullable PaletteColor targetPaletteColor);
	void updatePaletteSelection(@Nullable PaletteColor oldColor, @NonNull PaletteColor newColor);
}
