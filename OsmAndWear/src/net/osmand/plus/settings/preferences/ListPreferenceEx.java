package net.osmand.plus.settings.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDataStore;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndPreferencesDataStore;

public class ListPreferenceEx extends DialogPreference {

	private String[] entries;
	private Object[] entryValues;
	private Object selectedValue;
	private String description;

	public ListPreferenceEx(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public ListPreferenceEx(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public ListPreferenceEx(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ListPreferenceEx(Context context) {
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

	public void setEntries(String[] entries) {
		this.entries = entries;
	}

	public String[] getEntries() {
		return entries;
	}

	public void setEntryValues(Object[] entryValues) {
		this.entryValues = entryValues;
	}

	public Object[] getEntryValues() {
		return entryValues;
	}

	public void setValueIndex(int index) {
		if (entryValues != null && index >= 0 && index < entryValues.length) {
			setValue(entryValues[index]);
		}
	}

	public Object getValue() {
		return selectedValue;
	}

	public String getEntry() {
		int index = getValueIndex();
		return index >= 0 && entries != null ? entries[index] : null;
	}

	public int findIndexOfValue(Object value) {
		if (value != null && entryValues != null) {
			for (int i = 0; i < entryValues.length; i++) {
				if (entryValues[i].equals(value)) {
					return i;
				}
			}
		}
		return -1;
	}

	public int getValueIndex() {
		return findIndexOfValue(selectedValue);
	}

	@Override
	public CharSequence getDialogTitle() {
		CharSequence dialogTitle = super.getDialogTitle();
		return dialogTitle != null ? dialogTitle : getTitle();
	}

	@Override
	public CharSequence getSummary() {
		String entry = getEntry();
		return entry != null ? entry : super.getSummary();
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return getPersistedValue(null);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		selectedValue = restoreValue ? getPersistedValue(selectedValue) : (String) defaultValue;
		notifyChanged();
	}

	public void setValue(Object value) {
		// Always persist/notify the first time.
		boolean changed = selectedValue == null || !selectedValue.equals(value);
		if (changed) {
			selectedValue = value;
			persistValue(value);
			notifyChanged();
		}
	}

	private Object getPersistedValue(Object defaultValue) {
		PreferenceDataStore dataStore = getPreferenceDataStore();
		if (dataStore instanceof OsmAndPreferencesDataStore) {
			Object value = ((OsmAndPreferencesDataStore) dataStore).getValue(getKey(), defaultValue);
			if (value instanceof Enum) {
				return ((Enum) value).ordinal();
			} else if (value instanceof ApplicationMode) {
				return ((ApplicationMode) value).getStringKey();
			} else {
				return value;
			}
		}
		return null;
	}

	private void persistValue(Object value) {
		if (!shouldPersist()) {
			return;
		}
		PreferenceDataStore dataStore = getPreferenceDataStore();
		if (dataStore instanceof OsmAndPreferencesDataStore) {
			((OsmAndPreferencesDataStore) dataStore).putValue(getKey(), value);
		}
	}
}