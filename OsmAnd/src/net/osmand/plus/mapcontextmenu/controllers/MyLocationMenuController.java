package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;

public class MyLocationMenuController  extends MenuController {

	public MyLocationMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
	}

	@Override
	protected void setObject(Object object) {
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN;
	}

	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
	}

	@Override
	public String getCommonTypeStr() {
		return getMapActivity().getString(R.string.shared_string_location);
	}

	@Override
	public boolean needStreetName() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		ApplicationMode appMode = getMapActivity().getMyApplication().getSettings().getApplicationMode();
		return getMapActivity().getResources().getDrawable(appMode.getResourceLocationDay());
	}
}
