package net.osmand.shared.compat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.RoutingContext;
import net.osmand.router.TestEntry;
import net.osmand.shared.routing.RouteCalculationMode;
import net.osmand.shared.routing.RoutingRequest;
import net.osmand.shared.routing.TurnPreparation;
import net.osmand.shared.routing.TurnType;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link TurnPreparation} is a copy of the turn and time preparation in {@link RouteResultPreparation};
 * this runs both over the same route and compares the manoeuvres, lanes, times and speeds they give
 * every segment.
 *
 * The routes are the ones the routing tests use - the turn lane cases over {@code Turn_lanes_test.obf}
 * and the routing cases over the routing archive - found by the java planner and handed to both
 * preparations raw. What comes before the shared part stays java on both sides: the area routing
 * combine and the split with the attached roads need the routing context's tiles, and they are run
 * once through the java private methods on the copy's input, which is then converted road for road.
 *
 * Descriptions are not compared: java's {@code addTurnInfoDescriptions} formats text nothing in the
 * apps reads, and the copy does not carry it.
 */
@RunWith(Parameterized.class)
public class TurnPreparationCompatTest {

	private static final String RESOURCES = "src/test/resources/";

	private static int compared;
	private static int turns;

	private final TestEntry entry;
	private final List<String> maps;
	private final boolean lanes;

	public TurnPreparationCompatTest(String name, TestEntry entry, List<String> maps, boolean lanes) {
		this.entry = entry;
		this.maps = maps;
		this.lanes = lanes;
	}

