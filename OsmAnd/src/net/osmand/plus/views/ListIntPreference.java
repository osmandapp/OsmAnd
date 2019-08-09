package net.osmand.plus.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

public class ListIntPreference extends ListPreference {

	public ListIntPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public ListIntPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public ListIntPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ListIntPreference(Context context) {
		super(context);
	}

	@Override
	public void setEntries(CharSequence[] entries) {
		super.setEntries(entries);
	}

	@Override
	public void setEntries(int entriesResId) {
		super.setEntries(entriesResId);
	}

	@Override
	public void setEntryValues(CharSequence[] entryValues) {
		super.setEntryValues(entryValues);
	}

	public void setEntryValues(int[] entryValues) {
		String[] strings = new String[entryValues.length];
		for (int i = 0; i < entryValues.length; ++i) {
			strings[i] = Integer.toString(entryValues[i]);
		}
		super.setEntryValues(strings);
	}

	@Override
	public void setEntryValues(int entryValuesResId) {
		setEntryValues(getContext().getResources().getIntArray(entryValuesResId));
	}

	@Override
	protected boolean persistString(String value) {
		if (value == null) {
			return false;
		} else {
			return persistInt(Integer.valueOf(value));
		}
	}

	@Override
	protected String getPersistedString(String defaultReturnValue) {
		if (getSharedPreferences().contains(getKey())) {
			int intValue = getPersistedInt(0);
			return String.valueOf(intValue);
		} else {
			return defaultReturnValue;
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return String.valueOf(a.getInt(index, 0));
	}

	@Override
	public boolean callChangeListener(Object newValue) {
		return super.callChangeListener(Integer.valueOf((String) newValue));
	}
}
