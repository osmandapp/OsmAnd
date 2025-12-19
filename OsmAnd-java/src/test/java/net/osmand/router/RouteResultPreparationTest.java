package net.osmand.router;

import static net.osmand.util.RouterUtilTest.getNativeLibPath;
import static net.osmand.util.RouterUtilTest.getRoadId;
import static net.osmand.util.RouterUtilTest.getRoadStartPoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.logging.Log;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.data.LatLon;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

/**
 * Created by yurkiss on 04.03.16.
 */

@RunWith(Parameterized.class)
public class RouteResultPreparationTest {
    
    private final TestEntry te;
    private static RoutePlannerFrontEnd fe;
    private static RoutingContext ctx;

    private static final Path GENERATED_JSON_DIR = Paths.get("D:\\Projects\\git\\Osmand\\resources\\test-resources");
    private static final Path GENERATED_JSON_FILE = GENERATED_JSON_DIR.resolve("test_turn_lanes_segments.json");
    private static final List<Map<String, Object>> GENERATED_JSON = Collections.synchronizedList(new ArrayList<>());
	private static final int POLYLINE_POINT_LIMIT = 25;
	private static final int ATTACHED_ROUTES_LIMIT = 20;
    

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

    @AfterClass
    public static void writeGeneratedJson() throws IOException {
        Files.createDirectories(GENERATED_JSON_DIR);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(GENERATED_JSON_FILE)) {
            gson.toJson(GENERATED_JSON, writer);
        }
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
            ctx.requestNativePrepareResult = true;
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

        List<Map<String, Object>> segmentsJson = new ArrayList<>();
        Map<String, Object> testJson = new LinkedHashMap<>();
		String testId = te.getTestName().split("\\.")[0];
	    testJson.put("id", testId);
        testJson.put("testName", te.getTestName());
        testJson.put("segments", segmentsJson);
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
                    long segmentId = ObfConstants.getOsmObjectId(segment.getObject());
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
                    // {
	                //    "testName": te.getTestName(),
	                //    "segments": [{
	                //      "id": segmentId,
	                //      "lanes": lanes,
	                //      "next": turn
	                //    }, ...]
	                //  }
                    Map<String, Object> segmentJson = new LinkedHashMap<>();
					segmentJson.put("shot_id", testId + ":" + prevSegment);
					segmentJson.put("route_segment_index", prevSegment);
					segmentJson.put("route_total_segments", routeSegments.size());
                    segmentJson.put("id", segmentId);
	                Double distance = parseDistanceFromDescription(name);
	                segmentJson.put("distance_to_next_segment", distance != null ? round2(distance) : "");
                    segmentJson.put("lanes", lanes == null ? "" : lanes);

					TurnType turnType = segment.getTurnType();
					Map<String, Object> maneuverJson = new LinkedHashMap<>();
					maneuverJson.put("type", turn);
					maneuverJson.put("value", turnType.getValue());
					maneuverJson.put("skip_to_speak", skipToSpeak);
					maneuverJson.put("turn_angle", turnType.getTurnAngle() != 0 ? round2(turnType.getTurnAngle()) : "");
					maneuverJson.put("roundabout_exit", turnType.isRoundAbout() ? turnType.getExitOut() : "");
					segmentJson.put("maneuver", maneuverJson);

					RouteSegmentResult outgoingSegment = (prevSegment + 1) < routeSegments.size() ? routeSegments.get(prevSegment + 1) : null;
					segmentJson.put("incoming_route_segment_index", prevSegment > 0 ? (prevSegment - 1) : "");
					segmentJson.put("outgoing_route_segment_index", outgoingSegment != null ? (prevSegment + 1) : "");
					if (outgoingSegment != null) {
						segmentJson.put("turn_deviation_deg", safeDegreesDiff(segment.getBearingEnd(), outgoingSegment.getBearingBegin()));
					} else {
						segmentJson.put("turn_deviation_deg", "");
					}

					Map<String, Object> approachSegmentJson = buildRouteSegmentJson(segment, true);
					String approachTurnLanesTag = RouteResultPreparation.getTurnLanesString(segment);
					approachSegmentJson.put("turn_lanes_tag", approachTurnLanesTag == null ? "" : approachTurnLanesTag);
					segmentJson.put("approach_segment", approachSegmentJson);
					segmentJson.put("outgoing_segment", buildRouteSegmentJson(outgoingSegment, false));
					segmentJson.put("lane_guidance", buildLaneGuidanceJson(turnType));
					segmentJson.put("intersection", buildRoadSplitStructureJson(turnType.getRoadSplitStructure()));
					Map<String, Object> alternativesGeometryJson = new LinkedHashMap<>();
					List<RouteSegmentResult> attachedRoutesAtJunction = outgoingSegment != null
							? outgoingSegment.getAttachedRoutes(outgoingSegment.getStartPointIndex())
							: Collections.emptyList();
					alternativesGeometryJson.put("attached_routes",
							buildAttachedRoutesJson(attachedRoutesAtJunction, segment, outgoingSegment));
					segmentJson.put("alternatives_geometry", alternativesGeometryJson);
                    segmentsJson.add(segmentJson);

