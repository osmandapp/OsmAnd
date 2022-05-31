package net.osmand.plus.views.mapwidgets;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.SpeedConstants;
import net.osmand.plus.views.mapwidgets.widgets.AverageSpeedWidget;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_KILOMETER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_MILE;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;

public class AverageSpeedComputer {

	private static final long ADD_POINT_INTERVAL_MILLIS = 1000;
	private static final long MISSING_POINT_TIME_THRESHOLD = 5000;

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

	private final OsmandApplication app;
	private final OsmandSettings settings;

	private final SegmentsList segmentsList;

	private LatLon previousLatLon = null;
	private long previousTime = 0;
	private boolean previousPointLowSpeed = false;

	public AverageSpeedComputer(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.segmentsList = new SegmentsList();
	}

	public void updateLocation(@Nullable Location location) {
		long time = System.currentTimeMillis();

		boolean recordPaused = previousTime > 0 && time - previousTime > MISSING_POINT_TIME_THRESHOLD;
		if (recordPaused) {
			segmentsList.clear();
			previousLatLon = null;
			previousTime = 0;
			previousPointLowSpeed = false;
		}

		boolean save = location != null
				&& isEnabled()
				&& OsmAndLocationProvider.isNotSimulatedLocation(location)
				&& time - previousTime >= ADD_POINT_INTERVAL_MILLIS;
		if (save) {
			saveLocation(location, time);
		}
	}

	private boolean isEnabled() {
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		ApplicationMode appMode = settings.getApplicationMode();
		int filterModes = MapWidgetRegistry.AVAILABLE_MODE | MapWidgetRegistry.ENABLED_MODE;
		Set<MapWidgetInfo> sideWidgets = widgetRegistry.getWidgetsForPanel(appMode, filterModes, WidgetsPanel.getSidePanels());

		for (MapWidgetInfo widgetInfo : sideWidgets) {
			if (widgetInfo.widget instanceof AverageSpeedWidget) {
				return true;
			}
		}

		return false;
	}

	private void saveLocation(@NonNull Location location, long time) {
		LatLon latLon = new LatLon(location.getLatitude(), location.getLongitude());
		boolean lowSpeed = !location.hasSpeed() || location.getSpeed() < getSpeedToSkipInMetersPerSecond();

		if (previousLatLon != null && previousTime > 0) {
			double distance = MapUtils.getDistance(previousLatLon, latLon);
			segmentsList.addSegment(new Segment(distance, previousTime, time, previousPointLowSpeed || lowSpeed));
		}

		previousLatLon = latLon;
		previousTime = time;
		previousPointLowSpeed = lowSpeed;
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
	public float getAverageSpeed() {
		long measuredInterval = settings.AVERAGE_SPEED_MEASURED_INTERVAL_MILLIS.get();
		long intervalStart = System.currentTimeMillis() - measuredInterval;
		List<Segment> segments = segmentsList.getSegments(intervalStart, MISSING_POINT_TIME_THRESHOLD);

		boolean skipLowSpeed = settings.AVERAGE_SPEED_SKIP_STOPS.get();
		double totalDistance = 0;
		double totalTimeMillis = 0;

		for (Segment segment : segments) {
			if (!segment.lowSpeed || !skipLowSpeed) {
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

		private int tailIndex = 0;
		private int headIndex = 0;

		public SegmentsList() {
			int size = (int) (MEASURED_INTERVALS.get(MEASURED_INTERVALS.size() - 1) / ADD_POINT_INTERVAL_MILLIS) + 1;
			segments = new Segment[size];
		}

		@NonNull
		public List<Segment> getSegments(long fromTimeInclusive, long allowedStartTimeGap) {
			List<Segment> filteredSegments = new ArrayList<>();
			for (int i = tailIndex; i != headIndex; i = nextIndex(i)) {
				Segment segment = segments[i];
				if (segment == null || segment.startTime < fromTimeInclusive) {
					continue;
				}

				boolean firstSegment = filteredSegments.size() == 0;
				boolean hugeGap = fromTimeInclusive + allowedStartTimeGap < segment.startTime;
				if (firstSegment && hugeGap) {
					return Collections.emptyList();
				}

				filteredSegments.add(segment);
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

		public void clear() {
			Arrays.fill(segments, null);
			tailIndex = 0;
			headIndex = 0;
		}

		private int nextIndex(int index) {
			return (index + 1) % segments.length;
		}
	}

	private static class Segment {

		public final double distance;
		public final long startTime;
		public final long endTime;
		public final boolean lowSpeed;

		public Segment(double distance, long startTime, long endTime, boolean lowSpeed) {
			this.distance = distance;
			this.startTime = startTime;
			this.endTime = endTime;
			this.lowSpeed = lowSpeed;
		}
	}
}