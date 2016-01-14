package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.Algorithms;

public class TargetPointMenuController extends MenuController {

	private TargetPoint targetPoint;

	public TargetPointMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, TargetPoint targetPoint) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.targetPoint = targetPoint;
		final TargetPointsHelper targetPointsHelper = getMapActivity().getMyApplication().getTargetPointsHelper();
		final int intermediatePointsCount = targetPointsHelper.getIntermediatePoints().size();
		RoutingHelper routingHelper = getMapActivity().getMyApplication().getRoutingHelper();
		final boolean nav = routingHelper.isRoutePlanningMode() || routingHelper.isFollowingMode();
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				TargetPoint tp = getTargetPoint();
				if(tp.intermediate) {
					targetPointsHelper.removeWayPoint(true, tp.index);
				} else {
					targetPointsHelper.removeWayPoint(true, -1);
				}
				getMapActivity().getContextMenu().close();
				if (nav && intermediatePointsCount == 0) {
					getMapActivity().getMapActions().stopNavigationWithoutConfirm();
				}
			}
		};
		if (nav && intermediatePointsCount == 0) {
			leftTitleButtonController.caption = getMapActivity().getString(R.string.cancel_navigation);
			leftTitleButtonController.leftIconId = R.drawable.ic_action_remove_dark;
		} else {
			leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_delete);
			leftTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;
		}
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof TargetPoint) {
			this.targetPoint = (TargetPoint) object;
		}
	}

	public TargetPoint getTargetPoint() {
		return targetPoint;
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN;
	}

	@Override
	public boolean needTypeStr() {
		return !Algorithms.isEmpty(getNameStr());
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		if (!targetPoint.intermediate) {
			if (isLight()) {
				return getIconOrig(R.drawable.widget_target_day);
			} else {
				return getIconOrig(R.drawable.widget_target_night);
			}
		} else {
			if (isLight()) {
				return getIconOrig(R.drawable.widget_intermediate_day);
			} else {
				return getIconOrig(R.drawable.widget_intermediate_night);
			}
		}
	}

	@Override
	public String getTypeStr() {
		return targetPoint.getPointDescription(getMapActivity()).getTypeName();
	}

	@Override
	public boolean needStreetName() {
		return !needTypeStr();
	}
}
