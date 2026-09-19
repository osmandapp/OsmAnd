package net.osmand.plus.routepreparationmenu.data.parameters;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_LOCAL_ROUTING_GROUP_ID;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class LocalRoutingParameterGroup extends LocalRoutingParameter {

	public static final String KEY = NAVIGATION_LOCAL_ROUTING_GROUP_ID;

	private final String groupName;
	private final List<LocalRoutingParameter> routingParameters = new ArrayList<>();

	public String getKey() {
		if (groupName != null) {
			return groupName;
		}
		return KEY;
	}

	public LocalRoutingParameterGroup(ApplicationMode am, String groupName) {
		super(am);
		this.groupName = groupName;
	}

	public void addRoutingParameter(RoutingParameter routingParameter) {
		LocalRoutingParameter p = new LocalRoutingParameter(getApplicationMode());
		p.routingParameter = routingParameter;
		routingParameters.add(p);
	}

	public String getGroupName() {
		return groupName;
	}

	public List<LocalRoutingParameter> getRoutingParameters() {
		return routingParameters;
	}

	@Override
	public String getText(MapActivity mapActivity) {
		return AndroidUtils.getRoutingStringPropertyName(mapActivity, groupName,
				Algorithms.capitalizeFirstLetterAndLowercase(groupName.replace('_', ' ')));
	}

	@Override
	public boolean isSelected(OsmandSettings settings) {
		return false;
	}

	@Override
	public void setSelected(OsmandSettings settings, boolean isChecked) {
	}

	public LocalRoutingParameter getSelected(OsmandSettings settings) {
		for (LocalRoutingParameter p : routingParameters) {
			if (p.isSelected(settings)) {
				return p;
			}
		}
		return null;
	}
}
