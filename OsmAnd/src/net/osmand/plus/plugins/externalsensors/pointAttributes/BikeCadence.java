package net.osmand.plus.plugins.externalsensors.pointAttributes;

import static net.osmand.plus.plugins.externalsensors.SensorAttributesUtils.SENSOR_TAG_CADENCE;

import net.osmand.gpx.PointAttribute;

public class BikeCadence extends PointAttribute<Float> {

	public BikeCadence(Float value, float distance, float timeDiff, boolean firstPoint, boolean lastPoint) {
		super(value, distance, timeDiff, firstPoint, lastPoint);
	}

	@Override
	public String getKey() {
		return SENSOR_TAG_CADENCE;
	}

	@Override
	public boolean hasValidValue() {
		return value > 0;
	}
}
