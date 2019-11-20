package net.osmand.plus.routing;


import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ValueHolder;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RouteProvider.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static net.osmand.plus.notifications.OsmandNotification.NotificationType.NAVIGATION;

public class RoutingHelper {

	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(RoutingHelper.class);

	private static final float POSITION_TOLERANCE = 60;

	private List<WeakReference<IRouteInformationListener>> listeners = new LinkedList<>();
	private List<WeakReference<IRoutingDataUpdateListener>> updateListeners = new LinkedList<>();
	private OsmandApplication app;
	private TransportRoutingHelper transportRoutingHelper;

	private boolean isFollowingMode = false;
	private boolean isRoutePlanningMode = false;
	private boolean isPauseNavigation = false;

	private GPXRouteParamsBuilder currentGPXRoute = null;

	private RouteCalculationResult route = new RouteCalculationResult("");

	private LatLon finalLocation;
	private List<LatLon> intermediatePoints;
	private Location lastProjection;
	private Location lastFixedLocation;

	private static final int RECALCULATE_THRESHOLD_COUNT_CAUSING_FULL_RECALCULATE = 3;
	private static final int RECALCULATE_THRESHOLD_CAUSING_FULL_RECALCULATE_INTERVAL = 2*60*1000;
	private Thread currentRunningJob;
	private long lastTimeEvaluatedRoute = 0;
	private String lastRouteCalcError;
	private String lastRouteCalcErrorShort;
	private long recalculateCountInInterval = 0;
	private int evalWaitInterval = 0;
	private boolean waitingNextJob;
	private boolean routeWasFinished;

	private ApplicationMode mode;
	private OsmandSettings settings;

	private RouteProvider provider;
	private VoiceRouter voiceRouter;

	private static boolean isDeviatedFromRoute = false;
	private long deviateFromRouteDetected = 0;
	//private long wrongMovementDetected = 0;
	private boolean voiceRouterStopped = false;

	private RouteCalculationProgressCallback progressRoute;

//	private ProgressBar progress;
//	private Handler progressHandler;

	public boolean isDeviatedFromRoute() {
		return isDeviatedFromRoute;
	}

	public boolean isRouteWasFinished() {
		return routeWasFinished;
	}

	public RoutingHelper(OsmandApplication context){
		this.app = context;
		settings = context.getSettings();
		voiceRouter = new VoiceRouter(this, settings);
		provider = new RouteProvider();
		transportRoutingHelper = context.getTransportRoutingHelper();
		transportRoutingHelper.setRoutingHelper(this);
		setAppMode(settings.APPLICATION_MODE.get());
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
		return lastRouteCalcError;
	}

	public String getLastRouteCalcErrorShort() {
		return lastRouteCalcErrorShort;
	}

