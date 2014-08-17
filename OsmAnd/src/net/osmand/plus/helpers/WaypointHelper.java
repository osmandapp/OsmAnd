package net.osmand.plus.helpers;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.Amenity.AmenityRoutePoint;
import net.osmand.data.LocationPoint;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.routing.AlarmInfo;
import net.osmand.plus.routing.AlarmInfo.AlarmInfoType;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.util.MapUtils;

/**
 */
public class WaypointHelper {
	private static final int NOT_ANNOUNCED = 0;
	private static final int ANNOUNCED_ONCE = 1;

	private int searchDeviationRadius = 500;
	private static final int LONG_ANNOUNCE_RADIUS = 500;
	private static final int SHORT_ANNOUNCE_RADIUS = 150;

	OsmandApplication app;
	// every time we modify this collection, we change the reference (copy on write list)
	public static final int TARGETS = 0;
	public static final int ALARMS = 1;
	public static final int WAYPOINTS = 2;
	public static final int POI = 3;
	public static final int FAVORITES = 4;
	
	private List<List<LocationPointWrapper>> locationPoints = new ArrayList<List<LocationPointWrapper>>();
	private ConcurrentHashMap<LocationPoint, Integer> locationPointsStates = new ConcurrentHashMap<LocationPoint, Integer>();
	private TIntArrayList pointsProgress = new TIntArrayList();
	private Location lastKnownLocation;
	private RouteCalculationResult route;
	
	

	public WaypointHelper(OsmandApplication application) {
		app = application;
	}
	

	public List<LocationPointWrapper> getWaypoints(int type) {
		if(type >= locationPoints.size()) {
			return Collections.emptyList();
		}
		return locationPoints.get(type);
	}


	public void locationChanged(Location location) {
		app.getAppCustomization();
		lastKnownLocation = location;
		announceVisibleLocations();
	}

	public int getRouteDistance(LocationPointWrapper point) {
		return route.getDistanceToPoint(point.routeIndex);
	}

	public void removeVisibleLocationPoint(LocationPointWrapper lp) {
		// TODO FIXME
	}

	public void announceVisibleLocations() {
		// TODO FIXME
		if (lastKnownLocation != null && app.getRoutingHelper().isFollowingMode()) {
			String nameToAnnounce = null;
			List<LocationPoint> approachPoints = new ArrayList<LocationPoint>();
			List<LocationPoint> announcePoints = new ArrayList<LocationPoint>();
			for (LocationPoint point : locationPointsStates.keySet()) {
				double d1 = MapUtils.getDistance(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
						point.getLatitude(), point.getLongitude());
				int state = locationPointsStates.get(point);
				if (state <= ANNOUNCED_ONCE && app.getRoutingHelper().getVoiceRouter().isDistanceLess(lastKnownLocation.getSpeed(), d1, SHORT_ANNOUNCE_RADIUS)) {
					nameToAnnounce = (nameToAnnounce == null ? "" : ", ") + point.getName();
					locationPointsStates.remove(point);
					app.getMapActivity().getMapLayers().getMapControlsLayer().getWaypointDialogHelper().updateDialog();
					announcePoints.add(point);
				} else if (state == NOT_ANNOUNCED && app.getRoutingHelper().getVoiceRouter().isDistanceLess(lastKnownLocation.getSpeed(), d1, LONG_ANNOUNCE_RADIUS)) {
					locationPointsStates.put(point, state + 1);
					app.getMapActivity().getMapLayers().getMapControlsLayer().getWaypointDialogHelper().updateDialog();
					approachPoints.add(point);
				}
			}
			if (!announcePoints.isEmpty()) {
				app.getRoutingHelper().getVoiceRouter().announceWaypoint(announcePoints);
			}
			if (!approachPoints.isEmpty()) {
				app.getRoutingHelper().getVoiceRouter().approachWaypoint(lastKnownLocation, approachPoints);
			}

		}
	}
	
	public List<LocationPointWrapper> getAllPoints() {
		List<LocationPointWrapper> points = new ArrayList<WaypointHelper.LocationPointWrapper>();
		List<List<LocationPointWrapper>> local = locationPoints;
		TIntArrayList ps = pointsProgress;
		for(int i = 0; i < local.size(); i++) {
			List<LocationPointWrapper> loc = local.get(i);
			if(ps.get(i) < loc.size()) {
				points.addAll(loc.subList(ps.get(i), loc.size()));
			}
		}
		sortList(points);
		return points;
	}

	

