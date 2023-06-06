package net.osmand.gpx;

import static net.osmand.gpx.GPXUtilities.POINT_ELEVATION;
import static net.osmand.gpx.GPXUtilities.POINT_SPEED;
import static net.osmand.gpx.GPXUtilities.SENSOR_TAG_CADENCE;
import static net.osmand.gpx.GPXUtilities.SENSOR_TAG_BIKE_POWER;
import static net.osmand.gpx.GPXUtilities.SENSOR_TAG_HEART_RATE;
import static net.osmand.gpx.GPXUtilities.SENSOR_TAG_SPEED;
import static net.osmand.gpx.GPXUtilities.SENSOR_TAG_TEMPERATURE;

public abstract class PointAttribute<T extends Number> {

	public final T value;
	public final float distance;
	public final float timeDiff;
	public final boolean firstPoint;
	public final boolean lastPoint;

	public PointAttribute(T value, float distance, float timeDiff, boolean firstPoint, boolean lastPoint) {
		this.value = value;
		this.distance = distance;
		this.timeDiff = timeDiff;
		this.firstPoint = firstPoint;
		this.lastPoint = lastPoint;
	}

	public abstract String getKey();

	public abstract boolean hasValidValue();


	public static class Elevation extends PointAttribute<Float> {

		public Elevation(Float value, float distance, float timeDiff, boolean firstPoint, boolean lastPoint) {
			super(value, distance, timeDiff, firstPoint, lastPoint);
		}

		@Override
		public String getKey() {
			return POINT_ELEVATION;
		}

		@Override
		public boolean hasValidValue() {
			return !Float.isNaN(value);
		}
	}

	public static class Speed extends PointAttribute<Float> {

		public Speed(Float value, float distance, float timeDiff, boolean firstPoint, boolean lastPoint) {
			super(value, distance, timeDiff, firstPoint, lastPoint);
		}

		@Override
		public String getKey() {
			return POINT_SPEED;
		}

		@Override
		public boolean hasValidValue() {
			return value > 0;
		}
	}

	public static class HeartRate extends PointAttribute<Integer> {

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

	public static class SensorSpeed extends PointAttribute<Float> {

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

	public static class BikeCadence extends PointAttribute<Float> {

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

	public static class BikePower extends PointAttribute<Float> {

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

	public static class Temperature extends PointAttribute<Float> {

		public Temperature(Float value, float distance, float timeDiff, boolean firstPoint, boolean lastPoint) {
			super(value, distance, timeDiff, firstPoint, lastPoint);
		}

		@Override
		public String getKey() {
			return SENSOR_TAG_TEMPERATURE;
		}

		@Override
		public boolean hasValidValue() {
			return value > 0;
		}
	}
}