package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_LOCAL_ROUTING_ID;

import androidx.annotation.DrawableRes;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.router.GeneralRouter.RoutingParameter;

public class LocalRoutingParameter {

	public static final String KEY = NAVIGATION_LOCAL_ROUTING_ID;

	public RoutingParameter routingParameter;

	private final ApplicationMode mode;

	@DrawableRes
	public int activeIconId = -1;

	@DrawableRes
	public int disabledIconId = -1;

	public boolean canAddToRouteMenu() {
		return true;
	}

	public String getKey() {
		if (routingParameter != null) {
			return routingParameter.getId();
		}
		return KEY;
	}

	public int getActiveIconId() {
		return activeIconId;
	}

	public int getDisabledIconId() {
		return disabledIconId;
	}

	public LocalRoutingParameter(ApplicationMode mode) {
		this.mode = mode;
	}

	public String getText(MapActivity mapActivity) {
		return AndroidUtils.getRoutingStringPropertyName(mapActivity, routingParameter.getId(),
				routingParameter.getName());
	}

	public boolean isSelected(OsmandSettings settings) {
		CommonPreference<Boolean> property =
				settings.getCustomRoutingBooleanProperty(routingParameter.getId(), routingParameter.getDefaultBoolean());
		if (mode != null) {
			return property.getModeValue(mode);
		} else {
			return property.get();
		}
	}

	public void setSelected(OsmandSettings settings, boolean isChecked) {
		CommonPreference<Boolean> property =
				settings.getCustomRoutingBooleanProperty(routingParameter.getId(), routingParameter.getDefaultBoolean());
		if (mode != null) {
			property.setModeValue(mode, isChecked);
		} else {
			property.set(isChecked);
		}
	}

	public ApplicationMode getApplicationMode() {
		return mode;
	}
}
