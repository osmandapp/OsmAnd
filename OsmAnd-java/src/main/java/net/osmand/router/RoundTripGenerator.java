package net.osmand.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

/**
 * Round trip prototype on top of HH routing (OsmAnd-Issues#2827).
 * <p>
 * A loop is a regular polygon of waypoints on a circle that passes through the start. The circle
 * centre lies in the loop direction, so the route leaves towards it and comes back from the other
 * side. Each candidate is routed start -> w1 -> ... -> wk -> start. The road detour is not known in
 * advance, so the radius is rescaled by the measured ratio of road length to the straight
 * polygon and the loop is routed again. Candidates in several directions are ranked by the length
 * error and by the share of roads driven twice, and returned variants must not share most roads.
 * <p>
 * Directions are searched in parallel: the HH search itself costs about 3 ms per leg, but the
 * last-mile search around every waypoint costs ~160 ms and is what the request waits for.
 */
public class RoundTripGenerator {

	public static final int MIN_SHAPE = 2;
	public static final int MAX_SHAPE = 5;
	public static final int DIRECTIONS = 8; // candidate loops, each one is routed up to MAX_ITERATIONS times
	public static final int MAX_ITERATIONS = 3;
	// Loops are aimed a little longer than asked: a loop a few percent over the length reads as "about right",
	// under it as "short". Measured with 8% tolerance around the asked length, trimmed loops came out +6-8%.
	public static final double TARGET_MARGIN = 1.04;
	public static final double LENGTH_TOLERANCE = 0.025; // stop rescaling a loop within this share of the target
	public static final double LENGTH_BAND = 0.03; // loops within this share of the target are preferred
	public static final double INITIAL_DETOUR = 1.3; // road length / straight polygon length before anything is measured
	public static final double MAX_DETOUR = 1.0; // the polygon is never longer than the requested length
	public static final double OVERLAP_WEIGHT = 1.5; // score = length error + weight * overlap
	public static final double MAX_SIMILARITY = 0.5; // variants sharing more of their roads are duplicates
	public static final double MAX_LENGTH_ERROR = 0.35; // such candidates are only returned when nothing better exists
	// A point on the circle is often reached by a detour: the route drives to it and comes back the same
	// way (on a dual carriageway even along another road). Such a point is moved to where the detour
	// starts and the loop is routed again. Measured before: 14.9% of loop length were such detours.
	public static final double SPUR_JOIN_M = 30; // the route comes back this close to where it left
	public static final double SPUR_MIN_REACH_M = 150; // after going at least this far from there
	public static final double SPUR_MAX_LENGTH_M = 6000; // longer detours are a part of the loop, not a spur
	// with a direction given the loops fan around it, both orientations of the direction itself come first
	private static final double[] DIRECTION_OFFSETS = { 0, 0, -30, 30, -60, 60, -90, 90 };

	public interface LoopRouter {
		/** @return route start -> via... -> start, null if it could not be built. Called from several threads. */
		List<RouteSegmentResult> route(LatLon start, List<LatLon> via) throws IOException, InterruptedException;
	}

	public static class Params {
		public double distance; // meters
		public int variants = 3;
		public Double direction; // degrees, null means any
		public int shape = 3; // waypoints between start and finish
		public int seed;
		public int parallelism = 1; // how many loops may be routed at once (one routing context each)
		public boolean allowAStar; // fall back to A* where the maps carry no HH data (pedestrian), much slower
	}

	public static class RoundTrip {
		public List<RouteSegmentResult> route;
		public List<LatLon> waypoints;
		public double heading;
		public boolean clockwise;
		public double radius;
		public double distance;
		public double time;
		public double overlap; // share of the length driven more than once
		public double lengthError; // |length - target| / target
		public int iteration;
		Map<Piece, Double> pieces; // road piece -> its length

		public double score() {
			return lengthError + OVERLAP_WEIGHT * overlap;
		}

