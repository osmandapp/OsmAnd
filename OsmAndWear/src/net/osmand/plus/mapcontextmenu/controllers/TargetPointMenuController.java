package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.Algorithms;

public class TargetPointMenuController extends MenuController {

	private TargetPoint targetPoint;

	public TargetPointMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull TargetPoint targetPoint) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.targetPoint = targetPoint;
		builder.setShowNearestWiki(true);
		OsmandApplication app = mapActivity.getMyApplication();
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		int intermediatePointsCount = targetPointsHelper.getIntermediatePoints().size();
		RoutingHelper routingHelper = app.getRoutingHelper();
		boolean nav = routingHelper.isRoutePlanningMode() || routingHelper.isFollowingMode();
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					TargetPoint tp = getTargetPoint();
					OsmandApplication application = activity.getMyApplication();
					if (tp.start) {
						application.getTargetPointsHelper().clearStartPoint(true);
					} else if (tp.intermediate) {
						targetPointsHelper.removeWayPoint(true, tp.index);
					} else {
						targetPointsHelper.removeWayPoint(true, -1);
					}
					activity.getContextMenu().close();
					if (nav && intermediatePointsCount == 0 && !tp.start) {
						activity.getMapActions().stopNavigationWithoutConfirm();
						application.getTargetPointsHelper().clearStartPoint(false);
					}
				}
			}
		};
		if (nav && intermediatePointsCount == 0 && !targetPoint.start) {
			leftTitleButtonController.caption = mapActivity.getString(R.string.cancel_navigation);
			leftTitleButtonController.startIconId = R.drawable.ic_action_remove_dark;
		} else {
			leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_remove);
			leftTitleButtonController.startIconId = R.drawable.ic_action_delete_dark;
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
	public Drawable getRightIcon() {
		if (targetPoint.start) {
			return getIconOrig(R.drawable.list_startpoint);
		} else if (!targetPoint.intermediate) {
			return getIconOrig(R.drawable.list_destination);
		} else {
			return getIconOrig(R.drawable.list_intermediate);
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (targetPoint.start) {
				return mapActivity.getString(R.string.starting_point);
			} else {
				return targetPoint.getPointDescription(mapActivity).getTypeName();
			}
		} else {
			return "";
		}
	}

	@Override
	public boolean needStreetName() {
		return !needTypeStr();
	}
}
