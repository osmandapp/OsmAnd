package net.osmand.plus.settings.preferences;

import android.content.Context;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

public class EditTextPreferenceEx extends EditTextPreference {

	private String description;

	public EditTextPreferenceEx(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public EditTextPreferenceEx(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public EditTextPreferenceEx(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditTextPreferenceEx(Context context) {
		super(context);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDescription(int descriptionResId) {
		setDescription(getContext().getString(descriptionResId));
	}
}
