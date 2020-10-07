package net.osmand.plus.settings.preferences;

import android.content.Context;

import androidx.preference.DialogPreference;

import net.osmand.plus.R;
import net.osmand.plus.settings.bottomsheets.VehicleSizeAssets;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class SizePreference extends DialogPreference {

	private String[] entries;
	private String[] entryValues;
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

	public String[] getEntryValues() {
		return entryValues;
	}

	public void setEntryValues(String[] entryValues) {
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
				try {
					return Float.parseFloat(entryValues[i]);
				} catch (NumberFormatException e) {
					return 0.0f;
				}
			}
		}
		return 0.0f;
	}

	@Override
	public CharSequence getSummary() {
		String summary = entries[0];
		String persistedString = getValue();
		if (StringUtils.isBlank(persistedString)) {
			return summary;
		}
		if (!isPersistedStringEqualsZero(persistedString)) {
			try {
				final DecimalFormat df = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.US));
				persistedString = df.format(Double.parseDouble(persistedString) + 0.01d);
				summary = String.format(getContext().getString(R.string.ltr_or_rtl_combine_via_space),
						persistedString, getContext().getString(assets.getMetricShortRes()));
			} catch (NumberFormatException e) {
				summary = entries[0];
			}
		}
		return summary;
	}

	private boolean isPersistedStringEqualsZero(String persistedString) {
		return BigDecimal.ZERO.compareTo(new BigDecimal(persistedString)) == 0;
	}

	public String getValue () {
		return getPersistedString(defaultValue);
	}
}
