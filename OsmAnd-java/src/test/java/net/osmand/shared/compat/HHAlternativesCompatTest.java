package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.router.HHRouteDataStructure.HHRoutingConfig;
import net.osmand.router.HHRoutePlanner;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.RoutingContext;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.routing.RouteCalcResult;
import net.osmand.shared.routing.RouteCalculationMode;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * {@link net.osmand.shared.routing.HHAlternativeRoutes} is a copy of {@link net.osmand.router.HHAlternativeRoutes};
 * this asks both planners for alternatives on the same requests and compares what they propose:
 * how many, and each one segment by segment. The long routes of the planner benchmark find theirs
 * on the hub graph; the city hops of a few kilometres, where the two last-mile searches meet
 * before either reaches the hub graph, find theirs on the detailed road trees.
 *
 * The maps are not in the repository: the cases run when {@code OSMAND_OBF_DIRECTORY} holds
 * Noord-Holland, Upper Bavaria and Lower Austria, and are skipped otherwise.
 */
@RunWith(Parameterized.class)
public class HHAlternativesCompatTest {

	private static final String NOORD_HOLLAND = "Netherlands_noord-holland_europe.obf";
	private static final String BAVARIA = "Germany_bayern_upper-bavaria_europe.obf";
	private static final String LOWER_AUSTRIA = "Austria_lower-austria_europe.obf";

	private static int routes;
	private static int alternatives;

	private final String map;
	private final LatLon start;
	private final LatLon end;

	public HHAlternativesCompatTest(String name, String map, LatLon start, LatLon end) {
		this.map = map;
		this.start = start;
		this.end = end;
	}

