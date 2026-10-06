package net.osmand.router;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.map.OsmandRegions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies the profile's existing toll penalties only at locations in selected countries. */
public class CountryTollAvoidanceRouter extends GeneralRouter {

	private static final int MAX_CACHED_LOCATIONS = 8192;

	@FunctionalInterface
	public interface CountrySelector {
		boolean isAvoided(int x31, int y31);
	}

	private final CountrySelector countrySelector;
	private final GeneralRouter tollRouter;
	private final Map<Long, Boolean> avoidedLocations = new LinkedHashMap<Long, Boolean>(256, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
			return size() > MAX_CACHED_LOCATIONS;
		}
	};

	public CountryTollAvoidanceRouter(GeneralRouter router, OsmandRegions regions, Collection<String> countryIds) {
		this(router, createCountrySelector(regions, new HashSet<>(countryIds)));
	}

	public CountryTollAvoidanceRouter(GeneralRouter router, CountrySelector countrySelector) {
		super(router, router.getParameterValues());
		this.countrySelector = countrySelector;
		Map<String, String> tollParameters = new LinkedHashMap<>(router.getParameterValues());
		tollParameters.put(AVOID_TOLL, "true");
		tollRouter = router.build(tollParameters);
		copyImpassableRoads(router, this);
	}

	// GeneralRouter.build() rebuilds profile rules, but does not copy explicitly avoided roads.
	private static GeneralRouter copyImpassableRoads(GeneralRouter source, GeneralRouter target) {
		Set<Long> impassableRoads = new HashSet<>();
		for (long id : source.getImpassableRoadIds()) {
			impassableRoads.add(id);
		}
		target.setImpassableRoads(impassableRoads);
		return target;
	}

	/** A plain router for the initial native/HH search, retaining all other profile restrictions. */
	public GeneralRouter withoutCountryAvoidance() {
		return copyImpassableRoads(this, super.build(getParameterValues()));
	}

	/** Integer polygons passed once per native search; custom test selectors have no native geometry. */
	public int[][] getNativeCountryPolygons() {
		return countrySelector instanceof BoundarySelector ? ((BoundarySelector) countrySelector).polygons : null;
	}

	public GeneralRouter getNativeTollRouter() {
		return tollRouter;
	}

	/** Leave heap for the UI, indexed maps and other threads when native support is unavailable. */
	public long getJavaFallbackMemoryLimit(long configuredLimit) {
		Runtime runtime = Runtime.getRuntime();
		long used = runtime.totalMemory() - runtime.freeMemory();
		long available = runtime.maxMemory() - used - javaHeapReserve(runtime.maxMemory());
		long safeLimit = Math.min(64L << 20, Math.min(runtime.maxMemory() / 4, available / 2));
		if (safeLimit < (8L << 20)) {
			throw new IllegalStateException("Not enough Java heap for country-specific routing; use a native-enabled build");
		}
		return configuredLimit > 0 ? Math.min(configuredLimit, safeLimit) : safeLimit;
	}

	static long javaHeapReserve(long maxHeap) {
		return Math.max(32L << 20, maxHeap / 5);
	}

	static boolean hasJavaHeapHeadroom(long maxHeap, long usedHeap) {
		return maxHeap - usedHeap > javaHeapReserve(maxHeap);
	}

	/** Checks the actual candidate route, including intermediate countries and traversed toll booths. */
	public boolean affectsRoute(List<RouteSegmentResult> route) {
		for (RouteSegmentResult segment : route) {
			RouteDataObject road = segment.getObject();
			boolean forward = segment.isForwardDirection();
			// Only these two tags can change country-specific costs. Avoid evaluating profile
			// rules for every ordinary road and every geometry point of the fast candidate.
			if ("yes".equals(road.getValue("toll"))
					&& defineSpeedPriority(road, forward) != super.defineSpeedPriority(road, forward)) {
				return true;
			}
			int first = Math.min(segment.getStartPointIndex(), segment.getEndPointIndex());
			int last = Math.max(segment.getStartPointIndex(), segment.getEndPointIndex());
			for (int point = first; point <= last; point++) {
				if ("toll_booth".equals(road.getValue(point, "barrier"))
						&& defineRoutingObstacle(road, point, !forward) != super.defineRoutingObstacle(road, point, !forward)) {
					return true;
				}
			}
		}
		return false;
	}

	private static CountrySelector createCountrySelector(OsmandRegions regions, Set<String> countryIds) {
		List<CountryBoundary> boundaries = new ArrayList<>();
		try {
			// Snapshot exact integer polygons once. Point queries must not read the shared regions file.
			for (BinaryMapDataObject object : regions.getRegionBoundaryObjects(countryIds)) {
				boundaries.add(new CountryBoundary(object));
			}
		} catch (IOException e) {
			// Do not silently allow tolls when country-boundary data cannot be read.
			throw new UncheckedIOException("Unable to load country boundaries for toll-road avoidance", e);
		}
		return new BoundarySelector(boundaries);
	}

	private static class BoundarySelector implements CountrySelector {
		private final QuadTree<CountryBoundary> boundaries = new QuadTree<>(
				new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE), 8, 0.55f);
		private final int[][] polygons;

		private BoundarySelector(List<CountryBoundary> objects) {
			polygons = new int[objects.size()][];
			for (int i = 0; i < objects.size(); i++) {
				CountryBoundary boundary = objects.get(i);
				boundaries.insert(boundary, boundary.bounds);
				int[] coordinates = new int[boundary.object.getPointsLength() * 2];
				for (int point = 0; point < boundary.object.getPointsLength(); point++) {
					coordinates[point * 2] = boundary.object.getPoint31XTile(point);
					coordinates[point * 2 + 1] = boundary.object.getPoint31YTile(point);
				}
				polygons[i] = coordinates;
			}
		}

		@Override
		public boolean isAvoided(int x31, int y31) {
			return boundaries.checkIntersection(new QuadRect(x31, y31, x31, y31), -1,
					(point, boundary) -> boundary.bounds.contains(point)
							&& OsmandRegions.contain(boundary.object, x31, y31));
		}
	}

	private static class CountryBoundary {
		private final BinaryMapDataObject object;
		private final QuadRect bounds;

		private CountryBoundary(BinaryMapDataObject object) {
			this.object = object;
			int minX = object.getPoint31XTile(0);
			int minY = object.getPoint31YTile(0);
			int maxX = minX;
			int maxY = minY;
			for (int i = 1; i < object.getPointsLength(); i++) {
				minX = Math.min(minX, object.getPoint31XTile(i));
				minY = Math.min(minY, object.getPoint31YTile(i));
				maxX = Math.max(maxX, object.getPoint31XTile(i));
				maxY = Math.max(maxY, object.getPoint31YTile(i));
			}
			bounds = new QuadRect(minX, minY, maxX, maxY);
		}
	}

	@Override
	public float defineSpeedPriority(RouteDataObject road, boolean dir) {
		// Priority is evaluated per road, so use its middle geometry point; booths use their own location.
		if ("yes".equals(road.getValue("toll")) && road.getPointsLength() > 0
				&& isAvoided(road, road.getPointsLength() / 2)) {
			return tollRouter.defineSpeedPriority(road, dir);
		}
		return super.defineSpeedPriority(road, dir);
	}

	@Override
	public float defineRoutingObstacle(RouteDataObject road, int point, boolean isBackwardDir) {
		if ("toll_booth".equals(road.getValue(point, "barrier")) && isAvoided(road, point)) {
			return tollRouter.defineRoutingObstacle(road, point, isBackwardDir);
		}
		return super.defineRoutingObstacle(road, point, isBackwardDir);
	}

	private boolean isAvoided(RouteDataObject road, int point) {
		int x31 = road.getPoint31XTile(point);
		int y31 = road.getPoint31YTile(point);
		long key = ((long) x31 << 32) | (y31 & 0xffffffffL);
		Boolean avoided = avoidedLocations.get(key);
		if (avoided == null) {
			avoided = countrySelector.isAvoided(x31, y31);
			avoidedLocations.put(key, avoided);
		}
		return avoided;
	}

	@Override
	public GeneralRouter build(Map<String, String> params) {
		return new CountryTollAvoidanceRouter(copyImpassableRoads(this, super.build(params)), countrySelector);
	}

	@Override
	public void clearCaches() {
		super.clearCaches();
		tollRouter.clearCaches();
		avoidedLocations.clear();
	}
}