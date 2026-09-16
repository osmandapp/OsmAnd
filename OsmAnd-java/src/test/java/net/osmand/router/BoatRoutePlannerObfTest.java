package net.osmand.router;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.router.BoatRoutePlanner.BoatRoute;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.MapUtils;

/**
 * Boat routes over the water network and open water on real maps (OsmAnd-Issues #3170): the network part is
 * routed by BinaryRoutePlanner with the boat profile of routing.xml, {@link BoatRoutePlanner} decides the rest.
 *
 * Every case is a route checked by hand on the web map. Whole country maps are far too big for test resources, so
 * they are read from a local directory and a case is skipped when a map is missing, as in
 * {@link AlternativeRoutesTest}. Point {@code -Dosmand.maps.dir} at the folder holding the OBF files to run these.
 */
public class BoatRoutePlannerObfTest {

	private static final String MAPS_DIR = System.getProperty("osmand.maps.dir",
			System.getProperty("user.home") + "/osmand/maps");
	private static final String[] NETHERLANDS = { "Netherlands_friesland_europe_2.obf",
			"Netherlands_noord-holland_europe_2.obf", "Netherlands_zuid-holland_europe_2.obf",
			"Netherlands_utrecht_europe_2.obf", "Netherlands_flevoland_europe_2.obf" };

	private static List<BinaryMapIndexReader> readers(String... names) throws Exception {
		List<BinaryMapIndexReader> readers = new ArrayList<>();
		for (String name : names) {
			File file = new File(MAPS_DIR, name);
			Assume.assumeTrue("map " + file + " is not downloaded", file.exists());
			readers.add(new BinaryMapIndexReader(new RandomAccessFile(file, "r"), file));
		}
		return readers;
	}

