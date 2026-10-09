package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_OTHER_SETTINGS_ID;

import net.osmand.plus.R;

public class OtherSettingsRoutingParameter extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_OTHER_SETTINGS_ID;

	public OtherSettingsRoutingParameter() {
		super(null);
	}

	public String getKey() {
		return KEY;
	}

	public boolean canAddToRouteMenu() {
		return false;
	}

	@Override
	public int getActiveIconId() {
		return R.drawable.ic_action_settings;
	}

	@Override
	public int getDisabledIconId() {
		return R.drawable.ic_action_settings;
	}
}
