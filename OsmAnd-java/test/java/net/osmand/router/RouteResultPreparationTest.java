package net.osmand.router;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import org.apache.commons.logging.Log;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.*;

/**
 * Created by yurkiss on 04.03.16.
 */

@RunWith(Parameterized.class)
public class RouteResultPreparationTest {

    private static RoutePlannerFrontEnd fe;
    private static RoutingContext ctx;

    private String testName;
    private LatLon startPoint;
    private LatLon endPoint;
    private Map<Long, String> expectedResults;

    private Log log = PlatformUtil.getLog(RouteResultPreparationTest.class);

    public RouteResultPreparationTest(String testName, LatLon startPoint, LatLon endPoint, Map<Long, String> expectedResults) {
        this.testName = testName;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.expectedResults = expectedResults;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        String fileName = "../../resources/Turn_lanes_test.obf";

        File fl = new File(fileName);

        RandomAccessFile raf = new RandomAccessFile(fl, "r");

        fe = new RoutePlannerFrontEnd(false);
        RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("car", "true");
        params.put("short_way", "true");
        RoutingConfiguration config = builder.build("car", RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3, params);
        BinaryMapIndexReader[] binaryMapIndexReaders = {new BinaryMapIndexReader(raf, fl)};
        ctx = fe.buildRoutingContext(config, null, binaryMapIndexReaders,
                RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
        ctx.leftSideNavigation = false;
        RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;

    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() throws IOException {

        String fileName = "test_turn_lanes.json";

        Reader reader = new FileReader(fileName);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        TestEntry[] testEntries = gson.fromJson(reader, TestEntry[].class);
        ArrayList<Object[]> twoDArray = new ArrayList<Object[]>();
        for (int i = 0; i < testEntries.length; ++i) {
            Object[] arr = new Object[]{
                    testEntries[i].getTestName(),
                    testEntries[i].getStartPoint(),
                    testEntries[i].getEndPoint(),
                    testEntries[i].getExpectedResults()
            };
            twoDArray.add(arr);
        }
        reader.close();
        return twoDArray;

    }

    @Test
    public void testLanes() throws Exception {

        List<RouteSegmentResult> routeSegments = fe.searchRoute(ctx, startPoint, endPoint, null);
        Set<Long> reachedSegments = new TreeSet<Long>();

        int prevSegment = -1;
        for (int i = 0; i <= routeSegments.size(); i++) {
            if (i == routeSegments.size() || routeSegments.get(i).getTurnType() != null) {
                if (prevSegment >= 0) {
                    String lanes = getLanesString(routeSegments.get(prevSegment));
                    String name = routeSegments.get(prevSegment).getDescription();

                    long segmentId = routeSegments.get(prevSegment).getObject().getId();
                    String expectedResult = expectedResults.get(segmentId);
                    if (expectedResult != null) {
                        Assert.assertEquals("Segment " + segmentId, expectedResult, lanes);
                    } else {
                        //TODO: action if needed when expectedResults is null
                    }

                    System.out.println("segmentId: " + segmentId + " description: " + name);

                }
                prevSegment = i;
            }

            if (i < routeSegments.size()) {
                reachedSegments.add(routeSegments.get(i).getObject().getId());
            }
        }

        Set<Long> expectedSegments = expectedResults.keySet();
        for (Long expSegId : expectedSegments){
            Assert.assertTrue("Expected segment " + expSegId + " weren't reached in route segments " + reachedSegments.toString(), reachedSegments.contains(expSegId));
        }

    }


    private String getLanesString(RouteSegmentResult segment) {
        String turn = segment.getTurnType().toString();
        final int[] lns = segment.getTurnType().getLanes();
        if (lns != null) {
            String s = "";
            for (int h = 0; h < lns.length; h++) {
                if (h > 0) {
                    s += ", ";
                }
                if (lns[h] % 2 == 1) {
                    s += "+";
                }
                int pt = TurnType.getPrimaryTurn(lns[h]);
                if (pt == 0) {
                    pt = 1;
                }
                s += TurnType.valueOf(pt, false).toXmlString();
                int st = TurnType.getSecondaryTurn(lns[h]);
                if (st != 0) {
                    s += ";" + TurnType.valueOf(st, false).toXmlString();
                }

            }
            s += "";
            turn += s;
            return s;
        }
        return null;
    }

}
