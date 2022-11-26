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
	private int currentLocation;

	private final List<Integer> routePointIndexes = new ArrayList<>();

	private final Map<RouteDataObject, int[][]> pointNamesMap = new HashMap<>();

	public RouteDataResources() {
		this.locations = new ArrayList<>();
	}

	public RouteDataResources(List<Location> locations) {
		this.locations = locations;
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

	public Location getLocation(int index) {
		index += currentLocation;
		if (index >= locations.size()) {
			throw new IllegalStateException("Locations index: " + index + " out of bounds");
		}
		return locations.get(index);
	}

	public int getCurrentLocationIndex() {
		return currentLocation;
	}

	public void incrementCurrentLocation(int index) {
		currentLocation += index;
	}

	public Map<RouteDataObject, int[][]> getPointNamesMap() {
		return pointNamesMap;
	}
}
