package net.osmand.plus.widgets;

import static net.osmand.plus.utils.FontCache.FONT_WEIGHT_NORMAL;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.utils.FontCache;


public class TextViewEx extends androidx.appcompat.widget.AppCompatTextView {


	public TextViewEx(@NonNull Context context) {
		this(context, null);
	}

	public TextViewEx(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);

		parseAttributes(this, attrs, 0, 0);
	}

	public TextViewEx(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		parseAttributes(this, attrs, defStyleAttr, 0);
	}

	protected static void parseAttributes(@NonNull TextView textView, @Nullable AttributeSet attrs,
	                                      int defStyleAttr, int defStyleRes) {
		if (attrs != null) {
			Theme theme = textView.getContext().getTheme();
			TypedArray attributes = theme.obtainStyledAttributes(attrs, R.styleable.TextViewEx, defStyleAttr, defStyleRes);
			applyAttributes(textView, attributes);
			attributes.recycle();
		}
	}

	private static void applyAttributes(@NonNull TextView textView, @NonNull TypedArray attributes) {
		applyTypefaceWeight(textView, attributes);
	}

	private static void applyTypefaceWeight(@NonNull TextView textView, @NonNull TypedArray attributes) {
		if (!attributes.hasValue(R.styleable.TextViewEx_typefaceWeight) || textView.isInEditMode()) {
			return;
		}
		int weight = attributes.getInteger(R.styleable.TextViewEx_typefaceWeight, FONT_WEIGHT_NORMAL);

		Typeface typeface = textView.getTypeface();
		textView.setTypeface(FontCache.getFont(typeface, weight));
	}
}
