package net.osmand.plus.profiles;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

import java.util.ArrayList;

public enum ProfileIcons {
	DEFAULT(R.drawable.ic_world_globe_dark, R.string.app_mode_default, "ic_world_globe_dark"),
	CAR(R.drawable.ic_action_car_dark, R.string.app_mode_car, "ic_action_car_dark"),
	TAXI(R.drawable.ic_action_taxi, R.string.app_mode_taxi, "ic_action_taxi"),
	TRUCK(R.drawable.ic_action_truck_dark, R.string.app_mode_truck, "ic_action_truck_dark"),
	SHUTTLE_BUS(R.drawable.ic_action_shuttle_bus, R.string.app_mode_shuttle_bus, "ic_action_shuttle_bus"),
	BUS(R.drawable.ic_action_bus_dark, R.string.app_mode_bus, "ic_action_bus_dark"),
	SUBWAY(R.drawable.ic_action_subway, R.string.app_mode_subway, "ic_action_subway"),
	TRAIN(R.drawable.ic_action_train, R.string.app_mode_train, "ic_action_train"),
	MOTORCYCLE(R.drawable.ic_action_motorcycle_dark, R.string.app_mode_motorcycle, "ic_action_motorcycle_dark"),
	ENDURO_MOTORCYCLE(R.drawable.ic_action_enduro_motorcycle, R.string.app_mode_enduro_motorcycle, "ic_action_enduro_motorcycle"),
	MOTOR_SCOOTER(R.drawable.ic_action_motor_scooter, R.string.app_mode_motor_scooter, "ic_action_motor_scooter"),
	BICYCLE(R.drawable.ic_action_bicycle_dark, R.string.app_mode_bicycle, "ic_action_bicycle_dark"),
	MOUNTAIN_BICYCLE(R.drawable.ic_action_mountain_bike, R.string.app_mode_mountain_bicycle, "ic_action_mountain_bike"),
	HORSE(R.drawable.ic_action_horse, R.string.app_mode_horse, "ic_action_horse"),
	PEDESTRIAN(R.drawable.ic_action_pedestrian_dark, R.string.app_mode_pedestrian, "ic_action_pedestrian_dark"),
	TREKKING(R.drawable.ic_action_trekking_dark, R.string.app_mode_hiking, "ic_action_trekking_dark"),
	CLIMBING(R.drawable.ic_action_hill_climbing, R.string.app_mode_climbing, "ic_action_hill_climbing"),
	SKIING(R.drawable.ic_action_skiing, R.string.app_mode_skiing, "ic_action_skiing"),
	SAIL_BOAT(R.drawable.ic_action_sail_boat_dark, R.string.app_mode_boat, "ic_action_sail_boat_dark"),
	AIRCRAFT(R.drawable.ic_action_aircraft, R.string.app_mode_aircraft, "ic_action_aircraft"),
	HELICOPTER(R.drawable.ic_action_helicopter, R.string.app_mode_helicopter, "ic_action_helicopter"),
	PARAGLIDING(R.drawable.ic_action_paragliding, R.string.app_mode_paragliding, "ic_action_paragliding"),
	HANG_GLIDING(R.drawable.ic_action_hang_gliding, R.string.app_mode_hang_gliding, "ic_action_hang_gliding"),
	TRANSPORTER(R.drawable.ic_action_personal_transporter, R.string.app_mode_personal_transporter, "ic_action_personal_transporter"),
	MONOWHEEL(R.drawable.ic_action_monowheel, R.string.app_mode_monowheel, "ic_action_monowheel"),
	SCOOTER(R.drawable.ic_action_scooter, R.string.app_mode_scooter, "ic_action_scooter"),
	INLINE_SKATES(R.drawable.ic_action_inline_skates, R.string.app_mode_inline_skates, "ic_action_inline_skates"),
	WHEELCHAIR(R.drawable.ic_action_wheelchair, R.string.app_mode_wheelchair, "ic_action_wheelchair"),
	WHEELCHAIR_FORWARD(R.drawable.ic_action_wheelchair_forward, R.string.app_mode_wheelchair_forward, "ic_action_wheelchair_forward"),
	UFO(R.drawable.ic_action_ufo, R.string.app_mode_ufo, "ic_action_ufo"),
	BABY_TRANSPORT(R.drawable.ic_action_baby_transport, R.string.app_mode_baby_transport, "ic_action_baby_transport"),
	OFFROAD(R.drawable.ic_action_offroad, R.string.app_mode_offroad, "ic_action_offroad"),
	SUV(R.drawable.ic_action_suv, R.string.app_mode_suv, "ic_action_suv"),
	CAMPERVAN(R.drawable.ic_action_campervan, R.string.app_mode_campervan, "ic_action_campervan"),
	CAMPER(R.drawable.ic_action_camper, R.string.app_mode_camper, "ic_action_camper"),
	PICKUP_TRUCK(R.drawable.ic_action_pickup_truck, R.string.app_mode_pickup_truck, "ic_action_pickup_truck"),
	WAGON(R.drawable.ic_action_wagon, R.string.app_mode_wagon, "ic_action_wagon"),
	UTV(R.drawable.ic_action_ski_touring, R.string.app_mode_utv, "ic_action_ski_touring"),
	SKI_TOURING(R.drawable.ic_action_utv, R.string.app_mode_ski_touring, "ic_action_utv"),
	SNOWMOBILE(R.drawable.ic_action_snowmobile, R.string.app_mode_ski_snowmobile, "ic_action_snowmobile"),
	GO_CART(R.drawable.ic_action_go_cart, R.string.app_mode_go_cart, "ic_action_go_cart"),
	OSM(R.drawable.ic_action_openstreetmap_logo, R.string.app_mode_osm, "ic_action_openstreetmap_logo"),
	MOTORBOAT(R.drawable.ic_action_motorboat, R.string.app_mode_motorboat, "ic_action_motorboat"),
	KAYAK(R.drawable.ic_action_kayak, R.string.app_mode_kayak, "ic_action_kayak"),
	LIGHT_AIRCRAFT(R.drawable.ic_action_light_aircraft, R.string.app_mode_light_aircraft, "ic_action_light_aircraft");

	@DrawableRes
	private final int resId;
	@StringRes
	private final int titleId;
	private final String resStringId;

	ProfileIcons(@DrawableRes int resId, @StringRes int titleId, @NonNull String resStringId) {
		this.resId = resId;
		this.titleId = titleId;
		this.resStringId = resStringId;
	}

	public static ArrayList<Integer> getIcons() {
		ArrayList<Integer> list = new ArrayList<>();
		for (ProfileIcons pi : values()) {
			list.add(pi.resId);
		}
		return list;
	}

	public int getResId() {
		return resId;
	}

	public int getTitleId() {
		return titleId;
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