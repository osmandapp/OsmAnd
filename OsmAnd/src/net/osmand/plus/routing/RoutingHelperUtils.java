package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.helpers.enums.MetricsConstants;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.MapUtils;

import java.util.List;

public class RoutingHelperUtils {

	private static final float POSITION_TOLERANCE = 60;
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

	static LatLon getProject(Location loc, Location from, Location to) {
		return MapUtils.getProjection(loc.getLatitude(),
				loc.getLongitude(), from.getLatitude(), from.getLongitude(),
				to.getLatitude(), to.getLongitude());
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

	public static float getPosTolerance(float accuracy) {
		if (accuracy > 0) {
			return POSITION_TOLERANCE / 2 + accuracy;
		}
		return POSITION_TOLERANCE;
	}

	public static float getDefaultAllowedDeviation(OsmandSettings settings, ApplicationMode mode, float posTolerance) {
		if (settings.DISABLE_OFFROUTE_RECALC.getModeValue(mode)) {
			return -1.0f;
		} else if (mode.getRouteService() == RouteProvider.RouteService.DIRECT_TO) {
			return -1.0f;
		} else if (mode.getRouteService() == RouteProvider.RouteService.STRAIGHT) {
			MetricsConstants mc = settings.METRIC_SYSTEM.getModeValue(mode);
			if (mc == MetricsConstants.KILOMETERS_AND_METERS || mc == MetricsConstants.MILES_AND_METERS) {
				return 500.f;
			} else {
				// 1/4 mile
				return 482.f;
			}
		}
		return posTolerance * RoutingHelper.ALLOWED_DEVIATION;
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

	static float getArrivalDistance(ApplicationMode mode, OsmandSettings settings) {
		ApplicationMode m = mode == null ? settings.getApplicationMode() : mode;
		float defaultSpeed = Math.max(0.3f, m.getDefaultSpeed());

		/// Used to be: car - 90 m, bicycle - 50 m, pedestrian - 20 m
		// return ((float)settings.getApplicationMode().getArrivalDistance()) * settings.ARRIVAL_DISTANCE_FACTOR.getModeValue(m);
		// GPS_TOLERANCE - 12 m
		// 5 seconds: car - 80 m @ 50 kmh, bicyle - 45 m @ 25 km/h, bicyle - 25 m @ 10 km/h, pedestrian - 18 m @ 4 km/h,
		return RoutingHelper.GPS_TOLERANCE + defaultSpeed * 5 * RoutingHelper.ARRIVAL_DISTANCE_FACTOR;
	}

	public static void checkAndUpdateStartLocation(@NonNull OsmandApplication app, LatLon newStartLocation) {
		if (newStartLocation != null) {
			LatLon lastStartLocation = app.getSettings().getLastStartPoint();
			if (lastStartLocation == null || MapUtils.getDistance(newStartLocation, lastStartLocation) > CACHE_RADIUS) {
				app.getMapViewTrackingUtilities().detectDrivingRegion(newStartLocation);
				app.getSettings().setLastStartPoint(newStartLocation);
			}
		}
	}

	public static void checkAndUpdateStartLocation(@NonNull OsmandApplication app, Location nextStartLocation) {
		if (nextStartLocation != null) {
			checkAndUpdateStartLocation(app, new LatLon(nextStartLocation.getLatitude(), nextStartLocation.getLongitude()));
		}
	}
}
