package net.osmand.plus.routing;


import java.util.ArrayList;
import java.util.List;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RouteProvider.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class RoutingHelper {
	
	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(RoutingHelper.class);
	
	public static interface IRouteInformationListener {
		
		public void newRouteIsCalculated(boolean newRoute);
		
		public void routeWasCancelled();
	}
	
	private static final float POSITION_TOLERANCE = 60;
	
	private List<IRouteInformationListener> listeners = new ArrayList<IRouteInformationListener>();

	private OsmandApplication app;
	
	private boolean isFollowingMode = false;
	private boolean isRoutePlanningMode = false;
	private boolean isPauseNavigation = false;
	
	private GPXRouteParamsBuilder currentGPXRoute = null;

	private RouteCalculationResult route = new RouteCalculationResult("");
	
	private LatLon finalLocation;
	private List<LatLon> intermediatePoints;
	private Location lastProjection;
	private Location lastFixedLocation;
	
	private Thread currentRunningJob;
	private long lastTimeEvaluatedRoute = 0;
	private int evalWaitInterval = 3000;
	
	private ApplicationMode mode;
	private OsmandSettings settings;
	
	private RouteProvider provider = new RouteProvider();
	private VoiceRouter voiceRouter;

	private boolean isDeviatedFromRoute = false;
	private long deviateFromRouteDetected = 0;
	//private long wrongMovementDetected = 0;

	private RouteCalculationProgressCallback progressRoute;


//	private ProgressBar progress;
//	private Handler progressHandler;

	public boolean isDeviatedFromRoute() {
		return isDeviatedFromRoute;
	}

	public RoutingHelper(OsmandApplication context, CommandPlayer player){
		this.app = context;
		settings = context.getSettings();
		voiceRouter = new VoiceRouter(this, settings, player);
	}
	

	public boolean isFollowingMode() {
		return isFollowingMode;
	}
	
	public OsmandApplication getApplication() {
		return app;
	}
	
	public void setPauseNaviation(boolean b) {
		this.isPauseNavigation = b;
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

	
	
	public synchronized void setFinalAndCurrentLocation(LatLon finalLocation, List<LatLon> intermediatePoints, Location currentLocation){
		RouteCalculationResult previousRoute = route;
		clearCurrentRoute(finalLocation, intermediatePoints);
		// to update route
		setCurrentLocation(currentLocation, false, previousRoute);
	}
	
	public synchronized void clearCurrentRoute(LatLon newFinalLocation, List<LatLon> newIntermediatePoints) {
		route = new RouteCalculationResult("");
		isDeviatedFromRoute = false;
		evalWaitInterval = 3000;
		app.getWaypointHelper().setNewRoute(route);
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				for (IRouteInformationListener l : listeners) {
					l.routeWasCancelled();
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
	
	public void addListener(IRouteInformationListener l){
		listeners.add(l);
	}
	
	public boolean removeListener(IRouteInformationListener l){
		return listeners.remove(l);
	}
	
	public void updateLocation(Location currentLocation) {
		if(isFollowingMode() || (settings.getPointToStart() == null && isRoutePlanningMode) ||
				app.getLocationProvider().getLocationSimulation().isRouteAnimating()) {
			setCurrentLocation(currentLocation, false);
		}
	}
	
	public Location setCurrentLocation(Location currentLocation, boolean returnUpdatedLocation) {
		return setCurrentLocation(currentLocation, returnUpdatedLocation, route);
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
	
	private Location setCurrentLocation(Location currentLocation, boolean returnUpdatedLocation, RouteCalculationResult previousRoute) {
		Location locationProjection = currentLocation;
		if (finalLocation == null || currentLocation == null) {
			isDeviatedFromRoute = false;
			return locationProjection;
		}
		float posTolerance = POSITION_TOLERANCE;
		if(currentLocation.hasAccuracy()) {
			posTolerance = POSITION_TOLERANCE / 2 + currentLocation.getAccuracy();
		}
		boolean calculateRoute = false;
		synchronized (this) {
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
					double dist = getOrthogonalDistance(currentLocation, routeNodes.get(currentRoute - 1), routeNodes.get(currentRoute));
					if (dist > 1.7 * posTolerance) {
						log.info("Recalculate route, because correlation  : " + dist); //$NON-NLS-1$
						calculateRoute = true;
					}
					if(dist > 350) {
						voiceRouter.announceOffRoute(dist);
					}
				}
				// 3. Identify wrong movement direction
				Location next = route.getNextRouteLocation();
				boolean wrongMovementDirection = checkWrongMovementDirection(currentLocation, next);
				if (wrongMovementDirection && currentLocation.distanceTo(routeNodes.get(currentRoute)) >  2 * posTolerance) {
					log.info("Recalculate route, because wrong movement direction: " + currentLocation.distanceTo(routeNodes.get(currentRoute))); //$NON-NLS-1$
					calculateRoute = true;
				}
				// 4. Identify if UTurn is needed
				boolean uTurnIsNeeded = identifyUTurnIsNeeded(currentLocation, posTolerance);
				// 5. Update Voice router
				if (isFollowingMode) {
					// don't update in route planing mode
					boolean inRecalc = calculateRoute || isRouteBeingCalculated();
					if (!inRecalc && !wrongMovementDirection) {
						voiceRouter.updateStatus(currentLocation, false);
					} else if(isDeviatedFromRoute){
						voiceRouter.interruptRouteCommands();
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
			recalculateRouteInBackground(false, currentLocation, finalLocation, intermediatePoints, currentGPXRoute, 
					previousRoute.isCalculated() ? previousRoute : null, false);
		} else {
			Thread job = currentRunningJob;
			if(job instanceof RouteRecalculationThread) {
				RouteRecalculationThread thread = (RouteRecalculationThread) job;
				if(!thread.isParamsChanged()) {
					thread.stopCalculation();
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
			} else {
				break;
			}
		}
		
		// 2. check if intermediate found
		if(route.getIntermediatePointsToPass()  > 0 && route.getDistanceToNextIntermediate(lastFixedLocation) < POSITION_TOLERANCE * 2) {
			showMessage(app.getString(R.string.arrived_at_intermediate_point));
			route.passIntermediatePoint();
			
			TargetPointsHelper targets = app.getTargetPointsHelper();
			List<TargetPoint> ns = targets.getIntermediatePoints();
			int toDel = targets.getIntermediatePoints().size() - route.getIntermediatePointsToPass();
			int currentIndex = toDel - 1; 
			String name = currentIndex  < 0 || currentIndex  >= ns.size() || 
					ns.get(currentIndex ) == null ? "" : ns.get(currentIndex ).name;
			if(isFollowingMode) {
				voiceRouter.arrivedIntermediatePoint(name);
			}
			while(toDel > 0) {
				targets.removeWayPoint(false, 0);
				toDel--;
			}
			
			while(intermediatePoints != null  && route.getIntermediatePointsToPass() < intermediatePoints.size()) {
				intermediatePoints.remove(0);
			}
		}

		// 3. check if destination found
		Location lastPoint = routeNodes.get(routeNodes.size() - 1);
		if (currentRoute > routeNodes.size() - 3 && currentLocation.distanceTo(lastPoint) < (((float)settings.getApplicationMode().getArrivalDistance()) * settings.ARRIVAL_DISTANCE_FACTOR.get())) {
			showMessage(app.getString(R.string.arrived_at_destination));
			TargetPointsHelper targets = app.getTargetPointsHelper();
			TargetPoint tp = targets.getPointToNavigate();
			String description = tp == null ? "" : tp.name; 
			if(isFollowingMode) {
				voiceRouter.arrivedDestinationPoint(description);
			}
			if (OsmandPlugin.onDestinationReached()) {
				clearCurrentRoute(null, null);
				setRoutePlanningMode(false);
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						settings.APPLICATION_MODE.set(settings.DEFAULT_APPLICATION_MODE.get());
					}
				});
				// targets.clearPointToNavigate(false);
				return true;
			}

			if(app.getAppCustomization().onDestinationReached(app.getMapActivity())){
				//TODO do something
			}

		}
		return false;
	}
	

	public boolean identifyUTurnIsNeeded(Location currentLocation, float posTolerance) {
		if (finalLocation == null || currentLocation == null || !route.isCalculated()) {
			this.isDeviatedFromRoute = false;
			return isDeviatedFromRoute;
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
		this.isDeviatedFromRoute = isOffRoute;
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

	private void setNewRoute(RouteCalculationResult res, Location start){
		final boolean newRoute = !this.route.isCalculated();
		app.getWaypointHelper().setNewRoute(res);
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
					if (!wrongMovementDirection) {
						evalWaitInterval = 3000;
					} else {
						evalWaitInterval = evalWaitInterval * 3 / 2;
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
		
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				for (IRouteInformationListener l : listeners) {
					l.newRouteIsCalculated(newRoute);
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
	
		
	public static String formatStreetName(String name, String ref, String destination) {
		if(destination != null && destination.length() > 0){
			if(ref != null && ref.length() > 0) {
				destination = ref + " " + destination;
			}
			return destination;
		} else if(name != null && name.length() > 0){
			if(ref != null && ref.length() > 0) {
				name = ref + " " + name;
			}
			return name;
		} else {
			if(ref == null) {
				return "";
			}
			return ref;
		}
	}
	
	protected boolean isDistanceLess(float currentSpeed, double dist, double etalon, double defSpeed){
		if(dist < etalon || ((dist / currentSpeed) < (etalon / defSpeed))){
			return true;
		}
		return false;
	}
	
	public synchronized String getCurrentName(){
		NextDirectionInfo n = getNextRouteDirectionInfo(new NextDirectionInfo(), false);
		Location l = lastFixedLocation;
		float speed = 0;
		if(l != null && l.hasSpeed()) {
			speed = l.getSpeed();
		}
		if(n.distanceTo > 0  && n.directionInfo != null && !n.directionInfo.getTurnType().isSkipToSpeak() && 
				voiceRouter.isDistanceLess(speed, n.distanceTo, voiceRouter.PREPARE_DISTANCE * 0.75f)) {
			String nm = n.directionInfo.getStreetName();
			String rf = n.directionInfo.getRef();
			String dn = n.directionInfo.getDestinationName();
			return "\u2566 " + formatStreetName(nm, rf, dn);
		}
		RouteSegmentResult rs = getCurrentSegmentResult();
		if(rs != null) {
			String nm = rs.getObject().getName();
			String rf = rs.getObject().getRef();
			String dn = rs.getObject().getDestinationName();
			return "\u21E7 " + formatStreetName(nm, rf, dn);
		}
		return null;
	}

    public RouteSegmentResult getCurrentSegmentResult() {
        return route.getCurrentSegmentResult();
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
			
			RouteCalculationResult res = provider.calculateRouteImpl(params);
			if (params.calculationProgress.isCancelled) {
				currentRunningJob = null;
				return;
			}
			final boolean onlineSourceWithoutInternet = !res.isCalculated() && params.type.isOnline() && !settings.isInternetConnectionAvailable();
			if (onlineSourceWithoutInternet && settings.GPX_ROUTE_CALC_OSMAND_PARTS.get()) {
				if (params.previousToRecalculate != null && params.previousToRecalculate.isCalculated()) {
					res = provider.recalculatePartOfflineRoute(res, params);
				}
			}
			
			synchronized (RoutingHelper.this) {
				if (res.isCalculated()) {
					route = res;
				} else {
					evalWaitInterval = evalWaitInterval * 3 / 2;
					evalWaitInterval = Math.min(evalWaitInterval, 120000);
				}
				currentRunningJob = null;
			}
			if(res.isCalculated()){
				setNewRoute(res, params.start);
			}

			if (res.isCalculated()) {
				String msg = app.getString(R.string.new_route_calculated_dist) + ": " + 
							OsmAndFormatter.getFormattedDistance(res.getWholeDistance(), app);
				if (res.getRoutingTime() != 0f) {
					msg += " (" + Algorithms.formatDuration((int) res.getRoutingTime()) + ")";
				}
				showMessage(msg);
			} else if (onlineSourceWithoutInternet) {
				showMessage(app.getString(R.string.error_calculating_route)
						+ ":\n" + app.getString(R.string.internet_connection_required_for_online_route)); //$NON-NLS-1$
			} else {
				if (res.getErrorMessage() != null) {
					showMessage(app.getString(R.string.error_calculating_route) + ":\n" + res.getErrorMessage()); //$NON-NLS-1$
				} else {
					showMessage(app.getString(R.string.empty_route_calculated));
				}
			}
			lastTimeEvaluatedRoute = System.currentTimeMillis();
		}
	}
	
	public void recalculateRouteDueToSettingsChange() {
		recalculateRouteInBackground(true, lastFixedLocation, finalLocation, intermediatePoints, currentGPXRoute, route, true);
	}
	
	private void recalculateRouteInBackground(boolean force, final Location start, final LatLon end, final List<LatLon> intermediates,
			final GPXRouteParamsBuilder gpxRoute, final RouteCalculationResult previousRoute, boolean paramsChanged){
		if (start == null || end == null) {
			return;
		}
		if(currentRunningJob == null){
			// do not evaluate very often
			if (force || System.currentTimeMillis() - lastTimeEvaluatedRoute > evalWaitInterval) {
				RouteCalculationParams params = new RouteCalculationParams();
				params.start = start;
				params.end = end;
				params.intermediates = intermediates;
				params.gpxRoute = gpxRoute == null? null : gpxRoute.build(start, settings);
				params.previousToRecalculate = previousRoute;
				params.leftSide = settings.DRIVING_REGION.get().leftHandDriving;
				params.fast = settings.FAST_ROUTE_MODE.getModeValue(mode);
				params.type = settings.ROUTER_SERVICE.getModeValue(mode);
				params.mode = mode;
				params.ctx = app;
				if(previousRoute == null && params.type == RouteService.OSMAND) {
					params.calculationProgress = new RouteCalculationProgress();
					updateProgress(params.calculationProgress);
				}
				synchronized (this) {
					currentRunningJob = new RouteRecalculationThread("Calculating route", params, paramsChanged); //$NON-NLS-1$
					currentRunningJob.start();
				}
			}
		}
	}
	
	public Thread startTaskInRouteThreadIfPossible(final Runnable r) {
		if(currentRunningJob == null) {
			synchronized (this) {
				currentRunningJob = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							r.run();
						} finally {
							synchronized (RoutingHelper.this) {
								currentRunningJob = null;
							}
						}
					}
				}, "Calculating position"); //$NON-NLS-1$
				currentRunningJob.start();
			}
		}
		return currentRunningJob;
	}
	
	
	private void updateProgress(final RouteCalculationProgress calculationProgress) {
		if(progressRoute != null) {
			app.runInUIThread(new Runnable() {
				@Override
				public void run() {
					if (isRouteBeingCalculated()) {
						float p = Math.max(calculationProgress.distanceFromBegin, calculationProgress.distanceFromEnd);
						float all = calculationProgress.totalEstimatedDistance * 1.25f;
						if (all > 0) {
							int t = (int) Math.min(p * p / (all * all) * 100f, 99);
							progressRoute.updateProgress(t);
						}
						updateProgress(calculationProgress);
					} else {
						progressRoute.finish();
					}
				}
			}, 300);
		}
	}
	
	public void setProgressBar(RouteCalculationProgressCallback progressRoute) {
		this.progressRoute = progressRoute;
	}
	
	public interface RouteCalculationProgressCallback {
		
		// set visibility
		public void updateProgress(int progress);
		
		public void finish();
		
	}


	public boolean isRouteBeingCalculated(){
		return currentRunningJob instanceof RouteRecalculationThread;
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
	
	public GPXFile generateGPXFileWithRoute(){
		return provider.createOsmandRouterGPX(route, app);
	}


	public void notifyIfRouteIsCalculated() {
		if(route.isCalculated()) {
			voiceRouter.newRouteIsCalculated(true)	;
		}
	}


}
