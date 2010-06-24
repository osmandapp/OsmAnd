package com.osmand.activities;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.util.FloatMath;
import android.widget.Toast;

import com.osmand.LogUtil;
import com.osmand.R;
import com.osmand.OsmandSettings.ApplicationMode;
import com.osmand.activities.RouteProvider.RouteCalculationResult;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

public class RoutingHelper {
	
	private static final org.apache.commons.logging.Log log = LogUtil.getLog(RoutingHelper.class);

	// activity to show messages & refresh map when route is calculated
	private MapActivity activity;
	
	// instead of this properties RouteCalculationResult could be used
	private List<Location> routeNodes = new ArrayList<Location>();
	private List<RouteDirectionInfo> directionInfo = null;
	private int[] listDistance = null;

	// Note always currentRoute > get(currentDirectionInfo).routeOffset, 
	//         but currentRoute <= get(currentDirectionInfo+1).routeOffset 
	private int currentDirectionInfo = 0;
	private int currentRoute = 0;
	
	
	private LatLon finalLocation;
	private Location lastFixedLocation;
	private Thread currentRunningJob;
	private long lastTimeEvaluatedRoute = 0;
	private int evalWaitInterval = 3000;

	private ApplicationMode mode;
	
	private RouteProvider provider = new RouteProvider();
	
	
	// TEST CODE
//	private static List<Location> testRoute = new ArrayList<Location>();
//	private static void addTestLocation(double lat, double lon){
//		Location l = new Location("p");
//		l.setLatitude(lat);
//		l.setLongitude(lon);
//		testRoute.add(l);
//	}
//	static {
//		addTestLocation(53.83330108605482, 27.590844306640605);
//		addTestLocation(53.69992563864831, 27.52381053894041);
//		addTestLocation(53.69728328030487, 27.521235618286113);
//	}
	// END TEST CODE
	
	
	private RoutingHelper(){
	}
	
	private static RoutingHelper INSTANCE = new RoutingHelper(); 
	public static RoutingHelper getInstance(MapActivity activity){
		INSTANCE.activity = activity;
		return INSTANCE;
	}
	
	
	
	
	public synchronized void setFinalAndCurrentLocation(LatLon finalLocation, Location currentLocation){
		this.finalLocation = finalLocation;
		this.routeNodes.clear();
		listDistance = null;
		directionInfo = null;
		evalWaitInterval = 3000;
		// to update route
		setCurrentLocation(currentLocation);
	}
	
	public void setFinalLocation(LatLon finalLocation){
		setFinalAndCurrentLocation(finalLocation, getCurrentLocation());
	}
	
	public void setAppMode(ApplicationMode mode){
		this.mode = mode;
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
	
	
	public boolean finishAtLocation(Location currentLocation) {
		Location lastPoint = routeNodes.get(routeNodes.size() - 1);
		if(currentRoute > routeNodes.size() - 3 && currentLocation.distanceTo(lastPoint) < 60){
			if(lastFixedLocation != null && lastFixedLocation.distanceTo(lastPoint) < 60){
				if(activity != null){
					showMessage(activity.getString(R.string.arrived_at_destination));
				}
				updateCurrentRoute(routeNodes.size() - 1);
				// clear final location to prevent all time showing message
				finalLocation = null;
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
					if (newDist < dist) {
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
					if(Math.abs(bearing - bearingMovement) > 130 && Math.abs(bearing - bearingMovement) < 230){
						updateCurrentRoute(currentRoute + 1);
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

		lastFixedLocation = currentLocation;
		if(calculateRoute){
			calculateRoute(lastFixedLocation, finalLocation);
		}
	}
	
	private void setNewRoute(RouteCalculationResult res){
		routeNodes = res.getLocations();
		directionInfo = res.getDirections();
		listDistance = res.getListDistance();
		currentDirectionInfo = 0;
		currentRoute = 0;
	}
	
	public synchronized int getDistance(double lat, double lon){
		if(listDistance != null && currentRoute < listDistance.length){
			int dist = listDistance[currentRoute];
			Location l = routeNodes.get(currentRoute);
			dist += MapUtils.getDistance(lat, lon, l.getLatitude(), l.getLongitude());
			return dist;
		}
		return 0;
	}
	
	public RouteDirectionInfo getNextRouteDirectionInfo(){
		if(directionInfo != null && currentDirectionInfo < directionInfo.size() - 1){
			return directionInfo.get(currentDirectionInfo + 1);
		}
		return null;
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
	
	public void calculateRoute(final Location start, final LatLon end){
		if(currentRunningJob == null){
			// do not evaluate very often
			if (System.currentTimeMillis() - lastTimeEvaluatedRoute > evalWaitInterval) {
				synchronized (this) {
					currentRunningJob = new Thread(new Runnable() {
						@Override
						public void run() {
							RouteCalculationResult res = provider.calculateRouteImpl(start, end, mode);
							synchronized (RoutingHelper.this) {
								if (res.isCalculated()) {
									setNewRoute(res);
									// reset error wait interval
									evalWaitInterval = 3000;
								} else {
									evalWaitInterval = evalWaitInterval * 4 / 3;
									if(evalWaitInterval > 120000){
										evalWaitInterval  = 120000;
									}
								}
								currentRunningJob = null;
							}
							if (activity != null) {
								if (res.isCalculated()) {
									int[] dist = res.getListDistance();
									int l = dist != null && dist.length > 0 ? dist[0] : 0;
									showMessage(activity.getString(R.string.new_route_calculated_dist) + MapUtils.getFormattedDistance(l));
									// be aware that is non ui thread
									activity.getMapView().refreshMap();
								} else {
									if (res.getErrorMessage() != null) {
										showMessage(activity.getString(R.string.error_calculating_route) + res.getErrorMessage());
									} else if (res.getLocations() == null) {
										showMessage(activity.getString(R.string.error_calculating_route_occured));
									} else {
										showMessage(activity.getString(R.string.empty_route_calculated));
									}
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
	
	private void showMessage(final String msg){
		if (activity != null) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
				}
			});
		}
	}
	
	public boolean hasPointsToShow(){
		return finalLocation != null && !routeNodes.isEmpty();
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
	
	

	
	public static enum TurnType {
		C , // continue (go straight)
		TL, // turn left
		TSLL, // turn slight left
		TSHL, // turn sharp left
		TR, // turn right
		TSLR, // turn slight right
		TSHR, // turn sharp right
		TU, // U-turn
		
		// TODO Exit3...
	}
	
	public static class RouteDirectionInfo {
		public String descriptionRoute;
		public int expectedTime;
		public float turnAngle;
		public TurnType turnType;
		public int routePointOffset;
		public int afterLeftTime;
	}

	

}
