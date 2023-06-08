package net.osmand.plus.plugins.externalsensors.pointAttributes;


import static net.osmand.plus.plugins.externalsensors.SensorAttributesUtils.SENSOR_TAG_SPEED;

import net.osmand.gpx.PointAttribute;

public class SensorSpeed extends PointAttribute<Float> {

	public SensorSpeed(Float value, float distance, float timeDiff, boolean firstPoint, boolean lastPoint) {
		super(value, distance, timeDiff, firstPoint, lastPoint);
	}

	@Override
	public String getKey() {
		return SENSOR_TAG_SPEED;
	}

	@Override
	public boolean hasValidValue() {
		return value > 0;
	}
}
