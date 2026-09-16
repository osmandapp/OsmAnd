package net.osmand.router;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import net.osmand.data.LatLon;

/**
 * Routing across open water, where there is nothing to route on: the boat profile has no ways at sea,
 * so a route is built against the shores of {@link SeaObstacles} instead.
 *
 * A shortest path around polygonal obstacles only ever bends at their corners, so the graph is the
 * corners of the shore that are convex towards the water, pushed {@code cornerClearance} metres off
 * shore, plus the two endpoints. Edges are not stored: a pair is first rejected by a cheap tangent
 * test and only then verified against the shore geometry, which is what keeps this affordable.
 *
 * Plan on a generalized zoom and verify on a detailed one ({@link #crossesShore},
 * {@link #measureClearance}): both give the same route, and the coarse graph is an order of magnitude
 * faster to search.
 */
public class SeaRoutePlanner {

	public static class SeaRoutingConfig {
		/** How far off shore a graph node sits. */
		public double cornerClearance = 80;
		/** How close to the shore a leg of the route may pass. */
		public double minClearance = 40;
		/** Shore detail below this is not worth a graph node. */
		public double simplifyTolerance = 40;
		/** How far to look for water when an endpoint is on land. */
		public double snapRadius = 3000;
		/** How many pairs of water points around the endpoints are tried before giving up. */
		public int maxEndpointAttempts = 16;
	}

	public static class SeaRoute {
		public final List<LatLon> points = new ArrayList<>();
		public double distance;
		public int corners;
		public int expansions;
		public int visibilityChecks;
		/** Pairs of endpoints tried, 1 when the nearest water of both ends was usable. */
		public int attempts;
		/** Set when an endpoint was on land and had to be moved onto water. */
		public LatLon snappedStart;
		public LatLon snappedEnd;

		public int getLegs() {
			return Math.max(0, points.size() - 1);
		}
	}

	private static final double MAX_MEASURED_CLEARANCE = 2000;

	private final SeaRoutingConfig config;

	public SeaRoutePlanner() {
		this(new SeaRoutingConfig());
	}

	public SeaRoutePlanner(SeaRoutingConfig config) {
		this.config = config;
	}

	public SeaRoutingConfig getConfig() {
		return config;
	}

	/** Returns null when the endpoints cannot be put on water or no path exists within the obstacles. */
	public SeaRoute plan(SeaObstacles obstacles, LatLon start, LatLon end) {
		List<LatLon> froms = obstacles.waterCandidates(start, config.minClearance, config.snapRadius);
		List<LatLon> tos = obstacles.waterCandidates(end, config.minClearance, config.snapRadius);
		if (froms.isEmpty() || tos.isEmpty()) {
			return null;
		}
		List<Corner> corners = corners(obstacles);
		int attempts = 0, expansions = 0, visibilityChecks = 0;
		double direct = distance(obstacles, start, end);
		SeaRoute best = null;
		double bestCost = Double.MAX_VALUE;
		// The nearest water is not always the right one: in Vlissingen it is a dock basin that reaches the Scheldt
		// only through 15 km of docks, while water 300 m farther is 1.5 km from the target. Pairs are tried nearest
		// first and the shortest route wins, counting the moves onto water; a route about as short as the straight
		// line cannot be beaten, so the search stops there.
		search:
		for (int sum = 0; sum <= froms.size() + tos.size() - 2; sum++) {
			for (int i = 0; i <= sum; i++) {
				int j = sum - i;
				if (i >= froms.size() || j >= tos.size()) {
					continue;
				}
				if (attempts == config.maxEndpointAttempts) {
					break search;
				}
				attempts++;
				SeaRoute route = search(obstacles, corners, froms.get(i), tos.get(j),
						distance(obstacles, froms.get(i), tos.get(j)) * MAX_DETOUR + DETOUR_ALLOWANCE);
				expansions += route.expansions;
				visibilityChecks += route.visibilityChecks;
				if (route.points.isEmpty()) {
					continue;
				}
				double cost = distance(obstacles, start, froms.get(i)) + route.distance + distance(obstacles, tos.get(j), end);
				if (cost < bestCost) {
					bestCost = cost;
					best = route;
					best.snappedStart = froms.get(i).equals(start) ? null : froms.get(i);
					best.snappedEnd = tos.get(j).equals(end) ? null : tos.get(j);
				}
				if (bestCost <= direct * NEAR_STRAIGHT) {
					break search;
				}
			}
		}
		if (best != null) {
			best.expansions = expansions;
			best.visibilityChecks = visibilityChecks;
			best.attempts = attempts;
		}
		return best;
	}

