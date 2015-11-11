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
import net.osmand.util.Algorithms;

public class TargetPointMenuController extends MenuController {

	private TargetPoint targetPoint;

	public TargetPointMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, final TargetPoint targetPoint) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.targetPoint = targetPoint;
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				TargetPointsHelper targetPointsHelper = getMapActivity().getMyApplication().getTargetPointsHelper();
				if(targetPoint.intermediate) {
					targetPointsHelper.removeWayPoint(true, targetPoint.index);
				} else {
					targetPointsHelper.removeWayPoint(true, -1);
				}
				getMapActivity().getContextMenu().close();
			}
		};
		leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_delete);
		leftTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;
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
