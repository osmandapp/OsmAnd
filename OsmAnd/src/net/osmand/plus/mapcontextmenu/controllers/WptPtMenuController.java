package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import net.osmand.data.PointDescription;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.WptPtMenuBuilder;
import net.osmand.util.Algorithms;

public class WptPtMenuController extends MenuController {

	private WptPt wpt;

	public WptPtMenuController(MapActivity mapActivity, PointDescription pointDescription, WptPt wpt) {
		super(new WptPtMenuBuilder(mapActivity, wpt), pointDescription, mapActivity);
		this.wpt = wpt;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof WptPt) {
			this.wpt = (WptPt) object;
		}
	}

	@Override
	protected Object getObject() {
		return wpt;
	}

/*
	@Override
	public boolean handleSingleTapOnMap() {
		Fragment fragment = getMapActivity().getSupportFragmentManager().findFragmentByTag(FavoritePointEditor.TAG);
		if (fragment != null) {
			((FavoritePointEditorFragment)fragment).dismiss();
			return true;
		}
		return false;
	}
*/

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return FavoriteImageDrawable.getOrCreate(getMapActivity().getMyApplication(),
				wpt.getColor(ContextCompat.getColor(getMapActivity(), R.color.gpx_color_point)), false);
	}

	@Override
	public Drawable getSecondLineTypeIcon() {
		if (Algorithms.isEmpty(getTypeStr())) {
			return null;
		} else {
			return getIcon(R.drawable.map_small_group);
		}
	}

	@Override
	public String getTypeStr() {
		return wpt.category != null ? wpt.category : "";
	}

	@Override
	public String getCommonTypeStr() {
		return getMapActivity().getString(R.string.gpx_wpt);
	}
}