	public void setPauseNavigation(boolean b) {
		this.isPauseNavigation = b;
		if (b) {
			if (app.getNavigationService() != null) {
				app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_NAVIGATION);
			} else {
				app.getNotificationHelper().updateTopNotification();
				app.getNotificationHelper().refreshNotifications();
			}
		} else {
			app.startNavigationService(NavigationService.USED_BY_NAVIGATION, 0);
		}
	}

	public boolean isPauseNavigation() {
		return isPauseNavigation;
	}

	public void setFollowingMode(boolean follow) {
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
			app.startNavigationService(NavigationService.USED_BY_NAVIGATION, 0);
		}
	}

	public boolean isRoutePlanningMode() {
		return isRoutePlanningMode;
	}

	public void setRoutePlanningMode(boolean isRoutePlanningMode) {
		this.isRoutePlanningMode = isRoutePlanningMode;
	}

	public synchronized void setFinalAndCurrentLocation(LatLon finalLocation, List<LatLon> intermediatePoints, Location currentLocation){
		RouteCalculationResult previousRoute = route;
		clearCurrentRoute(finalLocation, intermediatePoints);
		// to update route
		setCurrentLocation(currentLocation, false, previousRoute, true);
	}

	public synchronized void clearCurrentRoute(LatLon newFinalLocation, List<LatLon> newIntermediatePoints) {
		route = new RouteCalculationResult("");
		isDeviatedFromRoute = false;
		evalWaitInterval = 0;
		app.getWaypointHelper().setNewRoute(route);
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				Iterator<WeakReference<IRouteInformationListener>> it = listeners.iterator();
				while(it.hasNext()) {
					WeakReference<IRouteInformationListener> ref = it.next();
					IRouteInformationListener l = ref.get();
					if(l == null) {
						it.remove();
					} else {
						l.routeWasCancelled();
					}
				}
			}
		});
		this.finalLocation = newFinalLocation;
		this.intermediatePoints = newIntermediatePoints;
		if(currentRunningJob instanceof RouteRecalculationThread) {
			((RouteRecalculationThread) currentRunningJob).stopCalculation();
		}
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
		routeWasFinished = true;
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				Iterator<WeakReference<IRouteInformationListener>> it = listeners.iterator();
				while(it.hasNext()) {
					WeakReference<IRouteInformationListener> ref = it.next();
					IRouteInformationListener l = ref.get();
					if(l == null) {
						it.remove();
					} else {
						l.routeWasFinished();
					}
				}
			}
		});
	}

	public GPXRouteParamsBuilder getCurrentGPXRoute() {
		return currentGPXRoute;
	}



	public void setGpxParams(GPXRouteParamsBuilder params) {
		currentGPXRoute = params;
	}

	public List<Location> getCurrentCalculatedRoute() {
		return route.getImmutableAllLocations();
	}

	public void setAppMode(ApplicationMode mode){
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

	public boolean isRouteCalculated(){
		return route.isCalculated();
	}

	public VoiceRouter getVoiceRouter() {
		return voiceRouter;
	}

	public Location getLastProjection(){
		return lastProjection;
	}


	public void addRouteDataListener(IRoutingDataUpdateListener listener) {
		updateListeners = updateListenersList(new ArrayList<>(updateListeners), listener, true);
	}

	public void removeRouteDataListener(IRoutingDataUpdateListener listener) {
		updateListeners = updateListenersList(new ArrayList<>(updateListeners), listener, false);
	}

	private List<WeakReference<IRoutingDataUpdateListener>> updateListenersList(
		List<WeakReference<IRoutingDataUpdateListener>> copyList,
		IRoutingDataUpdateListener listener, boolean isNewListener) {
		Iterator<WeakReference<IRoutingDataUpdateListener>> it = copyList.iterator();
		while (it.hasNext()) {
			WeakReference<IRoutingDataUpdateListener> ref = it.next();
			IRoutingDataUpdateListener l = ref.get();
			if (l == null || l == listener) {
				it.remove();
			}
		}
		if (isNewListener) {
			copyList.add(new WeakReference<>(listener));
		}
		return copyList;
	}

	public void addListener(IRouteInformationListener l){
		listeners = updateInformationListeners(new ArrayList<>(listeners), l, true);
		transportRoutingHelper.addListener(l);
	}

	public void removeListener(IRouteInformationListener lt){
		listeners = updateInformationListeners(new ArrayList<>(listeners), lt, false);
	}

	private List<WeakReference<IRouteInformationListener>> updateInformationListeners(
		List<WeakReference<IRouteInformationListener>> copyList,
		IRouteInformationListener listener, boolean isNewListener) {
		Iterator<WeakReference<IRouteInformationListener>> it = copyList.iterator();
		while (it.hasNext()) {
			WeakReference<IRouteInformationListener> ref = it.next();
			IRouteInformationListener l = ref.get();
			if (l == null || l == listener) {
				it.remove();
			}
		}

		if (isNewListener) {
			copyList.add(new WeakReference<>(listener));
		}
		return copyList;
	}


	public void updateLocation(Location currentLocation) {
		if (settings.getPointToStart() == null && settings.getMyLocationToStart() == null && currentLocation != null) {
			app.getTargetPointsHelper().setMyLocationPoint(
					new LatLon(currentLocation.getLatitude(), currentLocation.getLongitude()), false, null);
		}
		if(isFollowingMode() || (settings.getPointToStart() == null && isRoutePlanningMode) ||
				app.getLocationProvider().getLocationSimulation().isRouteAnimating()) {
			setCurrentLocation(currentLocation, false);
		}
	}

	public Location setCurrentLocation(Location currentLocation, boolean returnUpdatedLocation) {
		return setCurrentLocation(currentLocation, returnUpdatedLocation, route, false);
	}

	public double getRouteDeviation(){
		if (route == null ||
			route.getImmutableAllDirections().size() < 2 ||
			route.currentRoute == 0){
			return 0;
		}
		List<Location> routeNodes = route.getImmutableAllLocations();
		return getOrthogonalDistance(lastFixedLocation, routeNodes.get(route.currentRoute -1), routeNodes.get(route.currentRoute));
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
		float posTolerance = POSITION_TOLERANCE;
		if(currentLocation.hasAccuracy()) {
			posTolerance = POSITION_TOLERANCE / 2 + currentLocation.getAccuracy();
		}
		boolean calculateRoute = false;
		synchronized (this) {
			isDeviatedFromRoute = false;
			double distOrth = 0;

			// 0. Route empty or needs to be extended? Then re-calculate route.
			if(route.isEmpty()) {
				calculateRoute = true;
			} else {
				// 1. Update current route position status according to latest received location
				boolean finished = updateCurrentRouteStatus(currentLocation, posTolerance);
				if (finished) {
					return null;
				}
				List<Location> routeNodes = route.getImmutableAllLocations();
				int currentRoute = route.currentRoute;

				// 2. Analyze if we need to recalculate route
				// >100m off current route (sideways)
				if (currentRoute > 0) {
					distOrth = getOrthogonalDistance(currentLocation, routeNodes.get(currentRoute - 1), routeNodes.get(currentRoute));
					if ((!settings.DISABLE_OFFROUTE_RECALC.get()) && (distOrth > (1.7 * posTolerance))) {
						log.info("Recalculate route, because correlation  : " + distOrth); //$NON-NLS-1$
						isDeviatedFromRoute = true;
						calculateRoute = true;
					}
				}
				// 3. Identify wrong movement direction
				Location next = route.getNextRouteLocation();
				boolean wrongMovementDirection = checkWrongMovementDirection(currentLocation, next);
				if ((!settings.DISABLE_WRONG_DIRECTION_RECALC.get()) && wrongMovementDirection && (currentLocation.distanceTo(routeNodes.get(currentRoute)) > (2 * posTolerance))) {
					log.info("Recalculate route, because wrong movement direction: " + currentLocation.distanceTo(routeNodes.get(currentRoute))); //$NON-NLS-1$
					isDeviatedFromRoute = true;
					calculateRoute = true;
				}
				// 4. Identify if UTurn is needed
				if (identifyUTurnIsNeeded(currentLocation, posTolerance)) {
					isDeviatedFromRoute = true;
				}
				// 5. Update Voice router
				// Do not update in route planning mode
				if (isFollowingMode) {
					boolean inRecalc = calculateRoute || isRouteBeingCalculated();
					if (!inRecalc && !wrongMovementDirection) {
						voiceRouter.updateStatus(currentLocation, false);
						voiceRouterStopped = false;
					} else if (isDeviatedFromRoute && !voiceRouterStopped) {
						voiceRouter.interruptRouteCommands();
						voiceRouterStopped = true; // Prevents excessive execution of stop() code
					}
					if (distOrth > mode.getOffRouteDistance()) {
						voiceRouter.announceOffRoute(distOrth);
					}
				}

				// calculate projection of current location
				if (currentRoute > 0) {
					locationProjection = new Location(currentLocation);
					Location nextLocation = routeNodes.get(currentRoute);
					LatLon project = getProject(currentLocation, routeNodes.get(currentRoute - 1), routeNodes.get(currentRoute));

					locationProjection.setLatitude(project.getLatitude());
					locationProjection.setLongitude(project.getLongitude());
					// we need to update bearing too
					float bearingTo = locationProjection.bearingTo(nextLocation);
					locationProjection.setBearing(bearingTo);
				}
			}
			lastFixedLocation = currentLocation;
			lastProjection = locationProjection;
		}

		if (calculateRoute) {
			recalculateRouteInBackground(currentLocation, finalLocation, intermediatePoints, currentGPXRoute,
					previousRoute.isCalculated() ? previousRoute : null, false, !targetPointsChanged);
		} else {
			Thread job = currentRunningJob;
			if(job instanceof RouteRecalculationThread) {
				RouteRecalculationThread thread = (RouteRecalculationThread) job;
				if(!thread.isParamsChanged()) {
					thread.stopCalculation();
				}
				if (isFollowingMode){
					voiceRouter.announceBackOnRoute();
				}
			}
		}

		double projectDist = mode != null && mode.hasFastSpeed() ? posTolerance : posTolerance / 2;
		if(returnUpdatedLocation && locationProjection != null && currentLocation.distanceTo(locationProjection) < projectDist) {
			return locationProjection;
		} else {
			return currentLocation;
		}
	}

	private static double getOrthogonalDistance(Location loc, Location from, Location to) {
		return MapUtils.getOrthogonalDistance(loc.getLatitude(),
				loc.getLongitude(), from.getLatitude(), from.getLongitude(),
				to.getLatitude(), to.getLongitude());
	}

	private static LatLon getProject(Location loc, Location from, Location to) {
		return MapUtils.getProjection(loc.getLatitude(),
				loc.getLongitude(), from.getLatitude(), from.getLongitude(),
				to.getLatitude(), to.getLongitude());
	}

	private static int lookAheadFindMinOrthogonalDistance(Location currentLocation, List<Location> routeNodes, int currentRoute, int iterations) {
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

	private boolean updateCurrentRouteStatus(Location currentLocation, float posTolerance) {
		List<Location> routeNodes = route.getImmutableAllLocations();
		int currentRoute = route.currentRoute;
		// 1. Try to proceed to next point using orthogonal distance (finding minimum orthogonal dist)
		while (currentRoute + 1 < routeNodes.size()) {
			double dist = currentLocation.distanceTo(routeNodes.get(currentRoute));
			if(currentRoute > 0) {
				dist = getOrthogonalDistance(currentLocation, routeNodes.get(currentRoute - 1),
						routeNodes.get(currentRoute));
			}
			boolean processed = false;
			// if we are still too far try to proceed many points
			// if not then look ahead only 3 in order to catch sharp turns
			boolean longDistance = dist >= 250;
			int newCurrentRoute = lookAheadFindMinOrthogonalDistance(currentLocation, routeNodes, currentRoute, longDistance ? 15 : 8);
			double newDist = getOrthogonalDistance(currentLocation, routeNodes.get(newCurrentRoute),
					routeNodes.get(newCurrentRoute + 1));
			if(longDistance) {
				if(newDist < dist) {
					if (log.isDebugEnabled()) {
						log.debug("Processed by distance : (new) " + newDist + " (old) " + dist); //$NON-NLS-1$//$NON-NLS-2$
					}
					processed = true;
				}
			} else if (newDist < dist || newDist < 10) {
				// newDist < 10 (avoid distance 0 till next turn)
				if (dist > posTolerance) {
					processed = true;
					if (log.isDebugEnabled()) {
						log.debug("Processed by distance : " + newDist + " " + dist); //$NON-NLS-1$//$NON-NLS-2$
					}
				} else {
					// case if you are getting close to the next point after turn
					// but you have not yet turned (could be checked bearing)
					if (currentLocation.hasBearing() || lastFixedLocation != null) {
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
				route.updateCurrentRoute(newCurrentRoute + 1);
				currentRoute = newCurrentRoute + 1;
				app.getNotificationHelper().refreshNotification(NotificationType.NAVIGATION);
				if (!updateListeners.isEmpty()) {
					ArrayList<WeakReference<IRoutingDataUpdateListener>> tmp = new ArrayList<>(updateListeners);
					for (WeakReference<IRoutingDataUpdateListener> ref : tmp) {
						IRoutingDataUpdateListener l = ref.get();
						if (l != null) {
							l.onRoutingDataUpdate();
						}
					}
				}
			} else {
				break;
			}
		}

		// 2. check if intermediate found
		if(route.getIntermediatePointsToPass()  > 0
				&& route.getDistanceToNextIntermediate(lastFixedLocation) < getArrivalDistance() * 2f && !isRoutePlanningMode) {
			showMessage(app.getString(R.string.arrived_at_intermediate_point));
			route.passIntermediatePoint();
			TargetPointsHelper targets = app.getTargetPointsHelper();
			String name = "";
			if(intermediatePoints != null && !intermediatePoints.isEmpty()) {
				LatLon rm = intermediatePoints.remove(0);
				List<TargetPoint> ll = targets.getIntermediatePointsNavigation();
				int ind = -1;
				for(int i = 0; i < ll.size(); i++) {
					if(ll.get(i).point != null && MapUtils.getDistance(ll.get(i).point, rm) < 5) {
						name = ll.get(i).getOnlyName();
						ind = i;
						break;
					}
				}
				if(ind >= 0) {
					targets.removeWayPoint(false, ind);
				}
			}
			if(isFollowingMode) {
				voiceRouter.arrivedIntermediatePoint(name);
			}
			// double check
			while(intermediatePoints != null  && route.getIntermediatePointsToPass() < intermediatePoints.size()) {
				intermediatePoints.remove(0);
			}
		}

		// 3. check if destination found
		Location lastPoint = routeNodes.get(routeNodes.size() - 1);
		if (currentRoute > routeNodes.size() - 3
				&& currentLocation.distanceTo(lastPoint) < getArrivalDistance()
				&& !isRoutePlanningMode) {
			//showMessage(app.getString(R.string.arrived_at_destination));
			TargetPointsHelper targets = app.getTargetPointsHelper();
			TargetPoint tp = targets.getPointToNavigate();
			String description = tp == null ? "" : tp.getOnlyName();
			if(isFollowingMode) {
				voiceRouter.arrivedDestinationPoint(description);
			}
			boolean onDestinationReached = OsmandPlugin.onDestinationReached();
			onDestinationReached &= app.getAppCustomization().onDestinationReached();
			if (onDestinationReached) {
				clearCurrentRoute(null, null);
				setRoutePlanningMode(false);
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						settings.LAST_ROUTING_APPLICATION_MODE = settings.APPLICATION_MODE.get();
						//settings.APPLICATION_MODE.set(settings.DEFAULT_APPLICATION_MODE.get());
					}
				});
				finishCurrentRoute();
				// targets.clearPointToNavigate(false);
				return true;
			}

		}
		return false;
	}

	private float getArrivalDistance() {
		return ((float)settings.getApplicationMode().getArrivalDistance()) * settings.ARRIVAL_DISTANCE_FACTOR.get();
	}


	private boolean identifyUTurnIsNeeded(Location currentLocation, float posTolerance) {
		if (finalLocation == null || currentLocation == null || !route.isCalculated() || isPublicTransportMode()) {
			return false;
		}
		boolean isOffRoute = false;
		if (currentLocation.hasBearing()) {
			float bearingMotion = currentLocation.getBearing() ;
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
					if (deviateFromRouteDetected == 0) {
						deviateFromRouteDetected = System.currentTimeMillis();
					} else if ((System.currentTimeMillis() - deviateFromRouteDetected > 10000)) {
						isOffRoute = true;
						//log.info("bearingMotion is opposite to bearingRoute"); //$NON-NLS-1$
					}
				}
			} else {
				deviateFromRouteDetected = 0;
			}
		}
		return isOffRoute;
	}

	/**
	 * Wrong movement direction is considered when between
	 * current location bearing (determines by 2 last fixed position or provided)
	 * and bearing from currentLocation to next (current) point
	 * the difference is more than 60 degrees
	 */
	public boolean checkWrongMovementDirection(Location currentLocation, Location nextRouteLocation) {
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

	private void setNewRoute(RouteCalculationResult prevRoute, final RouteCalculationResult res, Location start){
		final boolean newRoute = !prevRoute.isCalculated();
		if (isFollowingMode) {
			if(lastFixedLocation != null) {
				start = lastFixedLocation;
			}
			// try remove false route-recalculated prompts by checking direction to second route node
			boolean wrongMovementDirection  = false;
			List<Location> routeNodes = res.getImmutableAllLocations();
			if (routeNodes != null && !routeNodes.isEmpty()) {
				int newCurrentRoute = lookAheadFindMinOrthogonalDistance(start, routeNodes, res.currentRoute, 15);
				if (newCurrentRoute + 1 < routeNodes.size()) {
					// This check is valid for Online/GPX services (offline routing is aware of route direction)
					wrongMovementDirection = checkWrongMovementDirection(start, routeNodes.get(newCurrentRoute + 1));
					// set/reset evalWaitInterval only if new route is in forward direction
					if (wrongMovementDirection) {
						evalWaitInterval = 3000;
					} else {
						evalWaitInterval = Math.max(3000, evalWaitInterval * 3 / 2);
						evalWaitInterval = Math.min(evalWaitInterval, 120000);
					}

				}
			}


			// trigger voice prompt only if new route is in forward direction
			// If route is in wrong direction after one more setLocation it will be recalculated
			if (!wrongMovementDirection || newRoute) {
				voiceRouter.newRouteIsCalculated(newRoute);
			}
		}
		app.getWaypointHelper().setNewRoute(res);

		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
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
				if (showToast.value && OsmandPlugin.isDevelopment()) {
					String msg = app.getString(R.string.new_route_calculated_dist_dbg,
							OsmAndFormatter.getFormattedDistance(res.getWholeDistance(), app),

							((int)res.getRoutingTime()) + " sec",
							res.getCalculateTime(), res.getVisitedSegments(), res.getLoadedTiles());
					app.showToastMessage(msg);
				}
			}
		});
	}

	public int getLeftDistance(){
		return route.getDistanceToFinish(lastFixedLocation);
	}

	public int getLeftDistanceNextIntermediate() {
		return route.getDistanceToNextIntermediate(lastFixedLocation);
	}

	public int getLeftTime() {
		return route.getLeftTime(lastFixedLocation);
	}

	public int getLeftTimeNextIntermediate() {
		return route.getLeftTimeToNextIntermediate(lastFixedLocation);
	}

	public OsmandSettings getSettings() {
		return settings;
	}

	public String getGeneralRouteInformation(){
		int dist = getLeftDistance();
		int hours = getLeftTime() / (60 * 60);
		int minutes = (getLeftTime() / 60) % 60;
		return app.getString(R.string.route_general_information, OsmAndFormatter.getFormattedDistance(dist, app),
				hours, minutes);
	}

	public Location getLocationFromRouteDirection(RouteDirectionInfo i){
		return route.getLocationFromRouteDirection(i);
	}

	public synchronized NextDirectionInfo getNextRouteDirectionInfo(NextDirectionInfo info, boolean toSpeak){
		NextDirectionInfo i = route.getNextRouteDirectionInfo(info, lastProjection, toSpeak);
		if(i != null) {
			i.imminent =  voiceRouter.calculateImminent(i.distanceTo, lastProjection);
		}
		return i;
	}

	public synchronized float getCurrentMaxSpeed() {
		return route.getCurrentMaxSpeed();
	}


	public static String formatStreetName(String name, String ref, String destination, String towards) {
	//Hardy, 2016-08-05:
	//Now returns: (ref) + ((" ")+name) + ((" ")+"toward "+dest) or ""

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

//	protected boolean isDistanceLess(float currentSpeed, double dist, double etalon, float defSpeed){
//		if(dist < etalon || ((dist / currentSpeed) < (etalon / defSpeed))){
//			return true;
//		}
//		return false;
//	}

	public synchronized String getCurrentName(TurnType[] next){
		NextDirectionInfo n = getNextRouteDirectionInfo(new NextDirectionInfo(), true);
		Location l = lastFixedLocation;
		float speed = 0;
		if(l != null && l.hasSpeed()) {
			speed = l.getSpeed();
		}
		if(n.distanceTo > 0  && n.directionInfo != null && !n.directionInfo.getTurnType().isSkipToSpeak() &&
				voiceRouter.isDistanceLess(speed, n.distanceTo, voiceRouter.PREPARE_DISTANCE * 0.75f, 0f)) {
			String nm = n.directionInfo.getStreetName();
			String rf = n.directionInfo.getRef();
			String dn = n.directionInfo.getDestinationName();
			if(next != null) {
				next[0] = n.directionInfo.getTurnType();
			}
			return formatStreetName(nm, rf, dn, "»");
		}
		RouteSegmentResult rs = getCurrentSegmentResult();
		if(rs != null) {
			String name = getRouteSegmentStreetName(rs);
			if (!Algorithms.isEmpty(name)) {
				return name;
			}
		}
		rs = getNextStreetSegmentResult();
		if(rs != null) {
			String name = getRouteSegmentStreetName(rs);
			if (!Algorithms.isEmpty(name)) {
				if(next != null) {
					next[0] = TurnType.valueOf(TurnType.C, false);
				}
				return name;
			}
		}
		return null;
	}

	private String getRouteSegmentStreetName(RouteSegmentResult rs) {
		String nm = rs.getObject().getName(settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get());
		String rf = rs.getObject().getRef(settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get(), rs.isForwardDirection());
		String dn = rs.getObject().getDestinationName(settings.MAP_PREFERRED_LOCALE.get(),
				settings.MAP_TRANSLITERATE_NAMES.get(), rs.isForwardDirection());
		return formatStreetName(nm, rf, dn, "»");
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

    public synchronized NextDirectionInfo getNextRouteDirectionInfoAfter(NextDirectionInfo previous, NextDirectionInfo to, boolean toSpeak){
		NextDirectionInfo i = route.getNextRouteDirectionInfoAfter(previous, to, toSpeak);
		if(i != null) {
			i.imminent =  voiceRouter.calculateImminent(i.distanceTo, null);
		}
		return i;
	}

	public List<RouteDirectionInfo> getRouteDirections(){
		return route.getRouteDirections();
	}



	private class RouteRecalculationThread extends Thread {

		private final RouteCalculationParams params;
		private boolean paramsChanged;
		private Thread prevRunningJob;

		public RouteRecalculationThread(String name, RouteCalculationParams params, boolean paramsChanged) {
			super(name);
			this.params = params;
			this.paramsChanged = paramsChanged;
			if(params.calculationProgress == null) {
				params.calculationProgress = new RouteCalculationProgress();
			}
		}

		public boolean isParamsChanged() {
			return paramsChanged;
		}

		public void stopCalculation(){
			params.calculationProgress.isCancelled = true;
		}


		@Override
		public void run() {
			synchronized (RoutingHelper.this) {
				routeWasFinished = false;
				currentRunningJob = this;
				waitingNextJob = prevRunningJob != null;
			}
			if(prevRunningJob != null) {
				while(prevRunningJob.isAlive()){
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				synchronized (RoutingHelper.this) {
					currentRunningJob = this;
					waitingNextJob = false;
				}
			}
			lastRouteCalcError = null;
			lastRouteCalcErrorShort = null;
			RouteCalculationResult res = provider.calculateRouteImpl(params);
			if (params.calculationProgress.isCancelled) {
				synchronized (RoutingHelper.this) {
					currentRunningJob = null;
				}
				return;
			}
			final boolean onlineSourceWithoutInternet = !res.isCalculated() &&
					params.mode.getRouteService().isOnline() && !settings.isInternetConnectionAvailable();
			if (onlineSourceWithoutInternet && settings.GPX_ROUTE_CALC_OSMAND_PARTS.get()) {
				if (params.previousToRecalculate != null && params.previousToRecalculate.isCalculated()) {
					res = provider.recalculatePartOfflineRoute(res, params);
				}
			}
			RouteCalculationResult prev = route;
			synchronized (RoutingHelper.this) {
				if (res.isCalculated()) {
					if (!params.inSnapToRoadMode && !params.inPublicTransportMode) {
						route = res;
					}
					if (params.resultListener != null) {
						params.resultListener.onRouteCalculated(res);
					}
				} else {
					evalWaitInterval = Math.max(3000, evalWaitInterval * 3 / 2); // for Issue #3899
					evalWaitInterval = Math.min(evalWaitInterval, 120000);
				}
				currentRunningJob = null;
			}
			if(res.isCalculated()){
				if (!params.inSnapToRoadMode && !params.inPublicTransportMode) {
					setNewRoute(prev, res, params.start);
				}
			} else if (onlineSourceWithoutInternet) {
				lastRouteCalcError = app.getString(R.string.error_calculating_route)
						+ ":\n" + app.getString(R.string.internet_connection_required_for_online_route);
				lastRouteCalcErrorShort = app.getString(R.string.error_calculating_route);
				showMessage(lastRouteCalcError); //$NON-NLS-1$
			} else {
				if (res.getErrorMessage() != null) {
					lastRouteCalcError = app.getString(R.string.error_calculating_route) + ":\n" + res.getErrorMessage();
					lastRouteCalcErrorShort = app.getString(R.string.error_calculating_route);
					showMessage(lastRouteCalcError); //$NON-NLS-1$
				} else {
					lastRouteCalcError = app.getString(R.string.empty_route_calculated);
					lastRouteCalcErrorShort = app.getString(R.string.empty_route_calculated);
					showMessage(lastRouteCalcError);
				}
			}
			app.getNotificationHelper().refreshNotification(NAVIGATION);
			lastTimeEvaluatedRoute = System.currentTimeMillis();
		}

		public void setWaitPrevJob(Thread prevRunningJob) {
			this.prevRunningJob = prevRunningJob;
		}
	}

	public void recalculateRouteDueToSettingsChange() {
		clearCurrentRoute(finalLocation, intermediatePoints);
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
			recalculateRouteInBackground(lastFixedLocation, finalLocation, intermediatePoints, currentGPXRoute, route, true, false);
		}
	}

	private void recalculateRouteInBackground(final Location start, final LatLon end, final List<LatLon> intermediates,
			final GPXRouteParamsBuilder gpxRoute, final RouteCalculationResult previousRoute, boolean paramsChanged, boolean onlyStartPointChanged){
		if (start == null || end == null) {
			return;
		}
		// do not evaluate very often
		if ((currentRunningJob == null && System.currentTimeMillis() - lastTimeEvaluatedRoute > evalWaitInterval)
				|| paramsChanged || !onlyStartPointChanged) {
			if(System.currentTimeMillis() - lastTimeEvaluatedRoute < RECALCULATE_THRESHOLD_CAUSING_FULL_RECALCULATE_INTERVAL) {
				recalculateCountInInterval ++;
			}
			final RouteCalculationParams params = new RouteCalculationParams();
			params.start = start;
			params.end = end;
			params.intermediates = intermediates;
			params.gpxRoute = gpxRoute == null ? null : gpxRoute.build(start, settings);
			params.onlyStartPointChanged = onlyStartPointChanged;
			if (recalculateCountInInterval < RECALCULATE_THRESHOLD_COUNT_CAUSING_FULL_RECALCULATE
					|| (gpxRoute != null && gpxRoute.isPassWholeRoute() && isDeviatedFromRoute)) {
				params.previousToRecalculate = previousRoute;
			} else {
				recalculateCountInInterval = 0;
			}
			params.leftSide = settings.DRIVING_REGION.get().leftHandDriving;
			params.fast = settings.FAST_ROUTE_MODE.getModeValue(mode);
			params.mode = mode;
			params.ctx = app;
			boolean updateProgress = false;
			if (params.mode.getRouteService() == RouteService.OSMAND) {
				params.calculationProgress = new RouteCalculationProgress();
				updateProgress = true;
			} else {
				params.resultListener = new RouteCalculationParams.RouteCalculationResultListener() {
					@Override
					public void onRouteCalculated(RouteCalculationResult route) {
						app.runInUIThread(new Runnable() {

							@Override
							public void run() {
								finishProgress(params);
							}
						});
					}
				};
			}
			startRouteCalculationThread(params, paramsChanged, updateProgress);
		}
	}

	public void startRouteCalculationThread(RouteCalculationParams params, boolean paramsChanged, boolean updateProgress) {
		synchronized (this) {
			final Thread prevRunningJob = currentRunningJob;
			getSettings().LAST_ROUTE_APPLICATION_MODE.set(getAppMode());
			RouteRecalculationThread newThread = new RouteRecalculationThread(
					"Calculating route", params, paramsChanged); //$NON-NLS-1$
			currentRunningJob = newThread;
			startProgress(params);
			if (updateProgress) {
				updateProgress(params);
			}
			if (prevRunningJob != null) {
				newThread.setWaitPrevJob(prevRunningJob);
			}
			currentRunningJob.start();
		}
	}

	private void startProgress(final RouteCalculationParams params) {
		if (params.calculationProgressCallback != null) {
			params.calculationProgressCallback.start();
		} else if (progressRoute != null) {
			progressRoute.start();
		}
	}

	private void updateProgress(final RouteCalculationParams params) {
		final RouteCalculationProgressCallback progressRoute;
		if (params.calculationProgressCallback != null) {
			progressRoute = params.calculationProgressCallback;
		} else {
			progressRoute = this.progressRoute;
		}
		if (progressRoute != null ) {
			app.runInUIThread(new Runnable() {

				@Override
				public void run() {
					RouteCalculationProgress calculationProgress = params.calculationProgress;
					if (isRouteBeingCalculated()) {
						float pr = calculationProgress.getLinearProgress();
						progressRoute.updateProgress((int) pr);
						Thread t = currentRunningJob;
						if(t instanceof RouteRecalculationThread && ((RouteRecalculationThread) t).params != params) {
							// different calculation started
							return;
						} else {
							if (calculationProgress.requestPrivateAccessRouting) {
								progressRoute.requestPrivateAccessRouting();
							}
							updateProgress(params);
						}
					} else {
						if (calculationProgress.requestPrivateAccessRouting) {
							progressRoute.requestPrivateAccessRouting();
						}
						progressRoute.finish();
					}
				}
			}, 300);
		}
	}

	private void finishProgress(RouteCalculationParams params) {
		final RouteCalculationProgressCallback progressRoute;
		if (params.calculationProgressCallback != null) {
			progressRoute = params.calculationProgressCallback;
		} else {
			progressRoute = this.progressRoute;
		}
		if (progressRoute != null ) {
			progressRoute.finish();
		}
	}

	public static void applyApplicationSettings(RouteCalculationParams params, OsmandSettings settings, ApplicationMode mode) {
		params.leftSide = settings.DRIVING_REGION.get().leftHandDriving;
		params.fast = settings.FAST_ROUTE_MODE.getModeValue(mode);
	}

	public void setProgressBar(RouteCalculationProgressCallback progressRoute) {
		this.progressRoute = progressRoute;
	}

	public interface RouteCalculationProgressCallback {

		void start();

		void updateProgress(int progress);

		void requestPrivateAccessRouting();

		void finish();
	}

	public boolean isPublicTransportMode() {
		return mode.isDerivedRoutingFrom(ApplicationMode.PUBLIC_TRANSPORT);
	}

	public boolean isOsmandRouting() {
		return mode.getRouteService() == RouteService.OSMAND;
	}

	public boolean isRouteBeingCalculated() {
		return currentRunningJob instanceof RouteRecalculationThread || waitingNextJob;
	}

	private void showMessage(final String msg){
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				app.showToastMessage(msg);
			}
		});
	}


	// NEVER returns null
	public RouteCalculationResult getRoute() {
		return route;
	}

	public GPXFile generateGPXFileWithRoute(String name){
		return provider.createOsmandRouterGPX(route, app, name);
	}


	public void notifyIfRouteIsCalculated() {
		if(route.isCalculated()) {
			voiceRouter.newRouteIsCalculated(true)	;
		}
	}


}
