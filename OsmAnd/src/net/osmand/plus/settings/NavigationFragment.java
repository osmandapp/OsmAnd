package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;

import static net.osmand.plus.settings.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;

public class NavigationFragment extends BaseProfileSettingsFragment {

	public static final String TAG = "NavigationFragment";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		return view;
	}

	@Override
	protected int getPreferenceResId() {
		return R.xml.navigation_settings;
	}


	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar_big;
	}

	protected String getToolbarTitle() {
		return getString(R.string.routing_settings_2);
	}

	@Override
	protected void createUI() {
		PreferenceScreen screen = getPreferenceScreen();

		Preference route_parameters = findAndRegisterPreference("route_parameters");

		SwitchPreference show_routing_alarms = (SwitchPreference) findAndRegisterPreference("show_routing_alarms");
		SwitchPreference speak_routing_alarms = (SwitchPreference) findAndRegisterPreference("speak_routing_alarms");
		Preference vehicle_parameters = findAndRegisterPreference("vehicle_parameters");
		Preference map_during_navigation = findAndRegisterPreference("map_during_navigation");
		SwitchPreference turn_screen_on = (SwitchPreference) findAndRegisterPreference("turn_screen_on");
		Preference reset_to_default = findAndRegisterPreference("reset_to_default");

		route_parameters.setIcon(getContentIcon(R.drawable.ic_action_track_16));
		show_routing_alarms.setIcon(getContentIcon(R.drawable.ic_action_alert));
		speak_routing_alarms.setIcon(getContentIcon(R.drawable.ic_action_volume_up));
		vehicle_parameters.setIcon(getContentIcon(R.drawable.ic_action_car_dark));
		map_during_navigation.setIcon(getContentIcon(R.drawable.ic_action_mapillary));
		turn_screen_on.setIcon(getContentIcon(R.drawable.ic_action_appearance));
		reset_to_default.setIcon(getContentIcon(R.drawable.ic_action_undo_dark));
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		Toast.makeText(getActivity(), "Change " + preference.getKey(), Toast.LENGTH_LONG).show();
		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		Toast.makeText(getActivity(), "Click " + preference.getKey(), Toast.LENGTH_LONG).show();
		return super.onPreferenceClick(preference);
	}

	public static boolean showInstance(FragmentManager fragmentManager, ApplicationMode mode) {
		try {
			Bundle args = new Bundle();
			args.putString(PROFILE_STRING_KEY, mode.getStringKey());

			NavigationFragment settingsNavigationFragment = new NavigationFragment();
			settingsNavigationFragment.setArguments(args);

			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, settingsNavigationFragment, NavigationFragment.TAG)
					.addToBackStack(NavigationFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}