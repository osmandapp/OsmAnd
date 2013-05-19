package net.osmand.plus;


import java.util.ArrayList;
import java.util.List;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.plus.routing.RoutingHelper;

public class TargetPointsHelper {

	private List<LatLon> intermediatePoints = new ArrayList<LatLon>(); 
	private List<String> intermediatePointNames = new ArrayList<String>();
	private LatLon pointToNavigate = null;
	private OsmandSettings settings;
	private RoutingHelper routingHelper;
	private ClientContext ctx;
	private List<StateChangedListener<Void>> listeners = new ArrayList<StateChangedListener<Void>>();
	
	public TargetPointsHelper(ClientContext ctx){
		this.ctx = ctx;
		this.settings = ctx.getSettings();
		this.routingHelper = ctx.getRoutingHelper();
		readFromSettings(settings);
	}

	private void readFromSettings(OsmandSettings settings) {
		pointToNavigate = settings.getPointToNavigate();
		intermediatePoints.clear();
		intermediatePointNames.clear();
		intermediatePoints.addAll(settings.getIntermediatePoints());
		intermediatePointNames.addAll(settings.getIntermediatePointDescriptions(intermediatePoints.size()));
	}
	
	public LatLon getPointToNavigate() {
		return pointToNavigate;
	}
	
	public String getPointNavigateDescription(){
		return settings.getPointNavigateDescription();
	}
	
	public List<String> getIntermediatePointNames() {
		return intermediatePointNames;
	}
	
	public List<LatLon> getIntermediatePoints() {
		return intermediatePoints;
	}
	
	public List<LatLon> getIntermediatePointsWithTarget() {
		List<LatLon> res = new ArrayList<LatLon>();
		res.addAll(intermediatePoints);
		if(pointToNavigate != null) {
			res.add(pointToNavigate);
		}
		return res;
	}
	
	public List<String> getIntermediatePointNamesWithTarget() {
		List<String> res = new ArrayList<String>();
		res.addAll(intermediatePointNames);
		if(pointToNavigate != null) {
			res.add(getPointNavigateDescription());
		}
		return res;
	}

	public LatLon getFirstIntermediatePoint(){
		if(intermediatePoints.size() > 0) {
			return intermediatePoints.get(0);
		}
		return null;
	}
	
	/**
	 * Clear the local and persistent waypoints list and destination.
	 */
	public void removeAllWayPoints(boolean updateRoute){
		settings.clearPointToNavigate();
		pointToNavigate = null;		
		settings.clearIntermediatePoints();
		settings.clearPointToNavigate(); 
		intermediatePoints.clear();
		intermediatePointNames.clear();	
		updateRouteAndReferesh(updateRoute);
	}

	/**
	 * Move an intermediate waypoint to the destination.
	 */
	public void makeWayPointDestination(boolean updateRoute, int index){
		pointToNavigate = intermediatePoints.remove(index);
		settings.setPointToNavigate(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), 
				intermediatePointNames.remove(index));		
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
						intermediatePointNames.remove(sz - 1));
			}
		} else {
			settings.deleteIntermediatePoint(index);
			intermediatePoints.remove(index);
			intermediatePointNames.remove(index);	
		}
		updateRouteAndReferesh(updateRoute);
	}

	private void updateRouteAndReferesh(boolean updateRoute) {
		if(updateRoute && ( routingHelper.isRouteBeingCalculated() || routingHelper.isRouteCalculated() ||
				routingHelper.isFollowingMode())) {
			Location lastKnownLocation = ctx.getLastKnownLocation();
			routingHelper.setFinalAndCurrentLocation(settings.getPointToNavigate(),
					settings.getIntermediatePoints(), lastKnownLocation, routingHelper.getCurrentGPXRoute());
		}
		updateListeners();
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
		readFromSettings(settings);
		updateRouteAndReferesh(updateRoute);
	}
	
	
	public void reorderAllTargetPoints(List<LatLon> point, 
			List<String> names, boolean updateRoute){
		settings.clearPointToNavigate();
		if (point.size() > 0) {
			settings.saveIntermediatePoints(point.subList(0, point.size() - 1), names.subList(0, point.size() - 1));
			LatLon p = point.get(point.size() - 1);
			String nm = names.get(point.size() - 1);
			settings.setPointToNavigate(p.getLatitude(), p.getLongitude(), nm);
		} else {
			settings.clearIntermediatePoints();
		}
		readFromSettings(settings);
		updateRouteAndReferesh(updateRoute);
	}
	
	public void navigateToPoint(LatLon point, boolean updateRoute, int intermediate){
		if(point != null){
			if(intermediate < 0) {
				settings.setPointToNavigate(point.getLatitude(), point.getLongitude(), null);
			} else {
				settings.insertIntermediatePoint(point.getLatitude(), point.getLongitude(), null, 
						intermediate, false);
			}
		} else {
			settings.clearPointToNavigate();
			settings.clearIntermediatePoints();
		}
		readFromSettings(settings);
		updateRouteAndReferesh(updateRoute);
		
	}
	
	public boolean checkPointToNavigate(ClientContext ctx ){
    	if(pointToNavigate == null){
    		ctx.showToastMessage(R.string.mark_final_location_first);
			return false;
		}
    	return true;
    }
	
	

	public void updatePointsFromSettings() {
		readFromSettings(settings);		
	}

	
}
