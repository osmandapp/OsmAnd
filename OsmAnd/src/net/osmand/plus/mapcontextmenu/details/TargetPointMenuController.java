package net.osmand.plus.mapcontextmenu.details;

import android.graphics.drawable.Drawable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;

public class TargetPointMenuController extends MenuController {

	private TargetPoint targetPoint;

	public TargetPointMenuController(OsmandApplication app, MapActivity mapActivity, final TargetPoint targetPoint) {
		super(new MenuBuilder(app), mapActivity);
		this.targetPoint = targetPoint;
	}

	@Override
	protected int getInitialMenuStatePortrait() {
		return MenuState.HEADER_ONLY;
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN;
	}

	@Override
	public boolean needTypeStr() {
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
	public String getNameStr() {
		return targetPoint.getOriginalPointDescription().getSimpleName(getMapActivity(), false);
	}

	@Override
	public String getTypeStr() {
		return targetPoint.getOnlyName();
	}

	@Override
	public boolean displayStreetNameinTitle() {
		return true;
	}

	@Override
	public boolean hasTitleButton() {
		return true;
	}

	@Override
	public String getTitleButtonCaption() {
		return getMapActivity().getString(R.string.delete_target_point);
	}

	@Override
	public void titleButtonPressed() {
		TargetPointsHelper targetPointsHelper = getMapActivity().getMyApplication().getTargetPointsHelper();
		if(targetPoint.intermediate) {
			targetPointsHelper.removeWayPoint(true, targetPoint.index);
		} else {
			targetPointsHelper.removeWayPoint(true, -1);
		}
		getMapActivity().getContextMenu().close();
	}

	@Override
	public boolean needStreetName() {
		return true;
	}
}
