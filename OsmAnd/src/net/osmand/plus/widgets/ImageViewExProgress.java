package net.osmand.plus.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ImageViewExProgress extends ImageView {

	public float percent;
	public int color1;
	public int color2;

	public ImageViewExProgress(Context context) {
		super(context);
	}

	public ImageViewExProgress(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public ImageViewExProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(21)
	public ImageViewExProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}


	@Override
	public void draw(Canvas canvas) {
		canvas.save();
		setColorFilter(color1);
		int width = getWidth();
		int widthP = (int) (width * percent);
		int height = getHeight();
		canvas.clipRect(new Rect(0, 0, widthP, height));
		super.draw(canvas);
		canvas.restore();

		canvas.save();
		setColorFilter(color2);
		int width2 = getWidth();
		int widthP2 = (int) (width2 * percent);
		int height2 = getHeight();
		canvas.clipRect(new Rect(widthP2, 0, width2, height2));
		super.draw(canvas);
		canvas.restore();
	}
}