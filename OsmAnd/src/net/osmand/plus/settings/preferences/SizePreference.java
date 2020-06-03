package net.osmand.plus.settings.preferences;

import android.content.Context;

import androidx.preference.DialogPreference;

import net.osmand.plus.R;
import net.osmand.plus.settings.bottomsheets.VehicleSizeAssets;

public class SizePreference extends DialogPreference {

	private String[] entries;
	private Object[] entryValues;
	private String description;
	private VehicleSizeAssets assets;

	public VehicleSizeAssets getAssets() {
		return assets;
	}

	public void setAssets(VehicleSizeAssets assets) {
		this.assets = assets;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	private String defaultValue;

	public SizePreference(Context context) {
		super(context);
	}

	public String[] getEntries() {
		return entries;
	}

	public void setEntries(String[] entries) {
		this.entries = entries;
	}

	public Object[] getEntryValues() {
		return entryValues;
	}

	public void setEntryValues(Object[] entryValues) {
		this.entryValues = entryValues;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEntryFromValue(String value) {
		for (int i = 0; i < entryValues.length; i++) {
			if (entryValues[i].equals(value)) {
				return entries[i];
			}
		}
		return "";
	}

	public float getValueFromEntries(String item) {
		String[] entries = getEntries();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].equals(item)) {
				return Float.parseFloat(getEntryValues()[i].toString());
			}
		}
		return 0.0f;
	}

	@Override
	public CharSequence getSummary() {
		String persistedString = getValue();
		if (!persistedString.equals(defaultValue)) {
			persistedString = String.valueOf(Float.parseFloat(persistedString) + 0.01f);
			return String.format(getContext().getString(R.string.ltr_or_rtl_combine_via_space), persistedString, getContext().getString(assets.getMetricShortRes()));
		}
		return "-";
	}

	public String getValue() {
		return getPersistedString(defaultValue);
	}
}
