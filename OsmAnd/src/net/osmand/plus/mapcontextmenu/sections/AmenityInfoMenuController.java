package net.osmand.plus.mapcontextmenu.sections;

import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;

public class AmenityInfoMenuController extends MenuController {

	public AmenityInfoMenuController(OsmandApplication app, final Amenity amenity) {
		super(new AmenityInfoMenuBuilder(app, amenity));
	}

	@Override
	public int getInitialMenuState() {
		return MenuState.HEADER_ONLY;
	}

	@Override
	public int getSupportedMenuStates() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

}
