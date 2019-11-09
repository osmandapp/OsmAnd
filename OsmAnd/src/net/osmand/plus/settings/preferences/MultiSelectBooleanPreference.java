package net.osmand.plus.settings.preferences;

import android.content.Context;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v7.preference.PreferenceDataStore;
import android.util.AttributeSet;

import net.osmand.plus.OsmandSettings.PreferencesDataStore;

import java.util.HashSet;
import java.util.Set;

public class MultiSelectBooleanPreference extends MultiSelectListPreference {

	private String description;

	public MultiSelectBooleanPreference(Context context) {
		super(context);
	}

	public MultiSelectBooleanPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MultiSelectBooleanPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public MultiSelectBooleanPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public CharSequence getDialogTitle() {
		CharSequence dialogTitle = super.getDialogTitle();
		return dialogTitle != null ? dialogTitle : getTitle();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		setValues(restoreValue ? getPersistedBooleanPrefsIds(getValues()) : (Set<String>) defaultValue);
	}

	public void setValues(Set<String> values) {
		if (!getValues().equals(values)) {
			getValues().clear();
			getValues().addAll(values);

			persistBooleanPrefs();
		}
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

	public String[] getPrefsIds() {
		CharSequence[] entryValues = getEntryValues();
		String[] prefsIds = new String[entryValues.length];
		for (int i = 0; i < entryValues.length; i++) {
			prefsIds[i] = entryValues[i].toString();
		}
		return prefsIds;
	}

	private void persistBooleanPrefs() {
		if (!shouldPersist()) {
			return;
		}
		PreferenceDataStore dataStore = getPreferenceDataStore();
		if (dataStore instanceof PreferencesDataStore) {
			PreferencesDataStore preferencesDataStore = (PreferencesDataStore) dataStore;

			for (String prefId : getPrefsIds()) {
				preferencesDataStore.putBoolean(prefId, getValues().contains(prefId));
			}
		}
	}

	public Set<String> getPersistedBooleanPrefsIds(Set<String> defaultReturnValue) {
		if (!shouldPersist()) {
			return defaultReturnValue;
		}

		Set<String> enabledPrefs = new HashSet<>();
		PreferenceDataStore dataStore = getPreferenceDataStore();

		if (dataStore instanceof PreferencesDataStore && getEntryValues() != null) {
			PreferencesDataStore preferencesDataStore = (PreferencesDataStore) dataStore;

			for (String prefId : getPrefsIds()) {
				boolean enabled = preferencesDataStore.getBoolean(prefId, false);
				if (enabled) {
					enabledPrefs.add(prefId);
				}
			}
		}

		return enabledPrefs;
	}
}