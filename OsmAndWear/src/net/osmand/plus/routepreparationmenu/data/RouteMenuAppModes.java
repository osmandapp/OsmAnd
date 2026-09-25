package net.osmand.plus.routepreparationmenu.data;

import net.osmand.plus.routepreparationmenu.data.parameters.LocalRoutingParameter;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.List;

public class RouteMenuAppModes {

	public ApplicationMode am;

	public List<LocalRoutingParameter> parameters;

	public RouteMenuAppModes(ApplicationMode am, List<LocalRoutingParameter> parameters) {
		this.am = am;
		this.parameters = parameters;
	}

	public boolean containsParameter(LocalRoutingParameter parameter) {
		for (LocalRoutingParameter p : parameters) {
			if (p.getKey().equals(parameter.getKey())) {
				return true;
			}
		}
		return false;
	}
}
