package net.osmand.plus.mapcontextmenu.controllers;

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
import net.osmand.plus.mapcontextmenu.MapContextMenuFragment;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController.TransportStopRoute;
import net.osmand.plus.views.TransportStopsLayer;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarView;
import net.osmand.plus.views.mapwidgets.MapInfoWidgetsFactory.TopToolbarViewController;

import java.lang.ref.WeakReference;
import java.util.List;

public class TransportRouteController extends MenuController {

	private TransportStopRoute transportStop;

	public TransportRouteController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription,
									TransportStopRoute transportStop) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.transportStop = transportStop;
		toolbarController = new TransportRouteToolbarController();
		toolbarController.setTitle(getNameStr());
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
		super.onClose();
		TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
		stopsLayer.setRoute(null);
	}

	public void onAcquireNewController(PointDescription pointDescription, Object object) {
		if (object instanceof TransportRouteStop) {
			TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
			stopsLayer.setRoute(null);
		}
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, final LatLon latLon) {
		super.addPlainMenuItems(typeStr, pointDescription, latLon);
		List<TransportStop> stops = transportStop.route.getForwardStops();
		boolean useEnglishNames = getMapActivity().getMyApplication().getSettings().usingEnglishNames();
		int currentStop = -1;
		for (int i = 0; i < stops.size(); i++) {
			final TransportStop stop = stops.get(i);
			if (stop.getName().equals(transportStop.stop.getName())) {
				currentStop = i;
				break;
			}
		}
		int defaultIcon = transportStop.type == null ? R.drawable.mx_route_bus_ref : transportStop.type.getResourceId();
		int startPosition = 0;
		if (!transportStop.showWholeRoute) {
			startPosition = (currentStop == -1 ? 0 : currentStop);
			if (currentStop > 0) {
				addPlainMenuItem(defaultIcon, getMapActivity().getString(R.string.route_stops_before, currentStop),
						false, false, new OnClickListener() {

							@Override
							public void onClick(View arg0) {
								MapContextMenu mm = getMapActivity().getContextMenu();
								transportStop.showWholeRoute = true;
								mm.showOrUpdate(latLon, getPointDescription(), transportStop);
								TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
								int cz = transportStop.calculateZoom(0, getMapActivity().getMapView().getCurrentRotatedTileBox());
								getMapActivity().changeZoom(cz - getMapActivity().getMapView().getZoom());
								stopsLayer.setRoute(transportStop.route);
							}
						});
			}
		}
		for (int i = startPosition; i < stops.size(); i++) {
			final TransportStop stop = stops.get(i);
			final String name = useEnglishNames ? stop.getEnName(true) : stop.getName();
			addPlainMenuItem(currentStop == i ? R.drawable.ic_action_marker_dark : defaultIcon,
					name, false, false, new OnClickListener() {

						@Override
						public void onClick(View arg0) {
							MapContextMenu mm = getMapActivity().getContextMenu();
							PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_STOP,
									getMapActivity().getString(R.string.transport_Stop), name);
							TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
							stopsLayer.setRoute(null);
							mm.show(stop.getLocation(), pd, stop);
							WeakReference<MapContextMenuFragment> rr = mm.findMenuFragment();
							if (rr != null && rr.get() != null) {
								rr.get().centerMarkerLocation();
							}
						}
					});
		}
	}

	public static class TransportRouteToolbarController extends TopToolbarViewController {

		public TransportRouteToolbarController() {
			super(TopToolbarViewControllerType.CONTEXT_MENU);
		}

		@Override
		public void onBackPressed(TopToolbarView view) {
			view.getMap().getContextMenu().close();
		}

		@Override
		public void onTitlePressed(TopToolbarView view) {

		}

		@Override
		public void onClosePressed(TopToolbarView view) {
			view.getMap().getContextMenu().close();
		}
	}
}
