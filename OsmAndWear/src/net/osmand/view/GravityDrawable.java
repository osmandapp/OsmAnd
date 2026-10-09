package net.osmand.view;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class GravityDrawable extends Drawable {

	// inner Drawable
	private final Drawable original;

	public GravityDrawable(@NonNull Drawable drawable) {
		this.original = drawable;
	}

	@Override
	public int getMinimumHeight() {
		return original.getMinimumHeight();
	}

	@Override
	public int getMinimumWidth() {
		return original.getMinimumWidth();
	}

	@Override
	public int getIntrinsicHeight() {
		return original.getIntrinsicHeight();
	}

	@Override
	public int getIntrinsicWidth() {
		return original.getIntrinsicWidth();
	}

	@Override
	public void setChangingConfigurations(int configs) {
		super.setChangingConfigurations(configs);
		original.setChangingConfigurations(configs);
	}

	@Override
	public void setBounds(int left, int top, int right, int bottom) {
		super.setBounds(left, top, right, bottom);
		original.setBounds(left, top, right, bottom);
	}

	@Override
	public void setAlpha(int alpha) {
		original.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		original.setColorFilter(cf);
	}

	@Override
	public int getOpacity() {
		return original.getOpacity();
	}

	@Override
	public void draw(Canvas canvas) {
		int halfCanvas = getBounds().height() / 3;
		int halfDrawable = original.getIntrinsicHeight() / 3;

		canvas.save();
		canvas.translate(0, -halfCanvas + halfDrawable);
		original.draw(canvas);
		canvas.restore();
	}

	public void setBoundsFrom(Drawable line2Icon) {
		line2Icon.setBounds(0, 0, line2Icon.getIntrinsicWidth(), line2Icon.getIntrinsicHeight());
		this.setBounds(0, 0, line2Icon.getIntrinsicWidth(), line2Icon.getIntrinsicHeight());
	}
}
