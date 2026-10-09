package net.osmand.router.sea;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingContext;
import net.osmand.router.sea.BoatRoutePlanner.BoatRoute;
import net.osmand.util.MapUtils;

/**
 * Boat routes over the water network and open water on real maps (OsmAnd-Issues #3170). The network part is
 * routed by BinaryRoutePlanner with the boat profile of routing.xml, {@link BoatRoutePlanner} decides the rest.
 *
 * Cases live in {@code boat_routing.json} (resources/test-resources): every route reported on the web map becomes
 * one entry with its link, the maps it needs and what is expected - allowed choices, ways it must use, whether it
 * reaches both points, a time limit. Whole country maps are far too big for test resources, so they are read from
 * a local directory and a case is skipped when a map is missing, as in {@link AlternativeRoutesTest}. Point
 * {@code -Dosmand.maps.dir} at the folder holding the OBF files to run these.
 */
@Ignore("Boat routing cases need country maps and are not ready for CI")
@RunWith(Parameterized.class)
public class BoatRoutingTest {

	private static final String MAPS_DIR = System.getProperty("osmand.maps.dir",
			System.getProperty("user.home") + "/osmand/maps");

	static class BoatCase {
		String name;
		String url;
		double[] start;
		double[] end;
		/** Intermediate points: every leg between neighbouring points must meet the expectations. */
		List<double[]> via;
		List<String> maps;
		List<String> choices;
		List<String> ways;
		Boolean joinsPoints;
		Long maxTimeMs;
		String status;
		boolean ignore;
	}

	/** How close the drawn route has to start and end to its points. */
	private static final double JOINED_METERS = 25;

	private final BoatCase boatCase;

