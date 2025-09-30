package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.measurementtool.PlanRoutePoint;
import net.osmand.plus.utils.ColorUtilities;

public class PlanRoutePointMenuController extends MenuController {

	private PlanRoutePoint point;

	public PlanRoutePointMenuController(@NonNull MapActivity mapActivity, PointDescription pointDescription,
	                                    @Nullable PlanRoutePoint point) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.point = point;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof PlanRoutePoint planRoutePoint) {
			this.point = planRoutePoint;
		}
	}

	@Override
	protected Object getObject() {
		return point;
	}

	@Nullable
	@Override
	public Drawable getRightIcon() {
		return getIcon(R.drawable.ic_action_measure_point, ColorUtilities.getActiveColorId(nightMode));
	}

	@NonNull
	@Override
	public String getNameStr() {
		return point.getTitle(getMapActivity());
	}

	@NonNull
	@Override
	public String getTypeStr() {
		return point.getSummary(getMapActivity(), true);
	}

	@Override
	public boolean needStreetName() {
		return false;
	}
}
