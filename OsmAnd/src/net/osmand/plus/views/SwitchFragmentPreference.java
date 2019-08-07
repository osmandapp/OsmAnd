package net.osmand.plus.views;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.util.AttributeSet;

public class SwitchFragmentPreference extends SwitchPreference {

	public SwitchFragmentPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public SwitchFragmentPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public SwitchFragmentPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SwitchFragmentPreference(Context context) {
		super(context);
	}

	@Override
	protected void onClick() {

	}
}
