package net.osmand.router;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.osmand.binary.ObfConstants;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TurnLanesOneShotJsonBuilder {
    private static final Path GENERATED_JSON_DIR = Paths.get("D:\\Projects\\git\\Osmand\\resources\\test-resources\\turn-lanes");
    private static final int ATTACHED_ROUTES_LIMIT = 20;
	private static final Pattern TEST_ID_PATTERN = Pattern.compile("^(\\d+(?:[\\.,]\\d+)*)");

	private static final Map<String, String> SEGMENTS = new HashMap<>();
	private final String testId;
    private final LatLon startPoint, endPoint;
    private final List<RouteSegmentResult> routeSegments;

	private String extractTestId(String testName) {
		String normalizedTestName = testName == null ? "" : testName.trim();
		Matcher matcher = TEST_ID_PATTERN.matcher(normalizedTestName);
		String id = matcher.find() ? matcher.group(1) : "";

		String prevId = SEGMENTS.get(startPoint + " -> " + endPoint);
		if (prevId != null)
			return null;

		SEGMENTS.put(startPoint + " -> " + endPoint, testId);
		return id;
	}

	public TurnLanesOneShotJsonBuilder(List<RouteSegmentResult> routeSegments,
	                                   String testName, LatLon startPoint, LatLon endPoint) {
		this.startPoint = startPoint;
		this.endPoint = endPoint;
		this.testId = extractTestId(testName);
		this.routeSegments = routeSegments;
	}

	public TurnLanesOneShotJsonBuilder(net.osmand.binary.BinaryMapIndexReader[] readers,
	                                    String testName, LatLon startPoint, LatLon endPoint) throws Exception {
		this.startPoint = startPoint;
		this.endPoint = endPoint;
		this.testId = extractTestId(testName);
		if (this.testId == null) {
			routeSegments = null;
			return;
		}
		Map<String, String> params = new HashMap<>();
		params.put("car", "true");
		RoutingConfiguration.RoutingMemoryLimits memoryLimit = new RoutingConfiguration.RoutingMemoryLimits(
				RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
				RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT
		);

		RoutingConfiguration config = RoutingConfiguration.getDefault().build("car", memoryLimit, params);
		RoutePlannerFrontEnd fe = new RoutePlannerFrontEnd();
		RoutingContext ctx = fe.buildRoutingContext(config, null, readers, RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
		ctx.leftSideNavigation = false;

		routeSegments = fe.searchRoute(ctx, startPoint, endPoint, null).detailed;
	}

	public void writeJson() throws IOException {
		if (testId == null) {
			return;
		}

		Files.createDirectories(GENERATED_JSON_DIR);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		Map<String, Object> testJson = buildTestJson();
		String safeTestId = Algorithms.convertToPermittedFileName(testId).trim();
		if (safeTestId.isEmpty()) {
			safeTestId = "unknown";
		}
		try (Writer writer = Files.newBufferedWriter(GENERATED_JSON_DIR.resolve(safeTestId + ".json"))) {
			gson.toJson(testJson, writer);
		}
	}

	private Map<String, Object> buildTestJson() {
		Map<String, Object> testJson = new LinkedHashMap<>();

		List<Map<String, Object>> segmentsJson = new ArrayList<>();
		testJson.put("id", testId);
		testJson.put("start_point", latLonToJson(startPoint));
		testJson.put("end_point", latLonToJson(endPoint));
		testJson.put("segments", segmentsJson);

		if (routeSegments == null || routeSegments.isEmpty()) {
			return testJson;
		}

		int prevSegment = -1;
		double dist = 0;
		int segmentIndex = 1;
		for (int i = 0; i <= routeSegments.size(); i++) {
			if (i == routeSegments.size() || routeSegments.get(i).getTurnType() != null) {
				if (prevSegment >= 0) {
					RouteSegmentResult segment = routeSegments.get(prevSegment);
					List<RouteSegmentResult> outgoingSegments = new ArrayList<>();
					for (int k = prevSegment; k < i && k < routeSegments.size(); k++) {
						outgoingSegments.add(routeSegments.get(k));
					}
					if (prevSegment == 0)
						segmentIndex = 0;
					if (i != routeSegments.size() || dist >= 10.0)
						appendJunctionShotJson(segmentIndex++, segmentsJson, segment, outgoingSegments, dist);
				}
				prevSegment = i;
				dist = 0;
			}
			if (i < routeSegments.size()) {
				dist += routeSegments.get(i).getDistance();
			}
		}

		return testJson;
	}

	private static void appendJunctionShotJson(int segmentIndex,
			List<Map<String, Object>> segmentsJson,
			RouteSegmentResult segment,
			List<RouteSegmentResult> outgoingSegments,
			double distToNextManeuver
	) {
		TurnType turnType = segment.getTurnType();
		if (turnType == null) {
			return;
		}
		String lanesString = lanesToString(turnType);
		String turn = turnType.toXmlString();

		Map<String, Object> segmentJson = new LinkedHashMap<>();
		segmentJson.put("segment_id", segmentIndex);
		//segmentJson.put("lanes_string", lanesString);
		segmentJson.put("lanes", buildLaneGuidanceJson(turnType));
		segmentJson.put("turn_type", turn);

		Map<String, Object> maneuverJson = new LinkedHashMap<>();
		maneuverJson.put("turn_angle", turnType.getTurnAngle() != 0 ? round2(turnType.getTurnAngle()) : "");
		maneuverJson.put("roundabout_exit", turnType.isRoundAbout() ? turnType.getExitOut() : "");
		//segmentJson.put("maneuver", maneuverJson);
		segmentJson.put("distance_to_next_point_m", round2(distToNextManeuver));

		segmentJson.put("junction", latLonToJson(segment.getEndPoint()));

		RouteSegmentResult firstOutgoingAfterJunction = outgoingSegments != null && outgoingSegments.size() > 1
				? outgoingSegments.get(1) : null;
		//segmentJson.put("turn_deviation_deg", safeDegreesDiff(segment.getBearingEnd(), segment.getBearingBegin()));

		//Map<String, Object> approachSegmentJson = buildRouteSegmentJson(segment, true);
		String approachTurnLanesTag = RouteResultPreparation.getTurnLanesString(segment);
		//segmentJson.put("turn_lanes_tag_count", segment.getObject().getLanes());
		segmentJson.put("turn_lanes_tag", approachTurnLanesTag == null ? "" : approachTurnLanesTag);
		//segmentJson.put("approach_segment", approachSegmentJson);
//		List<Map<String, Object>> outgoingSegmentsJson = new ArrayList<>();
//		if (outgoingSegments != null) {
//			for (RouteSegmentResult outgoingSegment : outgoingSegments) {
//				outgoingSegmentsJson.add(buildRouteSegmentJson(outgoingSegment, false));
//			}
//		}
		//segmentJson.put("outgoing_segments", outgoingSegmentsJson);
		//segmentJson.put("intersection", buildRoadSplitStructureJson(turnType.getRoadSplitStructure()));

		//Map<String, Object> alternativesGeometryJson = new LinkedHashMap<>();
		//List<RouteSegmentResult> attachedRoutesAtJunction = immediateOutgoingSegment != null
		//		? immediateOutgoingSegment.getAttachedRoutes(immediateOutgoingSegment.getStartPointIndex())
		//		: Collections.emptyList();
		//alternativesGeometryJson.put("attached_routes", buildAttachedRoutesJson(attachedRoutesAtJunction, segment, immediateOutgoingSegment));
		//segmentJson.put("alternatives_geometry", alternativesGeometryJson);

		segmentsJson.add(segmentJson);
	}

	private static String lanesToString(TurnType turnType) {
		int[] lanes = turnType == null ? null : turnType.getLanes();
		return lanes == null ? "" : TurnType.lanesToString(lanes);
	}

	private static Map<String, Object> latLonToJson(LatLon point) {
		Map<String, Object> json = new LinkedHashMap<>();
		if (point == null) {
			return json;
		}
		json.put("lat", round5(point.getLatitude()));
		json.put("lon", round5(point.getLongitude()));
		return json;
	}

	private static Object safeDegreesDiff(double a1, double a2) {
		double diff = MapUtils.degreesDiff(a1, a2);
		return round2(diff);
	}

	private static Object round2(double value) {
		if (!Double.isFinite(value)) {
			return "";
		}
		return Math.round(value * 100.0) / 100.0;
	}

	private static double round5(double value) {
		if (!Double.isFinite(value)) {
			return Double.NaN;
		}
		return Math.round(value * 100000.0) / 100000.0;
	}

	private static Map<String, Object> buildRouteSegmentJson(RouteSegmentResult segment, boolean polylineFromEnd) {
		Map<String, Object> json = new LinkedHashMap<>();
		if (segment == null) {
			return json;
		}
		//json.put("is_forward", segment.isForwardDirection());
		json.put("distance_m", round2(segment.getDistance()));
		//json.put("speed_mps", round2(segment.getSegmentSpeed()));
		//json.put("time_s", round2(segment.getSegmentTime()));
		json.put("bearing_begin_deg", round2(segment.getBearingBegin()));
		json.put("bearing_end_deg", round2(segment.getBearingEnd()));
		json.put("highway", segment.getObject().getHighway());
		json.put("oneway", segment.getObject().getOneway());
		json.put("name", segment.getObject().getName());
		return json;
	}

	private static List<Map<String, Object>> buildLaneGuidanceJson(TurnType turnType) {
		int[] lanes = turnType == null ? null : turnType.getLanes();
		List<Map<String, Object>> lanesJson = new ArrayList<>();
		if (lanes != null) {
			for (int laneValue : lanes) {
				Map<String, Object> laneJson = new LinkedHashMap<>();
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
		return lanesJson;
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
			json.put("turn_lanes_string", ai.parsedLanes == null ? "" : TurnType.lanesToString(ai.parsedLanes));
			res.add(json);
		}
		return res;
	}

	private static List<Map<String, Object>> buildAttachedRoutesJson(List<RouteSegmentResult> attachedRoutes, RouteSegmentResult approachSegment, RouteSegmentResult outgoingSegment) {
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
}
