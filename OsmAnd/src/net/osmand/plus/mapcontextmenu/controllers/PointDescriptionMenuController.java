package net.osmand.plus.mapcontextmenu.controllers;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_AVOID_ROADS_ID;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.TitleButtonController;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.util.Algorithms;

public class PointDescriptionMenuController extends MenuController {

	private boolean hasTypeInDescription;

	public PointDescriptionMenuController(@NonNull MapActivity mapActivity,
	                                      @NonNull PointDescription pointDescription) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		builder.setShowNearestWiki(true);
		initData();

		OsmandApplication app = mapActivity.getApp();
		RoutingHelper routingHelper = app.getRoutingHelper();
		OsmAndAppCustomization customization = app.getAppCustomization();
		if (customization.isFeatureEnabled(CONTEXT_MENU_AVOID_ROADS_ID)
				&& (routingHelper.isRoutePlanningMode() || routingHelper.isFollowingMode())) {
			leftTitleButtonController = new TitleButtonController(this) {
				@Override
				public void buttonPressed() {
					MapActivity activity = getMapActivity();
					if (activity != null) {
						app.getAvoidSpecificRoads().addImpassableRoad(activity, getLatLon(), false, false, null);
					}
				}
			};
			leftTitleButtonController.caption = mapActivity.getString(R.string.avoid_road);
			leftTitleButtonController.startIconId = R.drawable.ic_action_road_works_dark;
		}
	}

	private void initData() {
		hasTypeInDescription = !Algorithms.isEmpty(getPointDescription().getTypeName());
	}

	@Override
	protected void setObject(Object object) {
		initData();
	}

	@Override
	protected Object getObject() {
		return null;
	}

	@Override
	public boolean displayStreetNameInTitle() {
		return true;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getRightIcon() {
		return getIcon(getPointDescription().getItemIcon());
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		if (hasTypeInDescription) {
			return getIcon(R.drawable.ic_action_group_name_16);
		} else {
			return null;
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		if (hasTypeInDescription) {
			return getPointDescription().getTypeName();
		} else {
			return "";
		}
	}

	@NonNull
	@Override
	public String getCommonTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.shared_string_location);
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return !getPointDescription().isAddress();
	}
}
