package net.osmand.plus.profiles;

import android.content.Context;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
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

	public static List<ProfileDataObject> getDataObjects(OsmandApplication app,
	                                                     List<ApplicationMode> appModes) {
		List<ProfileDataObject> profiles = new ArrayList<>();
		for (ApplicationMode mode : appModes) {
			String description = mode.getDescription();
			if (Algorithms.isEmpty(description)) {
				description = getAppModeDescription(app, mode);
			}
			profiles.add(new ProfileDataObject(mode.toHumanString(), description,
					mode.getStringKey(), mode.getIconRes(), false, mode.getIconColorInfo()));
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
		profilesObjects.put(RoutingProfileDataObject.RoutingProfilesResources.STRAIGHT_LINE_MODE.name(), new RoutingProfileDataObject(
				RoutingProfileDataObject.RoutingProfilesResources.STRAIGHT_LINE_MODE.name(),
				context.getString(RoutingProfileDataObject.RoutingProfilesResources.STRAIGHT_LINE_MODE.getStringRes()),
				context.getString(R.string.special_routing_type),
				RoutingProfileDataObject.RoutingProfilesResources.STRAIGHT_LINE_MODE.getIconRes(),
				false, null));
		profilesObjects.put(RoutingProfileDataObject.RoutingProfilesResources.DIRECT_TO_MODE.name(), new RoutingProfileDataObject(
				RoutingProfileDataObject.RoutingProfilesResources.DIRECT_TO_MODE.name(),
				context.getString(RoutingProfileDataObject.RoutingProfilesResources.DIRECT_TO_MODE.getStringRes()),
				context.getString(R.string.special_routing_type),
				RoutingProfileDataObject.RoutingProfilesResources.DIRECT_TO_MODE.getIconRes(),
				false, null));
		if (context.getBRouterService() != null) {
			profilesObjects.put(RoutingProfileDataObject.RoutingProfilesResources.BROUTER_MODE.name(), new RoutingProfileDataObject(
					RoutingProfileDataObject.RoutingProfilesResources.BROUTER_MODE.name(),
					context.getString(RoutingProfileDataObject.RoutingProfilesResources.BROUTER_MODE.getStringRes()),
					context.getString(R.string.third_party_routing_type),
					RoutingProfileDataObject.RoutingProfilesResources.BROUTER_MODE.getIconRes(),
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
				} else if (RoutingProfileDataObject.RoutingProfilesResources.isRpValue(name.toUpperCase())) {
					iconRes = RoutingProfileDataObject.RoutingProfilesResources.valueOf(name.toUpperCase()).getIconRes();
					name = app.getString(RoutingProfileDataObject.RoutingProfilesResources.valueOf(name.toUpperCase()).getStringRes());
				}
				profilesObjects.put(routerKey, new RoutingProfileDataObject(routerKey, name, description,
						iconRes, false, fileName));
			}
		}
	}

}
