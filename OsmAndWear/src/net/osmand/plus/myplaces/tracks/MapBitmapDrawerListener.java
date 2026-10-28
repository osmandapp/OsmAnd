package net.osmand.plus.myplaces.tracks;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

public interface MapBitmapDrawerListener {

	default void onBitmapDrawing() {

	}

	default void onBitmapDrawn(boolean success) {

	}

	default boolean isBitmapSelectionSupported() {
		return false;
	}

	default void onBitmapDrawn(@NonNull Bitmap bitmap) {

	}
}
