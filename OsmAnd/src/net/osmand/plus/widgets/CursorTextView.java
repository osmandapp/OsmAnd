package net.osmand.plus.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class CursorTextView extends AppCompatTextView {

	private Paint linePaint;
	private Rect bounds;

	public CursorTextView(Context context) {
		super(context);
		init();
	}

	public CursorTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CursorTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		linePaint = new Paint();
		linePaint.setAntiAlias(true);
		linePaint.setFilterBitmap(true);
		linePaint.setColor(Color.RED);
		bounds = new Rect();
		setFocusable(true);
		setFocusableInTouchMode(true);
	}

	@Override
	public boolean getFreezesText() {
		return true;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
		float height = fontMetrics.bottom - fontMetrics.top + fontMetrics.leading;
		Paint paint = getPaint();
		String text = getText().toString();
		paint.getTextBounds(text, 0, text.length(), bounds);
		int width = bounds.width();
		canvas.drawLine(width, 0, width, height, linePaint);
	}
}
