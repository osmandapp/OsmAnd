package net.osmand.plus.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ToggleButton;

@Deprecated
public class SwitchEx extends ToggleButton {
	public SwitchEx(Context context) {
		super(context);
	}

	public SwitchEx(Context context, AttributeSet attrs) {
		super(context, attrs);

		TextViewEx.parseAttributes(this, attrs, 0, 0);
	}

	public SwitchEx(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		TextViewEx.parseAttributes(this, attrs, defStyleAttr, 0);
	}

	@TargetApi(21)
	public SwitchEx(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		TextViewEx.parseAttributes(this, attrs, defStyleAttr, defStyleRes);
	}

	public void setAllCapsCompat(boolean allCaps) {
		TextViewEx.setAllCapsCompat(this, allCaps);
	}
}
