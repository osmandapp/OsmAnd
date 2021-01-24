package net.osmand.plus.onlinerouting;

import net.osmand.data.LatLon;
import net.osmand.plus.routing.RouteDirectionInfo;

import java.util.List;

public class OnlineRoutingResponse {
	private List<LatLon> route;
	private List<RouteDirectionInfo> directions;

	public OnlineRoutingResponse(List<LatLon> route, List<RouteDirectionInfo> directions) {
		this.route = route;
		this.directions = directions;
	}

	public List<LatLon> getRoute() {
		return route;
	}

	public List<RouteDirectionInfo> getDirections() {
		return directions;
	}
}
