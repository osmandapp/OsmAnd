package net.osmand.plus.views.mapwidgets.utils;

import static net.osmand.plus.views.mapwidgets.utils.GlideUtils.calculateFormattedRatio;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.widgets.GlideAverageWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class AverageGlideComputer extends AverageValueComputer {

	public AverageGlideComputer(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
	protected boolean isEnabled() {
		ApplicationMode appMode = settings.getApplicationMode();
		MapWidgetRegistry registry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();

		for (MapWidgetInfo widgetInfo : registry.getAllWidgets()) {
			MapWidget widget = widgetInfo.widget;
			if (widget instanceof GlideAverageWidget
					&& widgetInfo.isEnabledForAppMode(appMode)
					&& WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetInfo.key, appMode)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void saveLocation(@NonNull Location location, long time) {
		if (location.hasAltitude()) {
			Location loc = new Location(location);
			loc.setTime(time);
			locations.add(loc);
			clearExpiredLocations(locations, BIGGEST_MEASURED_INTERVAL);
		}
	}

	@Nullable
	public String getFormattedAverageGlideRatio(long measuredInterval) {
		List<Location> locationsToUse = new ArrayList<>(locations);
		clearExpiredLocations(locationsToUse, measuredInterval);

		if (!Algorithms.isEmpty(locationsToUse)) {
			double distance = calculateTotalDistance(locationsToUse);
			double difference = calculateAltitudeDifference(locationsToUse);
			return calculateFormattedRatio(app, distance, difference);
		}
		return null;
	}

	private double calculateTotalDistance(@NonNull List<Location> locations) {
		double totalDistance = 0;
		for (int i = 0; i < locations.size() - 1; i++) {
			Location l1 = locations.get(i);
			Location l2 = locations.get(i + 1);
			totalDistance += MapUtils.getDistance(l1, l2);
		}
		return totalDistance;
	}

	private double calculateAltitudeDifference(@NonNull List<Location> locations) {
		int size = locations.size();
		if (size > 1) {
			Location start = locations.get(0);
			Location end = locations.get(size - 1);
			return start.getAltitude() - end.getAltitude();
		}
		return 0;
	}
}
