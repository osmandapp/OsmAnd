package net.osmand.router;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.osmand.binary.BinaryInspector;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by yurkiss on 04.03.16.
 */

@RunWith(Parameterized.class)
public class RouteTestingTestIgnore {


    private LatLon startPoint;
    private LatLon endPoint;
    private Map<Long, String> expectedResults;
	private Map<String, String> params;


    public RouteTestingTestIgnore(String testName, LatLon startPoint, LatLon endPoint, Map<Long, String> expectedResults, 
    		Map<String, String> params) {
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
        String fileName = "/test_routing.json";
        Reader reader = new InputStreamReader(RouteTestingTestIgnore.class.getResourceAsStream(fileName));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        TestEntry[] testEntries = gson.fromJson(reader, TestEntry[].class);
        ArrayList<Object[]> twoDArray = new ArrayList<Object[]>();
        for (int i = 0; i < testEntries.length; ++i) {
			if (!testEntries[i].isIgnore()) {
				Object[] arr = new Object[] { testEntries[i].getTestName(), testEntries[i].getStartPoint(),
						testEntries[i].getEndPoint(), testEntries[i].getExpectedResults(),
						testEntries[i].getParams()};
				twoDArray.add(arr);
			}
        }
        reader.close();
        return twoDArray;

    }

    @Test
    public void testRouting() throws Exception {
    	String fl = "../../resources/test-resources/Routing_test.obf";
    	RandomAccessFile raf = new RandomAccessFile(fl, "r");
    	RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd(false);
        
		BinaryMapIndexReader[] binaryMapIndexReaders = { new BinaryMapIndexReader(raf, new File(fl)) };
		RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
        RoutingConfiguration config = builder.build(params.containsKey("vehicle") ? 
        		params.get("vehicle") : "car", RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3, params);
        RoutingContext ctx = fe.buildRoutingContext(config, null, binaryMapIndexReaders,
                RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
        ctx.leftSideNavigation = false;
        List<RouteSegmentResult> routeSegments = fe.searchRoute(ctx, startPoint, endPoint, null);
        Set<Long> reachedSegments = new TreeSet<Long>();
        Assert.assertNotNull(routeSegments);
        int prevSegment = -1;
        for (int i = 0; i <= routeSegments.size(); i++) {
            if (i == routeSegments.size() || routeSegments.get(i).getTurnType() != null) {
                if (prevSegment >= 0) {
                    String name = routeSegments.get(prevSegment).getDescription();
                    long segmentId = routeSegments.get(prevSegment).getObject().getId() >> (BinaryInspector.SHIFT_ID );
                    System.out.println("segmentId: " + segmentId + " description: " + name);
                }
                prevSegment = i;
            }
            if (i < routeSegments.size()) {
                reachedSegments.add(routeSegments.get(i).getObject().getId() >> (BinaryInspector.SHIFT_ID ));
            }
        }

        Set<Long> expectedSegments = expectedResults.keySet();
        for (Long expSegId : expectedSegments){
            Assert.assertTrue("Expected segment " + (expSegId ) + 
            		" weren't reached in route segments " + reachedSegments.toString(), reachedSegments.contains(expSegId));
        }
    }



}
