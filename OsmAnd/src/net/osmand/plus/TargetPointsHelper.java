package net.osmand.plus;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.widget.Toast;

import net.osmand.access.AccessibleToast;
import net.osmand.osm.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;

public class TargetPointsHelper {

	private List<LatLon> intermediatePoints = new ArrayList<LatLon>(); 
	private List<String> intermediatePointNames = new ArrayList<String>();
	private LatLon pointToNavigate = null;
	private OsmandSettings settings;
	private RoutingHelper routingHelper;
	
	public TargetPointsHelper(OsmandSettings settings, RoutingHelper routingHelper){
		this.settings = settings;
		this.routingHelper = routingHelper;
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
	
	
	public void removeWayPoint(MapActivity map, boolean updateRoute, int index){
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
		updateRouteAndReferesh(map, updateRoute);
	}

	private void updateRouteAndReferesh(MapActivity map, boolean updateRoute) {
		if(updateRoute && ( routingHelper.isRouteBeingCalculated() || routingHelper.isRouteCalculated() ||
				routingHelper.isFollowingMode())) {
			Location lastKnownLocation = map == null ? routingHelper.getLastProjection() :  map.getLastKnownLocation();
			routingHelper.setFinalAndCurrentLocation(settings.getPointToNavigate(),
					settings.getIntermediatePoints(), lastKnownLocation, routingHelper.getCurrentGPXRoute());
		}
		if(map != null) {
			map.getMapView().refreshMap();
		}
	}
	
	
	public void clearPointToNavigate(MapActivity map, boolean updateRoute) {
		settings.clearPointToNavigate();
		settings.clearIntermediatePoints();
		readFromSettings(settings);
		updateRouteAndReferesh(map, updateRoute);
	}
	
	public void reorderAllTargetPoints(MapActivity map, List<LatLon> point, 
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
		updateRouteAndReferesh(map, updateRoute);
	}
	
	public void navigateToPoint(MapActivity map, LatLon point, boolean updateRoute, int intermediate){
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
		updateRouteAndReferesh(map, updateRoute);
		
	}
	
	public boolean checkPointToNavigate(Context ctx ){
    	if(pointToNavigate == null){
			AccessibleToast.makeText(ctx, R.string.mark_final_location_first, Toast.LENGTH_LONG).show();
			return false;
		}
    	return true;
    }
	
	public void navigatePointDialogAndLaunchMap(final Context ctx, final double lat, final double lon, final String name){
    	if(pointToNavigate != null) {
    		Builder builder = new AlertDialog.Builder(ctx);
    		builder.setTitle(R.string.new_destination_point_dialog);
    		builder.setItems(new String[] {
    				ctx.getString(R.string.replace_destination_point),
    				ctx.getString(R.string.add_as_first_destination_point),
    				ctx.getString(R.string.add_as_last_destination_point)
    		}, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(which == 0) {
						settings.setPointToNavigate(lat, lon, true, name);
					} else if(which == 2) {
						int sz = intermediatePoints.size();
						LatLon pt = pointToNavigate;
						settings.insertIntermediatePoint(pt.getLatitude(), pt.getLongitude(), 
								settings.getPointNavigateDescription(), sz, true);
						settings.setPointToNavigate(lat, lon, true, name);
					} else {
						settings.insertIntermediatePoint(lat, lon, name, 0, true);
					}
					readFromSettings(settings);
					MapActivity.launchMapActivityMoveToTop(ctx);
				}
			});
    		builder.show();
    	} else {
    		settings.setPointToNavigate(lat, lon, true, name);
    		readFromSettings(settings);
    		MapActivity.launchMapActivityMoveToTop(ctx);
    	}
    }
}
