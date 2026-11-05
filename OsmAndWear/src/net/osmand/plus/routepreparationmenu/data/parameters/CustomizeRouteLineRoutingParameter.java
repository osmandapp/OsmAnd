package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_CUSTOMIZE_ROUTE_LINE_ID;

import net.osmand.plus.R;

public class CustomizeRouteLineRoutingParameter extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_CUSTOMIZE_ROUTE_LINE_ID;

	public CustomizeRouteLineRoutingParameter() {
		super(null);
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public boolean canAddToRouteMenu() {
		return false;
	}

	@Override
	public int getActiveIconId() {
		return R.drawable.ic_action_appearance;
	}

	@Override
	public int getDisabledIconId() {
		return R.drawable.ic_action_appearance;
	}
}
