package net.osmand.router;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
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

/**
 * Created by yurkiss on 04.03.16.
 */

@RunWith(Parameterized.class)
public class RouteResultPreparationTest {
    
    private static RoutePlannerFrontEnd fe;
    private static RoutingContext ctx;

    private LatLon startPoint;
    private LatLon endPoint;
    private Map<Long, String> expectedResults;
    private Map<String, String> params;

    protected Log log = PlatformUtil.getLog(RouteResultPreparationTest.class);

    public RouteResultPreparationTest(LatLon startPoint, LatLon endPoint, Map<Long, String> expectedResults, Map<String, String> params) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.expectedResults = expectedResults;
        this.params = params;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;

    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() throws IOException {
        String fileName = "/test_turn_lanes.json";
        Reader reader = new InputStreamReader(RouteResultPreparationTest.class.getResourceAsStream(fileName));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        TestEntry[] testEntries = gson.fromJson(reader, TestEntry[].class);
        ArrayList<Object[]> twoDArray = new ArrayList<>();
        for (TestEntry testEntry : testEntries) {
            if (!testEntry.isIgnore()) {
                Object[] arr = new Object[]{testEntry.getStartPoint(),
                        testEntry.getEndPoint(), testEntry.getExpectedResults(), testEntry.getParams()};
                twoDArray.add(arr);
            }
        }
        reader.close();
        return twoDArray;

    }

    @Test
    public void testLanes() throws Exception {
        String fileName = "src/test/resources/Turn_lanes_test.obf";
        File fl = new File(fileName);
    
        RandomAccessFile raf = new RandomAccessFile(fl, "r");
        fe = new RoutePlannerFrontEnd();
        RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
        if (params == null) {
            params = new HashMap<>();
        }
        params.put("car", "true");
        RoutingMemoryLimits memoryLimit = new RoutingMemoryLimits(
                RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
                RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT
        );
        RoutingConfiguration config = builder.build("car", memoryLimit, params);
        BinaryMapIndexReader[] binaryMapIndexReaders = {new BinaryMapIndexReader(raf, fl)};
        ctx = fe.buildRoutingContext(config, null, binaryMapIndexReaders,
                RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
        ctx.leftSideNavigation = false;
        
        List<RouteSegmentResult> routeSegments = fe.searchRoute(ctx, startPoint, endPoint, null);
        Set<Long> reachedSegments = new TreeSet<Long>();
        Assert.assertNotNull(routeSegments);
        int prevSegment = -1;
        for (int i = 0; i <= routeSegments.size(); i++) {
            if (i == routeSegments.size() || routeSegments.get(i).getTurnType() != null) {
                if (prevSegment >= 0) {
                    RouteSegmentResult segment = routeSegments.get(prevSegment);
                    String lanes = getLanesString(segment);
                    String turn = segment.getTurnType().toXmlString();
                    String turnLanes = turn + ":" + lanes;
                    String name = segment.getDescription();
                    boolean skipToSpeak = segment.getTurnType().isSkipToSpeak();
                    long segmentId = segment.getObject().getId() >> (RouteResultPreparation.SHIFT_ID);
                    String expectedResult = expectedResults.get(segmentId);
                    int startPoint = -1;
                    if (params.containsKey("startPoint")) {
                        startPoint = Integer.parseInt(params.get("startPoint"));
                    }
                    if (expectedResult != null) {
                        if (startPoint < 0 || startPoint > 0 && segment.getStartPointIndex() == startPoint) {
                            if ("skipToSpeak".equals(expectedResult)) {
                                Assert.assertTrue("Segment " + segmentId + " is skipToSpeak", skipToSpeak);
                            } else if (!Algorithms.objectEquals(expectedResult, turnLanes)
                                    && !Algorithms.objectEquals(expectedResult, lanes)
                                    && !Algorithms.objectEquals(expectedResult, turn)) {
                                Assert.assertEquals("Segment " + segmentId, expectedResult, turnLanes);
                            }
                        }
                    }
                    
                    System.out.println("segmentId: " + segmentId + " description: " + name);
                }
                prevSegment = i;
            }
            if (i < routeSegments.size()) {
                reachedSegments.add(routeSegments.get(i).getObject().getId() >> (RouteResultPreparation.SHIFT_ID ));
            }
        }
        Set<Long> expectedSegments = expectedResults.keySet();
        for (Long expSegId : expectedSegments){
            Assert.assertTrue("Expected segment " + (expSegId ) + 
            		" weren't reached in route segments " + reachedSegments.toString(), reachedSegments.contains(expSegId));
        }
    }


    private String getLanesString(RouteSegmentResult segment) {
        final int[] lns = segment.getTurnType().getLanes();
        if (lns != null) {
        	return TurnType.lanesToString(lns);
        }
        return null;
    }

}
