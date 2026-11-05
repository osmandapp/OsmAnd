package net.osmand.plus.card.icon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IIconsPalette<IconData> {
	void updatePaletteColors();
	void updatePaletteIcons(@Nullable IconData targetIcon);
	void updatePaletteSelection(@Nullable IconData oldIcon, @NonNull IconData newIcon);
}
