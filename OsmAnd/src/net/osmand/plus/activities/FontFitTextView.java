package net.osmand.plus.activities;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.widget.TextView;

/* Based on 
 * from http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview
 */
public class FontFitTextView extends TextView {

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

	private void refitText(String text, int textWidth) {
		if (textWidth > 0) {
			Drawable left = getCompoundDrawables()[0];
			Drawable right = getCompoundDrawables()[2];
			float availableWidth = textWidth - this.getPaddingLeft()
					- this.getPaddingRight() - this.getCompoundDrawablePadding()
					- (left != null ? left.getMinimumWidth() : 0)
					- (right != null ? right.getMinimumWidth() : 0);

			setTextScaleX(1f);
			TextPaint tp = getPaint();
			float size = tp.measureText(text);

			if (size > availableWidth)
				setTextScaleX(availableWidth / size);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		refitText(this.getText().toString(), parentWidth);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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

}