	/** A route this close to the straight line is not worth trying farther water for. */
	private static final double NEAR_STRAIGHT = 1.1;

	private static double distance(SeaObstacles obstacles, LatLon a, LatLon b) {
		return Math.hypot(obstacles.x(a.getLongitude()) - obstacles.x(b.getLongitude()),
				obstacles.y(a.getLatitude()) - obstacles.y(b.getLatitude()));
	}

	/** A* between two water points; the result has no points when they are not connected. */
	/**
	 * A route longer than this many times the straight line - plus the allowance - is no route a boat wants; and a
	 * search that fails, the expensive kind, stops there instead of taking the whole graph.
	 */
	private static final double MAX_DETOUR = 3;
	private static final double DETOUR_ALLOWANCE = 10000;

	private SeaRoute search(SeaObstacles obstacles, List<Corner> corners, LatLon from, LatLon to, double maxDistance) {
		SeaRoute route = new SeaRoute();
		route.corners = corners.size();
		int size = corners.size() + 2;
		double[] x = new double[size], y = new double[size];
		x[0] = obstacles.x(from.getLongitude());
		y[0] = obstacles.y(from.getLatitude());
		x[1] = obstacles.x(to.getLongitude());
		y[1] = obstacles.y(to.getLatitude());
		for (int i = 0; i < corners.size(); i++) {
			x[i + 2] = corners.get(i).x;
			y[i + 2] = corners.get(i).y;
		}

		double[] g = new double[size];
		int[] parent = new int[size];
		boolean[] settled = new boolean[size];
		Arrays.fill(g, Double.POSITIVE_INFINITY);
		Arrays.fill(parent, -1);
		g[0] = 0;
		PriorityQueue<double[]> queue = new PriorityQueue<>(new java.util.Comparator<double[]>() {
			@Override
			public int compare(double[] a, double[] b) {
				return Double.compare(a[0], b[0]);
			}
		});
		queue.add(new double[] { heuristic(x, y, 0), 0 });
		while (!queue.isEmpty()) {
			double[] top = queue.poll();
			if (top[0] > maxDistance) {
				break;
			}
			int u = (int) top[1];
			if (u == 1) {
				break;
			}
			if (settled[u]) {
				continue;
			}
			settled[u] = true;
			route.expansions++;
			for (int v = 1; v < size; v++) {
				if (v == u || settled[v]) {
					continue;
				}
				double distance = g[u] + Math.hypot(x[v] - x[u], y[v] - y[u]);
				if (distance >= g[v] || distance + heuristic(x, y, v) >= g[1]) {
					continue;
				}
				if (v >= 2 && !corners.get(v - 2).isTangentFrom(x[u], y[u])) {
					continue;
				}
				route.visibilityChecks++;
				// every leg keeps the same distance from the shore; the endpoints were moved out to it
				if (!obstacles.isClear(x[u], y[u], x[v], y[v], config.minClearance)) {
					continue;
				}
				g[v] = distance;
				parent[v] = u;
				queue.add(new double[] { distance + heuristic(x, y, v), v });
			}
		}
		if (Double.isInfinite(g[1])) {
			return route;
		}
		route.distance = g[1];
		for (int v = 1; v != -1; v = parent[v]) {
			route.points.add(0, obstacles.latLon(x[v], y[v]));
		}
		return route;
	}

