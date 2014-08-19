package net.osmand.plus;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.MapUtils;

public class TargetPointsHelper {

	private List<TargetPoint> intermediatePoints = new ArrayList<TargetPoint>(); 
	private TargetPoint pointToNavigate = null;
	private TargetPoint pointToStart = null;
	private OsmandSettings settings;
	private RoutingHelper routingHelper;
	private List<StateChangedListener<Void>> listeners = new ArrayList<StateChangedListener<Void>>();
	private OsmandApplication ctx;
	
	public static class TargetPoint implements LocationPoint {
		public LatLon point;
		public String name;
		public int index;
		public boolean intermediate;
		
		public TargetPoint(LatLon point, String name) {
			this.point = point;
			this.name = name;
		}

		public TargetPoint(LatLon point, String name, int index) {
			this.point = point;
			this.name = name;
			this.index = index;
			this.intermediate = true;
		}

		public static TargetPoint create(LatLon point, String name) {
			if(point != null) {
				return new TargetPoint(point, name);
			}
			return null;
		}
		
		private String vName() {
			if(name.trim().length()==0) {
				return "";
			}
			return ": " + name;
		}
		
		public  String getVisibleName(Context ctx) {
			if (!intermediate) {
				return ctx.getString(R.string.destination_point, "")  + vName();
			} else {
				return (index + 1) + ". " + ctx.getString(R.string.intermediate_point, "")  + vName();
			}
		}

		public double getLatitude() {
			return point.getLatitude();
		}
		
		public double getLongitude() {
			return point.getLongitude();
		}

		@Override
		public String getName(Context ctx) {
			return getVisibleName(ctx);
		}

		@Override
		public int getColor() {
			return 0;
		}

		@Override
		public boolean isVisible() {
			return false;
		}
		
	}

	public TargetPointsHelper(OsmandApplication ctx){
		this.ctx = ctx;
		this.settings = ctx.getSettings();
		this.routingHelper = ctx.getRoutingHelper();
		readFromSettings(settings);
	}

	private void readFromSettings(OsmandSettings settings) {
		pointToNavigate = TargetPoint.create(settings.getPointToNavigate(), settings.getPointNavigateDescription());
		pointToStart = TargetPoint.create(settings.getPointToStart(), settings.getStartPointDescription());
		intermediatePoints.clear();
		List<LatLon> ips = settings.getIntermediatePoints();
		List<String> desc = settings.getIntermediatePointDescriptions(ips.size());
		for(int i = 0; i < ips.size(); i++) {
			intermediatePoints.add(new TargetPoint(ips.get(i), desc.get(i), i));
		}
	}
	
	public TargetPoint getPointToNavigate() {
		return pointToNavigate;
	}
	
	public TargetPoint getPointToStart() {
		return pointToStart;
	}
	
	public String getStartPointDescription(){
		return settings.getStartPointDescription();
	}
	
	
	public List<TargetPoint> getIntermediatePoints() {
		return intermediatePoints;
	}
	


	public List<TargetPoint> getIntermediatePointsWithTarget() {
		List<TargetPoint> res = new ArrayList<TargetPoint>();
		res.addAll(intermediatePoints);
		if(pointToNavigate != null) {
			res.add(pointToNavigate);
		}
		return res;
	}

	public TargetPoint getFirstIntermediatePoint(){
		if(intermediatePoints.size() > 0) {
			return intermediatePoints.get(0);
		}
		return null;
	}

	/**
	 * Clear the local and persistent waypoints list and destination.
	 */
	public void removeAllWayPoints(boolean updateRoute){
		settings.clearIntermediatePoints();
		settings.clearPointToNavigate();
		settings.clearPointToStart();
		pointToNavigate = null;
		pointToStart = null;
		intermediatePoints.clear();
		readFromSettings(settings);
		updateRouteAndReferesh(updateRoute);
	}

	/**
	 * Move an intermediate waypoint to the destination.
	 */
	public void makeWayPointDestination(boolean updateRoute, int index){
		pointToNavigate = intermediatePoints.remove(index);
		settings.setPointToNavigate(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(),
				pointToNavigate.name);
		settings.deleteIntermediatePoint(index);
		updateRouteAndReferesh(updateRoute);
	}

	public void removeWayPoint(boolean updateRoute, int index){
		if(index < 0){
			settings.clearPointToNavigate();
			pointToNavigate = null;
			int sz = intermediatePoints.size();
			if(sz > 0) {
				settings.deleteIntermediatePoint(sz- 1);
				pointToNavigate = intermediatePoints.remove(sz - 1);
				settings.setPointToNavigate(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(),
						pointToNavigate.name);
			}
		} else {
			settings.deleteIntermediatePoint(index);
			intermediatePoints.remove(index);
		}
		updateRouteAndReferesh(updateRoute);
	}

	public void updateRouteAndReferesh(boolean updateRoute) {
		if(updateRoute && ( routingHelper.isRouteBeingCalculated() || routingHelper.isRouteCalculated() ||
				routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode())) {
			updateRoutingHelper();
		}
		updateListeners();
	}