		public Map<String, Object> describe() {
			Map<String, Object> m = new HashMap<>();
			m.put("heading", Math.round(heading));
			m.put("clockwise", clockwise);
			m.put("radius", Math.round(radius));
			m.put("overlap", round3(overlap));
			m.put("lengthError", round3(lengthError));
			m.put("iteration", iteration);
			m.put("distance", Math.round(distance));
			m.put("time", Math.round(time));
			List<double[]> wpts = new ArrayList<>();
			for (LatLon l : waypoints) {
				wpts.add(new double[] { l.getLatitude(), l.getLongitude() });
			}
			m.put("waypoints", wpts);
			return m;
		}
	}

	private record Piece(long roadId, int index) {
	}

	private final LoopRouter router;
	public final List<RoundTrip> candidates = Collections.synchronizedList(new ArrayList<>());
	// measured road length per meter of straight polygon, shared by the directions routed in parallel
	private final List<Double> ratios = Collections.synchronizedList(new ArrayList<>());
	public volatile int routings;
	public volatile long routingMs;

	public RoundTripGenerator(LoopRouter router) {
		this.router = router;
	}

	/** Farthest a waypoint can get from the start (the diameter of the largest circle), to select maps */
	public static double maxReach(Params p) {
		int k = shape(p);
		return 2 * (p.distance / MAX_DETOUR) / polygonFactor(k);
	}

	public List<RoundTrip> generate(LatLon start, Params p) throws IOException, InterruptedException {
		int threads = Math.max(1, Math.min(p.parallelism, DIRECTIONS));
		if (threads == 1) {
			for (int d = 0; d < DIRECTIONS; d++) {
				routeDirection(start, p, d);
			}
		} else {
			ExecutorService pool = Executors.newFixedThreadPool(threads);
			try {
				List<Future<RoundTrip>> futures = new ArrayList<>();
				for (int d = 0; d < DIRECTIONS; d++) {
					final int direction = d;
					futures.add(pool.submit((Callable<RoundTrip>) () -> routeDirection(start, p, direction)));
				}
				for (Future<RoundTrip> f : futures) {
					try {
						f.get();
					} catch (ExecutionException e) {
						Throwable cause = e.getCause();
						if (cause instanceof InterruptedException) {
							throw (InterruptedException) cause;
						}
						if (cause instanceof IOException) {
							throw (IOException) cause;
						}
						throw new IOException(cause);
					}
				}
			} finally {
				pool.shutdownNow();
			}
		}
		List<RoundTrip> selected = select(new ArrayList<>(candidates), p.variants);
		// A returned loop outside the length band gets one more attempt, rescaled by its own measured length:
		// with 8 directions there are rarely three loops in the band, and only the returned ones pay for it.
		double target = p.distance * TARGET_MARGIN;
		for (int i = 0; i < selected.size(); i++) {
			RoundTrip c = selected.get(i);
			if (c.lengthError <= LENGTH_BAND || c.distance <= 0) {
				continue;
			}
			RoundTrip retry = route(start, c.heading, c.clockwise, c.radius * target / c.distance, shape(p), target);
			if (retry != null && retry.lengthError < c.lengthError) {
				retry.iteration = c.iteration + 1;
				selected.set(i, retry);
			}
		}
		return selected;
	}

	/** Route one direction, rescaling the radius until the loop is long enough */
	private RoundTrip routeDirection(LatLon start, Params p, int d) throws IOException, InterruptedException {
		int k = shape(p);
		double target = p.distance * TARGET_MARGIN;
		double maxPerimeter = maxReach(p) / 2 * polygonFactor(k);
		double heading = heading(p, d);
		boolean clockwise = ((d + p.seed) & 1) == 0;
		double ratio = ratios.isEmpty() ? INITIAL_DETOUR : median(ratios);
		RoundTrip best = null;
		for (int it = 0; it < MAX_ITERATIONS; it++) {
			double perimeter = Math.min(target / ratio, maxPerimeter);
			RoundTrip rt = route(start, heading, clockwise, perimeter / polygonFactor(k), k, target);
			if (rt == null) {
				break;
			}
			rt.iteration = it;
			double measured = rt.distance / perimeter;
			ratios.add(measured);
			if (best == null || rt.lengthError < best.lengthError) {
				best = rt;
			}
			if (rt.lengthError <= LENGTH_TOLERANCE) {
				break;
			}
			ratio = measured;
		}
		if (best != null) {
			candidates.add(best);
		}
		return best;
	}

