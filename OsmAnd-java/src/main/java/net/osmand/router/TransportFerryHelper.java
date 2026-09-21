package net.osmand.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.util.Algorithms;

/**
 * Public transport ferries: routes built by the map creator from route=ferry ways without a route relation
 * and ferry crossings of other routes. Stop flags are stored as route tags with indexes of the route stops,
 * generated stops are marked by a name tag of their own so that the map can hide them without the routes.
 * The planner doesn't use it directly: TransportRoutingConfiguration applies these rules to ferry routes.
 */
public class TransportFerryHelper {

	// stops generated at ferry way ends (not present in OSM), "j" marks a junction of ferry ways
	// in the water (only a change to the next ferry way at the same stop): "0,3:j,5"
	public static final String FERRY_STOPS_TAG = "osmand_ferry_stops";
	public static final String JUNCTION_VALUE = "j";
	// non-ferry route goes over a ferry before these stops:
	// "stop index:ferry interval:ferry duration:ferry length" (seconds and meters, 0 - unknown)
	public static final String CROSSINGS_TAG = "osmand_ferry_crossings";
	// the same generated stop in the stops tree, which the map reads without its routes
	private static final String SYNTHETIC_STOP_TAG = "osmand_ferry_synthetic";

	public static void addStopTag(Map<String, String> tags, String tag, int stop, String value) {
		tags.merge(tag, value == null ? String.valueOf(stop) : stop + ":" + value, (a, b) -> a + "," + b);
	}

	public static void markSyntheticStop(TransportStop stop) {
		stop.setName(SYNTHETIC_STOP_TAG, "yes");
	}

	public static boolean isFerry(TransportRoute route) {
		return FerryRoutingHelper.FERRY.equals(route.getType());
	}

	// the route lists the indexes of its generated stops
	public static boolean isSyntheticStop(TransportRoute route, int stop) {
		return getStopValue(route, FERRY_STOPS_TAG, stop) != null;
	}

	// the same stop in the stops tree carries the flag itself: a stop of a route has no name tags
	public static boolean isSyntheticStop(TransportStop stop) {
		return stop.getNamesMap(false).containsKey(SYNTHETIC_STOP_TAG);
	}

	public static boolean isJunctionStop(TransportRoute route, int stop) {
		return JUNCTION_VALUE.equals(getStopValue(route, FERRY_STOPS_TAG, stop));
	}

	// stops of the route the map draws
	public static List<TransportStop> getVisibleStops(TransportRoute route) {
		List<TransportStop> stops = new ArrayList<>();
		for (int i = 0; i < route.getForwardStops().size(); i++) {
			if (!isSyntheticStop(route, i)) {
				stops.add(route.getForwardStops().get(i));
			}
		}
		return stops;
	}

	// ferry with a duration tag moves with the speed from it
	public static double getTravelSpeed(TransportRoute route, double defaultSpeed) {
		int duration = getDuration(route);
		return duration > 0 ? (double) route.getDistance() / duration : defaultSpeed;
	}

	// staying on board: the ferry stops unless its duration includes the stops
	// (a junction isn't a stop, the ferry doesn't stop again at another berth of the same terminal)
	public static int getStopTime(TransportRoutingConfiguration cfg, TransportRoute route, int stop) {
		return isFerry(route) && !isJunctionStop(route, stop) && getDuration(route) == 0
				&& !isSameTerminal(route, stop - 1, stop) && !isSameTerminal(route, stop, stop + 1) ? cfg.ferryTerminalTime : 0;
	}

	// ferry stops between these ones (inclusive) are berths of one terminal: stops with the same name
	public static boolean isSameTerminal(TransportRoute route, int from, int to) {
		List<TransportStop> stops = route.getForwardStops();
		if (!isFerry(route) || from < 0 || to >= stops.size() || Algorithms.isEmpty(stops.get(from).getName())) {
			return false;
		}
		for (int i = from + 1; i <= to; i++) {
			if (!stops.get(from).getName().equals(stops.get(i).getName())) {
				return false;
			}
		}
		return true;
	}

	// getting off a ferry at the stop (at a junction the ride continues on the next ferry way)
	public static double getAlightingTime(TransportRoutingConfiguration cfg, TransportRoute route, int stop) {
		return isFerry(route) && !isJunctionStop(route, stop) ? FerryRoutingHelper.getAlightingTime(cfg.ferryTerminalTime) : 0;
	}

	// ferry on the way to the stop, same as the ferry route itself: getting on, sailing (its duration
	// or its length with the ferry speed) and getting off
	public static double getCrossingTime(TransportRoutingConfiguration cfg, TransportRoute route, int stop) {
		int[] crossing = getCrossing(route, stop);
		if (crossing == null) {
			return 0;
		}
		float speed = cfg.getSpeedByRouteType(FerryRoutingHelper.FERRY);
		double sailingTime = crossing[1] > 0 ? crossing[1] : speed > 0 ? crossing[2] / speed : 0;
		return cfg.getBoardingTime(FerryRoutingHelper.FERRY, crossing[0]) + sailingTime
				+ FerryRoutingHelper.getAlightingTime(cfg.ferryTerminalTime);
	}

	private static int[] getCrossing(TransportRoute route, int stop) {
		String value = getStopValue(route, CROSSINGS_TAG, stop);
		if (value == null) {
			return null;
		}
		int[] crossing = new int[3]; // interval, duration, length
		String[] values = value.split(":");
		for (int i = 0; i < values.length && i < crossing.length; i++) {
			crossing[i] = Integer.parseInt(values[i]);
		}
		return crossing;
	}

	// route distance is a sum of distances between stops
	private static int getDuration(TransportRoute route) {
		return isFerry(route) ? FerryRoutingHelper.parseDuration(route.getTags().get(FerryRoutingHelper.DURATION_TAG),
				route.getDistance()) : 0;
	}

	private static String getStopValue(TransportRoute route, String tag, int stop) {
		String value = route.getTags().get(tag);
		if (value != null) {
			for (String v : value.split(",")) {
				int sep = v.indexOf(':');
				if (Integer.parseInt(sep < 0 ? v : v.substring(0, sep)) == stop) {
					return sep < 0 ? "" : v.substring(sep + 1);
				}
			}
		}
		return null;
	}
}
