package net.osmand.plus.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LocationPoint;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.util.MapUtils;

/**
 */
public class WaypointHelper {
	OsmandApplication app;

	// every time we modify this collection, we change the reference (copy on write list)
	private List<LocationPoint> visibleLocationPoints = new ArrayList<LocationPoint>();
	private ConcurrentHashMap<LocationPoint, Integer> locationPointsStates = new ConcurrentHashMap<LocationPoint, Integer>();
	private Location lastKnownLocation;

	private static final int NOT_ANNOUNCED = 0;
	private static final int ANNOUNCED_ONCE = 1;

	private static final int LONG_ANNOUNCE_RADIUS = 500;
	private static final int SHORT_ANNOUNCE_RADIUS = 150;

	public static final int SEARCH_WAYPOINTS_RADIUS = 400;

	public WaypointHelper(OsmandApplication application) {
		app = application;
	}

	public List<LocationPoint> getAllVisibleLocationPoints() {
		return visibleLocationPoints;
	}

	public List<LocationPoint> getVisibleFavorites() {
		List<LocationPoint> points = new ArrayList<LocationPoint>();
		for (LocationPoint point : visibleLocationPoints){
			if (point instanceof FavouritePoint){
				points.add(point);
			}
		}
		return points;
	}

	public List<LocationPoint> getVisibleGpxPoints() {
		List<LocationPoint> points = new ArrayList<LocationPoint>();
		for (LocationPoint point : visibleLocationPoints){
			if (point instanceof GPXUtilities.WptPt){
				points.add(point);
			}
		}
		return points;
	}


	public void locationChanged(Location location) {
		app.getAppCustomization();
		lastKnownLocation = location;
		sortVisibleWaypoints();
		announceVisibleLocations();
	}

	private void sortVisibleWaypoints() {
		// TODO mark as passed
		if (lastKnownLocation != null) {
			Object[] loc = visibleLocationPoints.toArray();
			Arrays.sort(loc, getComparator(lastKnownLocation));
			visibleLocationPoints.clear();
			for (Object aLoc : loc) {
				visibleLocationPoints.add((LocationPoint) aLoc);
			}
		}
	}

	public void removeVisibleLocationPoint(LocationPoint lp) {
		this.visibleLocationPoints = removeFromList(visibleLocationPoints, lp);
		this.locationPointsStates.remove(lp);
	}

	public void announceVisibleLocations() {
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

	public void addVisibleLocationPoint(LocationPoint lp) {
		this.locationPointsStates.put(lp, NOT_ANNOUNCED);
		sortVisibleWaypoints();
	}

	public void clearAllVisiblePoints() {
		this.locationPointsStates.clear();
		this.visibleLocationPoints = new ArrayList<LocationPoint>();
	}

	public void setVisibleLocationPoints(List<LocationPoint> points) {
		locationPointsStates.clear();
		visibleLocationPoints.clear();
		if (points == null) {
			return;
		}
		for (LocationPoint p : points) {
			locationPointsStates.put(p, NOT_ANNOUNCED);
			visibleLocationPoints.add(p);
		}
		sortVisibleWaypoints();
	}
	
	public void setNewRoute(RouteCalculationResult res) {
		// TODO compare reset all !!
		ArrayList<LocationPoint> locationPoints = new ArrayList<LocationPoint>();
		if (app.getSettings().ANNOUNCE_NEARBY_FAVORITES.get()){
			locationPoints.addAll(app.getFavorites().getFavouritePoints());
			locationPoints.addAll(app.getAppCustomization().getWaypoints());
		}
		locationPoints.addAll(res.getLocationPoints());
		setVisibleLocationPoints(locationPoints);
	}

	public List<LocationPoint> removeFromList(List<LocationPoint> items, Object item) {
		List<LocationPoint> newArray = new ArrayList<LocationPoint>(items);
		newArray.remove(item);
		return newArray;
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

}
