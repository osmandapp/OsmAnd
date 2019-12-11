package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.SwitchPreferenceCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.profiles.EditProfileFragment.RoutingProfilesResources;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.router.GeneralRouter;

import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.DIALOG_TYPE;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.SELECTED_KEY;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.TYPE_NAV_PROFILE;

public class NavigationFragment extends BaseSettingsFragment {

	public static final String TAG = NavigationFragment.class.getSimpleName();

	@Override
	protected void setupPreferences() {
		Preference navigationType = findPreference("navigation_type");
		Preference routeParameters = findPreference("route_parameters");
		SwitchPreferenceCompat showRoutingAlarms = (SwitchPreferenceCompat) findPreference(settings.SHOW_ROUTING_ALARMS.getId());
		SwitchPreferenceCompat speakRoutingAlarms = (SwitchPreferenceCompat) findPreference(settings.VOICE_MUTE.getId());
		SwitchPreferenceCompat turnScreenOn = (SwitchPreferenceCompat) findPreference(settings.TURN_SCREEN_ON_ENABLED.getId());
		SwitchPreferenceEx animateMyLocation = (SwitchPreferenceEx) findPreference(settings.ANIMATE_MY_LOCATION.getId());

		GeneralRouter gr = app.getRoutingConfig().getRouter(getSelectedAppMode().getRoutingProfile());
		RoutingProfilesResources routingProfilesResources = RoutingProfilesResources.valueOf(gr.getProfileName().toUpperCase());
		navigationType.setSummary(routingProfilesResources.getStringRes());
		navigationType.setIcon(getContentIcon(routingProfilesResources.getIconRes()));
		routeParameters.setIcon(getContentIcon(R.drawable.ic_action_route_distance));
		showRoutingAlarms.setIcon(getContentIcon(R.drawable.ic_action_alert));
		speakRoutingAlarms.setIcon(getContentIcon(R.drawable.ic_action_volume_up));
		turnScreenOn.setIcon(getContentIcon(R.drawable.ic_action_turn_screen_on));

		setupVehicleParametersPref();
		speakRoutingAlarms.setChecked(!settings.VOICE_MUTE.getModeValue(getSelectedAppMode()));
		animateMyLocation.setDescription(getString(R.string.animate_my_location_desc));
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();
		if (settings.VOICE_MUTE.getId().equals(key) && newValue instanceof Boolean) {
			settings.VOICE_MUTE.setModeValue(getSelectedAppMode(), !(Boolean) newValue);
			updateMenu();
			return true;
		}
		return super.onPreferenceChange(preference, newValue);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals("navigation_type")) {
			if (getSelectedAppMode().isCustomProfile()) {
				final SelectProfileBottomSheetDialogFragment dialog = new SelectProfileBottomSheetDialogFragment();
				Bundle bundle = new Bundle();
				if (getSelectedAppMode() != null) {
					bundle.putString(SELECTED_KEY, getSelectedAppMode().getRoutingProfile());
				}
				bundle.putString(DIALOG_TYPE, TYPE_NAV_PROFILE);
				dialog.setArguments(bundle);
				if (getActivity() != null) {
					getActivity().getSupportFragmentManager().beginTransaction()
							.add(dialog, "select_nav_type").commitAllowingStateLoss();
				}
			}
		}
		return true;
	}

	private void setupVehicleParametersPref() {
		Preference vehicleParameters = findPreference("vehicle_parameters");
		int iconRes = getSelectedAppMode().getIconRes();
		vehicleParameters.setIcon(getContentIcon(iconRes));
	}

	private void updateMenu() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapRouteInfoMenu().updateMenu();
		}
	}
}