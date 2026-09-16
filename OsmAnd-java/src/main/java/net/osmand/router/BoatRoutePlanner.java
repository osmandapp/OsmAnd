package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.osmand.data.LatLon;
import net.osmand.router.SeaRoutePlanner.SeaRoute;
import net.osmand.router.SeaRoutePlanner.SeaRoutingConfig;
import net.osmand.util.MapUtils;

/**
 * Boat routes that mix the water network with open water (OsmAnd-Issues #3170).
 *
 * The water network is routed as usual, by {@link BinaryRoutePlanner} with the boat profile of routing.xml, so
 * fairways, canals, bridges, locks and the boat's parameters all apply there. This class only decides what happens
 * around that route: where it stops short of a requested point the gap is crossed over open water, and when the
 * network gives nothing - or open water alone is cheaper - the route goes over open water.
 *
 * Open water is weighed with the priority routing.xml gives water that no rule names, so a mapped fairway wins
 * unless the direct way is clearly shorter: the Wadden Sea is crossed by its channels, not over the flats.
 */
public class BoatRoutePlanner {

	/** A network route that ends this close to a requested point needs no open water to reach it. */
	public static final double ENDPOINT_TOLERANCE_METERS = 1000;
	/** routing.xml boat profile: the priority of water no rule names (fairway 2, river and canal 1.3). */
	public static final double OPEN_WATER_PRIORITY = 0.7;
	/** Longer open water legs plan on generalized shores and keep farther from them. */
	public static final double OFFSHORE_METERS = 80000;

	/** Shores of a corridor. Offshore corridors may use generalized world data, near shore ones must not. */
	public interface ShoreProvider {
		SeaObstacles load(double minLat, double minLon, double maxLat, double maxLon, boolean offshore) throws IOException;
	}

	public static class BoatRoute {
		/** Open water from the requested start to the network route, when the network starts farther away. */
		public List<LatLon> startConnector;
		public List<RouteSegmentResult> network;
		/** Open water from the network route to the requested end. */
		public List<LatLon> endConnector;
		/** The whole route over open water, when that was chosen. */
		public List<LatLon> openWater;
		public final Decision decision = new Decision();

		public boolean isNetwork() {
			return network != null;
		}

		public boolean isOpenWater() {
			return openWater != null;
		}

		public double getConnectorsDistance() {
			return length(startConnector) + length(endConnector);
		}
	}

	/** What was compared and what was chosen: for the log, for tests and for the server response. */
	public static class Decision {
		public int networkSegments;
		public double networkToStart = -1;
		public double networkToEnd = -1;
		public boolean startConnectorFailed;
		public boolean endConnectorFailed;
		public double networkCost = -1;
		public double openWaterCost = -1;
		public String choice = "none";
		public long timeMs;

		@Override
		public String toString() {
			return String.format(Locale.US,
					"%s: network %d segments ending %.0f m / %.0f m from the points%s%s, cost %.0f network vs %.0f open water, %d ms",
					choice, networkSegments, networkToStart, networkToEnd,
					startConnectorFailed ? ", no open water to the start" : "",
					endConnectorFailed ? ", no open water to the end" : "", networkCost, openWaterCost, timeMs);
		}
	}

	private final ShoreProvider shores;

	public BoatRoutePlanner(ShoreProvider shores) {
		this.shores = shores;
	}

