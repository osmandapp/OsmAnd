package net.osmand.router;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.router.HHRouteDataStructure.HHRoutingConfig;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.MapUtils;

/**
 * Regression cases for the alternative routes of {@link HHRoutePlanner} (OsmAnd-Issues #2843).
 *
 * These need a full city map, which is far too big to keep in test resources, so the map is taken
 * from a local directory and the test is skipped when it is not there. Point -Dosmand.maps.dir at
 * the directory holding the OBF files to run it.
 */
public class AlternativeRoutesTest {

	private static final String MAPS_DIR = System.getProperty("osmand.maps.dir",
			System.getProperty("user.home") + "/osmand/maps");
	private static final String KYIV_MAP = "Ukraine_kyiv_europe_2.obf";

	// how far a route may run from a point that it is expected to pass through
	private static final double EXPECTED_VIA_TOLERANCE = 500;
	// no route may drive the same piece of road twice beyond this (u-turn manoeuvres)
	private static final double MAX_RETRACED = 100;
	// a gap this large between consecutive points means a shortcut was not expanded into roads
	private static final double MAX_POINT_GAP = 4000;

	private static class Route {
		List<int[]> points = new ArrayList<>(); // x31, y31
		double length;
		double cost;
	}

	@Test
	public void podilToTeremkyOffersTheRouteThroughSviatoshyn() throws Exception {
		// The alternative through Sviatoshyn and Borshchahivka used to be dropped, because the
		// detailed cost of four of its shortcuts in a row disagreed with the hub graph.
		List<Route> routes = calculate(KYIV_MAP,
				new LatLon(50.46904, 30.51638), new LatLon(50.37401, 30.44741));
		assertSaneAlternatives(routes);
		assertSomeAlternativePassesThrough(routes, new LatLon(50.45620, 30.36338), "Sviatoshyn");
	}

	@Test
	public void podilToDarnytsiaDoesNotOfferTheMainRouteWithALoop() throws Exception {
		// This one used to return the main route plus a loop at the destination: 99% of the main
		// route was covered by the "alternative", which only added a detour of its own.
		List<Route> routes = calculate(KYIV_MAP,
				new LatLon(50.46947, 30.51361), new LatLon(50.43286, 30.60940));
		assertSaneAlternatives(routes);
	}

	private void assertSaneAlternatives(List<Route> routes) {
		Route main = routes.get(0);
		Assert.assertTrue("no alternatives were found", routes.size() > 1);
		Map<Long, Double> mainRoads = roads(main);
		double mainLength = length(mainRoads);
		for (int i = 1; i < routes.size(); i++) {
			Route alt = routes.get(i);
			String id = "alternative " + i;
			Map<Long, Double> altRoads = roads(alt);
			double altLength = length(altRoads);
			double shared = shared(altRoads, mainRoads);

			Assert.assertTrue(id + " costs " + Math.round(100 * (alt.cost / main.cost - 1))
					+ "% more than the main route",
					alt.cost <= main.cost * (1 + new HHRoutingConfig().ALT_STRETCH) + 1);
			Assert.assertTrue(id + " has only " + Math.round(altLength - shared)
					+ " m of roads of its own", altLength - shared >= 1500);
			Assert.assertTrue(id + " avoids only " + Math.round(mainLength - shared)
					+ " m of the main route, so it is the main route with a detour",
					mainLength - shared >= 1500);
			Assert.assertTrue(id + " drives " + Math.round(retraced(alt)) + " m of its own roads twice",
					retraced(alt) <= MAX_RETRACED);
			Assert.assertTrue(id + " has a " + Math.round(maxGap(alt))
					+ " m gap, a shortcut was not expanded into roads", maxGap(alt) <= MAX_POINT_GAP);
			Assert.assertEquals(id + " does not start where the main route starts", 0,
					distance(alt.points.get(0), main.points.get(0)), 200);
			Assert.assertEquals(id + " does not end where the main route ends", 0,
					distance(alt.points.get(alt.points.size() - 1), main.points.get(main.points.size() - 1)), 200);
		}
	}

	private void assertSomeAlternativePassesThrough(List<Route> routes, LatLon via, String name) {
		int vx = MapUtils.get31TileNumberX(via.getLongitude());
		int vy = MapUtils.get31TileNumberY(via.getLatitude());
		double best = Double.MAX_VALUE;
		for (int i = 1; i < routes.size(); i++) {
			for (int[] p : routes.get(i).points) {
				best = Math.min(best, MapUtils.squareRootDist31(p[0], p[1], vx, vy));
			}
		}
		Assert.assertTrue("no alternative passes through " + name + ", closest one is "
				+ Math.round(best) + " m away", best <= EXPECTED_VIA_TOLERANCE);
	}

