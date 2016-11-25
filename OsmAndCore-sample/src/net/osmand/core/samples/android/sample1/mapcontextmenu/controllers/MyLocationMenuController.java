package net.osmand.core.samples.android.sample1.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.core.samples.android.sample1.MainActivity;
import net.osmand.core.samples.android.sample1.OsmandResources;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MenuBuilder;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MenuController;


public class MyLocationMenuController  extends MenuController {

	public MyLocationMenuController(MainActivity mainActivity, PointDescription pointDescription) {
		super(new MenuBuilder(mainActivity), pointDescription, mainActivity);
		builder.setShowNearestWiki(true);
	}

	@Override
	protected void setObject(Object object) {
	}

	@Override
	protected Object getObject() {
		return getLatLon();
	}

	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
	}

	@Override
	public String getCommonTypeStr() {
		return getMainActivity().getMyApplication().getString("shared_string_location");
	}

	@Override
	public boolean needStreetName() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return OsmandResources.getDrawable("map_pedestrian_location");
	}
}
