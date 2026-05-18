package net.osmand.plus.settings.vehiclesize;

import static net.osmand.router.GeneralRouter.VEHICLE_HEIGHT;
import static net.osmand.router.GeneralRouter.VEHICLE_LENGTH;
import static net.osmand.router.GeneralRouter.VEHICLE_WEIGHT;
import static net.osmand.router.GeneralRouter.VEHICLE_WIDTH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum SizeType {

	WIDTH(VEHICLE_WIDTH),
	HEIGHT(VEHICLE_HEIGHT),
	LENGTH(VEHICLE_LENGTH),
	WEIGHT(VEHICLE_WEIGHT);

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
}
