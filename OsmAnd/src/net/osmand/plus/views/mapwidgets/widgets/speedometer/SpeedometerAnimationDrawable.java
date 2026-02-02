package net.osmand.plus.views.mapwidgets.widgets.speedometer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SpeedometerAnimationDrawable extends Drawable {
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private float animationProgress = 0f;
	private int backgroundColor = Color.WHITE;
	private int alertColor = Color.RED;
	private final float cornerRadius;

	private float centerX = -1;
	private float centerY = -1;

	public SpeedometerAnimationDrawable(float cornerRadius) {
		this.cornerRadius = cornerRadius;
	}

	public void setAlertColor(int color) {
		if (alertColor != color) {
			this.alertColor = color;
			invalidateSelf();
		}
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public int getAlertColor() {
		return this.alertColor;
	}

	public void setProgress(float progress) {
		this.animationProgress = progress;
		invalidateSelf();
	}

	public float getProgress() {
		return animationProgress;
	}

	public void setColors(int background, int alert) {
		this.backgroundColor = background;
		this.alertColor = alert;
		invalidateSelf();
	}

	private final android.graphics.Path clipPath = new android.graphics.Path();

	@Override
	public void draw(@NonNull Canvas canvas) {
		float width = getBounds().width();
		float height = getBounds().height();

		float cx = centerX > 0 ? centerX : width / 2;
		float cy = centerY > 0 ? centerY : height / 2;

		clipPath.reset();
		clipPath.addRoundRect(0, 0, width, height, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW);

		canvas.save();
		canvas.clipPath(clipPath);

		paint.setColor(backgroundColor);
		canvas.drawRect(0, 0, width, height, paint);

		if (animationProgress > 0f) {
			paint.setColor(alertColor);
			float initCircleRadius = cx - width;
			float maxRadius = width + cx;
			canvas.drawCircle(cx, cy, initCircleRadius + (maxRadius - initCircleRadius) * animationProgress, paint);
		}
		canvas.restore();
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

	public void setAnimationCenter(float x, float y) {
		this.centerX = x;
		this.centerY = y;
		invalidateSelf();
	}
}