package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.avoidroads.AvoidRoadInfo;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;

public class ImpassibleRoadsMenuController extends MenuController {

	private AvoidRoadInfo avoidRoadInfo;

	public ImpassibleRoadsMenuController(@NonNull MapActivity mapActivity,
										 @NonNull PointDescription pointDescription,
										 @NonNull AvoidRoadInfo avoidRoadInfo) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.avoidRoadInfo = avoidRoadInfo;
		OsmandApplication app = mapActivity.getMyApplication();
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					app.getAvoidSpecificRoads().removeImpassableRoad(
							ImpassibleRoadsMenuController.this.avoidRoadInfo);
					app.getRoutingHelper().onSettingsChanged();
					activity.getContextMenu().close();
				}
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_remove);
		leftTitleButtonController.startIconId = R.drawable.ic_action_delete_dark;
	}

	@Override
	protected void setObject(Object object) {
		avoidRoadInfo = (AvoidRoadInfo) object;
	}

	@Override
	protected Object getObject() {
		return avoidRoadInfo;
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
			return AppCompatResources.getDrawable(mapActivity, R.drawable.ic_pin_avoid_road);
		} else {
			return null;
		}
	}
}
