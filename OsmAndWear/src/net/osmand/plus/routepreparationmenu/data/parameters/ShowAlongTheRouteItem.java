package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_SHOW_ALONG_THE_ROUTE_ID;

import net.osmand.plus.R;

public class ShowAlongTheRouteItem extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_SHOW_ALONG_THE_ROUTE_ID;

	public ShowAlongTheRouteItem() {
		super(null);
	}

	public String getKey() {
		return KEY;
	}

	@Override
	public int getActiveIconId() {
		return R.drawable.ic_action_show_along_route;
	}

	@Override
	public int getDisabledIconId() {
		return R.drawable.ic_action_show_along_route;
	}
}
