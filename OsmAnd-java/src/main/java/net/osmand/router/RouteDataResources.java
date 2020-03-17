package net.osmand.router;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RouteDataResources {

	private Map<RouteTypeRule, Integer> rules = new LinkedHashMap<>();
	private Map<RouteSegmentResult, Map<RouteTypeRule, RouteTypeRule>> segmentRules = new LinkedHashMap<>();

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

	public Map<RouteTypeRule, RouteTypeRule> getSegmentRules(RouteSegmentResult segmentResult) {
		Map<RouteTypeRule, RouteTypeRule> ruleMap = segmentRules.get(segmentResult);
		if (ruleMap == null) {
			ruleMap = new LinkedHashMap<>();
			segmentRules.put(segmentResult, ruleMap);
		}
		return ruleMap;
	}

	public List<Location> getLocations() {
		return locations;
	}

	public boolean hasLocations() {
		return locations.size() > 0;
	}

	public Location getLocation(int index) {
		index += currentLocation;
		return index < locations.size() ? locations.get(index) : null;
	}

	public void incrementCurrentLocation(int index) {
		currentLocation += index;
	}
}
