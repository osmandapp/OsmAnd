package net.osmand.plus.mapcontextmenu.controllers;

import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController.TransportStopRoute;

public class TransportRouteController extends MenuController {

	private TransportStopRoute transportStop;

	public TransportRouteController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription,
			TransportStopRoute transportStop) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.transportStop = transportStop;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof TransportStopRoute) {
			this.transportStop = (TransportStopRoute) object;
		}
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public int getLeftIconId() {
		return this.transportStop.type.getTopResourceId();
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public boolean displayDistanceDirection() {
		return false;
	}
	
	@Override
	public boolean fabVisible() {
		return false;
	}
	
	@Override
	public boolean buttonsVisible() {
		return false;
	}
	
	@Override
	public boolean displayStreetNameInTitle() {
		return super.displayStreetNameInTitle();
	}

	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		super.addPlainMenuItems(typeStr, pointDescription, latLon);
		List<TransportStop> stops = transportStop.route.getForwardStops();
		boolean useEnglishNames = getMapActivity().getMyApplication().getSettings().usingEnglishNames();
		for (TransportStop stop : stops) {
			addPlainMenuItem(
					stop == transportStop.stop ? R.drawable.ic_action_marker_dark : transportStop.type.getResourceId(),
					useEnglishNames ? stop.getEnName(true) : stop.getName(), false, false, null);
		}
	}

}