	@AfterClass
	public static void routesWereCompared() {
		// the fixtures are short routes on small maps; what matters is that they routed and carried turns
		System.out.println("TurnPreparationCompatTest: " + compared + " segments, " + turns + " turns compared");
		assertEquals("segments compared: " + compared, true, compared > 1000);
		assertEquals("turns compared: " + turns, true, turns > 300);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static List<Object[]> data() throws IOException {
		List<Object[]> data = new ArrayList<>();
		for (TestEntry te : entries("/test_turn_lanes.json")) {
			data.add(new Object[] {"lanes: " + te.getTestName(), te, Arrays.asList(RESOURCES + "Turn_lanes_test.obf"), true});
		}
		for (TestEntry te : entries("/test_routing.json")) {
			List<String> maps = new ArrayList<>();
			Map<String, String> params = te.getParams();
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
		try (Reader reader = new InputStreamReader(Objects.requireNonNull(TurnPreparationCompatTest.class.getResourceAsStream(resource)))) {
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
	public void testBothPreparationsAgree() throws Exception {
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

		BinaryMapIndexReader[] readers = new BinaryMapIndexReader[maps.size()];
		for (int i = 0; i < maps.size(); i++) {
			File f = new File(maps.get(i));
			readers[i] = new BinaryMapIndexReader(new java.io.RandomAccessFile(f, "r"), f);
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
				fe.CALCULATE_MISSING_MAPS = false;
				if ("true".equals(params.get("hh"))) {
					fe.setDefaultHHRoutingConfig();
					fe.setUseOnlyHHRouting(true);
				}
				RoutingContext ctx = fe.buildRoutingContext(jconfig, null, readers, RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
				ctx.leftSideNavigation = false;
				ctx.requestNativePrepareResult = true;
				List<RouteSegmentResult> raw = fe.searchRoute(ctx, entry.getStartPoint(), entry.getEndPoint(), entry.getTransitPoint()).getList();
				if (raw == null || raw.isEmpty()) {
					// the routing tests without expected results are the ones about hanging, not about a route
					assertEquals(entry.getTestName() + " found no route", true, !lanes && entry.getExpectedResults() == null);
					continue;
				}
				List<RouteSegmentResult> forJava = copyRaw(raw);
				List<RouteSegmentResult> forCopy = copyRaw(raw);

				// the java-only steps, on the copy's input
				RouteResultPreparation rrp = new RouteResultPreparation();
				Method initRegion = privateMethod("checkAndInitRouteRegion", RoutingContext.class, net.osmand.binary.RouteDataObject.class);
				for (RouteSegmentResult s : forCopy) {
					initRegion.invoke(rrp, ctx, s.getObject());
					if (s.getObject().region != null) {
						s.getObject().region.findOrCreateRouteType(RoutingConfiguration.DirectionPoint.TAG, RoutingConfiguration.DirectionPoint.DELETE_TYPE);
					}
				}
				privateMethod("combineWayPointsForAreaRouting", RoutingContext.class, List.class).invoke(rrp, ctx, forCopy);
				rrp.validateAllPointsConnected(forCopy);
				privateMethod("splitRoadsAndAttachRoadSegments", RoutingContext.class, List.class).invoke(rrp, ctx, forCopy);
				List<net.osmand.shared.routing.RouteSegmentResult> copy = new JavaToShared().segments(forCopy);

				// the shared steps, on the copy
				RoutingRequest request = new RoutingRequest(kconfig, RouteCalculationMode.NORMAL);
				request.leftSideNavigation = ctx.leftSideNavigation;
				for (net.osmand.shared.routing.RouteSegmentResult s : copy) {
					TurnPreparation.filterMinorStops(s);
				}
				TurnPreparation.calculateTimeSpeed(request, copy);
				TurnPreparation.prepareTurnResults(request, copy);

				// the whole of java, on its own input
				ctx.requestNativePrepareResult = false;
				List<RouteSegmentResult> java = rrp.prepareResult(ctx, forJava).getList();

				String m = entry.getTestName() + " direction " + planRoadDirection;
				assertEquals(m + " segments", java.size(), copy.size());
				compared += java.size();
				for (RouteSegmentResult j : java) {
					if (j.getTurnType() != null) {
						turns++;
					}
				}
				for (int i = 0; i < java.size(); i++) {
					RouteSegmentResult j = java.get(i);
					net.osmand.shared.routing.RouteSegmentResult k = copy.get(i);
					String s = m + " segment " + i + " road " + j.getObject().id;
					assertEquals(s + " road", j.getObject().id, k.getObject().id);
					assertEquals(s + " start", j.getStartPointIndex(), k.getStartPointIndex());
					assertEquals(s + " end", j.getEndPointIndex(), k.getEndPointIndex());
					Same.close(s + " distance", j.getDistance(), k.getDistance());
					Same.close(s + " segmentTime", j.getSegmentTime(), k.getSegmentTime());
					Same.close(s + " speed", j.getSegmentSpeed(), k.getSegmentSpeed());
					Same.close(s + " routingTime", j.getRoutingTime(), k.getRoutingTime());
					assertTurn(s, j.getTurnType(), k.getTurnType());
				}
			}
		} finally {
			for (BinaryMapIndexReader reader : readers) {
				reader.close();
			}
		}
	}

	private static void assertTurn(String m, net.osmand.router.TurnType j, TurnType k) {
		assertEquals(m + " turn present", j == null, k == null);
		if (j == null) {
			return;
		}
		assertEquals(m + " turn", j.getValue(), k.getValue());
		assertEquals(m + " exit", j.getExitOut(), k.getExitOut());
		Same.close(m + " angle", j.getTurnAngle(), k.getTurnAngle());
		assertEquals(m + " skipToSpeak", j.isSkipToSpeak(), k.isSkipToSpeak());
		assertEquals(m + " possibleLeft", j.isPossibleLeftTurn(), k.isPossibleLeftTurn());
		assertEquals(m + " possibleRight", j.isPossibleRightTurn(), k.isPossibleRightTurn());
		assertEquals(m + " leftSide", j.isLeftSide(), k.isLeftSide());
		assertArrayEquals(m + " lanes", j.getLanes(), k.getLanes());
		assertEquals(m + " otherAngles", j.getOtherTurnAngles(), k.getOtherTurnAngles());
		assertEquals(m + " toString", j.toString(), k.toString());
	}

	/** A fresh segment per raw one: the same road object, the same stretch, nothing prepared yet. */
	private static List<RouteSegmentResult> copyRaw(List<RouteSegmentResult> raw) {
		List<RouteSegmentResult> copy = new ArrayList<>(raw.size());
		for (RouteSegmentResult r : raw) {
			assertNotNull("raw segments carry no turn yet", r.getObject());
			RouteSegmentResult c = new RouteSegmentResult(r.getObject(), r.getStartPointIndex(), r.getEndPointIndex());
			c.setRoutingTime(r.getRoutingTime());
			c.setSegmentTime(r.getSegmentTime());
			c.setSegmentSpeed(r.getSegmentSpeed());
			c.setDistance(r.getDistance());
			c.setTurnType(r.getTurnType());
			c.setGpxPointIndex(r.getGpxPointIndex());
			copy.add(c);
		}
		return copy;
	}

	private static Method privateMethod(String name, Class<?>... types) throws NoSuchMethodException {
		Method m = RouteResultPreparation.class.getDeclaredMethod(name, types);
		m.setAccessible(true);
		return m;
	}
}
