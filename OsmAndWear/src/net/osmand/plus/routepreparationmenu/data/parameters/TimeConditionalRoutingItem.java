package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_TIME_CONDITIONAL_ID;

import net.osmand.plus.R;

public class TimeConditionalRoutingItem extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_TIME_CONDITIONAL_ID;

	public String getKey() {
		return KEY;
	}

	public boolean canAddToRouteMenu() {
		return false;
	}

	public TimeConditionalRoutingItem() {
		super(null);
	}

	@Override
	public int getActiveIconId() {
		return R.drawable.ic_action_road_works_dark;
	}

	@Override
	public int getDisabledIconId() {
		return R.drawable.ic_action_road_works_dark;
	}
}
