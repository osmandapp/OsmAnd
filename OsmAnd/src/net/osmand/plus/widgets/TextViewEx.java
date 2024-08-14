package net.osmand.plus.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import net.osmand.plus.R;


public class TextViewEx extends androidx.appcompat.widget.AppCompatTextView {

	public TextViewEx(Context context) {
		super(context);
	}

	public TextViewEx(Context context, AttributeSet attrs) {
		super(context, attrs);

		parseAttributes(this, attrs, 0, 0);
	}

	public TextViewEx(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		parseAttributes(this, attrs, defStyleAttr, 0);
	}

	/*internal*/
	static void parseAttributes(TextView target, AttributeSet attrs, int defStyleAttr,
	                            int defStyleRes) {
		if (attrs == null) {
			return;
		}

		TypedArray resolvedAttrs = target.getContext().getTheme().obtainStyledAttributes(attrs,
				R.styleable.TextViewEx, defStyleAttr, defStyleRes);
		applyAttributes(resolvedAttrs, target);
		resolvedAttrs.recycle();
	}

	private static void applyAttributes(TypedArray resolvedAttributes, TextView target) {
		applyAttribute_textAllCapsCompat(resolvedAttributes, target);
	}

	public static void setAllCapsCompat(TextView target, boolean allCaps) {
		target.setAllCaps(allCaps);
	}

	public void setAllCapsCompat(boolean allCaps) {
		setAllCapsCompat(this, allCaps);
	}

	/*internal*/
	static void applyAttribute_textAllCapsCompat(TypedArray attributes, TextView target) {
		if (!attributes.hasValue(R.styleable.TextViewEx_textAllCapsCompat)) {
			return;
		}

		boolean textAllCaps = attributes.getBoolean(R.styleable.TextViewEx_textAllCapsCompat, false);
		if (!textAllCaps) {
			return;
		}
		setAllCapsCompat(target, true);
	}
}
