package net.osmand.plus.mapcontextmenu.controllers;

import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;
import net.osmand.binary.OsmandOdb.TransportRouteStop;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController.TransportStopRoute;
import net.osmand.plus.views.TransportStopsLayer;

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
		return this.transportStop.type == null ?
				R.drawable.mx_public_transport : 
				this.transportStop.type.getTopResourceId();
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
	public void onClose() {
		super.onHide();
		TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
		stopsLayer.setRoute(null);
	}
	
	public void onAcquireNewController(PointDescription pointDescription, Object object) {
		if(object instanceof TransportRouteStop) {
			TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
			stopsLayer.setRoute(null);
		}
	};

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, final LatLon latLon) {
		super.addPlainMenuItems(typeStr, pointDescription, latLon);
		List<TransportStop> stops = transportStop.route.getForwardStops();
		boolean useEnglishNames = getMapActivity().getMyApplication().getSettings().usingEnglishNames();
		for (final TransportStop stop : stops) {
			final String name = useEnglishNames ? stop.getEnName(true) : stop.getName();
			boolean currentStop = stop.getName().equals(transportStop.stop.getName()); 
			addPlainMenuItem(currentStop? R.drawable.ic_action_marker_dark : 
						(transportStop.type == null ? R.drawable.mx_route_bus_ref  : transportStop.type.getResourceId()),
					name , false, false, new OnClickListener() {
						
						@Override
						public void onClick(View arg0) {
							MapContextMenu mm = getMapActivity().getContextMenu();
							PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_STOP, 
									getMapActivity().getString(R.string.transport_Stop),
									name);
							TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
							stopsLayer.setRoute(null);
							mm.show(latLon, pd, stop);
						}
					});
		}
	}

}
