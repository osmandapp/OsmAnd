package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.WptPtMenuBuilder;

public class WptPtMenuController extends MenuController {

	private WptPt wpt;

	public WptPtMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, final WptPt wpt) {
		super(new WptPtMenuBuilder(app, wpt), pointDescription, mapActivity);
		this.wpt = wpt;
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
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
	public boolean needTypeStr() {
		return wpt.category != null;
	}

	@Override
	public Drawable getLeftIcon() {
		return FavoriteImageDrawable.getOrCreate(getMapActivity().getMyApplication(), wpt.getColor(), true);
	}

	@Override
	public Drawable getSecondLineIcon() {
		if (wpt.category != null) {
			return getIcon(R.drawable.ic_small_group);
		} else {
			return null;
		}
	}

	@Override
	public String getTypeStr() {
		return wpt.category != null ? wpt.category : getMapActivity().getString(R.string.shared_string_none);
	}

	@Override
	public String getNameStr() {
		return getPointDescription().getName();
	}
}