	/**
	 * Open water from one point to many targets - points of the water network - in a single search: the shortest
	 * route to every target reached within maxDistance, null for the others. A leg into a target need not keep clear
	 * of the shore: a network point in a harbour sits by a quay.
	 */
	public SeaRoute[] planToMany(SeaObstacles obstacles, LatLon start, List<LatLon> targets, double maxDistance) {
		SeaRoute[] result = new SeaRoute[targets.size()];
		List<LatLon> froms = obstacles.waterCandidates(start, config.minClearance, config.snapRadius);
		if (froms.isEmpty() || targets.isEmpty()) {
			return result;
		}
		if (froms.size() > 3) {
			froms = froms.subList(0, 3);
		}
		List<Corner> corners = corners(obstacles);
		// nodes: sources [0, s), targets [s, s + t), corners after them
		int s = froms.size(), t = targets.size(), size = s + t + corners.size();
		double[] x = new double[size], y = new double[size];
		for (int i = 0; i < s; i++) {
			x[i] = obstacles.x(froms.get(i).getLongitude());
			y[i] = obstacles.y(froms.get(i).getLatitude());
		}
		for (int i = 0; i < t; i++) {
			x[s + i] = obstacles.x(targets.get(i).getLongitude());
			y[s + i] = obstacles.y(targets.get(i).getLatitude());
		}
		for (int i = 0; i < corners.size(); i++) {
			x[s + t + i] = corners.get(i).x;
			y[s + t + i] = corners.get(i).y;
		}
		double[] g = new double[size];
		int[] parent = new int[size];
		boolean[] settled = new boolean[size];
		Arrays.fill(g, Double.POSITIVE_INFINITY);
		Arrays.fill(parent, -1);
		PriorityQueue<double[]> queue = new PriorityQueue<>(new java.util.Comparator<double[]>() {
			@Override
			public int compare(double[] a, double[] b) {
				return Double.compare(a[0], b[0]);
			}
		});
		for (int i = 0; i < s; i++) {
			g[i] = distance(obstacles, start, froms.get(i));
			queue.add(new double[] { g[i], i });
		}
		int reached = 0;
		while (!queue.isEmpty()) {
			double[] top = queue.poll();
			int u = (int) top[1];
			if (settled[u]) {
				continue;
			}
			if (top[0] > maxDistance) {
				break;
			}
			settled[u] = true;
			if (u >= s && u < s + t) {
				if (++reached == t) {
					break;
				}
				continue; // a route goes to a target, never through it
			}
			for (int v = s; v < size; v++) {
				if (settled[v]) {
					continue;
				}
				double d = g[u] + Math.hypot(x[v] - x[u], y[v] - y[u]);
				if (d >= g[v] || d > maxDistance) {
					continue;
				}
				boolean target = v < s + t;
				if (!target && !corners.get(v - s - t).isTangentFrom(x[u], y[u])) {
					continue;
				}
				if (!obstacles.isClear(x[u], y[u], x[v], y[v], target ? 0 : config.minClearance)) {
					continue;
				}
				g[v] = d;
				parent[v] = u;
				queue.add(new double[] { d, v });
			}
		}
		for (int i = 0; i < t; i++) {
			if (!settled[s + i]) {
				continue;
			}
			SeaRoute route = new SeaRoute();
			route.distance = g[s + i];
			route.corners = corners.size();
			int v = s + i;
			for (; v >= s; v = parent[v]) {
				route.points.add(0, obstacles.latLon(x[v], y[v]));
			}
			route.points.add(0, froms.get(v));
			if (!froms.get(v).equals(start)) {
				route.snappedStart = froms.get(v);
				route.points.add(0, start);
			}
			result[i] = route;
		}
		return result;
	}

