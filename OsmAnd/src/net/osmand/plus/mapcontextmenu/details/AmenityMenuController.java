package net.osmand.plus.mapcontextmenu.details;

import android.app.Activity;

import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;

public class AmenityMenuController extends MenuController {

	public AmenityMenuController(OsmandApplication app, Activity activity, final Amenity amenity) {
		super(new AmenityMenuBuilder(app, amenity), activity);
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
