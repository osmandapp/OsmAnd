package net.osmand.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.router.TransportRoutePlanner.TransportRouteSegment;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

/**
 * Public transport ferries: routes built by the map creator from route=ferry ways without a route relation
 * and ferry crossings of other routes. Stop flags are stored as route tags with indexes of the route stops.
 */
public class TransportFerryHelper {

	// stops generated at ferry way ends (not present in OSM): "0,5"
	public static final String SYNTHETIC_STOPS_TAG = "osmand:synthetic_ferry_stops";
	// synthetic stops in the water joining ferry ways: only a change to the next ferry way at the same stop
	public static final String JUNCTION_STOPS_TAG = "osmand:ferry_junction_stops";
	// non-ferry route goes over a ferry before these stops:
	// "stop index:ferry interval:ferry duration:ferry length" (seconds and meters, 0 - unknown)
	public static final String CROSSINGS_TAG = "osmand:ferry_crossings";

	public static void addStopTag(Map<String, String> tags, String tag, int stop, Object value) {
		tags.merge(tag, value == null ? String.valueOf(stop) : stop + ":" + value, (a, b) -> a + "," + b);
	}

	public static boolean isFerry(TransportRoute route) {
		return FerryRoutingHelper.FERRY.equals(route.getType());
	}

	public static boolean isSyntheticStop(TransportRoute route, int stop) {
		return getStopValue(route, SYNTHETIC_STOPS_TAG, stop) != null;
	}

	public static boolean isJunctionStop(TransportRoute route, int stop) {
		return getStopValue(route, JUNCTION_STOPS_TAG, stop) != null;
	}

	// synthetic stop from the stops tree is marked in the stop lists of its routes
	public static boolean isSyntheticStop(TransportStop stop) {
		if (stop.getRoutes() != null) {
			for (TransportRoute route : stop.getRoutes()) {
				List<TransportStop> stops = route.getForwardStops();
				for (int i = 0; i < stops.size(); i++) {
					if (stops.get(i).getId().longValue() == stop.getId().longValue()) {
						return isSyntheticStop(route, i);
					}
				}
			}
		}
		return false;
	}

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

	// Parallel ferry ways of different berths can be merged into one way going there and back,
	// so each hop takes the shortest part of a way between the nodes closest to its stops (in any direction)
	public static List<Way> getGeometry(TransportRoute route, int start, int end) {
		Way geometry = new Way(TransportRoutePlanner.GEOMETRY_WAY_ID);
		for (int i = start; i < end; i++) {
			for (Node n : getHopGeometry(route, i)) {
				geometry.addNode(n);
			}
		}
		return Collections.singletonList(geometry);
	}

	private static List<Node> getHopGeometry(TransportRoute route, int stop) {
		LatLon from = route.getForwardStops().get(stop).getLocation();
		LatLon to = route.getForwardStops().get(stop + 1).getLocation();
		List<Node> best = Arrays.asList(new Node(from.getLatitude(), from.getLongitude(), -1),
				new Node(to.getLatitude(), to.getLongitude(), -1));
		double bestCost = Double.MAX_VALUE;
		for (Way way : route.getForwardWays()) {
			if (way.getNodes().isEmpty()) {
				continue;
			}
			int fromInd = getClosestNode(way, from);
			int toInd = getClosestNode(way, to);
			List<Node> nodes = new ArrayList<>(way.getNodes().subList(Math.min(fromInd, toInd), Math.max(fromInd, toInd) + 1));
			if (fromInd > toInd) {
				Collections.reverse(nodes);
			}
			double length = 0;
			for (int k = 1; k < nodes.size(); k++) {
				length += MapUtils.getDistance(nodes.get(k - 1).getLatLon(), nodes.get(k).getLatLon());
			}
			double fromDist = MapUtils.getDistance(from, nodes.get(0).getLatLon());
			double toDist = MapUtils.getDistance(to, nodes.get(nodes.size() - 1).getLatLon());
			double cost = fromDist + toDist + length; // the shortest part, not going there and back
			if (fromDist < TransportRoutePlanner.MIN_DIST_STOP_TO_GEOMETRY
					&& toDist < TransportRoutePlanner.MIN_DIST_STOP_TO_GEOMETRY && cost < bestCost) {
				bestCost = cost;
				best = nodes;
			}
		}
		return best;
	}

	private static int getClosestNode(Way way, LatLon location) {
		int closest = 0;
		for (int i = 1; i < way.getNodes().size(); i++) {
			if (MapUtils.getDistance(location, way.getNodes().get(i).getLatLon())
					< MapUtils.getDistance(location, way.getNodes().get(closest).getLatLon())) {
				closest = i;
			}
		}
		return closest;
	}

	// TODO #17773 temporary: show only routes with ferries
	static boolean usesFerry(TransportRouteSegment segment) {
		return isFerry(segment.road) || segment.parentRoute != null && usesFerry(segment.parentRoute);
	}

	// ferry ways joined by a junction stop in the water are one ferry ride
	public static void mergeJunctionSegments(List<TransportRouteResultSegment> segments) {
		for (int i = segments.size() - 1; i > 0; i--) {
			TransportRouteResultSegment s = segments.get(i - 1);
			if (isJunctionStop(s.route, s.end)) {
				segments.set(i - 1, merge(s, segments.remove(i)));
			}
		}
	}

	private static TransportRouteResultSegment merge(TransportRouteResultSegment s, TransportRouteResultSegment next) {
		List<TransportStop> stops = new ArrayList<>(s.getTravelStops().subList(0, s.end - s.start));
		stops.addAll(next.getTravelStops().subList(1, next.end - next.start + 1));
		List<Way> ways = new ArrayList<>();
		for (Way w : s.route.getForwardWays()) {
			ways.add(new Way(w, w.getId()));
		}
		for (Way w : next.route.getForwardWays()) {
			ways.add(new Way(w, w.getId()));
		}
		TransportRouteResultSegment res = new TransportRouteResultSegment();
		res.route = new TransportRoute(s.route, stops, ways);
		res.start = 0;
		res.end = stops.size() - 1;
		res.walkDist = s.walkDist;
		res.walkTime = s.walkTime;
		res.depTime = s.depTime;
		res.travelTime = s.travelTime + next.travelTime;
		res.travelDistApproximate = s.travelDistApproximate + next.travelDistApproximate;
		return res;
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
