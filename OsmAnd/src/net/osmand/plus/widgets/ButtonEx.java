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

		TextViewEx.parseAttributes(this, attrs, 0, 0);
	}

	public ButtonEx(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		TextViewEx.parseAttributes(this, attrs, defStyleAttr, 0);
	}

	@TargetApi(21)
	public ButtonEx(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		TextViewEx.parseAttributes(this, attrs, defStyleAttr, defStyleRes);
	}

	public void setAllCapsCompat(boolean allCaps) {
		TextViewEx.setAllCapsCompat(this, allCaps);
	}
}
