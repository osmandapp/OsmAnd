package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_AVOID_ROADS_ID;

import net.osmand.plus.R;

public class AvoidRoadsRoutingParameter extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_AVOID_ROADS_ID;

	public AvoidRoadsRoutingParameter() {
		super(null);
	}

	public String getKey() {
		return KEY;
	}

	@Override
	public int getActiveIconId() {
		return R.drawable.ic_action_alert;
	}

	@Override
	public int getDisabledIconId() {
		return R.drawable.ic_action_alert;
	}
}
