package net.osmand.router;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.router.HHRouteDataStructure.HHRoutingConfig;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.MapUtils;

/**
 * The via-point invariant on random routes of one map: an alternative must be as good as the route
 * through one of its own points. If the route {@code start -> p -> end} through some point p of the
 * alternative is cheaper while it stays as distinct from the main route, the alternative is a bad
 * one - a driver would get the better route by asking for it with that single waypoint.
 *
 * This is a generator of candidates to check by eye rather than a pass/fail test: the map, the
 * number of routes and the seed come from the environment, and nothing runs without them.
 * <pre>
 *   OSMAND_ALT_INVARIANT_MAP=Ukraine_kyiv_europe_2.obf   (in -Dosmand.maps.dir, default ~/osmand/maps)
 *   OSMAND_ALT_INVARIANT_ROUTES=30                       (default 20)
 *   OSMAND_ALT_INVARIANT_SEED=7                          (default 1)
 *   OSMAND_ALT_INVARIANT_BBOX=50.3,30.3,50.6,30.8        (default: the map's bounds)
 *   OSMAND_ALT_INVARIANT_HOST=http://localhost:3000      (for the URLs in the report)
 *   OSMAND_ALT_INVARIANT_STRICT=1                        (fail on any violation; off by default)
 * </pre>
 * Every suspicious alternative is printed with a web-map URL that reproduces the better route.
 */
public class AlternativeRoutesViaInvariantTest {

	private static final String MAPS_DIR = System.getProperty("osmand.maps.dir",
			System.getProperty("user.home") + "/osmand/maps");

	/** via points are sampled along the alternative this far apart (meters) */
	private static final double VIA_STEP = 700;
	/** and not closer than this to either end, where any route is the same */
	private static final double VIA_MARGIN = 500;
	/** the route through a via point may share this much more of the main route than the alternative did */
	private static final double SHARE_TOLERANCE = 0.05;
	/** and is reported when it is at least this much cheaper */
	private static final double MIN_GAIN = 0.05;
	/** random routes are this long as the crow flies (meters) */
	private static final double MIN_LEG = 3000, MAX_LEG = 25000;

	private BinaryMapIndexReader[] readers;

	@Test
	public void viaInvariantOnRandomRoutes() throws Exception {
		String map = System.getenv("OSMAND_ALT_INVARIANT_MAP");
		Assume.assumeTrue("set OSMAND_ALT_INVARIANT_MAP=<obf> to run", map != null && !map.isEmpty());
		File f = new File(MAPS_DIR, map);
		Assume.assumeTrue("map " + f + " is not available", f.exists());
		int routes = Integer.parseInt(env("OSMAND_ALT_INVARIANT_ROUTES", "20"));
		Random rnd = new Random(Long.parseLong(env("OSMAND_ALT_INVARIANT_SEED", "1")));
		String host = env("OSMAND_ALT_INVARIANT_HOST", "http://localhost:3000");
		boolean strict = "1".equals(env("OSMAND_ALT_INVARIANT_STRICT", "0"));

		RandomAccessFile raf = new RandomAccessFile(f, "r");
		try {
			readers = new BinaryMapIndexReader[] {new BinaryMapIndexReader(raf, f)};
			double[] bbox = bbox(readers[0]);
			List<String> violations = new ArrayList<>();
			int done = 0, alternatives = 0;
			while (done < routes) {
				LatLon s = randomPoint(rnd, bbox), e = randomPoint(rnd, bbox);
				double leg = MapUtils.getDistance(s, e);
				if (leg < MIN_LEG || leg > MAX_LEG) {
					continue;
				}
				RouteCalcResult res;
				try {
					res = route(s, e, null, true);
				} catch (Exception | StackOverflowError ex) {
					continue;
				}
				if (res == null || res.getError() != null || res.getList().isEmpty()) {
					continue;
				}
				done++;
				List<RouteSegmentResult> main = res.getList();
				Map<Long, Double> mainRoads = roads(main);
				int k = 0;
				for (List<RouteSegmentResult> alt : res.getAlternatives()) {
					k++;
					alternatives++;
					String v = check(s, e, main, mainRoads, alt, k, host);
					if (v != null) {
						violations.add(v);
						System.out.println("VIA-INVARIANT " + v);
					}
				}
			}
			System.out.println(String.format("VIA-INVARIANT %s: %d routes, %d alternatives, %d suspicious",
					map, done, alternatives, violations.size()));
			if (strict) {
				Assert.assertTrue(violations.size() + " alternative(s) are beaten by a route through one of their own points",
						violations.isEmpty());
			}
		} finally {
			raf.close();
		}
	}

