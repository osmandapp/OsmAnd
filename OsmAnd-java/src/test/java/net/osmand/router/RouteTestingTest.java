package net.osmand.router;

import static net.osmand.util.RouterUtilTest.getNativeLibPath;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.osmand.NativeLibrary;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.RouterUtilTest;

@RunWith(Parameterized.class)
public class RouteTestingTest {
	private final TestEntry te;

	private static final int TIMEOUT = 1500;

	public RouteTestingTest(String name, TestEntry te) {
		this.te = te;
	}
	
	boolean isNative() {
		return false;
	}

	@BeforeClass
	public static void setUp() throws Exception {
		RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Iterable<Object[]> data() throws IOException {
		String fileName = "/test_routing.json";
		Reader reader = new InputStreamReader(Objects.requireNonNull(RouteTestingTest.class.getResourceAsStream(fileName)));
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		TestEntry[] testEntries = gson.fromJson(reader, TestEntry[].class);
		ArrayList<Object[]> arrayList = new ArrayList<>();
		for (TestEntry te : testEntries) {
			if (te.isIgnore()) {
				continue;
			}
			arrayList.add(new Object[]{te.getTestName(), te});
		}
		reader.close();
		return arrayList;

	}

//	@Ignore 
	@Test(timeout = TIMEOUT)
	public void testRouting() throws Exception {
		NativeLibrary nativeLibrary = null;
//		BinaryRoutePlanner.TRACE_ROUTING = true;
//		BinaryRoutePlanner.DEBUG_BREAK_EACH_SEGMENT = true; 
//		BinaryRoutePlanner.DEBUG_PRECISE_DIST_MEASUREMENT = true;
//		float DEFAULT_HR = 0;
		
		boolean useNative = isNative() && getNativeLibPath() != null && !te.isIgnoreNative();
		if (useNative) {
			boolean old = NativeLibrary.loadOldLib(getNativeLibPath());
			nativeLibrary = new NativeLibrary();
			if (!old) {
				throw new UnsupportedOperationException("Not supported");
			}
		}

		String fl = "src/test/resources/routing/Routing_test_archive.obf";
		RandomAccessFile raf = new RandomAccessFile(fl, "r");
		RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
		fe.CALCULATE_MISSING_MAPS = false;

		BinaryMapIndexReader[] binaryMapIndexReaders;// = { new BinaryMapIndexReader(raf, new File(fl)) };
		RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
		Map<String, String> params = te.getParams();
		if (params.containsKey("map")) {
			String fl1 = "src/test/resources/routing/" + params.get("map");
			RandomAccessFile raf1 = new RandomAccessFile(fl1, "r");
			binaryMapIndexReaders = new BinaryMapIndexReader[]{
					new BinaryMapIndexReader(raf1, new File(fl1)),
					new BinaryMapIndexReader(raf, new File(fl))
			};
			if (useNative) {
				Objects.requireNonNull(nativeLibrary).initMapFile(new File(fl1).getAbsolutePath(), true);
			}
		} else {
			binaryMapIndexReaders = new BinaryMapIndexReader[]{new BinaryMapIndexReader(raf, new File(fl))};
		}
		if (useNative) {
			Objects.requireNonNull(nativeLibrary).initMapFile(new File(fl).getAbsolutePath(), true);
		}
		for (int planRoadDirection = -1; planRoadDirection <= 1; planRoadDirection++) {
			if (params.containsKey("wrongPlanRoadDirection")) {
				if (params.get("wrongPlanRoadDirection").equals(planRoadDirection + "")) {
					continue;
				}
			}
			RoutingMemoryLimits memoryLimits = new RoutingMemoryLimits(
					RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
					RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT
			);
			RoutingConfiguration config = builder.build(params.containsKey("vehicle") ? params.get("vehicle") : "car",
					memoryLimits, params);

			if (params.containsKey("routeCalculationTime")) {
				config.routeCalculationTime = Long.parseLong(params.get("routeCalculationTime")); // conditional
			}
//			config.heuristicCoefficient = DEFAULT_HR;

			System.out.println("planRoadDirection: " + planRoadDirection);

			if (params.containsKey("heuristicCoefficient")) {
				config.heuristicCoefficient = Float.parseFloat(params.get("heuristicCoefficient"));
			}

			config.planRoadDirection = planRoadDirection;
			RoutingContext ctx;
			if (useNative) {
				ctx = fe.buildRoutingContext(config, nativeLibrary, binaryMapIndexReaders,
						RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
			} else {
				ctx = fe.buildRoutingContext(config, null, binaryMapIndexReaders,
						RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
			}

			ctx.leftSideNavigation = false;
			List<RouteSegmentResult> routeSegments = fe.searchRoute(ctx, te.getStartPoint(), te.getEndPoint(),
					te.getTransitPoint()).detailed;
			Set<Long> reachedSegments = new TreeSet<Long>();
			Assert.assertNotNull(routeSegments);
			int prevSegment = -1;
			for (int i = 0; i <= routeSegments.size(); i++) {
				if (i == routeSegments.size() || routeSegments.get(i).getTurnType() != null) {
					if (prevSegment >= 0) {
						String name = routeSegments.get(prevSegment).getDescription(false);
						long segmentId = routeSegments.get(prevSegment).getObject()
								.getId() >> (RouteResultPreparation.SHIFT_ID);
						System.out.println("segmentId: " + segmentId + " description: " + name);
					}
					prevSegment = i;
				}
				if (i < routeSegments.size()) {
					reachedSegments.add(routeSegments.get(i).getObject().getId() >> (RouteResultPreparation.SHIFT_ID));
				}
			}
			Map<String, String> expectedResults = te.getExpectedResults();
			if (expectedResults == null) {
				System.out.println("This is test on hanging routing");
				break;
			}
			checkRoutingTime(ctx, params);
			for (Entry<String, String> es : expectedResults.entrySet()) {
				long id = RouterUtilTest.getRoadId(es.getKey());
				switch (es.getValue()) {
					case "false":
						Assert.assertFalse("Expected segment " + id + " was wrongly reached in route segments "
								+ reachedSegments, reachedSegments.contains(id));
						break;
					case "true":
						Assert.assertTrue("Expected segment " + id + " weren't reached in route segments "
								+ reachedSegments, reachedSegments.contains(id));
						break;
					case "visitedSegments":
						Assert.assertTrue("Expected segments visit " + id + " less then actually visited segments "
								+ ctx.getVisitedSegments(), ctx.getVisitedSegments() < id);
						break;
					default: // case ID:N to check exact point within the ID's segment
						boolean isFound = false;
						int point = Integer.parseInt(es.getValue());
						for (RouteSegmentResult seg : routeSegments) {
							if (seg.getObject().getId() / 64 == id) {
								if (point >= Math.min(seg.getStartPointIndex(), seg.getEndPointIndex()) &&
										point <= Math.max(seg.getStartPointIndex(), seg.getEndPointIndex())) {
									isFound = true;
									break;
								}
							}
						}
						Assert.assertTrue("Expected point " + point + " is not found in segment " + id, isFound);
						break;
				}
			}
		}
	}
	
	private void checkRoutingTime(RoutingContext ctx, Map<String, String> params) {
		if (params.containsKey("maxRoutingTime")) {
			float maxRoutingTime = Float.parseFloat(params.get("maxRoutingTime"));
			Assert.assertTrue("Calculated routing time " + ctx.routingTime + " is bigger then max routing time " + maxRoutingTime, ctx.routingTime < maxRoutingTime);
		}
	}
	
	
}