package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_ROUTE_CALCULATE_ALTITUDE_ID;

public class CalculateAltitudeItem extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_ROUTE_CALCULATE_ALTITUDE_ID;

	public String getKey() {
		return KEY;
	}

	public boolean canAddToRouteMenu() {
		return false;
	}

	public CalculateAltitudeItem() {
		super(null);
	}
}
