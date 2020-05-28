package net.osmand.plus.settings.fragments;

import android.widget.ImageView;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.settings.bottomsheets.ScreenTimeoutBottomSheet;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class TurnScreenOnFragment extends BaseSettingsFragment implements OnPreferenceChanged {

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
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		if (settings.TURN_SCREEN_ON_TIME_INT.getId().equals(preference.getKey()) && preference instanceof ListPreferenceEx) {
			Object currentValue = ((ListPreferenceEx) preference).getValue();
			ImageView imageView = (ImageView) holder.findViewById(android.R.id.icon);
			if (imageView != null && currentValue instanceof Integer) {
				boolean enabled = preference.isEnabled() && (Integer) currentValue > 0;
				imageView.setEnabled(enabled);
			}
		}
	}

	@Override
	public void onDisplayPreferenceDialog(Preference preference) {
		if (settings.USE_SYSTEM_SCREEN_TIMEOUT.getId().equals(preference.getKey())) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ScreenTimeoutBottomSheet.showInstance(fragmentManager, preference.getKey(), this, false, getSelectedAppMode(), getApplyQueryType(), isProfileDependent());
			}
		} else {
			super.onDisplayPreferenceDialog(preference);
		}
	}

	private void setupUseSystemScreenTimeout() {
		SwitchPreferenceEx systemScreenTimeout = (SwitchPreferenceEx) findPreference(settings.USE_SYSTEM_SCREEN_TIMEOUT.getId());
		systemScreenTimeout.setDescription(R.string.system_screen_timeout_descr);
	}

	private void setupTurnScreenOnTimePref() {
		Integer[] entryValues = new Integer[] {-1, 0, 5, 10, 15, 20, 30, 45, 60};
		String[] entries = new String[entryValues.length];

		entries[0] = getString(R.string.shared_string_always);
		entries[1] = getString(R.string.shared_string_never);
		for (int i = 2; i < entryValues.length; i++) {
			entries[i] = entryValues[i] + " " + getString(R.string.int_seconds);
		}

		ListPreferenceEx turnScreenOnTime = (ListPreferenceEx) findPreference(settings.TURN_SCREEN_ON_TIME_INT.getId());
		turnScreenOnTime.setEnabled(!settings.USE_SYSTEM_SCREEN_TIMEOUT.getModeValue(getSelectedAppMode()));
		turnScreenOnTime.setEntries(entries);
		turnScreenOnTime.setEntryValues(entryValues);
		turnScreenOnTime.setIcon(getPersistentPrefIcon(R.drawable.ic_action_time_span));
	}

	private void setupTurnScreenOnSensorPref() {
		SwitchPreferenceEx turnScreenOnSensor = (SwitchPreferenceEx) findPreference(settings.TURN_SCREEN_ON_SENSOR.getId());
		turnScreenOnSensor.setIcon(getPersistentPrefIcon(R.drawable.ic_action_sensor_interaction));
		turnScreenOnSensor.setDescription(R.string.turn_screen_on_sensor_descr);
	}

	private void setupTurnScreenOnNavigationInstructionsPref() {
		SwitchPreferenceEx turnScreenOnNavigationInstructions = (SwitchPreferenceEx) findPreference(settings.TURN_SCREEN_ON_NAVIGATION_INSTRUCTIONS.getId());
		turnScreenOnNavigationInstructions.setIcon(getPersistentPrefIcon(R.drawable.ic_action_notification_navigation));
		turnScreenOnNavigationInstructions.setDescription(R.string.turn_screen_on_navigation_instructions_descr);
	}

	private void setupTurnScreenOnPowerButtonPref() {
		SwitchPreferenceEx turnScreenOnPowerButton = (SwitchPreferenceEx) findPreference(settings.TURN_SCREEN_ON_POWER_BUTTON.getId());
		turnScreenOnPowerButton.setIcon(getPersistentPrefIcon(R.drawable.ic_action_power_button));
		turnScreenOnPowerButton.setDescription(R.string.turn_screen_on_power_button_descr);
	}

	@Override
	public void onPreferenceChanged(String prefId) {
		if (settings.USE_SYSTEM_SCREEN_TIMEOUT.getId().equals(prefId)) {
			Preference turnScreenOnTime = findPreference(settings.TURN_SCREEN_ON_TIME_INT.getId());
			if (turnScreenOnTime != null) {
				turnScreenOnTime.setEnabled(!settings.USE_SYSTEM_SCREEN_TIMEOUT.getModeValue(getSelectedAppMode()));
			}
		}
	}
}