package net.osmand.plus.mapcontextmenu.controllers;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.resources.TransportIndexRepository;

import java.util.List;

public class TransportStopController extends MenuController {

	private TransportStop transportStop;

	public TransportStopController(OsmandApplication app, MapActivity mapActivity,
								   PointDescription pointDescription, TransportStop transportStop) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.transportStop = transportStop;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof TransportStop) {
			this.transportStop = (TransportStop) object;
		}
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public int getLeftIconId() {
		return R.drawable.mx_public_transport_stop_position;
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public String getTypeStr() {
		return getPointDescription().getTypeName();
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {

		List<TransportIndexRepository> reps = getMapActivity().getMyApplication()
				.getResourceManager().searchTransportRepositories(transportStop.getLocation().getLatitude(),
						transportStop.getLocation().getLongitude());

		boolean useEnglishNames = getMapActivity().getMyApplication().getSettings().usingEnglishNames();

		for (TransportIndexRepository t : reps) {
			if (t.acceptTransportStop(transportStop)) {
				List<String> l;
				if (useEnglishNames) {
					l = t.getRouteDescriptionsForStop(transportStop, "{1} {0} - {3}");
				} else {
					l = t.getRouteDescriptionsForStop(transportStop, "{1} {0} - {2}");
				}
				if (l != null) {
					for (String s : l) {
						addPlainMenuItem(R.drawable.ic_action_polygom_dark, s, false, false);
					}
				}
			}
		}
	}
}
