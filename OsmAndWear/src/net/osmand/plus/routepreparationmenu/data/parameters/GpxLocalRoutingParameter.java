package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_FOLLOW_TRACK_ID;

import net.osmand.plus.R;

public class GpxLocalRoutingParameter extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_FOLLOW_TRACK_ID;

	public String getKey() {
		return KEY;
	}

	public boolean canAddToRouteMenu() {
		return false;
	}

	public GpxLocalRoutingParameter() {
		super(null);
	}

	@Override
	public int getActiveIconId() {
		return R.drawable.ic_action_polygom_dark;
	}

	@Override
	public int getDisabledIconId() {
		return R.drawable.ic_action_polygom_dark;
	}
}