	/**
	 * @param points  start, intermediate points and end as requested
	 * @param network the {@link BinaryRoutePlanner} route through them, null or empty when it found none
	 */
	public BoatRoute plan(List<LatLon> points, List<RouteSegmentResult> network) throws IOException {
		long started = System.currentTimeMillis();
		BoatRoute route = new BoatRoute();
		Decision decision = route.decision;
		LatLon start = points.get(0), end = points.get(points.size() - 1);
		boolean joinable = network != null && !network.isEmpty();
		List<LatLon> startConnector = null, endConnector = null;
		if (joinable) {
			LatLon networkStart = network.get(0).getStartPoint();
			LatLon networkEnd = network.get(network.size() - 1).getEndPoint();
			decision.networkSegments = network.size();
			decision.networkToStart = MapUtils.getDistance(networkStart, start);
			decision.networkToEnd = MapUtils.getDistance(networkEnd, end);
			if (decision.networkToStart > ENDPOINT_TOLERANCE_METERS) {
				startConnector = openWater(start, networkStart);
				decision.startConnectorFailed = startConnector == null;
				joinable = startConnector != null;
			}
			if (joinable && decision.networkToEnd > ENDPOINT_TOLERANCE_METERS) {
				endConnector = openWater(networkEnd, end);
				decision.endConnectorFailed = endConnector == null;
				joinable = endConnector != null;
			}
			if (joinable && startConnector == null && endConnector == null) {
				route.network = network;
				decision.networkCost = routeDistance(network);
				decision.choice = "network";
				decision.timeMs = System.currentTimeMillis() - started;
				return route;
			}
		}
		// the router found nothing, stopped short, or put the request onto a far away waterway:
		// open water alone may be the better route
		List<LatLon> openWater = openWater(points);
		decision.openWaterCost = openWater == null ? -1 : length(openWater) / OPEN_WATER_PRIORITY;
		decision.networkCost = joinable
				? routeDistance(network) + (length(startConnector) + length(endConnector)) / OPEN_WATER_PRIORITY : -1;
		if (joinable && (openWater == null || decision.networkCost <= decision.openWaterCost)) {
			route.network = network;
			route.startConnector = startConnector;
			route.endConnector = endConnector;
			decision.choice = "network+connectors";
		} else if (openWater != null) {
			route.openWater = openWater;
			decision.choice = "openWater";
		} else if (network != null && !network.isEmpty()) {
			// it cannot be joined, but a route that stops short still beats a line over land
			route.network = network;
			decision.choice = "network, not joined";
		}
		decision.timeMs = System.currentTimeMillis() - started;
		return route;
	}

	/** Open water through the points in order, null when any leg has no way over water. */
	public List<LatLon> openWater(List<LatLon> points) throws IOException {
		List<LatLon> line = new ArrayList<>();
		line.add(points.get(0));
		for (int i = 1; i < points.size(); i++) {
			List<LatLon> leg = openWater(points.get(i - 1), points.get(i));
			if (leg == null) {
				return null;
			}
			line.addAll(leg.subList(1, leg.size()));
		}
		return line;
	}

	/** Open water from a to b, starting exactly at a and ending exactly at b, or null. */
	public List<LatLon> openWater(LatLon a, LatLon b) throws IOException {
		double direct = MapUtils.getDistance(a, b);
		boolean offshore = direct > OFFSHORE_METERS;
		SeaRoutingConfig config = new SeaRoutingConfig();
		config.cornerClearance = offshore ? 300 : 80;
		config.minClearance = offshore ? 150 : 40;
		double margin = Math.max(0.05, 0.3 * direct / 111000);
		double lonMargin = margin / Math.cos(Math.toRadians((a.getLatitude() + b.getLatitude()) / 2));
		SeaObstacles obstacles = shores.load(
				Math.min(a.getLatitude(), b.getLatitude()) - margin, Math.min(a.getLongitude(), b.getLongitude()) - lonMargin,
				Math.max(a.getLatitude(), b.getLatitude()) + margin, Math.max(a.getLongitude(), b.getLongitude()) + lonMargin,
				offshore);
		SeaRoute sea = new SeaRoutePlanner(config).plan(obstacles, a, b);
		if (sea == null) {
			return null;
		}
		List<LatLon> line = new ArrayList<>();
		line.add(a);
		// the planner's first and last points are a and b themselves unless they had to be moved onto water
		line.addAll(sea.snappedStart == null ? sea.points.subList(1, sea.points.size()) : sea.points);
		if (sea.snappedEnd != null) {
			line.add(b);
		}
		return line;
	}

	public static double length(List<LatLon> line) {
		double distance = 0;
		if (line != null) {
			for (int i = 1; i < line.size(); i++) {
				distance += MapUtils.getDistance(line.get(i - 1), line.get(i));
			}
		}
		return distance;
	}

	public static double routeDistance(List<RouteSegmentResult> route) {
		double distance = 0;
		for (RouteSegmentResult r : route) {
			distance += r.getDistance();
		}
		return distance;
	}
}
