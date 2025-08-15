package net.osmand.plus.settings.fragments;

import static net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine.ONLINE_ROUTING_ENGINE_PREFIX;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.DERIVED_PROFILE_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILES_LIST_UPDATED_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheet.PROFILE_KEY_ARG;
import static net.osmand.plus.routepreparationmenu.RouteOptionsBottomSheet.DIALOG_MODE_KEY;
import static net.osmand.plus.routing.TransportRoutingHelper.PUBLIC_TRANSPORT_KEY;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.profiles.SelectNavProfileBottomSheet;
import net.osmand.plus.profiles.SelectProfileBottomSheet.OnSelectProfileCallback;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.RoutingDataObject;
import net.osmand.plus.profiles.data.RoutingDataUtils;
import net.osmand.plus.profiles.data.RoutingProfilesHolder;
import net.osmand.plus.profiles.data.RoutingProfilesResources;
import net.osmand.plus.routepreparationmenu.RouteOptionsBottomSheet;
import net.osmand.plus.routepreparationmenu.RouteOptionsBottomSheet.DialogMode;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.Algorithms;

public class NavigationFragment extends BaseSettingsFragment implements OnSelectProfileCallback {

	public static final String TAG = NavigationFragment.class.getSimpleName();
	public static final String NAVIGATION_TYPE = "navigation_type";

	private static final String CUSTOMIZE_ROUTE_LINE = "customize_route_line";
	private static final String DETAILED_TRACK_GUIDANCE = "detailed_track_guidance";

	private RoutingProfilesHolder routingProfiles;
	private RoutingDataUtils routingDataUtils;
	private Preference navigationType;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		routingDataUtils = new RoutingDataUtils(app);
		updateRoutingProfiles();
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
		requireActionBarActivity().getOnBackPressedDispatcher().addCallback(this, callback);
	}

	@Override
	protected void setupPreferences() {
		navigationType = findPreference(NAVIGATION_TYPE);
		setupNavigationTypePref();

		Preference routeParameters = findPreference("route_parameters");
		SwitchPreferenceCompat showRoutingAlarms = findPreference(settings.SHOW_ROUTING_ALARMS.getId());

		routeParameters.setIcon(getContentIcon(R.drawable.ic_action_route_distance));
		showRoutingAlarms.setIcon(getPersistentPrefIcon(R.drawable.ic_action_alert));

		setupSpeakRoutingAlarmsPref();
		setupVehicleParametersPref();
		showHideCustomizeRouteLinePref();
		showTrackGuidancePref();
	}

	private void setupNavigationTypePref() {
		ApplicationMode appMode = getSelectedAppMode();
		String routingProfileKey = appMode.getRoutingProfile();
		String derivedProfile = appMode.getDerivedProfile();
		if (!Algorithms.isEmpty(routingProfileKey)) {
			ProfileDataObject profile = routingProfiles.get(routingProfileKey, derivedProfile);
			if (profile != null) {
				navigationType.setSummary(profile.getName());
				navigationType.setIcon(getActiveIcon(profile.getIconRes()));
			}
		}
	}

	private void setupSpeakRoutingAlarmsPref() {
		Drawable disabled = getContentIcon(R.drawable.ic_action_volume_mute);
		Drawable enabled = getActiveIcon(R.drawable.ic_action_volume_up);
		Drawable icon = getPersistentPrefIcon(enabled, disabled);

		SwitchPreferenceCompat speakRoutingAlarms = findPreference(settings.VOICE_MUTE.getId());
		speakRoutingAlarms.setIcon(icon);
		speakRoutingAlarms.setChecked(!settings.VOICE_MUTE.getModeValue(getSelectedAppMode()));
	}

	private void showHideCustomizeRouteLinePref() {
		Preference preference = findPreference(CUSTOMIZE_ROUTE_LINE);
		if (preference != null) {
			String routingProfile = getSelectedAppMode().getRoutingProfile();
			boolean isPublicTransport = PUBLIC_TRANSPORT_KEY.equals(routingProfile);
			preference.setVisible(!isPublicTransport);
		}
	}

	public void showTrackGuidancePref() {
		Preference preference = requirePreference(DETAILED_TRACK_GUIDANCE);
		preference.setIcon((getContentIcon(R.drawable.ic_action_attach_track)));
		preference.setSummary(settings.DETAILED_TRACK_GUIDANCE.getModeValue(getSelectedAppMode()).getNameRes());
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
		String prefId = preference.getKey();
		MapActivity activity = getMapActivity();
		if (activity != null) {
			ApplicationMode appMode = getSelectedAppMode();
			if (NAVIGATION_TYPE.equals(prefId)) {
				String selected = appMode.getRoutingProfile();
				SelectNavProfileBottomSheet.showInstance(activity, this, appMode, selected, false);
			} else if (CUSTOMIZE_ROUTE_LINE.equals(prefId)) {
				RouteLineAppearanceFragment.showInstance(activity, appMode);
			} else if (DETAILED_TRACK_GUIDANCE.equals(prefId)) {
				DetailedTrackGuidanceFragment.showInstance(activity, appMode, this);
			}
		}
		return false;
	}

	public void updateRoutingProfiles() {
		routingProfiles = routingDataUtils.getRoutingProfiles();
	}

	void updateRoutingProfile(@NonNull String profileKey, @Nullable String derivedProfile) {
		RoutingDataObject selected = routingProfiles.get(profileKey, derivedProfile);
		if (selected == null) {
			return;
		}
		routingProfiles.setSelected(selected);
		navigationType.setSummary(selected.getName());
		navigationType.setIcon(getActiveIcon(selected.getIconRes()));

		ApplicationMode appMode = getSelectedAppMode();
		updateAppMode(appMode, profileKey, derivedProfile);
	}

	public void updateAppMode(@NonNull ApplicationMode appMode, @NonNull String profileKey, @Nullable String derivedProfile) {
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
		appMode.setDerivedProfile(derivedProfile);
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
			updateRoutingProfiles();
		}
		String profileKey = args.getString(PROFILE_KEY_ARG);
		if (profileKey != null) {
			updateRoutingProfile(profileKey, args.getString(DERIVED_PROFILE_ARG));
		}
		showHideCustomizeRouteLinePref();
	}
}