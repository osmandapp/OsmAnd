package net.osmand.plus.activities;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.resources.TransportIndexRepository.RouteInfoLocation;


public class TransportRouteHelper {
	private static TransportRouteHelper inst = new TransportRouteHelper();
	public static TransportRouteHelper getInstance(){
		return inst;
	}

	private List<RouteInfoLocation> route = new ArrayList<RouteInfoLocation>();
	
	public List<RouteInfoLocation> getRoute() {
		return route;
	}
	
	public boolean routeIsCalculated(){
		if(route.isEmpty()){
			return false;
		}
		if(route.size() == 1 && route.get(0) == null){
			return false;
		}
		return true;
	}
	
	public void setRoute(List<RouteInfoLocation> route) {
		this.route = route;
	}
}
