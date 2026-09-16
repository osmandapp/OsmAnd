package net.osmand.router;

import org.junit.Assert;
import org.junit.Test;

import net.osmand.data.LatLon;
import net.osmand.router.SeaRoutePlanner.SeaRoute;
import net.osmand.router.SeaRoutePlanner.SeaRoutingConfig;

/**
 * Open water routing on hand-built shores (OsmAnd-Issues #3170, OsmAnd#2586).
 *
 * Coordinates are metres east and north of {@link #ORIGIN}, so a case can be read as a sketch. Shores
 * follow the OSM convention the loader relies on: walking the way, land is on the left, so an island
 * ring runs counter-clockwise.
 */
public class SeaRoutePlannerTest {

	private static final LatLon ORIGIN = new LatLon(35.9, 14.5);
	private static final double METERS_PER_DEGREE_LAT = SeaObstacles.METERS_PER_DEGREE_LAT;
	private static final double METERS_PER_DEGREE_LON = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(35.9));

	private static LatLon at(double east, double north) {
		return new LatLon(ORIGIN.getLatitude() + north / METERS_PER_DEGREE_LAT,
				ORIGIN.getLongitude() + east / METERS_PER_DEGREE_LON);
	}

	/** Square island with land inside, given counter-clockwise as OSM keeps a coastline. */
	private static double[] island(double east, double north, double size) {
		double[] corners = { east, north, east + size, north, east + size, north + size, east, north + size, east, north };
		double[] latLon = new double[corners.length];
		for (int i = 0; i < corners.length; i += 2) {
			LatLon p = at(corners[i], corners[i + 1]);
			latLon[i] = p.getLatitude();
			latLon[i + 1] = p.getLongitude();
		}
		return latLon;
	}

	private static SeaObstacles obstacles(double[]... coastlines) {
		SeaObstacles obstacles = new SeaObstacles(ORIGIN.getLatitude());
		for (double[] coastline : coastlines) {
			obstacles.addCoastline(coastline);
		}
		obstacles.build();
		return obstacles;
	}

	private static SeaRoutePlanner planner(double cornerClearance, double minClearance) {
		SeaRoutingConfig config = new SeaRoutingConfig();
		config.cornerClearance = cornerClearance;
		config.minClearance = minClearance;
		config.simplifyTolerance = 10;
		return new SeaRoutePlanner(config);
	}

	private static double distance(LatLon a, LatLon b) {
		double dx = (b.getLongitude() - a.getLongitude()) * METERS_PER_DEGREE_LON;
		double dy = (b.getLatitude() - a.getLatitude()) * METERS_PER_DEGREE_LAT;
		return Math.hypot(dx, dy);
	}

	@Test
	public void openWaterIsOneStraightLeg() {
		SeaObstacles obstacles = obstacles(island(20000, 20000, 1000));
		LatLon start = at(-2000, 500), end = at(3000, 500);
		SeaRoute route = planner(80, 40).plan(obstacles, start, end);

		Assert.assertNotNull(route);
		Assert.assertEquals(1, route.getLegs());
		Assert.assertEquals(distance(start, end), route.distance, 1);
		Assert.assertEquals(1, route.visibilityChecks); // the direct leg is tried first and works
	}

	@Test
	public void routeBendsAroundAnIslandInTheWay() {
		SeaObstacles obstacles = obstacles(island(0, 0, 1000));
		LatLon start = at(-2000, 500), end = at(3000, 500);
		SeaRoutePlanner planner = planner(80, 40);

		SeaRoute route = planner.plan(obstacles, start, end);

		Assert.assertNotNull(route);
		Assert.assertEquals(3, route.getLegs()); // start, two corners of the island, end
		Assert.assertFalse(planner.crossesShore(obstacles, route));
		Assert.assertTrue("route must be longer than the blocked straight line",
				route.distance > distance(start, end));
		Assert.assertTrue("detour of a 1 km island should stay well under a kilometre",
				route.distance < distance(start, end) + 1000);
	}

	@Test
	public void legsKeepTheConfiguredDistanceFromShore() {
		SeaObstacles obstacles = obstacles(island(0, 0, 1000));
		SeaRoutePlanner planner = planner(120, 60);

		SeaRoute route = planner.plan(obstacles, at(-2000, 500), at(3000, 500));

		Assert.assertNotNull(route);
		Assert.assertTrue("clearance " + planner.measureClearance(obstacles, route),
				planner.measureClearance(obstacles, route) >= 60);
	}

	@Test
	public void narrowGapIsUsedOnlyWhenTheBoatFits() {
		// two islands 60 m apart, the straight line runs through the gap
		SeaObstacles obstacles = obstacles(island(0, 0, 1000), island(0, 1060, 1000));
		LatLon start = at(-2000, 1030), end = at(3000, 1030);

		SeaRoutePlanner wideBoat = planner(80, 40);
		SeaRoute through = planner(15, 10).plan(obstacles, start, end);
		SeaRoute around = wideBoat.plan(obstacles, start, end);

		Assert.assertNotNull(through);
		Assert.assertEquals("a 60 m gap fits a 10 m clearance", 1, through.getLegs());
		Assert.assertNotNull(around);
		Assert.assertTrue("a 40 m clearance needs 80 m of gap, so the route goes around: "
				+ around.distance + " vs " + through.distance, around.distance > through.distance + 300);
		Assert.assertTrue("clearance " + wideBoat.measureClearance(obstacles, around),
				wideBoat.measureClearance(obstacles, around) >= 40);
	}

	@Test
	public void landAndWaterAreToldApartByTheSideOfTheShore() {
		SeaObstacles obstacles = obstacles(island(0, 0, 1000));

		Assert.assertTrue(obstacles.isLand(at(500, 500)));   // inside the island
		Assert.assertFalse(obstacles.isLand(at(-500, 500))); // west of it
		Assert.assertFalse(obstacles.isLand(at(50000, 0)));  // no shore anywhere near
		Assert.assertEquals(1, obstacles.getClosedRings());
		Assert.assertEquals(1, obstacles.getCoastlineOrientation());
	}

	@Test
	public void waterAreaRingHasLandOutsideIt() {
		// a lake: the same counter-clockwise ring, but water is what lies inside
		SeaObstacles obstacles = new SeaObstacles(ORIGIN.getLatitude());
		obstacles.addWaterAreaRing(island(0, 0, 4000));
		obstacles.build();

		Assert.assertFalse("inside the lake", obstacles.isLand(at(2000, 2000)));
		Assert.assertTrue("outside the lake", obstacles.isLand(at(-200, 2000)));
	}

	@Test
	public void routeCrossesALakeWithoutTouchingItsShore() {
		SeaObstacles obstacles = new SeaObstacles(ORIGIN.getLatitude());
		obstacles.addWaterAreaRing(island(0, 0, 4000));
		obstacles.addCoastline(island(1500, 1500, 1000)); // an island in the lake
		obstacles.build();
		SeaRoutePlanner planner = planner(80, 40);

		SeaRoute route = planner.plan(obstacles, at(500, 2000), at(3500, 2000));

		Assert.assertNotNull(route);
		Assert.assertTrue(route.getLegs() > 1); // the island is in the way
		Assert.assertFalse(planner.crossesShore(obstacles, route));
		Assert.assertTrue(planner.measureClearance(obstacles, route) >= 40);
	}

	@Test
	public void endpointOnLandIsMovedOntoWater() {
		SeaObstacles obstacles = obstacles(island(0, 0, 1000));
		SeaRoutePlanner planner = planner(80, 40);
		LatLon berth = at(900, 500); // 100 m inside the island, as a berth mapped behind the shore

		LatLon water = obstacles.snapToWater(berth, 40, 3000);

		Assert.assertNotNull(water);
		Assert.assertFalse(obstacles.isLand(water));
		Assert.assertTrue(distance(berth, water) <= 3000);

		SeaRoute route = planner.plan(obstacles, berth, at(3000, 500));
		Assert.assertNotNull(route);
		Assert.assertNotNull("the start was reported as moved", route.snappedStart);
		Assert.assertFalse(planner.crossesShore(obstacles, route));
	}

	@Test
	public void noRouteWhenTheDestinationIsLockedInland() {
		// a lake with no outlet, 20 km away from the destination: nothing can reach it
		SeaObstacles obstacles = new SeaObstacles(ORIGIN.getLatitude());
		obstacles.addWaterAreaRing(island(0, 0, 500));
		obstacles.build();

		SeaRoute route = planner(80, 40).plan(obstacles, at(250, 250), at(20000, 20000));

		Assert.assertNull(route);
	}

	@Test
	public void farFromAnyShoreTheBasemapTellsLandFromSea() {
		SeaObstacles obstacles = obstacles(island(0, 0, 1000));
		LatLon inland = at(50000, 50000);

		Assert.assertFalse("without land tiles a point far from any shore is open sea", obstacles.isLand(inland));

		obstacles.setFarFromShore((lat, lon) -> true);
		Assert.assertTrue(obstacles.isLand(inland));
		Assert.assertFalse("near a shore its side still decides", obstacles.isLand(at(-500, 500)));
		Assert.assertNull("no water to start from", planner(80, 40).plan(obstacles, inland, at(50500, 50000)));
	}

	@Test
	public void berthNextToAnEnclosedDockStillReachesTheSea() {
		// a 2 km island with a closed dock inside: the berth is 50 m from the dock and 650 m from the sea,
		// so the nearest water is the one a boat cannot leave - the Vlissingen docks behind their locks
		SeaObstacles obstacles = new SeaObstacles(ORIGIN.getLatitude());
		obstacles.addCoastline(island(0, 0, 2000));
		obstacles.addWaterAreaRing(island(1000, 900, 300));
		obstacles.build();
		SeaRoutePlanner planner = planner(80, 40);
		LatLon berth = at(1350, 1050);

		Assert.assertTrue(obstacles.isLand(berth));
		SeaRoute route = planner.plan(obstacles, berth, at(3000, 1050));

		Assert.assertNotNull("the dock is the nearest water, but the sea is reachable a little farther", route);
		Assert.assertTrue("attempts " + route.attempts, route.attempts > 1);
		Assert.assertFalse(obstacles.isLand(route.snappedStart));
		Assert.assertFalse(planner.crossesShore(obstacles, route));
	}
}
