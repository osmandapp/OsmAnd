/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteCalculationParams;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

/**
 * Rebuilds and applies a lower-energy route variant from modified segment lists.
 */
public final class EnergyRouteApplier {

	private EnergyRouteApplier() {
	}

	public static boolean applyVariant(@NonNull OsmandApplication app,
			@NonNull List<RouteSegmentResult> segments,
			@NonNull RouteCalculationResult currentRoute) {
		if (Algorithms.isEmpty(segments)) {
			return false;
		}
		RouteCalculationParams params = buildParams(app, currentRoute);
		if (params.end == null) {
			return false;
		}
		RouteCalculationResult newRoute = new RouteCalculationResult(
				new ArrayList<>(segments), params, null, null, true);
		if (newRoute.isEmpty() || newRoute.getErrorMessage() != null) {
			return false;
		}
		Location start = params.start;
		return app.getRoutingHelper().applyCalculatedRoute(newRoute, start);
	}

	@NonNull
	private static RouteCalculationParams buildParams(@NonNull OsmandApplication app,
			@NonNull RouteCalculationResult currentRoute) {
		RoutingHelper routingHelper = app.getRoutingHelper();
		RouteCalculationParams params = new RouteCalculationParams();
		params.ctx = app;
		params.mode = currentRoute.getAppMode() != null
				? currentRoute.getAppMode() : routingHelper.getAppMode();
		params.end = routingHelper.getFinalLocation();
		params.intermediates = routingHelper.getIntermediatePoints();
		params.initialCalculation = false;
		params.gpxFile = currentRoute.getGpxFile();
		params.start = resolveStartLocation(app, currentRoute);
		RoutingHelper.applyApplicationSettings(params, app.getSettings(), params.mode);
		return params;
	}

	@Nullable
	private static Location resolveStartLocation(@NonNull OsmandApplication app,
			@NonNull RouteCalculationResult currentRoute) {
		Location start = app.getLocationProvider().getLastKnownLocation();
		if (start != null) {
			return start;
		}
		List<Location> locations = currentRoute.getImmutableAllLocations();
		if (!locations.isEmpty()) {
			return locations.get(0);
		}
		return null;
	}
}
