package net.osmand.gpx;

import static net.osmand.gpx.GPXUtilities.POINT_ELEVATION;
import static net.osmand.gpx.GPXUtilities.POINT_SPEED;

public abstract class PointAttribute<T extends Number> {

	public final T value;
	public final float distance;
	public final float timeDiff;

	private boolean firstPoint = false;
	private boolean lastPoint = false;

	public PointAttribute(T value, float distance, float timeDiff) {
		this.value = value;
		this.distance = distance;
		this.timeDiff = timeDiff;
	}

	public abstract String getKey();

	public abstract boolean hasValidValue();

	public boolean isFirstPoint() {
		return firstPoint;
	}

	public void setFirstPoint(boolean firstPoint) {
		this.firstPoint = firstPoint;
	}

	public boolean isLastPoint() {
		return lastPoint;
	}

	public void setLastPoint(boolean lastPoint) {
		this.lastPoint = lastPoint;
	}

	public static class Elevation extends PointAttribute<Float> {

		public Elevation(Float value, float distance, float timeDiff) {
			super(value, distance, timeDiff);
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

		public Speed(Float value, float distance, float timeDiff) {
			super(value, distance, timeDiff);
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