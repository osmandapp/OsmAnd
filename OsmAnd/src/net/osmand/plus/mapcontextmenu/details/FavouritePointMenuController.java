package net.osmand.plus.mapcontextmenu.details;

import android.app.Activity;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;

public class FavouritePointMenuController extends MenuController {

	public FavouritePointMenuController(OsmandApplication app, Activity activity, final FavouritePoint fav) {
		super(new FavouritePointMenuBuilder(app, fav), activity);
	}

	@Override
	protected int getInitialMenuStatePortrait() {
		return MenuState.HEADER_ONLY;
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}
}
