package net.osmand.router;

import static net.osmand.util.RouterUtilTest.getExpectedIdSet;
import static net.osmand.util.RouterUtilTest.getNativeLibPath;
import static net.osmand.util.RouterUtilTest.getRoadId;
import static net.osmand.util.RouterUtilTest.getRoadStartPoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.Algorithms;

/**
 * Created by yurkiss on 04.03.16.
 */

@RunWith(Parameterized.class)
public class RouteResultPreparationTest {
    
    private final TestEntry te;
    private static RoutePlannerFrontEnd fe;
    private static RoutingContext ctx;
    

    protected Log log = PlatformUtil.getLog(RouteResultPreparationTest.class);

    public RouteResultPreparationTest(String name, TestEntry te) {
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
    public static Collection<Object[]> data() throws IOException {
        String fileName = "/test_turn_lanes.json";
        Reader reader = new InputStreamReader(Objects.requireNonNull(RouteResultPreparationTest.class.getResourceAsStream(fileName)));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        TestEntry[] testEntries = gson.fromJson(reader, TestEntry[].class);
        ArrayList<Object[]> twoDArray = new ArrayList<>();
        for (TestEntry testEntry : testEntries) {
            if (testEntry.isIgnore()) {
                continue;
            }
            twoDArray.add(new Object[]{testEntry.getTestName(), testEntry});
        }
        reader.close();
        return twoDArray;

    }

    @Test
    public void testLanes() throws Exception {
        NativeLibrary nativeLibrary = null;
        boolean useNative = isNative() && getNativeLibPath() != null && !te.isIgnoreNative();
        if (useNative) {
            boolean old = NativeLibrary.loadOldLib(getNativeLibPath());
            nativeLibrary = new NativeLibrary();
            if (!old) {
                //throw new UnsupportedOperationException("Not supported");
                useNative = false;
            }
        }
        
        String fileName = "src/test/resources/Turn_lanes_test.obf";
        File fl = new File(fileName);
    
        RandomAccessFile raf = new RandomAccessFile(fl, "r");
        fe = new RoutePlannerFrontEnd();
        RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
        if (useNative) {
            Objects.requireNonNull(nativeLibrary).initMapFile(fl.getAbsolutePath(), true);
        }
        Map<String, String> params = te.getParams();
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
        
        if (useNative) {
            ctx = fe.buildRoutingContext(config, nativeLibrary, binaryMapIndexReaders,
                    RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
        } else {
            ctx = fe.buildRoutingContext(config, null, binaryMapIndexReaders,
                    RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
        }
        ctx.leftSideNavigation = false;
        
        List<RouteSegmentResult> routeSegments = fe.searchRoute(ctx, te.getStartPoint(), te.getEndPoint(), null).detailed;
        Set<String> reachedSegmentsWithStartPoint = new TreeSet<>();
        Set<Long> reachedSegments = new TreeSet<>();
        Set<Long> checkedSegments = new TreeSet<>();
        Assert.assertNotNull(routeSegments);
        int prevSegment = -1;
        for (int i = 0; i <= routeSegments.size(); i++) {
            if (i == routeSegments.size() || routeSegments.get(i).getTurnType() != null) {
                if (prevSegment >= 0) {
                    RouteSegmentResult segment = routeSegments.get(prevSegment);
                    String lanes = getLanesString(segment);
                    String turn = segment.getTurnType().toXmlString();
                    String turnLanes = turn + ":" + lanes;
                    String name = segment.getDescription(false);
                    boolean skipToSpeak = segment.getTurnType().isSkipToSpeak();
                    if (skipToSpeak) {
                        turnLanes = "[MUTE] " + turnLanes;
                    }
                    long segmentId = segment.getObject().getId() >> (RouteResultPreparation.SHIFT_ID);
                    String expectedResult = null;
                    int startPoint = -1;
                    for (Entry<String, String> er : te.getExpectedResults().entrySet()) {
                        String roadInfo = er.getKey();
                        long id = getRoadId(roadInfo);
                        if (id == segmentId) {
                            expectedResult = er.getValue();
                            startPoint = getRoadStartPoint(roadInfo);
                        }
                    }
                    
                    if (expectedResult != null) {
                        if (startPoint < 0 || (startPoint >= 0 && segment.getStartPointIndex() == startPoint)) {
                             if (!Algorithms.objectEquals(expectedResult, turnLanes)
                                    && !Algorithms.objectEquals(expectedResult, lanes)
                                    && !Algorithms.objectEquals(expectedResult, turn)) {
                                Assert.assertEquals("Segment " + segmentId, expectedResult, turnLanes);
                            }
                        }
                    }
                    
                    System.out.println("segmentId: " + segmentId + " description: " + name);
                }
                prevSegment = i;
                if (i < routeSegments.size()) {
                    checkedSegments.add(routeSegments.get(i).getObject().getId() >> (RouteResultPreparation.SHIFT_ID));
                }
            }
            if (i < routeSegments.size()) {
                Long id = routeSegments.get(i).getObject().getId() >> (RouteResultPreparation.SHIFT_ID);
                int startPoint = routeSegments.get(i).getStartPointIndex();
                reachedSegmentsWithStartPoint.add(id + ":" + startPoint);
                reachedSegments.add(id);
            }
        }
        for (Entry<String, String> er : te.getExpectedResults().entrySet()) {
            String roadInfo = er.getKey();
            long id = getRoadId(roadInfo);
            int startPoint = getRoadStartPoint(roadInfo);

            Assert.assertTrue(
                    "Segment " + roadInfo + " was not reached in " + reachedSegmentsWithStartPoint,
                    startPoint == -1 ? reachedSegments.contains(id) : reachedSegmentsWithStartPoint.contains(roadInfo));

            if (!checkedSegments.contains(id)) {
                String expectedResult = er.getValue();
                if (!Algorithms.isEmpty(expectedResult)) {
                    Assert.assertEquals("Segment " + id, expectedResult, "NULL");
                }
            }
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
