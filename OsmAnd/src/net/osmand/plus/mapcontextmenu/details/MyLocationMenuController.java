package net.osmand.plus.mapcontextmenu.details;

import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;

public class MyLocationMenuController  extends MenuController {

	private PointDescription pointDescription;

	public MyLocationMenuController(OsmandApplication app, MapActivity mapActivity, final PointDescription pointDescription) {
		super(new MenuBuilder(app), mapActivity);
		this.pointDescription = pointDescription;
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
	public Drawable getLeftIcon() {
		ApplicationMode appMode = getMapActivity().getMyApplication().getSettings().getApplicationMode();
		return getMapActivity().getResources().getDrawable(appMode.getResourceLocation());
	}

	@Override
	public String getNameStr() {
		return pointDescription.getTypeName();
	}
}
