package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.RoutingContext;
import net.osmand.router.TestEntry;
import net.osmand.shared.data.KLatLon;
import net.osmand.shared.routing.RouteCalcResult;
import net.osmand.shared.routing.RouteCalculationMode;
import net.osmand.shared.routing.TurnType;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The OsmAnd-shared planner - reader, routing context, A* search and result preparation - is a
 * copy of the java one; this runs both over the same request and compares the routes they find.
 *
 * The requests are the routing tests' - the turn lane cases over {@code Turn_lanes_test.obf} and
 * the routing cases over the routing archive, in all three road directions - and both sides go the
 * whole way: their own reader over the same obf files, their own tile cache, search and
 * preparation. What comes out is compared segment by segment: the road, the stretch of it, the
 * routing and driving times, the distance and the manoeuvre; and the number of segments the search
 * settled, which two searches taking the same steps agree on. A request one side rejects the other
 * has to reject the same way. A case that asks for HH routing runs the HH search on both sides,
 * over the hub graph of its map, with the same last-mile and detailed searches underneath.
 */
@RunWith(Parameterized.class)
public class RoutePlannerCompatTest {

	private static final String RESOURCES = "src/test/resources/";

	private static int routes;
	private static int compared;

	private final TestEntry entry;
	private final List<String> maps;
	private final boolean lanes;

	public RoutePlannerCompatTest(String name, TestEntry entry, List<String> maps, boolean lanes) {
		this.entry = entry;
		this.maps = maps;
		this.lanes = lanes;
	}

