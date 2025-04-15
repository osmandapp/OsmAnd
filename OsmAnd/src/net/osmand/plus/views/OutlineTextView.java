package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.TextViewEx;

public class OutlineTextView extends TextViewEx {

	private boolean drawOutline = false;
	private int strokeColor = 0;
	private int strokeWidth = 0;

	private boolean isDrawing = false;

	public OutlineTextView(@NonNull Context context) {
		super(context);
		initResources(context);
	}

	public OutlineTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initResources(context);
	}

	public OutlineTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initResources(context);
	}

	private void initResources(@NonNull Context context) {
		strokeColor = ColorUtilities.getCardAndListBackgroundColor(context, false);
	}

	public void setStrokeColor(int strokeColor) {
		this.strokeColor = strokeColor;
	}

	public void setStrokeWidth(int strokeWidth) {
		this.strokeWidth = strokeWidth;
	}

	public void showOutline(boolean show) {
		drawOutline = show;
	}

	@Override
	public void invalidate() {
		if (isDrawing) return;
		super.invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (drawOutline && strokeWidth > 0) {
			isDrawing = true;

			if (strokeColor == 0) {
				initResources(getContext());
			}

			TextPaint paint = getPaint();
			int originalColor = getCurrentTextColor();
			Style originalStyle = paint.getStyle();
			float originalStroke = paint.getStrokeWidth();

			setTextColor(strokeColor);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(strokeWidth);
			super.onDraw(canvas);

			setTextColor(originalColor);
			paint.setStyle(originalStyle);
			paint.setStrokeWidth(originalStroke);
			super.onDraw(canvas);

			isDrawing = false;
		} else {
			super.onDraw(canvas);
		}
	}
}

