package net.osmand.plus.views;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ClipRoundCornersDrawable extends Drawable {

	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Path path = new Path();
	private final RectF rect = new RectF();
	private final float cornerRadius;
	@Nullable
	private final Drawable foreground;

	public ClipRoundCornersDrawable(int backgroundColor, float cornerRadius, @Nullable Drawable foreground) {
		this.cornerRadius = cornerRadius;
		this.foreground = foreground;
		paint.setColor(backgroundColor);
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		Rect bounds = getBounds();
		rect.set(bounds);
		path.reset();
		path.addRoundRect(rect, new float[] {
				cornerRadius, cornerRadius,
				cornerRadius, cornerRadius,
				0, 0,
				0, 0
		}, Path.Direction.CW);

		int save = canvas.save();
		canvas.clipPath(path);
		canvas.drawPath(path, paint);
		if (foreground != null) {
			int width = bounds.width();
			int intrinsicWidth = foreground.getIntrinsicWidth();
			int intrinsicHeight = foreground.getIntrinsicHeight();
			int height = intrinsicWidth > 0 && intrinsicHeight > 0
					? Math.round(width * intrinsicHeight / (float) intrinsicWidth)
					: bounds.height();
			foreground.setBounds(bounds.left, bounds.top, bounds.right, bounds.top + height);
			foreground.draw(canvas);
		}
		canvas.restoreToCount(save);
	}

	@Override
	public void setAlpha(int alpha) {
		paint.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		paint.setColorFilter(colorFilter);
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
}
