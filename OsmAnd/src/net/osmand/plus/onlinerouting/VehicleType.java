package net.osmand.plus.onlinerouting;

import android.content.Context;

import net.osmand.plus.R;
import net.osmand.util.Algorithms;

public enum VehicleType {
	CAR("car", R.string.routing_engine_vehicle_type_car),
	BIKE("bike", R.string.routing_engine_vehicle_type_bike),
	FOOT("foot", R.string.routing_engine_vehicle_type_foot),
	DRIVING("driving", R.string.routing_engine_vehicle_type_driving),
	CUSTOM("-", R.string.shared_string_custom);

	VehicleType(String key, int titleId) {
		this.key = key;
		this.titleId = titleId;
	}

	private String key;
	private int titleId;

	public String getKey() {
		return key;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(titleId);
	}

	public static VehicleType getVehicleByKey(String key) {
		for (VehicleType v : values()) {
			if (Algorithms.objectEquals(v.getKey(), key)) {
				return v;
			}
		}
		return CUSTOM;
	}
}