	/** The water network route as the server computes it, or null when the router finds none. */
	private static List<RouteSegmentResult> network(List<BinaryMapIndexReader> readers, LatLon start, LatLon end)
			throws Exception {
		RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
		fe.CALCULATE_MISSING_MAPS = false;
		RoutingMemoryLimits limits = new RoutingMemoryLimits(RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
				RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT);
		RoutingConfiguration config = RoutingConfiguration.getDefault().build("boat", limits, new HashMap<>());
		RoutingContext ctx = fe.buildRoutingContext(config, null, readers.toArray(new BinaryMapIndexReader[0]),
				RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
		try {
			RouteCalcResult result = fe.searchRoute(ctx, start, end, null);
			return result == null || result.detailed.isEmpty() ? null : result.detailed;
		} catch (IllegalArgumentException e) {
			return null; // no waterway found near the start or the end
		}
	}

	/**
	 * Coastline of the given maps, the world basemap only for offshore corridors, and the basemap's land tiles
	 * for points far from any shore - as the server does.
	 */
	private static BoatRoutePlanner planner(List<BinaryMapIndexReader> readers) throws Exception {
		File basemap = new File(MAPS_DIR, "World_basemap_2.obf");
		BasemapLandTiles landTiles = basemap.exists()
				? new BasemapLandTiles(new BinaryMapIndexReader(new RandomAccessFile(basemap, "r"), basemap)) : null;
		return new BoatRoutePlanner((minLat, minLon, maxLat, maxLon, offshore) -> {
			List<BinaryMapIndexReader> use = new ArrayList<>();
			for (BinaryMapIndexReader r : readers) {
				if (offshore || !r.getFile().getName().startsWith("World_")) {
					use.add(r);
				}
			}
			SeaObstacles obstacles = SeaObstacles.readCoastline(use.isEmpty() ? readers : use, minLat, minLon,
					maxLat, maxLon, offshore ? 9 : 12);
			obstacles.setFarFromShore(landTiles);
			return obstacles;
		});
	}

	private static BoatRoute route(List<BinaryMapIndexReader> readers, LatLon start, LatLon end) throws Exception {
		BoatRoute route = planner(readers).plan(Arrays.asList(start, end), network(readers, start, end));
		System.out.println(start + " -> " + end + ": " + route.decision);
		return route;
	}

	/**
	 * Fails when an open water line crosses the detailed coastline. A requested point on land - a berth mapped
	 * behind the shore, a town - can only be left across the shore, so the leg that touches it is allowed to.
	 */
	private static void assertStaysOnWater(List<BinaryMapIndexReader> readers, List<LatLon> line,
			LatLon... requested) throws Exception {
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
		SeaObstacles detailed = SeaObstacles.readCoastline(readers, minLat - 0.02, minLon - 0.03, maxLat + 0.02,
				maxLon + 0.03, 16);
		for (int i = 1; i < line.size(); i++) {
			LatLon a = line.get(i - 1), b = line.get(i);
			if (detailed.isClear(detailed.x(a.getLongitude()), detailed.y(a.getLatitude()),
					detailed.x(b.getLongitude()), detailed.y(b.getLatitude()), 0)) {
				continue;
			}
			boolean leavesLandPoint = false;
			for (LatLon p : requested) {
				if ((p.equals(a) || p.equals(b)) && detailed.isLand(p)) {
					leavesLandPoint = true;
				}
			}
			Assert.assertTrue("leg " + i + " of " + (line.size() - 1) + " " + a + " -> " + b
					+ " crosses the shore", leavesLandPoint);
		}
	}

	private static boolean usesWay(BoatRoute route, String name) {
		for (RouteSegmentResult r : route.network) {
			if (name.equals(r.getObject().getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Harlingen to the Vliestroom. The end is in a channel with no waterway near it, so the router stops about 6 km
	 * short. The Wadden Sea has to be crossed by its fairways - the straight way over the flats is shorter, but only
	 * on the map.
	 */
	@Test
	public void harlingenToVliestroomFollowsTheWaddenChannels() throws Exception {
		List<BinaryMapIndexReader> readers = readers(NETHERLANDS);
		LatLon harlingen = new LatLon(53.1730, 5.4150), vliestroom = new LatLon(53.378395, 5.066757);

		BoatRoute route = route(readers, harlingen, vliestroom);

		Assert.assertTrue(route.decision.toString(), route.isNetwork());
		Assert.assertNull("the network starts in Harlingen", route.startConnector);
		Assert.assertNotNull("the network stops short of the Vliestroom", route.endConnector);
		double total = BoatRoutePlanner.routeDistance(route.network) + route.getConnectorsDistance();
		Assert.assertTrue("route " + total + " m", total > 30000 && total < 50000);
		assertStaysOnWater(readers, route.endConnector, harlingen, vliestroom);
	}

	/**
	 * From the North Sea to a canal in Leiden: open water to the IJmuiden approach channel, then canals. The router
	 * alone puts the start 100 km away on the IJgeul, open water alone cannot reach Leiden at all.
	 */
	@Test
	public void northSeaToLeidenEntersThroughIJmuiden() throws Exception {
		List<BinaryMapIndexReader> readers = readers(NETHERLANDS);
		LatLon northSea = new LatLon(53.419250, 4.229736), leiden = new LatLon(52.160034, 4.504781);

		BoatRoute route = route(readers, northSea, leiden);

		Assert.assertTrue(route.decision.toString(), route.isNetwork());
		Assert.assertTrue("enters by the IJmuiden approach channel", usesWay(route, "IJgeul"));
		Assert.assertNotNull("open water from the North Sea to the channel", route.startConnector);
		Assert.assertTrue("the canals reach Leiden", route.decision.networkToEnd < BoatRoutePlanner.ENDPOINT_TOLERANCE_METERS);
		assertStaysOnWater(readers, route.startConnector, northSea, leiden);
	}

	/** Harlingen to West-Terschelling: fairways all the way, nothing to add. */
	@Test
	public void harlingenToTerschellingStaysOnTheFairways() throws Exception {
		List<BinaryMapIndexReader> readers = readers(NETHERLANDS);

		BoatRoute route = route(readers, new LatLon(53.1730, 5.4150), new LatLon(53.3640, 5.2200));

		Assert.assertEquals(route.decision.toString(), "network", route.decision.choice);
		Assert.assertNull(route.startConnector);
		Assert.assertNull(route.endConnector);
	}

	/** Vlissingen to Westkapelle: both points are on land, the water network is joined over short open water. */
	@Test
	public void vlissingenToWestkapelleReachesBothPoints() throws Exception {
		List<BinaryMapIndexReader> readers = readers("Netherlands_zeeland_europe_2.obf", "Belgium_flanders_europe_2.obf");

		LatLon vlissingen = new LatLon(51.4420, 3.5700), westkapelle = new LatLon(51.5400, 3.4400);

		BoatRoute route = route(readers, vlissingen, westkapelle);

		Assert.assertTrue(route.decision.toString(), route.isNetwork() || route.isOpenWater());
		assertStaysOnWater(readers, route.startConnector, vlissingen, westkapelle);
		assertStaysOnWater(readers, route.endConnector, vlissingen, westkapelle);
		assertStaysOnWater(readers, route.openWater, vlissingen, westkapelle);
	}

	/** Oslo to Drøbak: the router joins a stream 20 km away; the fjord itself is open water. */
	@Test
	public void osloToDrobakGoesDownTheFjord() throws Exception {
		List<BinaryMapIndexReader> readers = readers("Norway_oslo_europe_2.obf", "Norway_akershus_europe_2.obf");
		LatLon oslo = new LatLon(59.9040, 10.7270), drobak = new LatLon(59.6630, 10.6190);

		BoatRoute route = route(readers, oslo, drobak);

		Assert.assertTrue(route.decision.toString(), route.isOpenWater());
		assertStaysOnWater(readers, route.openWater, oslo, drobak);
		Assert.assertTrue(BoatRoutePlanner.length(route.openWater) < MapUtils.getDistance(oslo, drobak) * 1.2);
	}

	/** Valletta to Gozo: Malta has no water network for boats, the whole route is open water. */
	@Test
	public void vallettaToGozoIsOpenWater() throws Exception {
		List<BinaryMapIndexReader> readers = readers("Malta_europe_2.obf");

		LatLon valletta = new LatLon(35.9050, 14.5300), gozo = new LatLon(36.0200, 14.3050);

		BoatRoute route = route(readers, valletta, gozo);

		Assert.assertTrue(route.decision.toString(), route.isOpenWater());
		assertStaysOnWater(readers, route.openWater, valletta, gozo);
	}

	/**
	 * A boat in the forest of the Utrechtse Heuvelrug: no waterway and no shore within kilometres. A point that far
	 * from any coastline used to look like open sea and got a straight line through the trees.
	 */
	@Test
	public void forestFarFromAnyShoreHasNoOpenWaterRoute() throws Exception {
		Assume.assumeTrue("World_basemap_2.obf is not downloaded", new File(MAPS_DIR, "World_basemap_2.obf").exists());
		List<BinaryMapIndexReader> readers = readers(NETHERLANDS);

		BoatRoute route = route(readers, new LatLon(52.0600, 5.3700), new LatLon(52.0800, 5.4000));

		Assert.assertFalse(route.decision.toString(), route.isOpenWater());
	}
}
