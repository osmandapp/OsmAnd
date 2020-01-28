package net.osmand.plus.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.LayoutInflater;
import android.view.View;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.profiles.ProfileDataObject;
import net.osmand.plus.profiles.RoutingProfileDataObject;
import net.osmand.plus.profiles.RoutingProfileDataObject.RoutingProfilesResources;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.DIALOG_TYPE;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.IS_PROFILE_IMPORTED_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.PROFILE_KEY_ARG;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.SELECTED_KEY;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.TYPE_NAV_PROFILE;

public class NavigationFragment extends BaseSettingsFragment {

	public static final String TAG = NavigationFragment.class.getSimpleName();
	public static final String NAVIGATION_TYPE = "navigation_type";

	private SelectProfileBottomSheetDialogFragment.SelectProfileListener navTypeListener;
	private Map<String, RoutingProfileDataObject> routingProfileDataObjects;
	private Preference navigationType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		routingProfileDataObjects = getRoutingProfiles(app);
	}

	@Override
	protected void createToolbar(LayoutInflater inflater, View view) {
		super.createToolbar(inflater, view);
		view.findViewById(R.id.profile_button).setVisibility(View.GONE);
	}

	@Override
	protected void setupPreferences() {
		navigationType = findPreference(NAVIGATION_TYPE);
		Preference routeParameters = findPreference("route_parameters");
		SwitchPreferenceCompat showRoutingAlarms = (SwitchPreferenceCompat) findPreference(settings.SHOW_ROUTING_ALARMS.getId());
		SwitchPreferenceCompat speakRoutingAlarms = (SwitchPreferenceCompat) findPreference(settings.VOICE_MUTE.getId());
		SwitchPreferenceCompat turnScreenOn = (SwitchPreferenceCompat) findPreference(settings.TURN_SCREEN_ON_ENABLED.getId());
		SwitchPreferenceEx animateMyLocation = (SwitchPreferenceEx) findPreference(settings.ANIMATE_MY_LOCATION.getId());
		if (getSelectedAppMode().getRoutingProfile() != null) {
			GeneralRouter routingProfile = app.getRoutingConfig().getRouter(getSelectedAppMode().getRoutingProfile());
			if (routingProfile != null) {
				String profileNameUC = routingProfile.getProfileName().toUpperCase();
				if (RoutingProfilesResources.isRpValue(profileNameUC)) {
					RoutingProfilesResources routingProfilesResources = RoutingProfilesResources.valueOf(profileNameUC);
					navigationType.setSummary(routingProfilesResources.getStringRes());
					navigationType.setIcon(getContentIcon(routingProfilesResources.getIconRes()));
				} else {
					navigationType.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark));
				}
			}
		}
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
		if (preference.getKey().equals(NAVIGATION_TYPE)) {
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
		return false;
	}

	public SelectProfileBottomSheetDialogFragment.SelectProfileListener getNavProfileListener() {
		if (navTypeListener == null) {
			navTypeListener = new SelectProfileBottomSheetDialogFragment.SelectProfileListener() {
				@Override
				public void onSelectedType(Bundle args) {
					if (args.getBoolean(IS_PROFILE_IMPORTED_ARG)) {
						routingProfileDataObjects = getRoutingProfiles(app);
					}
					updateRoutingProfile(args.getString(PROFILE_KEY_ARG));
				}
			};
		}
		return navTypeListener;
	}

	void updateRoutingProfile(String profileKey) {
		RoutingProfileDataObject selectedRoutingProfileDataObject = routingProfileDataObjects.get(profileKey);
		if (profileKey == null || selectedRoutingProfileDataObject == null) {
			return;
		}
		for (Map.Entry<String, RoutingProfileDataObject> rp : routingProfileDataObjects.entrySet()) {
			boolean selected = profileKey.equals(rp.getKey());
			rp.getValue().setSelected(selected);
		}
		navigationType.setSummary(selectedRoutingProfileDataObject.getName());
		navigationType.setIcon(getContentIcon(selectedRoutingProfileDataObject.getIconRes()));

		ApplicationMode appMode = getSelectedAppMode();
		RouteProvider.RouteService routeService;
		if (profileKey.equals(RoutingProfilesResources.STRAIGHT_LINE_MODE.name())) {
			routeService = RouteProvider.RouteService.STRAIGHT;
		} else if (profileKey.equals(RoutingProfilesResources.BROUTER_MODE.name())) {
			routeService = RouteProvider.RouteService.BROUTER;
		} else {
			routeService = RouteProvider.RouteService.OSMAND;
		}
		appMode.setRouteService(app, routeService);
		appMode.setRoutingProfile(app, profileKey);
	}

	public static Map<String, RoutingProfileDataObject> getRoutingProfiles(OsmandApplication context) {
		Map<String, RoutingProfileDataObject> profilesObjects = new HashMap<>();
		profilesObjects.put(RoutingProfilesResources.STRAIGHT_LINE_MODE.name(), new RoutingProfileDataObject(
				RoutingProfilesResources.STRAIGHT_LINE_MODE.name(),
				context.getString(RoutingProfilesResources.STRAIGHT_LINE_MODE.getStringRes()),
				context.getString(R.string.special_routing_type),
				RoutingProfilesResources.STRAIGHT_LINE_MODE.getIconRes(),
				false, null));
		if (context.getBRouterService() != null) {
			profilesObjects.put(RoutingProfilesResources.BROUTER_MODE.name(), new RoutingProfileDataObject(
					RoutingProfilesResources.BROUTER_MODE.name(),
					context.getString(RoutingProfilesResources.BROUTER_MODE.getStringRes()),
					context.getString(R.string.third_party_routing_type),
					RoutingProfilesResources.BROUTER_MODE.getIconRes(),
					false, null));
		}

		Map<String, GeneralRouter> inputProfiles = context.getRoutingConfig().getAllRouters();
		for (Map.Entry<String, GeneralRouter> e : inputProfiles.entrySet()) {
			if (!e.getKey().equals("geocoding")) {
				int iconRes = R.drawable.ic_action_gdirections_dark;
				String name = e.getValue().getProfileName();
				String description = context.getString(R.string.osmand_default_routing);
				if (!Algorithms.isEmpty(e.getValue().getFilename())) {
					description = e.getValue().getFilename();
				} else if (RoutingProfilesResources.isRpValue(name.toUpperCase())){
					iconRes = RoutingProfilesResources.valueOf(name.toUpperCase()).getIconRes();
					name = context
							.getString(RoutingProfilesResources.valueOf(name.toUpperCase()).getStringRes());
				}
				profilesObjects.put(e.getKey(), new RoutingProfileDataObject(e.getKey(), name, description,
						iconRes, false, e.getValue().getFilename()));
			}
		}
		return profilesObjects;
	}

	public static List<ProfileDataObject> getBaseProfiles(Context ctx) {
		List<ProfileDataObject> profiles = new ArrayList<>();
		for (ApplicationMode mode : ApplicationMode.getDefaultValues()) {
			if (mode != ApplicationMode.DEFAULT) {
				profiles.add(new ProfileDataObject(mode.toHumanString(ctx), mode.getDescription(ctx),
						mode.getStringKey(), mode.getIconRes(), false, mode.getIconColorInfo()));
			}
		}
		return profiles;
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