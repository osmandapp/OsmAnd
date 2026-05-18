package net.osmand.plus.widgets;

import android.content.Context;
import android.util.AttributeSet;

@Deprecated
public class SwitchEx extends androidx.appcompat.widget.AppCompatToggleButton {

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
}
