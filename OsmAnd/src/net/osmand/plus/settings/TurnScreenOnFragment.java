package net.osmand.plus.settings;

import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class TurnScreenOnFragment extends BaseSettingsFragment {

	public static final String TAG = TurnScreenOnFragment.class.getSimpleName();

	@Override
	protected void setupPreferences() {
		Preference turnScreenOnInfo = findPreference("turn_screen_on_info");
		turnScreenOnInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		setupTurnScreenOnTimePref();
		setupTurnScreenOnSensorPref();
		enableDisablePreferences(settings.TURN_SCREEN_ON_ENABLED.getModeValue(getSelectedAppMode()));
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);

		view.findViewById(R.id.toolbar_switch_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ApplicationMode selectedMode = getSelectedAppMode();
				boolean checked = !settings.TURN_SCREEN_ON_ENABLED.getModeValue(selectedMode);
				settings.TURN_SCREEN_ON_ENABLED.setModeValue(selectedMode, checked);
				updateToolbarSwitch();
				enableDisablePreferences(checked);
			}
		});
	}

	@Override
	protected void updateToolbar() {
		super.updateToolbar();
		updateToolbarSwitch();
	}

	private void updateToolbarSwitch() {
		View view = getView();
		if (view == null) {
			return;
		}
		boolean checked = settings.TURN_SCREEN_ON_ENABLED.getModeValue(getSelectedAppMode());

		int color = checked ? getActiveProfileColor() : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		View switchContainer = view.findViewById(R.id.toolbar_switch_container);
		AndroidUtils.setBackground(switchContainer, new ColorDrawable(color));

		SwitchCompat switchView = (SwitchCompat) switchContainer.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);

		TextView title = switchContainer.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_on : R.string.shared_string_off);
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