package net.osmand.plus.mapcontextmenu.details;

import android.graphics.drawable.Drawable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;

public class OsMoMenuController extends MenuController {

	private OsMoDevice device;

	public OsMoMenuController(OsmandApplication app, MapActivity mapActivity, final OsMoDevice device) {
		super(new MenuBuilder(app), mapActivity);
		this.device = device;
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
	public boolean needTypeStr() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		if (isLight()) {
			return getIconOrig(R.drawable.widget_osmo_connected_location_day);
		} else {
			return getIconOrig(R.drawable.widget_osmo_connected_location_night);
		}
	}

	@Override
	public String getNameStr() {
		return device.getVisibleName();
	}

	@Override
	public String getTypeStr() {
		// todo
		return "";
	}

	@Override
	public boolean needStreetName() {
		return false;
	}
}