	private RoundTrip route(LatLon start, double heading, boolean clockwise, double radius, int k, double target)
			throws IOException, InterruptedException {
		LatLon center = MapUtils.rhumbDestinationPoint(start, radius, heading);
		List<LatLon> via = new ArrayList<>();
		for (int i = 1; i <= k; i++) {
			// the start is at heading + 180 seen from the centre
			double angle = heading + 180 + (clockwise ? 1 : -1) * i * 360.0 / (k + 1);
			via.add(MapUtils.rhumbDestinationPoint(center, radius, angle));
		}
		List<RouteSegmentResult> res = routeCounted(start, via);
		if (res == null || res.isEmpty()) {
			return null;
		}
		List<LatLon> trimmed = trimSpurs(res, via);
		if (trimmed != null) {
			List<RouteSegmentResult> retry = routeCounted(start, trimmed);
			if (retry != null && !retry.isEmpty()) {
				res = retry;
				via = trimmed;
			}
		}
		RoundTrip rt = new RoundTrip();
		rt.route = res;
		rt.waypoints = via;
		rt.heading = heading;
		rt.clockwise = clockwise;
		rt.radius = radius;
		rt.pieces = new HashMap<>();
		double repeated = 0;
		for (RouteSegmentResult s : res) {
			rt.time += s.getSegmentTime();
			RouteDataObject o = s.getObject();
			int st = s.getStartPointIndex();
			int en = s.getEndPointIndex();
			int dir = st <= en ? 1 : -1;
			for (int i = st; i != en; i += dir) {
				int j = i + dir;
				double len = MapUtils.measuredDist31(o.getPoint31XTile(i), o.getPoint31YTile(i),
						o.getPoint31XTile(j), o.getPoint31YTile(j));
				Piece piece = new Piece(o.getId(), Math.min(i, j));
				if (rt.pieces.put(piece, len) != null) {
					repeated += len;
				}
				rt.distance += len;
			}
		}
		rt.overlap = rt.distance > 0 ? repeated / rt.distance : 0;
		rt.lengthError = Math.abs(rt.distance - target) / target;
		return rt;
	}

	private List<RouteSegmentResult> routeCounted(LatLon start, List<LatLon> via)
			throws IOException, InterruptedException {
		long t = System.currentTimeMillis();
		try {
			return router.route(start, via);
		} finally {
			routingMs += System.currentTimeMillis() - t;
			routings++;
		}
	}