	@AfterClass
	public static void routesWereCompared() {
		System.out.println("RoutePlannerCompatTest: " + routes + " routes, " + compared + " segments compared");
		assertTrue("routes compared: " + routes, routes > 300);
		assertTrue("segments compared: " + compared, compared > 1000);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() throws IOException {
		List<Object[]> data = new ArrayList<>();
		for (TestEntry te : entries("/test_turn_lanes.json")) {
			data.add(new Object[] {"lanes: " + te.getTestName(), te, Arrays.asList(RESOURCES + "Turn_lanes_test.obf"), true});
		}
		for (TestEntry te : entries("/test_routing.json")) {
			Map<String, String> params = te.getParams();
			List<String> maps = new ArrayList<>();
			if (params != null && params.containsKey("map")) {
				maps.add(RESOURCES + "routing/" + params.get("map"));
			}
			maps.add(RESOURCES + "routing/Routing_test_archive.obf");
			data.add(new Object[] {"routing: " + te.getTestName(), te, maps, false});
		}
		return data;
	}

	private static List<TestEntry> entries(String resource) throws IOException {
		List<TestEntry> entries = new ArrayList<>();
		try (Reader reader = new InputStreamReader(Objects.requireNonNull(RoutePlannerCompatTest.class.getResourceAsStream(resource)))) {
			Gson gson = new GsonBuilder().create();
			for (TestEntry te : gson.fromJson(reader, TestEntry[].class)) {
				if (!te.isIgnore()) {
					entries.add(te);
				}
			}
		}
		return entries;
	}

	@Test(timeout = 300_000)
	public void testBothPlannersAgree() throws Exception {
		Map<String, String> params = new LinkedHashMap<>();
		if (entry.getParams() != null) {
			params.putAll(entry.getParams());
		}
		if (lanes) {
			params.put("car", "true");
		}
		String vehicle = lanes ? "car" : params.getOrDefault("vehicle", "car");
		RoutingMemoryLimits limits = new RoutingMemoryLimits(RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
				RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT);
		RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false;
		net.osmand.shared.routing.RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION = false;

		BinaryMapIndexReader[] readers = new BinaryMapIndexReader[maps.size()];
		List<net.osmand.shared.binary.BinaryMapIndexReader> kreaders = new ArrayList<>();
		for (int i = 0; i < maps.size(); i++) {
			File f = new File(maps.get(i));
			readers[i] = new BinaryMapIndexReader(new java.io.RandomAccessFile(f, "r"), f);
			kreaders.add(new net.osmand.shared.binary.BinaryMapIndexReader(f.getPath()));
		}
		try {
			for (int planRoadDirection = -1; planRoadDirection <= 1; planRoadDirection++) {
				if (lanes && planRoadDirection != 0) {
					continue;
				}
				if (params.containsKey("wrongPlanRoadDirection") && params.get("wrongPlanRoadDirection").equals(planRoadDirection + "")) {
					continue;
				}
				RoutingConfiguration jconfig = RoutingConfiguration.getDefault().build(vehicle, limits, new HashMap<>(params));
				net.osmand.shared.routing.RoutingConfiguration kconfig = net.osmand.shared.routing.RoutingConfiguration.getDefault()
						.build(vehicle, new net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits(
								RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3, RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT),
								new LinkedHashMap<>(params));
				if (params.containsKey("routeCalculationTime")) {
					jconfig.routeCalculationTime = Long.parseLong(params.get("routeCalculationTime"));
					kconfig.routeCalculationTime = jconfig.routeCalculationTime;
				}
				if (params.containsKey("heuristicCoefficient")) {
					jconfig.heuristicCoefficient = Float.parseFloat(params.get("heuristicCoefficient"));
					kconfig.heuristicCoefficient = jconfig.heuristicCoefficient;
				}
				jconfig.planRoadDirection = planRoadDirection;
				kconfig.planRoadDirection = planRoadDirection;

				RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
				RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false;
				RoutingContext ctx = fe.buildRoutingContext(jconfig, null, readers, RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
				ctx.leftSideNavigation = false;

				net.osmand.shared.routing.RoutePlannerFrontEnd kfe = new net.osmand.shared.routing.RoutePlannerFrontEnd();
				net.osmand.shared.routing.RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = false;
				net.osmand.shared.routing.RoutingContext kctx = kfe.buildRoutingContext(kconfig, kreaders, RouteCalculationMode.NORMAL);
				kctx.leftSideNavigation = false;
				if ("true".equals(params.get("hh"))) {
					fe.setDefaultHHRoutingConfig();
					fe.setUseOnlyHHRouting(true);
					fe.setHHRouteCpp(false);
					kfe.setDefaultHHRoutingConfig();
					kfe.setUseOnlyHHRouting(true);
				}

				String m = entry.getTestName() + " direction " + planRoadDirection;
				List<RouteSegmentResult> java;
				List<net.osmand.shared.routing.RouteSegmentResult> copy;
				try {
					java = fe.searchRoute(ctx, entry.getStartPoint(), entry.getEndPoint(), entry.getTransitPoint()).getList();
				} catch (RuntimeException e) {
					// the copy has to fail the same way, and there is nothing else to compare
					try {
						kfe.searchRoute(kctx, latLon(entry.getStartPoint()), latLon(entry.getEndPoint()), latLons(entry.getTransitPoint()));
						assertEquals(m + " java threw, the copy did not", e.toString(), "no exception");
					} catch (RuntimeException k) {
						assertEquals(m + " exception", e.getClass().getSimpleName() + ": " + e.getMessage(),
								k.getClass().getSimpleName() + ": " + k.getMessage());
					}
					continue;
				}
				RouteCalcResult kres = kfe.searchRoute(kctx, latLon(entry.getStartPoint()), latLon(entry.getEndPoint()), latLons(entry.getTransitPoint()));
				copy = kres.getList();

				assertEquals(m + " visited segments", ctx.getVisitedSegments(), kctx.getVisitedSegments());
				assertEquals(m + " loaded tiles", ctx.getLoadedTiles(), kctx.getLoadedTiles());
				Same.close(m + " routingTime", ctx.routingTime, kctx.routingTime);
				assertEquals(m + " segments", java.size(), copy.size());
				routes++;
				compared += java.size();
				for (int i = 0; i < java.size(); i++) {
					RouteSegmentResult j = java.get(i);
					net.osmand.shared.routing.RouteSegmentResult k = copy.get(i);
					Same.segment(m + " segment " + i + " road " + j.getObject().id, j, k);
				}
			}
		} finally {
			for (BinaryMapIndexReader reader : readers) {
				reader.close();
			}
			for (net.osmand.shared.binary.BinaryMapIndexReader reader : kreaders) {
				reader.close();
			}
		}
	}

	private static KLatLon latLon(LatLon l) {
		return new KLatLon(l.getLatitude(), l.getLongitude());
	}

	private static List<KLatLon> latLons(List<LatLon> list) {
		List<KLatLon> res = new ArrayList<>();
		for (LatLon l : list) {
			res.add(latLon(l));
		}
		return res;
	}
}
