package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.util.KMapUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RoutingHelperUtils {

	private static final int CACHE_RADIUS = 100000;
	public static final int MAX_BEARING_DEVIATION = 45;
	private static final Log log = LogFactory.getLog(RoutingHelperUtils.class);

	@NonNull
	public static String formatStreetName(@Nullable String name, @Nullable String ref, @Nullable String destination,
	                                      @NonNull String towards) {
		return formatStreetName(name, ref, destination, towards, null);
	}

	@NonNull
	public static String formatStreetName(@Nullable String name, @Nullable String originalRef, @Nullable String destination,
	                                      @NonNull String towards, @Nullable List<RoadShield> shields) {
		StringBuilder formattedStreetName = new StringBuilder();
		if (originalRef != null && originalRef.length() > 0) {
			String[] refs = originalRef.split(";");
			for (String ref : refs) {
				if (shields == null || !isRefEqualsShield(shields, ref)) {
					if (formattedStreetName.length() > 0) {
						formattedStreetName.append(" ");
					}
					formattedStreetName.append(ref);
				}
			}
		}
		if (name != null && name.length() > 0) {
			if (formattedStreetName.length() > 0) {
				formattedStreetName.append(" ");
			}
			formattedStreetName.append(name);
		}
		if (destination != null && destination.length() > 0) {
			if (formattedStreetName.length() > 0) {
				formattedStreetName.append(" ");
			}
			if (!Algorithms.isEmpty(towards)) {
				formattedStreetName.append(towards).append(" ");
			}
			formattedStreetName.append(destination);
		}
		return formattedStreetName.toString().replace(";", ", ");
	}

	private static boolean isRefEqualsShield(@NonNull List<RoadShield> shields, @NonNull String ref) {
		for (RoadShield shield : shields) {
			String shieldValue = shield.getValue();
			if (ref.equals(shieldValue) || String.valueOf(Algorithms.extractIntegerNumber(ref)).equals(shieldValue)) {
				return true;
			}
		}
		return false;
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
		List<TargetPoint> targetPoints = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
		for (TargetPoint l : targetPoints) {
			MapUtils.insetLatLonRect(rect, l.getLatitude(), l.getLongitude());
		}

		return rect.left == 0 && rect.right == 0 ? null : rect;
	}

	@NonNull
	public static Location getProject(@NonNull Location loc, @NonNull Location from, @NonNull Location to) {
		LatLon project = MapUtils.getProjection(loc.getLatitude(),
				loc.getLongitude(), from.getLatitude(), from.getLongitude(),
				to.getLatitude(), to.getLongitude());
		Location locationProjection = new Location(loc);
		locationProjection.setLatitude(project.getLatitude());
		locationProjection.setLongitude(project.getLongitude());
		return locationProjection;
	}

	public static void approximateBearingIfNeeded(@NonNull RoutingHelper routingHelper,
			@NonNull Location projection, @NonNull Location location, @NonNull Location previousRouteLocation, @NonNull Location currentRouteLocation,
			@NonNull Location nextRouteLocation, boolean previewNextTurn) {
		double dist = location.distanceTo(projection);
		double maxDist = routingHelper.getMaxAllowedProjectDist(currentRouteLocation);
		if (dist >= maxDist) {
			return;
		}

		float projectionOffsetN = (float) MapUtils.getProjectionCoeff(
				location.getLatitude(), location.getLongitude(),
				previousRouteLocation.getLatitude(), previousRouteLocation.getLongitude(),
				currentRouteLocation.getLatitude(), currentRouteLocation.getLongitude());
		float currentSegmentBearing = MapUtils.normalizeDegrees360(previousRouteLocation.bearingTo(currentRouteLocation));

		float approximatedBearing = currentSegmentBearing;
		if (previewNextTurn) {
			float offset = projectionOffsetN * projectionOffsetN;
			float nextSegmentBearing = MapUtils.normalizeDegrees360(currentRouteLocation.bearingTo(nextRouteLocation));
			float segmentsBearingDelta = MapUtils.unifyRotationDiff(currentSegmentBearing, nextSegmentBearing) * offset;
			approximatedBearing = MapUtils.normalizeDegrees360(currentSegmentBearing + segmentsBearingDelta);
		}

		boolean setApproximated = true;
		if (location.hasBearing() && dist >= maxDist / 2) {
			float rotationDiff = MapUtils.unifyRotationDiff(location.getBearing(), approximatedBearing);
			setApproximated = Math.abs(rotationDiff) < MAX_BEARING_DEVIATION;
		}
		if (setApproximated) {
			projection.setBearing(approximatedBearing);
		}
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
	 * and bearing from prevLocation to next (current) point
	 * the difference is more than 60 degrees
	 */
	public static boolean checkWrongMovementDirection(Location currentLocation, Location prevRouteLocation, Location nextRouteLocation) {
		// measuring without bearing could be really error prone (with last fixed location)
		// this code has an effect on route recalculation which should be detected without mistakes
		if (currentLocation.hasBearing() && nextRouteLocation != null) {
			final float ASSUME_AS_INVALID_BEARING = 90.0f; // special case (possibly only in the Android emulator)
			float bearingMotion = currentLocation.getBearing();
			if (bearingMotion == ASSUME_AS_INVALID_BEARING) {
				return false;
			}
			float bearingToRoute = prevRouteLocation != null
					? prevRouteLocation.bearingTo(nextRouteLocation)
					: currentLocation.bearingTo(nextRouteLocation);
			double diff = MapUtils.degreesDiff(bearingMotion, bearingToRoute);
			if (Math.abs(diff) > 90f) {
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


	public static void updateDrivingRegionIfNeeded(@NonNull OsmandApplication app, @Nullable LatLon newStartLocation, boolean force) {
		OsmandSettings settings = app.getSettings();
		if (settings.DRIVING_REGION_AUTOMATIC.get() && newStartLocation != null) {
			LatLon lastStartLocation = settings.getLastStartPoint();
			if (lastStartLocation == null || MapUtils.getDistance(newStartLocation, lastStartLocation) > CACHE_RADIUS || force) {
				app.getMapViewTrackingUtilities().detectDrivingRegion(newStartLocation);
				settings.setLastStartPoint(newStartLocation);
			}
		}
	}

	public static void updateDrivingRegionIfNeeded(@NonNull OsmandApplication app, @Nullable Location nextStartLocation, boolean force) {
		if (nextStartLocation != null) {
			LatLon newStartLocation = new LatLon(nextStartLocation.getLatitude(), nextStartLocation.getLongitude());
			updateDrivingRegionIfNeeded(app, newStartLocation, force);
		}
	}

	public static RoutingParameter getParameterForDerivedProfile(@NonNull String id, @NonNull ApplicationMode appMode, @NonNull GeneralRouter router) {
		Map<String, RoutingParameter> parameters = getParametersForDerivedProfile(appMode, router);
		return parameters.get(id);
	}

	@NonNull
	public static Map<String, RoutingParameter> getParametersForDerivedProfile(@NonNull ApplicationMode appMode, @NonNull GeneralRouter router) {
		String derivedProfile = appMode.getDerivedProfile();
		Map<String, RoutingParameter> parameters = new LinkedHashMap<>();
		for (Entry<String, RoutingParameter> entry : router.getParameters().entrySet()) {
			String[] profiles = entry.getValue().getProfiles();
			if (profiles == null || Arrays.asList(profiles).contains(derivedProfile)) {
				parameters.put(entry.getKey(), entry.getValue());
			}
		}
		return parameters;
	}

	@NonNull
	public static List<Location> predictLocations(@NonNull Location previousLocation, @NonNull Location currentLocation,
	                                        double timeInSeconds, @NonNull RouteCalculationResult route, int interpolationPercent) {
		float speedPrev = previousLocation.getSpeed();
		float speedNew = currentLocation.getSpeed();
		double avgSpeed = (speedPrev + speedNew) / 2.0;
		double remainingDistance = avgSpeed * timeInSeconds * (interpolationPercent / 100.0);

		List<Location> predictedLocations = new ArrayList<>();
		int currentRoute = route.getCurrentRouteForLocation(currentLocation) + 1;
		List<Location> routeLocations = route.getImmutableAllLocations();
		for (int i = currentRoute; i < routeLocations.size() - 1; i++) {
			Location pointA;
			Location pointB;
			if (i == currentRoute) {
				pointA = currentLocation;
				pointB = routeLocations.get(i);
			} else {
				pointA = routeLocations.get(i);
				pointB = routeLocations.get(i + 1);
			}
			double segmentDistance = pointA.distanceTo(pointB);
			if (remainingDistance <= segmentDistance) {
				double fraction = remainingDistance / segmentDistance;
				KLatLon interpolatedLoc = KMapUtils.INSTANCE.interpolateLatLon(
						pointA.getLatitude(), pointA.getLongitude(), pointB.getLatitude(), pointB.getLongitude(), fraction);
				Location predictedPoint = buildPredictedLocation(currentLocation, pointA, pointB);
				predictedPoint.setLatitude(interpolatedLoc.getLatitude());
				predictedPoint.setLongitude(interpolatedLoc.getLongitude());
				predictedLocations.add(predictedPoint);
				break;
			} else {
				predictedLocations.add(buildPredictedLocation(currentLocation, pointA, pointB));
				remainingDistance -= segmentDistance;
			}
		}

		if (predictedLocations.isEmpty() && !routeLocations.isEmpty()) {
			Location lastRouteLocation = routeLocations.get(routeLocations.size() - 1);
			predictedLocations.add(buildPredictedLocation(currentLocation, currentLocation, lastRouteLocation));
		}

		return predictedLocations;
	}

	@NonNull
	private static Location buildPredictedLocation(Location currentLocation, Location pointA, Location pointB) {
		Location predictedPoint = new Location(currentLocation);
		predictedPoint.setProvider("predicted");
		predictedPoint.setLatitude(pointB.getLatitude());
		predictedPoint.setLongitude(pointB.getLongitude());
		predictedPoint.setBearing(pointA.bearingTo(pointB));
		return predictedPoint;
	}
}
