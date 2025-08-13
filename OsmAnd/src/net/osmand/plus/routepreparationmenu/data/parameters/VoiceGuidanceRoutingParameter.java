package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_VOICE_GUIDANCE_ID;

public class VoiceGuidanceRoutingParameter extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_VOICE_GUIDANCE_ID;

	public String getKey() {
		return KEY;
	}

	public VoiceGuidanceRoutingParameter() {
		super(null);
	}
}
