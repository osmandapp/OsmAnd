package net.osmand.plus.settings;

import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.SwitchPreferenceCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class TurnScreenOnFragment extends BaseSettingsFragment {

	public static final String TAG = "TurnScreenOnFragment";

	@Override
	protected int getPreferencesResId() {
		return R.xml.turn_screen_on;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	@Override
	protected int getToolbarTitle() {
		return R.string.turn_screen_on;
	}

	@Override
	protected void setupPreferences() {
		Preference turnScreenOnInfo = findPreference("turn_screen_on_info");
		turnScreenOnInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupTurnScreenOnTimePref();
		setupTurnScreenOnSensorPref();
	}

	@Override
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);

		if (settings.TURN_SCREEN_ON_ENABLED.getId().equals(preference.getKey())) {
			boolean checked = ((SwitchPreferenceCompat) preference).isChecked();
			int color = checked ? getActiveProfileColor() : ContextCompat.getColor(app, R.color.preference_top_switch_off);

			AndroidUtils.setBackground(holder.itemView, new ColorDrawable(color));
		}
	}

	private void setupTurnScreenOnTimePref() {
		Integer[] entryValues = new Integer[] {0, 5, 10, 15, 20, 30, 45, 60};
		String[] entries = new String[entryValues.length];

		entries[0] = getString(R.string.shared_string_never);
		for (int i = 1; i < entryValues.length; i++) {
			entries[i] = entryValues[i] + " " + getString(R.string.int_seconds);
		}

		ListPreferenceEx turnScreenOnTime = (ListPreferenceEx) findPreference(settings.TURN_SCREEN_ON_TIME_INT.getId());
		turnScreenOnTime.setEntries(entries);
		turnScreenOnTime.setEntryValues(entryValues);
		turnScreenOnTime.setIcon(getContentIcon(R.drawable.ic_action_time_span));
	}

	private void setupTurnScreenOnSensorPref() {
		String title = getString(R.string.turn_screen_on_sensor);
		String description = getString(R.string.turn_screen_on_sensor_descr);

		SwitchPreferenceEx turnScreenOnSensor = (SwitchPreferenceEx) findPreference(settings.TURN_SCREEN_ON_SENSOR.getId());
		turnScreenOnSensor.setIcon(getContentIcon(R.drawable.ic_action_sensor_interaction));
		turnScreenOnSensor.setTitle(title);
		turnScreenOnSensor.setDescription(description);
	}
}