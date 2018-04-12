package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.routing.RoutingHelper;

public class ImpassibleRoadsMenuController extends MenuController {

	private RouteDataObject route;

	public ImpassibleRoadsMenuController(final MapActivity mapActivity,
										 PointDescription pointDescription, RouteDataObject route) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.route = route;
		final OsmandApplication app = mapActivity.getMyApplication();
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				app.getAvoidSpecificRoads().removeImpassableRoad(
						ImpassibleRoadsMenuController.this.route);
				RoutingHelper rh = app.getRoutingHelper();
				if (rh.isRouteCalculated() || rh.isRouteBeingCalculated()) {
					rh.recalculateRouteDueToSettingsChange();
				}
				getMapActivity().getContextMenu().close();
			}
		};
		leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_remove);
		leftTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_action_delete_dark, true);
	}

	@Override
	protected void setObject(Object object) {
		route = (RouteDataObject) object;
	}

	@Override
	protected Object getObject() {
		return route;
	}

	@Override
	public String getTypeStr() {
		return getMapActivity().getString(R.string.road_blocked);
	}

	@Override
	public Drawable getRightIcon() {
		return ContextCompat.getDrawable(getMapActivity(), R.drawable.map_pin_avoid_road);
	}
}
