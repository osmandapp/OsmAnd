package net.osmand.plus.activities;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

/* Based on 
 * from http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview
 */
public class FontFitTextView extends TextView {

	private static float MAX_TEXT_SIZE = 20;

	public FontFitTextView(Context context) {
		this(context, null);
	}

	public FontFitTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		float size = this.getTextSize();
		if (size > MAX_TEXT_SIZE)
			setTextSize(MAX_TEXT_SIZE);
	}
	
    // Default constructor override
    public FontFitTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
		float size = this.getTextSize();
		if (size > MAX_TEXT_SIZE)
			setTextSize(MAX_TEXT_SIZE);
    }

	private void refitText(String text, int textWidth) {
		if (textWidth > 0) {
			float availableWidth = textWidth - this.getPaddingLeft()
					- this.getPaddingRight();

			TextPaint tp = getPaint();
			Rect rect = new Rect();
			tp.getTextBounds(text, 0, text.length(), rect);
			float size = rect.width();

			if (size > availableWidth)
				setTextScaleX(availableWidth / size);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
		refitText(this.getText().toString(), parentWidth);
		this.setMeasuredDimension(parentWidth, parentHeight);
	}

	@Override
	protected void onTextChanged(final CharSequence text, final int start,
			final int before, final int after) {
		refitText(text.toString(), this.getWidth());
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (w != oldw) {
			refitText(this.getText().toString(), w);
		}
	}
	
//    /**
//     * Resize text after measuring
//     */
//    @Override
//    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        if(changed) { // || mNeedsResize) {
//            int widthLimit = (right - left) - getCompoundPaddingLeft() - getCompoundPaddingRight();
//            int heightLimit = (bottom - top) - getCompoundPaddingBottom() - getCompoundPaddingTop();
//            resizeText(widthLimit, heightLimit);
//        }
//        super.onLayout(changed, left, top, right, bottom);
//    }

}
