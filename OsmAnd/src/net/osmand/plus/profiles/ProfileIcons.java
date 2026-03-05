package net.osmand.plus.profiles;

import android.content.res.Resources;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public enum ProfileIcons {
	DEFAULT(R.drawable.ic_world_globe_dark, "ic_world_globe_dark"),
	CAR(R.drawable.ic_action_car_dark, "ic_action_car_dark"),
	TAXI(R.drawable.ic_action_taxi, "ic_action_taxi"),
	TRUCK(R.drawable.ic_action_truck_dark, "ic_action_truck_dark"),
	SHUTTLE_BUS(R.drawable.ic_action_shuttle_bus, "ic_action_shuttle_bus"),
	BUS(R.drawable.ic_action_bus_dark, "ic_action_bus_dark"),
	SUBWAY(R.drawable.ic_action_subway, "ic_action_subway"),
	BICYCLE(R.drawable.ic_action_bicycle_dark, "ic_action_bicycle_dark"),
	HORSE(R.drawable.ic_action_horse, "ic_action_horse"),
	SAIL_BOAT(R.drawable.ic_action_sail_boat_dark, "ic_action_sail_boat_dark"),
	AIRCRAFT(R.drawable.ic_action_aircraft, "ic_action_aircraft"),
	HELICOPTER(R.drawable.ic_action_helicopter, "ic_action_helicopter"),
	TRANSPORTER(R.drawable.ic_action_personal_transporter, "ic_action_personal_transporter"),
	MONOWHEEL(R.drawable.ic_action_monowheel, "ic_action_monowheel"),
	SCOOTER(R.drawable.ic_action_scooter, "ic_action_scooter"),
	WHEELCHAIR(R.drawable.ic_action_wheelchair, "ic_action_wheelchair"),
	WHEELCHAIR_FORWARD(R.drawable.ic_action_wheelchair_forward, "ic_action_wheelchair_forward"),
	UFO(R.drawable.ic_action_ufo, "ic_action_ufo"),
	BABY_TRANSPORT(R.drawable.ic_action_baby_transport, "ic_action_baby_transport"),
	SUV(R.drawable.ic_action_suv, "ic_action_suv"),
	CAMPERVAN(R.drawable.ic_action_campervan, "ic_action_campervan"),
	CAMPER(R.drawable.ic_action_camper, "ic_action_camper"),
	PICKUP_TRUCK(R.drawable.ic_action_pickup_truck, "ic_action_pickup_truck"),
	WAGON(R.drawable.ic_action_wagon, "ic_action_wagon"),
	SKI_TOURING(R.drawable.ic_action_utv, "ic_action_utv"),
	GO_CART(R.drawable.ic_action_go_cart, "ic_action_go_cart"),
	OSM(R.drawable.ic_action_openstreetmap_logo, "ic_action_openstreetmap_logo");

	@DrawableRes
	private final int resId;
	private final String resStringId;

	ProfileIcons(@DrawableRes int resId, @NonNull String resStringId) {
		this.resId = resId;
		this.resStringId = resStringId;
	}

	public static ArrayList<Integer> getIcons(OsmandApplication app) {
		LinkedHashSet<Integer> iconIds = new LinkedHashSet<>();
		for (ProfileIcons pi : values()) {
			iconIds.add(pi.resId);
		}
		for (RouteActivity activity : RouteActivityHelper.INSTANCE.getActivities()) {
			int categoryIconId = app.getResources().getIdentifier("mx_" + activity.getIconName(), "drawable", app.getPackageName());
			if (categoryIconId != 0) {
				iconIds.add(categoryIconId);
			}
		}
		return new ArrayList<>(iconIds);
	}

	public int getResId() {
		return resId;
	}

	public String getResStringId() {
		return resStringId;
	}

	@NonNull
	public static String getResStringByResId(int resId) {
		String resStringId = findResStringByResId(resId);
		return resStringId != null ? resStringId : DEFAULT.getResStringId();
	}

	@NonNull
	public static String getResStringByResId(@NonNull OsmandApplication app, int resId) {
		String resStringId = findResStringByResId(resId);
		if (resStringId == null) {
			try {
				resStringId = app.getResources().getResourceEntryName(resId);
			} catch (Resources.NotFoundException e) {
				return DEFAULT.getResStringId();
			}
		}
		String canonicalIconName = getCanonicalIconName(app, resStringId);
		return canonicalIconName != null ? canonicalIconName : DEFAULT.getResStringId();
	}

	@Nullable
	public static String getCanonicalIconName(@NonNull OsmandApplication app, @Nullable String iconName) {
		if (Algorithms.isEmpty(iconName)) {
			return iconName;
		}
		String mappedIconName = getMappedMxIconName(iconName);
		if (mappedIconName != null) {
			if (hasDrawable(app, mappedIconName)) {
				return mappedIconName;
			}
			return hasDrawable(app, iconName) ? iconName : null;
		}
		return hasDrawable(app, iconName) ? iconName : null;
	}

	private static boolean hasDrawable(@NonNull OsmandApplication app, @NonNull String iconName) {
		return app.getResources().getIdentifier(iconName, "drawable", app.getPackageName()) != 0;
	}

	@Nullable
	private static String getMappedMxIconName(@NonNull String iconName) {
		return switch (iconName) {
			case "ic_action_train" -> "mx_activities_train";
			case "ic_action_motorcycle_dark" -> "mx_activities_motorcycle";
			case "ic_action_enduro_motorcycle" -> "mx_activities_enduro_motorcycle";
			case "ic_action_motor_scooter" -> "mx_activities_motor_scooter";
			case "ic_action_mountain_bike" -> "mx_activities_mountain_bike";
			case "ic_action_pedestrian_dark" -> "mx_activities_pedestrian";
			case "ic_action_trekking_dark" -> "mx_activities_trekking";
			case "ic_action_hill_climbing" -> "mx_activities_hill_climbing";
			case "ic_action_skiing" -> "mx_activities_skiing";
			case "ic_action_paragliding" -> "mx_activities_paragliding";
			case "ic_action_hang_gliding" -> "mx_activities_hang_gliding";
			case "ic_action_inline_skates" -> "mx_activities_inline_skates";
			case "ic_action_offroad" -> "mx_activities_offroad";
			case "ic_action_ski_touring" -> "mx_activities_ski_touring";
			case "ic_action_snowmobile" -> "mx_activities_snowmobile";
			case "ic_action_motorboat" -> "mx_activities_motorboat";
			case "ic_action_kayak" -> "mx_activities_kayak";
			case "ic_action_light_aircraft" -> "mx_activities_light_aircraft";
			default -> null;
		};
	}

	private static String findResStringByResId(int resId) {
		for (ProfileIcons pi : values()) {
			if (pi.resId == resId) {
				return pi.resStringId;
			}
		}
		return null;
	}
}