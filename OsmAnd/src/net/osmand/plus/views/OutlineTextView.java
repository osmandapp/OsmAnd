package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class OutlineTextView extends AppCompatTextView {

	private boolean drawOutline = false;
	private TextPaint strokePaint;

	public OutlineTextView(@NonNull Context context) {
		super(context);
		initResources();
	}

	public OutlineTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initResources();
	}

	public OutlineTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initResources();
	}

	private void initResources() {
		strokePaint = new TextPaint(getPaint());
		strokePaint.setStyle(Paint.Style.STROKE);
		strokePaint.setAntiAlias(true);
	}

	public void setStrokeColor(int strokeColor) {
		strokePaint.setColor(strokeColor);
	}

	public void setStrokeWidth(int strokeWidth) {
		strokePaint.setStrokeWidth(strokeWidth);
	}

	public void showOutline(boolean show) {
		drawOutline = show;
	}

	@ColorInt
	public int getStrokeColor() {
		return strokePaint.getColor();
	}

	public int getStrokeWidth() {
		return (int) strokePaint.getStrokeWidth();
	}

	public boolean shouldShowOutline() {
		return drawOutline;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (drawOutline && strokePaint.getStrokeWidth() > 0) {
			CharSequence text = getText();
			if (TextUtils.isEmpty(text)) return;

			Layout layout = getLayout();
			if (layout == null) {
				layout = createFallbackLayout();
			}
			TextPaint paint = getPaint();
			strokePaint.setTypeface(getTypeface());
			strokePaint.setLetterSpacing(paint.getLetterSpacing());
			strokePaint.setTextSize(paint.getTextSize());

			int save = canvas.save();
			canvas.translate(getTotalPaddingLeft(), getTotalPaddingTop());

			for (int i = 0; i < layout.getLineCount(); i++) {
				int lineStart = layout.getLineStart(i);
				int lineEnd = layout.getLineEnd(i);
				int ellipsisStart = layout.getEllipsisStart(i);
				int ellipsisCount = layout.getEllipsisCount(i);

				float x = layout.getLineLeft(i);
				float y = layout.getLineBaseline(i);

				CharSequence lineText = text.subSequence(lineStart, lineEnd);
				if (ellipsisCount > 0 && ellipsisStart >= 0 && ellipsisStart < lineText.length()) {
					lineText = TextUtils.concat(lineText.subSequence(0, ellipsisStart), "…");
				}
				canvas.drawText(lineText, 0, lineText.length(), x, y, strokePaint);
			}

			canvas.restoreToCount(save);
			super.onDraw(canvas);
		} else {
			super.onDraw(canvas);
		}
	}

	private Layout.Alignment getAlignment()  {
		return switch (getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) {
			case Gravity.CENTER_HORIZONTAL -> Layout.Alignment.ALIGN_CENTER;
			case Gravity.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE;
			default -> Layout.Alignment.ALIGN_NORMAL;
		};
	}

	private Layout createFallbackLayout() {
		int availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
		return StaticLayout.Builder.obtain(getText(), 0, getText().length(), getPaint(), availableWidth)
				.setAlignment(getAlignment())
				.setEllipsize(getEllipsize())
				.setMaxLines(getMaxLines())
				.setIncludePad(getIncludeFontPadding())
				.build();
	}
}

