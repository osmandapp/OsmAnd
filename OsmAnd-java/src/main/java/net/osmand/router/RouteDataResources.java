package net.osmand.router;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RouteDataResources {

	private Map<RouteTypeRule, Integer> rules = new LinkedHashMap<>();

	private List<Location> locations;
	private int currentLocation;

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

	public boolean hasLocations() {
		return locations.size() > 0;
	}

	public Location currentLocation() {
		return currentLocation < locations.size() ? locations.get(currentLocation) : null;
	}

	public Location nextLocation() {
		currentLocation++;
		return currentLocation < locations.size() ? locations.get(currentLocation) : null;
	}
}
