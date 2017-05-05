package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.Algorithms;

public class TargetPointMenuController extends MenuController {

	private TargetPoint targetPoint;

	public TargetPointMenuController(MapActivity mapActivity, PointDescription pointDescription, TargetPoint targetPoint) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.targetPoint = targetPoint;
		builder.setShowNearestWiki(true);
		final TargetPointsHelper targetPointsHelper = getMapActivity().getMyApplication().getTargetPointsHelper();
		final int intermediatePointsCount = targetPointsHelper.getIntermediatePoints().size();
		RoutingHelper routingHelper = getMapActivity().getMyApplication().getRoutingHelper();
		final boolean nav = routingHelper.isRoutePlanningMode() || routingHelper.isFollowingMode();
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				TargetPoint tp = getTargetPoint();
				if (tp.start) {
					getMapActivity().getMyApplication().getTargetPointsHelper().clearStartPoint(true);
				} else if (tp.intermediate) {
					targetPointsHelper.removeWayPoint(true, tp.index);
				} else {
					targetPointsHelper.removeWayPoint(true, -1);
				}
				getMapActivity().getContextMenu().close();
				if (nav && intermediatePointsCount == 0 && !tp.start) {
					getMapActivity().getMapActions().stopNavigationWithoutConfirm();
					getMapActivity().getMyApplication().getTargetPointsHelper().clearStartPoint(false);
				}
			}
		};
		if (nav && intermediatePointsCount == 0 && !targetPoint.start) {
			leftTitleButtonController.caption = getMapActivity().getString(R.string.cancel_navigation);
			leftTitleButtonController.leftIconId = R.drawable.ic_action_remove_dark;
		} else {
			leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_remove);
			leftTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;
		}
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof TargetPoint) {
			this.targetPoint = (TargetPoint) object;
		}
	}

	@Override
	protected Object getObject() {
		return targetPoint;
	}

	public TargetPoint getTargetPoint() {
		return targetPoint;
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
		if (targetPoint.start) {
			return getIconOrig(R.drawable.list_startpoint);
		} else if (!targetPoint.intermediate) {
			return getIconOrig(R.drawable.list_destination);
		} else {
			return getIconOrig(R.drawable.list_intermediate);
		}
	}

	@Override
	public String getTypeStr() {
		if (targetPoint.start) {
			return getMapActivity().getString(R.string.starting_point);
		} else {
			return targetPoint.getPointDescription(getMapActivity()).getTypeName();
		}
	}

	@Override
	public boolean needStreetName() {
		return !needTypeStr();
	}
}
