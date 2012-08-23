package net.osmand.plus.activities;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/* Based on 
 * from http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview
 */
public class FontFitTextView extends TextView {

	private static float MAX_TEXT_SIZE = 28;

	public FontFitTextView(Context context) {
		this(context, null);
	}

	public FontFitTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
        setSingleLine();
        setEllipsize(TruncateAt.MARQUEE);
	}
	
    // Default constructor override
    public FontFitTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSingleLine();
        setEllipsize(TruncateAt.MARQUEE);
    }

	private void refitText(String text, int textWidth, int textHeight, boolean layout) {
		if (textWidth > 0) {
//			Drawable left = getCompoundDrawables()[0];
//			Drawable right = getCompoundDrawables()[2];
//			float availableWidth = textWidth - this.getPaddingLeft()
//					- this.getPaddingRight() - this.getCompoundDrawablePadding()
//					- (left != null ? left.getMinimumWidth() : 0)
//					- (right != null ? right.getMinimumWidth() : 0);

			float availableWidth = textWidth;
			TextPaint tp = getPaint();
			tp.setTextSize(MAX_TEXT_SIZE);
			int lines = text.length() / 25 + 1;

			Rect rect = new Rect();
			tp.getTextBounds(text, 0, text.length(), rect);
			while (rect.width() > (availableWidth + 5) * lines || rect.height() * lines >  textHeight ) {
				tp.setTextSize(tp.getTextSize() - 1);
				tp.getTextBounds(text, 0, text.length(), rect);
				// setTextScaleX(availableWidth / size);
			}

			//if (getLineCount() != lines) {
				setLines(lines);
				setMaxLines(lines);
				setGravity(Gravity.TOP);
			//}
			setTextSize(TypedValue.COMPLEX_UNIT_PX, tp.getTextSize());
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
		refitText(this.getText().toString(), parentWidth, parentHeight, false);
		this.setMeasuredDimension(parentWidth, parentHeight);
	}

	@Override
	protected void onTextChanged(final CharSequence text, final int start,
			final int before, final int after) {
		refitText(text.toString(), this.getWidth(), this.getHeight(), true);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (w != oldw || h != oldh) {
			refitText(this.getText().toString(), w, h, true);
		}
	}

}
