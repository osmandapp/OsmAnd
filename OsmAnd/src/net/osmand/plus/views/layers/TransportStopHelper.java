package net.osmand.plus.views.layers;

import static net.osmand.plus.mapcontextmenu.controllers.TransportStopController.SHOW_STOPS_RADIUS_METERS_UI;
import static net.osmand.util.MapUtils.ROUNDING_ERROR;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.*;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.views.MapLayers;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TransportStopHelper {

	public static final int SHOW_STOPS_RADIUS_METERS = SHOW_STOPS_RADIUS_METERS_UI * 6 / 5;
	public static final int MAX_DISTANCE_BETWEEN_AMENITY_AND_LOCAL_STOPS = 20;
	public static final int SHOW_SUBWAY_STOPS_FROM_ENTRANCES_RADIUS_METERS = 400;

	private final OsmandApplication app;
	private final MapLayers mapLayers;

	public TransportStopHelper(@NonNull Context context) {
		app = (OsmandApplication) context.getApplicationContext();
		mapLayers = app.getOsmandMap().getMapLayers();
	}

	public static void sortTransportStopRoutes(@NonNull List<TransportStopRoute> routes) {
		routes.sort((o1, o2) -> {
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
		});
	}

	private static void sortTransportStopsExits(@NonNull LatLon latLon,	@NonNull List<TransportStop> transportStops) {
		for (TransportStop transportStop : transportStops) {
			for (TransportStopExit exit : transportStop.getExits()) {
				int distance = (int) MapUtils.getDistance(latLon, exit.getLocation());
				if (transportStop.distance > distance) {
					transportStop.distance = distance;
				}
			}
		}
		transportStops.sort((s1, s2) -> Algorithms.compare(s1.distance, s2.distance));
	}

	private static void sortTransportStops(@NonNull LatLon latLon, @NonNull List<TransportStop> transportStops) {
		for (TransportStop transportStop : transportStops) {
			transportStop.distance = (int) MapUtils.getDistance(latLon, transportStop.getLocation());
		}
		transportStops.sort((s1, s2) -> Algorithms.compare(s1.distance, s2.distance));
	}

	@Nullable
	private static List<TransportStop> findTransportStopsAt(@NonNull OsmandApplication app,
			double latitude, double longitude, int radiusMeters) {
		QuadRect ll = MapUtils.calculateLatLonBbox(latitude, longitude, radiusMeters);
		try {
			return app.getResourceManager().searchTransportSync(ll.top, ll.left, ll.bottom, ll.right, null);
		} catch (IOException e) {
			return null;
		}
	}

	@Nullable
	public static TransportStop findBestTransportStopForAmenity(@NonNull OsmandApplication app, @NonNull Amenity amenity) {
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

	public static void processTransportStopAggregated(@NonNull OsmandApplication app, @NonNull TransportStop transportStop) {
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

	@NonNull
	private static TransportStopAggregated processTransportStopsForAmenity(
			@NonNull List<TransportStop> transportStops, @NonNull Amenity amenity) {
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
	private static List<TransportStop> findSubwayStopsForAmenityExit(
			@NonNull List<TransportStop> transportStops, @NonNull LatLon amenityExitLocation) {
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

	public static boolean checkSameRoute(@NonNull List<TransportStopRoute> stopRoutes, @NonNull TransportRoute route) {
		for (TransportStopRoute stopRoute : stopRoutes) {
			if (stopRoute.route.compareRoute(route)) {
				return true;
			}
		}
		return false;
	}
}