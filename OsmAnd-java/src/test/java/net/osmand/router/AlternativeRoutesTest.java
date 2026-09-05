package net.osmand.router;

import java.io.File;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
 * Alternative routes of {@link HHRoutePlanner} (OsmAnd-Issues #2843).
 *
 * Cases live in {@code alternatives/test_alternative_routes.json}: the map, start and end, and what
 * the alternatives are expected to do. Every case also gets the general checks in
 * {@link #assertSaneAlternatives}, each of which stands for something that has actually gone wrong.
 *
 * The cases need whole city maps, far too big for test resources, so the map is taken from a local
 * directory and a case is skipped when its map is not there. Point {@code -Dosmand.maps.dir} at the
 * directory holding the OBF files to run them.
 */
@RunWith(Parameterized.class)
public class AlternativeRoutesTest {

	private static final String MAPS_DIR = System.getProperty("osmand.maps.dir",
			System.getProperty("user.home") + "/osmand/maps");

	/** no route may drive the same piece of road twice beyond this (u-turn manoeuvres) */
	private static final double MAX_RETRACED = 100;
	/** a larger gap between consecutive points means a shortcut was not expanded into roads */
	private static final double MAX_POINT_GAP = 4000;
	/** an alternative must offer, and must avoid, at least this share of the main route */
	private static final double MIN_DISTINCT_REL = 0.2;
	/** ... and never less than this, however short the route is */
	private static final double MIN_DISTINCT_FLOOR = 300;

	public static class ExpectedVia {
		String name;
		double latitude;
		double longitude;
		double toleranceMeters = 500;
	}

	public static class AlternativeTestEntry {
		String testName;
		String description;
		String map;
		String vehicle = "car";
		LatLon startPoint;
		LatLon endPoint;
		int minAlternatives = 1;
		int maxAlternatives = Integer.MAX_VALUE;
		boolean ignore;
		/** at least one alternative must pass through each of these */
		List<ExpectedVia> expectedVia = new ArrayList<>();
		/** no alternative may pass through any of these */
		List<ExpectedVia> unexpectedVia = new ArrayList<>();
	}

	private final AlternativeTestEntry te;

	public AlternativeRoutesTest(String name, AlternativeTestEntry te) {
		this.te = te;
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Iterable<Object[]> data() throws Exception {
		Reader reader = new InputStreamReader(Objects.requireNonNull(AlternativeRoutesTest.class
				.getResourceAsStream("/alternatives/test_alternative_routes.json")));
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		AlternativeTestEntry[] entries = gson.fromJson(reader, AlternativeTestEntry[].class);
		reader.close();
		List<Object[]> cases = new ArrayList<>();
		for (AlternativeTestEntry e : entries) {
			if (!e.ignore) {
				cases.add(new Object[] {e.testName, e});
			}
		}
		return cases;
	}

	@Test
	public void testAlternatives() throws Exception {
		List<Route> routes = calculate();
		Route main = routes.get(0);
		int found = routes.size() - 1;
		Assert.assertTrue("expected at least " + te.minAlternatives + " alternative(s), found " + found,
				found >= te.minAlternatives);
		Assert.assertTrue("expected at most " + te.maxAlternatives + " alternative(s), found " + found,
				found <= te.maxAlternatives);
		assertSaneAlternatives(routes, main);
		for (ExpectedVia via : te.expectedVia) {
			double d = closestAlternative(routes, via);
			Assert.assertTrue("no alternative passes through " + via.name + ", closest one is "
					+ Math.round(d) + " m away", d <= via.toleranceMeters);
		}
		for (ExpectedVia via : te.unexpectedVia) {
			double d = closestAlternative(routes, via);
			Assert.assertTrue("an alternative passes through " + via.name + " (" + Math.round(d) + " m)",
					d > via.toleranceMeters);
		}
	}

	private void assertSaneAlternatives(List<Route> routes, Route main) {
		Map<Long, Double> mainRoads = roads(main);
		double mainLength = length(mainRoads);
		HHRoutingConfig limits = new HHRoutingConfig();
		double maxCost = main.cost * (1 + limits.ALT_STRETCH) + limits.ALT_STRETCH_ABS + 1;
		for (int i = 1; i < routes.size(); i++) {
			Route alt = routes.get(i);
			String id = "alternative " + i;
			Map<Long, Double> altRoads = roads(alt);
			double shared = shared(altRoads, mainRoads);

			Assert.assertTrue(id + " costs " + Math.round(100 * (alt.cost / main.cost - 1))
					+ "% more than the main route", alt.cost <= maxCost);
			double minDistinct = Math.max(MIN_DISTINCT_FLOOR, MIN_DISTINCT_REL * mainLength);
			Assert.assertTrue(id + " has only " + Math.round(length(altRoads) - shared)
					+ " m of roads of its own", length(altRoads) - shared >= minDistinct);
			Assert.assertTrue(id + " avoids only " + Math.round(mainLength - shared)
					+ " m of the main route, so it is the main route with a detour",
					mainLength - shared >= minDistinct / 2);
			Assert.assertTrue(id + " drives " + Math.round(retraced(alt)) + " m of its own roads twice",
					retraced(alt) <= MAX_RETRACED);
			Assert.assertTrue(id + " has a " + Math.round(maxGap(alt))
					+ " m gap, a shortcut was not expanded into roads", maxGap(alt) <= MAX_POINT_GAP);
			Assert.assertEquals(id + " does not start where the main route starts", 0,
					distance(alt.points.get(0), main.points.get(0)), 200);
			Assert.assertEquals(id + " does not end where the main route ends", 0,
					distance(alt.points.get(alt.points.size() - 1),
							main.points.get(main.points.size() - 1)), 200);
		}
	}

	private static double closestAlternative(List<Route> routes, ExpectedVia via) {
		int vx = MapUtils.get31TileNumberX(via.longitude);
		int vy = MapUtils.get31TileNumberY(via.latitude);
		double best = Double.MAX_VALUE;
		for (int i = 1; i < routes.size(); i++) {
			for (int[] p : routes.get(i).points) {
				best = Math.min(best, MapUtils.squareRootDist31(p[0], p[1], vx, vy));
			}
		}
		return best;
	}

	private List<Route> calculate() throws Exception {
		File f = new File(MAPS_DIR, te.map);
		Assume.assumeTrue("map " + f + " is not available", f.exists());

		RandomAccessFile raf = new RandomAccessFile(f, "r");
		try {
			BinaryMapIndexReader[] readers = {new BinaryMapIndexReader(raf, f)};
			RoutePlannerFrontEnd router = new RoutePlannerFrontEnd();
			Builder builder = RoutingConfiguration.getDefault();
			RoutingMemoryLimits limits = new RoutingMemoryLimits(
					RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
					RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT);
			RoutingConfiguration config = builder.build(te.vehicle, limits,
					new LinkedHashMap<String, String>());
			RoutingContext ctx = router.buildRoutingContext(config, null, readers,
					RouteCalculationMode.NORMAL);
			ctx.calculationProgress = new RouteCalculationProgress();

			HHRoutingConfig hh = HHRoutePlanner.prepareDefaultRoutingConfig(null);
			hh.calcAlternative();
			router.setUseOnlyHHRouting(true).setHHRoutingConfig(hh);

			RouteCalcResult res = router.searchRoute(ctx, te.startPoint, te.endPoint,
					new ArrayList<LatLon>());
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

	private static class Route {
		final List<int[]> points = new ArrayList<>(); // x31, y31
		double cost;
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
				int[] last = r.points.isEmpty() ? null : r.points.get(r.points.size() - 1);
				if (last == null || last[0] != p[0] || last[1] != p[1]) {
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
			long key = key(r.points.get(i - 1), r.points.get(i));
			Double prev = m.get(key);
			m.put(key, (prev == null ? 0 : prev) + distance(r.points.get(i - 1), r.points.get(i)));
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
		Map<Long, Boolean> seen = new HashMap<>();
		double dup = 0;
		for (int i = 1; i < r.points.size(); i++) {
			long k = key(r.points.get(i - 1), r.points.get(i));
			if (seen.put(k, true) != null) {
				dup += distance(r.points.get(i - 1), r.points.get(i));
			}
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
