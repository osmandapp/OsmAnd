package net.osmand.router;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.router.SeaRoutePlanner.SeaRoute;
import net.osmand.router.SeaRoutePlanner.SeaRoutingConfig;
import net.osmand.util.MapUtils;

/**
 * Open water routing against real maps: the shores come out of the map section of an OBF, no map
 * generation and no routing section involved.
 *
 * Whole country maps are far too big for test resources, so they are read from a local directory and a
 * case is skipped when its map is missing - same arrangement as {@link AlternativeRoutesTest}. Point
 * {@code -Dosmand.maps.dir} at the folder holding the OBF files to run these.
 */
public class SeaRoutePlannerObfTest {

	private static final String MAPS_DIR = System.getProperty("osmand.maps.dir", System.getProperty("user.home") + "/osmand/maps");
	private static final int PLAN_ZOOM = 12;
	private static final int VERIFY_ZOOM = 16;

	private List<BinaryMapIndexReader> readers(String... names) throws Exception {
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		for (String name : names) {
			File file = new File(MAPS_DIR, name);
			Assume.assumeTrue("map " + file + " is not downloaded", file.exists());
			readers.add(new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file));
		}
		return readers;
	}

	private SeaObstacles load(List<BinaryMapIndexReader> readers, LatLon start, LatLon end, int zoom) throws Exception {
		double margin = Math.max(0.05, 0.3 * MapUtils.getDistance(start, end) / 111000);
		double lonMargin = margin / Math.cos(Math.toRadians((start.getLatitude() + end.getLatitude()) / 2));
		return SeaObstacles.readCoastline(readers,
				Math.min(start.getLatitude(), end.getLatitude()) - margin,
				Math.min(start.getLongitude(), end.getLongitude()) - lonMargin,
				Math.max(start.getLatitude(), end.getLatitude()) + margin,
				Math.max(start.getLongitude(), end.getLongitude()) + lonMargin, zoom);
	}

	private static SeaRoutePlanner planner() {
		SeaRoutingConfig config = new SeaRoutingConfig();
		config.cornerClearance = 80;
		config.minClearance = 40;
		return new SeaRoutePlanner(config);
	}

	/** Valletta to Mġarr on Gozo: out of Grand Harbour, through the Comino channel. */
	@Test
	public void vallettaToGozo() throws Exception {
		List<BinaryMapIndexReader> readers = readers("Malta_europe_2.obf");
		LatLon start = new LatLon(35.9050, 14.5300), end = new LatLon(36.0200, 14.3050);
		SeaRoutePlanner planner = planner();

		SeaObstacles planning = load(readers, start, end, PLAN_ZOOM);
		SeaRoute route = planner.plan(planning, start, end);

		Assert.assertNotNull(route);
		double direct = MapUtils.getDistance(start, end);
		Assert.assertTrue("route " + route.distance + " vs direct " + direct, route.distance < direct * 1.15);

		// the plan is made on generalized geometry, so it is verified against the detailed one
		SeaObstacles detailed = load(readers, start, end, VERIFY_ZOOM);
		Assert.assertFalse("route crosses the detailed coastline", planner.crossesShore(detailed, route));
		Assert.assertTrue("clearance " + planner.measureClearance(detailed, route),
				planner.measureClearance(detailed, route) > 20);
	}

	/** Around the west of Malta, where the straight line runs over the island. */
	@Test
	public void aroundTheWestCoastOfMalta() throws Exception {
		List<BinaryMapIndexReader> readers = readers("Malta_europe_2.obf");
		LatLon start = new LatLon(35.9300, 14.3300), end = new LatLon(35.8150, 14.4500);
		SeaRoutePlanner planner = planner();

		SeaObstacles planning = load(readers, start, end, PLAN_ZOOM);
		SeaRoute route = planner.plan(planning, start, end);

		Assert.assertNotNull(route);
		Assert.assertTrue("the island forces a detour", route.getLegs() > 1);
		Assert.assertTrue(route.distance > MapUtils.getDistance(start, end));

		SeaObstacles detailed = load(readers, start, end, VERIFY_ZOOM);
		Assert.assertFalse(planner.crossesShore(detailed, route));
	}

	/** Oslo to Drøbak: 27 km of fjord, hundreds of islands, two maps. */
	@Test
	public void osloFjordThroughTheIslands() throws Exception {
		List<BinaryMapIndexReader> readers = readers("Norway_oslo_europe_2.obf", "Norway_akershus_europe_2.obf");
		LatLon start = new LatLon(59.9040, 10.7270), end = new LatLon(59.6630, 10.6190);
		SeaRoutePlanner planner = planner();

		SeaObstacles planning = load(readers, start, end, PLAN_ZOOM);
		SeaRoute route = planner.plan(planning, start, end);

		Assert.assertNotNull(route);
		Assert.assertTrue("corners " + route.corners, route.corners > 100);
		Assert.assertTrue(route.distance < MapUtils.getDistance(start, end) * 1.2);

		SeaObstacles detailed = load(readers, start, end, VERIFY_ZOOM);
		Assert.assertFalse(planner.crossesShore(detailed, route));
		Assert.assertTrue("clearance " + planner.measureClearance(detailed, route),
				planner.measureClearance(detailed, route) > 20);
	}

	/** An offshore passage on the world basemap alone, with no detailed map downloaded. */
	@Test
	public void basemapOnlyPassageAroundCapoPassero() throws Exception {
		List<BinaryMapIndexReader> readers = readers("World_basemap_2.obf");
		LatLon start = new LatLon(36.6900, 14.8500), end = new LatLon(37.0600, 15.3200);
		SeaRoutingConfig config = new SeaRoutingConfig();
		config.cornerClearance = 300;
		config.minClearance = 150;
		SeaRoutePlanner planner = new SeaRoutePlanner(config);

		SeaObstacles planning = load(readers, start, end, 10);
		SeaRoute route = planner.plan(planning, start, end);

		Assert.assertNotNull(route);
		Assert.assertTrue("the cape has to be rounded", route.getLegs() > 1);
		Assert.assertFalse(planner.crossesShore(planning, route));
	}

	/**
	 * Harlingen: the harbour is closed off by the coastline, so a berth inside it reads as land. The
	 * route may only start once the point has been moved onto open water.
	 */
	@Test
	public void harbourBerthIsMovedOntoWater() throws Exception {
		List<BinaryMapIndexReader> readers = readers("Netherlands_friesland_europe_2.obf");
		LatLon berth = new LatLon(53.1730, 5.4150), terschelling = new LatLon(53.3640, 5.2200);

		SeaObstacles planning = load(readers, berth, terschelling, PLAN_ZOOM);
		Assert.assertTrue("the berth sits behind the coastline", planning.isLand(berth));

		LatLon water = planning.snapToWater(berth, 40, 3000);
		Assert.assertNotNull("no water within 3 km of the berth", water);
		Assert.assertFalse(planning.isLand(water));
		Assert.assertTrue(MapUtils.getDistance(berth, water) <= 3000);
	}

	/**
	 * Vlissingen: the nearest water to a point in town is a dock behind the locks, from which the Scheldt
	 * cannot be reached over open water. The route has to fall back to farther water instead of failing.
	 */
	@Test
	public void vlissingenTownPointReachesTheScheldtPastTheDocks() throws Exception {
		List<BinaryMapIndexReader> readers = readers("Netherlands_zeeland_europe_2.obf", "Belgium_flanders_europe_2.obf");
		LatLon town = new LatLon(51.4420, 3.5700), scheldt = new LatLon(51.4300, 3.5800);
		SeaRoutePlanner planner = planner();

		SeaObstacles planning = load(readers, town, scheldt, PLAN_ZOOM);
		SeaRoute route = planner.plan(planning, town, scheldt);

		Assert.assertNotNull("no route out of Vlissingen", route);
		Assert.assertTrue("the nearest water should have been skipped, attempts " + route.attempts, route.attempts > 1);
		Assert.assertFalse(planner.crossesShore(planning, route));
	}

	/**
	 * The IJsselmeer carries no coastline at all - it is a natural=water area - so a coastline-only
	 * load sees open water and would happily route over land. Guards the reason water areas have to be
	 * loaded as well.
	 */
	@Test
	public void lakeHasNoCoastlineAtAll() throws Exception {
		List<BinaryMapIndexReader> readers = readers("Netherlands_friesland_europe_2.obf",
				"Netherlands_noord-holland_europe_2.obf");
		LatLon enkhuizen = new LatLon(52.7100, 5.3000), stavoren = new LatLon(52.8850, 5.3600);

		SeaObstacles planning = load(readers, enkhuizen, stavoren, PLAN_ZOOM);

		Assert.assertEquals("no coastline is expected around the IJsselmeer", 0, planning.getSegmentsCount());
	}

	@Test
	public void coarsePlanningAgreesWithDetailedPlanning() throws Exception {
		List<BinaryMapIndexReader> readers = readers("Malta_europe_2.obf");
		LatLon start = new LatLon(35.9050, 14.5300), end = new LatLon(36.0200, 14.3050);
		SeaRoutePlanner planner = planner();

		SeaRoute coarse = planner.plan(load(readers, start, end, PLAN_ZOOM), start, end);
		SeaRoute detailed = planner.plan(load(readers, start, end, VERIFY_ZOOM), start, end);

		Assert.assertNotNull(coarse);
		Assert.assertNotNull(detailed);
		Assert.assertEquals("planning on z" + PLAN_ZOOM + " must give the same route as on z" + VERIFY_ZOOM,
				detailed.distance, coarse.distance, Math.max(50, detailed.distance * 0.01));
		Assert.assertTrue("the coarse graph is the cheaper one, " + coarse.corners + " vs " + detailed.corners,
				coarse.corners <= detailed.corners);
	}
}
