package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;

public class ScreenAlertsFragment extends BaseProfileSettingsFragment {

	public static final String TAG = "ScreenAlertsFragment";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		return view;
	}

	@Override
	protected int getPreferenceResId() {
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
	protected void createUI() {
		PreferenceScreen screen = getPreferenceScreen();

		SwitchPreference SHOW_ROUTING_ALARMS = (SwitchPreference) findAndRegisterPreference(settings.SHOW_ROUTING_ALARMS.getId());
		Preference SHOW_TRAFFIC_WARNINGS_DESCR = findAndRegisterPreference("show_routing_alarms_descr");
		SwitchPreference SHOW_TRAFFIC_WARNINGS = (SwitchPreference) findAndRegisterPreference(settings.SHOW_TRAFFIC_WARNINGS.getId());
		SwitchPreference SHOW_PEDESTRIAN = (SwitchPreference) findAndRegisterPreference(settings.SHOW_PEDESTRIAN.getId());
		SwitchPreference SHOW_CAMERAS = (SwitchPreference) findAndRegisterPreference(settings.SHOW_CAMERAS.getId());
		SwitchPreference SHOW_LANES = (SwitchPreference) findAndRegisterPreference(settings.SHOW_LANES.getId());
		SwitchPreference SHOW_TUNNELS = (SwitchPreference) findAndRegisterPreference(settings.SHOW_TUNNELS.getId());

		SHOW_TRAFFIC_WARNINGS_DESCR.setIcon(getContentIcon(R.drawable.ic_action_info_dark));
		SHOW_TRAFFIC_WARNINGS.setIcon(getIcon(R.drawable.list_warnings_traffic_calming));
		SHOW_PEDESTRIAN.setIcon(getIcon(R.drawable.list_warnings_pedestrian));
		SHOW_CAMERAS.setIcon(getIcon(R.drawable.list_warnings_speed_camera));
		SHOW_LANES.setIcon(getIcon(R.drawable.ic_action_lanes));
		SHOW_TUNNELS.setIcon(getIcon(R.drawable.list_warnings_tunnel));
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		try {
			ScreenAlertsFragment settingsNavigationFragment = new ScreenAlertsFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, settingsNavigationFragment, ScreenAlertsFragment.TAG)
					.addToBackStack(ScreenAlertsFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}