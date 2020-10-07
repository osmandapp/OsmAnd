package net.osmand.plus.settings.bottomsheets;

import net.osmand.plus.R;

import static net.osmand.router.GeneralRouter.*;

public enum VehicleSizeAssets {
	BOAT_HEIGHT(VEHICLE_HEIGHT, GeneralRouterProfile.BOAT, R.drawable.img_help_vessel_height_day,
			R.drawable.img_help_vessel_height_night,
			R.string.vessel_height_limit_description, R.string.shared_string_meters, R.string.m),
	BOAT_WIDTH(VEHICLE_WIDTH, GeneralRouterProfile.BOAT, R.drawable.img_help_vessel_width_day,
			R.drawable.img_help_vessel_width_night,
			R.string.vessel_width_limit_description, R.string.shared_string_meters, R.string.m),
	WIDTH(VEHICLE_WIDTH, GeneralRouterProfile.CAR, R.drawable.img_help_width_limit_day,
			R.drawable.img_help_width_limit_night,
			R.string.width_limit_description, R.string.shared_string_meters, R.string.m),
	HEIGHT(VEHICLE_HEIGHT, GeneralRouterProfile.CAR, R.drawable.img_help_height_limit_day,
			R.drawable.img_help_height_limit_night,
			R.string.height_limit_description, R.string.shared_string_meters, R.string.m),
	WEIGHT(VEHICLE_WEIGHT, GeneralRouterProfile.CAR, R.drawable.img_help_weight_limit_day,
			R.drawable.img_help_weight_limit_night,
			R.string.weight_limit_description, R.string.shared_string_tones, R.string.metric_ton),
	LENGTH(VEHICLE_LENGTH, GeneralRouterProfile.CAR, R.drawable.img_help_length_limit_day,
			R.drawable.img_help_length_limit_night,
			R.string.lenght_limit_description, R.string.shared_string_meters, R.string.m);

	String routerParameterName;
	GeneralRouterProfile routerProfile;
	int dayIconId;
	int nightIconId;
	int descriptionRes;
	int metricRes;
	int metricShortRes;

	VehicleSizeAssets(String routerParameterName, GeneralRouterProfile routerProfile, int dayIconId, int nightIconId,
	                  int descriptionRes, int metricRes, int metricShortRes) {
		this.routerParameterName = routerParameterName;
		this.routerProfile = routerProfile;
		this.dayIconId = dayIconId;
		this.nightIconId = nightIconId;
		this.descriptionRes = descriptionRes;
		this.metricRes = metricRes;
		this.metricShortRes = metricShortRes;
	}

	public static VehicleSizeAssets getAssets(String parameterName, GeneralRouterProfile routerProfile) {
		for (VehicleSizeAssets type : VehicleSizeAssets.values()) {
			if (type.routerParameterName.equals(parameterName) && type.routerProfile == routerProfile) {
				return type;
			}
		}
		return null;
	}

	public int getDayIconId() {
		return dayIconId;
	}

	public int getNightIconId() {
		return nightIconId;
	}

	public int getDescriptionRes() {
		return descriptionRes;
	}

	public int getMetricRes() {
		return metricRes;
	}

	public int getMetricShortRes() {
		return metricShortRes;
	}
}
