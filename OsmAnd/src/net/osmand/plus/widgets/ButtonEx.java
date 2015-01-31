package net.osmand.plus.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;

import net.osmand.plus.R;
import net.osmand.plus.helpers.FontCache;

/**
 * Created by Alexey Pelykh on 30.01.2015.
 */
public class ButtonEx extends Button {
	public ButtonEx(Context context) {
		super(context);
	}

	public ButtonEx(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (attrs != null) {
			TypedArray resolvedAttrs = context.getTheme().obtainStyledAttributes(attrs,
					R.styleable.OsmandWidgets, 0, 0);
			parseAttributes(resolvedAttrs);
			resolvedAttrs.recycle();
		}
	}

	public ButtonEx(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (attrs != null) {
			TypedArray resolvedAttrs = context.getTheme().obtainStyledAttributes(attrs,
					R.styleable.OsmandWidgets, 0, 0);
			parseAttributes(resolvedAttrs);
			resolvedAttrs.recycle();
		}
	}

	@TargetApi(21)
	public ButtonEx(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		if (attrs != null) {
			TypedArray resolvedAttrs = context.getTheme().obtainStyledAttributes(attrs,
					R.styleable.OsmandWidgets, defStyleAttr, defStyleRes);
			parseAttributes(resolvedAttrs);
			resolvedAttrs.recycle();
		}
	}

	private void parseAttributes(TypedArray resolvedAttributes) {
		if (resolvedAttributes.hasValue(R.styleable.OsmandWidgets_typeface) && !isInEditMode()) {
			String typefaceName = resolvedAttributes.getString(R.styleable.OsmandWidgets_typeface);
			Typeface typeface = FontCache.getFont(getContext(), typefaceName);
			if (typeface != null)
				setTypeface(typeface);
		}
	}
}
