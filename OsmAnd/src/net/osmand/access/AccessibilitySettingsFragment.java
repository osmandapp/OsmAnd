package net.osmand.access;

import android.support.v4.app.FragmentManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.access.AccessibilityMode;
import net.osmand.plus.access.RelativeDirectionStyle;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet;
import net.osmand.plus.profiles.SelectCopyAppModeBottomSheet.CopyAppModePrefsListener;
import net.osmand.plus.settings.BaseSettingsFragment;
import net.osmand.plus.settings.OnPreferenceChanged;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet;
import net.osmand.plus.settings.bottomsheets.ResetProfilePrefsBottomSheet.ResetAppModePrefsListener;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class AccessibilitySettingsFragment extends BaseSettingsFragment implements OnPreferenceChanged, CopyAppModePrefsListener, ResetAppModePrefsListener {

	private static final String COPY_PLUGIN_SETTINGS = "copy_plugin_settings";
	private static final String RESET_TO_DEFAULT = "reset_to_default";

	@Override
	protected void setupPreferences() {
		setupAccessibilityModePref();
		setupSpeechRatePref();

		setupSmartAutoAnnouncePref();
		setupAutoAnnouncePeriodPref();

		setupDisableOffRouteRecalculationPref();
		setupDisableWrongDirectionRecalculationPref();

		setupDirectionStylePref();
		setupDirectionAudioFeedbackPref();
		setupDirectionHapticFeedbackPref();

		setupCopyProfileSettingsPref();
		setupResetToDefaultPref();

		updateAccessibilityOptions();
	}

	private void setupAccessibilityModePref() {
		AccessibilityMode[] accessibilityModes = AccessibilityMode.values();
		String[] entries = new String[accessibilityModes.length];
		Integer[] entryValues = new Integer[accessibilityModes.length];

		for (int i = 0; i < entries.length; i++) {
			entries[i] = accessibilityModes[i].toHumanString(app);
			entryValues[i] = accessibilityModes[i].ordinal();
		}

		ListPreferenceEx accessibilityMode = (ListPreferenceEx) findPreference(settings.ACCESSIBILITY_MODE.getId());
		accessibilityMode.setEntries(entries);
		accessibilityMode.setEntryValues(entryValues);
		accessibilityMode.setDescription(R.string.accessibility_mode_descr);
	}

	private void setupSpeechRatePref() {
		Float[] entryValues = new Float[] {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};
		String[] entries = new String[entryValues.length];

		for (int i = 0; i < entries.length; i++) {
			entries[i] = (int) (entryValues[i] * 100) + " %";
		}

		ListPreferenceEx speechRate = (ListPreferenceEx) findPreference(settings.SPEECH_RATE.getId());
		speechRate.setEntries(entries);
		speechRate.setEntryValues(entryValues);
		speechRate.setIcon(getContentIcon(R.drawable.ic_world_globe_dark));
		speechRate.setDescription(R.string.speech_rate_descr);
	}

	private void setupSmartAutoAnnouncePref() {
		SwitchPreferenceEx smartAutoAnnounce = (SwitchPreferenceEx) findPreference(settings.ACCESSIBILITY_SMART_AUTOANNOUNCE.getId());
		smartAutoAnnounce.setDescription(getString(R.string.access_smart_autoannounce_descr));
	}

	private void setupAutoAnnouncePeriodPref() {
		int[] seconds = new int[] {5, 10, 15, 20, 30, 45, 60, 90};
		int[] minutes = new int[] {2, 3, 5};

		Integer[] entryValues = new Integer[seconds.length + minutes.length];
		String[] entries = new String[entryValues.length];
		int k = 0;
		for (int second : seconds) {
			entryValues[k] = second * 1000;
			entries[k] = second + " " + getString(R.string.int_seconds);
			k++;
		}
		for (int minute : minutes) {
			entryValues[k] = (minute * 60) * 1000;
			entries[k] = minute + " " + getString(R.string.int_min);
			k++;
		}

		ListPreferenceEx autoAnnouncePeriod = (ListPreferenceEx) findPreference(settings.ACCESSIBILITY_AUTOANNOUNCE_PERIOD.getId());
		autoAnnouncePeriod.setEntries(entries);
		autoAnnouncePeriod.setEntryValues(entryValues);
		autoAnnouncePeriod.setDescription(R.string.access_autoannounce_period_descr);
	}

	private void setupDisableOffRouteRecalculationPref() {
		SwitchPreferenceEx disableOffRouteRecalculation = (SwitchPreferenceEx) findPreference(settings.DISABLE_OFFROUTE_RECALC.getId());
		disableOffRouteRecalculation.setDescription(getString(R.string.access_disable_offroute_recalc_descr));
	}

	private void setupDisableWrongDirectionRecalculationPref() {
		SwitchPreferenceEx disableWrongDirectionRecalculation = (SwitchPreferenceEx) findPreference(settings.DISABLE_WRONG_DIRECTION_RECALC.getId());
		disableWrongDirectionRecalculation.setDescription(getString(R.string.access_disable_wrong_direction_recalc_descr));
	}

	private void setupDirectionStylePref() {
		RelativeDirectionStyle[] relativeDirectionStyles = RelativeDirectionStyle.values();
		String[] entries = new String[relativeDirectionStyles.length];
		Integer[] entryValues = new Integer[relativeDirectionStyles.length];

		for (int i = 0; i < entries.length; i++) {
			entries[i] = relativeDirectionStyles[i].toHumanString(app);
			entryValues[i] = relativeDirectionStyles[i].ordinal();
		}

		ListPreferenceEx directionStyle = (ListPreferenceEx) findPreference(settings.DIRECTION_STYLE.getId());
		directionStyle.setEntries(entries);
		directionStyle.setEntryValues(entryValues);
		directionStyle.setDescription(R.string.settings_direction_style_descr);
	}

	private void setupDirectionAudioFeedbackPref() {
		SwitchPreferenceEx directionAudioFeedback = (SwitchPreferenceEx) findPreference(settings.DIRECTION_AUDIO_FEEDBACK.getId());
		directionAudioFeedback.setDescription(getString(R.string.access_direction_audio_feedback_descr));
	}

	private void setupDirectionHapticFeedbackPref() {
		SwitchPreferenceEx directionHapticFeedback = (SwitchPreferenceEx) findPreference(settings.DIRECTION_HAPTIC_FEEDBACK.getId());
		directionHapticFeedback.setDescription(getString(R.string.access_direction_haptic_feedback_descr));
	}

	private void setupCopyProfileSettingsPref() {
		Preference copyProfilePrefs = findPreference(COPY_PLUGIN_SETTINGS);
		copyProfilePrefs.setIcon(getActiveIcon(R.drawable.ic_action_copy));
	}

	private void setupResetToDefaultPref() {
		Preference resetToDefault = findPreference(RESET_TO_DEFAULT);
		resetToDefault.setIcon(getActiveIcon(R.drawable.ic_action_reset_to_default_dark));
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		if (settings.ACCESSIBILITY_MODE.getId().equals(prefId)) {
			updateAccessibilityOptions();
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();
		if (COPY_PLUGIN_SETTINGS.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				SelectCopyAppModeBottomSheet.showInstance(fragmentManager, this, false, getSelectedAppMode());
			}
		} else if (RESET_TO_DEFAULT.equals(prefId)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ResetProfilePrefsBottomSheet.showInstance(fragmentManager, prefId, this, false, getSelectedAppMode());
			}
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void copyAppModePrefs(ApplicationMode appMode) {
		OsmandMonitoringPlugin plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			app.getSettings().copyProfilePreferences(appMode, getSelectedAppMode(), plugin.getPreferences());
			updateAllSettings();
		}
	}

	@Override
	public void resetAppModePrefs(ApplicationMode appMode) {
		OsmandMonitoringPlugin plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			app.getSettings().resetProfilePreferences(appMode, plugin.getPreferences());
			updateAllSettings();
		}
	}

	private void updateAccessibilityOptions() {
		boolean accessibilityEnabled = app.accessibilityEnabledForMode(getSelectedAppMode());
		PreferenceScreen screen = getPreferenceScreen();
		OsmandMonitoringPlugin plugin = OsmandPlugin.getPlugin(OsmandMonitoringPlugin.class);
		if (screen != null && plugin != null) {
			for (int i = 0; i < screen.getPreferenceCount(); i++) {
				Preference preference = screen.getPreference(i);
				String prefId = preference.getKey();
				if (!settings.ACCESSIBILITY_MODE.getId().equals(prefId) && !settings.SPEECH_RATE.getId().equals(prefId)
						&& !RESET_TO_DEFAULT.equals(prefId) && !COPY_PLUGIN_SETTINGS.equals(prefId))
					preference.setEnabled(accessibilityEnabled);
			}
		}
	}
}