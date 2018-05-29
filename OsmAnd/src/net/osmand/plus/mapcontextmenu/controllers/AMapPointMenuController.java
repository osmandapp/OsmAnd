package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

public class AMapPointMenuController extends MenuController {

	private AMapPoint point;

	public AMapPointMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull AMapPoint point) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.point = point;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof AMapPoint) {
			this.point = (AMapPoint) object;
		}
	}

	@Override
	protected Object getObject() {
		return point;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, final LatLon latLon) {
		for (String detail : point.getDetails()) {
			builder.addPlainMenuItem(R.drawable.ic_action_info_dark, detail, true, false, null);
		}
		super.addPlainMenuItems(typeStr, pointDescription, latLon);
	}

	@Override
	public Drawable getRightIcon() {
		return getIcon(R.drawable.ic_action_get_my_location);
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		if (!Algorithms.isEmpty(point.getShortName())) {
			return getIcon(R.drawable.ic_small_group);
		} else {
			return null;
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		if (!Algorithms.isEmpty(point.getTypeName())) {
			return point.getTypeName();
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
		return false;
	}
}