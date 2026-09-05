package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_INTERRUPT_MUSIC_ID;

public class InterruptMusicRoutingParameter extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_INTERRUPT_MUSIC_ID;

	public String getKey() {
		return KEY;
	}

	public InterruptMusicRoutingParameter() {
		super(null);
	}
}
