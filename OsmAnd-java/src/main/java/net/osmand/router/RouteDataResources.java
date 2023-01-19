package net.osmand.router;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RouteDataResources {

	private final Map<RouteTypeRule, Integer> rules = new LinkedHashMap<>();

	private final List<Location> locations;
	private int currentSegmentStartLocationIndex;

	private final List<Integer> routePointIndexes;

	private final Map<RouteDataObject, int[][]> pointNamesMap = new HashMap<>();

	public RouteDataResources() {
		this.locations = new ArrayList<>();
		routePointIndexes = new ArrayList<>();
	}

	public RouteDataResources(List<Location> locations, List<Integer> routePointIndexes) {
		this.locations = locations;
		this.routePointIndexes = routePointIndexes;
	}

	public Map<RouteTypeRule, Integer> getRules() {
		return rules;
	}

	public List<Location> getLocations() {
		return locations;
	}

	public List<Integer> getRoutePointIndexes() {
		return routePointIndexes;
	}

	public Location getCurrentSegmentLocation(int offset) {
		int locationIndex = currentSegmentStartLocationIndex + offset;
		if (locationIndex >= locations.size()) {
			throw new IllegalStateException("Locations index: " + locationIndex + " out of bounds");
		}
		return locations.get(locationIndex);
	}

	public int getCurrentSegmentStartLocationIndex() {
		return currentSegmentStartLocationIndex;
	}

	public void updateNextSegmentStartLocation(int currentSegmentLength) {
		int routePointIndex = routePointIndexes.indexOf(currentSegmentStartLocationIndex + currentSegmentLength);
		boolean overlappingNextRouteSegment =
				!(routePointIndex > 0 && routePointIndex < routePointIndexes.size() - 1);
		currentSegmentStartLocationIndex += overlappingNextRouteSegment
				? currentSegmentLength - 1
				: currentSegmentLength;
	}

	public Map<RouteDataObject, int[][]> getPointNamesMap() {
		return pointNamesMap;
	}
}
