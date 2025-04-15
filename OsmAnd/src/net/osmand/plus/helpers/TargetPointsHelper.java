package net.osmand.plus.helpers;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.GeocodingLookupService.OnAddressLookupResult;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class TargetPointsHelper {

	private final OsmandApplication ctx;
	private OsmandSettings settings;
	private final RoutingHelper routingHelper;

	private final List<TargetPoint> intermediatePoints = new ArrayList<>();
	private TargetPoint pointToNavigate;
	private TargetPoint pointToStart;
	private TargetPoint pointToNavigateBackup;
	private TargetPoint pointToStartBackup;
	private TargetPoint myLocationToStart;
	private final List<StateChangedListener<Void>> listeners = new ArrayList<>();
	private final List<TargetPointChangedListener> pointListeners = new ArrayList<>();

	private AddressLookupRequest startPointRequest;
	private AddressLookupRequest targetPointRequest;
	private AddressLookupRequest myLocationPointRequest;


	public interface TargetPointChangedListener {
		void onTargetPointChanged(TargetPoint targetPoint);
	}

	public TargetPointsHelper(@NonNull OsmandApplication ctx) {
		this.ctx = ctx;
		this.settings = ctx.getSettings();
		this.routingHelper = ctx.getRoutingHelper();
		readFromSettings();

		OsmAndAppCustomizationListener customizationListener = () -> {
			settings = this.ctx.getSettings();
			readFromSettings();
			updateRouteAndRefresh(true);
		};
		ctx.getAppCustomization().addListener(customizationListener);
	}

	public void lookupAddressAll() {
		lookupAddressForPointToNavigate();
		lookupAddressForStartPoint();
		for (TargetPoint targetPoint : intermediatePoints) {
			lookupAddressForIntermediatePoint(targetPoint);
		}
		lookupAddressForMyLocationPoint();
	}

	private void readFromSettings() {
		pointToNavigate = TargetPoint.create(settings.getPointToNavigate(), settings.getPointNavigateDescription());
		pointToStart = TargetPoint.createStartPoint(settings.getPointToStart(), settings.getStartPointDescription());
		pointToNavigateBackup = TargetPoint.create(settings.getPointToNavigateBackup(), settings.getPointNavigateDescriptionBackup());
		pointToStartBackup = TargetPoint.createStartPoint(settings.getPointToStartBackup(), settings.getStartPointDescriptionBackup());
		myLocationToStart = TargetPoint.create(settings.getMyLocationToStart(), settings.getMyLocationToStartDescription());
		intermediatePoints.clear();
		List<LatLon> ips = settings.getIntermediatePoints();
		List<String> desc = settings.getIntermediatePointDescriptions(ips.size());
		for(int i = 0; i < ips.size(); i++) {
			TargetPoint targetPoint = new TargetPoint(ips.get(i),
					PointDescription.deserializeFromString(desc.get(i), ips.get(i)), i);
			intermediatePoints.add(targetPoint);
		}
		if (!ctx.isApplicationInitializing()) {
			lookupAddressAll();
		}
	}

	private void readMyLocationPointFromSettings() {
		myLocationToStart = TargetPoint.create(settings.getMyLocationToStart(), settings.getMyLocationToStartDescription());
		if (!ctx.isApplicationInitializing()) {
			lookupAddressForMyLocationPoint();
		}
	}

	private void lookupAddressForIntermediatePoint(TargetPoint targetPoint) {
		if (targetPoint != null && targetPoint.getOriginalPointDescription().isSearchingAddress(ctx)) {
			cancelPointAddressRequests(targetPoint.getLatLon());
			AddressLookupRequest lookupRequest = new AddressLookupRequest(targetPoint.getLatLon(), new OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					for (TargetPoint p : intermediatePoints) {
						if (p.getLatLon().equals(targetPoint.getLatLon())) {
							p.getOriginalPointDescription().setName(address);
							settings.updateIntermediatePoint(p.getLatLon().getLatitude(), p.getLatLon().getLongitude(),
									p.getOriginalPointDescription());
							updateRouteAndRefresh(false);
							updateTargetPoint(p);
							break;
						}
					}
				}
			}, null);
			ctx.getGeocodingLookupService().lookupAddress(lookupRequest);
		}
	}

	private void lookupAddressForStartPoint() {
		if (pointToStart != null && pointToStart.isSearchingAddress(ctx)
				&& (startPointRequest == null || !startPointRequest.getLatLon().equals(pointToStart.getLatLon()))) {
			cancelStartPointAddressRequest();
			startPointRequest = new AddressLookupRequest(pointToStart.getLatLon(), new OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					startPointRequest = null;
					if (pointToStart != null) {
						pointToStart.getOriginalPointDescription().setName(address);
						settings.setPointToStart(pointToStart.getLatLon().getLatitude(), pointToStart.getLatLon().getLongitude(),
								pointToStart.getOriginalPointDescription());
						updateRouteAndRefresh(false);
						updateTargetPoint(pointToStart);
					}
				}
			}, null);
			ctx.getGeocodingLookupService().lookupAddress(startPointRequest);
		}
	}

	private void lookupAddressForPointToNavigate() {
		if (pointToNavigate != null && pointToNavigate.isSearchingAddress(ctx)
				&& (targetPointRequest == null || !targetPointRequest.getLatLon().equals(pointToNavigate.getLatLon()))) {
			cancelTargetPointAddressRequest();
			targetPointRequest = new AddressLookupRequest(pointToNavigate.getLatLon(), new OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					targetPointRequest = null;
					if (pointToNavigate != null) {
						pointToNavigate.getOriginalPointDescription().setName(address);
						settings.setPointToNavigate(pointToNavigate.getLatLon().getLatitude(), pointToNavigate.getLatLon().getLongitude(),
								pointToNavigate.getOriginalPointDescription());
						updateRouteAndRefresh(false);
						updateTargetPoint(pointToNavigate);
					}
				}
			}, null);
			ctx.getGeocodingLookupService().lookupAddress(targetPointRequest);
		}
	}

	private void lookupAddressForMyLocationPoint() {
		if (myLocationToStart != null && myLocationToStart.isSearchingAddress(ctx)
				&& (myLocationPointRequest == null || !myLocationPointRequest.getLatLon().equals(myLocationToStart.getLatLon()))) {
			cancelMyLocationPointAddressRequest();
			myLocationPointRequest = new AddressLookupRequest(myLocationToStart.getLatLon(), new OnAddressLookupResult() {
				@Override
				public void geocodingDone(String address) {
					myLocationPointRequest = null;
					if (myLocationToStart != null) {
						myLocationToStart.getOriginalPointDescription().setName(address);
						settings.setMyLocationToStart(myLocationToStart.getLatLon().getLatitude(), myLocationToStart.getLatLon().getLongitude(),
								myLocationToStart.getOriginalPointDescription());
						updateRouteAndRefresh(false);
						updateTargetPoint(myLocationToStart);
					}
				}
			}, null);
			ctx.getGeocodingLookupService().lookupAddress(myLocationPointRequest);
		}
	}

	@Nullable
	public TargetPoint getPointToNavigate() {
		return pointToNavigate;
	}

	public TargetPoint getPointToStart() {
		return pointToStart;
	}

	public TargetPoint getPointToNavigateBackup() {
		return pointToNavigateBackup;
	}

	public TargetPoint getPointToStartBackup() {
		return pointToStartBackup;
	}

	public TargetPoint getMyLocationToStart() {
		return myLocationToStart;
	}

	public boolean isBackupPointsAvailable() {
		TargetPoint startPoint = getPointToStartBackup();
		TargetPoint endPoint = getPointToNavigateBackup();
		if (startPoint == null) {
			startPoint = getMyLocationToStart();
		}
		return startPoint != null && endPoint != null;
	}

	public List<TargetPoint> getIntermediatePoints() {
		return intermediatePoints;
	}

	public List<TargetPoint> getIntermediatePointsNavigation() {
		List<TargetPoint> intermediatePoints = new ArrayList<>();
		if (settings.USE_INTERMEDIATE_POINTS_NAVIGATION.get()) {
			intermediatePoints.addAll(this.intermediatePoints);
		}
		return intermediatePoints;
	}

	public List<LatLon> getIntermediatePointsLatLon() {
		List<LatLon> intermediatePointsLatLon = new ArrayList<>();
		for (TargetPoint t : this.intermediatePoints) {
			intermediatePointsLatLon.add(t.getLatLon());
		}
		return intermediatePointsLatLon;
	}

	public List<LatLon> getIntermediatePointsLatLonNavigation() {
		List<LatLon> intermediatePointsLatLon = new ArrayList<>();
		if (settings.USE_INTERMEDIATE_POINTS_NAVIGATION.get()) {
			for (TargetPoint t : this.intermediatePoints) {
				intermediatePointsLatLon.add(t.getLatLon());
			}
		}
		return intermediatePointsLatLon;
	}

	public List<TargetPoint> getAllPoints() {
		List<TargetPoint> res = new ArrayList<>();
		if(pointToStart != null) {
			res.add(pointToStart);
		}
		res.addAll(this.intermediatePoints);
		if(pointToNavigate != null) {
			res.add(pointToNavigate);
		}
		return res;
	}

	@NonNull
	public List<TargetPoint> getIntermediatePointsWithTarget() {
		List<TargetPoint> res = new ArrayList<>(this.intermediatePoints);
		if (pointToNavigate != null) {
			res.add(pointToNavigate);
		}
		return res;
	}

	@Nullable
	public TargetPoint getFirstIntermediatePoint() {
		return getIntermediatePoint(0);
	}

	@Nullable
	public TargetPoint getIntermediatePoint(int intermediatePointIndex) {
		if (intermediatePointIndex < intermediatePoints.size()) {
			return intermediatePoints.get(intermediatePointIndex);
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
		updateMyLocationToStart();
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
		cancelPointAddressRequests(targetPoint.getLatLon());

		pointToNavigate = targetPoint;
		settings.setPointToNavigate(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(),
				pointToNavigate.getOriginalPointDescription());
		pointToNavigate.intermediate = false;
		settings.deleteIntermediatePoint(index);

		lookupAddressForPointToNavigate();
		updateRouteAndRefresh(updateRoute);
	}

	public void removeWayPoint(boolean updateRoute, int index) {
		int sz = intermediatePoints.size();
		if (index < 0) {
			cancelTargetPointAddressRequest();
			settings.clearPointToNavigate();
			pointToNavigate = null;
			if (sz > 0) {
				settings.deleteIntermediatePoint(sz - 1);
				pointToNavigate = intermediatePoints.remove(sz - 1);
				pointToNavigate.intermediate = false;
				settings.setPointToNavigate(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(),
						pointToNavigate.getOriginalPointDescription());
				lookupAddressForPointToNavigate();
			}
		} else if (sz > index) {
			settings.deleteIntermediatePoint(index);
			TargetPoint targetPoint = intermediatePoints.remove(index);
			cancelPointAddressRequests(targetPoint.getLatLon());
			int ind = 0;
			for(TargetPoint tp : intermediatePoints) {
				tp.index = ind++;
			}
		}
		updateRouteAndRefresh(updateRoute);
	}

	public void updateRouteAndRefresh(boolean updateRoute) {
		if (updateRoute && (routingHelper.isPublicTransportMode() || routingHelper.isRouteBeingCalculated() ||
				routingHelper.isRouteCalculated() || routingHelper.isFollowingMode() || routingHelper.isRoutePlanningMode())) {
			updateRoutingHelper();
		}
		updateListeners();
	}

	public void updateMyLocationToStart() {
		if (pointToStart == null) {
			Location lastKnownLocation = ctx.getLocationProvider().getLastStaleKnownLocation();
			LatLon latLon = lastKnownLocation != null ?
					new LatLon(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()) : null;
			RoutingHelperUtils.updateDrivingRegionIfNeeded(ctx, latLon, false);
			setMyLocationPoint(latLon, false, null);
		}
	}

	private void updateRoutingHelper() {
		LatLon start = settings.getPointToStart();
		LatLon finish = settings.getPointToNavigate();
		List<LatLon> is = getIntermediatePointsLatLonNavigation();
		Location lastKnownLocation = ctx.getLocationProvider().getLastStaleKnownLocation();
		if ((routingHelper.isFollowingMode() && lastKnownLocation != null) || start == null) {
			routingHelper.setFinalAndCurrentLocation(finish, is, lastKnownLocation);
		} else {
			Location loc = wrap(start);
			routingHelper.setFinalAndCurrentLocation(finish, is, loc);
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

	public void removeListener(StateChangedListener<Void> l) {
		listeners.remove(l);
	}

	private void updateListeners() {
		for(StateChangedListener<Void> l : listeners) {
			l.stateChanged(null);
		}
	}

	public void clearPointToNavigate(boolean updateRoute) {
		ctx.logRoutingEvent("clearPointToNavigate updateRoute " + updateRoute);
		cancelTargetPointAddressRequest();
		cancelAllIntermediatePointsAddressRequests();
		settings.clearPointToNavigate();
		settings.clearIntermediatePoints();
		intermediatePoints.clear();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void clearStartPoint(boolean updateRoute) {
		ctx.logRoutingEvent("clearStartPoint updateRoute " + updateRoute);
		cancelStartPointAddressRequest();
		settings.clearPointToStart();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void clearAllIntermediatePoints(boolean updateRoute) {
		ctx.logRoutingEvent("clearAllIntermediatePoints updateRoute " + updateRoute);
		cancelAllIntermediatePointsAddressRequests();
		settings.clearIntermediatePoints();
		intermediatePoints.clear();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void clearAllPoints(boolean updateRoute) {
		ctx.logRoutingEvent("clearAllPoints updateRoute " + updateRoute);
		cancelStartPointAddressRequest();
		cancelAllIntermediatePointsAddressRequests();
		cancelTargetPointAddressRequest();
		settings.clearPointToStart();
		settings.clearIntermediatePoints();
		settings.clearPointToNavigate();
		intermediatePoints.clear();
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void clearBackupPoints() {
		settings.clearPointToStartBackup();
		settings.clearIntermediatePointsBackup();
		settings.clearPointToNavigateBackup();
		readFromSettings();
	}

	public void reorderAllTargetPoints(List<TargetPoint> point, boolean updateRoute) {
		cancelTargetPointAddressRequest();
		cancelAllIntermediatePointsAddressRequests();
		settings.clearPointToNavigate();
		if (!point.isEmpty()) {
			List<TargetPoint> subList = point.subList(0, point.size() - 1);
			ArrayList<String> names = new ArrayList<>(subList.size());
			ArrayList<LatLon> ls = new ArrayList<>(subList.size());
			for (int i = 0; i < subList.size(); i++) {
				names.add(PointDescription.serializeToString(subList.get(i).getOriginalPointDescription()));
				ls.add(subList.get(i).getLatLon());
			}
			settings.saveIntermediatePoints(ls, names);
			TargetPoint p = point.get(point.size() - 1);
			settings.setPointToNavigate(p.getLatitude(), p.getLongitude(), p.getOriginalPointDescription());
		} else {
			settings.clearIntermediatePoints();
		}
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public void reorderIntermediatePoints(List<TargetPoint> points, boolean updateRoute) {
		cancelAllIntermediatePointsAddressRequests();
		if (!points.isEmpty()) {
			ArrayList<String> names = new ArrayList<>(points.size());
			ArrayList<LatLon> ls = new ArrayList<>(points.size());
			for (int i = 0; i < points.size(); i++) {
				names.add(PointDescription.serializeToString(points.get(i).getOriginalPointDescription()));
				ls.add(points.get(i).getLatLon());
			}
			settings.saveIntermediatePoints(ls, names);
		} else {
			settings.clearIntermediatePoints();
		}
		readFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public boolean hasTooLongDistanceToNavigate() {
		if (routingHelper.getAppMode().getRouteService() != RouteService.OSMAND) {
			return false;
		}
		if (settings.ROUTING_TYPE.get().isHHRouting() &&
				(ApplicationMode.CAR.isDerivedRoutingFrom(routingHelper.getAppMode())
						|| ApplicationMode.BICYCLE.isDerivedRoutingFrom(routingHelper.getAppMode()))) {
			return false;
		}
		Location current = routingHelper.getLastProjection();
        double dist = 400000;
        if (ApplicationMode.BICYCLE.isDerivedRoutingFrom(routingHelper.getAppMode())
                && settings.getCustomRoutingBooleanProperty("height_obstacles", false).getModeValue(routingHelper.getAppMode())) { dist = 50000; }
		List<TargetPoint> list = getIntermediatePointsWithTarget();
		if(!list.isEmpty()) {
			if(current != null && MapUtils.getDistance(list.get(0).getLatLon(), current.getLatitude(), current.getLongitude()) > dist) {
				return true;
			}
			for(int i = 1; i < list.size(); i++) {
				if(MapUtils.getDistance(list.get(i - 1).getLatLon(), list.get(i).getLatLon()) > dist) {
					return true;
				}
			}
		}
		return false;
	}

	public void navigateToPoint(LatLon point, boolean updateRoute, int intermediate) {
		navigateToPoint(point, updateRoute, intermediate, null);
	}

	public void navigateToPoint(LatLon point, boolean updateRoute, int intermediate, PointDescription historyName) {
		ctx.logRoutingEvent("navigateToPoint point " + point + " updateRoute " + updateRoute + " intermediate " + intermediate + " historyName " + historyName);
		if (point != null) {
			PointDescription pointDescription;
			if (historyName == null) {
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
			} else {
				pointDescription = historyName;
			}
			if (pointDescription.isLocation() && Algorithms.isEmpty(pointDescription.getName())) {
				pointDescription.setName(PointDescription.getSearchAddressStr(ctx));
			}

			if (intermediate < 0 || intermediate > intermediatePoints.size()) {
				if(intermediate > intermediatePoints.size()) {
					TargetPoint pn = getPointToNavigate();
					if(pn != null) {
						settings.insertIntermediatePoint(pn.getLatitude(), pn.getLongitude(), pn.getOriginalPointDescription(),
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

	public void setStartPoint(LatLon startPoint, boolean updateRoute, PointDescription name) {
		ctx.logRoutingEvent("setStartPoint startPoint " + startPoint + " updateRoute " + updateRoute + " name " + name);
		if (startPoint != null) {
			PointDescription pointDescription;
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

	public void setMyLocationPoint(LatLon startPoint, boolean updateRoute, PointDescription name) {
		ctx.logRoutingEvent("setMyLocationPoint startPoint " + startPoint + " updateRoute " + updateRoute + " name " + name);
		if (startPoint != null) {
			PointDescription pointDescription;
			if (name == null) {
				pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");
			} else {
				pointDescription = name;
			}
			if (pointDescription.isLocation() && Algorithms.isEmpty(pointDescription.getName())) {
				pointDescription.setName(PointDescription.getSearchAddressStr(ctx));
			}
			settings.setMyLocationToStart(startPoint.getLatitude(), startPoint.getLongitude(), pointDescription);
		} else {
			settings.clearMyLocationToStart();
		}
		readMyLocationPointFromSettings();
		updateRouteAndRefresh(updateRoute);
	}

	public boolean checkPointToNavigateShort(){
    	if (pointToNavigate == null){
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

	private void cancelMyLocationPointAddressRequest() {
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

	public void addPointListener(TargetPointChangedListener l) {
		if (!pointListeners.contains(l)) {
			pointListeners.add(l);
		}
	}

	public void removePointListener(TargetPointChangedListener l) {
		pointListeners.remove(l);
	}

	private void updateTargetPoint(TargetPoint targetPoint) {
		for (TargetPointChangedListener l : pointListeners) {
			l.onTargetPointChanged(targetPoint);
		}
	}
}
