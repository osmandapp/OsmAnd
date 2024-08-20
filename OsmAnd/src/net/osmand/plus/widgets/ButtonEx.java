package net.osmand.plus.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

@Deprecated
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
}
