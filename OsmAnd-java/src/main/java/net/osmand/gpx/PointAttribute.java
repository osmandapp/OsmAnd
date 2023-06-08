package net.osmand.gpx;

import static net.osmand.gpx.GPXUtilities.POINT_ELEVATION;
import static net.osmand.gpx.GPXUtilities.POINT_SPEED;

public abstract class PointAttribute<T extends Number> {

	public final T value;
	public float distance;
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
}