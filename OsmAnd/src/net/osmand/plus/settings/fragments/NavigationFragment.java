package net.osmand.plus.settings.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.profiles.ProfileDataObject;
import net.osmand.plus.profiles.ProfileDataUtils;
import net.osmand.plus.routepreparationmenu.RouteOptionsBottomSheet;
import net.osmand.plus.routepreparationmenu.RouteOptionsBottomSheet.DialogMode;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.profiles.RoutingProfileDataObject.RoutingProfilesResources;
import net.osmand.plus.profiles.SelectProfileBottomSheet;
import net.osmand.plus.profiles.SelectProfileBottomSheet.OnSelectProfileCallback;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.util.Algorithms;

import java.util.Map;

import static net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine.ONLINE_ROUTING_ENGINE_PREFIX;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILES_LIST_UPDATED_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILE_KEY_ARG;
import static net.osmand.plus.routepreparationmenu.RouteOptionsBottomSheet.DIALOG_MODE_KEY;

public class NavigationFragment extends BaseSettingsFragment implements OnSelectProfileCallback {

	public static final String TAG = NavigationFragment.class.getSimpleName();
	public static final String NAVIGATION_TYPE = "navigation_type";

	private Map<String, ProfileDataObject> routingProfileDataObjects;
	private Preference navigationType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		updateRoutingProfilesDataObjects();
		setupOnBackPressedCallback();
	}

	private void setupOnBackPressedCallback() {
		OnBackPressedCallback callback = new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				MapActivity mapActivity = getMapActivity();
				Bundle args = getArguments();
				if (mapActivity != null && args != null) {
					String dialogModeName = args.getString(DIALOG_MODE_KEY, null);
					if (DialogMode.getModeByName(dialogModeName) == DialogMode.PLAN_ROUTE) {
						FragmentManager fm = mapActivity.getSupportFragmentManager();
						Fragment fragment = fm.findFragmentByTag(MeasurementToolFragment.TAG);
						if (fragment != null) {
							RouteOptionsBottomSheet.showInstance(
									mapActivity, fragment, DialogMode.PLAN_ROUTE,
									getSelectedAppMode().getStringKey());
							((MeasurementToolFragment) fragment).getOnBackPressedCallback().setEnabled(true);
						}
					}
				}
				dismiss();
			}
		};
		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, callback);
	}

	@Override
	protected void setupPreferences() {
		navigationType = findPreference(NAVIGATION_TYPE);
		setupNavigationTypePref();

		Preference routeParameters = findPreference("route_parameters");
		SwitchPreferenceCompat showRoutingAlarms = (SwitchPreferenceCompat) findPreference(settings.SHOW_ROUTING_ALARMS.getId());
		SwitchPreferenceEx animateMyLocation = (SwitchPreferenceEx) findPreference(settings.ANIMATE_MY_LOCATION.getId());

		routeParameters.setIcon(getContentIcon(R.drawable.ic_action_route_distance));
		showRoutingAlarms.setIcon(getPersistentPrefIcon(R.drawable.ic_action_alert));

		setupSpeakRoutingAlarmsPref();
		setupVehicleParametersPref();

		animateMyLocation.setDescription(getString(R.string.animate_my_location_desc));
	}

	private void setupNavigationTypePref() {
		String routingProfileKey = getSelectedAppMode().getRoutingProfile();
		if (!Algorithms.isEmpty(routingProfileKey)) {
			ProfileDataObject routingProfileDataObject = routingProfileDataObjects.get(routingProfileKey);
			if (routingProfileDataObject != null) {
				navigationType.setSummary(routingProfileDataObject.getName());
				navigationType.setIcon(getActiveIcon(routingProfileDataObject.getIconRes()));
			}
		}
	}

	private void setupSpeakRoutingAlarmsPref() {
		Drawable disabled = getContentIcon(R.drawable.ic_action_volume_mute);
		Drawable enabled = getActiveIcon(R.drawable.ic_action_volume_up);
		Drawable icon = getPersistentPrefIcon(enabled, disabled);

		SwitchPreferenceCompat speakRoutingAlarms = (SwitchPreferenceCompat) findPreference(settings.VOICE_MUTE.getId());
		speakRoutingAlarms.setIcon(icon);
		speakRoutingAlarms.setChecked(!settings.VOICE_MUTE.getModeValue(getSelectedAppMode()));
	}

	@Override
	public void onApplyPreferenceChange(String prefId, boolean applyToAllProfiles, Object newValue) {
		if (settings.VOICE_MUTE.getId().equals(prefId) && newValue instanceof Boolean) {
			applyPreference(prefId, applyToAllProfiles, !(Boolean) newValue);
			updateMenu();
		} else {
			applyPreference(prefId, applyToAllProfiles, newValue);
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals(NAVIGATION_TYPE)) {
			String routingProfileKey =
					getSelectedAppMode() != null ? getSelectedAppMode().getRoutingProfile() : null;
			if (getActivity() != null) {
				SelectProfileBottomSheet.showInstance(
						getActivity(), SelectProfileBottomSheet.DialogMode.NAVIGATION_PROFILE,
						this, getSelectedAppMode(), routingProfileKey, false);
			}
		}
		return false;
	}

	private void updateRoutingProfilesDataObjects() {
		routingProfileDataObjects = ProfileDataUtils.getRoutingProfiles(app);
	}

	void updateRoutingProfile(String profileKey) {
		ProfileDataObject selectedRoutingProfileDataObject = routingProfileDataObjects.get(profileKey);
		if (profileKey == null || selectedRoutingProfileDataObject == null) {
			return;
		}
		for (Map.Entry<String, ProfileDataObject> rp : routingProfileDataObjects.entrySet()) {
			boolean selected = profileKey.equals(rp.getKey());
			rp.getValue().setSelected(selected);
		}
		navigationType.setSummary(selectedRoutingProfileDataObject.getName());
		navigationType.setIcon(getActiveIcon(selectedRoutingProfileDataObject.getIconRes()));

		ApplicationMode appMode = getSelectedAppMode();
		RouteService routeService;
		if (profileKey.equals(RoutingProfilesResources.STRAIGHT_LINE_MODE.name())) {
			routeService = RouteService.STRAIGHT;
		} else if (profileKey.equals(RoutingProfilesResources.DIRECT_TO_MODE.name())) {
			routeService = RouteService.DIRECT_TO;
		} else if (profileKey.equals(RoutingProfilesResources.BROUTER_MODE.name())) {
			routeService = RouteService.BROUTER;
		} else if (profileKey.startsWith(ONLINE_ROUTING_ENGINE_PREFIX)) {
			routeService = RouteService.ONLINE;
		} else {
			routeService = RouteService.OSMAND;
		}
		appMode.setRouteService(routeService);
		appMode.setRoutingProfile(profileKey);
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

	@Override
	public void onProfileSelected(Bundle args) {
		if (args.getBoolean(PROFILES_LIST_UPDATED_ARG)) {
			updateRoutingProfilesDataObjects();
		}
		updateRoutingProfile(args.getString(PROFILE_KEY_ARG));
	}
}