	public void clearAllVisiblePoints() {
		this.locationPointsStates.clear();
		this.locationPoints = new ArrayList<List<LocationPointWrapper>>();
	}

	
	public void setNewRoute(RouteCalculationResult res) {
		this.route = res;
		recalculateAllPoints();
	}
	
	private float dist(LocationPoint l, List<Location> locations, int[] ind) {
		float dist = Float.POSITIVE_INFINITY;
		// Special iterations because points stored by pairs!
		for (int i = 1; i < locations.size(); i ++) {
			final double ld = MapUtils.getOrthogonalDistance(
					l.getLatitude(), l.getLongitude(), 
					locations.get(i - 1).getLatitude(), locations.get(i - 1).getLongitude(), 
					locations.get(i).getLatitude(), locations.get(i).getLongitude());
			if(ld < dist){
				if(ind != null) {
					ind[0] = i;
				}
				dist = (float) ld;
			}
		}
		return dist;
	}

	private void recalculateAllPoints() {
		ArrayList<List<LocationPointWrapper>> locationPoints = new ArrayList<List<LocationPointWrapper>>();
		if (route != null && !route.isEmpty()) {
			if (showFavorites()) {
				findLocationPoints(route, FAVORITES, getArray(locationPoints, FAVORITES), app.getFavorites()
						.getFavouritePoints(), announceFavorites());
			}
			calculateAlarms(getArray(locationPoints, ALARMS));
			if (showGPXWaypoints()) {
				findLocationPoints(route, WAYPOINTS, getArray(locationPoints, WAYPOINTS), app.getAppCustomization()
						.getWaypoints(), announceGPXWaypoints());
				findLocationPoints(route, WAYPOINTS, getArray(locationPoints, WAYPOINTS), route.getLocationPoints(),
						announceGPXWaypoints());
			}
			if(showPOI()) {
				calculatePoi(locationPoints);
			}
		}
		for (List<LocationPointWrapper> list : locationPoints) {
			sortList(list);
		}
		this.locationPoints = locationPoints;
		this.locationPointsStates.clear();
		TIntArrayList list = new TIntArrayList(locationPoints.size());
		list.fill(0, locationPoints.size(), 0);
		this.pointsProgress = list;
		
	}


	protected void sortList(List<LocationPointWrapper> list) {
		Collections.sort(list, new Comparator<LocationPointWrapper>() {
			@Override
			public int compare(LocationPointWrapper olhs, LocationPointWrapper orhs) {
				int lhs = olhs.routeIndex;
				int rhs = orhs.routeIndex;
				return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
			}
		});
	}


