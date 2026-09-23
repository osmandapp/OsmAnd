package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Moves the route line from the road axis onto the lanes: the lanes suggested for the next maneuver,
 * otherwise the middle of the carriageway, with smooth lane changes in between.
 * The carriageway position follows the map renderer (MapRasterizer_P "realistic roads"): lane placement,
 * lane blocks of branches at splits and merges, and the taper to the own position of a road.
 */
public class RouteLaneLine {

	public static final String REALISTIC_ROADS_ATTR = "realisticRoads";

	// Lane guidance starts to pull the line this far before the maneuver and is fully applied from FULL_DISTANCE
	private static final double GUIDANCE_DISTANCE = 400;
	private static final double FULL_DISTANCE = 150;
	// After a maneuver the line returns to the middle of the carriageway within this distance
	private static final double RELEASE_DISTANCE = 60;
	// A lane change takes at least this many meters along the road per meter across it
	private static final double LANE_CHANGE_SLOPE = 12;
	// Same as the map renderer: a road reaches its own position this far from a joined end
	private static final double SHIFT_TAPER_LENGTH = 100;
	// Same as the map renderer: roads join only when they go on within 40 degrees
	private static final double MAX_JOIN_COS = 0.766;

	private static RouteCalculationResult cachedRoute;
	private static List<Location> cachedLocations;
	private static double[] cachedOffsets;

	public static boolean isEnabled(@NonNull OsmandApplication app) {
		return app.getSettings().getCustomRenderBooleanProperty(REALISTIC_ROADS_ATTR).get();
	}

	/**
	 * Locations of the route line, moved onto the lanes, one per route location.
	 */
	@NonNull
	public static List<Location> getLocations(@NonNull OsmandApplication app, @NonNull RouteCalculationResult route) {
		List<Location> locations = route.getImmutableAllLocations();
		if (!isEnabled(app)) {
			return locations;
		}
		synchronized (RouteLaneLine.class) {
			if (cachedRoute != route || cachedLocations == null) {
				cachedOffsets = calculateOffsets(locations, route.getImmutableAllSegments());
				cachedLocations = cachedOffsets != null ? shiftLocations(locations, cachedOffsets) : locations;
				cachedRoute = route;
			}
			return cachedLocations;
		}
	}

