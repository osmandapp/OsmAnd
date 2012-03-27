package net.osmand.plus.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.LogUtil;
import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import net.osmand.plus.routing.RouteProvider.RouteCalculationResult;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.voice.CommandPlayer;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.util.FloatMath;
import android.widget.Toast;

public class RoutingHelper {
	
	private static final org.apache.commons.logging.Log log = LogUtil.getLog(RoutingHelper.class);
	
	public static interface IRouteInformationListener {
		
		public void newRouteIsCalculated(boolean updateRoute, boolean suppressTurnPrompt);
		
		public void routeWasCancelled();
	}
	
	private final float POSITION_TOLERANCE = 60;
	
	private final double DISTANCE_TO_USE_OSMAND_ROUTER = 20000;
	
	private List<IRouteInformationListener> listeners = new ArrayList<IRouteInformationListener>();

	private Context context;
	
	private boolean isFollowingMode = false;
	
	private GPXRouteParams currentGPXRoute = null;
	// instead of this properties RouteCalculationResult could be used
	private List<Location> routeNodes = new ArrayList<Location>();
	private List<RouteDirectionInfo> directionInfo = null;
	private int[] listDistance = null;

	// Note always currentRoute > get(currentDirectionInfo).routeOffset, 
	//         but currentRoute <= get(currentDirectionInfo+1).routeOffset 
	protected int currentDirectionInfo = 0;
	protected int currentRoute = 0;
	
	
	private LatLon finalLocation;
	private Location lastFixedLocation;
	private Thread currentRunningJob;
	private long lastTimeEvaluatedRoute = 0;
	private int evalWaitInterval = 3000;
	
	private ApplicationMode mode;
	private OsmandSettings settings;
	
	private RouteProvider provider = new RouteProvider();
	private VoiceRouter voiceRouter;

	private Handler uiHandler;

	public static boolean makeUturnWhenPossible = false;
	public static boolean suppressTurnPrompt = false;
	public static int turnImminent = 0;
	private long makeUTwpDetected = 0;

	public static boolean makeUturnWhenPossible() {
		return makeUturnWhenPossible;
	}

	public static boolean suppressTurnPrompt() {
		return suppressTurnPrompt;
	}

	public static int turnImminent() {
		return turnImminent;
	}
	


	public RoutingHelper(OsmandSettings settings, Context context, CommandPlayer player){
		this.settings = settings;
		this.context = context;
		voiceRouter = new VoiceRouter(this, player);
		uiHandler = new Handler();
	}

	public boolean isFollowingMode() {
		return isFollowingMode;
	}

	public void setFollowingMode(boolean isFollowingMode) {
		this.isFollowingMode = isFollowingMode;
	}
	
	
	public void setFinalAndCurrentLocation(LatLon finalLocation, Location currentLocation){
		setFinalAndCurrentLocation(finalLocation, currentLocation, null);
	}
	
	public synchronized void setFinalAndCurrentLocation(LatLon finalLocation, Location currentLocation, GPXRouteParams gpxRoute){
		clearCurrentRoute(finalLocation);
		currentGPXRoute = gpxRoute;
		// to update route
		setCurrentLocation(currentLocation);
		
	}
	
