package net.osmand.plus.widgets;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.appcompat.text.AllCapsTransformationMethod;

import net.osmand.plus.R;
import net.osmand.plus.helpers.FontCache;

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

	/*internal*/ static void parseAttributes(TextView target, AttributeSet attrs, int defStyleAttr,
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
		applyAttribute_typeface(resolvedAttributes, target);
		applyAttribute_textAllCapsCompat(resolvedAttributes, target);
	}

	/*internal*/ static void applyAttribute_typeface(TypedArray resolvedAttributes,
													 TextView target) {
		if (!resolvedAttributes.hasValue(R.styleable.TextViewEx_typeface)
				|| target.isInEditMode()) {
			return;
		}

		String typefaceName = resolvedAttributes.getString(R.styleable.TextViewEx_typeface);
		Typeface typeface = FontCache.getFont(target.getContext(), typefaceName);
		int style = target.getTypeface() == null ? 0 : target.getTypeface().getStyle();
		if (typeface != null)
			target.setTypeface(typeface, style);
	}

	public static void setAllCapsCompat(TextView target, boolean allCaps) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			updateAllCapsNewAPI(target, allCaps);
			return;
		}

		if (allCaps) {
			target.setTransformationMethod(new AllCapsTransformationMethod(target.getContext()));
		} else {
			target.setTransformationMethod(null);
		}
	}

	@SuppressLint("NewApi")
	private static void updateAllCapsNewAPI(TextView target, boolean allCaps) {
		target.setAllCaps(allCaps);
	}

	public void setAllCapsCompat(boolean allCaps) {
		setAllCapsCompat(this, allCaps);
	}

	/*internal*/ static void applyAttribute_textAllCapsCompat(TypedArray resolvedAttributes,
														TextView target) {
		if (!resolvedAttributes.hasValue(R.styleable.TextViewEx_textAllCapsCompat)) {
			return;
		}

		boolean textAllCaps = resolvedAttributes.getBoolean(
				R.styleable.TextViewEx_textAllCapsCompat, false);
		if (!textAllCaps) {
			return;
		}
		setAllCapsCompat(target, true);
	}
	
}
