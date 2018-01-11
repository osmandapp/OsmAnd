package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.view.View;

import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.views.TransportStopsLayer;
import net.osmand.util.Algorithms;

public class AMapPointMenuController  extends MenuController {

	private AMapPoint point;

	public AMapPointMenuController(MapActivity mapActivity, PointDescription pointDescription, final AMapPoint point) {
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

	@Override
	public String getTypeStr() {
		if (!Algorithms.isEmpty(point.getTypeName())) {
			return point.getTypeName();
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
		return false;
	}
}