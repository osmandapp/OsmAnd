package net.osmand.gpx;

import static net.osmand.gpx.GPXUtilities.GPXTPX_PREFIX;
import static net.osmand.gpx.GPXUtilities.OSMAND_EXTENSIONS_PREFIX;
import static net.osmand.gpx.GPXUtilities.POINT_ELEVATION;
import static net.osmand.gpx.GPXUtilities.POINT_SPEED;
import static net.osmand.util.CollectionUtils.equalsToAny;

public class PointAttributes {

	public static final String SENSOR_TAG_HEART_RATE = GPXTPX_PREFIX + "hr";
	public static final String SENSOR_TAG_SPEED = OSMAND_EXTENSIONS_PREFIX + "speed_sensor";
	public static final String SENSOR_TAG_CADENCE = GPXTPX_PREFIX + "cad" ;
	public static final String SENSOR_TAG_BIKE_POWER = GPXTPX_PREFIX + "power";
	public static final String SENSOR_TAG_TEMPERATURE = "temp_sensor";
	public static final String SENSOR_TAG_TEMPERATURE_W = GPXTPX_PREFIX + "wtemp";
	public static final String SENSOR_TAG_TEMPERATURE_A = GPXTPX_PREFIX + "atemp";
	public static final String SENSOR_TAG_DISTANCE = OSMAND_EXTENSIONS_PREFIX + "bike_distance_sensor";

	public static final String DEV_RAW_ZOOM = "raw_zoom";
	public static final String DEV_ANIMATED_ZOOM = "animated_zoom";
	public static final String DEV_INTERPOLATION_OFFSET_N = "offset";

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
	public float waterTemperature;
	public float airTemperature;

	public float rawZoom;
	public float animatedZoom;
	public float interpolationOffsetN;

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
				return getTemperature();
			case SENSOR_TAG_TEMPERATURE_W:
				return waterTemperature;
			case SENSOR_TAG_TEMPERATURE_A:
				return airTemperature;
			case DEV_RAW_ZOOM:
				return rawZoom;
			case DEV_ANIMATED_ZOOM:
				return animatedZoom;
			case DEV_INTERPOLATION_OFFSET_N:
				return interpolationOffsetN;
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
			case SENSOR_TAG_TEMPERATURE_W:
				waterTemperature = value;
				break;
			case SENSOR_TAG_TEMPERATURE_A:
				airTemperature = value;
				break;
			case DEV_RAW_ZOOM:
				rawZoom = value;
				break;
			case DEV_ANIMATED_ZOOM:
				animatedZoom = value;
				break;
			case DEV_INTERPOLATION_OFFSET_N:
				interpolationOffsetN = value;
				break;
		}
	}

	public float getTemperature() {
		if (!Float.isNaN(airTemperature)) {
			return !Float.isNaN(waterTemperature) ? Math.max(waterTemperature, airTemperature) : airTemperature;
		}
		return waterTemperature;
	}

	public boolean hasValidValue(String tag) {
		float value = getAttributeValue(tag);
		if (equalsToAny(tag, SENSOR_TAG_TEMPERATURE, SENSOR_TAG_TEMPERATURE_W, SENSOR_TAG_TEMPERATURE_A, POINT_ELEVATION)) {
			return !Float.isNaN(value);
		}
		return value > 0;
	}
}