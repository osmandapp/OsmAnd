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
 * Draws the route line in one lane of the road: it keeps its lane, and changes lanes only when the lane ends
 * or the lane guidance of the next maneuver asks for other lanes; every lane change takes LANE_CHANGE_LENGTH.
 * The carriageway position follows the map renderer (MapRasterizer_P "realistic roads"): lane placement,
 * lane blocks of branches at splits and merges, and the taper to the own position of a road.
 * <p>
 * The line has more points than the route (lane changes need them): route location indexes are converted
 * with {@link #toLineIndex}.
 */
public class RouteLaneLine {

	public static final String REALISTIC_ROADS_ATTR = "realisticRoads";

	// The lanes of a maneuver are taken this far before it
	private static final double GUIDANCE_DISTANCE = 250;
	// The first lane of the route is chosen for a maneuver within this distance
	private static final double START_GUIDANCE_DISTANCE = 2000;
	private static final double LANE_CHANGE_LENGTH = 50;
	private static final double LANE_CHANGE_STEP = 5;
	// Offset changes of the road position are drawn with points this far apart
	private static final double SHIFT_STEP = 10;
	// Same as the map renderer: a road reaches its own position this far from a joined end
	private static final double SHIFT_TAPER_LENGTH = 100;
	// Same as the map renderer: roads join only when they go on within 40 degrees
	private static final double MAX_JOIN_COS = 0.766;

	private static RouteCalculationResult cachedRoute;
	private static LaneLine cachedLine;

	private static class LaneLine {
		// Line points, the route locations are among them
		List<Location> locations;
		double[] distances;
		double[] offsets;
		// Line index of every route location
		int[] lineIndexes;
		// Route locations moved onto the lanes
		List<Location> routeLocations;
	}

	public static boolean isEnabled(@NonNull OsmandApplication app) {
		return app.getSettings().getCustomRenderBooleanProperty(REALISTIC_ROADS_ATTR).get();
	}

	@Nullable
	private static LaneLine getLine(@NonNull OsmandApplication app, @NonNull RouteCalculationResult route) {
		if (!isEnabled(app)) {
			return null;
		}
		synchronized (RouteLaneLine.class) {
			if (cachedRoute != route) {
				cachedLine = buildLine(route.getImmutableAllLocations(), route.getImmutableAllSegments());
				cachedRoute = route;
			}
			return cachedLine;
		}
	}

	/**
	 * Points of the route line on the lanes.
	 */
	@NonNull
	public static List<Location> getLineLocations(@NonNull OsmandApplication app, @NonNull RouteCalculationResult route) {
		LaneLine line = getLine(app, route);
		return line != null ? line.locations : route.getImmutableAllLocations();
	}

	/**
	 * Route locations moved onto the lanes, one per route location.
	 */
	@NonNull
	public static List<Location> getLocations(@NonNull OsmandApplication app, @NonNull RouteCalculationResult route) {
		LaneLine line = getLine(app, route);
		return line != null ? line.routeLocations : route.getImmutableAllLocations();
	}

	/**
	 * Index in {@link #getLineLocations} of the route location.
	 */
	public static int toLineIndex(@NonNull OsmandApplication app, @NonNull RouteCalculationResult route, int locationIndex) {
		LaneLine line = getLine(app, route);
		if (line == null || locationIndex < 0 || locationIndex >= line.lineIndexes.length) {
			return locationIndex;
		}
		return line.lineIndexes[locationIndex];
	}

	/**
	 * Index in {@link #getLineLocations} of the first line point after the projection, which lies on the route
	 * segment from locationIndex - 1 to locationIndex.
	 */
	public static int toLineIndex(@NonNull OsmandApplication app, @NonNull RouteCalculationResult route,
	                              @Nullable Location projection, int locationIndex) {
		LaneLine line = getLine(app, route);
		if (line == null || locationIndex <= 0 || locationIndex >= line.lineIndexes.length) {
			return toLineIndex(app, route, locationIndex);
		}
		double distance = projectionDistance(route, line, projection, locationIndex);
		int index = line.lineIndexes[locationIndex - 1] + 1;
		while (index < line.lineIndexes[locationIndex] && line.distances[index] <= distance) {
			index++;
		}
		return index;
	}

	/**
	 * The given point of the route segment (from locationIndex - 1 to locationIndex) moved onto the lanes.
	 */
	@Nullable
	public static Location shiftProjection(@NonNull OsmandApplication app, @NonNull RouteCalculationResult route,
	                                       @Nullable Location projection, int locationIndex) {
		LaneLine line = projection != null ? getLine(app, route) : null;
		List<Location> locations = route.getImmutableAllLocations();
		if (line == null || locationIndex <= 0 || locationIndex >= locations.size()) {
			return projection;
		}
		double distance = projectionDistance(route, line, projection, locationIndex);
		int from = line.lineIndexes[locationIndex - 1];
		int to = line.lineIndexes[locationIndex];
		double offset = line.offsets[to];
		for (int i = from; i < to; i++) {
			if (distance <= line.distances[i + 1]) {
				double length = line.distances[i + 1] - line.distances[i];
				double t = length > 0 ? clamp((distance - line.distances[i]) / length, 0, 1) : 0;
				offset = line.offsets[i] + (line.offsets[i + 1] - line.offsets[i]) * t;
				break;
			}
		}
		Location shifted = new Location(projection);
		shift(shifted, locations.get(locationIndex - 1), locations.get(locationIndex), offset);
		return shifted;
	}

	private static double projectionDistance(@NonNull RouteCalculationResult route, @NonNull LaneLine line,
	                                         @Nullable Location projection, int locationIndex) {
		int from = line.lineIndexes[locationIndex - 1];
		if (projection == null) {
			return line.distances[from];
		}
		Location start = route.getImmutableAllLocations().get(locationIndex - 1);
		return line.distances[from] + MapUtils.getDistance(start.getLatitude(), start.getLongitude(),
				projection.getLatitude(), projection.getLongitude());
	}

	@Nullable
	private static LaneLine buildLine(@NonNull List<Location> locations, @NonNull List<RouteSegmentResult> segments) {
		int count = locations.size();
		if (count < 2 || segments.size() != count) {
			return null;
		}
		double[] distances = new double[count];
		for (int i = 1; i < count; i++) {
			distances[i] = distances[i - 1] + distance(locations.get(i - 1), locations.get(i));
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
				locationPoints[i] = road.segment.getStartPointIndex() + road.step() * road.passed++;
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

		// One lane per location: keep the lane, change it only when it ends or the guidance asks for others
		int[] laneOf = new int[count];
		double[] shifts = new double[count];
		double[] offsets = new double[count];
		// Lane change completed at a location: how far the line moves across the road there
		double[] changes = new double[count];
		int nextGuidance = 0;
		for (int i = 0; i < count; i++) {
			while (nextGuidance < guidanceIndexes.size() && guidanceIndexes.get(nextGuidance) < i) {
				nextGuidance++;
			}
			Road road = locationRoads[i];
			if (road == null) {
				laneOf[i] = i > 0 ? laneOf[i - 1] : 0;
				offsets[i] = i > 0 ? offsets[i - 1] : 0;
				continue;
			}
			double shift = road.shiftAt(locationPoints[i]);
			shifts[i] = shift;
			int[] block = null;
			double guidanceDistance = Double.MAX_VALUE;
			if (nextGuidance < guidanceIndexes.size()) {
				guidanceDistance = distances[guidanceIndexes.get(nextGuidance)] - distances[i];
				block = road.lanesBlock(guidanceLanes.get(nextGuidance));
			}
			int lane;
			if (i == 0 || locationRoads[i - 1] == null) {
				lane = block != null && guidanceDistance <= START_GUIDANCE_DISTANCE ? block[1] : road.lanes - 1;
			} else {
				// The lane at the same place across the road
				lane = road.laneAt(shift, offsets[i - 1]);
			}
			int kept = lane;
			if (block != null && guidanceDistance <= GUIDANCE_DISTANCE && (lane < block[0] || lane > block[1])) {
				lane = lane < block[0] ? block[0] : block[1];
			}
			laneOf[i] = lane;
			offsets[i] = road.laneCentre(shift, lane);
			if (i > 0) {
				double keptOffset = road.laneCentre(shift, kept);
				// Lane ended at this node: the previous lane is off this road
				double previousOffset = offsets[i - 1];
				boolean laneEnded = Math.abs(keptOffset - previousOffset) > road.laneWidth * 0.75
						&& locationRoads[i - 1] != road;
				changes[i] = (offsets[i] - keptOffset) + (laneEnded ? keptOffset - previousOffset : 0);
			}
		}

		// Line points: route locations plus points where the position changes along a segment
		List<Location> linePoints = new ArrayList<>();
		List<Double> lineDistances = new ArrayList<>();
		List<Double> lineOffsets = new ArrayList<>();
		int[] lineIndexes = new int[count];
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				double from = distances[i - 1];
				double length = distances[i] - from;
				double changeStart = distances[i] - LANE_CHANGE_LENGTH;
				double start = offsets[i - 1];
				double end = offsets[i] - changes[i];
				boolean changing = changes[i] != 0;
				boolean moving = Math.abs(end - start) > 0.2;
				double step = changing ? LANE_CHANGE_STEP : SHIFT_STEP;
				if ((changing || moving) && length > step) {
					int parts = (int) Math.min(200, Math.ceil(length / step));
					for (int k = 1; k < parts; k++) {
						double t = (double) k / parts;
						double d = from + length * t;
						double offset = start + (end - start) * t;
						if (changing && d > changeStart) {
							offset += changes[i] * smoothstep((d - changeStart) / LANE_CHANGE_LENGTH);
						}
						linePoints.add(interpolate(locations.get(i - 1), locations.get(i), t));
						lineDistances.add(d);
						lineOffsets.add(offset);
					}
				}
			}
			lineIndexes[i] = linePoints.size();
			linePoints.add(new Location(locations.get(i)));
			lineDistances.add(distances[i]);
			lineOffsets.add(offsets[i]);
		}
		// Lane changes that start before the previous route location
		for (int i = 1; i < count; i++) {
			if (changes[i] == 0) {
				continue;
			}
			double changeStart = distances[i] - LANE_CHANGE_LENGTH;
			for (int j = lineIndexes[i - 1]; j >= 0 && lineDistances.get(j) > changeStart; j--) {
				double d = lineDistances.get(j);
				lineOffsets.set(j, lineOffsets.get(j) + changes[i] * smoothstep((d - changeStart) / LANE_CHANGE_LENGTH));
			}
		}

		int size = linePoints.size();
		LaneLine line = new LaneLine();
		line.distances = new double[size];
		line.offsets = new double[size];
		List<Location> shifted = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			line.distances[i] = lineDistances.get(i);
			line.offsets[i] = lineOffsets.get(i);
			Location location = new Location(linePoints.get(i));
			shift(location, linePoints.get(Math.max(0, i - 1)), linePoints.get(Math.min(size - 1, i + 1)), line.offsets[i]);
			shifted.add(location);
		}
		line.locations = Collections.unmodifiableList(shifted);
		line.lineIndexes = lineIndexes;
		List<Location> routeLocations = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			routeLocations.add(shifted.get(lineIndexes[i]));
		}
		line.routeLocations = Collections.unmodifiableList(routeLocations);
		return line;
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

		// Split without the other branches known: the lane guidance of the maneuver, or the side the branch
		// leaves to, gives the block of the parent lanes the branch takes
		boolean branch = a.link != b.link || (a.link && b.link);
		if (a.lanes > b.lanes) {
			int blockStart = guidanceBlockStart(b.segment.getTurnType(), a.lanes, b.lanes);
			if (blockStart < 0 && branch) {
				boolean right = dot(dirB, new double[] {dirA[1], -dirA[0]}) > 0;
				blockStart = right ? a.lanes - b.lanes : 0;
			}
			if (blockStart >= 0) {
				b.setShiftAt(nodeB, a.left(a.ownShift) + blockStart * a.laneWidth + b.width() / 2);
				return;
			}
		}
		// Merge: the branch takes the block on the side it comes from
		if (a.lanes < b.lanes && branch) {
			boolean fromLeft = dot(dirA, new double[] {dirB[1], -dirB[0]}) > 0;
			int blockStart = fromLeft ? 0 : b.lanes - a.lanes;
			a.setShiftAt(nodeA, b.left(b.ownShift) + blockStart * b.laneWidth + a.width() / 2);
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

	/**
	 * First parent lane of the branch from the active lanes of the maneuver, -1 when they do not tell.
	 */
	private static int guidanceBlockStart(@Nullable TurnType turn, int parentLanes, int branchLanes) {
		int[] lanes = turn != null ? turn.getLanes() : null;
		if (lanes == null || lanes.length != parentLanes) {
			return -1;
		}
		int first = -1;
		int last = -1;
		for (int i = 0; i < lanes.length; i++) {
			if (lanes[i] % 2 == 1) {
				if (first < 0) {
					first = i;
				}
				last = i;
			}
		}
		if (first < 0) {
			return -1;
		}
		if (last - first + 1 == branchLanes) {
			return first;
		}
		if (last == parentLanes - 1) {
			return parentLanes - branchLanes;
		}
		return first == 0 ? 0 : -1;
	}

	@NonNull
	private static Location interpolate(@NonNull Location a, @NonNull Location b, double t) {
		// A plain line point: the first and the last route locations have a provider of their own
		Location location = new Location("");
		location.setLatitude(a.getLatitude() + (b.getLatitude() - a.getLatitude()) * t);
		location.setLongitude(a.getLongitude() + (b.getLongitude() - a.getLongitude()) * t);
		return location;
	}

	private static double distance(@NonNull Location a, @NonNull Location b) {
		return MapUtils.getDistance(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
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

		int step() {
			return segment.isForwardDirection() ? 1 : -1;
		}

		double width() {
			return lanes * laneWidth;
		}

		double left(double shift) {
			return shift - width() / 2;
		}

		double laneCentre(double shift, int lane) {
			return left(shift) + (lane + 0.5) * laneWidth;
		}

		/**
		 * The lane under the offset, the nearest one when the offset is off the road.
		 */
		int laneAt(double shift, double offset) {
			int lane = (int) Math.floor((offset - left(shift)) / laneWidth);
			return Math.max(0, Math.min(lanes - 1, lane));
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
			int other = start ? point + step() : point - step();
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
		 * Active lanes of the guidance on this road, first and last from the left. When the guidance counts
		 * other lanes than this road has, the lanes keep to the same edge.
		 */
		@Nullable
		int[] lanesBlock(@NonNull int[] guidance) {
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
				return null;
			}
			if (guidance.length != lanes) {
				if (last == guidance.length - 1) {
					int move = lanes - guidance.length;
					first += move;
					last += move;
				} else if (first != 0) {
					first = (int) Math.floor((double) first / guidance.length * lanes);
					last = (int) Math.ceil((double) (last + 1) / guidance.length * lanes) - 1;
				}
			}
			first = Math.max(0, Math.min(lanes - 1, first));
			last = Math.max(first, Math.min(lanes - 1, last));
			return new int[] {first, last};
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