	/** True when any leg crosses a shore of the given obstacles - the check against detailed geometry. */
	public boolean crossesShore(SeaObstacles obstacles, SeaRoute route) {
		for (int i = 1; i < route.points.size(); i++) {
			LatLon a = route.points.get(i - 1), b = route.points.get(i);
			if (!obstacles.isClear(obstacles.x(a.getLongitude()), obstacles.y(a.getLatitude()),
					obstacles.x(b.getLongitude()), obstacles.y(b.getLatitude()), 0)) {
				return true;
			}
		}
		return false;
	}

	/** The closest the route comes to a shore, capped at 2 km. */
	public double measureClearance(SeaObstacles obstacles, SeaRoute route) {
		double min = MAX_MEASURED_CLEARANCE;
		for (int i = 1; i < route.points.size(); i++) {
			LatLon a = route.points.get(i - 1), b = route.points.get(i);
			min = Math.min(min, obstacles.clearance(obstacles.x(a.getLongitude()), obstacles.y(a.getLatitude()),
					obstacles.x(b.getLongitude()), obstacles.y(b.getLatitude()), MAX_MEASURED_CLEARANCE));
		}
		return min;
	}

	private static double heuristic(double[] x, double[] y, int v) {
		return Math.hypot(x[v] - x[1], y[v] - y[1]);
	}

	/** A corner of the shore that a route may bend around, with the shore edges that meet there. */
	private static class Corner {
		double x, y;          // the graph node, pushed off shore
		double vx, vy;        // the shore vertex itself
		double px, py, nx, ny; // the neighbouring shore vertices

		/**
		 * A corner is only useful seen from a point when the shore does not continue across the line of
		 * sight, i.e. when both neighbours lie on the same side of it. Rejecting the rest here avoids
		 * the geometry test for most pairs.
		 */
		boolean isTangentFrom(double fromX, double fromY) {
			double dx = vx - fromX, dy = vy - fromY;
			double toPrev = dx * (py - vy) - dy * (px - vx);
			double toNext = dx * (ny - vy) - dy * (nx - vx);
			return toPrev * toNext >= 0;
		}
	}

	private List<Corner> corners(SeaObstacles obstacles) {
		List<double[]> simplified = new ArrayList<>();
		for (double[] piece : obstacles.getPieces()) {
			simplified.add(simplify(piece, config.simplifyTolerance));
		}
		// pieces of one shore are stored separately, so a corner at their junction needs the neighbour piece
		Map<Long, double[]> startsAt = new HashMap<>(), endsAt = new HashMap<>();
		for (double[] piece : simplified) {
			startsAt.put(pointKey(piece, 0), piece);
			endsAt.put(pointKey(piece, piece.length - 2), piece);
		}
		List<Corner> corners = new ArrayList<>();
		for (int p = 0; p < simplified.size(); p++) {
			double[] piece = simplified.get(p);
			int landSide = obstacles.getPieceLandSide(p);
			boolean ring = pointKey(piece, 0) == pointKey(piece, piece.length - 2);
			for (int i = 0; i < piece.length - (ring ? 2 : 0); i += 2) {
				double[] prev = previousPoint(piece, i, ring, endsAt);
				double[] next = nextPoint(piece, i, startsAt);
				if (prev == null || next == null) {
					continue;
				}
				Corner corner = corner(obstacles, piece[i], piece[i + 1], prev, next, landSide);
				if (corner != null) {
					corners.add(corner);
				}
			}
		}
		return corners;
	}

