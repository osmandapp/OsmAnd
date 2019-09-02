package net.osmand.plus.settings;

import android.content.Context;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;

import net.osmand.plus.R;

public class CoordinatesFormatFragment extends BaseSettingsFragment {

	public static final String TAG = "CoordinatesFormatFragment";

	@Override
	protected int getPreferencesResId() {
		return R.xml.coordinates_format;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	protected String getToolbarTitle() {
		return getString(R.string.coordinates_format);
	}

	@Override
	protected void setupPreferences() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference generalSettings = findPreference("coordinates_format_info");
		generalSettings.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		CheckBoxPreference degreesPref = (CheckBoxPreference) findPreference("format_degrees");
		CheckBoxPreference minutesPref = (CheckBoxPreference) findPreference("format_minutes");
		CheckBoxPreference secondsPref = (CheckBoxPreference) findPreference("format_seconds");
		CheckBoxPreference utmPref = (CheckBoxPreference) findPreference("format_utm");
		CheckBoxPreference olcPref = (CheckBoxPreference) findPreference("format_olc");

		degreesPref.setSummary(R.string.exit_at);
		minutesPref.setSummary(R.string.exit_at);
		secondsPref.setSummary(R.string.exit_at);
		utmPref.setSummary(R.string.exit_at);
		olcPref.setSummary(R.string.exit_at);

		//		settings.COORDINATES_FORMAT.get()

//		coordinatesFormat.setEntries(new String[] {
//				PointDescription.formatToHumanString(ctx, PointDescription.FORMAT_DEGREES),
//				PointDescription.formatToHumanString(ctx, PointDescription.FORMAT_MINUTES),
//				PointDescription.formatToHumanString(ctx, PointDescription.FORMAT_SECONDS),
//				PointDescription.formatToHumanString(ctx, PointDescription.UTM_FORMAT),
//				PointDescription.formatToHumanString(ctx, PointDescription.OLC_FORMAT)
//		});
//		coordinatesFormat.setEntryValues(new Integer[] {
//				PointDescription.FORMAT_DEGREES,
//				PointDescription.FORMAT_MINUTES,
//				PointDescription.FORMAT_SECONDS,
//				PointDescription.UTM_FORMAT,
//				PointDescription.OLC_FORMAT
//		});
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();

		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
			Preference pref = getPreferenceScreen().getPreference(i);
			if (pref instanceof CheckBoxPreference) {
				CheckBoxPreference checkBoxPreference = ((CheckBoxPreference) pref);
				if (!checkBoxPreference.getKey().equals(key)) {
					checkBoxPreference.setChecked(false);
				}
			}
		}

		return super.onPreferenceClick(preference);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();

		return super.onPreferenceChange(preference, newValue);
	}
}