	@AfterClass
	public static void routesWereCompared() {
		System.out.println("HHAlternativesCompatTest: " + routes + " routes, " + alternatives + " alternatives compared");
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() {
		String directory = System.getenv("OSMAND_OBF_DIRECTORY");
		List<Object[]> data = new ArrayList<>();
		if (directory == null) {
			return data;
		}
		Object[][] routes = {
				{"amsterdam schiphol 17 km", NOORD_HOLLAND, new LatLon(52.3791, 4.9003), new LatLon(52.3105, 4.7683)},
				{"amsterdam haarlem 20 km", NOORD_HOLLAND, new LatLon(52.3791, 4.9003), new LatLon(52.3874, 4.6462)},
				{"amsterdam den helder 80 km", NOORD_HOLLAND, new LatLon(52.3791, 4.9003), new LatLon(52.9563, 4.7606)},
				{"munich airport 35 km", BAVARIA, new LatLon(48.1374, 11.5755), new LatLon(48.3538, 11.7861)},
				{"munich rosenheim 65 km", BAVARIA, new LatLon(48.1374, 11.5755), new LatLon(47.8561, 12.1289)},
				{"munich garmisch 90 km", BAVARIA, new LatLon(48.1374, 11.5755), new LatLon(47.4917, 11.0954)},
				{"vienna schwechat 20 km", LOWER_AUSTRIA, new LatLon(48.2082, 16.3738), new LatLon(48.1103, 16.5697)},
				{"st poelten krems 30 km", LOWER_AUSTRIA, new LatLon(48.2047, 15.6256), new LatLon(48.4103, 15.6136)},
				{"st poelten wr neustadt 70 km", LOWER_AUSTRIA, new LatLon(48.2047, 15.6256), new LatLon(47.8100, 16.2450)},
				// the Amstelveen case of alternatives/test_alternative_routes.json, and hops of the same kind
				{"amstelveen fluweelboomlaan westwijk 4 km", NOORD_HOLLAND, new LatLon(52.286065, 4.866343), new LatLon(52.28491, 4.823599)},
				{"amsterdam centraal museumplein 3 km", NOORD_HOLLAND, new LatLon(52.3791, 4.9003), new LatLon(52.3580, 4.8811)},
				{"munich hauptbahnhof ostbahnhof 4 km", BAVARIA, new LatLon(48.1402, 11.5600), new LatLon(48.1270, 11.6040)},
				{"vienna westbahnhof prater 6 km", LOWER_AUSTRIA, new LatLon(48.1965, 16.3383), new LatLon(48.2167, 16.3960)},
		};
		for (Object[] route : routes) {
			File file = new File(directory, (String) route[1]);
			if (file.exists()) {
				data.add(new Object[] {route[0], file.getPath(), route[2], route[3]});
			}
		}
		return data;
	}

	@Test(timeout = 600_000)
	public void testBothPlannersProposeTheSame() throws Exception {
		Assume.assumeTrue("OSMAND_OBF_DIRECTORY is not set", map != null);
		RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false;
		net.osmand.shared.routing.RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false;
		RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false;
		net.osmand.shared.routing.RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false;
		RoutingMemoryLimits limits = new RoutingMemoryLimits(256, 256);

		File f = new File(map);
		BinaryMapIndexReader reader = new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f);
		net.osmand.shared.binary.BinaryMapIndexReader kreader = new net.osmand.shared.binary.BinaryMapIndexReader(f.getPath());
		try {
			RoutingConfiguration jconfig = RoutingConfiguration.getDefault().build("car", limits, new LinkedHashMap<>());
			net.osmand.shared.routing.RoutingConfiguration kconfig = net.osmand.shared.routing.RoutingConfiguration.getDefault()
					.build("car", new net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits(256, 256), new LinkedHashMap<>());

			RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
			HHRoutingConfig hh = HHRoutePlanner.prepareDefaultRoutingConfig(null);
			hh.calcAlternative();
			fe.setUseOnlyHHRouting(true).setHHRoutingConfig(hh);
			fe.setHHRouteCpp(false);
			RoutingContext ctx = fe.buildRoutingContext(jconfig, null, new BinaryMapIndexReader[] {reader},
					RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
			ctx.leftSideNavigation = false;
			RouteResultPreparation.RouteCalcResult jres = fe.searchRoute(ctx, start, end, null);

			net.osmand.shared.routing.RoutePlannerFrontEnd kfe = new net.osmand.shared.routing.RoutePlannerFrontEnd();
			net.osmand.shared.routing.HHRoutingConfig khh = net.osmand.shared.routing.HHRoutePlanner.prepareDefaultRoutingConfig(null);
			khh.calcAlternative();
			kfe.setUseOnlyHHRouting(true).setHHRoutingConfig(khh);
			net.osmand.shared.routing.RoutingContext kctx = kfe.buildRoutingContext(kconfig, Arrays.asList(kreader), RouteCalculationMode.NORMAL);
			kctx.leftSideNavigation = false;
			kctx.regionsCoveringStartAndTargets = ctx.regionsCoveringStartAndTargets;
			RouteCalcResult kres = kfe.searchRoute(kctx, new KLatLon(start.getLatitude(), start.getLongitude()),
					new KLatLon(end.getLatitude(), end.getLongitude()), null);

			String m = f.getName() + " " + start + " -> " + end;
			assertEquals(m + " error", jres.getError(), kres.getError());
			assertRoute(m + " main", jres.getList(), kres.getList(), false);
			List<List<RouteSegmentResult>> jalts = jres.getAlternatives();
			List<List<net.osmand.shared.routing.RouteSegmentResult>> kalts = kres.getAlternatives();
			assertEquals(m + " alternatives", jalts.size(), kalts.size());
			for (int a = 0; a < jalts.size(); a++) {
				assertRoute(m + " alternative " + a, jalts.get(a), kalts.get(a), true);
				alternatives++;
			}
			routes++;
		} finally {
			reader.close();
			kreader.close();
		}
	}

	/**
	 * The roads, the stretches of them and the turns have to be the same. So do the times, except
	 * on an alternative from the detailed trees: when several road points of one plateau tie on
	 * cost, java keeps whichever its hash map yields first and the copy the first inserted, and the
	 * route assembled through either is the same roads with the cost of the junction summed at a
	 * different point - the last float digits of one segment's time differ, nothing else.
	 */
	private static void assertRoute(String m, List<RouteSegmentResult> java, List<net.osmand.shared.routing.RouteSegmentResult> copy,
			boolean alternative) {
		assertEquals(m + " segments", java.size(), copy.size());
		for (int i = 0; i < java.size(); i++) {
			RouteSegmentResult j = java.get(i);
			net.osmand.shared.routing.RouteSegmentResult k = copy.get(i);
			String s = m + " segment " + i + " road " + j.getObject().id;
			assertEquals(s + " road", j.getObject().id, k.getObject().id);
			assertEquals(s + " start", j.getStartPointIndex(), k.getStartPointIndex());
			assertEquals(s + " end", j.getEndPointIndex(), k.getEndPointIndex());
			if (alternative) {
				if (Math.abs(j.getRoutingTime() - k.getRoutingTime()) > 1e-5) {
					System.out.println(s + " of " + java.size() + " routingTime " + j.getRoutingTime() + " vs " + k.getRoutingTime());
				}
				assertEquals(s + " routingTime", j.getRoutingTime(), k.getRoutingTime(), 0.01);
			} else {
				Same.close(s + " routingTime", j.getRoutingTime(), k.getRoutingTime());
			}
			Same.close(s + " distance", j.getDistance(), k.getDistance());
			assertEquals(s + " turn", String.valueOf(j.getTurnType()), String.valueOf(k.getTurnType()));
		}
	}
}
