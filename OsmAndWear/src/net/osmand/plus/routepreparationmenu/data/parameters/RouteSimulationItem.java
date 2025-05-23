package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_ROUTE_SIMULATION_ID;

public class RouteSimulationItem extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_ROUTE_SIMULATION_ID;

	public String getKey() {
		return KEY;
	}

	public boolean canAddToRouteMenu() {
		return false;
	}

	public RouteSimulationItem() {
		super(null);
	}
}