	private List<Route> calculate(String map, LatLon start, LatLon end) throws Exception {
		File f = new File(MAPS_DIR, map);
		Assume.assumeTrue("map " + f + " is not available", f.exists());

		RandomAccessFile raf = new RandomAccessFile(f, "r");
		try {
			BinaryMapIndexReader[] readers = {new BinaryMapIndexReader(raf, f)};
			RoutePlannerFrontEnd router = new RoutePlannerFrontEnd();
			Builder builder = RoutingConfiguration.getDefault();
			RoutingMemoryLimits limits = new RoutingMemoryLimits(
					RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
					RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT);
			RoutingConfiguration config = builder.build("car", limits, new LinkedHashMap<String, String>());
			RoutingContext ctx = router.buildRoutingContext(config, null, readers, RouteCalculationMode.NORMAL);
			ctx.calculationProgress = new RouteCalculationProgress();

			HHRoutingConfig hh = HHRoutePlanner.prepareDefaultRoutingConfig(null);
			hh.calcAlternative();
			router.setUseOnlyHHRouting(true).setHHRoutingConfig(hh);

			RouteCalcResult res = router.searchRoute(ctx, start, end, new ArrayList<LatLon>());
			Assert.assertNull(res.getError(), res.getError());
			Assert.assertFalse("main route is empty", res.getList().isEmpty());

			List<Route> routes = new ArrayList<>();
			routes.add(toRoute(res.getList()));
			for (List<RouteSegmentResult> alt : res.getAlternatives()) {
				routes.add(toRoute(alt));
			}
			return routes;
		} finally {
			raf.close();
		}
	}

	private static Route toRoute(List<RouteSegmentResult> segments) {
		Route r = new Route();
		for (RouteSegmentResult s : segments) {
			r.cost += s.getRoutingTime();
			RouteDataObject o = s.getObject();
			int i = s.getStartPointIndex(), end = s.getEndPointIndex();
			int step = i <= end ? 1 : -1;
			while (true) {
				int[] p = {o.getPoint31XTile(i), o.getPoint31YTile(i)};
				if (r.points.isEmpty() || r.points.get(r.points.size() - 1)[0] != p[0]
						|| r.points.get(r.points.size() - 1)[1] != p[1]) {
					r.points.add(p);
				}
				if (i == end) {
					break;
				}
				i += step;
			}
		}
		return r;
	}

	/** road piece -> length, so that two routes can be compared on the roads themselves */
	private static Map<Long, Double> roads(Route r) {
		Map<Long, Double> m = new HashMap<>();
		for (int i = 1; i < r.points.size(); i++) {
			int[] a = r.points.get(i - 1), b = r.points.get(i);
			long key = key(a, b);
			double d = distance(a, b);
			Double prev = m.get(key);
			m.put(key, (prev == null ? 0 : prev) + d);
		}
		return m;
	}

	private static long key(int[] a, int[] b) {
		long p = (((long) a[0]) << 32) | (a[1] & 0xffffffffL);
		long q = (((long) b[0]) << 32) | (b[1] & 0xffffffffL);
		return Math.min(p, q) * 1000003L + Math.max(p, q);
	}

	private static double distance(int[] a, int[] b) {
		return MapUtils.squareRootDist31(a[0], a[1], b[0], b[1]);
	}

	private static double length(Map<Long, Double> roads) {
		double d = 0;
		for (Double v : roads.values()) {
			d += v;
		}
		return d;
	}

	private static double shared(Map<Long, Double> a, Map<Long, Double> b) {
		double common = 0;
		for (Map.Entry<Long, Double> e : a.entrySet()) {
			Double v = b.get(e.getKey());
			if (v != null) {
				common += Math.min(v, e.getValue());
			}
		}
		return common;
	}

	private static double retraced(Route r) {
		Map<Long, Double> seen = new HashMap<>();
		double dup = 0;
		for (int i = 1; i < r.points.size(); i++) {
			int[] a = r.points.get(i - 1), b = r.points.get(i);
			long k = key(a, b);
			if (seen.containsKey(k)) {
				dup += distance(a, b);
			}
			seen.put(k, 1.0);
		}
		return dup;
	}

	private static double maxGap(Route r) {
		double mx = 0;
		for (int i = 1; i < r.points.size(); i++) {
			mx = Math.max(mx, distance(r.points.get(i - 1), r.points.get(i)));
		}
		return mx;
	}
}
