package net.osmand.router.sea;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingContext;
import net.osmand.router.sea.SeaRoutePlanner.SeaRoute;
import net.osmand.router.sea.SeaRoutePlanner.SeaRoutingConfig;
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
	/** A network route that starts or ends farther than this from its point is joined to it by a straight line. */
	public static final double JOIN_METERS = 20;
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
		/**
		 * Straight from a point near the network (within {@link #ENDPOINT_TOLERANCE_METERS}) to where the network route
		 * starts, and from where it ends to the end point: what the walk to the road is for a car. It may cross a pier
		 * or a quay - the point is in a harbour or on land - so it is not checked against the shores.
		 */
		public List<LatLon> startJoin, endJoin;
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
			return length(startJoin) + length(startConnector) + length(endConnector) + length(endJoin);
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
		if (route.network != null) {
			// a point near the network is its own entry: the router starts where the point projects onto a way,
			// which can be hundreds of metres away (the Kiel Fjord seen from the Kiel Canal)
			List<RouteSegmentResult> network = route.network;
			if (route.startConnector == null) {
				route.startJoin = join(start, network.get(0).getStartPoint());
			}
			if (route.endConnector == null) {
				route.endJoin = join(network.get(network.size() - 1).getEndPoint(), end);
			}
		}
		decision.timeMs = System.currentTimeMillis() - started;
		return route;
	}

	private static List<LatLon> join(LatLon a, LatLon b) {
		return MapUtils.getDistance(a, b) <= JOIN_METERS ? null : new ArrayList<>(Arrays.asList(a, b));
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
			result = calc == null ? null : calc.getList();
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
		return MapUtils.getDistance(a, b) > LONG_ROUTE_METERS ? openWaterAlongCoarseWay(a, b) : openWaterDirect(a, b);
	}

	/**
	 * Longer than this, open water is planned along a coarse way first: all corners of the shores of a box that big
	 * at once - 120 thousand from the Baltic to the Atlantic - take minutes.
	 */
	public static final double LONG_ROUTE_METERS = 100000;
	/** Legs of a long route: short enough for its detailed shores to load and plan in a moment. */
	public static final double LEG_METERS = 50000;

	private List<LatLon> openWaterDirect(LatLon a, LatLon b) throws IOException {
		SeaRoute sea = planLeg(a, b, false, false);
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

	/** Open water on the shores of the box of a and b; its points run from a, or the water a was moved to, to b's. */
	private SeaRoute planLeg(LatLon a, LatLon b, boolean aOnWater, boolean bOnWater) throws IOException {
		// a leg of a long route is near shore by definition, however long a strait made it: never the generalized
		// shores, which leave out the islands south of Singapore
		boolean offshore = !aOnWater && !bOnWater && MapUtils.getDistance(a, b) > OFFSHORE_METERS;
		return new SeaRoutePlanner(config(offshore)).plan(load(a, b, offshore), a, b, aOnWater, bOnWater);
	}

	/**
	 * A long route: a coarse way over the cells of the generalized coastline of the whole box first, then legs between
	 * points of open water on it about {@link #LEG_METERS} apart, each planned on the detailed shores of its own box like
	 * a short route - in open sea that is a straight line, but only the detailed shores know the islets the generalized
	 * coastline leaves out (east of Belitung). A leg that
	 * cannot be planned means the coarse way took a passage too narrow for the detailed shores (the Limfjord at
	 * Thyborøn): the shore cells around it are closed and the coarse way found again.
	 */
	private List<LatLon> openWaterAlongCoarseWay(LatLon a, LatLon b) throws IOException {
		// a wider box when there is no way in this one: round Europe from the Baltic to the Aegean reaches far
		// beyond the box of its ends
		for (double margin : BOX_MARGINS) {
			SeaObstacles coarse = load(a, b, true, margin);
			if (coarse.getSegmentsCount() == 0) {
				return new ArrayList<>(Arrays.asList(a, b)); // far out at sea, nothing in the way
			}
			boolean[] noWay = new boolean[1];
			List<LatLon> line = openWaterAlongCoarseWay(coarse, a, b, noWay);
			if (line != null || !noWay[0]) {
				return line;
			}
		}
		return null;
	}

	/** Margins of the box of a long route, in parts of its straight line, tried in turn while there is no way. */
	private static final double[] BOX_MARGINS = { 0.3, 1, 3 };

	private List<LatLon> openWaterAlongCoarseWay(SeaObstacles coarse, LatLon a, LatLon b, boolean[] noWay)
			throws IOException {
		double ax = coarse.x(a.getLongitude()), ay = coarse.y(a.getLatitude());
		double bx = coarse.x(b.getLongitude()), by = coarse.y(b.getLatitude());
		Set<Long> closed = new HashSet<>();
		while (true) {
			SeaObstacles.CoarseWay way = coarse.coarseWay(ax, ay, bx, by, closed);
			noWay[0] = way == null && closed.isEmpty();
			if (way == null) {
				return null;
			}
			List<Integer> stops = new ArrayList<>();
			stops.add(0);
			double run = 0;
			for (int k = 1; k < way.size() - 1; k++) {
				run += Math.hypot(way.x.get(k) - way.x.get(k - 1), way.y.get(k) - way.y.get(k - 1));
				if (way.water.get(k) && run >= LEG_METERS) {
					stops.add(k);
					run = 0;
				}
			}
			stops.add(way.size() - 1);
			List<LatLon> line = new ArrayList<>();
			line.add(a);
			boolean planned = true;
			// each leg starts where the one before ended: a point of the coarse way that turned out to be on land
			// (Singapore at a finer zoom) is moved onto water by its leg and never drawn
			LatLon p = a;
			for (int i = 1; i < stops.size() && planned; i++) {
				boolean first = i == 1, last = i == stops.size() - 1;
				LatLon q = last ? b : coarse.latLon(way.x.get(stops.get(i)), way.y.get(stops.get(i)));
				double px = coarse.x(p.getLongitude()), py = coarse.y(p.getLatitude());
				double qx = coarse.x(q.getLongitude()), qy = coarse.y(q.getLatitude());
				SeaRoute leg = planLeg(p, q, !first, !last);
				if (leg != null) {
					line.addAll(first && leg.snappedStart != null ? leg.points : leg.points.subList(1, leg.points.size()));
					if (last && leg.snappedEnd != null) {
						line.add(b);
					}
					p = line.get(line.size() - 1);
					continue;
				}
				// the coarse cells cannot see a peninsula the generalized coastline left out (Beara in Ireland):
				// the ends of the leg in between are closed with the shores around it
				Set<Long> passage = coarse.shoreCellsBetween(px, py, qx, qy);
				if (!first) {
					passage.add(way.cells.get(stops.get(i - 1)));
				}
				if (!last) {
					passage.add(way.cells.get(stops.get(i)));
				}
				if (passage.isEmpty() || !closed.addAll(passage)) {
					return null;
				}
				planned = false;
			}
			if (planned) {
				return line;
			}
		}
	}

	/** The shores of the box of two points with a margin for a detour. */
	private SeaObstacles load(LatLon a, LatLon b, boolean offshore) throws IOException {
		return load(a, b, offshore, BOX_MARGINS[0]);
	}

	private SeaObstacles load(LatLon a, LatLon b, boolean offshore, double marginPart) throws IOException {
		double direct = MapUtils.getDistance(a, b);
		double margin = Math.min(80, Math.max(0.05, marginPart * direct / 111000));
		double lonMargin = margin / Math.cos(Math.toRadians((a.getLatitude() + b.getLatitude()) / 2));
		// within the map's coordinates: a box over the pole reads nothing
		return shores.load(
				Math.max(-85, Math.min(a.getLatitude(), b.getLatitude()) - margin),
				Math.max(-180, Math.min(a.getLongitude(), b.getLongitude()) - lonMargin),
				Math.min(85, Math.max(a.getLatitude(), b.getLatitude()) + margin),
				Math.min(180, Math.max(a.getLongitude(), b.getLongitude()) + lonMargin), offshore);
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
