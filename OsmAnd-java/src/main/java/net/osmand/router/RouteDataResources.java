package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;

import java.util.LinkedHashMap;
import java.util.Map;

public class RouteDataResources {

	private Map<RouteTypeRule, Integer> rules = new LinkedHashMap<>();

	public Map<RouteTypeRule, Integer> getRules() {
		return rules;
	}

}
