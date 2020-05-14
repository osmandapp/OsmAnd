package net.osmand.plus.settings;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.profiles.ProfileDataObject;
import net.osmand.plus.profiles.RoutingProfileDataObject;
import net.osmand.plus.profiles.RoutingProfileDataObject.RoutingProfilesResources;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.router.GeneralRouter;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	public static final String OSMAND_NAVIGATION = "osmand_navigation";

	private SelectProfileBottomSheetDialogFragment.SelectProfileListener navTypeListener;
	private Map<String, RoutingProfileDataObject> routingProfileDataObjects;
	private Preference navigationType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		routingProfileDataObjects = getRoutingProfiles(app);
	}

	@Override
	protected void setupPreferences() {
		navigationType = findPreference(NAVIGATION_TYPE);
		setupNavigationTypePref();

		Preference routeParameters = findPreference("route_parameters");
		SwitchPreferenceCompat showRoutingAlarms = (SwitchPreferenceCompat) findPreference(settings.SHOW_ROUTING_ALARMS.getId());
		SwitchPreferenceCompat turnScreenOn = (SwitchPreferenceCompat) findPreference(settings.TURN_SCREEN_ON_ENABLED.getId());
		SwitchPreferenceEx animateMyLocation = (SwitchPreferenceEx) findPreference(settings.ANIMATE_MY_LOCATION.getId());

		routeParameters.setIcon(getContentIcon(R.drawable.ic_action_route_distance));
		showRoutingAlarms.setIcon(getPersistentPrefIcon(R.drawable.ic_action_alert));
		turnScreenOn.setIcon(getPersistentPrefIcon(R.drawable.ic_action_turn_screen_on));

		setupSpeakRoutingAlarmsPref();
		setupVehicleParametersPref();

		animateMyLocation.setDescription(getString(R.string.animate_my_location_desc));
	}

	private void setupNavigationTypePref() {
		String routingProfileKey = getSelectedAppMode().getRoutingProfile();
		if (!Algorithms.isEmpty(routingProfileKey)) {
			RoutingProfileDataObject routingProfileDataObject = routingProfileDataObjects.get(routingProfileKey);
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
			final SelectProfileBottomSheetDialogFragment dialog = new SelectProfileBottomSheetDialogFragment();
			Bundle bundle = new Bundle();
			if (getSelectedAppMode() != null) {
				bundle.putString(SELECTED_KEY, getSelectedAppMode().getRoutingProfile());
			}
			bundle.putString(DIALOG_TYPE, TYPE_NAV_PROFILE);
			dialog.setArguments(bundle);
			dialog.setUsedOnMap(false);
			dialog.setAppMode(getSelectedAppMode());
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
		navigationType.setIcon(getActiveIcon(selectedRoutingProfileDataObject.getIconRes()));

		ApplicationMode appMode = getSelectedAppMode();
		RouteProvider.RouteService routeService;
		if (profileKey.equals(RoutingProfilesResources.STRAIGHT_LINE_MODE.name())) {
			routeService = RouteProvider.RouteService.STRAIGHT;
		} else if (profileKey.equals(RoutingProfilesResources.DIRECT_TO_MODE.name())) {
			routeService = RouteProvider.RouteService.DIRECT_TO;
		} else if (profileKey.equals(RoutingProfilesResources.BROUTER_MODE.name())) {
			routeService = RouteProvider.RouteService.BROUTER;
		} else {
			routeService = RouteProvider.RouteService.OSMAND;
		}
		appMode.setRouteService(routeService);
		appMode.setRoutingProfile(profileKey);
	}

	public static List<RoutingProfileDataObject> getSortedRoutingProfiles(OsmandApplication app) {
		List<RoutingProfileDataObject> result = new ArrayList<>();
		Map<String, List<RoutingProfileDataObject>> routingProfilesByFileNames = getRoutingProfilesByFileNames(app);
		List<String> fileNames = new ArrayList<>(routingProfilesByFileNames.keySet());
		Collections.sort(fileNames, new Comparator<String>() {
			@Override
			public int compare(String s, String t1) {
				return s.equals(OSMAND_NAVIGATION) ? -1 : t1.equals(OSMAND_NAVIGATION) ? 1 : s.compareToIgnoreCase(t1);
			}
		});
		for (String fileName : fileNames) {
			List<RoutingProfileDataObject> routingProfilesFromFile = routingProfilesByFileNames.get(fileName);
			if (routingProfilesFromFile != null) {
				Collections.sort(routingProfilesFromFile);
				result.addAll(routingProfilesFromFile);
			}
		}
		return result;
	}

	public static Map<String, List<RoutingProfileDataObject>> getRoutingProfilesByFileNames(OsmandApplication app) {
		Map<String, List<RoutingProfileDataObject>> result = new HashMap<>();
		for (final RoutingProfileDataObject profile : getRoutingProfiles(app).values()) {
			String fileName = profile.getFileName() != null ? profile.getFileName() : OSMAND_NAVIGATION;
			if (result.containsKey(fileName)) {
				result.get(fileName).add(profile);
			} else {
				result.put(fileName, new ArrayList<RoutingProfileDataObject>() {
					{ add(profile); }
				});
			}
		}
		return result;
	}

	public static Map<String, RoutingProfileDataObject> getRoutingProfiles(OsmandApplication context) {
		Map<String, RoutingProfileDataObject> profilesObjects = new HashMap<>();
		profilesObjects.put(RoutingProfilesResources.STRAIGHT_LINE_MODE.name(), new RoutingProfileDataObject(
				RoutingProfilesResources.STRAIGHT_LINE_MODE.name(),
				context.getString(RoutingProfilesResources.STRAIGHT_LINE_MODE.getStringRes()),
				context.getString(R.string.special_routing_type),
				RoutingProfilesResources.STRAIGHT_LINE_MODE.getIconRes(),
				false, null));
		profilesObjects.put(RoutingProfilesResources.DIRECT_TO_MODE.name(), new RoutingProfileDataObject(
				RoutingProfilesResources.DIRECT_TO_MODE.name(),
				context.getString(RoutingProfilesResources.DIRECT_TO_MODE.getStringRes()),
				context.getString(R.string.special_routing_type),
				RoutingProfilesResources.DIRECT_TO_MODE.getIconRes(),
				false, null));
		if (context.getBRouterService() != null) {
			profilesObjects.put(RoutingProfilesResources.BROUTER_MODE.name(), new RoutingProfileDataObject(
					RoutingProfilesResources.BROUTER_MODE.name(),
					context.getString(RoutingProfilesResources.BROUTER_MODE.getStringRes()),
					context.getString(R.string.third_party_routing_type),
					RoutingProfilesResources.BROUTER_MODE.getIconRes(),
					false, null));
		}

		List<String> disabledRouterNames = OsmandPlugin.getDisabledRouterNames();
		for (RoutingConfiguration.Builder builder : context.getAllRoutingConfigs()) {
			collectRoutingProfilesFromConfig(context, builder, profilesObjects, disabledRouterNames);
		}
		return profilesObjects;
	}

	private static void collectRoutingProfilesFromConfig(OsmandApplication app, RoutingConfiguration.Builder builder,
	                                                     Map<String, RoutingProfileDataObject> profilesObjects, List<String> disabledRouterNames) {
		for (Map.Entry<String, GeneralRouter> entry : builder.getAllRouters().entrySet()) {
			String routerKey = entry.getKey();
			GeneralRouter router = entry.getValue();
			if (!routerKey.equals("geocoding") && !disabledRouterNames.contains(router.getFilename())) {
				int iconRes = R.drawable.ic_action_gdirections_dark;
				String name = router.getProfileName();
				String description = app.getString(R.string.osmand_default_routing);
				String fileName = router.getFilename();
				if (!Algorithms.isEmpty(fileName)) {
					description = fileName;
				} else if (RoutingProfilesResources.isRpValue(name.toUpperCase())) {
					iconRes = RoutingProfilesResources.valueOf(name.toUpperCase()).getIconRes();
					name = app.getString(RoutingProfilesResources.valueOf(name.toUpperCase()).getStringRes());
				}
				profilesObjects.put(routerKey, new RoutingProfileDataObject(routerKey, name, description,
						iconRes, false, fileName));
			}
		}
	}

	public static List<ProfileDataObject> getBaseProfiles(OsmandApplication app) {
		return getBaseProfiles(app, false);
	}

	public static List<ProfileDataObject> getBaseProfiles(OsmandApplication app, boolean includeBrowseMap) {
		List<ProfileDataObject> profiles = new ArrayList<>();
		for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
			if (mode != ApplicationMode.DEFAULT || includeBrowseMap) {
				String description = mode.getDescription();
				if (Algorithms.isEmpty(description)) {
					description = getAppModeDescription(app, mode);
				}
				profiles.add(new ProfileDataObject(mode.toHumanString(), description,
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