	public synchronized void clearCurrentRoute(LatLon newFinalLocation) {
		this.routeNodes.clear();
		listDistance = null;
		directionInfo = null;
		makeUturnWhenPossible = false;
		turnImminent = 0;
		evalWaitInterval = 3000;
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for (IRouteInformationListener l : listeners) {
					l.routeWasCancelled();
				}
			}
		});
		this.finalLocation = newFinalLocation;
		if (newFinalLocation == null) {
			settings.FOLLOW_THE_ROUTE.set(false);
			settings.FOLLOW_THE_GPX_ROUTE.set(null);
			// clear last fixed location
			this.lastFixedLocation = null;
			this.isFollowingMode = false;
		}
	}
	
	public GPXRouteParams getCurrentGPXRoute() {
		return currentGPXRoute;
	}
	
	public List<Location> getCurrentRoute() {
		return currentGPXRoute == null || currentGPXRoute.points.isEmpty() ? Collections
				.unmodifiableList(routeNodes) : Collections
				.unmodifiableList(currentGPXRoute.points);
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
	
	
	public boolean isRouterEnabled(){
		return finalLocation != null && lastFixedLocation != null;
	}
	public boolean isRouteCalculated(){
		return !routeNodes.isEmpty();
	}
	
	public VoiceRouter getVoiceRouter() {
		return voiceRouter;
	}
	
	
	public boolean finishAtLocation(Location currentLocation) {
		Location lastPoint = routeNodes.get(routeNodes.size() - 1);
		if(currentRoute > routeNodes.size() - 3 && currentLocation.distanceTo(lastPoint) < POSITION_TOLERANCE){
			if(lastFixedLocation != null && lastFixedLocation.distanceTo(lastPoint) < POSITION_TOLERANCE){
				showMessage(context.getString(R.string.arrived_at_destination));
				voiceRouter.arrivedDestinationPoint();
				clearCurrentRoute(null);
				
			}
			lastFixedLocation = currentLocation;
			return true;
		}
		return false;
	}
	
	public Location getCurrentLocation() {
		return lastFixedLocation;
	}
	
	private void updateCurrentRoute(int currentRoute){
		this.currentRoute = currentRoute;
		if(directionInfo != null){
			while(currentDirectionInfo < directionInfo.size() - 1 && 
					directionInfo.get(currentDirectionInfo + 1).routePointOffset < currentRoute){
				currentDirectionInfo ++;
			}
			
		}
	}
	
	public void addListener(IRouteInformationListener l){
		listeners.add(l);
	}
	
	public boolean removeListener(IRouteInformationListener l){
		return listeners.remove(l);
	}
	
	
	public void setCurrentLocation(Location currentLocation) {
		if(finalLocation == null || currentLocation == null){
			makeUturnWhenPossible = false;
			suppressTurnPrompt = false;
			turnImminent = 0;
			return;
		}
		
		boolean calculateRoute = false;
		synchronized (this) {
			// 0. Route empty or needs to be extended? Then re-calculate route.
			if(routeNodes.isEmpty() || routeNodes.size() <= currentRoute){
				calculateRoute = true;
			} else {
				// 1.
				tryMarkPassedRoute(currentLocation);

				// 2. check if destination found
				if(finishAtLocation(currentLocation)){
					return;
				}
				
				// 3. check if closest location already passed
				if(currentRoute + 1 < routeNodes.size()){
					float bearingRouteNext = routeNodes.get(currentRoute).bearingTo(routeNodes.get(currentRoute + 1));
					float bearingToRoute = currentLocation.bearingTo(routeNodes.get(currentRoute));
					// only 35 degrees for that case because it wrong catches sharp turns 
					if(Math.abs(bearingRouteNext - bearingToRoute) > 140 && Math.abs(bearingRouteNext - bearingToRoute) < 220){
						if(log.isDebugEnabled()){
							log.debug("Processed point bearingToRoute : "+ bearingToRoute +" bearingRouteNext " + bearingRouteNext); //$NON-NLS-1$ //$NON-NLS-2$
						}
						updateCurrentRoute(currentRoute + 1);
					}
				}
				
				// 3.5 check that we already pass very sharp turn by missing one point (so our turn is sharper than expected)
				// instead of that rule possible could be introduced another if the dist < 5m mark the location as already passed
				if(currentRoute + 2 < routeNodes.size()){
					float bearingRouteNextNext = routeNodes.get(currentRoute + 1).bearingTo(routeNodes.get(currentRoute + 2));
					float bearingToRouteNext = currentLocation.bearingTo(routeNodes.get(currentRoute + 1));
					// only 15 degrees for that case because it wrong catches sharp turns 
					if(Math.abs(bearingRouteNextNext - bearingToRouteNext) > 165 && Math.abs(bearingRouteNextNext - bearingToRouteNext) < 195){
						if(log.isDebugEnabled()){
							log.debug("Processed point bearingToRouteNext : "+ bearingToRouteNext +" bearingRouteNextNext " + bearingRouteNextNext); //$NON-NLS-1$ //$NON-NLS-2$
						}
						updateCurrentRoute(currentRoute + 2);
					}
				}
				
				// 4. >60m off current route (sideways)? Then re-calculate route.
				if(currentRoute > 0){
					float bearingRoute = routeNodes.get(currentRoute - 1).bearingTo(routeNodes.get(currentRoute));
					float bearingToRoute = currentLocation.bearingTo(routeNodes.get(currentRoute));
					float d = Math.abs(currentLocation.distanceTo(routeNodes.get(currentRoute)) * FloatMath.sin((bearingToRoute - bearingRoute)*3.14f/180f));
					if(d > POSITION_TOLERANCE) {
						log.info("Recalculate route, because correlation  : " + d); //$NON-NLS-1$
						calculateRoute = true;
					}
				} 
				
				// 5. Sum distance to last and current route nodes
				if(!calculateRoute){
					float d = currentLocation.distanceTo(routeNodes.get(currentRoute));
					if (d > POSITION_TOLERANCE) {
						if (currentRoute > 0) {
							// 5a. Greater than 2*distance between them? Then re-calculate route. (Case often covered by 4., but still needed.)
							float f1 = currentLocation.distanceTo(routeNodes.get(currentRoute - 1)) + d;
							float c = routeNodes.get(currentRoute - 1).distanceTo(routeNodes.get(currentRoute));
							if (c * 2 < d + f1) {
								log.info("Recalculate route, because too far from points : " + d + " " + f1 + " >> " + c); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								calculateRoute = true;
							}
						} else {
							// 5b. Too far from route start? Then re-calculate route.
							log.info("Recalculate route, because too far from start : " + d); //$NON-NLS-1$
							calculateRoute = true;
						}
					}
				}
				
				// 6. + 7.
				directionDetection(currentLocation);

				if ((suppressTurnPrompt == false && calculateRoute == false) || makeUturnWhenPossible == true) {
					voiceRouter.updateStatus(currentLocation, makeUturnWhenPossible);
				}
			}
		}
		lastFixedLocation = currentLocation;

		// 8. Strange Direction? Then re-calculate route. (Added new, may possibly even replace triggers 4, 5a, 5b ?)
		if(suppressTurnPrompt && (currentLocation.distanceTo(routeNodes.get(currentRoute)) > POSITION_TOLERANCE) ){
			calculateRoute = true;
		}

		if(calculateRoute){
			recalculateRouteInBackground(lastFixedLocation, finalLocation, currentGPXRoute);
		}
	}
	
	public synchronized void tryMarkPassedRoute(Location currentLocation) {
		// 1. try to mark passed route (move forward)
		float dist = currentLocation.distanceTo(routeNodes.get(currentRoute));
		while(currentRoute + 1 < routeNodes.size()){
			float newDist = currentLocation.distanceTo(routeNodes.get(currentRoute + 1));
			boolean processed = false;
			if (newDist < dist){
				if(newDist > 150){
					// may be that check is really not needed ? only for start position
					if(currentRoute > 0 ){
						// check that we are not far from the route (if we are on the route distance doesn't matter) 
						float bearingRoute = routeNodes.get(currentRoute - 1).bearingTo(routeNodes.get(currentRoute));
						float bearingToRoute = currentLocation.bearingTo(routeNodes.get(currentRoute));
						float d = Math.abs(currentLocation.distanceTo(routeNodes.get(currentRoute)) * FloatMath.sin((bearingToRoute - bearingRoute)*3.14f/180f));
						if(d > POSITION_TOLERANCE){
							processed = true;
						}
					} else {
						processed = true;
					}
					if(processed && log.isDebugEnabled()){
						log.debug("Processed distance : " + newDist + " " + dist);  //$NON-NLS-1$//$NON-NLS-2$
					}
				} else {
					// case if you are getting close to the next point after turn
					//  but you have not yet turned (could be checked bearing)
					if(currentLocation.hasBearing() || lastFixedLocation != null){
						float bearingToRoute = currentLocation.bearingTo(routeNodes.get(currentRoute));
						float bearingRouteNext = routeNodes.get(currentRoute).bearingTo(routeNodes.get(currentRoute+1));
						float bearingMotion = currentLocation.hasBearing() ? currentLocation.getBearing() : lastFixedLocation.bearingTo(currentLocation);
						if(Math.abs(bearingMotion - bearingToRoute) > Math.abs(bearingMotion - bearingRouteNext)){
							if(log.isDebugEnabled()){
								log.debug("Processed point bearing deltas : " + Math.abs(currentLocation.getBearing() - bearingToRoute) + " " //$NON-NLS-1$ //$NON-NLS-2$
										+ Math.abs(currentLocation.getBearing() - bearingRouteNext));
							}
							processed = true;
						}
					}
				}
			}
			if(processed){
				// that node already passed
				updateCurrentRoute(currentRoute + 1);
				dist = newDist;
			} else {
				break;
			}
		}
	}

	public synchronized void directionDetection(Location currentLocation) {
		makeUturnWhenPossible = false;
		suppressTurnPrompt = false;
		if(finalLocation == null || currentLocation == null){
			turnImminent = 0;
			return;
		}
		// 6. + 7. Direction detection, by Hardy, Feb 2012
		if(routeNodes.size() > 0){
			if (currentLocation.hasBearing() || lastFixedLocation != null) {
				float bearingMotion = currentLocation.hasBearing() ? currentLocation.getBearing() : lastFixedLocation.bearingTo(currentLocation);
				float bearingToRoute = currentLocation.bearingTo(routeNodes.get(currentRoute));
				// 6. Suppress turn prompt if prescribed direction of motion is between 45 and 135 degrees off
				if (Math.abs(bearingMotion - bearingToRoute) > 45f && 360 - Math.abs(bearingMotion - bearingToRoute) > 45f) {
					// disregard upper bound to suppress turn prompt also for recalculated still-opposite route
					//if (Math.abs(bearingMotion - bearingToRoute) <= 135f && 360 - Math.abs(bearingMotion - bearingToRoute) <= 135f) {
						suppressTurnPrompt = true;
						//log.info("bearingMotion is off from bearingToRoute between >45 and <=135 degrees"); //$NON-NLS-1$
					//}
				}
				// 7. Check necessity for unscheduled U-turn, Issue 863
				if (Math.abs(bearingMotion - bearingToRoute) > 135f && 360 - Math.abs(bearingMotion - bearingToRoute) > 135f) {
					float d = currentLocation.distanceTo(routeNodes.get(currentRoute));
					// 60m tolerance to allow for GPS inaccuracy
					if (d > POSITION_TOLERANCE) {
						if (makeUTwpDetected == 0) {
							makeUTwpDetected = System.currentTimeMillis();
						// require 5 sec since first detection, to avoid false positive announcements
						} else if ((System.currentTimeMillis() - makeUTwpDetected > 5000)) {
							makeUturnWhenPossible = true;
							turnImminent = 1;
							//log.info("bearingMotion is opposite to bearingRoute"); //$NON-NLS-1$
						}
					}
				} else { 
					makeUTwpDetected = 0;
				}
			}
		}
	}

	private synchronized void setNewRoute(RouteCalculationResult res, Location start){
		final boolean updateRoute = !routeNodes.isEmpty();
		routeNodes = res.getLocations();
		directionInfo = res.getDirections();
		listDistance = res.getListDistance();
		currentRoute = 0;
		currentDirectionInfo = 0;
		if(isFollowingMode){
			//tryMarkPassedRoute(start);
			// try remove false route-recaluated prompts by checking direction to second route node
			if(routeNodes.size() > 1){
				currentRoute = 1;
			}
			directionDetection(start);

			// set/reset evalWaitInterval only if new route is in forward direction
			if(!suppressTurnPrompt){
				evalWaitInterval = 3001;
			} else {
				evalWaitInterval = evalWaitInterval * 4 / 3;
				evalWaitInterval = Math.min(evalWaitInterval, 120000);
			}

			// trigger voice prompt only if new route is in forward direction (but see also additional 60sec timer for this message in voiceRouter)
			voiceRouter.newRouteIsCalculated(updateRoute, suppressTurnPrompt);
		} 
		currentRoute = 0;
		currentDirectionInfo = 0;

		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				for (IRouteInformationListener l : listeners) {
					l.newRouteIsCalculated(updateRoute, suppressTurnPrompt);
				}
			}
		});
		
	}
	
	public synchronized int getLeftDistance(){
		if(listDistance != null && currentRoute < listDistance.length){
			int dist = listDistance[currentRoute];
			Location l = routeNodes.get(currentRoute);
			if(lastFixedLocation != null){
				dist += lastFixedLocation.distanceTo(l);
			}
			return dist;
		}
		return 0;
	}
	
	public String getGeneralRouteInformation(){
		int dist = getLeftDistance();
		int hours = getLeftTime() / (60 * 60);
		int minutes = (getLeftTime() / 60) % 60;
		return context.getString(R.string.route_general_information, OsmAndFormatter.getFormattedDistance(dist, context),
				hours, minutes);
	}
	
	public Location getLocationFromRouteDirection(RouteDirectionInfo i){
		if(i.routePointOffset < routeNodes.size()){
			return routeNodes.get(i.routePointOffset);
		}
		return null;
	}
	
	
	public RouteDirectionInfo getNextRouteDirectionInfo(){
		if(directionInfo != null && currentDirectionInfo < directionInfo.size() - 1){
			return directionInfo.get(currentDirectionInfo + 1);
		}
		return null;
	}
	public RouteDirectionInfo getNextNextRouteDirectionInfo(){
		if(directionInfo != null && currentDirectionInfo < directionInfo.size() - 2){
			return directionInfo.get(currentDirectionInfo + 2);
		}
		return null;
	}
	
	public List<RouteDirectionInfo> getRouteDirections(){
		if(directionInfo != null && currentDirectionInfo < directionInfo.size()){
			if(currentDirectionInfo == 0){
				return directionInfo;
			}
			if(currentDirectionInfo < directionInfo.size() - 1){
				return directionInfo.subList(currentDirectionInfo + 1, directionInfo.size());
			}
		}
		return Collections.emptyList();
	}
	
	public int getDistanceToNextRouteDirection() {
		if (directionInfo != null && currentDirectionInfo < directionInfo.size()) {
			int dist = listDistance[currentRoute];
			if (currentDirectionInfo < directionInfo.size() - 1) {
				dist -= listDistance[directionInfo.get(currentDirectionInfo + 1).routePointOffset];
			}
			if(lastFixedLocation != null){
				dist += lastFixedLocation.distanceTo(routeNodes.get(currentRoute));
			}

			if (dist <= 100 || makeUturnWhenPossible == true) {
				turnImminent = 1;
			} else if (dist <= 3000) {
				turnImminent = 0;
			} else {
				turnImminent = -1;
			}

			//Show turnImminent for at least 5 sec (changed to 6 for device delay) if moving, cut off at 300m to avoid speed artifacts
			if(lastFixedLocation != null && lastFixedLocation.hasSpeed()){
				if ((dist < (lastFixedLocation.getSpeed() * 6f)) && (dist < 300)) {
					turnImminent = 1;
				}
			}

			return dist;
		}
		turnImminent = 0;
		return 0;
	}
	
	public synchronized int getLeftTime(){
		if(directionInfo != null && currentDirectionInfo < directionInfo.size()){
			int t = directionInfo.get(currentDirectionInfo).afterLeftTime;
			int e = directionInfo.get(currentDirectionInfo).expectedTime;
			if (e > 0) {
				int passedDist = listDistance[directionInfo.get(currentDirectionInfo).routePointOffset] - listDistance[currentRoute];
				int wholeDist = listDistance[directionInfo.get(currentDirectionInfo).routePointOffset];
				if (currentDirectionInfo < directionInfo.size() - 1) {
					wholeDist -= listDistance[directionInfo.get(currentDirectionInfo + 1).routePointOffset];
				}
				if (wholeDist > 0) {
					t = (int) (t + ((float)e) * (1 - (float) passedDist / (float) wholeDist));
				}
			}
			return t;
		}
		return 0;
	}
	
	private void recalculateRouteInBackground(final Location start, final LatLon end, final GPXRouteParams gpxRoute){
		if (start == null || end == null) {
			return;
		}
		
		// temporary check while osmand offline router is not stable
		RouteService serviceToUse = settings.ROUTER_SERVICE.get();
		if (serviceToUse == RouteService.OSMAND && !settings.USE_OSMAND_ROUTING_SERVICE_ALWAYS.get()) {
			double distance = MapUtils.getDistance(end, start.getLatitude(), start.getLongitude());
			if (distance > DISTANCE_TO_USE_OSMAND_ROUTER) {
				// display 'temporarily switched to CloudMade' message only once per session (and never for GPX routes), mark all subsequent resets as '3001'
				if (evalWaitInterval == 3000 && settings.FOLLOW_THE_GPX_ROUTE.get() == null) {
					showMessage(context.getString(R.string.osmand_routing_experimental), Toast.LENGTH_LONG);
					evalWaitInterval = 3001;
				}
				serviceToUse = RouteService.CLOUDMADE;
			}
		}
		final RouteService service = serviceToUse;
		
		if(currentRunningJob == null){
			// do not evaluate very often
			if (System.currentTimeMillis() - lastTimeEvaluatedRoute > evalWaitInterval) {
				final boolean fastRouteMode = settings.FAST_ROUTE_MODE.get();
				synchronized (this) {
					currentRunningJob = new Thread(new Runnable() {
						@Override
						public void run() {
							RouteCalculationResult res = provider.calculateRouteImpl(start, end, mode, service, context, gpxRoute, fastRouteMode);
							synchronized (RoutingHelper.this) {
								if (res.isCalculated()) {
									setNewRoute(res, start);
									// reset error wait interval
									// set/reset evalWaitInterval in setNewRoute and only if new route is in forward direction
									// evalWaitInterval = 3001;
								} else {
									evalWaitInterval = evalWaitInterval * 4 / 3;
									evalWaitInterval = Math.min(evalWaitInterval, 120000);
								}
								currentRunningJob = null;
							}

							if (res.isCalculated()) {
								int[] dist = res.getListDistance();
								int l = dist != null && dist.length > 0 ? dist[0] : 0;
								showMessage(context.getString(R.string.new_route_calculated_dist)
										+ ": " + OsmAndFormatter.getFormattedDistance(l, context)); //$NON-NLS-1$
							} else if (service != RouteService.OSMAND && !settings.isInternetConnectionAvailable()) {
									showMessage(context.getString(R.string.error_calculating_route)
										+ ":\n" + context.getString(R.string.internet_connection_required_for_online_route), Toast.LENGTH_LONG); //$NON-NLS-1$
							} else {
								if (res.getErrorMessage() != null) {
									showMessage(context.getString(R.string.error_calculating_route) + ":\n" + res.getErrorMessage(), Toast.LENGTH_LONG); //$NON-NLS-1$
								} else if (res.getLocations() == null) {
									showMessage(context.getString(R.string.error_calculating_route_occured), Toast.LENGTH_LONG);
								} else {
									showMessage(context.getString(R.string.empty_route_calculated), Toast.LENGTH_LONG);
								}
							}
							lastTimeEvaluatedRoute = System.currentTimeMillis();
						}
					}, "Calculating route"); //$NON-NLS-1$
					currentRunningJob.start();
				}
			}
		}
	}
	
	public boolean isRouteBeingCalculated(){
		return currentRunningJob != null;
	}
	
	private void showMessage(final String msg, final int length) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				AccessibleToast.makeText(context, msg, length).show();
			}
		});
	}
	private void showMessage(final String msg){
		showMessage(msg, Toast.LENGTH_SHORT);
	}
	
	public boolean hasPointsToShow(){
		return finalLocation != null && !routeNodes.isEmpty();
	}
	
	protected Context getContext() {
		return context;
	}
	
	public synchronized void fillLocationsToShow(double topLatitude, double leftLongitude, double bottomLatitude,double rightLongitude, List<Location> l){
		l.clear();
		boolean previousVisible = false;
		if(lastFixedLocation != null){
			if(leftLongitude <= lastFixedLocation.getLongitude() && lastFixedLocation.getLongitude() <= rightLongitude &&
					bottomLatitude <= lastFixedLocation.getLatitude() && lastFixedLocation.getLatitude() <= topLatitude){
				l.add(lastFixedLocation);
				previousVisible = true;
			}
		}
		
		for (int i = currentRoute; i < routeNodes.size(); i++) {
			Location ls = routeNodes.get(i);
			if(leftLongitude <= ls.getLongitude() && ls.getLongitude() <= rightLongitude &&
					bottomLatitude <= ls.getLatitude() && ls.getLatitude() <= topLatitude){
				l.add(ls);
				if (!previousVisible) {
					if (i > currentRoute) {
						l.add(0, routeNodes.get(i - 1));
					} else if (lastFixedLocation != null) {
						l.add(0, lastFixedLocation);
					}
				}
				previousVisible = true;
			} else if(previousVisible){
				l.add(ls);
				previousVisible = false;
				// do not continue make method more efficient (because it calls in UI thread)
				// this break also has logical sense !
				break;
			}
		}
	}
	
	public GPXFile generateGPXFileWithRoute(){
		return provider.createOsmandRouterGPX(currentRoute, routeNodes, currentDirectionInfo, directionInfo);
	}

	
	public static class TurnType {
		public static final String C = "C"; // continue (go straight) //$NON-NLS-1$
		public static final String TL = "TL"; // turn left //$NON-NLS-1$
		public static final String TSLL = "TSLL"; // turn slightly left //$NON-NLS-1$
		public static final String TSHL = "TSHL"; // turn sharply left //$NON-NLS-1$
		public static final String TR = "TR"; // turn right //$NON-NLS-1$
		public static final String TSLR = "TSLR"; // turn slightly right //$NON-NLS-1$
		public static final String TSHR = "TSHR"; // turn sharply right //$NON-NLS-1$
		public static final String TU = "TU"; // U-turn //$NON-NLS-1$
		public static final String TRU = "TRU"; // Right U-turn //$NON-NLS-1$
		public static String[] predefinedTypes = new String[] {C, TL, TSLL, TSHL, TR, TSLR, TSHR, TU, TRU}; 
		
		
		public static TurnType valueOf(String s){
			for(String v : predefinedTypes){
				if(v.equals(s)){
					return new TurnType(v);
				}
			}
			if (s != null && s.startsWith("EXIT")) { //$NON-NLS-1$
				return getExitTurn(Integer.parseInt(s.substring(4)), 0);
			}
			return null;
		}
		
		private final String value;
		private int exitOut;
		// calculated CW head rotation if previous direction to NORTH
		private float turnAngle;
		
		public static TurnType getExitTurn(int out, float angle){
			TurnType r = new TurnType("EXIT", out); //$NON-NLS-1$
			r.setTurnAngle(angle);
			return r;
		}
		private TurnType(String value, int exitOut){
			this.value = value;
			this.exitOut = exitOut;
		}

		// calculated CW head rotation if previous direction to NORTH
		public float getTurnAngle() {
			return turnAngle;
		}
		

		public void setTurnAngle(float turnAngle) {
			this.turnAngle = turnAngle;
		}
		
		
		private TurnType(String value){
			this.value = value;
		}
		public String getValue() {
			return value;
		}
		public int getExitOut() {
			return exitOut;
		}
		public boolean isRoundAbout(){
			return value.equals("EXIT"); //$NON-NLS-1$
		}
	}
	
	public static class RouteDirectionInfo {
		public String descriptionRoute = ""; //$NON-NLS-1$
		// expected time after route point
		public int expectedTime;
		
		public TurnType turnType;
		// location when you should action (turn or go ahead)
		public int routePointOffset;
		
		// TODO add from parser
		public String ref;
		public String streetName;
		// speed limit in m/s (should be array of speed limits?)
		public float speedLimit; 
		
		// calculated vars
		
		// after action (excluding expectedTime)
		public int afterLeftTime;
		// distance after action (for i.e. after turn to next turn)
		public int distance;
	}

	

}
