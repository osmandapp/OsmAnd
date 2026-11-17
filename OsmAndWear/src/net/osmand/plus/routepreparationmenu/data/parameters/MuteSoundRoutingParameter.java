package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_SOUND_ID;

import net.osmand.plus.R;

public class MuteSoundRoutingParameter extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_SOUND_ID;

	public MuteSoundRoutingParameter() {
		super(null);
	}

	public String getKey() {
		return KEY;
	}

	@Override
	public int getActiveIconId() {
		return R.drawable.ic_action_volume_up;
	}

	@Override
	public int getDisabledIconId() {
		return R.drawable.ic_action_volume_mute;
	}
}
