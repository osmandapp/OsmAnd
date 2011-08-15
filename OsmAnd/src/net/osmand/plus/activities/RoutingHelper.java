package net.osmand.plus.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.LogUtil;
import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.RouteProvider.RouteCalculationResult;
import net.osmand.plus.activities.RouteProvider.RouteService;
import net.osmand.plus.voice.CommandPlayer;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.FloatMath;
import android.widget.Toast;

public class RoutingHelper {
	
	private static final org.apache.commons.logging.Log log = LogUtil.getLog(RoutingHelper.class);
	
	public static interface IRouteInformationListener {
		
		public void newRouteIsCalculated(boolean updateRoute);
		
		public void routeWasCancelled();
	}
	
	private final double DISTANCE_TO_USE_OSMAND_ROUTER = 20000;
	
	private List<IRouteInformationListener> listeners = new ArrayList<IRouteInformationListener>();

	private Context context;
	
	// activity to show messages & refresh map when route is calculated	
	private Activity uiActivity;
	
	private boolean isFollowingMode = false;
	
	private List<Location> currentGPXRoute = null;
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

	
	
	
	public RoutingHelper(OsmandSettings settings, Context context, CommandPlayer player){
		this.settings = settings;
		this.context = context;
		voiceRouter = new VoiceRouter(this, player);
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
	
	public synchronized void setFinalAndCurrentLocation(LatLon finalLocation, Location currentLocation, List<Location> gpxRoute){
		clearCurrentRoute(finalLocation);
		currentGPXRoute = gpxRoute;
		// to update route
		setCurrentLocation(currentLocation);
		
	}
	
	public void clearCurrentRoute(LatLon newFinalLocation){
		this.routeNodes.clear();
		listDistance = null;
		directionInfo = null;
		evalWaitInterval = 3000;
		if(uiActivity != null){
			uiActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					for(IRouteInformationListener l : listeners){
						l.routeWasCancelled();
					}
				}
			});
		}
		this.finalLocation = newFinalLocation;
		if(newFinalLocation == null){
			settings.FOLLOW_TO_THE_ROUTE.set(false);
			// clear last fixed location
			this.lastFixedLocation = null;
			this.isFollowingMode = false;
		}
	}
	
	public List<Location> getCurrentGPXRoute() {
		return currentGPXRoute;
	}
	
	public List<Location> getCurrentRoute() {
		return currentGPXRoute == null || currentGPXRoute.isEmpty() ? Collections
				.unmodifiableList(routeNodes) : Collections
				.unmodifiableList(currentGPXRoute);
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
		if(currentRoute > routeNodes.size() - 3 && currentLocation.distanceTo(lastPoint) < 60){
			if(lastFixedLocation != null && lastFixedLocation.distanceTo(lastPoint) < 60){
				showMessage(context.getString(R.string.arrived_at_destination));
				voiceRouter.arrivedDestinationPoint();
				clearCurrentRoute(null);
				
			}
			lastFixedLocation = currentLocation;
			return true;
		}
		return false;
	}
	
	public void setUiActivity(Activity uiActivity) {
		this.uiActivity = uiActivity;
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
			return;
		}
		boolean calculateRoute  = false;
		synchronized (this) {
			if(routeNodes.isEmpty() || routeNodes.size() <= currentRoute){
				calculateRoute = true;
			} else {
				// Check whether user follow by route in correct direction
				
				// 1. try to mark passed route (move forward)
				float dist = currentLocation.distanceTo(routeNodes.get(currentRoute));
				while(currentRoute + 1 < routeNodes.size()){
					float newDist = currentLocation.distanceTo(routeNodes.get(currentRoute + 1));
					boolean proccesed = false;
					if (newDist < dist){
						if(newDist > 150){
							// may be that check is really not needed ? only for start position
							if(currentRoute > 0 ){
								// check that we are not far from the route (if we are on the route distance doesn't matter) 
								float bearing = routeNodes.get(currentRoute - 1).bearingTo(routeNodes.get(currentRoute));
								float bearingMovement = currentLocation.bearingTo(routeNodes.get(currentRoute));
								float d = Math.abs(currentLocation.distanceTo(routeNodes.get(currentRoute)) * FloatMath.sin((bearingMovement - bearing)*3.14f/180f));
								if(d > 50){
									proccesed = true;
								}
							} else {
								proccesed = true;
							}
							if(proccesed && log.isDebugEnabled()){
								log.debug("Processed distance : " + newDist + " " + dist);  //$NON-NLS-1$//$NON-NLS-2$
							}
							
						} else {
							// case if you are getting close to the next point after turn
							//  but you haven't turned before (could be checked bearing)
							if(currentLocation.hasBearing() || lastFixedLocation != null){
								float bearingToPoint = currentLocation.bearingTo(routeNodes.get(currentRoute));
								float bearingBetweenPoints = routeNodes.get(currentRoute).bearingTo(routeNodes.get(currentRoute+1));
								float bearing = currentLocation.hasBearing() ? currentLocation.getBearing() : lastFixedLocation.bearingTo(currentLocation);
								if(Math.abs(bearing - bearingToPoint) >
									Math.abs(bearing - bearingBetweenPoints)){
									if(log.isDebugEnabled()){
										log.debug("Processed point bearing : " + Math.abs(currentLocation.getBearing() - bearingToPoint) + " " //$NON-NLS-1$ //$NON-NLS-2$
												+ Math.abs(currentLocation.getBearing() - bearingBetweenPoints));
									}
									proccesed = true;
								}
							}
							
							
						}
					}
					if(proccesed){
						// that node already passed
						updateCurrentRoute(currentRoute + 1);
						dist = newDist;
					} else {
						break;
					}
						
				}
				// 2. check if destination found
				if(finishAtLocation(currentLocation)){
					return;
				}
				
				// 3. check if closest location already passed
				if(currentRoute + 1 < routeNodes.size()){
					float bearing = routeNodes.get(currentRoute).bearingTo(routeNodes.get(currentRoute + 1));
					float bearingMovement = currentLocation.bearingTo(routeNodes.get(currentRoute));
					// only 35 degrees for that case because it wrong catches sharp turns 
					if(Math.abs(bearing - bearingMovement) > 140 && Math.abs(bearing - bearingMovement) < 220){
						if(log.isDebugEnabled()){
							log.debug("Processed point movement bearing  : "+bearingMovement +" bearing " + bearing); //$NON-NLS-1$ //$NON-NLS-2$
						}
						updateCurrentRoute(currentRoute + 1);
					}
				}
				
				// 3.5 check that we already pass very sharp turn by missing one point (so our turn is sharper than expected)
				// instead of that rule possible could be introduced another if the dist < 5m mark the location as already passed
				if(currentRoute + 2 < routeNodes.size()){
					float bearing = routeNodes.get(currentRoute + 1).bearingTo(routeNodes.get(currentRoute + 2));
					float bearingMovement = currentLocation.bearingTo(routeNodes.get(currentRoute + 1));
					// only 15 degrees for that case because it wrong catches sharp turns 
					if(Math.abs(bearing - bearingMovement) > 165 && Math.abs(bearing - bearingMovement) < 195){
						if(log.isDebugEnabled()){
							log.debug("Processed point movement bearing 2 : "+bearingMovement +" bearing " + bearing); //$NON-NLS-1$ //$NON-NLS-2$
						}
						updateCurrentRoute(currentRoute + 2);
					}
				}
				
				// 4. evaluate distance to the route and reevaluate if needed
				if(currentRoute > 0){
					float bearing = routeNodes.get(currentRoute - 1).bearingTo(routeNodes.get(currentRoute));
					float bearingMovement = currentLocation.bearingTo(routeNodes.get(currentRoute));
					float d = Math.abs(currentLocation.distanceTo(routeNodes.get(currentRoute)) * FloatMath.sin((bearingMovement - bearing)*3.14f/180f));
					if(d > 50) {
						log.info("Recalculate route, because correlation  : " + d); //$NON-NLS-1$
						calculateRoute = true;
					}
				} 
				
				// 5. also check bearing by summing distance
				if(!calculateRoute){
					float d = currentLocation.distanceTo(routeNodes.get(currentRoute));
					if (d > 80) {
						if (currentRoute > 0) {
							// possibly that case is not needed (often it is covered by 4.)
							float f1 = currentLocation.distanceTo(routeNodes.get(currentRoute - 1)) + d;
							float c = routeNodes.get(currentRoute - 1).distanceTo(routeNodes.get(currentRoute));
							if (c * 2 < d + f1) {
								log.info("Recalculate route, because too far from points : " + d + " " + f1 + " >> " + c); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								calculateRoute = true;
							}
						} else {
							// that case is needed
							log.info("Recalculate route, because too far from start : " + d); //$NON-NLS-1$
							calculateRoute = true;
						}
					}
				}
				
				// 5. Also bearing could be checked (is it same direction)
//				float bearing;
//				if(currentLocation.hasBearing()){
//					bearing = currentLocation.getBearing();
//				} else if(lastFixedLocation != null){
//					bearing = lastFixedLocation.bearingTo(currentLocation);
//				}
//				bearingRoute = currentLocation.bearingTo(routeNodes.get(currentRoute));
//				if (Math.abs(bearing - bearingRoute) > 60f && 360 - Math.abs(bearing - bearingRoute) > 60f) {
//				      something wrong however it could be starting movement
//				}
			}
		}
		voiceRouter.updateStatus(currentLocation);

		lastFixedLocation = currentLocation;
		if(calculateRoute){
			recalculateRouteInBackground(lastFixedLocation, finalLocation, currentGPXRoute);
		}
	}
	
	private synchronized void setNewRoute(RouteCalculationResult res){
		boolean updateRoute = !routeNodes.isEmpty();
		routeNodes = res.getLocations();
		directionInfo = res.getDirections();
		listDistance = res.getListDistance();
		currentDirectionInfo = 0;
		currentRoute = 0;
		if(isFollowingMode){
			voiceRouter.newRouteIsCalculated(updateRoute);
		} 
		for(IRouteInformationListener l : listeners){
			l.newRouteIsCalculated(updateRoute);
		}
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
			return dist;
		}
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
	
	private void recalculateRouteInBackground(final Location start, final LatLon end, final List<Location> currentGPXRoute){
		if (start == null || end == null) {
			return;
		}
		
		// temporary check while osmand offline router is not stable
		RouteService serviceToUse = settings.ROUTER_SERVICE.get();
		if (serviceToUse == RouteService.OSMAND && !settings.USE_OSMAND_ROUTING_SERVICE_ALWAYS.get()) {
			double distance = MapUtils.getDistance(end, start.getLatitude(), start.getLongitude());
			if (distance > DISTANCE_TO_USE_OSMAND_ROUTER) {
				showMessage(context.getString(R.string.osmand_routing_experimental));
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
							if(service != RouteService.OSMAND && !settings.isInternetConnectionAvailable()){
								showMessage(context.getString(R.string.internet_connection_required_for_online_route), Toast.LENGTH_LONG);
							}
							RouteCalculationResult res = provider.calculateRouteImpl(start, end, mode, service, context, currentGPXRoute, fastRouteMode);
							synchronized (RoutingHelper.this) {
								if (res.isCalculated()) {
									setNewRoute(res);
									// reset error wait interval
									evalWaitInterval = 3000;
								} else {
									evalWaitInterval = evalWaitInterval * 4 / 3;
									if (evalWaitInterval > 120000) {
										evalWaitInterval = 120000;
									}
								}
								currentRunningJob = null;
							}

							if (res.isCalculated()) {
								int[] dist = res.getListDistance();
								int l = dist != null && dist.length > 0 ? dist[0] : 0;
								showMessage(context.getString(R.string.new_route_calculated_dist)
										+ " : " + OsmAndFormatter.getFormattedDistance(l, context)); //$NON-NLS-1$
								if (uiActivity instanceof MapActivity) {
									// be aware that is non ui thread
									((MapActivity) uiActivity).getMapView().refreshMap();
								}
							} else {
								if (res.getErrorMessage() != null) {
									showMessage(context.getString(R.string.error_calculating_route) + " : " + res.getErrorMessage()); //$NON-NLS-1$
								} else if (res.getLocations() == null) {
									showMessage(context.getString(R.string.error_calculating_route_occured));
								} else {
									showMessage(context.getString(R.string.empty_route_calculated));
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
	
	private void showMessage(final String msg, final int length){
		if (uiActivity != null) {
			uiActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if(uiActivity != null){
						Toast.makeText(uiActivity, msg, length).show();
					}
				}
			});
		}
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
	
	

	
	public static class TurnType {
		public static final String C = "C"; // continue (go straight) //$NON-NLS-1$
		public static final String TL = "TL"; // turn left //$NON-NLS-1$
		public static final String TSLL = "TSLL"; // turn slight left //$NON-NLS-1$
		public static final String TSHL = "TSHL"; // turn sharp left //$NON-NLS-1$
		public static final String TR = "TR"; // turn right //$NON-NLS-1$
		public static final String TSLR = "TSLR"; // turn slight right //$NON-NLS-1$
		public static final String TSHR = "TSHR"; // turn sharp right //$NON-NLS-1$
		public static final String TU = "TU"; // U-turn //$NON-NLS-1$
		public static String[] predefinedTypes = new String[] {C, TL, TSLL, TSHL, TR, TSLR, TSHR, TU}; 
		
		
		public static TurnType valueOf(String s){
			for(String v : predefinedTypes){
				if(v.equals(s)){
					return new TurnType(v);
				}
			}
			if(s!= null && s.startsWith("EXIT")){ //$NON-NLS-1$
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
		
		// calculated vars
		
		// after action (excluding expectedTime)
		public int afterLeftTime;
		// distance after action (for i.e. after turn to next turn)
		public int distance;
	}

	

}
