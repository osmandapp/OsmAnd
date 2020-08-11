package net.osmand.view;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

public class GravityDrawable extends Drawable {

    // inner Drawable
    private final Drawable mDrawable;

    public GravityDrawable(Drawable drawable) {
        mDrawable = drawable;
    }

    @Override
    public int getIntrinsicWidth() {
        if (mDrawable != null)  return mDrawable.getIntrinsicWidth(); else return 0;
    }

    @Override
    public int getIntrinsicHeight() {
        if (mDrawable != null) return mDrawable.getIntrinsicHeight(); else return 0;
    }

    @Override
    public void draw(Canvas canvas) {
        int halfCanvas= canvas.getHeight() / 2;
        int halfDrawable = mDrawable.getIntrinsicHeight() / 2;

        // align to top
        canvas.save();
        canvas.translate(0, -halfCanvas + halfDrawable);
        mDrawable.draw(canvas);
        canvas.restore();
    }

    @Override
    public void setAlpha(int i) {
        if (mDrawable != null) mDrawable.setAlpha(i);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        if (mDrawable != null) mDrawable.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        if (mDrawable != null) return mDrawable.getOpacity(); else return PixelFormat.UNKNOWN;
    }

    public void setBoundsFrom(Drawable line2Icon) {
        line2Icon.setBounds(0, 0, line2Icon.getIntrinsicWidth(), line2Icon.getIntrinsicHeight());
        this.setBounds(0, 0, line2Icon.getIntrinsicWidth(), line2Icon.getIntrinsicHeight());
    }
}
