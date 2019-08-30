package net.osmand.plus.settings;

import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

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
	protected String getToolbarTitle() {
		return getString(R.string.routing_settings_2);
	}

	@Override
	protected void setupPreferences() {
		Preference routeParameters = findPreference("route_parameters");
		SwitchPreference showRoutingAlarms = (SwitchPreference) findPreference("show_routing_alarms");
		SwitchPreference speakRoutingAlarms = (SwitchPreference) findPreference("speak_routing_alarms");
		Preference vehicleParameters = findPreference("vehicle_parameters");
		Preference mapDuringNavigation = findPreference("map_during_navigation");
		SwitchPreference turnScreenOn = (SwitchPreference) findPreference("turn_screen_on");

		routeParameters.setIcon(getContentIcon(R.drawable.ic_action_route_distance));
		showRoutingAlarms.setIcon(getContentIcon(R.drawable.ic_action_alert));
		speakRoutingAlarms.setIcon(getContentIcon(R.drawable.ic_action_volume_up));

		int iconRes = getSelectedAppMode().getIconRes();
		vehicleParameters.setIcon(getContentIcon(iconRes));

		turnScreenOn.setIcon(getContentIcon(R.drawable.ic_action_turn_screen_on));
	}
}