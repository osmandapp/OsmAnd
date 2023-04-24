package net.osmand.plus.widgets;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.FILTER_BITMAP_FLAG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.VectorDrawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import net.osmand.plus.utils.AndroidUtils;

public class LayerDrawableImageView extends AppCompatImageView {
	public LayerDrawableImageView(@NonNull Context context) {
		super(context);
	}

	public LayerDrawableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public LayerDrawableImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (getDrawable() instanceof LayerDrawable) {
			int canvasWidth = canvas.getWidth();
			int canvasHeight = canvas.getHeight();
			int locationX = canvasWidth / 2;
			int locationY = canvasHeight / 2;
			LayerDrawable layerSource = (LayerDrawable) getDrawable();
			int drawableWidth = layerSource.getIntrinsicWidth();
			float scale = (float) canvasWidth / drawableWidth;
			canvas.save();
			canvas.concat(getImageMatrix());
			AndroidUtils.drawScaledLayerDrawable(canvas, layerSource, locationX, locationY, scale);
			canvas.restore();
		} else {
			super.onDraw(canvas);
		}
	}
}