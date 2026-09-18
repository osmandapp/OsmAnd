package net.osmand.router;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.router.HHRouteDataStructure.HHNetworkRouteRes;
import net.osmand.router.HHRouteDataStructure.HHNetworkSegmentRes;
import net.osmand.router.HHRouteDataStructure.HHRoutingConfig;
import net.osmand.router.HHRouteDataStructure.HHRoutingContext;
import net.osmand.router.HHRouteDataStructure.NetworkDBPoint;
import net.osmand.router.HHRouteDataStructure.NetworkDBSegment;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The hub-graph edges a long route drops before its detailed phase come back with the costs the
 * roads corrected, so a recalculation converges the way it does with the edges kept.
 *
 * A route over a real map (the routing test fixture with an HH section is a cutout too small for
 * a hub edge), the first of the cases whose map is in {@code OSMAND_OBF_DIRECTORY}, skipped when
 * none is. The hub graph agrees with its roads, so the test ages it by hand: it halves the cost of
 * every hub edge of the route, as a map update would have, and routes again. With the edges
 * dropped and the corrections lost, the search finds the same wrong shortcuts on every pass and
 * spends all its recalculations - which the count catches.
 */
public class HHRecalculationTest {

	private static final Object[][] CASES = {
			{"Germany_bayern_lower-franconia_europe.obf", new LatLon(49.7913, 9.9534), new LatLon(50.0444, 10.2322)},
			{"Germany_bayern_upper-bavaria_europe.obf", new LatLon(48.1374, 11.5755), new LatLon(47.4917, 11.0954)},
			{"Netherlands_noord-holland_europe.obf", new LatLon(52.3791, 4.9003), new LatLon(52.9563, 4.7606)},
			{"Austria_lower-austria_europe.obf", new LatLon(48.2047, 15.6256), new LatLon(47.8100, 16.2450)}
	};

	private LatLon start;
	private LatLon end;

	@Test
	public void droppedEdgesKeepTheirCorrectedCosts() throws Exception {
		String directory = System.getenv("OSMAND_OBF_DIRECTORY");
		File map = null;
		for (Object[] c : CASES) {
			File f = directory == null ? null : new File(directory, (String) c[0]);
			if (f != null && f.exists()) {
				map = f;
				start = (LatLon) c[1];
				end = (LatLon) c[2];
				break;
			}
		}
		Assume.assumeTrue("none of the maps of the cases in OSMAND_OBF_DIRECTORY", map != null);

		int threshold = HHRoutePlanner.FREE_EDGES_SETTLED_POINTS;
		boolean collect = HHRoutePlanner.COLLECT_GARBAGE_AFTER_FREEING_EDGES;
		BinaryMapIndexReader reader = new BinaryMapIndexReader(new RandomAccessFile(map, "r"), map);
		try {
			RoutingConfiguration config = RoutingConfiguration.getDefault().build("car",
					new RoutingMemoryLimits(RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
							RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT), new LinkedHashMap<String, String>());
			RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
			RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false;
			HHRoutingConfig hhConfig = HHRoutingConfig.astar(0).calcDetailed(2).cacheContext(null);
			fe.setHHRoutingConfig(hhConfig);
			fe.setUseOnlyHHRouting(true);
			RoutingContext ctx = fe.buildRoutingContext(config, null, new BinaryMapIndexReader[] {reader},
					RouteCalculationMode.NORMAL);

			HHRoutePlanner.FREE_EDGES_SETTLED_POINTS = Integer.MAX_VALUE;
			Outcome fresh = route(fe, ctx);
			Assert.assertFalse("the route has to run over the hub graph", fresh.hubEdges.isEmpty());
			Assert.assertEquals("a hub graph that agrees with the roads", 0, fresh.recalculations);

			age(hhConfig, fresh);
			Outcome kept = route(fe, ctx);
			Assert.assertTrue("an aged hub graph has to make the planner recalculate", kept.recalculations > 0);
			Assert.assertEquals("the roads win over the aged hub graph", fresh.roads, kept.roads);

			HHRoutePlanner.FREE_EDGES_SETTLED_POINTS = 0;
			for (boolean gc : new boolean[] {false, true}) {
				HHRoutePlanner.COLLECT_GARBAGE_AFTER_FREEING_EDGES = gc;
				age(hhConfig, fresh);
				Outcome dropped = route(fe, ctx);
				Assert.assertEquals("recalculations with the edges dropped (gc=" + gc + ")",
						kept.recalculations, dropped.recalculations);
				Assert.assertEquals("route with the edges dropped (gc=" + gc + ")", fresh.roads, dropped.roads);
			}
		} finally {
			HHRoutePlanner.FREE_EDGES_SETTLED_POINTS = threshold;
			HHRoutePlanner.COLLECT_GARBAGE_AFTER_FREEING_EDGES = collect;
			reader.close();
		}
	}

	private static class Outcome {
		final List<String> roads;
		final List<NetworkDBSegment> hubEdges;
		final int recalculations;

		Outcome(List<String> roads, List<NetworkDBSegment> hubEdges, int recalculations) {
			this.roads = roads;
			this.hubEdges = hubEdges;
			this.recalculations = recalculations;
		}
	}

	private Outcome route(RoutePlannerFrontEnd fe, RoutingContext ctx) throws Exception {
		HHNetworkRouteRes res = (HHNetworkRouteRes) fe.searchRoute(ctx, start, end, null);
		Assert.assertTrue("route: " + res.getError(), res.isCorrect());
		List<String> roads = new ArrayList<>();
		for (RouteSegmentResult r : res.detailed) {
			roads.add(r.getObject().getId() + ":" + r.getStartPointIndex() + "-" + r.getEndPointIndex());
		}
		List<NetworkDBSegment> hubEdges = new ArrayList<>();
		for (HHNetworkSegmentRes s : res.segments) {
			if (s.segment != null) {
				hubEdges.add(s.segment);
			}
		}
		return new Outcome(roads, hubEdges, res.stats.recalculations);
	}

	/** Halves the cost of the route's hub edges in the cached context, the way a map update leaves shortcuts behind the roads. */
	private void age(HHRoutingConfig hhConfig, Outcome fresh) {
		HHRoutingContext<NetworkDBPoint> hctx = (HHRoutingContext<NetworkDBPoint>) hhConfig.cacheCtx;
		for (NetworkDBSegment edge : fresh.hubEdges) {
			NetworkDBSegment loaded = edge.direction ? edge.start.getSegment(edge.end, true)
					: edge.end.getSegment(edge.start, false);
			if (loaded == null) {
				loaded = edge;
			}
			hctx.setEdgeCost(loaded, loaded.dist / 2);
		}
	}
}
