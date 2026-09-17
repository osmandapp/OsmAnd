package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.router.SeaRoutePlanner.SeaRoute;
import net.osmand.router.SeaRoutePlanner.SeaRoutingConfig;
import net.osmand.util.MapUtils;

/**
 * Boat routes that mix the water network with open water (OsmAnd-Issues #3170).
 *
 * The water network is routed as usual, by {@link BinaryRoutePlanner} with the boat profile of routing.xml, so
 * fairways, canals, bridges, locks and the boat's parameters all apply there. Open water joins it: a point that is
 * not on the network is joined to the network points it can reach over open water, and the whole trip may be open
 * water too. Everything is priced the way routing.xml prices it - seconds, with the priority of the water - so a
 * fairway (priority 2) beats open water (the default 0.7) unless the direct way is clearly shorter.
 */
public class BoatRoutePlanner {

	/** A network route that ends this close to a requested point needs no open water to reach it. */
	public static final double ENDPOINT_TOLERANCE_METERS = 1000;
	/** routing.xml boat profile: the priority of water no rule names. */
	public static final double OPEN_WATER_PRIORITY = 0.7;
	/** routing.xml boat profile: the priority of a fairway, the highest a boat gets - a lower bound for the network. */
	public static final double FAIRWAY_PRIORITY = 2;
	/** Longer open water legs plan on generalized shores and keep farther from them. */
	public static final double OFFSHORE_METERS = 80000;
	/** How many ways into the water network are tried around a point that is not on it. */
	public static final int ENTRIES_PER_POINT = 4;
	private static final int ENTRY_CANDIDATES = 80;
	private static final double ENTRY_CELL_METERS = 1000;
	/** A point on a tidal flat may be reached across the flat from this far: nothing else reaches it. */
	private static final double BARRIER_FREE_METERS = 10000;
	private static final int NETWORK_ZOOM = 12;

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

		/** Metres over the network and open water together. */
		public double getDistance() {
			return (network == null ? 0 : routeDistance(network)) + getConnectorsDistance() + length(openWater);
		}

