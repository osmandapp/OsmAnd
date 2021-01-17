package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.util.MapUtils;

import java.util.List;

public class RoutingHelperUtils {

	private static final int CACHE_RADIUS = 100000;

	@NonNull
	public static String formatStreetName(String name, String ref, String destination, String towards) {
		String formattedStreetName = "";
		if (ref != null && ref.length() > 0) {
			formattedStreetName = ref;
		}
		if (name != null && name.length() > 0) {
			if (formattedStreetName.length() > 0) {
				formattedStreetName = formattedStreetName + " ";
			}
			formattedStreetName = formattedStreetName + name;
		}
		if (destination != null && destination.length() > 0) {
			if (formattedStreetName.length() > 0) {
				formattedStreetName = formattedStreetName + " ";
			}
			formattedStreetName = formattedStreetName + towards + " " + destination;
		}
		return formattedStreetName.replace(";", ", ");
	}

	@Nullable
	public static QuadRect getRouteRect(@NonNull OsmandApplication app, @NonNull RouteCalculationResult result) {
		QuadRect rect = new QuadRect(0, 0, 0, 0);
		Location lt = app.getRoutingHelper().getLastProjection();
		if (lt == null) {
			lt = app.getTargetPointsHelper().getPointToStartLocation();
		}
		if (lt == null) {
			lt = app.getLocationProvider().getLastKnownLocation();
		}
		if (lt != null) {
			MapUtils.insetLatLonRect(rect, lt.getLatitude(), lt.getLongitude());
		}
		List<Location> list = result.getImmutableAllLocations();
		for (Location l : list) {
			MapUtils.insetLatLonRect(rect, l.getLatitude(), l.getLongitude());
		}
		List<TargetPointsHelper.TargetPoint> targetPoints = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
		for (TargetPointsHelper.TargetPoint l : targetPoints) {
			MapUtils.insetLatLonRect(rect, l.getLatitude(), l.getLongitude());
		}

		return rect.left == 0 && rect.right == 0 ? null : rect;
	}

	static Location getProject(Location loc, Location from, Location to) {
		LatLon project = MapUtils.getProjection(loc.getLatitude(),
				loc.getLongitude(), from.getLatitude(), from.getLongitude(),
				to.getLatitude(), to.getLongitude());
		Location locationProjection = new Location(loc);
		locationProjection.setLatitude(project.getLatitude());
		locationProjection.setLongitude(project.getLongitude());
		// we need to update bearing too
		float bearingTo = locationProjection.bearingTo(to);
		locationProjection.setBearing(bearingTo);
		return locationProjection;
	}

	static double getOrthogonalDistance(Location loc, Location from, Location to) {
		return MapUtils.getOrthogonalDistance(loc.getLatitude(),
				loc.getLongitude(), from.getLatitude(), from.getLongitude(),
				to.getLatitude(), to.getLongitude());
	}

	static int lookAheadFindMinOrthogonalDistance(Location currentLocation, List<Location> routeNodes, int currentRoute, int iterations) {
		double newDist;
		double dist = Double.POSITIVE_INFINITY;
		int index = currentRoute;
		while (iterations > 0 && currentRoute + 1 < routeNodes.size()) {
			newDist = getOrthogonalDistance(currentLocation, routeNodes.get(currentRoute), routeNodes.get(currentRoute + 1));
			if (newDist < dist) {
				index = currentRoute;
				dist = newDist;
			}
			currentRoute++;
			iterations--;
		}
		return index;
	}

	/**
	 * Wrong movement direction is considered when between
	 * current location bearing (determines by 2 last fixed position or provided)
	 * and bearing from currentLocation to next (current) point
	 * the difference is more than 60 degrees
	 */
	public static boolean checkWrongMovementDirection(Location currentLocation, Location nextRouteLocation) {
		// measuring without bearing could be really error prone (with last fixed location)
		// this code has an effect on route recalculation which should be detected without mistakes
		if (currentLocation.hasBearing() && nextRouteLocation != null) {
			float bearingMotion = currentLocation.getBearing();
			float bearingToRoute = currentLocation.bearingTo(nextRouteLocation);
			double diff = MapUtils.degreesDiff(bearingMotion, bearingToRoute);
			if (Math.abs(diff) > 60f) {
				// require delay interval since first detection, to avoid false positive
				//but leave out for now, as late detection is worse than false positive (it may reset voice router then cause bogus turn and u-turn prompting)
				//if (wrongMovementDetected == 0) {
				//	wrongMovementDetected = System.currentTimeMillis();
				//} else if ((System.currentTimeMillis() - wrongMovementDetected > 500)) {
				return true;
				//}
			} else {
				//wrongMovementDetected = 0;
				return false;
			}
		}
		//wrongMovementDetected = 0;
		return false;
	}

	static boolean identifyUTurnIsNeeded(@NonNull RoutingHelper routingHelper, @NonNull Location currentLocation, double posTolerance) {
		RouteCalculationResult route = routingHelper.getRoute();
		if (routingHelper.getFinalLocation() == null || currentLocation == null || !route.isCalculated() || routingHelper.isPublicTransportMode()) {
			return false;
		}
		boolean isOffRoute = false;
		if (currentLocation.hasBearing()) {
			float bearingMotion = currentLocation.getBearing();
			Location nextRoutePosition = route.getNextRouteLocation();
			float bearingToRoute = currentLocation.bearingTo(nextRoutePosition);
			double diff = MapUtils.degreesDiff(bearingMotion, bearingToRoute);
			// 7. Check if you left the route and an unscheduled U-turn would bring you back (also Issue 863)
			// This prompt is an interim advice and does only sound if a new route in forward direction could not be found in x seconds
			if (Math.abs(diff) > 135f) {
				float d = currentLocation.distanceTo(nextRoutePosition);
				// 60m tolerance to allow for GPS inaccuracy
				if (d > posTolerance) {
					// require x sec continuous since first detection
					long deviateFromRouteDetected = routingHelper.getDeviateFromRouteDetected();
					if (deviateFromRouteDetected == 0) {
						routingHelper.setDeviateFromRouteDetected(System.currentTimeMillis());
					} else if ((System.currentTimeMillis() - deviateFromRouteDetected > 10000)) {
						isOffRoute = true;
						//log.info("bearingMotion is opposite to bearingRoute"); //$NON-NLS-1$
					}
				}
			} else {
				routingHelper.setDeviateFromRouteDetected(0);
			}
		}
		return isOffRoute;
	}


	public static void checkAndUpdateStartLocation(@NonNull OsmandApplication app, LatLon newStartLocation, boolean force) {
		if (newStartLocation != null) {
			LatLon lastStartLocation = app.getSettings().getLastStartPoint();
			if (lastStartLocation == null || MapUtils.getDistance(newStartLocation, lastStartLocation) > CACHE_RADIUS || force) {
				app.getMapViewTrackingUtilities().detectDrivingRegion(newStartLocation);
				app.getSettings().setLastStartPoint(newStartLocation);
			}
		}
	}

	public static void checkAndUpdateStartLocation(@NonNull OsmandApplication app, Location nextStartLocation, boolean force) {
		if (nextStartLocation != null) {
			LatLon newStartLocation = new LatLon(nextStartLocation.getLatitude(), nextStartLocation.getLongitude());
			checkAndUpdateStartLocation(app, newStartLocation, force);
		}
	}
}
