package net.osmand.plus.widgets.tools;

import android.graphics.Bitmap;

import com.squareup.picasso.Transformation;

public class CropRectTransformation implements Transformation {

	private static final String KEY = "CropRectTransformation";

	@Override
	public Bitmap transform(Bitmap source) {
		int size = Math.min(source.getWidth(), source.getHeight());

		int x = (source.getWidth() - size * 2) / 2;
		int y = (source.getHeight() - size) / 2;

		Bitmap bitmap = Bitmap.createBitmap(source, x, y, size * 2, size);
		if (bitmap != source) {
			source.recycle();
		}

		return bitmap;
	}

	@Override
	public String key() {
		return KEY;
	}
}
