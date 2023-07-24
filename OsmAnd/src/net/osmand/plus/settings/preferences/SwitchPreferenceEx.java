package net.osmand.plus.settings.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.SwitchPreferenceCompat;

public class SwitchPreferenceEx extends SwitchPreferenceCompat {

	private String description;
	private boolean overrideOnClick = true;

	public SwitchPreferenceEx(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public SwitchPreferenceEx(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public SwitchPreferenceEx(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SwitchPreferenceEx(Context context) {
		super(context);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDescription(int titleResId) {
		setDescription(getContext().getString(titleResId));
	}

	public void setOverrideOnClick(boolean overrideOnClick) {
		this.overrideOnClick = overrideOnClick;
	}

	@Override
	protected void onClick() {
		if (getFragment() == null && getIntent() == null && overrideOnClick) {
			getPreferenceManager().showDialog(this);
		}
	}
}