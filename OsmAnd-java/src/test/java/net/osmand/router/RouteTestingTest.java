package net.osmand.router;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.*;
import java.util.Map.Entry;
import net.osmand.NativeLibrary;
import net.osmand.binary.BinaryMapIndexReader;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@RunWith(Parameterized.class)
public class RouteTestingTest {
	private final TestEntry te;

	private static final int TIMEOUT = 1500;
	private static final boolean NATIVE_LIB = false;

	public RouteTestingTest(String name, TestEntry te) {
		this.te = te;
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

	@Test(timeout = TIMEOUT)
	public void testRouting() throws Exception {
		NativeLibrary nativeLibrary;
		if (NATIVE_LIB) {
			boolean old = NativeLibrary.loadOldLib("/Users/plotva/work/osmand/core-legacy/binaries/darwin/intel/Release");
			nativeLibrary = new NativeLibrary();
			if (!old) {
				throw new UnsupportedOperationException("Not supported");
			}
		}

		String fl = "src/test/resources/Routing_test.obf";
		RandomAccessFile raf = new RandomAccessFile(fl, "r");
		RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();

		BinaryMapIndexReader[] binaryMapIndexReaders;// = { new BinaryMapIndexReader(raf, new File(fl)) };
		RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
		Map<String, String> params = te.getParams();
		if (params.containsKey("map")) {
			String fl1 = "src/test/resources/" + params.get("map");
			RandomAccessFile raf1 = new RandomAccessFile(fl1, "r");
			binaryMapIndexReaders = new BinaryMapIndexReader[]{
					new BinaryMapIndexReader(raf1, new File(fl1)),
					new BinaryMapIndexReader(raf, new File(fl))
			};
			if (NATIVE_LIB) {
				nativeLibrary.initMapFile(new File(fl1).getAbsolutePath(), true);
			}
		} else {
			binaryMapIndexReaders = new BinaryMapIndexReader[]{new BinaryMapIndexReader(raf, new File(fl))};
		}
		if (NATIVE_LIB) {
			nativeLibrary.initMapFile(new File(fl).getAbsolutePath(), true);
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

			System.out.println("planRoadDirection: " + planRoadDirection);

			if (params.containsKey("heuristicCoefficient")) {
				config.heuristicCoefficient = Float.parseFloat(params.get("heuristicCoefficient"));
			}

			config.planRoadDirection = planRoadDirection;
			RoutingContext ctx;
			if (NATIVE_LIB) {
				ctx = fe.buildRoutingContext(config, nativeLibrary, binaryMapIndexReaders,
						RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
			} else {
				ctx = fe.buildRoutingContext(config, null, binaryMapIndexReaders,
						RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
			}

			ctx.leftSideNavigation = false;
			List<RouteSegmentResult> routeSegments = fe.searchRoute(ctx, te.getStartPoint(), te.getEndPoint(),
					te.getTransitPoint());
			Set<Long> reachedSegments = new TreeSet<Long>();
			Assert.assertNotNull(routeSegments);
			int prevSegment = -1;
			for (int i = 0; i <= routeSegments.size(); i++) {
				if (i == routeSegments.size() || routeSegments.get(i).getTurnType() != null) {
					if (prevSegment >= 0) {
						String name = routeSegments.get(prevSegment).getDescription();
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
			Map<Long, String> expectedResults = te.getExpectedResults();
			if (expectedResults == null) {
				System.out.println("This is test on hanging routing");
				break;
			}
			for (Entry<Long, String> es : expectedResults.entrySet()) {
				switch (es.getValue()) {
					case "false":
						Assert.assertFalse("Expected segment " + (es.getKey()) + " was wrongly reached in route segments "
								+ reachedSegments, reachedSegments.contains(es.getKey()));
						break;
					case "true":
						Assert.assertTrue("Expected segment " + (es.getKey()) + " weren't reached in route segments "
								+ reachedSegments, reachedSegments.contains(es.getKey()));
						break;
					case "visitedSegments":
						Assert.assertTrue("Expected segments visit " + (es.getKey()) + " less then actually visited segments "
								+ ctx.getVisitedSegments(), ctx.getVisitedSegments() < es.getKey());
						break;
				}
			}
		}
	}
}