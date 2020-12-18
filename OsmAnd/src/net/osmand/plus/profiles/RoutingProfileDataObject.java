package net.osmand.plus.profiles;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.router.GeneralRouter;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RoutingProfileDataObject extends ProfileDataObject {
	public static final String OSMAND_NAVIGATION = "osmand_navigation";

	private String fileName;

	public RoutingProfileDataObject(String stringKey, String name, String descr,  int iconRes, boolean isSelected, String fileName) {
		super(name, descr, stringKey, iconRes, isSelected, null);
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public enum RoutingProfilesResources {
		DIRECT_TO_MODE(R.string.routing_profile_direct_to, R.drawable.ic_action_navigation_type_direct_to),
		STRAIGHT_LINE_MODE(R.string.routing_profile_straightline, R.drawable.ic_action_split_interval),
		BROUTER_MODE(R.string.routing_profile_broutrer, R.drawable.ic_action_split_interval),
		CAR(R.string.rendering_value_car_name, R.drawable.ic_action_car_dark),
		PEDESTRIAN(R.string.rendering_value_pedestrian_name, R.drawable.ic_action_pedestrian_dark),
		BICYCLE(R.string.rendering_value_bicycle_name, R.drawable.ic_action_bicycle_dark),
		SKI(R.string.routing_profile_ski, R.drawable.ic_action_skiing),
		PUBLIC_TRANSPORT(R.string.app_mode_public_transport, R.drawable.ic_action_bus_dark),
		BOAT(R.string.app_mode_boat, R.drawable.ic_action_sail_boat_dark),
		GEOCODING(R.string.routing_profile_geocoding, R.drawable.ic_action_world_globe);

		int stringRes;
		int iconRes;

		RoutingProfilesResources(int stringRes, int iconRes) {
			this.stringRes = stringRes;
			this.iconRes = iconRes;
		}

		public int getStringRes() {
			return stringRes;
		}

		public int getIconRes() {
			return iconRes;
		}

		private static final List<String> rpValues = new ArrayList<>();

		static {
			for (RoutingProfilesResources rpr : RoutingProfilesResources.values()) {
				rpValues.add(rpr.name());
			}
		}

		public static boolean isRpValue(String value) {
			return rpValues.contains(value);
		}
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
}
