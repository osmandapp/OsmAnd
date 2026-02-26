package net.osmand.plus.views.mapwidgets.utils;

import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_KILOMETER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_MILE;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.widgets.AverageSpeedWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkerSideWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class AverageSpeedComputer extends AverageValueComputer {

	private static final boolean CALCULATE_UNIFORM_SPEED = true;

	private final SegmentsList segmentsList;

	private Location previousLocation;
	private long previousTime;

	public AverageSpeedComputer(@NonNull OsmandApplication app) {
		super(app);
		this.segmentsList = new SegmentsList();
	}

	protected boolean isEnabled() {
		ApplicationMode appMode = settings.getApplicationMode();
		MapWidgetRegistry registry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();

		for (MapWidgetInfo widgetInfo : registry.getAllWidgets()) {
			MapWidget widget = widgetInfo.widget;
			boolean usesAverageSpeed = widget instanceof AverageSpeedWidget || widget instanceof MapMarkerSideWidget;
			if (usesAverageSpeed
					&& widgetInfo.isEnabledForAppMode(appMode)
					&& WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetInfo.key, appMode)) {
				return true;
			}
		}
		return false;
	}

	protected void saveLocation(@NonNull Location location, long time) {
		if (CALCULATE_UNIFORM_SPEED) {
			if (location.hasSpeed()) {
				Location loc = new Location(location);
				loc.setTime(time);
				locations.add(loc);
				clearExpiredLocations(locations, BIGGEST_MEASURED_INTERVAL);
			}
		} else if (time - previousTime >= ADD_POINT_INTERVAL_MILLIS) {
			if (previousLocation != null && previousTime > 0) {
				double distance = MapUtils.getDistance(previousLocation, location);
				segmentsList.addSegment(new Segment(distance, previousTime, time));
			}
			previousLocation = location;
			previousTime = time;
		}
	}

	private float getSpeedToSkipInMetersPerSecond() {
		SpeedConstants speedConstant = settings.SPEED_SYSTEM.get();
		return switch (speedConstant) {
			case METERS_PER_SECOND -> 1;
			case KILOMETERS_PER_HOUR, MINUTES_PER_KILOMETER -> 1 / 3.6f;
			case MILES_PER_HOUR, MINUTES_PER_MILE ->
					METERS_IN_ONE_MILE / (3.6f * METERS_IN_KILOMETER);
			case NAUTICALMILES_PER_HOUR ->
					METERS_IN_ONE_NAUTICALMILE / (3.6f * METERS_IN_KILOMETER);
			default -> throw new IllegalStateException("Unsupported speed system");
		};
	}

	/**
	 * @return average speed in meters/second or {@link Float#NaN} if average speed cannot be calculated
	 */
	public float getAverageSpeed(long measuredInterval, boolean skipLowSpeed) {
		if (CALCULATE_UNIFORM_SPEED) {
			return calculateUniformSpeed(measuredInterval, skipLowSpeed);
		} else {
			return calculateNonUniformSpeed(measuredInterval, skipLowSpeed);
		}
	}

	private float calculateUniformSpeed(long measuredInterval, boolean skipLowSpeed) {
		List<Location> locationsToUse = new ArrayList<>(locations);
		clearExpiredLocations(locationsToUse, measuredInterval);

		if (!Algorithms.isEmpty(locationsToUse)) {
			float totalSpeed = 0;
			float speedToSkip = getSpeedToSkipInMetersPerSecond();

			int countedLocations = 0;
			for (Location location : locationsToUse) {
				if (!skipLowSpeed || location.getSpeed() >= speedToSkip) {
					totalSpeed += location.getSpeed();
					countedLocations++;
				}
			}
			return countedLocations != 0 ? totalSpeed / countedLocations : Float.NaN;
		}
		return Float.NaN;
	}

	private float calculateNonUniformSpeed(long measuredInterval, boolean skipLowSpeed) {
		long intervalStart = System.currentTimeMillis() - measuredInterval;
		List<Segment> segments = segmentsList.getSegments(intervalStart);

		double totalDistance = 0;
		double totalTimeMillis = 0;

		float speedToSkip = getSpeedToSkipInMetersPerSecond();

		for (Segment segment : segments) {
			if (!skipLowSpeed || !segment.isLowSpeed(speedToSkip)) {
				totalDistance += segment.distance;
				totalTimeMillis += segment.endTime - segment.startTime;
			}
		}

		return totalTimeMillis == 0 ? Float.NaN : (float) (totalDistance / totalTimeMillis * 1000);
	}

	public static int getConvertedSpeedToSkip(@NonNull SpeedConstants speedSystem) {
		switch (speedSystem) {
			case METERS_PER_SECOND:
			case KILOMETERS_PER_HOUR:
			case MILES_PER_HOUR:
			case NAUTICALMILES_PER_HOUR:
				return 1;
			case MINUTES_PER_KILOMETER:
			case MINUTES_PER_MILE:
				return 60;
			default:
				throw new IllegalStateException("Unsupported speed system");
		}
	}

	public void resetLocations() {
		locations.clear();
	}

	private static class SegmentsList {

		private final Segment[] segments;

		private int tailIndex;
		private int headIndex;

		public SegmentsList() {
			int size = (int) (BIGGEST_MEASURED_INTERVAL / ADD_POINT_INTERVAL_MILLIS) + 1;
			segments = new Segment[size];
		}

		@NonNull
		public List<Segment> getSegments(long fromTimeInclusive) {
			List<Segment> filteredSegments = new ArrayList<>();
			for (int i = tailIndex; i != headIndex; i = nextIndex(i)) {
				Segment segment = segments[i];
				if (segment != null && segment.startTime >= fromTimeInclusive) {
					filteredSegments.add(segment);
				}
			}
			return filteredSegments;
		}

		public void removeSegments(long toTimeExclusive) {
			for (int i = tailIndex; i != headIndex; i = nextIndex(i)) {
				Segment segment = segments[i];
				if (segment != null && segment.startTime < toTimeExclusive) {
					deleteFromTail();
				}
			}
		}

		public void addSegment(@NonNull Segment segment) {
			cleanUpIfOverflowed();
			headIndex = nextIndex(headIndex);
			segments[headIndex] = segment;
		}

		private void cleanUpIfOverflowed() {
			if (nextIndex(headIndex) == tailIndex) {
				deleteFromTail();
			}
		}

		private void deleteFromTail() {
			segments[tailIndex] = null;
			tailIndex = nextIndex(tailIndex);
		}

		private int nextIndex(int index) {
			return (index + 1) % segments.length;
		}
	}

	private static class Segment {

		public final double distance;
		public final long startTime;
		public final long endTime;
		public final float speed;

		public Segment(double distance, long startTime, long endTime) {
			this.distance = distance;
			this.startTime = startTime;
			this.endTime = endTime;
			this.speed = (float) (distance / ((endTime - startTime) / 1000f));
		}

		public boolean isLowSpeed(float speedToSkip) {
			return speed < speedToSkip;
		}
	}
}