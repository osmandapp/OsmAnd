package net.osmand.access;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.access.AccessibilityMode;
import net.osmand.plus.access.RelativeDirectionStyle;
import net.osmand.plus.activities.SettingsBaseActivity;

public class SettingsAccessibilityActivity extends SettingsBaseActivity {

	private ListPreference accessibilityModePreference;
	private ListPreference directionStylePreference;
	private ListPreference autoannouncePeriodPreference;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getToolbar().setTitle(R.string.shared_string_accessibility);
		PreferenceScreen grp = getPreferenceScreen();

		String[] entries = new String[AccessibilityMode.values().length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = AccessibilityMode.values()[i].toHumanString(getMyApplication());
		}
		accessibilityModePreference = createListPreference(settings.ACCESSIBILITY_MODE, entries, AccessibilityMode.values(),
				R.string.accessibility_mode, R.string.accessibility_mode_descr);
		accessibilityModePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			private final OnPreferenceChangeListener committer = accessibilityModePreference.getOnPreferenceChangeListener();
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (committer != null)
					committer.onPreferenceChange(preference, newValue);
				updateAllSettings();
				return true;
			}
		});
		addSpeechRateSetting(grp);
		
		grp.addPreference(accessibilityModePreference);
		PreferenceCategory cat = new PreferenceCategory(this);
		cat.setKey("accessibility_options");
		cat.setTitle(R.string.accessibility_options);
		cat.setEnabled(getMyApplication().accessibilityEnabled());
		grp.addPreference(cat);

		entries = new String[RelativeDirectionStyle.values().length];
		for (int i = 0; i < entries.length; i++) {
			entries[i] = RelativeDirectionStyle.values()[i].toHumanString(getMyApplication());
		}
		directionStylePreference = createListPreference(settings.DIRECTION_STYLE, entries, RelativeDirectionStyle.values(),
				R.string.settings_direction_style, R.string.settings_direction_style_descr);
		directionStylePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			private final OnPreferenceChangeListener committer = directionStylePreference.getOnPreferenceChangeListener();
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (committer != null)
					committer.onPreferenceChange(preference, newValue);
				updateAllSettings();
				return true;
			}
		});
		cat.addPreference(directionStylePreference);

		cat.addPreference(createCheckBoxPreference(settings.ACCESSIBILITY_SMART_AUTOANNOUNCE, R.string.access_smart_autoannounce,
				R.string.access_smart_autoannounce_descr));

		final int[] seconds = new int[] {5, 10, 15, 20, 30, 45, 60, 90};
		final int[] minutes = new int[] {2, 3, 5};
		autoannouncePeriodPreference = createTimeListPreference(settings.ACCESSIBILITY_AUTOANNOUNCE_PERIOD, seconds, minutes, 1000,
				R.string.access_autoannounce_period, R.string.access_autoannounce_period_descr);
		autoannouncePeriodPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			private final OnPreferenceChangeListener committer = autoannouncePeriodPreference.getOnPreferenceChangeListener();
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (committer != null)
					committer.onPreferenceChange(preference, newValue);
				updateAllSettings();
				return true;
			}
		});
		cat.addPreference(autoannouncePeriodPreference);
		cat.addPreference(createCheckBoxPreference(settings.DIRECTION_AUDIO_FEEDBACK, R.string.access_direction_audio_feedback,
				R.string.access_direction_audio_feedback_descr));
		cat.addPreference(createCheckBoxPreference(settings.DIRECTION_HAPTIC_FEEDBACK, R.string.access_direction_haptic_feedback,
				R.string.access_direction_haptic_feedback_descr));

	}


	protected void addSpeechRateSetting(PreferenceGroup grp) {
		Float[] sprValues = new Float[] {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f} ;
		String[] sprNames = new String[sprValues.length];
		for(int i = 0; i < sprNames.length; i++) {
			sprNames[i] = (int)(sprValues[i] * 100) + " %";
		}
		grp.addPreference(createListPreference(settings.SPEECH_RATE, sprNames, sprValues, R.string.speech_rate, R.string.speech_rate_descr));
	}



	public void updateAllSettings() {
		super.updateAllSettings();
		PreferenceCategory accessibilityOptions = ((PreferenceCategory)(getPreferenceScreen().findPreference("accessibility_options")));
		if (accessibilityOptions != null)
			accessibilityOptions.setEnabled(getMyApplication().accessibilityEnabled());
		if(accessibilityModePreference != null) {
			accessibilityModePreference.setSummary(getString(R.string.accessibility_mode_descr) + "  [" + settings.ACCESSIBILITY_MODE.get().toHumanString(getMyApplication()) + "]");
		}
		if(directionStylePreference != null) {
			directionStylePreference.setSummary(getString(R.string.settings_direction_style_descr) + "  [" + settings.DIRECTION_STYLE.get().toHumanString(getMyApplication()) + "]");
		}
		if(autoannouncePeriodPreference != null) {
			autoannouncePeriodPreference.setSummary(getString(R.string.access_autoannounce_period_descr) + "  [" + autoannouncePeriodPreference.getEntry() + "]");
		}
	}

}
