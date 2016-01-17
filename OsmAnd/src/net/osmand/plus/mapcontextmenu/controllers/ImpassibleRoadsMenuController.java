package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

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

	public ImpassibleRoadsMenuController(final OsmandApplication app, final MapActivity mapActivity,
										 PointDescription pointDescription, RouteDataObject route) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.route = route;
		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				app.getDefaultRoutingConfig().removeImpassableRoad(
						ImpassibleRoadsMenuController.this.route);
				RoutingHelper rh = app.getRoutingHelper();
				if (rh.isRouteCalculated() || rh.isRouteBeingCalculated()) {
					rh.recalculateRouteDueToSettingsChange();
				}
				getMapActivity().getContextMenu().close();
			}
		};
		rightTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_remove);
		rightTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;
	}

	@Override
	protected void setObject(Object object) {
		route = (RouteDataObject) object;
	}

	@Override
	public String getTypeStr() {
		return getMapActivity().getString(R.string.road_blocked);
	}

	@Override
	public Drawable getLeftIcon() {
		return getMapActivity().getResources().getDrawable(R.drawable.map_pin_avoid_road);
	}
}
