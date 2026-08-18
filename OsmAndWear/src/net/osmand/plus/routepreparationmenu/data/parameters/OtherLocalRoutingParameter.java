package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_OTHER_LOCAL_ROUTING_ID;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmandSettings;

public class OtherLocalRoutingParameter extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_OTHER_LOCAL_ROUTING_ID;

	public String getKey() {
		return KEY;
	}

	public boolean canAddToRouteMenu() {
		return false;
	}

	public String text;
	public boolean selected;
	public int id;

	public OtherLocalRoutingParameter(int id, String text, boolean selected) {
		super(null);
		this.text = text;
		this.selected = selected;
		this.id = id;
	}

	@Override
	public String getText(MapActivity mapActivity) {
		return text;
	}

	@Override
	public boolean isSelected(OsmandSettings settings) {
		return selected;
	}

	@Override
	public void setSelected(OsmandSettings settings, boolean isChecked) {
		selected = isChecked;
	}
}