                    System.out.println("segmentId: " + segmentId + " -> " + turn + ", dist: " + segment.getDistance() + ", desc: " + name);
                }
                prevSegment = i;
                if (i < routeSegments.size()) {
                    checkedSegments.add(ObfConstants.getOsmObjectId(routeSegments.get(i).getObject()));
                }
            }
            if (i < routeSegments.size()) {
				Long id = ObfConstants.getOsmObjectId(routeSegments.get(i).getObject());
                int startPoint = routeSegments.get(i).getStartPointIndex();
                reachedSegmentsWithStartPoint.add(id + ":" + startPoint);
                reachedSegments.add(id);
            }
        }

        synchronized (GENERATED_JSON) {
            boolean alreadyAdded = false;
            for (Map<String, Object> existingTestJson : GENERATED_JSON) {
                if (Objects.equals(existingTestJson.get("id"), testJson.get("id"))) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                GENERATED_JSON.add(testJson);
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

	private static Object safeDegreesDiff(double a1, double a2) {
		double diff = MapUtils.degreesDiff(a1, a2);
		return round2(diff);
	}

	private static Object round2(double value) {
		if (!Double.isFinite(value)) {
			return "";
		}
		double rounded = Math.round(value * 100.0) / 100.0;
		return rounded;
	}

	private static Map<String, Object> buildRouteSegmentJson(RouteSegmentResult segment, boolean polylineFromEnd) {
		Map<String, Object> json = new LinkedHashMap<>();
		if (segment == null) {
			return json;
		}
		json.put("start_point_index", segment.getStartPointIndex());
		json.put("end_point_index", segment.getEndPointIndex());
		json.put("is_forward", segment.isForwardDirection());
		json.put("distance_m", round2(segment.getDistance()));
		json.put("speed_mps", round2(segment.getSegmentSpeed()));
		json.put("time_s", round2(segment.getSegmentTime()));
		json.put("bearing_begin_deg", round2(segment.getBearingBegin()));
		json.put("bearing_end_deg", round2(segment.getBearingEnd()));
		json.put("highway", segment.getObject().getHighway());
		json.put("oneway", segment.getObject().getOneway());
		json.put("lanes_tag", segment.getObject().getLanes());
		json.put("name", segment.getObject().getName());
		return json;
	}

	private static Map<String, Object> buildLaneGuidanceJson(TurnType turnType) {
		Map<String, Object> json = new LinkedHashMap<>();
		int[] lanes = turnType == null ? null : turnType.getLanes();
		json.put("lanes_string", lanes == null ? "" : TurnType.lanesToString(lanes));
		List<Map<String, Object>> lanesJson = new ArrayList<>();
		if (lanes != null) {
			for (int i = 0; i < lanes.length; i++) {
				int laneValue = lanes[i];
				Map<String, Object> laneJson = new LinkedHashMap<>();
				laneJson.put("index", i);
				laneJson.put("active", laneValue % 2 == 1);
				int primary = TurnType.getPrimaryTurn(laneValue);
				if (primary == 0) {
					primary = TurnType.C;
				}
				laneJson.put("primary", TurnType.valueOf(primary, false).toXmlString());
				int secondary = TurnType.getSecondaryTurn(laneValue);
				laneJson.put("secondary", secondary == 0 ? "" : TurnType.valueOf(secondary, false).toXmlString());
				int tertiary = TurnType.getTertiaryTurn(laneValue);
				laneJson.put("tertiary", tertiary == 0 ? "" : TurnType.valueOf(tertiary, false).toXmlString());
				lanesJson.add(laneJson);
			}
		}
		json.put("lanes", lanesJson);
		return json;
	}

	private static Map<String, Object> buildRoadSplitStructureJson(RoadSplitStructure rs) {
		Map<String, Object> json = new LinkedHashMap<>();
		if (rs == null) {
			return json;
		}
		json.put("keep_left", rs.keepLeft);
		json.put("keep_right", rs.keepRight);
		json.put("speak", rs.speak);
		json.put("roads_on_left", rs.roadsOnLeft);
		json.put("roads_on_right", rs.roadsOnRight);
		json.put("left_lanes", rs.leftLanes);
		json.put("right_lanes", rs.rightLanes);
		json.put("left_max_prio", rs.leftMaxPrio);
		json.put("right_max_prio", rs.rightMaxPrio);
		json.put("attached_roads_left", buildRoadSplitAttachedInfoJson(rs.leftLanesInfo));
		json.put("attached_roads_right", buildRoadSplitAttachedInfoJson(rs.rightLanesInfo));
		return json;
	}

	private static List<Map<String, Object>> buildRoadSplitAttachedInfoJson(List<RoadSplitStructure.AttachedRoadInfo> infos) {
		List<Map<String, Object>> res = new ArrayList<>();
		if (infos == null) {
			return res;
		}
		for (RoadSplitStructure.AttachedRoadInfo ai : infos) {
			Map<String, Object> json = new LinkedHashMap<>();
			json.put("attached_angle_deg", round2(ai.attachedAngle));
			json.put("lanes_count", ai.lanes);
			json.put("speak_priority", ai.speakPriority);
			json.put("attached_on_right", ai.attachedOnTheRight);
			json.put("synthetic_turn_value", ai.turnType);
			json.put("turn_lanes_encoded", ai.parsedLanes == null ? new int[0] : ai.parsedLanes);
			json.put("turn_lanes_string", ai.parsedLanes == null ? "" : TurnType.lanesToString(ai.parsedLanes));
			res.add(json);
		}
		return res;
	}

	private static List<Map<String, Object>> buildAttachedRoutesJson(List<RouteSegmentResult> attachedRoutes,
	                                                               RouteSegmentResult approachSegment,
	                                                               RouteSegmentResult outgoingSegment) {
		List<Map<String, Object>> res = new ArrayList<>();
		if ((attachedRoutes == null || attachedRoutes.isEmpty()) && outgoingSegment == null) {
			return res;
		}
		double approachBearing = approachSegment == null ? Double.NaN : approachSegment.getBearingEnd();
		Set<Long> usedWayIds = new HashSet<>();
		if (outgoingSegment != null) {
			Map<String, Object> outgoingJson = buildRouteSegmentJson(outgoingSegment, false);
			outgoingJson.put("attached_angle_deg", safeDegreesDiff(approachBearing, outgoingSegment.getBearingBegin()));
			outgoingJson.put("is_selected", true);
			res.add(outgoingJson);
			usedWayIds.add(ObfConstants.getOsmObjectId(outgoingSegment.getObject()));
		}
		List<RouteSegmentResult> sorted = new ArrayList<>(attachedRoutes == null ? Collections.emptyList() : attachedRoutes);
		Collections.sort(sorted, (a, b) -> {
			double aAngle = MapUtils.degreesDiff(approachBearing, a.getBearingBegin());
			double bAngle = MapUtils.degreesDiff(approachBearing, b.getBearingBegin());
			if (!Double.isFinite(aAngle)) {
				aAngle = 0;
			}
			if (!Double.isFinite(bAngle)) {
				bAngle = 0;
			}
			int angleCompare = Double.compare(aAngle, bAngle);
			if (angleCompare != 0) {
				return angleCompare;
			}
			long aId = ObfConstants.getOsmObjectId(a.getObject());
			long bId = ObfConstants.getOsmObjectId(b.getObject());
			if (aId != bId) {
				return Long.compare(aId, bId);
			}
			if (a.getStartPointIndex() != b.getStartPointIndex()) {
				return Integer.compare(a.getStartPointIndex(), b.getStartPointIndex());
			}
			return Integer.compare(a.getEndPointIndex(), b.getEndPointIndex());
		});
		long outgoingId = outgoingSegment == null ? Long.MIN_VALUE : ObfConstants.getOsmObjectId(outgoingSegment.getObject());
		for (RouteSegmentResult ar : sorted) {
			if (res.size() >= ATTACHED_ROUTES_LIMIT) {
				break;
			}
			long arId = ObfConstants.getOsmObjectId(ar.getObject());
			if (usedWayIds.contains(arId)) {
				continue;
			}
			Map<String, Object> json = buildRouteSegmentJson(ar, false);
			json.put("attached_angle_deg", safeDegreesDiff(approachBearing, ar.getBearingBegin()));
			json.put("is_selected", outgoingSegment != null && arId == outgoingId);
			res.add(json);
			usedWayIds.add(arId);
		}
		return res;
	}

	private static Double parseDistanceFromDescription(String description) {
		if (Algorithms.isEmpty(description)) {
			return null;
		}
		String normalized = description.toLowerCase();
		int goIndex = normalized.lastIndexOf(" go ");
		String tail = goIndex >= 0 ? normalized.substring(goIndex) : normalized;

		java.util.regex.Matcher kmMatcher = java.util.regex.Pattern
				.compile("(\\d+(?:\\.\\d+)?)\\s*km")
				.matcher(tail);
		if (kmMatcher.find()) {
			return Double.parseDouble(kmMatcher.group(1));
		}

		java.util.regex.Matcher mMatcher = java.util.regex.Pattern
				.compile("(\\d+(?:\\.\\d+)?)\\s*m")
				.matcher(tail);
		if (mMatcher.find()) {
			return Double.parseDouble(mMatcher.group(1)) / 1000.0;
		}
		return null;
	}

}
