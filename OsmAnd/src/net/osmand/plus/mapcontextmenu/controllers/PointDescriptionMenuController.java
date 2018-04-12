package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.search.SearchHistoryFragment;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.Algorithms;

public class PointDescriptionMenuController extends MenuController {

	private boolean hasTypeInDescription;

	public PointDescriptionMenuController(final MapActivity mapActivity, final PointDescription pointDescription) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		builder.setShowNearestWiki(true);
		initData();

		final OsmandApplication app = mapActivity.getMyApplication();
		final RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isRoutePlanningMode() || routingHelper.isFollowingMode()) {
			leftTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					app.getAvoidSpecificRoads().addImpassableRoad(mapActivity, getLatLon(), false, null, false);
				}
			};
			leftTitleButtonController.caption = mapActivity.getString(R.string.avoid_road);
			leftTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_action_road_works_dark, true);
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
		return getIcon(SearchHistoryFragment.getItemIcon(getPointDescription()));
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		if (hasTypeInDescription) {
			return getIcon(R.drawable.ic_small_group);
		} else {
			return null;
		}
	}

	@Override
	public String getTypeStr() {
		if (hasTypeInDescription) {
			return getPointDescription().getTypeName();
		} else {
			return "";
		}
	}

	@Override
	public String getCommonTypeStr() {
		return getMapActivity().getString(R.string.shared_string_location);
	}

	@Override
	public boolean needStreetName() {
		return !getPointDescription().isAddress();
	}
}
