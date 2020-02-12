package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
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

	public ImpassibleRoadsMenuController(@NonNull MapActivity mapActivity,
										 @NonNull PointDescription pointDescription,
										 @NonNull RouteDataObject route) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.route = route;
		final OsmandApplication app = mapActivity.getMyApplication();
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					app.getAvoidSpecificRoads().removeImpassableRoad(
							ImpassibleRoadsMenuController.this.route);
					RoutingHelper rh = app.getRoutingHelper();
					if (rh.isRouteCalculated() || rh.isRouteBeingCalculated()) {
						rh.recalculateRouteDueToSettingsChange();
					}
					activity.getContextMenu().close();
				}
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_remove);
		leftTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;
	}

	@Override
	protected void setObject(Object object) {
		route = (RouteDataObject) object;
	}

	@Override
	protected Object getObject() {
		return route;
	}

	@NonNull
	@Override
	public String getTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.road_blocked);
		} else {
			return "";
		}
	}

	@Override
	public Drawable getRightIcon() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return ContextCompat.getDrawable(mapActivity, R.drawable.map_pin_avoid_road);
		} else {
			return null;
		}
	}
}
