package net.osmand.plus.views.mapwidgets;

import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_KILOMETER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_MILE;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.views.mapwidgets.widgets.AverageSpeedWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkerSideWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class AverageSpeedComputer {

	private static final long ADD_POINT_INTERVAL_MILLIS = 1000;
	private static final boolean CALCULATE_UNIFORM_SPEED = true;

	public static final List<Long> MEASURED_INTERVALS;
	public static final long DEFAULT_INTERVAL_MILLIS = 30 * 60 * 1000L;

	static {
		List<Long> modifiableIntervals = new ArrayList<>();
		modifiableIntervals.add(15 * 1000L);
		modifiableIntervals.add(30 * 1000L);
		modifiableIntervals.add(45 * 1000L);
		for (int i = 1; i <= 60; i++) {
			modifiableIntervals.add(i * 60 * 1000L);
		}
		MEASURED_INTERVALS = Collections.unmodifiableList(modifiableIntervals);
	}

	private static final long BIGGEST_MEASURED_INTERVAL = MEASURED_INTERVALS.get(MEASURED_INTERVALS.size() - 1);

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final SegmentsList segmentsList;
	private final List<Location> locations = new LinkedList<>();

	private Location previousLocation;
	private long previousTime;

	public AverageSpeedComputer(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.segmentsList = new SegmentsList();
	}

	public void updateLocation(@Nullable Location location) {
		if (location != null) {
			long time = System.currentTimeMillis();
			boolean save = isEnabled() && OsmAndLocationProvider.isNotSimulatedLocation(location);
			if (save) {
				saveLocation(location, time);
			}
		}
	}

	private boolean isEnabled() {
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		ApplicationMode appMode = settings.getApplicationMode();
		List<MapWidgetInfo> widgetInfos = widgetRegistry.getAllWidgets();

		for (MapWidgetInfo widgetInfo : widgetInfos) {
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

	private void saveLocation(@NonNull Location location, long time) {
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

	private void clearExpiredLocations(@NonNull List<Location> locations, long measuredInterval) {
		long expirationTime = System.currentTimeMillis() - measuredInterval;
		for (Iterator<Location> iterator = locations.iterator(); iterator.hasNext(); ) {
			Location location = iterator.next();
			if (location.getTime() < expirationTime) {
				iterator.remove();
			} else {
				break;
			}
		}
	}

	private float getSpeedToSkipInMetersPerSecond() {
		SpeedConstants speedConstant = settings.SPEED_SYSTEM.get();
		switch (speedConstant) {
			case METERS_PER_SECOND:
				return 1;
			case KILOMETERS_PER_HOUR:
			case MINUTES_PER_KILOMETER:
				return 1 / 3.6f;
			case MILES_PER_HOUR:
			case MINUTES_PER_MILE:
				return METERS_IN_ONE_MILE / (3.6f * METERS_IN_KILOMETER);
			case NAUTICALMILES_PER_HOUR:
				return METERS_IN_ONE_NAUTICALMILE / (3.6f * METERS_IN_KILOMETER);
			default:
				throw new IllegalStateException("Unsupported speed system");
		}
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