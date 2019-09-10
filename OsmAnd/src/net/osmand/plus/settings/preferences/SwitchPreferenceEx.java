package net.osmand.plus.settings.preferences;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.util.AttributeSet;

public class SwitchPreferenceEx extends SwitchPreference {

	private String description;

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

	@Override
	protected void onClick() {
		if (getFragment() == null && getIntent() == null) {
			getPreferenceManager().showDialog(this);
		}
	}
}