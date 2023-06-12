package net.osmand.plus.plugins.externalsensors.pointAttributes;


import static net.osmand.plus.plugins.externalsensors.SensorAttributesUtils.SENSOR_TAG_HEART_RATE;

import net.osmand.gpx.PointAttribute;

public class HeartRate extends PointAttribute<Integer> {

	public HeartRate(Integer value, float distance, float timeDiff, boolean firstPoint, boolean lastPoint) {
		super(value, distance, timeDiff, firstPoint, lastPoint);
	}

	@Override
	public String getKey() {
		return SENSOR_TAG_HEART_RATE;
	}

	@Override
	public boolean hasValidValue() {
		return value > 0;
	}
}
