package net.osmand.plus.mapcontextmenu.sections;

import android.app.Activity;

import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;

public class AmenityInfoMenuController extends MenuController {

	public AmenityInfoMenuController(OsmandApplication app, Activity activity, final Amenity amenity) {
		super(new AmenityInfoMenuBuilder(app, amenity), activity);
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