	/**
	 * Moves every waypoint that is reached by an out-and-back detour to the point where the detour
	 * leaves the loop. The detour is found on the geometry, not on road ids, so that driving there and
	 * back on the two carriageways of one street counts too.
	 *
	 * @return the new waypoints, null when no waypoint sits on a detour
	 */
	static List<LatLon> trimSpurs(List<RouteSegmentResult> route, List<LatLon> via) {
		List<LatLon> pts = new ArrayList<>();
		for (RouteSegmentResult s : route) {
			int st = s.getStartPointIndex();
			int en = s.getEndPointIndex();
			int dir = st <= en ? 1 : -1;
			for (int i = st; ; i += dir) {
				if (pts.isEmpty() || i != st) {
					pts.add(s.getPoint(i));
				}
				if (i == en) {
					break;
				}
			}
		}
		if (pts.size() < 3) {
			return null;
		}
		double[] cum = new double[pts.size()];
		for (int i = 1; i < pts.size(); i++) {
			cum[i] = cum[i - 1] + MapUtils.getDistance(pts.get(i - 1), pts.get(i));
		}
		List<LatLon> res = new ArrayList<>(via);
		boolean changed = false;
		for (int w = 0; w < via.size(); w++) {
			int tip = 0;
			double best = Double.MAX_VALUE;
			for (int i = 0; i < pts.size(); i++) {
				double d = MapUtils.getDistance(pts.get(i), via.get(w));
				if (d < best) {
					best = d;
					tip = i;
				}
			}
			// the widest pair around the tip: leaves at 'from', comes back next to it at 'to'
			int from = -1;
			for (int i = tip; i >= 0 && cum[tip] - cum[i] <= SPUR_MAX_LENGTH_M; i--) {
				if (MapUtils.getDistance(pts.get(i), pts.get(tip)) < SPUR_MIN_REACH_M) {
					continue;
				}
				for (int j = tip + 1; j < pts.size() && cum[j] - cum[i] <= SPUR_MAX_LENGTH_M; j++) {
					if (MapUtils.getDistance(pts.get(i), pts.get(j)) < SPUR_JOIN_M) {
						from = i; // keeps being overwritten while i goes back: the widest detour wins
						break;
					}
				}
			}
			if (from >= 0) {
				res.set(w, pts.get(from));
				changed = true;
			}
		}
		return changed ? res : null;
	}

	static List<RoundTrip> select(List<RoundTrip> candidates, int count) {
		// a loop within the length band beats any loop outside it, whatever their repeated roads: the score
		// alone let a +9% loop with fewer repeats win over a +3% one
		candidates.sort(Comparator.comparingInt((RoundTrip c) -> c.lengthError <= LENGTH_BAND ? 0 : 1)
				.thenComparingDouble(RoundTrip::score));
		List<RoundTrip> res = new ArrayList<>();
		for (RoundTrip c : candidates) {
			if (res.size() >= count) {
				break;
			}
			if (!res.isEmpty() && c.lengthError > MAX_LENGTH_ERROR) {
				continue;
			}
			boolean duplicate = false;
			for (RoundTrip r : res) {
				if (similarity(c, r) > MAX_SIMILARITY) {
					duplicate = true;
					break;
				}
			}
			if (!duplicate) {
				res.add(c);
			}
		}
		return res;
	}

	/** Length of the roads both loops use, as a share of the shorter loop */
	static double similarity(RoundTrip a, RoundTrip b) {
		Map<Piece, Double> small = a.pieces.size() <= b.pieces.size() ? a.pieces : b.pieces;
		Map<Piece, Double> large = small == a.pieces ? b.pieces : a.pieces;
		double shared = 0;
		for (Map.Entry<Piece, Double> e : small.entrySet()) {
			if (large.containsKey(e.getKey())) {
				shared += e.getValue();
			}
		}
		double len = Math.min(a.distance, b.distance);
		return len > 0 ? shared / len : 0;
	}

	private static double heading(Params p, int d) {
		if (p.direction != null) {
			return normalize(p.direction + DIRECTION_OFFSETS[d % DIRECTION_OFFSETS.length]);
		}
		// golden angle: every seed gives another set of directions
		return normalize(p.seed * 137.508 + d * 360.0 / DIRECTIONS);
	}

	private static int shape(Params p) {
		return Math.max(MIN_SHAPE, Math.min(MAX_SHAPE, p.shape));
	}

	/** Perimeter of the polygon through the start with k waypoints, in radii */
	private static double polygonFactor(int k) {
		return 2 * (k + 1) * Math.sin(Math.PI / (k + 1));
	}

	private static double median(List<Double> values) {
		List<Double> s;
		synchronized (values) {
			s = new ArrayList<>(values);
		}
		Collections.sort(s);
		return s.get(s.size() / 2);
	}

	private static double normalize(double deg) {
		double r = deg % 360;
		return r < 0 ? r + 360 : r;
	}

	private static double round3(double v) {
		return Math.round(v * 1000) / 1000.0;
	}
}
