package net.osmand.plus.card.icon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface OnIconsPaletteListener<T> {
	void onIconSelectedFromPalette(@Nullable T icon);
}
