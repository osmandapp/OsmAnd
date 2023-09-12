package net.osmand.gpx;

import static net.osmand.gpx.GPXUtilities.POINT_ELEVATION;
import static net.osmand.gpx.GPXUtilities.POINT_SPEED;

public class PointAttributes {

	public static final String SENSOR_TAG_HEART_RATE = "hr";
	public static final String SENSOR_TAG_SPEED = "osmand:speed_sensor";
	public static final String SENSOR_TAG_CADENCE = "cadence";
	public static final String SENSOR_TAG_BIKE_POWER = "power";
	public static final String SENSOR_TAG_TEMPERATURE = "temp";
	public static final String SENSOR_TAG_DISTANCE = "osmand:bike_distance_sensor";

	public float distance;
	public final float timeDiff;
	public final boolean firstPoint;
	public final boolean lastPoint;

	public float speed;
	public float elevation;
	public float heartRate;
	public float sensorSpeed;
	public float bikeCadence;
	public float bikePower;
	public float temperature;

	public PointAttributes(float distance, float timeDiff, boolean firstPoint, boolean lastPoint) {
		this.distance = distance;
		this.timeDiff = timeDiff;
		this.firstPoint = firstPoint;
		this.lastPoint = lastPoint;
	}

	public Float getAttributeValue(String tag) {
		switch (tag) {
			case POINT_SPEED:
				return speed;
			case POINT_ELEVATION:
				return elevation;
			case SENSOR_TAG_HEART_RATE:
				return heartRate;
			case SENSOR_TAG_SPEED:
				return sensorSpeed;
			case SENSOR_TAG_CADENCE:
				return bikeCadence;
			case SENSOR_TAG_BIKE_POWER:
				return bikePower;
			case SENSOR_TAG_TEMPERATURE:
				return temperature;
		}
		return null;
	}

	public void setAttributeValue(String tag, float value) {
		switch (tag) {
			case POINT_SPEED:
				speed = value;
				break;
			case POINT_ELEVATION:
				elevation = value;
				break;
			case SENSOR_TAG_HEART_RATE:
				heartRate = value;
				break;
			case SENSOR_TAG_SPEED:
				sensorSpeed = value;
				break;
			case SENSOR_TAG_CADENCE:
				bikeCadence = value;
				break;
			case SENSOR_TAG_BIKE_POWER:
				bikePower = value;
				break;
			case SENSOR_TAG_TEMPERATURE:
				temperature = value;
				break;
		}
	}

	public boolean hasValidValue(String tag) {
		float value = getAttributeValue(tag);
		if (SENSOR_TAG_TEMPERATURE.equals(tag) || POINT_ELEVATION.equals(tag)) {
			return !Float.isNaN(value);
		}
		return value > 0;
	}
}