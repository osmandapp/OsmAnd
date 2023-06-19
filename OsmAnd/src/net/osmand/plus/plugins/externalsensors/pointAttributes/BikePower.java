package net.osmand.plus.plugins.externalsensors.pointAttributes;


import static net.osmand.plus.plugins.externalsensors.SensorAttributesUtils.SENSOR_TAG_BIKE_POWER;

import net.osmand.gpx.PointAttribute;

public class BikePower extends PointAttribute<Float> {

	public BikePower(Float value, float distance, float timeDiff, boolean firstPoint, boolean lastPoint) {
		super(value, distance, timeDiff, firstPoint, lastPoint);
	}

	@Override
	public String getKey() {
		return SENSOR_TAG_BIKE_POWER;
	}

	@Override
	public boolean hasValidValue() {
		return value > 0;
	}
}
