package com.osmand.activities;

import java.util.ArrayList;
import java.util.List;

import com.osmand.TransportIndexRepository.RouteInfoLocation;

public class TransportRouteHelper {
	private static TransportRouteHelper inst = new TransportRouteHelper();
	public static TransportRouteHelper getInstance(){
		return inst;
	}

	private List<RouteInfoLocation> route = new ArrayList<RouteInfoLocation>();
	
	public List<RouteInfoLocation> getRoute() {
		return route;
	}
	
	public void setRoute(List<RouteInfoLocation> route) {
		this.route = route;
	}
}
