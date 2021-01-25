package net.osmand.plus.onlinerouting;

import net.osmand.Location;
import net.osmand.plus.routing.RouteDirectionInfo;

import java.util.List;

public class OnlineRoutingResponse {
	private List<Location> route;
	private List<RouteDirectionInfo> directions;

	public OnlineRoutingResponse(List<Location> route, List<RouteDirectionInfo> directions) {
		this.route = route;
		this.directions = directions;
	}

	public List<Location> getRoute() {
		return route;
	}

	public List<RouteDirectionInfo> getDirections() {
		return directions;
	}
}
