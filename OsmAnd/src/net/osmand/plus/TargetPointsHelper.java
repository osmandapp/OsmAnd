package net.osmand.plus;


import android.content.Context;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class TargetPointsHelper {

	private List<TargetPoint> intermediatePoints = new ArrayList<TargetPoint>(); 
	private TargetPoint pointToNavigate = null;
	private TargetPoint pointToStart = null;
	private OsmandSettings settings;
	private RoutingHelper routingHelper;
	private List<StateChangedListener<Void>> listeners = new ArrayList<StateChangedListener<Void>>();
	private OsmandApplication ctx;

	private AddressLookupRequest startPointRequest;
	private AddressLookupRequest targetPointRequest;

	public static class TargetPoint implements LocationPoint {
		public LatLon point;
		private PointDescription pointDescription;
		public int index;
		public boolean intermediate;
		public boolean start;

		public TargetPoint(LatLon point, PointDescription name) {
			this.point = point;
			this.pointDescription = name;
		}

		public TargetPoint(LatLon point, PointDescription name, int index) {
			this.point = point;
			this.pointDescription = name;
			this.index = index;
			this.intermediate = true;
		}
		
		public PointDescription getPointDescription(Context ctx) {
			if (!intermediate) {
				return new PointDescription(PointDescription.POINT_TYPE_TARGET, ctx.getString(R.string.destination_point, ""), 
						getOnlyName());
			} else {
				return new PointDescription(PointDescription.POINT_TYPE_TARGET, (index + 1) + ". " + ctx.getString(R.string.intermediate_point, ""), 
						getOnlyName());
			}
		}
		
		public PointDescription getOriginalPointDescription() {
			return pointDescription;
		};
		
		public String getOnlyName() {
			return pointDescription == null ? "" : pointDescription.getName();
		}

		public boolean isSearchingAddress(Context ctx) {
			return pointDescription != null && pointDescription.isSearchingAddress(ctx);
		}

		public static TargetPoint create(LatLon point, PointDescription name) {
			if(point != null) {
				return new TargetPoint(point, name);
			}
			return null;
		}

		public static TargetPoint createStartPoint(LatLon point, PointDescription name) {
			if (point != null) {
				TargetPoint target = new TargetPoint(point, name);
				target.start = true;
				return target;
			}
			return null;
		}

		public double getLatitude() {
			return point.getLatitude();
		}
		
		public double getLongitude() {
			return point.getLongitude();
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
		readFromSettings();
	}

	private void readFromSettings() {
		pointToNavigate = TargetPoint.create(settings.getPointToNavigate(), settings.getPointNavigateDescription());
		lookupAddressForPointToNavigate();

		pointToStart = TargetPoint.createStartPoint(settings.getPointToStart(), settings.getStartPointDescription());
		lookupAddessForStartPoint();

		intermediatePoints.clear();
		List<LatLon> ips = settings.getIntermediatePoints();
		List<String> desc = settings.getIntermediatePointDescriptions(ips.size());
		for(int i = 0; i < ips.size(); i++) {
			final TargetPoint targetPoint = new TargetPoint(ips.get(i),
					PointDescription.deserializeFromString(desc.get(i), ips.get(i)), i);
			intermediatePoints.add(targetPoint);
			lookupAddressForIntermediatePoint(targetPoint);
		}
	}

	private void lookupAddressForIntermediatePoint(final TargetPoint targetPoint) {
		if (targetPoint != null && targetPoint.pointDescription.isSearchingAddress(ctx)) {
			cancelPointAddressRequests(targetPoint.point);
			AddressLookupRequest lookupRequest = new AddressLookupRequest(targetPoint.point, new GeocodingLookupService.OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					if (intermediatePoints.contains(targetPoint)) {
						targetPoint.pointDescription.setName(address);
						settings.updateIntermediatePoint(targetPoint.point.getLatitude(), targetPoint.point.getLongitude(),
								targetPoint.pointDescription);
						updateRouteAndRefresh(false);
					}
				}
			}, null);
			ctx.getGeocodingLookupService().lookupAddress(lookupRequest);
		}
	}

	private void lookupAddessForStartPoint() {
		if (pointToStart != null && pointToStart.isSearchingAddress(ctx)
				&& (startPointRequest == null || !startPointRequest.getLatLon().equals(pointToStart.point))) {
			cancelStartPointAddressRequest();
			startPointRequest = new AddressLookupRequest(pointToStart.point, new GeocodingLookupService.OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					startPointRequest = null;
					if (pointToStart != null) {
						pointToStart.pointDescription.setName(address);
						settings.setPointToStart(pointToStart.point.getLatitude(), pointToStart.point.getLongitude(),
								pointToStart.pointDescription);
						updateRouteAndRefresh(false);
					}
				}
			}, null);
			ctx.getGeocodingLookupService().lookupAddress(startPointRequest);
		}
	}

	private void lookupAddressForPointToNavigate() {
		if (pointToNavigate != null && pointToNavigate.isSearchingAddress(ctx)
				&& (targetPointRequest == null || !targetPointRequest.getLatLon().equals(pointToNavigate.point))) {
			cancelTargetPointAddressRequest();
			targetPointRequest = new AddressLookupRequest(pointToNavigate.point, new GeocodingLookupService.OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					targetPointRequest = null;
					if (pointToNavigate != null) {
						pointToNavigate.pointDescription.setName(address);
						settings.setPointToNavigate(pointToNavigate.point.getLatitude(), pointToNavigate.point.getLongitude(),
								pointToNavigate.pointDescription);
						updateRouteAndRefresh(false);
					}
				}
			}, null);
			ctx.getGeocodingLookupService().lookupAddress(targetPointRequest);
		}
	}

	public TargetPoint getPointToNavigate() {
		return pointToNavigate;
	}
	
	public TargetPoint getPointToStart() {
		return pointToStart;
	}
	
	public PointDescription getStartPointDescription(){
		return settings.getStartPointDescription();
	}
	
	
	public List<TargetPoint> getIntermediatePoints() {
		return intermediatePoints;
	}
	
	public List<TargetPoint> getIntermediatePointsNavigation() {
		List<TargetPoint> intermediatePoints = new ArrayList<>();
		if (settings.USE_INTERMEDIATE_POINTS_NAVIGATION.get()) {
			for (TargetPoint t : this.intermediatePoints) {
				intermediatePoints.add(t);
			}
		}
		return intermediatePoints;
	}
	
	public List<LatLon> getIntermediatePointsLatLon() {
		List<LatLon> intermediatePointsLatLon = new ArrayList<LatLon>();
		for (TargetPoint t : this.intermediatePoints) {
			intermediatePointsLatLon.add(t.point);
		}
		return intermediatePointsLatLon;
	}
	
	public List<LatLon> getIntermediatePointsLatLonNavigation() {
		List<LatLon> intermediatePointsLatLon = new ArrayList<LatLon>();
		if (settings.USE_INTERMEDIATE_POINTS_NAVIGATION.get()) {
			for (TargetPoint t : this.intermediatePoints) {
				intermediatePointsLatLon.add(t.point);
			}
		}
		return intermediatePointsLatLon;
	}

	public List<TargetPoint> getAllPoints() {
		List<TargetPoint> res = new ArrayList<TargetPoint>();
		if(pointToStart != null) {
			res.add(pointToStart);
		}
		res.addAll(this.intermediatePoints);
		if(pointToNavigate != null) {
			res.add(pointToNavigate);
		}
		return res;
	}

	public List<TargetPoint> getIntermediatePointsWithTarget() {
		List<TargetPoint> res = new ArrayList<TargetPoint>();
		res.addAll(this.intermediatePoints);
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

	public void restoreTargetPoints(boolean updateRoute) {
		settings.restoreTargetPoints();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

		/**
		 * Clear the local and persistent waypoints list and destination.
		 */
	public void removeAllWayPoints(boolean updateRoute, boolean clearBackup){
		cancelStartPointAddressRequest();
		cancelTargetPointAddressRequest();
		cancelAllIntermediatePointsAddressRequests();

		settings.clearIntermediatePoints();
		settings.clearPointToNavigate();
		settings.clearPointToStart();
		if (clearBackup) {
			settings.backupTargetPoints();
		}
		pointToNavigate = null;
		pointToStart = null;
		intermediatePoints.clear();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	/**
	 * Move an intermediate waypoint to the destination.
	 */
	public void makeWayPointDestination(boolean updateRoute, int index){
		TargetPoint targetPoint = intermediatePoints.remove(index);
		cancelTargetPointAddressRequest();
		cancelPointAddressRequests(targetPoint.point);

		pointToNavigate = targetPoint;
		settings.setPointToNavigate(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(),
				pointToNavigate.pointDescription);
		pointToNavigate.intermediate = false;
		settings.deleteIntermediatePoint(index);

		lookupAddressForPointToNavigate();
		updateRouteAndRefresh(updateRoute);
	}

	public void removeWayPoint(boolean updateRoute, int index){
		if (index < 0) {
			cancelTargetPointAddressRequest();
			settings.clearPointToNavigate();
			pointToNavigate = null;
			int sz = intermediatePoints.size();
			if (sz > 0) {
				settings.deleteIntermediatePoint(sz - 1);
				pointToNavigate = intermediatePoints.remove(sz - 1);
				pointToNavigate.intermediate = false;
				settings.setPointToNavigate(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(),
						pointToNavigate.pointDescription);
				lookupAddressForPointToNavigate();
			}
		} else {
			settings.deleteIntermediatePoint(index);
			TargetPoint targetPoint = intermediatePoints.remove(index);
			cancelPointAddressRequests(targetPoint.point);
			int ind = 0;
			for(TargetPoint tp : intermediatePoints) {
				tp.index = ind++;
			}
		}
		updateRouteAndRefresh(updateRoute);
	}

	public void updateRouteAndRefresh(boolean updateRoute) {
		if(updateRoute && ( routingHelper.isRouteBeingCalculated() || routingHelper.isRouteCalculated() ||
				routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode())) {
			updateRoutingHelper();
		}
		updateListeners();
	}

	private void updateRoutingHelper() {
		LatLon start = settings.getPointToStart();
		Location lastKnownLocation = ctx.getLocationProvider().getLastKnownLocation();
		List<LatLon> is = getIntermediatePointsLatLonNavigation();
		if((routingHelper.isFollowingMode() && lastKnownLocation != null) || start == null) {
			routingHelper.setFinalAndCurrentLocation(settings.getPointToNavigate(),
					is, lastKnownLocation);
		} else {
			Location loc = wrap(start);
			routingHelper.setFinalAndCurrentLocation(settings.getPointToNavigate(),
					is, loc);
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
		cancelTargetPointAddressRequest();
		cancelAllIntermediatePointsAddressRequests();
		settings.clearPointToNavigate();
		settings.clearIntermediatePoints();
		intermediatePoints.clear();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void clearStartPoint(boolean updateRoute) {
		cancelStartPointAddressRequest();
		settings.clearPointToStart();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}


	public void reorderAllTargetPoints(List<TargetPoint> point, boolean updateRoute) {
		cancelTargetPointAddressRequest();
		cancelAllIntermediatePointsAddressRequests();
		settings.clearPointToNavigate();
		if (point.size() > 0) {
			List<TargetPoint> subList = point.subList(0, point.size() - 1);
			ArrayList<String> names = new ArrayList<String>(subList.size());
			ArrayList<LatLon> ls = new ArrayList<LatLon>(subList.size());
			for(int i = 0; i < subList.size(); i++) {
				names.add(PointDescription.serializeToString(subList.get(i).pointDescription));
				ls.add(subList.get(i).point);
			}
			settings.saveIntermediatePoints(ls, names);
			TargetPoint p = point.get(point.size() - 1);
			settings.setPointToNavigate(p.getLatitude(), p.getLongitude(), p.pointDescription);
		} else {
			settings.clearIntermediatePoints();
		}
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}


	public boolean hasTooLongDistanceToNavigate() {
		if(settings.ROUTER_SERVICE.get() != RouteService.OSMAND) {
			return false;
		}
		Location current = routingHelper.getLastProjection();
		double dist = 400000;
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

	public void navigateToPoint(final LatLon point, boolean updateRoute, int intermediate, PointDescription historyName){
		if(point != null){
			final PointDescription pointDescription;
			if (historyName == null) {
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
			} else {
				pointDescription = historyName;
			}
			if (pointDescription.isLocation() && Algorithms.isEmpty(pointDescription.getName())) {
				pointDescription.setName(PointDescription.getSearchAddressStr(ctx));
			}

			if(intermediate < 0 || intermediate > intermediatePoints.size()) {
				if(intermediate > intermediatePoints.size()) {
					final TargetPoint pn = getPointToNavigate();
					if(pn != null) {
						settings.insertIntermediatePoint(pn.getLatitude(), pn.getLongitude(), pn.pointDescription,
								intermediatePoints.size());
					}
				}
				settings.setPointToNavigate(point.getLatitude(), point.getLongitude(), pointDescription);
			} else {
				settings.insertIntermediatePoint(point.getLatitude(), point.getLongitude(), pointDescription,
						intermediate);
			}
		} else {
			cancelTargetPointAddressRequest();
			cancelAllIntermediatePointsAddressRequests();
			settings.clearPointToNavigate();
			settings.clearIntermediatePoints();
		}
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void setStartPoint(final LatLon startPoint, boolean updateRoute, PointDescription name) {
		if(startPoint != null) {
			final PointDescription pointDescription;
			if (name == null) {
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
			} else {
				pointDescription = name;
			}
			if (pointDescription.isLocation() && Algorithms.isEmpty(pointDescription.getName())) {
				pointDescription.setName(PointDescription.getSearchAddressStr(ctx));
			}
			settings.setPointToStart(startPoint.getLatitude(), startPoint.getLongitude(), pointDescription);
		} else {
			settings.clearPointToStart();
		}
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
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

	private void cancelStartPointAddressRequest() {
		if (startPointRequest != null) {
			ctx.getGeocodingLookupService().cancel(startPointRequest);
			startPointRequest = null;
		}
	}

	private void cancelTargetPointAddressRequest() {
		if (targetPointRequest != null) {
			ctx.getGeocodingLookupService().cancel(targetPointRequest);
			targetPointRequest = null;
		}
	}

	private void cancelAllIntermediatePointsAddressRequests() {
		List<LatLon> intermediatePointsLatLon = getIntermediatePointsLatLon();
		for (LatLon latLon : intermediatePointsLatLon) {
			cancelPointAddressRequests(latLon);
		}
	}

	private void cancelPointAddressRequests(LatLon latLon) {
		if (latLon != null) {
			ctx.getGeocodingLookupService().cancel(latLon);
		}
	}
}
