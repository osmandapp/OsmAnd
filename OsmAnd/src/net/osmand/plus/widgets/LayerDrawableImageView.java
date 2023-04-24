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
			Paint bitmapPaint = new Paint(ANTI_ALIAS_FLAG | FILTER_BITMAP_FLAG);
			int layers = layerSource.getNumberOfLayers() - 1;
			canvas.save();
			canvas.concat(getImageMatrix());
			for (int i = 0; i <= layers; i++) {
				Drawable drawable = layerSource.getDrawable(i);
				if (drawable != null) {
					if (drawable instanceof VectorDrawable) {
						int widthVector = (int) (drawable.getIntrinsicWidth() * scale);
						int heightVector = (int) (drawable.getIntrinsicHeight() * scale);
						Rect boundsVector = new Rect(locationX - widthVector / 2, locationY - heightVector / 2,
								locationX + widthVector / 2, locationY + heightVector / 2);
						drawable.setBounds(boundsVector);
						drawable.draw(canvas);
					} else {
						int width = (int) (drawable.getIntrinsicWidth() * scale);
						int height = (int) (drawable.getIntrinsicHeight() * scale);
						Bitmap srcBitmap = ((BitmapDrawable) drawable).getBitmap();
						Bitmap tmpBitmap = srcBitmap.copy(srcBitmap.getConfig(), true);
						Bitmap bitmap = AndroidUtils.scaleBitmap(tmpBitmap, width, height, false);
						canvas.drawBitmap(bitmap, locationX - width / 2f, locationY - height / 2f, bitmapPaint);
					}
				}
			}
			canvas.restore();
		} else {
			super.onDraw(canvas);
		}
	}
}