	private Corner corner(SeaObstacles obstacles, double vx, double vy, double[] prev, double[] next, int landSide) {
		double inX = vx - prev[0], inY = vy - prev[1], outX = next[0] - vx, outY = next[1] - vy;
		double turn = inX * outY - inY * outX;
		if (landSide == 0) {
			// a barrier such as a tidal flat edge has no known land side: a route wraps any bend on its outer side
			landSide = turn > 0 ? 1 : -1;
		}
		if (turn * landSide <= 0) {
			return null; // concave towards the water: a route never bends here
		}
		double inLength = Math.hypot(inX, inY), outLength = Math.hypot(outX, outY);
		if (inLength == 0 || outLength == 0) {
			return null;
		}
		// normal pointing away from land: the water side is the right of travel when land is on the left
		double inNormalX = inY / inLength * landSide, inNormalY = -inX / inLength * landSide;
		double outNormalX = outY / outLength * landSide, outNormalY = -outX / outLength * landSide;
		double bisectorX = inNormalX + outNormalX, bisectorY = inNormalY + outNormalY;
		double bisectorLength = Math.hypot(bisectorX, bisectorY);
		if (bisectorLength < 1e-9) {
			return null;
		}
		bisectorX /= bisectorLength;
		bisectorY /= bisectorLength;
		// a sharp corner needs a longer offset to stay clearance metres away from both of its edges
		double offset = config.cornerClearance / Math.max(0.35, bisectorX * inNormalX + bisectorY * inNormalY);
		Corner corner = new Corner();
		corner.vx = vx;
		corner.vy = vy;
		corner.px = prev[0];
		corner.py = prev[1];
		corner.nx = next[0];
		corner.ny = next[1];
		corner.x = vx + bisectorX * offset;
		corner.y = vy + bisectorY * offset;
		if (!obstacles.isClear(vx + bisectorX * 0.5, vy + bisectorY * 0.5, corner.x, corner.y, 0)) {
			return null; // the offset landed on the far side of a narrow inlet
		}
		return corner;
	}

	private static double[] previousPoint(double[] piece, int i, boolean ring, Map<Long, double[]> endsAt) {
		if (i > 0) {
			return new double[] { piece[i - 2], piece[i - 1] };
		}
		if (ring) {
			return new double[] { piece[piece.length - 4], piece[piece.length - 3] };
		}
		double[] neighbour = endsAt.get(pointKey(piece, 0));
		return neighbour == null || neighbour == piece ? null
				: new double[] { neighbour[neighbour.length - 4], neighbour[neighbour.length - 3] };
	}

	private static double[] nextPoint(double[] piece, int i, Map<Long, double[]> startsAt) {
		if (i < piece.length - 2) {
			return new double[] { piece[i + 2], piece[i + 3] };
		}
		double[] neighbour = startsAt.get(pointKey(piece, i));
		return neighbour == null || neighbour == piece ? null : new double[] { neighbour[2], neighbour[3] };
	}

	private static long pointKey(double[] piece, int i) {
		return (((long) Math.round(piece[i] * 10)) << 32) ^ (Math.round(piece[i + 1] * 10) & 0xffffffffL);
	}

	/** Douglas-Peucker; the kept points are original ones, so an offset corner stays on real geometry. */
	static double[] simplify(double[] piece, double tolerance) {
		int count = piece.length / 2;
		if (count < 3 || tolerance <= 0) {
			return piece;
		}
		boolean[] keep = new boolean[count];
		keep[0] = keep[count - 1] = true;
		Deque<int[]> stack = new ArrayDeque<>();
		stack.push(new int[] { 0, count - 1 });
		while (!stack.isEmpty()) {
			int[] range = stack.pop();
			double maxDistance = -1;
			int farthest = -1;
			for (int i = range[0] + 1; i < range[1]; i++) {
				double d = SeaObstacles.pointToSegment(piece[i * 2], piece[i * 2 + 1], piece[range[0] * 2],
						piece[range[0] * 2 + 1], piece[range[1] * 2], piece[range[1] * 2 + 1]);
				if (d > maxDistance) {
					maxDistance = d;
					farthest = i;
				}
			}
			if (farthest > 0 && maxDistance > tolerance) {
				keep[farthest] = true;
				stack.push(new int[] { range[0], farthest });
				stack.push(new int[] { farthest, range[1] });
			}
		}
		int kept = 0;
		for (boolean k : keep) {
			if (k) {
				kept++;
			}
		}
		double[] result = new double[kept * 2];
		int at = 0;
		for (int i = 0; i < count; i++) {
			if (keep[i]) {
				result[at++] = piece[i * 2];
				result[at++] = piece[i * 2 + 1];
			}
		}
		return result;
	}
}
