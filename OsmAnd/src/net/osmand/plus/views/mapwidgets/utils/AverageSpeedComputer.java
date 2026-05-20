package net.osmand.plus.views.mapwidgets.utils;

import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_KILOMETER;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_MILE;
import static net.osmand.plus.utils.OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;
import static net.osmand.plus.views.mapwidgets.WidgetType.AVERAGE_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_1;
import static net.osmand.plus.views.mapwidgets.WidgetType.SIDE_MARKER_2;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.shared.settings.enums.SpeedConstants;

import java.util.ArrayList;
import java.util.List;

public class AverageSpeedComputer extends AverageValueComputer {

	private final List<MapWidgetInfo> widgetInfos = new ArrayList<>();

	public AverageSpeedComputer(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
	protected boolean isEnabled() {
		MapLayers mapLayers = app.getOsmandMap().getMapLayers();
		MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
		if (mapInfoLayer == null) {
			return false;
		}
		MapActivity activity = mapInfoLayer.getMapActivity();
		if (activity == null) {
			return false;
		}
		ApplicationMode appMode = settings.getApplicationMode();
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);
		MapWidgetRegistry widgetRegistry = mapLayers.getMapWidgetRegistry();

		widgetInfos.clear();
		widgetRegistry.collectWidgetsInfo(widgetInfos, appMode, layoutMode, null, null, true);

		for (int i = 0; i < widgetInfos.size(); i++) {
			MapWidgetInfo widgetInfo = widgetInfos.get(i);
			WidgetType type = widgetInfo.getWidgetType();

			if (type == AVERAGE_SPEED || type == SIDE_MARKER_1 || type == SIDE_MARKER_2) {
				if (WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetInfo.key, appMode)) {
					return true;
				}
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
		return switch (speedSystem) {
			case METERS_PER_SECOND, KILOMETERS_PER_HOUR, MILES_PER_HOUR, NAUTICALMILES_PER_HOUR -> 1;
			case MINUTES_PER_KILOMETER, MINUTES_PER_MILE -> 60;
			default -> throw new IllegalStateException("Unsupported speed system");
		};
	}
}