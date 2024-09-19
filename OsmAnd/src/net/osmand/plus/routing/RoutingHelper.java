package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.LocationsHolder;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.LatLon;
import net.osmand.data.ValueHolder;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.GpxRouteApproximation;
import net.osmand.router.RouteExporter;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class RoutingHelper {

	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(RoutingHelper.class);

	// POS_TOLERANCE
	// 1) calculate current closest segment of the route during navigation
	// 2) identify u-turn, projected distance
	// 3) calculate max allowed deviation before route recalculation * multiplier
	private static final float POS_TOLERANCE = 60; // 60m or 30m + accuracy
	private static final float POS_TOLERANCE_DEVIATION_MULTIPLIER = 2;
	private static final int MAX_POSSIBLE_SPEED = 140;// 504 km/h

	private List<WeakReference<IRouteInformationListener>> listeners = new LinkedList<>();
	private List<WeakReference<IRoutingDataUpdateListener>> updateListeners = new LinkedList<>();
	private List<WeakReference<IRouteSettingsListener>> settingsListeners = new LinkedList<>();

	private final OsmandApplication app;
	private OsmandSettings settings;
	private final RouteProvider provider;
	private final VoiceRouter voiceRouter;
	private final RouteRecalculationHelper routeRecalculationHelper;
	private final TransportRoutingHelper transportRoutingHelper;

	private boolean isFollowingMode;
	private boolean isRoutePlanningMode;
	private boolean isPauseNavigation;

	private GPXRouteParamsBuilder currentGPXRoute;

	private RouteCalculationResult route = new RouteCalculationResult("");

	private LatLon finalLocation;
	private List<LatLon> intermediatePoints;
	private Location lastProjection;
	private Location lastFixedLocation;
	private Location lastGoodRouteLocation;
	private boolean routeWasFinished;
	private ApplicationMode mode;
	private boolean deviceHasBearing;

	private boolean isDeviatedFromRoute;
	private long deviateFromRouteDetected;
	//private long wrongMovementDetected = 0;
	private boolean voiceRouterStopped;

	public boolean isDeviatedFromRoute() {
		return isDeviatedFromRoute;
	}

	public boolean isRouteWasFinished() {
		return routeWasFinished;
	}

	public RoutingHelper(OsmandApplication context) {
		this.app = context;
		settings = context.getSettings();
		voiceRouter = new VoiceRouter(this);
		provider = new RouteProvider();
		routeRecalculationHelper = new RouteRecalculationHelper(this);
		transportRoutingHelper = context.getTransportRoutingHelper();
		transportRoutingHelper.setRoutingHelper(this);
		setAppMode(settings.APPLICATION_MODE.get());

		OsmAndAppCustomizationListener customizationListener = () -> settings = app.getSettings();
		app.getAppCustomization().addListener(customizationListener);
	}

	RouteProvider getProvider() {
		return provider;
	}

	void resetRouteWasFinished() {
		routeWasFinished = false;
	}

	void setRoute(RouteCalculationResult route) {
		this.route = route;
	}

	long getDeviateFromRouteDetected() {
		return deviateFromRouteDetected;
	}

	void setDeviateFromRouteDetected(long deviateFromRouteDetected) {
		this.deviateFromRouteDetected = deviateFromRouteDetected;
	}

	public TransportRoutingHelper getTransportRoutingHelper() {
		return transportRoutingHelper;
	}

	public boolean isFollowingMode() {
		return isFollowingMode;
	}

	public OsmandApplication getApplication() {
		return app;
	}

	public String getLastRouteCalcError() {
		return routeRecalculationHelper.getLastRouteCalcError();
	}

	public String getLastRouteCalcErrorShort() {
		return routeRecalculationHelper.getLastRouteCalcErrorShort();
	}

	public void resumeNavigation() {
		setRoutePlanningMode(false);
		setFollowingMode(true);
		setCurrentLocation(app.getLocationProvider().getLastKnownLocation(), false);
	}

	public void pauseNavigation() {
		setRoutePlanningMode(true);
		setFollowingMode(false);
		setPauseNavigation(true);
	}

	public void setPauseNavigation(boolean pause) {
		app.logRoutingEvent("setPauseNavigation pause " + pause);
		this.isPauseNavigation = pause;
		if (pause) {
			if (app.getNavigationService() != null) {
				app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_NAVIGATION);
			} else {
				app.getNotificationHelper().updateTopNotification();
				app.getNotificationHelper().refreshNotifications();
			}
		} else {
			app.startNavigationService(NavigationService.USED_BY_NAVIGATION);
		}
	}

	public boolean isPauseNavigation() {
		return isPauseNavigation;
	}

	public void setFollowingMode(boolean follow) {
		app.logRoutingEvent("setFollowingMode follow " + follow);
		isFollowingMode = follow;
		isPauseNavigation = false;
		if (!follow) {
			if (app.getNavigationService() != null) {
				app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_NAVIGATION);
			} else {
				app.getNotificationHelper().updateTopNotification();
				app.getNotificationHelper().refreshNotifications();
			}
		} else {
			app.startNavigationService(NavigationService.USED_BY_NAVIGATION);
		}
	}

	public boolean isRoutePlanningMode() {
		return isRoutePlanningMode;
	}

	public void setRoutePlanningMode(boolean isRoutePlanningMode) {
		this.isRoutePlanningMode = isRoutePlanningMode;
	}

	public synchronized void setFinalAndCurrentLocation(LatLon finalLocation, List<LatLon> intermediatePoints, Location currentLocation) {
		app.logRoutingEvent("setFinalAndCurrentLocation finalLocation " + finalLocation + " intermediatePoints " + intermediatePoints + " currentLocation " + currentLocation);
		RoutingHelperUtils.updateDrivingRegionIfNeeded(app, currentLocation, false);
		RouteCalculationResult previousRoute = route;
		clearCurrentRoute(finalLocation, intermediatePoints);
		// to update route
		setCurrentLocation(currentLocation, false, previousRoute, true);
	}

	public synchronized void clearCurrentRoute(LatLon newFinalLocation, List<LatLon> newIntermediatePoints) {
		app.logRoutingEvent("clearCurrentRoute newFinalLocation " + newFinalLocation + " newIntermediatePoints " + newIntermediatePoints);
		route = new RouteCalculationResult("");
		isDeviatedFromRoute = false;
		routeRecalculationHelper.resetEvalWaitInterval();
		app.getWaypointHelper().setNewRoute(route);
		app.runInUIThread(() -> {
			Iterator<WeakReference<IRouteInformationListener>> it = listeners.iterator();
			while (it.hasNext()) {
				WeakReference<IRouteInformationListener> ref = it.next();
				IRouteInformationListener l = ref.get();
				if (l == null) {
					it.remove();
				} else {
					l.routeWasCancelled();
				}
			}
		});
		this.finalLocation = newFinalLocation;
		this.lastGoodRouteLocation = null;
		this.intermediatePoints = newIntermediatePoints;
		routeRecalculationHelper.stopCalculation();
		if (newFinalLocation == null) {
			settings.FOLLOW_THE_ROUTE.set(false);
			settings.FOLLOW_THE_GPX_ROUTE.set(null);
			// clear last fixed location
			this.lastProjection = null;
			setFollowingMode(false);
		}
		transportRoutingHelper.clearCurrentRoute(newFinalLocation);
	}

	private synchronized void finishCurrentRoute() {
		app.logRoutingEvent("finishCurrentRoute");
		routeWasFinished = true;
		app.runInUIThread(() -> {
			Iterator<WeakReference<IRouteInformationListener>> it = listeners.iterator();
			while (it.hasNext()) {
				WeakReference<IRouteInformationListener> ref = it.next();
				IRouteInformationListener l = ref.get();
				if (l == null) {
					it.remove();
				} else {
					l.routeWasFinished();
				}
			}
		});
	}

	void newRouteCalculated(boolean newRoute, RouteCalculationResult res) {
		app.logRoutingEvent("newRouteCalculated newRoute " + newRoute + " res " + res);
		app.runInUIThread(() -> {
			ValueHolder<Boolean> showToast = new ValueHolder<>();
			showToast.value = true;
			Iterator<WeakReference<IRouteInformationListener>> it = listeners.iterator();
			while (it.hasNext()) {
				WeakReference<IRouteInformationListener> ref = it.next();
				IRouteInformationListener l = ref.get();
				if (l == null) {
					it.remove();
				} else {
					l.newRouteIsCalculated(newRoute, showToast);
				}
			}
			if (showToast.value && newRoute && PluginsHelper.isDevelopment() && settings.DEBUG_RENDERING_INFO.get()) {
				String msg = app.getString(R.string.new_route_calculated_dist_dbg,
						OsmAndFormatter.getFormattedDistance(res.getWholeDistance(), app),
						((int) res.getRoutingTime()) + " sec",
						res.getCalculateTime(), res.getVisitedSegments(), res.getLoadedTiles());
				app.showToastMessage(msg);
			}
		});
	}

	public GPXRouteParamsBuilder getCurrentGPXRoute() {
		return currentGPXRoute;
	}

	public boolean isCurrentGPXRouteV2() {
		return currentGPXRoute != null && RouteExporter.OSMAND_ROUTER_V2.equals(currentGPXRoute.getFile().getAuthor());
	}

	@Nullable
	public GpxFile getCurrentGPX() {
		return currentGPXRoute != null ? currentGPXRoute.getFile() : null;
	}

	public void setGpxParams(GPXRouteParamsBuilder params) {
		app.logRoutingEvent("setGpxParams params " + params);
		currentGPXRoute = params;
	}

	public List<Location> getCurrentCalculatedRoute() {
		return route.getImmutableAllLocations();
	}

	public void setAppMode(ApplicationMode mode) {
		this.mode = mode;
		voiceRouter.updateAppMode();
	}

	public ApplicationMode getAppMode() {
		return mode;
	}

	public LatLon getFinalLocation() {
		return finalLocation;
	}

	public List<LatLon> getIntermediatePoints() {
		return intermediatePoints;
	}

	public boolean isRouteCalculated() {
		return route.isCalculated();
	}

	@NonNull
	public VoiceRouter getVoiceRouter() {
		return voiceRouter;
	}

	@Nullable
	public Location getLastProjection() {
		return lastProjection;
	}

	public Location getLastFixedLocation() {
		return lastFixedLocation;
	}

	public void addRouteDataListener(@NonNull IRoutingDataUpdateListener listener) {
		updateListeners = Algorithms.updateWeakReferencesList(updateListeners, listener, true);
	}

	public void removeRouteDataListener(@NonNull IRoutingDataUpdateListener listener) {
		updateListeners = Algorithms.updateWeakReferencesList(updateListeners, listener, false);
	}

	public void addRouteSettingsListener(@NonNull IRouteSettingsListener listener) {
		settingsListeners = Algorithms.updateWeakReferencesList(settingsListeners, listener, true);
	}

	public void removeRouteSettingsListener(@NonNull IRouteSettingsListener listener) {
		settingsListeners = Algorithms.updateWeakReferencesList(settingsListeners, listener, false);
	}

	public void addListener(@NonNull IRouteInformationListener l) {
		listeners = Algorithms.updateWeakReferencesList(listeners, l, true);
		transportRoutingHelper.addListener(l);
	}

	public void removeListener(@NonNull IRouteInformationListener lt) {
		listeners = Algorithms.updateWeakReferencesList(listeners, lt, false);
	}

	public void updateLocation(Location currentLocation) {
		if (settings.getPointToStart() == null && settings.getMyLocationToStart() == null && currentLocation != null) {
			app.getTargetPointsHelper().setMyLocationPoint(
					new LatLon(currentLocation.getLatitude(), currentLocation.getLongitude()), false, null);
		}
		if (isFollowingMode() || (settings.getPointToStart() == null && isRoutePlanningMode) ||
				app.getLocationProvider().getLocationSimulation().isRouteAnimating()) {
			setCurrentLocation(currentLocation, false);
		}
	}

	public Location setCurrentLocation(Location currentLocation, boolean returnUpdatedLocation) {
		return setCurrentLocation(currentLocation, returnUpdatedLocation, route, false);
	}

	public double getRouteDeviation() {
		if (route == null ||
				route.getImmutableAllDirections().size() < 2 ||
				route.currentRoute == 0) {
			return 0;
		}
		List<Location> routeNodes = route.getImmutableAllLocations();
		return RoutingHelperUtils.getOrthogonalDistance(lastFixedLocation, routeNodes.get(route.currentRoute - 1), routeNodes.get(route.currentRoute));
	}

	private Location setCurrentLocation(Location currentLocation, boolean returnUpdatedLocation,
	                                    RouteCalculationResult previousRoute, boolean targetPointsChanged) {
		Location locationProjection = currentLocation;
		if (isPublicTransportMode() && currentLocation != null && finalLocation != null &&
				(targetPointsChanged || transportRoutingHelper.getStartLocation() == null)) {
			lastFixedLocation = currentLocation;
			lastProjection = locationProjection;
			transportRoutingHelper.setApplicationMode(mode);
			transportRoutingHelper.setFinalAndCurrentLocation(finalLocation,
					new LatLon(currentLocation.getLatitude(), currentLocation.getLongitude()));
		}
		if (finalLocation == null || currentLocation == null || isPublicTransportMode()) {
			isDeviatedFromRoute = false;
			return locationProjection;
		}
		float posTolerance = getPosTolerance(currentLocation.hasAccuracy() ? currentLocation.getAccuracy() : 0);
		boolean calculateRoute = false;
		synchronized (this) {
			isDeviatedFromRoute = false;
			double distOrth = 0;

			// 0. Route empty or needs to be extended? Then re-calculate route.
			if (route.isEmpty()) {
				calculateRoute = !route.hasMissingMaps() || isLocationJumping(currentLocation, targetPointsChanged);
			} else {
				// 1. Update current route position status according to latest received location
				boolean finished = updateCurrentRouteStatus(currentLocation, posTolerance);
				if (finished) {
					return null;
				}
				List<Location> routeNodes = route.getImmutableAllLocations();
				int currentRoute = route.currentRoute;
				double allowableDeviation = route.getRouteRecalcDistance();
				if (allowableDeviation <= 0) {
					allowableDeviation = getDefaultAllowedDeviation(settings, route.getAppMode(), posTolerance);
				}

				// 2. Analyze if we need to recalculate route
				// >100m off current route (sideways) or parameter (for Straight line)
				if (allowableDeviation > 0) {
					if (currentRoute == 0) {
						distOrth = currentLocation.distanceTo(routeNodes.get(currentRoute)); // deviation at the start
					} else {
						distOrth = RoutingHelperUtils.getOrthogonalDistance(currentLocation, routeNodes.get(currentRoute - 1), routeNodes.get(currentRoute));
					}
					if (distOrth > allowableDeviation) {
						log.info("Recalculate route, because correlation  : " + distOrth); //$NON-NLS-1$
						isDeviatedFromRoute = true;
						calculateRoute = !settings.DISABLE_OFFROUTE_RECALC.get();
					}
				}
				// 3. Identify wrong movement direction
				Location next = route.getNextRouteLocation();
				Location prev = route.getRouteLocationByDistance(-15);//-15 meters
				boolean isStraight =
						route.getRouteService() == RouteService.DIRECT_TO || route.getRouteService() == RouteService.STRAIGHT;
				boolean wrongMovementDirection = RoutingHelperUtils.checkWrongMovementDirection(currentLocation, prev, next);
				if ((allowableDeviation > 0 && wrongMovementDirection && !isStraight
						&& (currentLocation.distanceTo(routeNodes.get(currentRoute)) > allowableDeviation)) && !settings.DISABLE_WRONG_DIRECTION_RECALC.get()) {
					log.info("Recalculate route, because wrong movement direction: " + currentLocation.distanceTo(routeNodes.get(currentRoute))); //$NON-NLS-1$
					isDeviatedFromRoute = true;
					calculateRoute = true;
				}
				// 4. Identify if UTurn is needed
				if (RoutingHelperUtils.identifyUTurnIsNeeded(this, currentLocation, posTolerance)) {
					isDeviatedFromRoute = true;
				}
				// 5. Update Voice router
				// Do not update in route planning mode
				boolean inRecalc = (calculateRoute || isRouteBeingCalculated());
				if (isFollowingMode) {
					if (!inRecalc && !wrongMovementDirection) {
						voiceRouter.updateStatus(currentLocation, false);
						voiceRouterStopped = false;
					} else if (isDeviatedFromRoute && !voiceRouterStopped) {
						voiceRouter.interruptRouteCommands();
						voiceRouterStopped = true; // Prevents excessive execution of stop() code
					}
					voiceRouter.announceOffRoute(distOrth);
				}

				// calculate projection of current location
				if (currentRoute > 0 && !inRecalc) {
					Location previousRouteLocation = routeNodes.get(currentRoute - 1);
					Location currentRouteLocation = routeNodes.get(currentRoute);
					locationProjection = RoutingHelperUtils.getProject(currentLocation, previousRouteLocation,
							currentRouteLocation);
					if (settings.SNAP_TO_ROAD.get() && currentRoute + 1 < routeNodes.size()) {
						Location nextRouteLocation = routeNodes.get(currentRoute + 1);
						RoutingHelperUtils.approximateBearingIfNeeded(this,
								locationProjection, currentLocation,
								previousRouteLocation, currentRouteLocation, nextRouteLocation);
					}
				}
			}
			lastFixedLocation = currentLocation;
			lastProjection = locationProjection;
			if (!route.isEmpty()) {
				lastGoodRouteLocation = currentLocation;
			}
		}

		if (calculateRoute) {
			routeRecalculationHelper.recalculateRouteInBackground(currentLocation, finalLocation, intermediatePoints, currentGPXRoute,
					previousRoute.isCalculated() ? previousRoute : null, false, !targetPointsChanged);
		} else {
			routeRecalculationHelper.stopCalculationIfParamsNotChanged();
		}

		double projectDist = mode != null && mode.hasFastSpeed() ? posTolerance : posTolerance / 2;
		if (returnUpdatedLocation && locationProjection != null && currentLocation.distanceTo(locationProjection) < projectDist) {
			return locationProjection;
		} else {
			return currentLocation;
		}
	}

	private boolean isLocationJumping(Location currentLocation, boolean targetPointsChanged) {
		if (route.hasMissingMaps() && lastGoodRouteLocation != null && !targetPointsChanged) {
			double time = currentLocation.getTime() - lastGoodRouteLocation.getTime();
			double dist = currentLocation.distanceTo(lastGoodRouteLocation);
			if (time > 0) {
				double speed = dist / (time / 1000);
				return speed > MAX_POSSIBLE_SPEED;
			}
		}
		return false;
	}

	public double getMaxAllowedProjectDist(@NonNull Location location) {
		float posTolerance = getPosTolerance(location.hasAccuracy() ? location.getAccuracy() : 0);
		return mode != null && mode.hasFastSpeed() ? posTolerance : posTolerance / 2;
	}

	private boolean updateCurrentRouteStatus(Location currentLocation, double posTolerance) {
		List<Location> routeNodes = route.getImmutableAllLocations();
		int currentRoute = route.currentRoute;
		// 1. Try to proceed to next point using orthogonal distance (finding minimum orthogonal dist)
		currentRoute = calculateCurrentRoute(currentLocation, posTolerance, routeNodes, currentRoute, true);

		// 2. check if intermediate found
		if (route.getIntermediatePointsToPass() > 0
				&& route.getDistanceToNextIntermediate(lastFixedLocation) < voiceRouter.getArrivalDistance() && !isRoutePlanningMode) {
			showMessage(app.getString(R.string.arrived_at_intermediate_point));
			route.passIntermediatePoint();
			TargetPointsHelper targets = app.getTargetPointsHelper();
			String name = "";
			if (intermediatePoints != null && !intermediatePoints.isEmpty()) {
				LatLon rm = intermediatePoints.remove(0);
				List<TargetPoint> ll = targets.getIntermediatePointsNavigation();
				int ind = -1;
				for (int i = 0; i < ll.size(); i++) {
					if (ll.get(i).point != null && MapUtils.getDistance(ll.get(i).point, rm) < 5) {
						name = ll.get(i).getOnlyName();
						ind = i;
						break;
					}
				}
				if (ind >= 0) {
					targets.removeWayPoint(false, ind);
				}
			}
			if (isFollowingMode) {
				voiceRouter.arrivedIntermediatePoint(name);
			}
			// double check
			while (intermediatePoints != null && route.getIntermediatePointsToPass() < intermediatePoints.size()) {
				intermediatePoints.remove(0);
			}
		}

		// 3. check if destination found
		Location lastPoint = routeNodes.get(routeNodes.size() - 1);
		if (currentRoute > routeNodes.size() - 3
				&& currentLocation.distanceTo(lastPoint) < voiceRouter.getArrivalDistance()
				&& !isRoutePlanningMode) {
			//showMessage(app.getString(R.string.arrived_at_destination));
			TargetPointsHelper targets = app.getTargetPointsHelper();
			TargetPoint tp = targets.getPointToNavigate();
			String description = tp == null ? "" : tp.getOnlyName();
			if (isFollowingMode) {
				voiceRouter.arrivedDestinationPoint(description);
			}
			boolean onDestinationReached = true;
			if (onDestinationReached) {
				clearCurrentRoute(null, null);
				setRoutePlanningMode(false);
				app.runInUIThread(() -> {
					settings.LAST_ROUTING_APPLICATION_MODE = settings.APPLICATION_MODE.get();
					//settings.setApplicationMode(settings.DEFAULT_APPLICATION_MODE.get());
				});
				finishCurrentRoute();
				// targets.clearPointToNavigate(false);
				return true;
			}
		}

		// 4. update angle point
		if (route.getRouteVisibleAngle() > 0) {
			// proceed to the next point with min acceptable bearing
			double ANGLE_TO_DECLINE = route.getRouteVisibleAngle();
			int nextPoint = route.currentRoute;
			for (; nextPoint < routeNodes.size() - 1; nextPoint++) {
				float bearingTo = currentLocation.bearingTo(routeNodes.get(nextPoint));
				float bearingTo2 = routeNodes.get(nextPoint).bearingTo(routeNodes.get(nextPoint + 1));
				if (Math.abs(MapUtils.degreesDiff(bearingTo2, bearingTo)) <= ANGLE_TO_DECLINE) {
					break;
				}
			}

			if (nextPoint > 0) {
				Location next = routeNodes.get(nextPoint);
				Location prev = routeNodes.get(nextPoint - 1);
				float bearing = prev.bearingTo(next);
				double bearingTo = Math.abs(MapUtils.degreesDiff(bearing, currentLocation.bearingTo(next)));
				double bearingPrev = Math.abs(MapUtils.degreesDiff(bearing, currentLocation.bearingTo(prev)));
				while (true) {
					Location mp = MapUtils.calculateMidPoint(prev, next);
					if (mp.distanceTo(next) <= 100) {
						break;
					}
					double bearingMid = Math.abs(MapUtils.degreesDiff(bearing, currentLocation.bearingTo(mp)));
					if (bearingPrev < ANGLE_TO_DECLINE) {
						next = mp;
						bearingTo = bearingMid;
					} else if (bearingTo < ANGLE_TO_DECLINE) {
						prev = mp;
						bearingPrev = bearingMid;
					} else {
						break;
					}
				}
				route.updateNextVisiblePoint(nextPoint, next);
			}
		}

		// 5. Update car navigation
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession != null) {
			app.runInUIThread(() -> carNavigationSession.updateCarNavigation(currentLocation));
		}
		return false;
	}

	public int calculateCurrentRoute(@NonNull Location currentLocation, double posTolerance,
	                                 @NonNull List<Location> routeNodes, int currentRoute,
	                                 boolean updateAndNotify) {
		while (currentRoute + 1 < routeNodes.size()) {
			double dist = currentLocation.distanceTo(routeNodes.get(currentRoute));
			if (currentRoute > 0) {
				dist = RoutingHelperUtils.getOrthogonalDistance(currentLocation, routeNodes.get(currentRoute - 1),
						routeNodes.get(currentRoute));
			}
			boolean processed = false;
			// if we are still too far try to proceed many points
			// if not then look ahead only 3 in order to catch sharp turns
			boolean longDistance = dist >= 250;
			int newCurrentRoute = RoutingHelperUtils.lookAheadFindMinOrthogonalDistance(currentLocation, routeNodes, currentRoute, longDistance ? 15 : 8);
			double newDist = RoutingHelperUtils.getOrthogonalDistance(currentLocation, routeNodes.get(newCurrentRoute),
					routeNodes.get(newCurrentRoute + 1));
			if (longDistance) {
				if (newDist < dist) {
					if (log.isDebugEnabled()) {
						log.debug("Processed by distance : (new) " + newDist + " (old) " + dist); //$NON-NLS-1$//$NON-NLS-2$
					}
					processed = true;
				}
			} else if (newDist < dist || newDist < posTolerance / 8) {
				// newDist < posTolerance / 8 - 4-8 m (avoid distance 0 till next turn)
				if (dist > posTolerance) {
					processed = true;
					if (log.isDebugEnabled()) {
						log.debug("Processed by distance : " + newDist + " " + dist); //$NON-NLS-1$//$NON-NLS-2$
					}
				} else {
					if (currentLocation.hasBearing() && !deviceHasBearing) {
						deviceHasBearing = true;
					}
					// lastFixedLocation.bearingTo -  gives artefacts during u-turn, so we avoid for devices with bearing
					if ((currentRoute > 0 || newCurrentRoute > 0) &&
							(currentLocation.hasBearing() || (!deviceHasBearing && lastFixedLocation != null))) {
						float bearingToRoute = currentLocation.bearingTo(routeNodes.get(currentRoute));
						float bearingRouteNext = routeNodes.get(newCurrentRoute).bearingTo(routeNodes.get(newCurrentRoute + 1));
						float bearingMotion = currentLocation.hasBearing() ? currentLocation.getBearing() : lastFixedLocation
								.bearingTo(currentLocation);
						double diff = Math.abs(MapUtils.degreesDiff(bearingMotion, bearingToRoute));
						double diffToNext = Math.abs(MapUtils.degreesDiff(bearingMotion, bearingRouteNext));
						if (diff > diffToNext) {
							if (log.isDebugEnabled()) {
								log.debug("Processed point bearing deltas : " + diff + " " + diffToNext);
							}
							processed = true;
						}
					}
				}
			}
			if (processed) {
				// that node already passed
				currentRoute = newCurrentRoute + 1;
				if (updateAndNotify) {
					route.updateCurrentRoute(newCurrentRoute + 1);
					app.getNotificationHelper().refreshNotification(NotificationType.NAVIGATION);
					fireRoutingDataUpdateEvent();
				}
			} else {
				break;
			}
		}
		return currentRoute;
	}

	public static float getPosTolerance(float accuracy) {
		if (accuracy > 0) {
			return POS_TOLERANCE / 2 + accuracy;
		}
		return POS_TOLERANCE;
	}

	private static float getDefaultAllowedDeviation(OsmandSettings settings, ApplicationMode mode, float posTolerance) {
		if (mode.getRouteService() == RouteService.DIRECT_TO) {
			return -1.0f;
		} else if (mode.getRouteService() == RouteService.STRAIGHT) {
			MetricsConstants mc = settings.METRIC_SYSTEM.getModeValue(mode);
			if (mc == MetricsConstants.KILOMETERS_AND_METERS || mc == MetricsConstants.MILES_AND_METERS) {
				return 500.f;
			} else {
				// 1/4 mile
				return 482.f;
			}
		}
		return posTolerance * POS_TOLERANCE_DEVIATION_MULTIPLIER;
	}

	public static float getDefaultAllowedDeviation(OsmandSettings settings, ApplicationMode mode) {
		return getDefaultAllowedDeviation(settings, mode, getPosTolerance(0));
	}

	private void fireRoutingDataUpdateEvent() {
		if (!updateListeners.isEmpty()) {
			ArrayList<WeakReference<IRoutingDataUpdateListener>> tmp = new ArrayList<>(updateListeners);
			for (WeakReference<IRoutingDataUpdateListener> ref : tmp) {
				IRoutingDataUpdateListener l = ref.get();
				if (l != null) {
					l.onRoutingDataUpdate();
				}
			}
		}
	}

	private void fireRouteSettingsChangedEvent(@Nullable ApplicationMode mode) {
		if (!settingsListeners.isEmpty()) {
			ArrayList<WeakReference<IRouteSettingsListener>> tmp = new ArrayList<>(settingsListeners);
			for (WeakReference<IRouteSettingsListener> ref : tmp) {
				IRouteSettingsListener l = ref.get();
				if (l != null) {
					l.onRouteSettingsChanged(mode);
				}
			}
		}
	}

	public int getLeftDistance() {
		return route.getDistanceToFinish(lastFixedLocation);
	}

	public int getLeftDistanceNextIntermediate() {
		return route.getDistanceToNextIntermediate(lastFixedLocation);
	}

	public int getLeftTime() {
		return route.getLeftTime(lastFixedLocation);
	}

	public int getLeftTimeNextTurn() {
		return route.getLeftTimeToNextTurn(lastFixedLocation);
	}

	public int getLeftTimeNextIntermediate() {
		return route.getLeftTimeToNextIntermediate(lastFixedLocation);
	}

	public OsmandSettings getSettings() {
		return settings;
	}

	public String getGeneralRouteInformation() {
		int dist = getLeftDistance();
		int hours = getLeftTime() / (60 * 60);
		int minutes = (getLeftTime() / 60) % 60;
		return app.getString(R.string.route_general_information, OsmAndFormatter.getFormattedDistance(dist, app),
				hours, minutes);
	}

	public Location getLocationFromRouteDirection(RouteDirectionInfo i) {
		return route.getLocationFromRouteDirection(i);
	}

	public synchronized NextDirectionInfo getNextRouteDirectionInfo(NextDirectionInfo info, boolean toSpeak) {
		NextDirectionInfo i = route.getNextRouteDirectionInfo(info, lastProjection, toSpeak);
		if (i != null) {
			i.imminent = voiceRouter.calculateImminent(i.distanceTo, lastProjection);
		}
		return i;
	}

	public synchronized float getCurrentMaxSpeed() {
		return route.getCurrentMaxSpeed(getAppMode().getRouteTypeProfile());
	}

	@NonNull
	public synchronized CurrentStreetName getCurrentName(NextDirectionInfo n) {
		return CurrentStreetName.getCurrentName(this, n);
	}

	public RouteSegmentResult getCurrentSegmentResult() {
		return route.getCurrentSegmentResult();
	}

	public RouteSegmentResult getNextStreetSegmentResult() {
		return route.getNextStreetSegmentResult();
	}

	public List<RouteSegmentResult> getUpcomingTunnel(float distToStart) {
		return route.getUpcomingTunnel(distToStart);
	}

	public synchronized NextDirectionInfo getNextRouteDirectionInfoAfter(NextDirectionInfo previous, NextDirectionInfo to, boolean toSpeak) {
		NextDirectionInfo i = route.getNextRouteDirectionInfoAfter(previous, to, toSpeak);
		if (i != null) {
			i.imminent = voiceRouter.calculateImminent(i.distanceTo, null);
		}
		return i;
	}

	public List<RouteDirectionInfo> getRouteDirections() {
		return new ArrayList<>(route.getRouteDirections());
	}

	public void onSettingsChanged() {
		onSettingsChanged(false);
	}

	public void onSettingsChanged(boolean forceRouteRecalculation) {
		onSettingsChanged(mode, forceRouteRecalculation);
	}

	public void onSettingsChanged(@Nullable ApplicationMode mode) {
		onSettingsChanged(mode, false);
	}

	public void onSettingsChanged(@Nullable ApplicationMode mode, boolean forceRouteRecalculation) {
		if (forceRouteRecalculation ||
				((mode == null || mode.equals(this.mode)) && (isRouteCalculated() || isRouteBeingCalculated()))) {
			recalculateRouteDueToSettingsChange(true);
		}
		fireRouteSettingsChangedEvent(mode);
	}

	public void recalculateRouteDueToSettingsChange(boolean clearCurrentRoute) {
		if (clearCurrentRoute) {
			clearCurrentRoute(finalLocation, intermediatePoints);
		}
		if (isPublicTransportMode()) {
			Location start = lastFixedLocation;
			LatLon finish = finalLocation;
			transportRoutingHelper.setApplicationMode(mode);
			if (start != null && finish != null) {
				transportRoutingHelper.setFinalAndCurrentLocation(finish,
						new LatLon(start.getLatitude(), start.getLongitude()));
			} else {
				transportRoutingHelper.recalculateRouteDueToSettingsChange();
			}
		} else {
			routeRecalculationHelper.recalculateRouteInBackground(lastFixedLocation, finalLocation,
					intermediatePoints, currentGPXRoute, route, true, false);
		}
	}

	public void startRouteCalculationThread(RouteCalculationParams params) {
		routeRecalculationHelper.startRouteCalculationThread(params, true, true);
	}

	public static void applyApplicationSettings(RouteCalculationParams params, OsmandSettings settings, ApplicationMode mode) {
		params.leftSide = settings.DRIVING_REGION.get().leftHandDriving;
		params.fast = settings.FAST_ROUTE_MODE.getModeValue(mode);
	}

	public void addCalculationProgressListener(@NonNull RouteCalculationProgressListener listener) {
		routeRecalculationHelper.addCalculationProgressListener(listener);
	}

	public void removeCalculationProgressListener(@NonNull RouteCalculationProgressListener listener) {
		routeRecalculationHelper.removeCalculationProgressListener(listener);
	}

	public boolean isPublicTransportMode() {
		return mode.isDerivedRoutingFrom(ApplicationMode.PUBLIC_TRANSPORT);
	}

	public boolean isBoatMode() {
		return mode.isDerivedRoutingFrom(ApplicationMode.BOAT);
	}

	public boolean isOsmandRouting() {
		return mode.getRouteService() == RouteService.OSMAND;
	}

	public boolean isRouteBeingCalculated() {
		return routeRecalculationHelper.isRouteBeingCalculated();
	}

	private void showMessage(String msg) {
		app.runInUIThread(() -> app.showToastMessage(msg));
	}

	@NonNull
	public RouteCalculationResult getRoute() {
		return route;
	}

	public GpxFile generateGPXFileWithRoute(String name) {
		return generateGPXFileWithRoute(route, name);
	}

	public GpxFile generateGPXFileWithRoute(RouteCalculationResult route, String name) {
		return provider.createOsmandRouterGPX(route, app, name);
	}

	public RoutingEnvironment getRoutingEnvironment(OsmandApplication ctx, ApplicationMode mode, LatLon start, LatLon end) throws IOException {
		return provider.getRoutingEnvironment(ctx, mode, start, end);
	}

	public List<GpxPoint> generateGpxPoints(RoutingEnvironment env, GpxRouteApproximation gctx, LocationsHolder locationsHolder) {
		return provider.generateGpxPoints(env, gctx, locationsHolder);
	}

	public GpxRouteApproximation calculateGpxApproximation(RoutingEnvironment env, GpxRouteApproximation gctx, List<GpxPoint> points, ResultMatcher<GpxRouteApproximation> resultMatcher, boolean useExternalTimestamps) throws IOException, InterruptedException {
		return provider.calculateGpxPointsApproximation(env, gctx, points, resultMatcher, useExternalTimestamps);
	}

	public void notifyIfRouteIsCalculated() {
		if (route.isCalculated()) {
			voiceRouter.newRouteIsCalculated(true);
		}
	}
}
