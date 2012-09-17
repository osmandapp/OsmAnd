package net.osmand.plus.activities;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FontFitTextView extends LinearLayout {

	private static float MAX_TEXT_SIZE = 28;
	private TextView tv;

	public FontFitTextView(Context context) {
		this(context, null);
		tv = new TextView(context);
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		tv.setTextColor(Color.WHITE);
		addView(tv, lp);
	}

	public FontFitTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		tv = new TextView(context);
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		tv.setTextColor(Color.WHITE);
		addView(tv, lp);
	}
	
    // Default constructor override
//    public FontFitTextView(Context context, AttributeSet attrs, int defStyle) {
//        super(context, attrs, defStyle);
//    }

	private void refitText(String text, int textWidth, int textHeight, boolean layout) {
		if (textWidth > 0) {
			float availableWidth = textWidth;
			TextPaint tp = new TextPaint();
			tp.setTextSize(MAX_TEXT_SIZE);
			int lines = text.length() / 25 + 1;

			Rect rect = new Rect();
			tp.getTextBounds(text, 0, text.length(), rect);
			while ((rect.width() > (availableWidth + 5) * lines || rect.height() * lines > textHeight) 
					&& tp.getTextSize() > 8) {
				tp.setTextSize(tp.getTextSize() - 1);
				tp.getTextBounds(text, 0, text.length(), rect);
			}

			if (tv.getLineCount() != lines) {
				if (lines == 1) {
					setGravity(Gravity.CENTER_VERTICAL);
				} else {
					setGravity(Gravity.TOP);
				}
				tv.setLines(lines);
				tv.setMaxLines(lines);
			}
			if(tv.getTextSize() != tp.getTextSize()) {
				tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, tp.getTextSize());
				tv.requestLayout();
			}
		}
	}
	
	public void setText(String text){
		tv.setText(text);
		requestLayout();
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if(changed) {
			refitText(tv.getText().toString(), r - l, b - t, true);
		}
	}


}
