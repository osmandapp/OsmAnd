package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.drawable.Drawable;

import net.osmand.Location;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;

public class OsMoMenuController extends MenuController {

	private OsMoDevice device;

	public OsMoMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, final OsMoDevice device) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.device = device;
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
		return getPointDescription().getName();
	}

	@Override
	public String getTypeStr() {
		OsmandApplication app = getMapActivity().getMyApplication();
		StringBuilder sb = new StringBuilder();
		final Location l = device.getLastLocation();
		if(l != null && l.hasSpeed()) {
			sb.append(OsmAndFormatter.getFormattedSpeed(l.getSpeed(), app));
			sb.append(" — ");
		}
		Location myLocation = app.getLocationProvider().getLastKnownLocation();
		if (myLocation != null) {
			float dist = myLocation.distanceTo(l);
			sb.append(OsmAndFormatter.getFormattedDistance(dist, app));
		}
		return sb.toString();
	}

	@Override
	public boolean needStreetName() {
		return false;
	}
}