	/** the report line for a beaten alternative, or null when none of its points does better */
	private String check(LatLon s, LatLon e, List<RouteSegmentResult> main, Map<Long, Double> mainRoads,
			List<RouteSegmentResult> alt, int k, String host) {
		double mainCost = cost(main), altCost = cost(alt);
		double altShare = shared(roads(alt), mainRoads) / length(alt);
		List<LatLon> pts = points(alt);
		double total = length(alt), along = 0, next = VIA_MARGIN;
		double best = altCost, bestShare = altShare;
		LatLon bestVia = null;
		for (int i = 1; i < pts.size(); i++) {
			along += MapUtils.getDistance(pts.get(i - 1), pts.get(i));
			if (along < next || along > total - VIA_MARGIN) {
				continue;
			}
			next = along + VIA_STEP;
			List<RouteSegmentResult> v;
			try {
				RouteCalcResult r = route(s, e, pts.get(i), false);
				if (r == null || r.getError() != null) {
					continue;
				}
				v = r.getList();
			} catch (Exception | StackOverflowError ex) {
				continue;
			}
			double c = cost(v), sh = shared(roads(v), mainRoads) / length(v);
			if (sh <= altShare + SHARE_TOLERANCE && c < best) {
				best = c;
				bestShare = sh;
				bestVia = pts.get(i);
			}
		}
		if (bestVia == null || best > altCost * (1 - MIN_GAIN)) {
			return null;
		}
		return String.format("%s -> %s alt%d +%.1f%% shares %.0f%% | via %s: +%.1f%% shares %.0f%% (%.0f%% cheaper) %s/map/navigate/?start=%s&end=%s&via=%s&profile=car",
				ll(s), ll(e), k, 100 * (altCost / mainCost - 1), 100 * altShare, ll(bestVia),
				100 * (best / mainCost - 1), 100 * bestShare, 100 * (altCost - best) / altCost, host, ll(s), ll(e), ll(bestVia));
	}

	private RouteCalcResult route(LatLon s, LatLon e, LatLon via, boolean alternatives) throws Exception {
		RoutePlannerFrontEnd router = new RoutePlannerFrontEnd();
		RoutingMemoryLimits limits = new RoutingMemoryLimits(RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
				RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT);
		RoutingConfiguration config = RoutingConfiguration.getDefault().build("car", limits,
				new LinkedHashMap<String, String>());
		RoutingContext ctx = router.buildRoutingContext(config, null, readers, RouteCalculationMode.NORMAL);
		ctx.calculationProgress = new RouteCalculationProgress();
		HHRoutingConfig hh = HHRoutePlanner.prepareDefaultRoutingConfig(null);
		if (alternatives) {
			hh.calcAlternative();
		}
		router.setUseOnlyHHRouting(true).setHHRoutingConfig(hh);
		List<LatLon> inter = new ArrayList<>();
		if (via != null) {
			inter.add(via);
		}
		return router.searchRoute(ctx, s, e, inter);
	}

	private static String env(String name, String def) {
		String v = System.getenv(name);
		return v == null || v.isEmpty() ? def : v;
	}

	private static double[] bbox(BinaryMapIndexReader reader) {
		String env = System.getenv("OSMAND_ALT_INVARIANT_BBOX");
		if (env != null && !env.isEmpty()) {
			String[] t = env.split(",");
			return new double[] {Double.parseDouble(t[0]), Double.parseDouble(t[1]), Double.parseDouble(t[2]),
					Double.parseDouble(t[3])};
		}
		MapRoot root = reader.getMapIndexes().get(0).getRoots().get(0);
		return new double[] {MapUtils.get31LatitudeY(root.getBottom()), MapUtils.get31LongitudeX(root.getLeft()),
				MapUtils.get31LatitudeY(root.getTop()), MapUtils.get31LongitudeX(root.getRight())};
	}

	private static LatLon randomPoint(Random rnd, double[] bbox) {
		return new LatLon(bbox[0] + rnd.nextDouble() * (bbox[2] - bbox[0]), bbox[1] + rnd.nextDouble() * (bbox[3] - bbox[1]));
	}

	private static String ll(LatLon p) {
		return String.format("%.6f,%.6f", p.getLatitude(), p.getLongitude());
	}

	private static double cost(List<RouteSegmentResult> r) {
		double c = 0;
		for (RouteSegmentResult x : r) {
			c += x.getRoutingTime();
		}
		return c;
	}

	private static List<LatLon> points(List<RouteSegmentResult> r) {
		List<LatLon> p = new ArrayList<>();
		for (RouteSegmentResult x : r) {
			RouteDataObject o = x.getObject();
			int i = x.getStartPointIndex(), end = x.getEndPointIndex(), step = i <= end ? 1 : -1;
			while (true) {
				p.add(new LatLon(MapUtils.get31LatitudeY(o.getPoint31YTile(i)), MapUtils.get31LongitudeX(o.getPoint31XTile(i))));
				if (i == end) {
					break;
				}
				i += step;
			}
		}
		return p;
	}

	/** road piece -> length, the same measure the alternatives themselves are judged by */
	private static Map<Long, Double> roads(List<RouteSegmentResult> r) {
		Map<Long, Double> m = new HashMap<>();
		for (RouteSegmentResult x : r) {
			RouteDataObject o = x.getObject();
			int i = x.getStartPointIndex(), end = x.getEndPointIndex(), step = i <= end ? 1 : -1;
			while (i != end) {
				int j = i + step;
				long key = o.getId() * 4096L + Math.min(i, j);
				double len = MapUtils.getDistance(MapUtils.get31LatitudeY(o.getPoint31YTile(i)),
						MapUtils.get31LongitudeX(o.getPoint31XTile(i)), MapUtils.get31LatitudeY(o.getPoint31YTile(j)),
						MapUtils.get31LongitudeX(o.getPoint31XTile(j)));
				Double prev = m.get(key);
				m.put(key, (prev == null ? 0 : prev) + len);
				i = j;
			}
		}
		return m;
	}

	private static double length(List<RouteSegmentResult> r) {
		double d = 0;
		for (Double v : roads(r).values()) {
			d += v;
		}
		return d;
	}

	private static double shared(Map<Long, Double> a, Map<Long, Double> b) {
		double c = 0;
		for (Map.Entry<Long, Double> e : a.entrySet()) {
			Double v = b.get(e.getKey());
			if (v != null) {
				c += Math.min(v, e.getValue());
			}
		}
		return c;
	}
}