	private void updateRoutingHelper() {
		LatLon start = settings.getPointToStart();
		Location lastKnownLocation = ctx.getLocationProvider().getLastKnownLocation();
		if((routingHelper.isFollowingMode() && lastKnownLocation != null) || start == null) {
			routingHelper.setFinalAndCurrentLocation(settings.getPointToNavigate(),
				settings.getIntermediatePoints(), lastKnownLocation);
		} else {
			Location loc = wrap(start);
			routingHelper.setFinalAndCurrentLocation(settings.getPointToNavigate(),
					settings.getIntermediatePoints(), loc);
		}
	}


	private Location wrap(LatLon l) {
		if(l == null) {
			return null;
		}
		Location loc = new Location("map");
		loc.setLatitude(l.getLatitude());
		loc.setLongitude(l.getLongitude());
		return loc;
	}
	
	private Location wrap(TargetPoint l) {
		if(l == null) {
			return null;
		}
		Location loc = new Location("map");
		loc.setLatitude(l.getLatitude());
		loc.setLongitude(l.getLongitude());
		return loc;
	}

	public void addListener(StateChangedListener<Void> l) {
		listeners.add(l);
	}


	private void updateListeners() {
		for(StateChangedListener<Void> l : listeners) {
			l.stateChanged(null);
		}
	}

	public void clearPointToNavigate(boolean updateRoute) {
		settings.clearPointToNavigate();
		settings.clearIntermediatePoints();
		intermediatePoints.clear();
		readFromSettings(settings);
		updateRouteAndReferesh(updateRoute);
	}

	public void clearStartPoint(boolean updateRoute) {
		settings.clearPointToStart();
		readFromSettings(settings);
		updateRouteAndReferesh(updateRoute);
	}


	public void reorderAllTargetPoints(List<TargetPoint> point, boolean updateRoute){
		settings.clearPointToNavigate();
		if (point.size() > 0) {
			List<TargetPoint> subList = point.subList(0, point.size() - 1);
			ArrayList<String> names = new ArrayList<String>(subList.size());
			ArrayList<LatLon> ls = new ArrayList<LatLon>(subList.size());
			for(int i = 0; i < subList.size(); i++) {
				names.add(subList.get(i).name);
				ls.add(subList.get(i).point);
			}
			settings.saveIntermediatePoints(ls, names);
			TargetPoint p = point.get(point.size() - 1);
			settings.setPointToNavigate(p.getLatitude(), p.getLongitude(), p.name);
		} else {
			settings.clearIntermediatePoints();
		}
		readFromSettings(settings);
		updateRouteAndReferesh(updateRoute);
	}


	public boolean hasTooLongDistanceToNavigate() {
		if(settings.ROUTER_SERVICE.get() != RouteService.OSMAND) {
			return false;
		}
		Location current = routingHelper.getLastProjection();
		double dist = 300000;
		List<TargetPoint> list = getIntermediatePointsWithTarget();
		if(!list.isEmpty()) {
			if(current != null && MapUtils.getDistance(list.get(0).point, current.getLatitude(), current.getLongitude()) > dist) {
				return true;
			}
			for(int i = 1; i < list.size(); i++) {
				if(MapUtils.getDistance(list.get(i-1).point, list.get(i).point) > dist) {
					return true;
				}
			}
		}
		return false;
	}

	public void navigateToPoint(LatLon point, boolean updateRoute, int intermediate){
		navigateToPoint(point, updateRoute, intermediate, null);
	}

	public void navigateToPoint(LatLon point, boolean updateRoute, int intermediate, String historyName){
		if(point != null){
			if(intermediate < 0 || intermediate > intermediatePoints.size()) {
				if(intermediate > intermediatePoints.size()) {
					TargetPoint pn = getPointToNavigate();
					if(pn != null) {
						settings.insertIntermediatePoint(pn.getLatitude(), pn.getLongitude(), pn.name,
								intermediatePoints.size());
					}
				}
				settings.setPointToNavigate(point.getLatitude(), point.getLongitude(), historyName);
			} else {
				settings.insertIntermediatePoint(point.getLatitude(), point.getLongitude(), historyName,
						intermediate);
			}
		} else {
			settings.clearPointToNavigate();
			settings.clearIntermediatePoints();
		}
		readFromSettings(settings);
		updateRouteAndReferesh(updateRoute);
	}

	public void setStartPoint(LatLon startPoint, boolean updateRoute, String name) {
		if(startPoint != null) {
			settings.setPointToStart(startPoint.getLatitude(), startPoint.getLongitude(), name);
		} else {
			settings.clearPointToStart();
		}
		readFromSettings(settings);
		updateRouteAndReferesh(updateRoute);
	}

	public boolean checkPointToNavigate(){
    	if(pointToNavigate == null){
    		ctx.showToastMessage(R.string.mark_final_location_first);
			return false;
		}
    	return true;
    }

	public boolean checkPointToNavigateShort(){
    	if(pointToNavigate == null){
    		ctx.showShortToastMessage(R.string.mark_final_location_first);
			return false;
		}
    	return true;
    }

	public Location getPointToStartLocation() {
		return wrap(getPointToStart());
	}
}
