package net.osmand.shared.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.router.HHJavaAccess;
import net.osmand.router.HHRouteDataStructure.HHNetworkRouteRes;
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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link net.osmand.shared.routing.HHRoutePlanner} is a copy of {@link net.osmand.router.HHRoutePlanner};
 * this runs both over the same request on a real map - the routes of the planner benchmark, with
 * and without a parameter the hub graph was built for - and compares what they find: the route
 * segment by segment, the hub-graph edges it follows and their costs, and the counters of the
 * search, which two searches taking the same steps agree on.
 *
 * The maps are not in the repository: the cases run when {@code OSMAND_OBF_DIRECTORY} holds
 * Noord-Holland, Upper Bavaria and Lower Austria, and are skipped otherwise. The routing test
 * fixture with an HH section is covered by {@link RoutePlannerCompatTest}.
 */
@RunWith(Parameterized.class)
public class HHRoutePlannerCompatTest {

	private static final String NOORD_HOLLAND = "Netherlands_noord-holland_europe.obf";
	private static final String BAVARIA = "Germany_bayern_upper-bavaria_europe.obf";
	private static final String LOWER_AUSTRIA = "Austria_lower-austria_europe.obf";

	private static int routes;
	private static int compared;

	private final String map;
	private final LatLon start;
	private final LatLon end;
	private final Map<String, String> params;

	public HHRoutePlannerCompatTest(String name, String map, LatLon start, LatLon end, Map<String, String> params) {
		this.map = map;
		this.start = start;
		this.end = end;
		this.params = params;
	}

	@AfterClass
	public static void routesWereCompared() {
		System.out.println("HHRoutePlannerCompatTest: " + routes + " routes, " + compared + " segments compared");
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
		};
		List<Map<String, String>> paramSets = new ArrayList<>();
		paramSets.add(new LinkedHashMap<>());
		Map<String, String> avoidMotorway = new LinkedHashMap<>();
		avoidMotorway.put("avoid_motorway", "true");
		paramSets.add(avoidMotorway);
		for (Object[] route : routes) {
			File file = new File(directory, (String) route[1]);
			if (!file.exists()) {
				continue;
			}
			for (Map<String, String> params : paramSets) {
				data.add(new Object[] {route[0] + (params.isEmpty() ? "" : " " + params), file.getPath(), route[2], route[3], params});
			}
		}
		return data;
	}

	@Test(timeout = 600_000)
	public void testBothPlannersAgree() throws Exception {
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
			RoutingConfiguration jconfig = RoutingConfiguration.getDefault().build("car", limits, new LinkedHashMap<>(params));
			net.osmand.shared.routing.RoutingConfiguration kconfig = net.osmand.shared.routing.RoutingConfiguration.getDefault()
					.build("car", new net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits(256, 256),
							new LinkedHashMap<>(params));

			RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
			fe.setDefaultHHRoutingConfig();
			fe.setUseOnlyHHRouting(true);
			fe.setHHRouteCpp(false);
			RoutingContext ctx = fe.buildRoutingContext(jconfig, null, new BinaryMapIndexReader[] {reader},
					RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
			ctx.leftSideNavigation = false;
			HHNetworkRouteRes jres = (HHNetworkRouteRes) fe.searchRoute(ctx, start, end, null);

			net.osmand.shared.routing.RoutePlannerFrontEnd kfe = new net.osmand.shared.routing.RoutePlannerFrontEnd();
			kfe.setDefaultHHRoutingConfig();
			kfe.setUseOnlyHHRouting(true);
			net.osmand.shared.routing.RoutingContext kctx = kfe.buildRoutingContext(kconfig, Arrays.asList(kreader), RouteCalculationMode.NORMAL);
			kctx.leftSideNavigation = false;
			// java derives these from the world regions inside searchRoute; the copy takes them from the request
			kctx.regionsCoveringStartAndTargets = ctx.regionsCoveringStartAndTargets;
			RouteCalcResult kres = kfe.searchRoute(kctx, new KLatLon(start.getLatitude(), start.getLongitude()),
					new KLatLon(end.getLatitude(), end.getLongitude()), null);
			List<net.osmand.shared.routing.RouteSegmentResult> copy = kres.getList();

			String m = f.getName() + " " + start + " -> " + end + " " + params;
			assertEquals(m + " error", jres.getError(), kres.getError());
			assertTrue(m + " is an HH result", kres instanceof net.osmand.shared.routing.HHNetworkRouteRes);
			net.osmand.shared.routing.HHNetworkRouteRes khh = (net.osmand.shared.routing.HHNetworkRouteRes) kres;
			assertEquals(m + " hub segments", jres.segments.size(), khh.segments.size());
			Same.close(m + " hh time", jres.getHHRoutingTime(), khh.getHHRoutingTime());
			Same.close(m + " hh detailed", jres.getHHRoutingDetailed(), khh.getHHRoutingDetailed());
			assertEquals(m + " stats", HHJavaAccess.stats(jres), stats(khh));
			assertEquals(m + " visited segments", ctx.getVisitedSegments(), kctx.getVisitedSegments());
			Same.close(m + " routingTime", ctx.routingTime, kctx.routingTime);
			assertEquals(m + " segments", jres.getList().size(), copy.size());
			routes++;
			compared += copy.size();
			for (int i = 0; i < copy.size(); i++) {
				RouteSegmentResult j = jres.getList().get(i);
				net.osmand.shared.routing.RouteSegmentResult k = copy.get(i);
				String s = m + " segment " + i + " road " + j.getObject().id;
				assertEquals(s + " road", j.getObject().id, k.getObject().id);
				assertEquals(s + " start", j.getStartPointIndex(), k.getStartPointIndex());
				assertEquals(s + " end", j.getEndPointIndex(), k.getEndPointIndex());
				Same.close(s + " routingTime", j.getRoutingTime(), k.getRoutingTime());
				Same.close(s + " distance", j.getDistance(), k.getDistance());
				Same.close(s + " segmentTime", j.getSegmentTime(), k.getSegmentTime());
				assertEquals(s + " turn", String.valueOf(j.getTurnType()), String.valueOf(k.getTurnType()));
			}
		} finally {
			reader.close();
			kreader.close();
		}
	}

	private static String stats(net.osmand.shared.routing.HHNetworkRouteRes route) {
		net.osmand.shared.routing.RoutingStats s = route.stats;
		if (s == null) {
			return "no stats";
		}
		return "visited " + s.visitedVertices + " unique " + s.uniqueVisitedVertices + " added " + s.addedVertices
				+ " firstMet " + s.firstRouteVisitedVertices + " edges " + s.loadEdgesCnt;
	}
}
