package net.osmand.plus.profiles;

import android.content.Context;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.profiles.RoutingProfileDataObject.RoutingProfilesResources;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.GeneralRouter;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileDataUtils {

	public static final String OSMAND_NAVIGATION = "osmand_navigation";
	public static final String ONLINE_NAVIGATION = "online_navigation";

	public static List<ProfileDataObject> getDataObjects(OsmandApplication app,
	                                                     List<ApplicationMode> appModes) {
		List<ProfileDataObject> profiles = new ArrayList<>();
		for (ApplicationMode mode : appModes) {
			String description = mode.getDescription();
			if (Algorithms.isEmpty(description)) {
				description = getAppModeDescription(app, mode);
			}
			profiles.add(new ProfileDataObject(mode.toHumanString(), description,
					mode.getStringKey(), mode.getIconRes(), false, mode.getProfileColor(false), mode.getProfileColor(true)));
		}
		return profiles;
	}

	public static String getAppModeDescription(Context ctx, ApplicationMode mode) {
		String description;
		if (mode.isCustomProfile()) {
			description = ctx.getString(R.string.profile_type_user_string);
		} else {
			description = ctx.getString(R.string.profile_type_osmand_string);
		}

		return description;
	}

	public static List<ProfileDataObject> getSortedRoutingProfiles(OsmandApplication app) {
		List<ProfileDataObject> result = new ArrayList<>();
		Map<String, List<ProfileDataObject>> routingProfilesByFileNames = getRoutingProfilesByFileNames(app);
		List<String> fileNames = new ArrayList<>(routingProfilesByFileNames.keySet());
		Collections.sort(fileNames, new Comparator<String>() {
			@Override
			public int compare(String s, String t1) {
				// OsmAnd navigation should be at the top of the list
				if (s.equals(OSMAND_NAVIGATION)) {
					return -1;
				} else if (t1.equals(OSMAND_NAVIGATION)) {
					return 1;

				// Online navigation should be at the bottom of the list
				} else if (s.equals(ONLINE_NAVIGATION)) {
					return 1;
				} else if (t1.equals(ONLINE_NAVIGATION)) {
					return -1;

				// Other sorted by file names
				} else {
					return s.compareToIgnoreCase(t1);
				}
			}
		});
		for (String fileName : fileNames) {
			List<ProfileDataObject> routingProfilesFromFile = routingProfilesByFileNames.get(fileName);
			if (routingProfilesFromFile != null) {
				Collections.sort(routingProfilesFromFile);
				result.addAll(routingProfilesFromFile);
			}
		}
		return result;
	}

	public static List<OnlineRoutingEngineDataObject> getOnlineRoutingProfiles(OsmandApplication app) {
		List<OnlineRoutingEngineDataObject> objects = new ArrayList<>();
		List<OnlineRoutingEngine> engines = app.getOnlineRoutingHelper().getEngines();
		for (int i = 0; i < engines.size(); i++) {
			OnlineRoutingEngine engine = engines.get(i);
			objects.add(new OnlineRoutingEngineDataObject(
					engine.getName(app), engine.getBaseUrl(), engine.getStringKey(), i));
		}
		return objects;
	}

	public static Map<String, List<ProfileDataObject>> getRoutingProfilesByFileNames(OsmandApplication app) {
		Map<String, List<ProfileDataObject>> result = new HashMap<>();
		for (final ProfileDataObject profile : getRoutingProfiles(app).values()) {
			String fileName = null;
			if (profile instanceof RoutingProfileDataObject) {
				fileName = ((RoutingProfileDataObject) profile).getFileName();
			} else if (profile instanceof OnlineRoutingEngineDataObject) {
				fileName = ONLINE_NAVIGATION;
			}
			fileName = fileName != null ? fileName : OSMAND_NAVIGATION;
			if (result.containsKey(fileName)) {
				result.get(fileName).add(profile);
			} else {
				result.put(fileName, new ArrayList<ProfileDataObject>() {
					{ add(profile); }
				});
			}
		}
		return result;
	}

	public static Map<String, ProfileDataObject> getRoutingProfiles(OsmandApplication context) {
		Map<String, ProfileDataObject> profilesObjects = new HashMap<>();
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
		for (OnlineRoutingEngineDataObject onlineEngine : getOnlineRoutingProfiles(context)) {
			profilesObjects.put(onlineEngine.getStringKey(), onlineEngine);
		}
		return profilesObjects;
	}

	private static void collectRoutingProfilesFromConfig(OsmandApplication app, RoutingConfiguration.Builder builder,
	                                                     Map<String, ProfileDataObject> profilesObjects, List<String> disabledRouterNames) {
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

}
