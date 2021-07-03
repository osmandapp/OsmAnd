package net.osmand.plus.routing;

import net.osmand.plus.OsmandApplication;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ColoringTypeAvailabilityCache {

	private final OsmandApplication app;
	private final Map<RouteColoringType, Boolean> cache = new HashMap<>();

	private RouteCalculationResult prevRoute = null;

	public ColoringTypeAvailabilityCache(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public boolean isColoringAvailable(@NonNull RouteColoringType routeColoringType,
	                                   @Nullable String routeInfoAttribute) {
		RouteCalculationResult currRoute = app.getRoutingHelper().getRoute();
		if (!currRoute.equals(prevRoute)) {
			cache.clear();
			prevRoute = currRoute;
		}
		Boolean available = cache.get(routeColoringType);
		if (available == null) {
			available = routeColoringType.isAvailableForDrawing(app, routeInfoAttribute);
			cache.put(routeColoringType, available);
		}
		return available;
	}
}