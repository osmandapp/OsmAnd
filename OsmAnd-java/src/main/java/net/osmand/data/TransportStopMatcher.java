package net.osmand.data;

import static net.osmand.util.MapUtils.ROUNDING_ERROR;

import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class TransportStopMatcher {

	public static final int SHOW_STOPS_RADIUS_METERS_UI = 150;
	public static final int SHOW_STOPS_RADIUS_METERS = SHOW_STOPS_RADIUS_METERS_UI * 6 / 5;
	public static final int MAX_DISTANCE_BETWEEN_AMENITY_AND_LOCAL_STOPS = 20;
	public static final int SHOW_SUBWAY_STOPS_FROM_ENTRANCES_RADIUS_METERS = 400;

	private static final String TRANSPORTATION_CATEGORY = "transportation";
	private static final String PUBLIC_TRANSPORT_FILTER = "public_transport";
	private static final String SUBWAY_ENTRANCE_SUBTYPE = "subway_entrance";
	private static final String PUBLIC_TRANSPORT_STATION_SUBTYPE = "public_transport_station";

	public static boolean isPublicTransportStop(Amenity amenity) {
		PoiCategory category = amenity.getType();
		if (!TRANSPORTATION_CATEGORY.equals(category.getKeyName())) {
			return false;
		}
		PoiFilter filter = category.getPoiFilterByName(PUBLIC_TRANSPORT_FILTER);
		if (filter == null) {
			return false;
		}
		for (PoiType type : filter.getPoiTypes()) {
			if (type.getKeyName().equals(amenity.getSubType())) {
				return true;
			}
		}
		return false;
	}

	public static boolean isSubwayEntrance(Amenity amenity) {
		return SUBWAY_ENTRANCE_SUBTYPE.equals(amenity.getSubType())
				|| PUBLIC_TRANSPORT_STATION_SUBTYPE.equals(amenity.getSubType());
	}

	public static int getSearchRadius(Amenity amenity) {
		return isSubwayEntrance(amenity) ? SHOW_SUBWAY_STOPS_FROM_ENTRANCES_RADIUS_METERS : SHOW_STOPS_RADIUS_METERS;
	}

	public static TransportStop findBestStopForAmenity(List<TransportStop> transportStops, Amenity amenity) {
		TransportStopAggregated stopAggregated = aggregateStopsForAmenity(transportStops, amenity);
		List<TransportStop> localStops = stopAggregated.getLocalTransportStops();
		List<TransportStop> nearbyStops = stopAggregated.getNearbyTransportStops();
		if (!localStops.isEmpty()) {
			return localStops.get(0);
		} else if (!nearbyStops.isEmpty()) {
			return nearbyStops.get(0);
		}
		return null;
	}

	public static TransportStopAggregated aggregateStopsForAmenity(List<TransportStop> transportStops, Amenity amenity) {
		TransportStopAggregated stopAggregated;
		LatLon loc = amenity.getLocation();
		sortTransportStops(loc, transportStops);

		if (isSubwayEntrance(amenity)) {
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
		return stopAggregated;
	}

	private static TransportStopAggregated processTransportStopsForAmenity(
			List<TransportStop> transportStops, Amenity amenity) {
		TransportStopAggregated stopAggregated = new TransportStopAggregated();
		stopAggregated.setAmenity(amenity);
		List<TransportStop> amenityStops = new ArrayList<>();
		if (SUBWAY_ENTRANCE_SUBTYPE.equals(amenity.getSubType())) {
			amenityStops = findSubwayStopsForAmenityExit(transportStops, amenity.getLocation());
		}
		LatLon amenityLocation = amenity.getLocation();
		for (TransportStop stop : transportStops) {
			stop.setTransportStopAggregated(stopAggregated);
			boolean stopAddedAsLocal = false;
			if (PUBLIC_TRANSPORT_STATION_SUBTYPE.equals(amenity.getSubType()) && (stop.getName().equals(amenity.getName()) ||
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

	private static boolean hasCommonExit(LatLon exitLocation, List<TransportStop> amenityStops) {
		for (TransportStop amenityStop : amenityStops) {
			for (TransportStopExit amenityExit : amenityStop.getExits()) {
				if (MapUtils.getDistance(amenityExit.getLocation(), exitLocation) < ROUNDING_ERROR) {
					return true;
				}
			}
		}
		return false;
	}

	private static List<TransportStop> findSubwayStopsForAmenityExit(
			List<TransportStop> transportStops, LatLon amenityExitLocation) {
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

	private static void sortTransportStops(LatLon latLon, List<TransportStop> transportStops) {
		for (TransportStop transportStop : transportStops) {
			transportStop.distance = (int) MapUtils.getDistance(latLon, transportStop.getLocation());
		}
		transportStops.sort((s1, s2) -> Algorithms.compare(s1.distance, s2.distance));
	}

	private static void sortTransportStopsExits(LatLon latLon, List<TransportStop> transportStops) {
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
}
