package net.osmand.plus.settings.bottomsheets;

import net.osmand.plus.R;
import net.osmand.router.GeneralRouter;

public enum VehicleSizeAssets {
	WIDTH(GeneralRouter.VEHICLE_WIDTH, R.drawable.img_help_width_limit_day, R.drawable.img_help_width_limit_night,
			R.string.width_limit_description, R.string.shared_string_meters, R.string.m),
	HEIGHT(GeneralRouter.VEHICLE_HEIGHT, R.drawable.img_help_height_limit_day, R.drawable.img_help_height_limit_night,
			R.string.height_limit_description, R.string.shared_string_meters, R.string.m),
	WEIGHT(GeneralRouter.VEHICLE_WEIGHT, R.drawable.img_help_weight_limit_day, R.drawable.img_help_weight_limit_night,
			R.string.weight_limit_description, R.string.shared_string_tones, R.string.metric_ton);

	String routerParameterName;
	int dayIconId;
	int nightIconId;
	int descriptionRes;
	int metricRes;
	int metricShortRes;

	VehicleSizeAssets(String routerParameterName, int dayIconId, int nightIconId, int descriptionRes, int metricRes,
	                  int metricShortRes) {
		this.routerParameterName = routerParameterName;
		this.dayIconId = dayIconId;
		this.nightIconId = nightIconId;
		this.descriptionRes = descriptionRes;
		this.metricRes = metricRes;
		this.metricShortRes = metricShortRes;
	}

	public static VehicleSizeAssets getAssets(String parameterName) {
		for (VehicleSizeAssets type : VehicleSizeAssets.values()) {
			if (type.routerParameterName.equals(parameterName)) {
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
