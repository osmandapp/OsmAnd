package net.osmand.plus.settings;

import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import net.osmand.plus.R;

public class ScreenAlertsFragment extends BaseSettingsFragment {

	public static final String TAG = "ScreenAlertsFragment";

	@Override
	protected int getPreferencesResId() {
		return R.xml.screen_alerts;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar;
	}

	protected String getToolbarTitle() {
		return getString(R.string.screen_alerts);
	}

	@Override
	protected void setupPreferences() {
		setupShowRoutingAlarmsPref();

		Preference showRoutingAlarmsInfo = findPreference("show_routing_alarms_info");
		SwitchPreference showTrafficWarnings = (SwitchPreference) findPreference(settings.SHOW_TRAFFIC_WARNINGS.getId());
		SwitchPreference showPedestrian = (SwitchPreference) findPreference(settings.SHOW_PEDESTRIAN.getId());
		SwitchPreference showCameras = (SwitchPreference) findPreference(settings.SHOW_CAMERAS.getId());
		SwitchPreference showTunnels = (SwitchPreference) findPreference(settings.SHOW_TUNNELS.getId());

		showRoutingAlarmsInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		showTrafficWarnings.setIcon(getIcon(R.drawable.list_warnings_traffic_calming));
		showPedestrian.setIcon(getIcon(R.drawable.list_warnings_pedestrian));
		showCameras.setIcon(getIcon(R.drawable.list_warnings_speed_camera));
		showTunnels.setIcon(getIcon(R.drawable.list_warnings_tunnel));
	}

	private void setupShowRoutingAlarmsPref() {
		SwitchPreference showRoutingAlarms = (SwitchPreference) findPreference(settings.SHOW_ROUTING_ALARMS.getId());

	}
}