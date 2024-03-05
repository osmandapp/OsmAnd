package net.osmand.plus.settings.fragments;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.ScreenTimeoutBottomSheet;
import net.osmand.plus.settings.bottomsheets.WakeTimeBottomSheet;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class TurnScreenOnFragment extends BaseSettingsFragment {

	public static final String TAG = TurnScreenOnFragment.class.getSimpleName();

	@Override
	protected void setupPreferences() {
		setupUseSystemScreenTimeout();
		setupTurnScreenOnTimePref();
		setupTurnScreenOnSensorPref();
		setupTurnScreenOnNavigationInstructionsPref();
		setupTurnScreenOnPowerButtonPref();
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		String prefId = preference.getKey();
		if (settings.TURN_SCREEN_ON_TIME_INT.getId().equals(prefId) && preference instanceof ListPreferenceEx) {
			TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
			if (summaryView != null && !preference.isEnabled()) {
				summaryView.setText(R.string.default_screen_timeout);
			}
		} else if ("turn_screen_on_info".equals(prefId) || "turn_screen_on_options_info".equals(prefId)) {
			TextView titleView = (TextView) holder.findViewById(android.R.id.title);
			if (titleView != null) {
				titleView.setTextColor(getDisabledTextColor());
			}
		}
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		FragmentManager fragmentManager = getFragmentManager();
		ApplicationMode appMode = getSelectedAppMode();
		String prefId = preference.getKey();
		if (settings.USE_SYSTEM_SCREEN_TIMEOUT.getId().equals(prefId)) {
			if (fragmentManager != null) {
				ScreenTimeoutBottomSheet.showInstance(fragmentManager, prefId, this, false, appMode, getApplyQueryType(), isProfileDependent());
			}
		} else if (settings.TURN_SCREEN_ON_TIME_INT.getId().equals(prefId)) {
			if (fragmentManager != null) {
				WakeTimeBottomSheet.showInstance(fragmentManager, prefId, this, false, appMode, getApplyQueryType(), isProfileDependent());
			}
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	private void setupUseSystemScreenTimeout() {
		SwitchPreferenceEx systemScreenTimeout = findPreference(settings.USE_SYSTEM_SCREEN_TIMEOUT.getId());
		systemScreenTimeout.setDescription(R.string.system_screen_timeout_descr);
	}

	private void setupTurnScreenOnTimePref() {
		Integer[] entryValues = {0, 5, 10, 15, 20, 30, 45, 60};
		String[] entries = new String[entryValues.length];

		entries[0] = getString(R.string.keep_screen_on);
		for (int i = 1; i < entryValues.length; i++) {
			entries[i] = entryValues[i] + " " + getString(R.string.int_seconds);
		}

		ListPreferenceEx turnScreenOnTime = findPreference(settings.TURN_SCREEN_ON_TIME_INT.getId());
		turnScreenOnTime.setEnabled(!settings.USE_SYSTEM_SCREEN_TIMEOUT.getModeValue(getSelectedAppMode()));
		turnScreenOnTime.setEntries(entries);
		turnScreenOnTime.setEntryValues(entryValues);
		turnScreenOnTime.setDescription(getString(R.string.turn_screen_on_wake_time_descr, getString(R.string.keep_screen_on)));
		turnScreenOnTime.setIcon(getPersistentPrefIcon(R.drawable.ic_action_time_span));
	}

	private void setupTurnScreenOnSensorPref() {
		SwitchPreferenceEx turnScreenOnSensor = findPreference(settings.TURN_SCREEN_ON_SENSOR.getId());
		turnScreenOnSensor.setIcon(getPersistentPrefIcon(R.drawable.ic_action_sensor_interaction));
		turnScreenOnSensor.setDescription(R.string.turn_screen_on_sensor_descr);
	}

	private void setupTurnScreenOnNavigationInstructionsPref() {
		SwitchPreferenceEx turnScreenOnNavigationInstructions = findPreference(settings.TURN_SCREEN_ON_NAVIGATION_INSTRUCTIONS.getId());
		turnScreenOnNavigationInstructions.setIcon(getPersistentPrefIcon(R.drawable.ic_action_notification_navigation));
		turnScreenOnNavigationInstructions.setDescription(R.string.turn_screen_on_navigation_instructions_descr);
	}

	private void setupTurnScreenOnPowerButtonPref() {
		ApplicationMode appMode = getSelectedAppMode();
		boolean enabled = settings.TURN_SCREEN_ON_TIME_INT.getModeValue(appMode) == 0 || settings.USE_SYSTEM_SCREEN_TIMEOUT.getModeValue(appMode);
		SwitchPreferenceEx turnScreenOnPowerButton = findPreference(settings.TURN_SCREEN_ON_POWER_BUTTON.getId());
		turnScreenOnPowerButton.setEnabled(enabled);
		turnScreenOnPowerButton.setDescription(R.string.turn_screen_on_power_button_descr);
		turnScreenOnPowerButton.setIcon(getPersistentPrefIcon(R.drawable.ic_action_power_button));
		turnScreenOnPowerButton.setChecked(enabled && settings.TURN_SCREEN_ON_POWER_BUTTON.getModeValue(appMode));
		turnScreenOnPowerButton.setSummaryOff(enabled ? R.string.shared_string_disabled : R.string.turn_screen_on_power_button_disabled);
	}

	@Override
	public void onPreferenceChanged(@NonNull String prefId) {
		if (settings.USE_SYSTEM_SCREEN_TIMEOUT.getId().equals(prefId)) {
			Preference turnScreenOnTime = findPreference(settings.TURN_SCREEN_ON_TIME_INT.getId());
			if (turnScreenOnTime != null) {
				turnScreenOnTime.setEnabled(!settings.USE_SYSTEM_SCREEN_TIMEOUT.getModeValue(getSelectedAppMode()));
			}
		}
	}
}