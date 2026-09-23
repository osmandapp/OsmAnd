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
 * Used when the map draws roads with their real width (render property "realisticRoads").
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

		// Road carriageway at each location, in the driving frame: meters to the right of the road axis
		Carriageway[] roads = new Carriageway[count];
		for (int i = 0; i < count; i++) {
			roads[i] = Carriageway.of(segments.get(i));
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
			Carriageway road = roads[i];
			double middle = road.middle();
			double target = middle;
			double weight = 0;
			if (nextGuidance < guidanceIndexes.size()) {
				int index = guidanceIndexes.get(nextGuidance);
				double before = distances[index] - distances[i];
				if (before <= GUIDANCE_DISTANCE) {
					target = road.lanesPosition(guidanceLanes.get(nextGuidance));
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

	private static double smoothstep(double t) {
		t = clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * Lanes of the road in the driving direction. Positions are meters to the right of the road axis.
	 */
	private static class Carriageway {

		final int lanes;
		final double laneWidth;
		// Left edge of the lanes of the driving direction
		final double left;

		Carriageway(int lanes, double laneWidth, double left) {
			this.lanes = lanes;
			this.laneWidth = laneWidth;
			this.left = left;
		}

		double middle() {
			return left + lanes * laneWidth / 2;
		}

		/**
		 * Middle of the active lanes. When the guidance counts other lanes than this road has, the lanes
		 * keep to the same edge.
		 */
		double lanesPosition(@NonNull int[] guidance) {
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
				return middle();
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
			return left + lanePosition * laneWidth;
		}

		@NonNull
		static Carriageway of(@Nullable RouteSegmentResult segment) {
			RouteDataObject road = segment != null ? segment.getObject() : null;
			if (road == null) {
				return new Carriageway(1, 3.0, -1.5);
			}
			String highway = road.getHighway();
			if (highway == null) {
				highway = "";
			}
			boolean link = highway.endsWith("_link");
			boolean motorway = highway.startsWith("motorway");
			boolean trunk = highway.startsWith("trunk");
			double laneWidth;
			if (motorway || trunk) {
				laneWidth = 3.5;
			} else if (highway.startsWith("primary") || highway.startsWith("secondary")) {
				laneWidth = 3.3;
			} else if (highway.startsWith("tertiary")) {
				laneWidth = 3.1;
			} else {
				laneWidth = 2.9;
			}
			boolean oneway = road.getOneway() != 0 || road.roundabout() || motorway;
			int lanes = road.getLanes();
			if (oneway) {
				if (lanes <= 0) {
					lanes = (motorway || trunk || highway.startsWith("primary") || highway.startsWith("secondary")) && !link ? 2 : 1;
				}
				double left = -lanes * laneWidth / 2;
				Double placementLine = getPlacementLine(road.getValue("placement"), lanes, laneWidth);
				if (placementLine != null) {
					left = -placementLine;
				}
				return new Carriageway(lanes, laneWidth, left);
			}
			// Two-way road: the lanes of the driving direction are on the right of the axis
			int forward = lanes > 1 ? lanes - lanes / 2 : 1;
			return new Carriageway(forward, laneWidth, 0);
		}

		/**
		 * Distance from the left edge of the lanes to the road axis, see OSM key "placement".
		 */
		@Nullable
		static Double getPlacementLine(@Nullable String placement, int lanes, double laneWidth) {
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
					return lane * laneWidth;
				default:
					return null;
			}
		}
	}
}
