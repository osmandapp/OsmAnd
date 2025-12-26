package net.osmand.plus.settings.vehiclespecs;

import static net.osmand.router.GeneralRouter.MAX_AXLE_LOAD;
import static net.osmand.router.GeneralRouter.VEHICLE_HEIGHT;
import static net.osmand.router.GeneralRouter.VEHICLE_LENGTH;
import static net.osmand.router.GeneralRouter.VEHICLE_WEIGHT;
import static net.osmand.router.GeneralRouter.VEHICLE_WIDTH;
import static net.osmand.router.GeneralRouter.WEIGHT_RATING;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.CollectionUtils;

public enum SpecificationType {

	WIDTH(VEHICLE_WIDTH),
	HEIGHT(VEHICLE_HEIGHT),
	LENGTH(VEHICLE_LENGTH),
	WEIGHT(VEHICLE_WEIGHT),
	AXLE_LOAD(MAX_AXLE_LOAD),
	WEIGHT_FULL_LOAD(WEIGHT_RATING);

	private final String key;

	SpecificationType(@NonNull String key) {
		this.key = key;
	}

	@Nullable
	public static SpecificationType getByKey(@NonNull String key) {
		for (SpecificationType type : values()) {
			if (key.equals(type.key)) {
				return type;
			}
		}
		return null;
	}

	public boolean isWeightRelated() {
		return CollectionUtils.equalsToAny(this, WEIGHT, AXLE_LOAD, WEIGHT_FULL_LOAD);
	}
}
