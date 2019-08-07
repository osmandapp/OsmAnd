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

import static net.osmand.plus.settings.profiles.SettingsProfileFragment.IS_USER_PROFILE;
import static net.osmand.plus.settings.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;

public class SettingsNavigationFragment extends SettingsBaseProfileDependentFragment {

	public static final String TAG = "SettingsNavigationFragment";

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

		Preference route_parameters = screen.findPreference("route_parameters");

		SwitchPreference show_routing_alarms = (SwitchPreference) screen.findPreference("show_routing_alarms");
		SwitchPreference speak_routing_alarms = (SwitchPreference) screen.findPreference("speak_routing_alarms");
		Preference vehicle_parameters = screen.findPreference("vehicle_parameters");
		Preference map_during_navigation = screen.findPreference("map_during_navigation");
		SwitchPreference turn_screen_on = (SwitchPreference) screen.findPreference("turn_screen_on");
		Preference reset_to_default = screen.findPreference("reset_to_default");

		route_parameters.setOnPreferenceChangeListener(this);
		show_routing_alarms.setOnPreferenceChangeListener(this);
		speak_routing_alarms.setOnPreferenceChangeListener(this);
		vehicle_parameters.setOnPreferenceChangeListener(this);
		map_during_navigation.setOnPreferenceChangeListener(this);
		turn_screen_on.setOnPreferenceChangeListener(this);

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

			SettingsNavigationFragment settingsNavigationFragment = new SettingsNavigationFragment();
			settingsNavigationFragment.setArguments(args);

			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, settingsNavigationFragment, SettingsNavigationFragment.TAG)
					.addToBackStack(SettingsNavigationFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}