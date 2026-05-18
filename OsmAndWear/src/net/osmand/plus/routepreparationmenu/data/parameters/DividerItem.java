package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_DIVIDER_ID;

public class DividerItem extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_DIVIDER_ID;

	public String getKey() {
		return KEY;
	}

	public boolean canAddToRouteMenu() {
		return false;
	}

	public DividerItem() {
		super(null);
	}
}