	public BoatRoutingTest(String name, BoatCase boatCase) {
		this.boatCase = boatCase;
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Iterable<Object[]> data() throws Exception {
		List<Object[]> cases = new ArrayList<>();
		InputStream in = BoatRoutingTest.class.getResourceAsStream("/boat_routing.json");
		if (in == null) {
			return cases; // the resources checkout is not next to android
		}
		try (InputStreamReader reader = new InputStreamReader(in)) {
			for (BoatCase c : new Gson().fromJson(reader, BoatCase[].class)) {
				if (!c.ignore) {
					cases.add(new Object[] { c.name, c });
				}
			}
		}
		return cases;
	}

	/** A case without maxTimeMs: every working case takes a few seconds, a hang shows up in 20. */
	private static final long DEFAULT_TIME_LIMIT_MS = 20000;

	@Test
	public void route() throws Exception {
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		for (String name : boatCase.maps) {
			File file = new File(MAPS_DIR, name);
			Assume.assumeTrue("map " + file + " is not downloaded", file.exists());
			readers.add(new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file));
		}
		List<LatLon> points = new ArrayList<>();
		points.add(new LatLon(boatCase.start[0], boatCase.start[1]));
		if (boatCase.via != null) {
			for (double[] p : boatCase.via) {
				points.add(new LatLon(p[0], p[1]));
			}
		}
		points.add(new LatLon(boatCase.end[0], boatCase.end[1]));

		RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
		fe.CALCULATE_MISSING_MAPS = false;
		RoutingMemoryLimits limits = new RoutingMemoryLimits(RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
				RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT);
		RoutingConfiguration config = RoutingConfiguration.getDefault().build("boat", limits, new HashMap<>());
		RoutingContext ctx = fe.buildRoutingContext(config, null, readers.toArray(new BinaryMapIndexReader[0]),
				RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);

		long started = System.currentTimeMillis();
		// a hang fails at the case's own limit, not after minutes: the planner cannot be interrupted, so it runs in a
		// daemon thread that is left behind
		BoatRoutePlanner planner = planner(readers);
		java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
			Thread thread = new Thread(r, "boat-route");
			thread.setDaemon(true);
			return thread;
		});
		java.util.concurrent.Future<List<BoatRoute>> future = executor.submit(() -> planner.route(fe, ctx, points));
		executor.shutdown();
		long limitMs = boatCase.maxTimeMs != null ? boatCase.maxTimeMs : DEFAULT_TIME_LIMIT_MS;
		List<BoatRoute> legs;
		try {
			legs = future.get(limitMs, java.util.concurrent.TimeUnit.MILLISECONDS);
		} catch (java.util.concurrent.TimeoutException e) {
			throw new AssertionError("no route after " + limitMs + " ms - " + boatCase.name + ", " + boatCase.url);
		}
		long timeMs = System.currentTimeMillis() - started;
		Assert.assertEquals(points.size() - 1, legs.size());
		String what = boatCase.name + " [" + boatCase.status + "]: " + legs.stream().map(l -> l.decision.toString())
				.collect(java.util.stream.Collectors.joining("; ")) + ", " + timeMs + " ms in total, " + boatCase.url;
		System.out.println(what);

		for (String way : boatCase.ways == null ? new ArrayList<String>() : boatCase.ways) {
			Assert.assertTrue("does not use " + way + " - " + what, legs.stream().anyMatch(l -> usesWay(l, way)));
		}
		if (boatCase.maxTimeMs != null) {
			Assert.assertTrue("took " + timeMs + " ms - " + what, timeMs <= boatCase.maxTimeMs);
		}
		for (int i = 0; i < legs.size(); i++) {
			BoatRoute route = legs.get(i);
			LatLon start = points.get(i), end = points.get(i + 1);
			if (boatCase.choices != null) {
				Assert.assertTrue("leg " + i + " choice not in " + boatCase.choices + " - " + what,
						boatCase.choices.contains(route.decision.choice));
			}
			if (Boolean.TRUE.equals(boatCase.joinsPoints)) {
				Assert.assertTrue("leg " + i + " has no route - " + what, route.isOpenWater() || route.isNetwork());
				LatLon first = route.isOpenWater() ? route.openWater.get(0)
						: route.startJoin != null ? route.startJoin.get(0)
						: route.startConnector != null ? route.startConnector.get(0)
						: route.network.get(0).getStartPoint();
				LatLon last = route.isOpenWater() ? route.openWater.get(route.openWater.size() - 1)
						: route.endJoin != null ? route.endJoin.get(route.endJoin.size() - 1)
						: route.endConnector != null ? route.endConnector.get(route.endConnector.size() - 1)
						: route.network.get(route.network.size() - 1).getEndPoint();
				Assert.assertTrue("leg " + i + " starts " + (int) MapUtils.getDistance(first, start) + " m from its point - "
						+ what, MapUtils.getDistance(first, start) <= JOINED_METERS);
				Assert.assertTrue("leg " + i + " ends " + (int) MapUtils.getDistance(last, end) + " m from its point - "
						+ what, MapUtils.getDistance(last, end) <= JOINED_METERS);
			}
			assertStaysOnWater(readers, route.startConnector, start, end);
			assertStaysOnWater(readers, route.endConnector, start, end);
			assertStaysOnWater(readers, route.openWater, start, end);
		}
	}

	/**
	 * Shores of the given maps, the world basemap only for offshore corridors, and the basemap's land tiles for
	 * points far from any shore - as the server does.
	 */
	private static BoatRoutePlanner planner(List<BinaryMapIndexReader> readers) throws Exception {
		File basemap = new File(MAPS_DIR, "World_basemap_2.obf");
		BinaryMapIndexReader base = basemap.exists()
				? new BinaryMapIndexReader(new RandomAccessFile(basemap, "r"), basemap) : null;
		BasemapLandTiles landTiles = base == null ? null : new BasemapLandTiles(base);
		return new BoatRoutePlanner((minLat, minLon, maxLat, maxLon, offshore) -> {
			List<BinaryMapIndexReader> use = new ArrayList<>(readers);
			if (base != null && offshore) {
				use.add(base);
			}
			SeaObstacles obstacles = SeaObstacles.readShores(use, minLat, minLon, maxLat, maxLon, offshore ? 9 : 12);
			if (base != null && !offshore && obstacles.getSegmentsCount() == 0) {
				// as the server does: no detailed map here, the basemap is better than nothing
				use.add(base);
				obstacles = SeaObstacles.readShores(use, minLat, minLon, maxLat, maxLon, 11);
			}
			obstacles.setFarFromShore(landTiles);
			return obstacles;
		});
	}

	private static boolean usesWay(BoatRoute route, String name) {
		if (route.network == null) {
			return false;
		}
		for (RouteSegmentResult r : route.network) {
			if (name.equals(r.getObject().getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Fails when an open water line crosses the detailed shores. Two legs are allowed to, both touching a requested
	 * point: from a point on land - a berth mapped behind the shore, a town - across the shore, and to a point on a
	 * tidal flat across the flat's edge (not across coastline, and not for more than 10 km).
	 */
	private static void assertStaysOnWater(List<BinaryMapIndexReader> readers, List<LatLon> line, LatLon... requested)
			throws Exception {
		if (line == null || line.size() < 2) {
			return;
		}
		double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
		for (LatLon p : line) {
			minLat = Math.min(minLat, p.getLatitude());
			maxLat = Math.max(maxLat, p.getLatitude());
			minLon = Math.min(minLon, p.getLongitude());
			maxLon = Math.max(maxLon, p.getLongitude());
		}
		SeaObstacles detailed = SeaObstacles.readShores(readers, minLat - 0.02, minLon - 0.03, maxLat + 0.02,
				maxLon + 0.03, 16);
		for (int i = 1; i < line.size(); i++) {
			LatLon a = line.get(i - 1), b = line.get(i);
			if (detailed.isClear(detailed.x(a.getLongitude()), detailed.y(a.getLatitude()),
					detailed.x(b.getLongitude()), detailed.y(b.getLatitude()), 0)) {
				continue;
			}
			boolean allowed = false;
			for (LatLon p : requested) {
				if ((p.equals(a) || p.equals(b)) && detailed.isLand(p)) {
					allowed = true; // leaving a point on land
				} else if (MapUtils.getDistance(a, p) <= 10000 && MapUtils.getDistance(b, p) <= 10000) {
					// the approach to a point on a tidal flat: flat edges only, never coastline
					detailed.setBarriersEnabled(false);
					allowed |= detailed.isClear(detailed.x(a.getLongitude()), detailed.y(a.getLatitude()),
							detailed.x(b.getLongitude()), detailed.y(b.getLatitude()), 0);
					detailed.setBarriersEnabled(true);
				}
			}
			Assert.assertTrue("leg " + i + " of " + (line.size() - 1) + " " + a + " -> " + b + " crosses the shore; line "
					+ line + ", requested " + java.util.Arrays.toString(requested), allowed);
		}
	}
}
