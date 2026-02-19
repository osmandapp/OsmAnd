package net.osmand.plus.profiles;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.shared.gpx.primitives.RouteActivity;

import java.util.ArrayList;

public enum ProfileIcons {
	DEFAULT(R.drawable.ic_world_globe_dark, "ic_world_globe_dark"),
	CAR(R.drawable.ic_action_car_dark, "ic_action_car_dark"),
	TAXI(R.drawable.ic_action_taxi, "ic_action_taxi"),
	TRUCK(R.drawable.ic_action_truck_dark, "ic_action_truck_dark"),
	SHUTTLE_BUS(R.drawable.ic_action_shuttle_bus, "ic_action_shuttle_bus"),
	BUS(R.drawable.ic_action_bus_dark, "ic_action_bus_dark"),
	SUBWAY(R.drawable.ic_action_subway, "ic_action_subway"),
	TRAIN(R.drawable.ic_action_train, "ic_action_train"),
	MOTORCYCLE(R.drawable.ic_action_motorcycle_dark, "ic_action_motorcycle_dark"),
	ENDURO_MOTORCYCLE(R.drawable.ic_action_enduro_motorcycle, "ic_action_enduro_motorcycle"),
	MOTOR_SCOOTER(R.drawable.ic_action_motor_scooter, "ic_action_motor_scooter"),
	BICYCLE(R.drawable.ic_action_bicycle_dark, "ic_action_bicycle_dark"),
	MOUNTAIN_BICYCLE(R.drawable.ic_action_mountain_bike, "ic_action_mountain_bike"),
	HORSE(R.drawable.ic_action_horse, "ic_action_horse"),
	PEDESTRIAN(R.drawable.ic_action_pedestrian_dark, "ic_action_pedestrian_dark"),
	TREKKING(R.drawable.ic_action_trekking_dark, "ic_action_trekking_dark"),
	CLIMBING(R.drawable.ic_action_hill_climbing, "ic_action_hill_climbing"),
	SKIING(R.drawable.ic_action_skiing, "ic_action_skiing"),
	SAIL_BOAT(R.drawable.ic_action_sail_boat_dark, "ic_action_sail_boat_dark"),
	AIRCRAFT(R.drawable.ic_action_aircraft, "ic_action_aircraft"),
	HELICOPTER(R.drawable.ic_action_helicopter, "ic_action_helicopter"),
	PARAGLIDING(R.drawable.ic_action_paragliding, "ic_action_paragliding"),
	HANG_GLIDING(R.drawable.ic_action_hang_gliding, "ic_action_hang_gliding"),
	TRANSPORTER(R.drawable.ic_action_personal_transporter, "ic_action_personal_transporter"),
	MONOWHEEL(R.drawable.ic_action_monowheel, "ic_action_monowheel"),
	SCOOTER(R.drawable.ic_action_scooter, "ic_action_scooter"),
	INLINE_SKATES(R.drawable.ic_action_inline_skates, "ic_action_inline_skates"),
	WHEELCHAIR(R.drawable.ic_action_wheelchair, "ic_action_wheelchair"),
	WHEELCHAIR_FORWARD(R.drawable.ic_action_wheelchair_forward, "ic_action_wheelchair_forward"),
	UFO(R.drawable.ic_action_ufo, "ic_action_ufo"),
	BABY_TRANSPORT(R.drawable.ic_action_baby_transport, "ic_action_baby_transport"),
	OFFROAD(R.drawable.ic_action_offroad, "ic_action_offroad"),
	SUV(R.drawable.ic_action_suv, "ic_action_suv"),
	CAMPERVAN(R.drawable.ic_action_campervan, "ic_action_campervan"),
	CAMPER(R.drawable.ic_action_camper, "ic_action_camper"),
	PICKUP_TRUCK(R.drawable.ic_action_pickup_truck, "ic_action_pickup_truck"),
	WAGON(R.drawable.ic_action_wagon, "ic_action_wagon"),
	UTV(R.drawable.ic_action_ski_touring, "ic_action_ski_touring"),
	SKI_TOURING(R.drawable.ic_action_utv, "ic_action_utv"),
	SNOWMOBILE(R.drawable.ic_action_snowmobile, "ic_action_snowmobile"),
	GO_CART(R.drawable.ic_action_go_cart, "ic_action_go_cart"),
	OSM(R.drawable.ic_action_openstreetmap_logo, "ic_action_openstreetmap_logo"),
	MOTORBOAT(R.drawable.ic_action_motorboat, "ic_action_motorboat"),
	KAYAK(R.drawable.ic_action_kayak, "ic_action_kayak"),
	LIGHT_AIRCRAFT(R.drawable.ic_action_light_aircraft, "ic_action_light_aircraft");

	@DrawableRes
	private final int resId;
	private final String resStringId;

	ProfileIcons(@DrawableRes int resId, @NonNull String resStringId) {
		this.resId = resId;
		this.resStringId = resStringId;
	}

	public static ArrayList<Integer> getIcons(OsmandApplication app) {
		ArrayList<Integer> list = new ArrayList<>();
		for (ProfileIcons pi : values()) {
			list.add(pi.resId);
		}
		for (RouteActivity activity : RouteActivityHelper.INSTANCE.getActivities()) {
			int categoryIconId = app.getResources().getIdentifier("mx_" + activity.getIconName(), "drawable", app.getPackageName());
			if (categoryIconId != 0) {
				list.add(categoryIconId);
			}
		}
		return list;
	}

	public int getResId() {
		return resId;
	}

	public String getResStringId() {
		return resStringId;
	}

	public static String getResStringByResId(int resId) {
		for (ProfileIcons pi : values()) {
			if (pi.resId == resId) {
				return pi.resStringId;
			}
		}
		return DEFAULT.getResStringId();
	}
}