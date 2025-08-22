package net.osmand.plus.settings.vehiclesize;

import static net.osmand.router.GeneralRouter.MAX_AXLE_LOAD;
import static net.osmand.router.GeneralRouter.VEHICLE_HEIGHT;
import static net.osmand.router.GeneralRouter.VEHICLE_LENGTH;
import static net.osmand.router.GeneralRouter.VEHICLE_WEIGHT;
import static net.osmand.router.GeneralRouter.VEHICLE_WIDTH;
import static net.osmand.router.GeneralRouter.WEIGHT_RATING;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum SizeType {

	WIDTH(VEHICLE_WIDTH),
	HEIGHT(VEHICLE_HEIGHT),
	LENGTH(VEHICLE_LENGTH),
	WEIGHT(VEHICLE_WEIGHT),
	AXLE_LOAD(MAX_AXLE_LOAD),
	WEIGHT_FULL_LOAD(WEIGHT_RATING);

	private String key;

	SizeType(String key) {
		this.key = key;
	}

	@Nullable
	public static SizeType getByKey(@NonNull String key) {
		for (SizeType type : values()) {
			if (key.equals(type.key)) {
				return type;
			}
		}
		return null;
	}

	public boolean isWeightType () {
		return this == SizeType.WEIGHT
				|| this == SizeType.AXLE_LOAD
				|| this == SizeType.WEIGHT_FULL_LOAD;
	}
}
