package net.osmand.plus.profiles;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

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
	MOTORCYCLE(R.drawable.ic_action_motorcycle_dark, R.string.app_mode_motorcycle, "ic_action_motorcycle_dark"),
	BICYCLE(R.drawable.ic_action_bicycle_dark, R.string.app_mode_bicycle, "ic_action_bicycle_dark"),
	HORSE(R.drawable.ic_action_horse, R.string.app_mode_horse, "ic_action_horse"),
	PEDESTRIAN(R.drawable.ic_action_pedestrian_dark, R.string.app_mode_pedestrian, "ic_action_pedestrian_dark"),
	TREKKING(R.drawable.ic_action_trekking_dark, R.string.app_mode_hiking, "ic_action_trekking_dark"),
	SKIING(R.drawable.ic_action_skiing, R.string.app_mode_skiing, "ic_action_skiing"),
	SAIL_BOAT(R.drawable.ic_action_sail_boat_dark, R.string.app_mode_boat, "ic_action_sail_boat_dark"),
	AIRCRAFT(R.drawable.ic_action_aircraft, R.string.app_mode_aircraft, "ic_action_aircraft"),
	HELICOPTER(R.drawable.ic_action_helicopter, R.string.app_mode_helicopter, "ic_action_helicopter"),
	TRANSPORTER(R.drawable.ic_action_personal_transporter, R.string.app_mode_personal_transporter, "ic_action_personal_transporter"),
	MONOWHEEL(R.drawable.ic_action_monowheel, R.string.app_mode_monowheel, "ic_action_monowheel"),
	SCOOTER(R.drawable.ic_action_scooter, R.string.app_mode_scooter, "ic_action_scooter"),
	UFO(R.drawable.ic_action_ufo, R.string.app_mode_ufo, "ic_action_ufo"),
	OFFROAD(R.drawable.ic_action_offroad, R.string.app_mode_offroad, "ic_action_offroad"),
	CAMPERVAN(R.drawable.ic_action_campervan, R.string.app_mode_campervan, "ic_action_campervan"),
	CAMPER(R.drawable.ic_action_camper, R.string.app_mode_camper, "ic_action_camper"),
	PICKUP_TRUCK(R.drawable.ic_action_pickup_truck, R.string.app_mode_pickup_truck, "ic_action_pickup_truck"),
	WAGON(R.drawable.ic_action_wagon, R.string.app_mode_wagon, "ic_action_wagon"),
	UTV(R.drawable.ic_action_utv, R.string.app_mode_utv, "ic_action_utv"),
	OSM(R.drawable.ic_action_openstreetmap_logo, R.string.app_mode_osm, "ic_action_openstreetmap_logo");

	@DrawableRes
	private int resId;
	@StringRes
	private int titleId;
	private String resStringId;

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