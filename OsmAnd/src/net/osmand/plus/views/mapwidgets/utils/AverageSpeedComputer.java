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

import java.util.Iterator;
import java.util.Queue;

public class AverageSpeedComputer extends AverageValueComputer {

	public AverageSpeedComputer(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
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

	@Override
	protected void saveLocation(@NonNull Location location, long time) {
		if (location.hasSpeed()) {
			Location loc = new Location(location);
			loc.setTime(time);
			locations.add(loc);
			clearExpiredLocations(BIGGEST_MEASURED_INTERVAL);
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

	public float getAverageSpeed(long startTimestamp, long measuredInterval, boolean skipLowSpeed) {
		return calculateUniformSpeed(startTimestamp, measuredInterval, skipLowSpeed);
	}

	private float calculateUniformSpeed(long startTimestamp, long measuredInterval, boolean skipLowSpeed) {
		long now = System.currentTimeMillis();
		float totalSpeed = 0;
		int countedLocations = 0;
		float speedToSkip = getSpeedToSkipInMetersPerSecond();

		// Iterate over the concurrent queue
		for (Location location : locations) {
			long locationTime = location.getTime();

			// Check if the location is within the measured interval and after the start timestamp
			if (locationTime >= startTimestamp && now - locationTime <= measuredInterval) {
				if (!skipLowSpeed || location.getSpeed() >= speedToSkip) {
					totalSpeed += location.getSpeed();
					countedLocations++;
				}
			}
		}
		return countedLocations != 0 ? totalSpeed / countedLocations : Float.NaN;
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
}