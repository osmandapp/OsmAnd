package net.osmand.plus.card.icon;

import androidx.annotation.NonNull;

public interface OnIconsPaletteListener<T> {
	void onIconSelectedFromPalette(@NonNull T icon);
}