	/**
	 * The given point of the route segment (from locationIndex - 1 to locationIndex) moved onto the lanes.
	 */
	@Nullable
	public static Location shiftProjection(@NonNull OsmandApplication app, @NonNull RouteCalculationResult route,
	                                       @Nullable Location projection, int locationIndex) {
		if (projection == null || !isEnabled(app)) {
			return projection;
		}
		getLocations(app, route);
		double[] offsets;
		synchronized (RouteLaneLine.class) {
			offsets = cachedRoute == route ? cachedOffsets : null;
		}
		List<Location> locations = route.getImmutableAllLocations();
		if (offsets == null || locationIndex <= 0 || locationIndex >= locations.size()) {
			return projection;
		}
		Location from = locations.get(locationIndex - 1);
		Location to = locations.get(locationIndex);
		double length = MapUtils.getDistance(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
		double passed = MapUtils.getDistance(from.getLatitude(), from.getLongitude(), projection.getLatitude(), projection.getLongitude());
		double t = length > 0 ? Math.min(1, passed / length) : 0;
		double offset = offsets[locationIndex - 1] * (1 - t) + offsets[locationIndex] * t;
		Location shifted = new Location(projection);
		shift(shifted, from, to, offset);
		return shifted;
	}

	@Nullable
	private static double[] calculateOffsets(@NonNull List<Location> locations, @NonNull List<RouteSegmentResult> segments) {
		int count = locations.size();
		if (count < 2 || segments.size() != count) {
			return null;
		}
		double[] distances = new double[count];
		for (int i = 1; i < count; i++) {
			Location a = locations.get(i - 1);
			Location b = locations.get(i);
			distances[i] = distances[i - 1] + MapUtils.getDistance(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
		}

		// Roads of the route in order, and the road and point index of every location
		List<Road> roads = new ArrayList<>();
		Road[] locationRoads = new Road[count];
		int[] locationPoints = new int[count];
		for (int i = 0; i < count; i++) {
			RouteSegmentResult segment = segments.get(i);
			Road road = roads.isEmpty() ? null : roads.get(roads.size() - 1);
			if (road == null || road.segment != segment) {
				road = segment != null ? new Road(segment) : null;
				if (road != null) {
					roads.add(road);
				}
			}
			locationRoads[i] = road;
			if (road != null) {
				int step = road.segment.isForwardDirection() ? 1 : -1;
				locationPoints[i] = road.segment.getStartPointIndex() + step * road.passed++;
			}
		}
		for (int i = 1; i < roads.size(); i++) {
			resolveJoin(roads.get(i - 1), roads.get(i));
		}

		// Maneuvers with lane guidance: the lanes apply to the road before the location of the maneuver
		List<Integer> guidanceIndexes = new ArrayList<>();
		List<int[]> guidanceLanes = new ArrayList<>();
		RouteSegmentResult previous = null;
		for (int i = 0; i < count; i++) {
			RouteSegmentResult segment = segments.get(i);
			if (segment != previous && segment != null && i > 0) {
				TurnType turnType = segment.getTurnType();
				int[] lanes = turnType != null ? turnType.getLanes() : null;
				if (lanes != null && lanes.length > 0 && hasActiveLane(lanes)) {
					guidanceIndexes.add(i);
					guidanceLanes.add(lanes);
				}
			}
			previous = segment;
		}

		double[] offsets = new double[count];
		int nextGuidance = 0;
		int lastGuidanceIndex = -1;
		for (int i = 0; i < count; i++) {
			while (nextGuidance < guidanceIndexes.size() && guidanceIndexes.get(nextGuidance) < i) {
				lastGuidanceIndex = guidanceIndexes.get(nextGuidance);
				nextGuidance++;
			}
			Road road = locationRoads[i];
			if (road == null) {
				offsets[i] = i > 0 ? offsets[i - 1] : 0;
				continue;
			}
			double middle = road.shiftAt(locationPoints[i]);
			double target = middle;
			double weight = 0;
			if (nextGuidance < guidanceIndexes.size()) {
				int index = guidanceIndexes.get(nextGuidance);
				double before = distances[index] - distances[i];
				if (before <= GUIDANCE_DISTANCE) {
					target = road.lanesPosition(middle, guidanceLanes.get(nextGuidance));
					weight = smoothstep((GUIDANCE_DISTANCE - before) / (GUIDANCE_DISTANCE - FULL_DISTANCE));
				}
			}
			offsets[i] = middle + (target - middle) * weight;
			if (lastGuidanceIndex >= 0 && weight == 0) {
				// Leave the maneuver lane gradually
				double after = distances[i] - distances[lastGuidanceIndex];
				if (after < RELEASE_DISTANCE && i > 0) {
					offsets[i] = offsets[i - 1] + (middle - offsets[i - 1]) * smoothstep(after / RELEASE_DISTANCE);
				}
			}
		}

		// Limit the lateral speed, in both directions, so that every lane change is smooth
		for (int i = 1; i < count; i++) {
			double max = (distances[i] - distances[i - 1]) / LANE_CHANGE_SLOPE;
			offsets[i] = clamp(offsets[i], offsets[i - 1] - max, offsets[i - 1] + max);
		}
		for (int i = count - 2; i >= 0; i--) {
			double max = (distances[i + 1] - distances[i]) / LANE_CHANGE_SLOPE;
			offsets[i] = clamp(offsets[i], offsets[i + 1] - max, offsets[i + 1] + max);
		}
		return offsets;
	}

	/**
	 * Lines up the lanes of two consecutive roads of the route at their common node, like the map renderer does.
	 */
	private static void resolveJoin(@NonNull Road a, @NonNull Road b) {
		RouteDataObject objectA = a.segment.getObject();
		RouteDataObject objectB = b.segment.getObject();
		if (objectA.getId() == objectB.getId() || !a.oneway || !b.oneway) {
			return;
		}
		int nodeA = a.segment.getEndPointIndex();
		int nodeB = b.segment.getStartPointIndex();
		if (!a.isEnd(nodeA) || !b.isEnd(nodeB)) {
			return;
		}
		double[] dirA = a.directionAt(nodeA, false);
		double[] dirB = b.directionAt(nodeB, true);
		if (dot(dirA, dirB) < MAX_JOIN_COS) {
			return;
		}

		// Other one-way roads that leave the node nearly straight: the route takes a branch of a split
		List<Road> siblings = new ArrayList<>();
		List<double[]> siblingDirections = new ArrayList<>();
		for (RouteSegmentResult attached : b.segment.getAttachedRoutes(nodeB)) {
			RouteDataObject object = attached.getObject();
			if (object.getId() == objectA.getId() || object.getId() == objectB.getId()) {
				continue;
			}
			Road sibling = new Road(attached);
			int node = attached.getStartPointIndex();
			if (!sibling.oneway || !sibling.isEnd(node) || !sibling.isTravelStart(node)) {
				continue;
			}
			double[] direction = sibling.directionAt(node, true);
			if (dot(dirA, direction) >= MAX_JOIN_COS) {
				siblings.add(sibling);
				siblingDirections.add(direction);
			}
		}

		if (!siblings.isEmpty()) {
			// Split: every branch takes its block of the parent lanes, from left to right
			double[] parentRight = {dirA[1], -dirA[0]};
			double side = dot(dirB, parentRight);
			int blockStart = 0;
			for (int i = 0; i < siblings.size(); i++) {
				if (dot(siblingDirections.get(i), parentRight) < side) {
					blockStart += siblings.get(i).lanes;
				}
			}
			b.setShiftAt(nodeB, a.left(a.ownShift) + blockStart * a.laneWidth + b.width() / 2);
			return;
		}

		// A link that joins a wider road merges from the right: it takes the right lanes
		if (a.link && !b.link && b.lanes > a.lanes) {
			a.setShiftAt(nodeA, b.left(b.ownShift) + (b.lanes - a.lanes) * b.laneWidth + a.width() / 2);
			return;
		}

		// One road continues the other: the edges stay in line on one side
		Road wider = a.lanes >= b.lanes ? a : b;
		boolean alignLeft = true;
		if (a.hasPlacement && b.hasPlacement) {
			double leftA = a.left(a.ownShift);
			double leftB = b.left(b.ownShift);
			alignLeft = Math.abs(leftA - leftB) <= Math.abs(leftA + a.width() - leftB - b.width());
		} else {
			String turns = wider.segment.getObject().getValue("turn:lanes");
			if (turns != null) {
				String[] lanes = turns.split("\\|", -1);
				if (lanes[0].contains("merge_to_right") && !lanes[lanes.length - 1].contains("merge_to_left")) {
					alignLeft = false;
				}
			}
		}
		Road anchor = null;
		Road follower = null;
		if (a.hasPlacement) {
			anchor = a;
			follower = b;
		} else if (b.hasPlacement) {
			anchor = b;
			follower = a;
		} else if (a.lanes != b.lanes) {
			anchor = wider;
			follower = wider == a ? b : a;
		}
		if (anchor != null) {
			double anchorLeft = anchor.left(anchor.ownShift);
			double shift = alignLeft
					? anchorLeft + follower.width() / 2
					: anchorLeft + anchor.width() - follower.width() / 2;
			follower.setShiftAt(follower == a ? nodeA : nodeB, shift);
		}
	}

	@NonNull
	private static List<Location> shiftLocations(@NonNull List<Location> locations, @NonNull double[] offsets) {
		int count = locations.size();
		List<Location> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			Location location = new Location(locations.get(i));
			Location from = locations.get(Math.max(0, i - 1));
			Location to = locations.get(Math.min(count - 1, i + 1));
			shift(location, from, to, offsets[i]);
			result.add(location);
		}
		return Collections.unmodifiableList(result);
	}

	private static void shift(@NonNull Location location, @NonNull Location from, @NonNull Location to, double offset) {
		if (offset == 0) {
			return;
		}
		double cos = Math.cos(Math.toRadians(location.getLatitude()));
		double east = (to.getLongitude() - from.getLongitude()) * cos;
		double north = to.getLatitude() - from.getLatitude();
		double length = Math.sqrt(east * east + north * north);
		if (length <= 0) {
			return;
		}
		// Right of the driving direction
		double rightEast = north / length;
		double rightNorth = -east / length;
		double metersPerDegree = 111320.0;
		location.setLatitude(location.getLatitude() + rightNorth * offset / metersPerDegree);
		location.setLongitude(location.getLongitude() + rightEast * offset / (metersPerDegree * cos));
	}

	private static boolean hasActiveLane(@NonNull int[] lanes) {
		for (int lane : lanes) {
			if (lane % 2 == 1) {
				return true;
			}
		}
		return false;
	}

	private static double dot(@NonNull double[] a, @NonNull double[] b) {
		return a[0] * b[0] + a[1] * b[1];
	}

	private static double smoothstep(double t) {
		t = clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * A road of the route with the lanes of the driving direction. Offsets are meters to the right of the
	 * road axis in the driving direction; the shift is the middle of the lanes of the driving direction.
	 */
	private static class Road {

		final RouteSegmentResult segment;
		final int lanes;
		final double laneWidth;
		final boolean oneway;
		final boolean link;
		final boolean hasPlacement;
		final double ownShift;
		// Shift at the first and the last point of the way, as the renderer resolves them
		double firstShift;
		double lastShift;
		double[] wayDistances;
		int passed;

		Road(@NonNull RouteSegmentResult segment) {
			this.segment = segment;
			RouteDataObject road = segment.getObject();
			String highway = road.getHighway();
			if (highway == null) {
				highway = "";
			}
			link = highway.endsWith("_link");
			boolean motorway = highway.startsWith("motorway");
			boolean trunk = highway.startsWith("trunk");
			boolean major = motorway || trunk || highway.startsWith("primary") || highway.startsWith("secondary");
			if (motorway || trunk) {
				laneWidth = 3.5;
			} else if (highway.startsWith("primary") || highway.startsWith("secondary")) {
				laneWidth = 3.3;
			} else if (highway.startsWith("tertiary")) {
				laneWidth = 3.1;
			} else {
				laneWidth = 2.9;
			}
			oneway = road.getOneway() != 0 || road.roundabout() || motorway;
			int tagged = road.getLanes();
			if (oneway) {
				lanes = tagged > 0 ? tagged : (major && !link ? 2 : 1);
				Double line = getPlacementLine(road.getValue("placement"), lanes, laneWidth);
				hasPlacement = line != null;
				ownShift = hasPlacement ? lanes * laneWidth / 2 - line : 0;
			} else {
				// Two-way road centred on the axis: the lanes of the driving direction are on the right
				int total = tagged > 0 ? tagged : 2;
				int backward = total / 2;
				int forward = total - backward;
				boolean alongWay = segment.isForwardDirection();
				lanes = Math.max(1, alongWay ? forward : backward);
				int opposite = alongWay ? backward : forward;
				hasPlacement = false;
				ownShift = (opposite - lanes) * laneWidth / 2 + lanes * laneWidth / 2;
			}
			firstShift = ownShift;
			lastShift = ownShift;
		}

		double width() {
			return lanes * laneWidth;
		}

		double left(double shift) {
			return shift - width() / 2;
		}

		boolean isEnd(int point) {
			return point == 0 || point == segment.getObject().getPointsLength() - 1;
		}

		boolean isTravelStart(int point) {
			return segment.isForwardDirection() ? point == 0 : point == segment.getObject().getPointsLength() - 1;
		}

		void setShiftAt(int point, double shift) {
			if (point == 0) {
				firstShift = shift;
			} else {
				lastShift = shift;
			}
		}

		/**
		 * Unit driving direction (east, north) leaving the point (start) or arriving at it.
		 */
		@NonNull
		double[] directionAt(int point, boolean start) {
			RouteDataObject road = segment.getObject();
			int step = segment.isForwardDirection() ? 1 : -1;
			int other = start ? point + step : point - step;
			other = Math.max(0, Math.min(road.getPointsLength() - 1, other));
			int from = start ? point : other;
			int to = start ? other : point;
			double lat = MapUtils.get31LatitudeY(road.getPoint31YTile(from));
			double east = (MapUtils.get31LongitudeX(road.getPoint31XTile(to)) - MapUtils.get31LongitudeX(road.getPoint31XTile(from)))
					* Math.cos(Math.toRadians(lat));
			double north = MapUtils.get31LatitudeY(road.getPoint31YTile(to)) - lat;
			double length = Math.sqrt(east * east + north * north);
			return length > 0 ? new double[] {east / length, north / length} : new double[] {1, 0};
		}

		/**
		 * Shift at the point of the way: each joined end tapers into the own position, like the renderer.
		 */
		double shiftAt(int point) {
			if (firstShift == ownShift && lastShift == ownShift) {
				return ownShift;
			}
			if (wayDistances == null) {
				RouteDataObject road = segment.getObject();
				int n = road.getPointsLength();
				wayDistances = new double[n];
				for (int i = 1; i < n; i++) {
					wayDistances[i] = wayDistances[i - 1] + MapUtils.measuredDist31(road.getPoint31XTile(i - 1),
							road.getPoint31YTile(i - 1), road.getPoint31XTile(i), road.getPoint31YTile(i));
				}
			}
			point = Math.max(0, Math.min(wayDistances.length - 1, point));
			double total = wayDistances[wayDistances.length - 1];
			double distance = wayDistances[point];
			if (total <= 2 * SHIFT_TAPER_LENGTH) {
				double t = total > 0 ? distance / total : 0;
				return firstShift + (lastShift - firstShift) * smoothstep(t);
			}
			double fromFirst = Math.min(1, distance / SHIFT_TAPER_LENGTH);
			double fromLast = Math.min(1, (total - distance) / SHIFT_TAPER_LENGTH);
			return ownShift + (firstShift - ownShift) * (1 - smoothstep(fromFirst))
					+ (lastShift - ownShift) * (1 - smoothstep(fromLast));
		}

		/**
		 * Middle of the active lanes. When the guidance counts other lanes than this road has, the lanes
		 * keep to the same edge.
		 */
		double lanesPosition(double shift, @NonNull int[] guidance) {
			int first = -1;
			int last = -1;
			for (int i = 0; i < guidance.length; i++) {
				if (guidance[i] % 2 == 1) {
					if (first < 0) {
						first = i;
					}
					last = i;
				}
			}
			if (first < 0) {
				return shift;
			}
			double centre = (first + last + 1) / 2.0;
			double lanePosition;
			if (guidance.length == lanes) {
				lanePosition = centre;
			} else if (last == guidance.length - 1) {
				lanePosition = lanes - (guidance.length - centre);
			} else if (first == 0) {
				lanePosition = centre;
			} else {
				lanePosition = centre / guidance.length * lanes;
			}
			lanePosition = clamp(lanePosition, 0.5, Math.max(0.5, lanes - 0.5));
			return left(shift) + lanePosition * laneWidth;
		}
	}

	/**
	 * Distance from the left edge of the lanes to the road axis, see OSM key "placement".
	 */
	@Nullable
	private static Double getPlacementLine(@Nullable String placement, int lanes, double laneWidth) {
		if (Algorithms.isEmpty(placement)) {
			return null;
		}
		String[] parts = placement.split(":");
		if (parts.length != 2) {
			return null;
		}
		int lane;
		try {
			lane = Integer.parseInt(parts[1].trim());
		} catch (NumberFormatException e) {
			return null;
		}
		if (lane < 1 || lane > lanes) {
			return null;
		}
		switch (parts[0]) {
			case "left_of":
				return (lane - 1) * laneWidth;
			case "middle_of":
				return (lane - 0.5) * laneWidth;
			case "right_of":
				return (double) lane * laneWidth;
			default:
				return null;
		}
	}
}
