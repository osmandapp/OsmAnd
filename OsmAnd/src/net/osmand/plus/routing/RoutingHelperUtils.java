package net.osmand.plus.routing;

import static net.osmand.plus.routing.CurrentStreetName.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RoutingHelperUtils {

	private static final int CACHE_RADIUS = 100000;
	public static final int MAX_BEARING_DEVIATION = 45;

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
			formattedStreetName.append(towards).append(" ").append(destination);
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
		List<TargetPointsHelper.TargetPoint> targetPoints = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
		for (TargetPointsHelper.TargetPoint l : targetPoints) {
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
	                                              @NonNull Location projection,
	                                              @NonNull Location location,
	                                              @NonNull Location previousRouteLocation,
	                                              @NonNull Location currentRouteLocation,
	                                              @NonNull Location nextRouteLocation) {
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
		float nextSegmentBearing = MapUtils.normalizeDegrees360(currentRouteLocation.bearingTo(nextRouteLocation));
		float segmentsBearingDelta = MapUtils.unifyRotationDiff(currentSegmentBearing, nextSegmentBearing)
				* projectionOffsetN * projectionOffsetN;
		float approximatedBearing = MapUtils.normalizeDegrees360(currentSegmentBearing + segmentsBearingDelta);

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
}
