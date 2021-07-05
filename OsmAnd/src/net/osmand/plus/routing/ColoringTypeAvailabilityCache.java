package net.osmand.plus.routing;

import net.osmand.plus.OsmandApplication;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ColoringTypeAvailabilityCache {

	private final OsmandApplication app;
	private final Map<String, Boolean> cache = new HashMap<>();

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

		String key = routeColoringType.isRouteInfoAttribute() ?
				routeInfoAttribute : routeColoringType.getName();

		Boolean available = cache.get(key);
		if (available == null) {
			available = routeColoringType.isAvailableForDrawing(app, routeInfoAttribute);
			cache.put(key, available);
		}
		return available;
	}
}