package net.osmand.plus.mapcontextmenu.controllers;

import static net.osmand.util.MapUtils.ROUNDING_ERROR;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.data.TransportStopAggregated;
import net.osmand.data.TransportStopExit;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.TransportStopMenuBuilder;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TransportStopController extends MenuController {

	public static final int SHOW_STOPS_RADIUS_METERS_UI = 150;
	public static final int SHOW_STOPS_RADIUS_METERS = SHOW_STOPS_RADIUS_METERS_UI * 6 / 5;
	public static final int SHOW_SUBWAY_STOPS_FROM_ENTRANCES_RADIUS_METERS = 400;
	public static final int MAX_DISTANCE_BETWEEN_AMENITY_AND_LOCAL_STOPS = 20;

	private TransportStop transportStop;
	private final List<TransportStopRoute> routesNearby = new ArrayList<>();
	private final List<TransportStopRoute> routesOnTheSameExit = new ArrayList<>();
	private TransportStopType topType;

	public TransportStopController(@NonNull MapActivity mapActivity,
	                               @NonNull PointDescription pointDescription,
	                               @NonNull TransportStop transportStop) {
		super(new TransportStopMenuBuilder(mapActivity, transportStop), pointDescription, mapActivity);
		this.transportStop = transportStop;
		processRoutes();
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof TransportStop) {
			this.transportStop = (TransportStop) object;
			processRoutes();
		}
	}

	public void processRoutes() {
		routesOnTheSameExit.clear();
		routesNearby.clear();
		processTransportStop(routesOnTheSameExit, routesNearby);
	}

	@Override
	protected Object getObject() {
		return transportStop;
	}

	@Override
	public int getRightIconId() {
		if (topType == null) {
			return R.drawable.mx_public_transport;
		} else {
			return topType.getTopResourceId();
		}
	}

	@Override
	public List<TransportStopRoute> getTransportStopRoutes() {
		List<TransportStopRoute> routes = new ArrayList<>(routesOnTheSameExit);
		routes.addAll(routesNearby);
		return routes;
	}

	@Override
	protected List<TransportStopRoute> getSubTransportStopRoutes(boolean nearby) {
		return nearby ? routesNearby : routesOnTheSameExit;
	}

	@Override
	public boolean needStreetName() {
		return Algorithms.isEmpty(getNameStr());
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@NonNull
	@Override
	public String getNameStr() {
		Amenity amenity = transportStop.getAmenity();
		if (amenity == null) {
			return transportStop.getName(getPreferredMapLang(), isTransliterateNames());
		} else {
			return amenity.getName(getPreferredMapLang(), isTransliterateNames());
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		Amenity amenity = transportStop.getAmenity();
		if (amenity != null) {
			AmenityMenuController.addTypeMenuItem(amenity, builder);
		} else {
			super.addPlainMenuItems(typeStr, pointDescription, latLon);
		}
	}

	private void processTransportStop(List<TransportStopRoute> routesOnTheSameExit, List<TransportStopRoute> routesNearby) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			boolean useEnglishNames = app.getSettings().usingEnglishNames();
			if (transportStop.getTransportStopAggregated() == null) {
				processTransportStopAggregated(app, transportStop);
			}
			ArrayList<TransportStop> transportStopsSameExit = new ArrayList<>(transportStop.getLocalTransportStops());
			ArrayList<TransportStop> nearbyTransportStops = new ArrayList<>(transportStop.getNearbyTransportStops());

			addTransportStopRoutes(app, transportStopsSameExit, routesOnTheSameExit, useEnglishNames);
			sortTransportStopRoutes(routesOnTheSameExit);
			if (topType == null && !Algorithms.isEmpty(routesOnTheSameExit)) {
				topType = routesOnTheSameExit.get(0).type;
			}
			addTransportStopRoutes(app, nearbyTransportStops, routesNearby, useEnglishNames);
			sortTransportStopRoutes(routesNearby);
		}
	}

	private void sortTransportStopRoutes(List<TransportStopRoute> routes) {
		Collections.sort(routes, new Comparator<TransportStopRoute>() {

			@Override
			public int compare(TransportStopRoute o1, TransportStopRoute o2) {
//					int radEqual = 50;
//					int dist1 = o1.distance / radEqual;
//					int dist2 = o2.distance / radEqual;
//					if (dist1 != dist2) {
//						return Algorithms.compare(dist1, dist2);
//					}
				int i1 = Algorithms.extractFirstIntegerNumber(o1.route.getRef());
				int i2 = Algorithms.extractFirstIntegerNumber(o2.route.getRef());
				if (i1 != i2) {
					return Algorithms.compare(i1, i2);
				}
				return o1.desc.compareTo(o2.desc);
			}
		});
	}

	private void addTransportStopRoutes(OsmandApplication app, List<TransportStop> stops, List<TransportStopRoute> routes, boolean useEnglishNames) {
		for (TransportStop tstop : stops) {
			if (!tstop.isDeleted()) {
				addRoutes(app, routes, useEnglishNames, tstop, transportStop, (int) MapUtils.getDistance(tstop.getLocation(), transportStop.getLocation()));
			}
		}
	}

	private void addRoutes(OsmandApplication app, List<TransportStopRoute> routes, boolean useEnglishNames, TransportStop s, TransportStop refStop, int dist) {
		List<TransportRoute> rts = app.getResourceManager().getRoutesForStop(s);
		if (rts != null) {
			for (TransportRoute rs : rts) {
				boolean routeAlreadyAdded = checkSameRoute(routes, rs);
				if (routeAlreadyAdded) {
					continue;
				}
				TransportStopType type = TransportStopType.findType(rs.getType());
				if (topType == null && type != null && type.isTopType()) {
					topType = type;
				}
				TransportStopRoute r = new TransportStopRoute();
				r.type = type;
				r.desc = useEnglishNames ? rs.getEnName(true) : rs.getName();
				r.route = rs;
				r.refStop = refStop;
				r.stop = s;
				r.distance = dist;
				routes.add(r);
			}
		}
	}

	private static void sortTransportStopsExits(@NonNull LatLon latLon, @NonNull List<TransportStop> transportStops) {
		for (TransportStop transportStop : transportStops) {
			for (TransportStopExit exit : transportStop.getExits()) {
				int distance = (int) MapUtils.getDistance(latLon, exit.getLocation());
				if (transportStop.distance > distance) {
					transportStop.distance = distance;
				}
			}
		}
		Collections.sort(transportStops, new Comparator<TransportStop>() {
			@Override
			public int compare(TransportStop s1, TransportStop s2) {
				return Algorithms.compare(s1.distance, s2.distance);
			}
		});
	}

	private static void sortTransportStops(@NonNull LatLon latLon, @NonNull List<TransportStop> transportStops) {
		for (TransportStop transportStop : transportStops) {
			transportStop.distance = (int) MapUtils.getDistance(latLon, transportStop.getLocation());
		}
		Collections.sort(transportStops, new Comparator<TransportStop>() {

			@Override
			public int compare(TransportStop s1, TransportStop s2) {
				return Algorithms.compare(s1.distance, s2.distance);
			}
		});
	}

	@Nullable
	private static List<TransportStop> findTransportStopsAt(OsmandApplication app, double latitude, double longitude, int radiusMeters) {
		QuadRect ll = MapUtils.calculateLatLonBbox(latitude, longitude, radiusMeters);
		try {
			return app.getResourceManager().searchTransportSync(ll.top, ll.left, ll.bottom, ll.right, null);
		} catch (IOException e) {
			return null;
		}
	}

	@Nullable
	public static TransportStop findBestTransportStopForAmenity(OsmandApplication app, Amenity amenity) {
		TransportStopAggregated stopAggregated;
		boolean isSubwayEntrance = "subway_entrance".equals(amenity.getSubType())
				|| "public_transport_station".equals(amenity.getSubType());

		LatLon loc = amenity.getLocation();
		int radiusMeters = isSubwayEntrance ? SHOW_SUBWAY_STOPS_FROM_ENTRANCES_RADIUS_METERS : SHOW_STOPS_RADIUS_METERS;
		List<TransportStop> transportStops = findTransportStopsAt(app, loc.getLatitude(), loc.getLongitude(), radiusMeters);
		if (transportStops == null) {
			return null;
		}
		sortTransportStops(loc, transportStops);

		if (isSubwayEntrance) {
			stopAggregated = processTransportStopsForAmenity(transportStops, amenity);
		} else {
			stopAggregated = new TransportStopAggregated();
			stopAggregated.setAmenity(amenity);
			TransportStop nearestStop = null;
			String amenityName = amenity.getName().toLowerCase();
			for (TransportStop stop : transportStops) {
				stop.setTransportStopAggregated(stopAggregated);
				String stopName = stop.getName().toLowerCase();
				if (((stopName.contains(amenityName) || amenityName.contains(stopName))
						&& MapUtils.getDistance(stop.getLocation(), loc) < MAX_DISTANCE_BETWEEN_AMENITY_AND_LOCAL_STOPS
						&& (nearestStop == null
						|| nearestStop.getLocation().equals(stop.getLocation())))
						|| stop.getLocation().equals(loc)) {
					stopAggregated.addLocalTransportStop(stop);
					if (nearestStop == null) {
						nearestStop = stop;
					}
				} else {
					stopAggregated.addNearbyTransportStop(stop);
				}
			}
		}

		List<TransportStop> localStops = stopAggregated.getLocalTransportStops();
		List<TransportStop> nearbyStops = stopAggregated.getNearbyTransportStops();
		if (!localStops.isEmpty()) {
			return localStops.get(0);
		} else if (!nearbyStops.isEmpty()) {
			return nearbyStops.get(0);
		}
		return null;
	}

	private static void processTransportStopAggregated(OsmandApplication app, TransportStop transportStop) {
		TransportStopAggregated stopAggregated = new TransportStopAggregated();
		transportStop.setTransportStopAggregated(stopAggregated);
		TransportStop localStop = null;
		LatLon loc = transportStop.getLocation();
		List<TransportStop> transportStops = findTransportStopsAt(app, loc.getLatitude(), loc.getLongitude(), SHOW_STOPS_RADIUS_METERS);
		if (transportStops != null) {
			for (TransportStop stop : transportStops) {
				if (localStop == null && transportStop.equals(stop)) {
					localStop = stop;
				} else {
					stopAggregated.addNearbyTransportStop(stop);
				}
			}
		}
		stopAggregated.addLocalTransportStop(localStop == null ? transportStop : localStop);
	}

	private static TransportStopAggregated processTransportStopsForAmenity(List<TransportStop> transportStops, Amenity amenity) {
		TransportStopAggregated stopAggregated = new TransportStopAggregated();
		stopAggregated.setAmenity(amenity);
		List<TransportStop> amenityStops = new ArrayList<>();
		if ("subway_entrance".equals(amenity.getSubType())) {
			amenityStops = findSubwayStopsForAmenityExit(transportStops, amenity.getLocation());
		}
		LatLon amenityLocation = amenity.getLocation();
		for (TransportStop stop : transportStops) {
			stop.setTransportStopAggregated(stopAggregated);
			boolean stopAddedAsLocal = false;
			if ("public_transport_station".equals(amenity.getSubType()) && (stop.getName().equals(amenity.getName()) ||
					stop.getEnName(false).equals(amenity.getEnName(false)))) {
				stopAggregated.addLocalTransportStop(stop);
				stopAddedAsLocal = true;
			} else {
				for (TransportStopExit exit : stop.getExits()) {
					LatLon exitLocation = exit.getLocation();
					if (MapUtils.getDistance(exitLocation, amenityLocation) < ROUNDING_ERROR
							|| hasCommonExit(exitLocation, amenityStops)) {
						stopAddedAsLocal = true;
						stopAggregated.addLocalTransportStop(stop);
						break;
					}
				}
			}
			if (!stopAddedAsLocal && MapUtils.getDistance(stop.getLocation(), amenityLocation)
					<= SHOW_SUBWAY_STOPS_FROM_ENTRANCES_RADIUS_METERS) {
				stopAggregated.addNearbyTransportStop(stop);
			}
		}
		sortTransportStopsExits(amenityLocation, stopAggregated.getLocalTransportStops());
		sortTransportStopsExits(amenityLocation, stopAggregated.getNearbyTransportStops());
		return stopAggregated;
	}

	private static boolean hasCommonExit(@NonNull LatLon exitLocation, @NonNull List<TransportStop> amenityStops) {
		for (TransportStop amenityStop : amenityStops) {
			for (TransportStopExit amenityExit : amenityStop.getExits()) {
				if (MapUtils.getDistance(amenityExit.getLocation(), exitLocation) < ROUNDING_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	@NonNull
	private static List<TransportStop> findSubwayStopsForAmenityExit(@NonNull List<TransportStop> transportStops,
	                                                                 @NonNull LatLon amenityExitLocation) {
		List<TransportStop> foundStops = new ArrayList<>();
		for (TransportStop stop : transportStops) {
			for (TransportStopExit exit : stop.getExits()) {
				if (MapUtils.getDistance(exit.getLocation(), amenityExitLocation) < ROUNDING_ERROR) {
					foundStops.add(stop);
				}
			}
		}
		return foundStops;
	}

	private static boolean checkSameRoute(List<TransportStopRoute> stopRoutes, TransportRoute route) {
		for (TransportStopRoute stopRoute : stopRoutes) {
			if (stopRoute.route.compareRoute(route)) {
				return true;
			}
		}
		return false;
	}
}