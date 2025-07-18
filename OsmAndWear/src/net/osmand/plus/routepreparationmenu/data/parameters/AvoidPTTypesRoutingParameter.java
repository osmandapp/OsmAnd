package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_AVOID_PT_TYPES_ID;

import net.osmand.plus.R;

public class AvoidPTTypesRoutingParameter extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_AVOID_PT_TYPES_ID;

	public AvoidPTTypesRoutingParameter() {
		super(null);
	}

	public String getKey() {
		return KEY;
	}

	@Override
	public int getActiveIconId() {
		return R.drawable.ic_action_bus_dark;
	}

	@Override
	public int getDisabledIconId() {
		return R.drawable.ic_action_bus_dark;
	}
}
