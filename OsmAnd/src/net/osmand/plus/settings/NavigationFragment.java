package net.osmand.plus.settings;

import android.support.v7.preference.Preference;
import android.support.v7.preference.SwitchPreferenceCompat;

import net.osmand.plus.R;

public class NavigationFragment extends BaseSettingsFragment {

	public static final String TAG = "NavigationFragment";

	@Override
	protected int getPreferencesResId() {
		return R.xml.navigation_settings_new;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar_big;
	}

	@Override
	protected int getToolbarTitle() {
		return R.string.routing_settings_2;
	}

	@Override
	protected void setupPreferences() {
		Preference routeParameters = findPreference("route_parameters");
		SwitchPreferenceCompat showRoutingAlarms = (SwitchPreferenceCompat) findPreference(settings.SHOW_ROUTING_ALARMS.getId());
		SwitchPreferenceCompat speakRoutingAlarms = (SwitchPreferenceCompat) findPreference(settings.SPEAK_ROUTING_ALARMS.getId());
		SwitchPreferenceCompat turnScreenOn = (SwitchPreferenceCompat) findPreference(settings.TURN_SCREEN_ON_ENABLED.getId());

		routeParameters.setIcon(getContentIcon(R.drawable.ic_action_route_distance));
		showRoutingAlarms.setIcon(getContentIcon(R.drawable.ic_action_alert));
		speakRoutingAlarms.setIcon(getContentIcon(R.drawable.ic_action_volume_up));
		turnScreenOn.setIcon(getContentIcon(R.drawable.ic_action_turn_screen_on));

		setupVehicleParametersPref();
	}

	private void setupVehicleParametersPref() {
		Preference vehicleParameters = findPreference("vehicle_parameters");
		int iconRes = getSelectedAppMode().getIconRes();
		vehicleParameters.setIcon(getContentIcon(iconRes));
	}
}