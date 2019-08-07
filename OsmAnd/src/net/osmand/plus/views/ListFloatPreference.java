package net.osmand.plus.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

public class ListFloatPreference extends ListPreference {

	public ListFloatPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public ListFloatPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public ListFloatPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ListFloatPreference(Context context) {
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

	public void setEntryValues(float[] entryValues) {
		String[] strings = new String[entryValues.length];
		for (int i = 0; i < entryValues.length; ++i) {
			strings[i] = Float.toString(entryValues[i]);
		}
		super.setEntryValues(strings);
	}

	public void setEntryValues(int[] entryValues) {
		String[] strings = new String[entryValues.length];
		for (int i = 0; i < entryValues.length; ++i) {
			strings[i] = Float.toString(entryValues[i]);
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
			return persistFloat(Float.valueOf(value));
		}
	}

	@Override
	protected String getPersistedString(String defaultReturnValue) {
		if (getSharedPreferences().contains(getKey())) {
			float floatValue = getPersistedFloat(0f);
			return String.valueOf(floatValue);
		} else {
			return defaultReturnValue;
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return String.valueOf(a.getFloat(index, 0f));
	}
}