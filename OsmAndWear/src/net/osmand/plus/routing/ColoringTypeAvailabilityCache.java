package net.osmand.plus.routing;

import static net.osmand.plus.routing.ColoringStyleAlgorithms.isAvailableForDrawingRoute;
import static net.osmand.plus.routing.ColoringStyleAlgorithms.isAvailableInSubscription;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.shared.routing.ColoringType;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ColoringTypeAvailabilityCache {

	private final OsmandApplication app;
	private final Map<String, Boolean> cache = new HashMap<>();

	private RouteCalculationResult prevRoute;

	public ColoringTypeAvailabilityCache(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public boolean isColoringAvailable(@NonNull RouteCalculationResult route,
	                                   @NonNull ColoringType routeColoringType,
	                                   @Nullable String routeInfoAttribute) {
		if (!route.equals(prevRoute)) {
			resetCache();
			prevRoute = route;
		}
		String key = routeColoringType.getName(routeInfoAttribute);
		Boolean available = cache.get(key);
		if (available == null) {
			ColoringStyle coloringStyle = new ColoringStyle(routeColoringType, routeInfoAttribute);
			boolean drawing = isAvailableForDrawingRoute(app, coloringStyle, route);
			boolean subscription = isAvailableInSubscription(app, coloringStyle, true);
			available = drawing && subscription;
			cache.put(key, available);
		}
		return available;
	}

	public void resetCache() {
		cache.clear();
	}
}