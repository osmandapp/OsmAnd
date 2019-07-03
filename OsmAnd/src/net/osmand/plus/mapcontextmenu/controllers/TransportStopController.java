package net.osmand.plus.mapcontextmenu.controllers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
import net.osmand.plus.resources.TransportIndexRepository;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gnu.trove.list.array.TLongArrayList;

public class TransportStopController extends MenuController {

	public static final int SHOW_STOPS_RADIUS_METERS = 150;
	public static final int SHOW_SUBWAY_STOPS_FROM_ENTRANCES_RADIUS_METERS = 400;

	private TransportStop transportStop;
	private List<TransportStopRoute> routesNearby = new ArrayList<>();
	private List<TransportStopRoute> routesOnTheSameExit = new ArrayList<>();
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
		return transportStop.getName(getPreferredMapLang(), isTransliterateNames());
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
			List<TransportIndexRepository> reps = mapActivity.getMyApplication()
					.getResourceManager().searchTransportRepositories(transportStop.getLocation().getLatitude(),
							transportStop.getLocation().getLongitude());

			boolean useEnglishNames = mapActivity.getMyApplication().getSettings().usingEnglishNames();

			for (TransportIndexRepository t : reps) {
				if (t.acceptTransportStop(transportStop)) {
					ArrayList<TransportStop> transportStopsSameExit = new ArrayList<TransportStop>(transportStop.getLocalTransportStops());
					ArrayList<TransportStop> nearbyTransportStops = new ArrayList<TransportStop>(transportStop.getNearbyTransportStops());

					addTransportStopRoutes(transportStopsSameExit, routesOnTheSameExit, useEnglishNames, t);
					addTransportStopRoutes(nearbyTransportStops, routesNearby, useEnglishNames, t);
				}
			}
			sortTransportStopRoutes(routesOnTheSameExit);
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

	private void addTransportStopRoutes(List<TransportStop> stops, List<TransportStopRoute> routes, boolean useEnglishNames, TransportIndexRepository t) {
		for (TransportStop tstop : stops) {
			if (!tstop.isDeleted()) {
				addRoutes(routes, useEnglishNames, t, tstop, transportStop, (int) MapUtils.getDistance(tstop.getLocation(), transportStop.getLocation()));
			}
		}
	}

	private void addRoutes(List<TransportStopRoute> routes, boolean useEnglishNames, TransportIndexRepository t, TransportStop s, TransportStop refStop, int dist) {
		Collection<TransportRoute> rts = t.getRouteForStop(s);
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

	public static void sortTransportStops(@NonNull LatLon latLon, List<TransportStop> transportStops) {
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

	@NonNull
	public static List<TransportStop> findTransportStopsAt(OsmandApplication app, double latitude, double longitude, int radiusMeters) {
		ArrayList<TransportStop> transportStops = new ArrayList<>();
		List<TransportIndexRepository> reps = app.getResourceManager().searchTransportRepositories(latitude, longitude);

		TLongArrayList addedTransportStops = new TLongArrayList();
		for (TransportIndexRepository t : reps) {
			ArrayList<TransportStop> stops = new ArrayList<>();
			QuadRect ll = MapUtils.calculateLatLonBbox(latitude, longitude, radiusMeters);
			t.searchTransportStops(ll.top, ll.left, ll.bottom, ll.right, -1, stops, null);
			for (TransportStop transportStop : stops) {
				if (!addedTransportStops.contains(transportStop.getId())) {
					addedTransportStops.add(transportStop.getId());
					if (!transportStop.isDeleted()) {
						transportStops.add(transportStop);
					}
				}
			}
		}
		return transportStops;
	}

	@Nullable
	public static TransportStop findBestTransportStopForAmenity(OsmandApplication app, Amenity amenity) {
		TransportStopAggregated stopAggregated;
		boolean isSubwayEntrance = amenity.getSubType().equals("subway_entrance");

		LatLon loc = amenity.getLocation();
		int radiusMeters = isSubwayEntrance ? SHOW_SUBWAY_STOPS_FROM_ENTRANCES_RADIUS_METERS : SHOW_STOPS_RADIUS_METERS;
		List<TransportStop> transportStops = findTransportStopsAt(app, loc.getLatitude(), loc.getLongitude(), radiusMeters);
		sortTransportStops(loc, transportStops);

		if (isSubwayEntrance) {
			stopAggregated = processTransportStopsForAmenity(transportStops, amenity);
		} else {
			stopAggregated = new TransportStopAggregated();
			stopAggregated.setAmenity(amenity);
			TransportStop nearestStop = null;
			for (TransportStop stop : transportStops) {
				stop.setTransportStopAggregated(stopAggregated);
				if ((stop.getName().startsWith(amenity.getName())
						&& (nearestStop == null
						|| nearestStop.getLocation().equals(stop.getLocation())
						|| nearestStop.compareStopExits(stop)))
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

	public static TransportStopAggregated processTransportStopsForAmenity(List<TransportStop> transportStops, Amenity amenity) {
		TransportStopAggregated stopAggregated = new TransportStopAggregated();
		stopAggregated.setAmenity(amenity);

		for (TransportStop stop : transportStops) {
			stop.setTransportStopAggregated(stopAggregated);
			List<TransportStopExit> stopExits = stop.getExits();
			boolean stopOnSameExitAdded = false;
			for (TransportStopExit exit : stopExits) {
				if (exit.getLocation().equals(amenity.getLocation())) {
					stopOnSameExitAdded = true;
					stopAggregated.addLocalTransportStop(stop);
					break;
				}
			}
			if (!stopOnSameExitAdded && MapUtils.getDistance(stop.getLocation(), amenity.getLocation()) <= SHOW_STOPS_RADIUS_METERS) {
				stopAggregated.addNearbyTransportStop(stop);
			}
		}

		return stopAggregated;
	}

	public static boolean checkSameRoute(List<TransportStopRoute> stopRoutes, TransportRoute route) {
		for (TransportStopRoute stopRoute : stopRoutes) {
			if (stopRoute.route.compareRoute(route)) {
				return true;
			}
		}
		return false;
	}
}