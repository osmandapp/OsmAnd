package net.osmand.plus.profiles.data;

import net.osmand.plus.R;

import java.util.ArrayList;
import java.util.List;


public class RoutingDataObject extends ProfileDataObject {

	private String fileName;

	public RoutingDataObject(String stringKey,
	                         String name,
	                         String description,
	                         int iconRes,
	                         boolean isSelected,
	                         String fileName) {
		super(name, description, stringKey, iconRes, isSelected, null, null);
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	public boolean isOnline() {
		return false;
	}

	public boolean isPredefined() {
		return false;
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

}