	protected void calculatePoi(ArrayList<List<LocationPointWrapper>> locationPoints) {
		PoiFilter pf = getPoiFilter();
		if (pf != null) {
			final List<Location> locs = route.getImmutableAllLocations();
			List<Amenity> amenities = app.getResourceManager().searchAmenitiesOnThePath(locs, searchDeviationRadius,
					pf, new ResultMatcher<Amenity>() {

						@Override
						public boolean publish(Amenity object) {
							return true;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
					});
			List<LocationPointWrapper> array = getArray(locationPoints, POI);
			for (Amenity a : amenities) {
				AmenityRoutePoint rp = a.getRoutePoint();
				int i = locs.indexOf(rp.pointA);
				if (i >= 0) {
					LocationPointWrapper lwp = new LocationPointWrapper(route, POI, new AmenityLocationPoint(a),
							(float) rp.deviateDistance, i);
					lwp.setAnnounce(announcePOI());
					array.add(lwp);
				}
			}
		}
	}
	
	

	private void calculateAlarms(List<LocationPointWrapper> array) {
		for(AlarmInfo i : route.getAlarmInfo()) {
			if(i.getType() == AlarmInfoType.SPEED_CAMERA) {
				if(app.getSettings().SHOW_CAMERAS.get()){
					LocationPointWrapper lw = new LocationPointWrapper(route, ALARMS, i, 0, i.getLocationIndex());	
					lw.setAnnounce(app.getSettings().SPEAK_SPEED_CAMERA.get());
					array.add(lw);
				}
			} else {
				if(app.getSettings().SHOW_TRAFFIC_WARNINGS.get()){
					LocationPointWrapper lw = new LocationPointWrapper(route, ALARMS, i, 0, i.getLocationIndex());	
					lw.setAnnounce(app.getSettings().SPEAK_TRAFFIC_WARNINGS.get());
					array.add(lw);
				}
			}
			
		}
		
	}


	private List<LocationPointWrapper> getArray(ArrayList<List<LocationPointWrapper>> array,
			int ind) {
		while(array.size() <= ind) {
			array.add(new ArrayList<WaypointHelper.LocationPointWrapper>());
		}
		return array.get(ind);
	}


	private void findLocationPoints(RouteCalculationResult rt, int type, List<LocationPointWrapper> locationPoints,
			List<? extends LocationPoint> points, boolean announce) {
		List<Location> immutableAllLocations = rt.getImmutableAllLocations();
		int[] ind = new int[1];
		for(LocationPoint p : points) {
			float dist = dist(p, immutableAllLocations, ind);
			if(dist <= searchDeviationRadius) {
				LocationPointWrapper lpw = new LocationPointWrapper(rt, type, p, dist, ind[0]);
				lpw.setAnnounce(announce);
				locationPoints.add(lpw);
			}
		}
	}


	private Comparator<Object> getComparator(final net.osmand.Location lastLocation) {
		return new Comparator<Object>() {
			@Override
			public int compare(Object locationPoint, Object locationPoint2) {
				double d1 = MapUtils.getDistance(lastLocation.getLatitude(), lastLocation.getLongitude(),
						((LocationPoint) locationPoint).getLatitude(), ((LocationPoint) locationPoint).getLongitude());
				double d2 = MapUtils.getDistance(lastLocation.getLatitude(), lastLocation.getLongitude(),
						((LocationPoint) locationPoint2).getLatitude(), ((LocationPoint) locationPoint2).getLongitude());
				return Double.compare(d1, d2);
			}

		};
	}
	
	
	/// 
	public PoiFilter getPoiFilter() {
		return app.getPoiFilters().getFilterById(app.getSettings().getPoiFilterForMap());
	}
	public boolean showPOI() {
		return app.getSettings().ANNOUNCE_NEARBY_POI.get();
	}
	
	public boolean announcePOI() {
		return app.getSettings().ANNOUNCE_NEARBY_POI.get();
	}

	public boolean showGPXWaypoints() {
		return app.getSettings().GPX_SPEAK_WPT.get();
	}
	
	public boolean announceGPXWaypoints() {
		return app.getSettings().GPX_SPEAK_WPT.get();
	}
	
	public boolean showFavorites() {
		return app.getSettings().ANNOUNCE_NEARBY_FAVORITES.get();
	}
	
	public boolean showAlarms() {
		return app.getSettings().SPEAK_SPEED_CAMERA.get() || 
				app.getSettings().SPEAK_TRAFFIC_WARNINGS.get();
	}
	
	public boolean announceFavorites() {
		return app.getSettings().ANNOUNCE_NEARBY_FAVORITES.get();
	}
	
	public class LocationPointWrapper {
		LocationPoint point;
		float deviationDistance;
		int routeIndex;
		boolean announce = true;
		RouteCalculationResult route;
		int type;
		
		
		public LocationPointWrapper(RouteCalculationResult rt, int type, LocationPoint point, float deviationDistance, int routeIndex) {
			this.route = rt;
			this.type = type;
			this.point = point;
			this.deviationDistance = deviationDistance;
			this.routeIndex = routeIndex;
		}
		
		public void setAnnounce(boolean announce) {
			this.announce = announce;
		}
		
		public float getDeviationDistance() {
			return deviationDistance;
		}

		public LocationPoint getPoint() {
			return point;
		}
		
		@Override
		public int hashCode() {
			return ((point == null) ? 0 : point.hashCode());
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LocationPointWrapper other = (LocationPointWrapper) obj;
			if (point == null) {
				if (other.point != null)
					return false;
			} else if (!point.equals(other.point))
				return false;
			return true;
		}
		
	}
	
	private class AmenityLocationPoint implements LocationPoint {

		Amenity a;
		
		public AmenityLocationPoint(Amenity a) {
			this.a = a;
		}

		@Override
		public double getLatitude() {
			return a.getLocation().getLatitude();
		}

		@Override
		public double getLongitude() {
			return a.getLocation().getLongitude();
		}

		@Override
		public String getName() {
			return OsmAndFormatter.getPoiSimpleFormat(a, app, app.getSettings().usingEnglishNames());
		}

		@Override
		public int getColor() {
			return 0;
		}

		@Override
		public boolean isVisible() {
			return true;
		}

	}

}


