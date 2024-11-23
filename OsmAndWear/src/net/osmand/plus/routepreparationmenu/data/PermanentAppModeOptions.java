package net.osmand.plus.routepreparationmenu.data;

import static net.osmand.router.GeneralRouter.USE_HEIGHT_OBSTACLES;

import androidx.annotation.NonNull;

import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routepreparationmenu.data.parameters.AvoidPTTypesRoutingParameter;
import net.osmand.plus.routepreparationmenu.data.parameters.AvoidRoadsRoutingParameter;
import net.osmand.plus.routepreparationmenu.data.parameters.MuteSoundRoutingParameter;

import java.util.Arrays;
import java.util.List;

public enum PermanentAppModeOptions {

	CAR(MuteSoundRoutingParameter.KEY, AvoidRoadsRoutingParameter.KEY),
	BICYCLE(MuteSoundRoutingParameter.KEY, RoutingOptionsHelper.DRIVING_STYLE, USE_HEIGHT_OBSTACLES),
	PEDESTRIAN(MuteSoundRoutingParameter.KEY, USE_HEIGHT_OBSTACLES),
	PUBLIC_TRANSPORT(MuteSoundRoutingParameter.KEY, AvoidPTTypesRoutingParameter.KEY),
	BOAT(MuteSoundRoutingParameter.KEY),
	AIRCRAFT(MuteSoundRoutingParameter.KEY),
	SKI(MuteSoundRoutingParameter.KEY, RoutingOptionsHelper.DRIVING_STYLE, USE_HEIGHT_OBSTACLES),
	HORSE(MuteSoundRoutingParameter.KEY),
	OTHER(MuteSoundRoutingParameter.KEY);

	public final List<String> routingParameters;

	PermanentAppModeOptions(@NonNull String... routingParameters) {
		this.routingParameters = Arrays.asList(routingParameters);
	}
}