		/** Seconds: segment times over the network, the boat's speed over open water. */
		public double getTime() {
			double time = 0;
			if (network != null) {
				for (RouteSegmentResult r : network) {
					time += r.getSegmentTime();
				}
			}
			double openWaterDistance = getConnectorsDistance() + length(openWater);
			return decision.speed > 0 ? time + openWaterDistance / decision.speed : time;
		}
	}

	/** What was compared and what was chosen: for the log, for tests and for the server response. */
	public static class Decision {
		public int networkSegments;
		public double networkToStart = -1;
		public double networkToEnd = -1;
		public int startEntries;
		public int endEntries;
		public int networkSearches;
		public int networkFailures;
		public double speed;
		public double networkCost = -1;
		public double openWaterCost = -1;
		public String choice = "none";
		public long entriesMs;
		public long networkMs;
		public long openWaterMs;
		public long timeMs;

		@Override
		public String toString() {
			return String.format(Locale.US,
					"%s: network %d segments ending %.0f m / %.0f m from the points, entries %d / %d, %d network searches (%d failed), cost %.0f network vs %.0f open water, speed %.2f, %d ms (entries %d, network %d, open water %d)",
					choice, networkSegments, networkToStart, networkToEnd,
					startEntries, endEntries, networkSearches,
					networkFailures, networkCost, openWaterCost, speed, timeMs, entriesMs, networkMs, openWaterMs);
		}
	}

	/** A way into the water network from a requested point: the network point and the open water to it. */
	private static class Entry {
		LatLon point;
		/** From the requested point to the network for the start, from the network to the point for the end. */
		List<LatLon> line;
		/** routing.xml seconds of the open water part. */
		double cost;
		/** How far from the point the network route may start or end. */
		double reach = ENDPOINT_TOLERANCE_METERS;
	}

	private final ShoreProvider shores;

	public BoatRoutePlanner(ShoreProvider shores) {
		this.shores = shores;
	}

	/**
	 * The boat route through the points in order, one leg per pair of neighbouring points. Rivers, canals and ports
	 * are only ever reached as requested points, so every leg is planned on its own; a leg with no route has the
	 * choice "none".
	 */
	public List<BoatRoute> route(RoutePlannerFrontEnd fe, RoutingContext ctx, List<LatLon> points)
			throws IOException, InterruptedException {
		List<BoatRoute> legs = new ArrayList<>();
		for (int i = 1; i < points.size(); i++) {
			legs.add(route(fe, ctx, points.get(i - 1), points.get(i)));
		}
		return legs;
	}

	/**
	 * The boat route from start to end: over the water network where it can, joined to the points - or replaced -
	 * by open water where that is cheaper or the only way.
	 */
	public BoatRoute route(RoutePlannerFrontEnd fe, RoutingContext ctx, LatLon start, LatLon end)
			throws IOException, InterruptedException {
		long started = System.currentTimeMillis();
		BoatRoute route = new BoatRoute();
		Decision decision = route.decision;
		double speed = boatSpeed(ctx);
		decision.speed = speed;
		double direct = MapUtils.getDistance(start, end);
		long phase = System.currentTimeMillis();
		List<Entry> starts = entries(fe, ctx, start, end, true, direct, speed);
		List<Entry> ends = entries(fe, ctx, end, start, false, direct, speed);
		decision.startEntries = starts.size();
		decision.endEntries = ends.size();
		decision.entriesMs = System.currentTimeMillis() - phase;

		// candidates are the network between an entry near the start and one near the end, and open water all the
		// way (a pair of nulls); cheapest lower bound first, and a candidate that cannot beat the best route found
		// is not computed at all - a failed search is the expensive kind
		List<Entry[]> pairs = new ArrayList<>();
		for (Entry s : starts) {
			for (Entry e : ends) {
				pairs.add(new Entry[] { s, e });
			}
		}
		pairs.add(new Entry[] { null, null });
		double openWaterBound = direct / (speed * OPEN_WATER_PRIORITY);
		pairs.sort((a, b) -> Double.compare(a[0] == null ? openWaterBound : lowerBound(a, speed),
				b[0] == null ? openWaterBound : lowerBound(b, speed)));
		double bestCost = Double.MAX_VALUE;
		for (Entry[] pair : pairs) {
			double bound = pair[0] == null ? openWaterBound : lowerBound(pair, speed);
			if (bound >= bestCost) {
				break;
			}
			if (pair[0] == null) {
				// TODO when open water crosses a fairway, decide by the fairway's direction whether to follow it -
				// open water is now either the whole route or the connectors to the entries, never a fairway crossed
				// in the middle
				phase = System.currentTimeMillis();
				List<LatLon> openWater = openWater(start, end);
				decision.openWaterMs = System.currentTimeMillis() - phase;
				if (openWater != null && length(openWater) / (speed * OPEN_WATER_PRIORITY) < bestCost) {
					bestCost = length(openWater) / (speed * OPEN_WATER_PRIORITY);
					decision.openWaterCost = bestCost;
					route.openWater = openWater;
					route.network = null;
					route.startConnector = null;
					route.endConnector = null;
					decision.choice = "openWater";
				}
				continue;
			}
			decision.networkSearches++;
			phase = System.currentTimeMillis();
			List<RouteSegmentResult> network = networkRoute(fe, ctx, pair[0], pair[1]);
			decision.networkMs += System.currentTimeMillis() - phase;
			if (network == null) {
				decision.networkFailures++;
				continue;
			}
			double cost = pair[0].cost + routingTime(network) + pair[1].cost;
			if (cost < bestCost) {
				bestCost = cost;
				route.openWater = null;
				route.network = network;
				route.startConnector = pair[0].line;
				route.endConnector = pair[1].line;
				decision.networkCost = cost;
				decision.networkSegments = network.size();
				decision.networkToStart = distance(network.get(0), start);
				decision.networkToEnd = distance(network.get(network.size() - 1), end);
				decision.choice = pair[0].line == null && pair[1].line == null ? "network" : "network+connectors";
			}
		}
		decision.timeMs = System.currentTimeMillis() - started;
		return route;
	}

	private static double lowerBound(Entry[] pair, double speed) {
		return pair[0].cost + pair[1].cost
				+ MapUtils.getDistance(pair[0].point, pair[1].point) / (speed * FAIRWAY_PRIORITY);
	}

	/**
	 * Ways into the water network around a point. A point on the network is its own way in. Otherwise these are
	 * the network points on open water around it that open water reaches, cheapest first - so a fairway behind a dam
	 * (the IJsselmeer seen from the North Sea) is never one of them.
	 */
	private List<Entry> entries(RoutePlannerFrontEnd fe, RoutingContext ctx, LatLon point, LatLon other,
			boolean isStart, double direct, double speed) throws IOException {
		List<Entry> entries = new ArrayList<>();
		RouteSegmentPoint segment = fe.findRouteSegment(point.getLatitude(), point.getLongitude(), ctx, null);
		LatLon snapped = segment == null ? null
				: new LatLon(MapUtils.get31LatitudeY(segment.preciseY), MapUtils.get31LongitudeX(segment.preciseX));
		if (snapped != null && MapUtils.getDistance(snapped, point) <= ENDPOINT_TOLERANCE_METERS) {
			Entry entry = new Entry();
			entry.point = point;
			// the router may start on another way next to the one the point snapped to
			entry.reach = MapUtils.getDistance(snapped, point) + ENDPOINT_TOLERANCE_METERS;
			entries.add(entry);
			return entries;
		}
		double radius = Math.max(15000, Math.min(120000, direct * 0.6));
		boolean offshore = radius > 40000;
		double latMargin = radius / 111000;
		double lonMargin = latMargin / Math.cos(Math.toRadians(point.getLatitude()));
		SeaObstacles obstacles = shores.load(point.getLatitude() - latMargin, point.getLongitude() - lonMargin,
				point.getLatitude() + latMargin, point.getLongitude() + lonMargin, offshore);
		List<LatLon> candidates = networkPoints(ctx, obstacles, point, radius);
		if (snapped != null && MapUtils.getDistance(snapped, point) <= radius) {
			candidates.add(snapped);
		}
		SeaRoutePlanner planner = new SeaRoutePlanner(config(offshore));
		SeaRoute[] reached = planner.planToMany(obstacles, point, candidates, radius * 1.5);
		if (!anyReached(reached)) {
			obstacles.setBarriersEnabled(false);
			reached = planner.planToMany(obstacles, point, candidates, BARRIER_FREE_METERS);
			obstacles.setBarriersEnabled(true);
		}
		for (int i = 0; i < reached.length; i++) {
			if (reached[i] == null) {
				continue;
			}
			Entry entry = new Entry();
			entry.point = candidates.get(i);
			entry.line = new ArrayList<>(reached[i].points);
			if (!isStart) {
				Collections.reverse(entry.line);
			}
			entry.cost = reached[i].distance / (speed * OPEN_WATER_PRIORITY);
			entries.add(entry);
		}
		entries.sort((a, b) -> Double.compare(a.cost + MapUtils.getDistance(a.point, other) / (speed * FAIRWAY_PRIORITY),
				b.cost + MapUtils.getDistance(b.point, other) / (speed * FAIRWAY_PRIORITY)));
		return entries.size() > ENTRIES_PER_POINT ? new ArrayList<>(entries.subList(0, ENTRIES_PER_POINT)) : entries;
	}

	private static boolean anyReached(SeaRoute[] reached) {
		for (SeaRoute r : reached) {
			if (r != null) {
				return true;
			}
		}
		return false;
	}

	/** Points of the water network on open water within radius, one per square kilometre, nearest first. */
	private static List<LatLon> networkPoints(RoutingContext ctx, SeaObstacles obstacles, LatLon point, double radius) {
		int tile = 1 << (31 - NETWORK_ZOOM);
		double tileMeters = 40075016 * Math.cos(Math.toRadians(point.getLatitude())) / (1 << NETWORK_ZOOM);
		int tiles = (int) Math.ceil(radius / tileMeters);
		int x = MapUtils.get31TileNumberX(point.getLongitude()), y = MapUtils.get31TileNumberY(point.getLatitude());
		Map<Long, double[]> nearest = new HashMap<>();
		Set<Long> seen = new HashSet<>();
		List<RouteDataObject> objects = new ArrayList<>();
		// loadTileData loads the 3 x 3 tiles around the given one
		for (int i = -tiles; i <= tiles; i += 3) {
			for (int j = -tiles; j <= tiles; j += 3) {
				objects.clear();
				ctx.loadTileData(x + i * tile, y + j * tile, NETWORK_ZOOM, objects);
				for (RouteDataObject o : objects) {
					if (!seen.add(o.getId()) || !ctx.getRouter().acceptLine(o)) {
						continue;
					}
					for (int k = 0; k < o.getPointsLength(); k++) {
						double lat = MapUtils.get31LatitudeY(o.getPoint31YTile(k));
						double lon = MapUtils.get31LongitudeX(o.getPoint31XTile(k));
						double d = MapUtils.getDistance(point.getLatitude(), point.getLongitude(), lat, lon);
						if (d > radius) {
							continue;
						}
						long cell = (((long) Math.floor(obstacles.x(lon) / ENTRY_CELL_METERS)) << 32)
								^ (((long) Math.floor(obstacles.y(lat) / ENTRY_CELL_METERS)) & 0xffffffffL);
						double[] known = nearest.get(cell);
						if ((known != null && known[2] <= d) || obstacles.isLand(new LatLon(lat, lon))) {
							continue;
						}
						nearest.put(cell, new double[] { lat, lon, d });
					}
				}
			}
		}
		List<double[]> sorted = new ArrayList<>(nearest.values());
		sorted.sort((a, b) -> Double.compare(a[2], b[2]));
		List<LatLon> points = new ArrayList<>();
		for (int i = 0; i < sorted.size() && i < ENTRY_CANDIDATES; i++) {
			points.add(new LatLon(sorted.get(i)[0], sorted.get(i)[1]));
		}
		return points;
	}

	/** The network route between two network points, or null when the router has none or joins other waterways. */
	private static List<RouteSegmentResult> networkRoute(RoutePlannerFrontEnd fe, RoutingContext ctx, Entry from, Entry to)
			throws IOException, InterruptedException {
		LatLon a = from.point, b = to.point;
		List<RouteSegmentResult> result;
		try {
			RouteCalcResult calc = fe.searchRoute(ctx, a, b, null);
			result = calc == null ? null : calc.detailed;
		} catch (IllegalArgumentException e) {
			return null;
		}
		if (result == null || result.isEmpty()
				|| distance(result.get(0), a) > from.reach
				|| distance(result.get(result.size() - 1), b) > to.reach) {
			return null;
		}
		return result;
	}

	/**
	 * From a point to the part of the way a segment covers. Not to its first or last point: a route that starts in
	 * the middle of a long fairway begins at a way point kilometres from where it joins the fairway.
	 */
	private static double distance(RouteSegmentResult segment, LatLon p) {
		int step = segment.isForwardDirection() ? 1 : -1;
		double min = MapUtils.getDistance(segment.getPoint(segment.getStartPointIndex()), p);
		for (int i = segment.getStartPointIndex(); i != segment.getEndPointIndex(); i += step) {
			LatLon from = segment.getPoint(i), to = segment.getPoint(i + step);
			min = Math.min(min, MapUtils.getOrthogonalDistance(p.getLatitude(), p.getLongitude(), from.getLatitude(),
					from.getLongitude(), to.getLatitude(), to.getLongitude()));
		}
		return min;
	}

	private static double routingTime(List<RouteSegmentResult> route) {
		double time = 0;
		for (RouteSegmentResult r : route) {
			time += r.getRoutingTime();
		}
		return time;
	}

	/** Metres per second, as the router uses it for routing time. */
	private static double boatSpeed(RoutingContext ctx) {
		double speed = ctx.getRouter().getDefaultSpeed();
		return speed > 0 ? speed : 5 / 3.6;
	}

	private static SeaRoutingConfig config(boolean offshore) {
		SeaRoutingConfig config = new SeaRoutingConfig();
		config.cornerClearance = offshore ? 300 : 80;
		config.minClearance = offshore ? 150 : 40;
		return config;
	}

	/** Open water from a to b, starting exactly at a and ending exactly at b, or null. */
	public List<LatLon> openWater(LatLon a, LatLon b) throws IOException {
		double direct = MapUtils.getDistance(a, b);
		boolean offshore = direct > OFFSHORE_METERS;
		double margin = Math.max(0.05, 0.3 * direct / 111000);
		double lonMargin = margin / Math.cos(Math.toRadians((a.getLatitude() + b.getLatitude()) / 2));
		SeaObstacles obstacles = shores.load(
				Math.min(a.getLatitude(), b.getLatitude()) - margin, Math.min(a.getLongitude(), b.getLongitude()) - lonMargin,
				Math.max(a.getLatitude(), b.getLatitude()) + margin, Math.max(a.getLongitude(), b.getLongitude()) + lonMargin,
				offshore);
		SeaRoute sea = new SeaRoutePlanner(config(offshore)).plan(obstacles, a, b);
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
