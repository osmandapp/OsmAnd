package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class RouteDataResources {

	private List<RouteSegmentResult> routeSegments = new LinkedList<>();
	private List<RouteDataObject> routeDataObjects = new LinkedList<>();
	private List<RouteRegion> routeRegions = new LinkedList<>();

	public List<RouteSegmentResult> getRouteSegments() {
		return routeSegments;
	}

	public List<RouteDataObject> getRouteDataObjects() {
		return routeDataObjects;
	}

	public List<RouteRegion> getRouteRegions() {
		return routeRegions;
	}
}
