package net.osmand.plus.plugins.externalsensors.pointAttributes;

import static net.osmand.plus.plugins.externalsensors.SensorAttributesUtils.SENSOR_TAG_TEMPERATURE;

import net.osmand.gpx.PointAttribute;

public class Temperature extends PointAttribute<Float> {

	public Temperature(Float value, float distance, float timeDiff, boolean firstPoint, boolean lastPoint) {
		super(value, distance, timeDiff, firstPoint, lastPoint);
	}

	@Override
	public String getKey() {
		return SENSOR_TAG_TEMPERATURE;
	}

	@Override
	public boolean hasValidValue() {
		return !Float.isNaN(value);
	}
}
