package net.osmand.plus.mapcontextmenu.controllers;

import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;

import net.osmand.binary.OsmandOdb.TransportRouteStop;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.TransportStop;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.views.TransportStopsLayer;

import java.util.List;

public class TransportRouteController extends MenuController {

	private TransportStopRoute transportRoute;

	public TransportRouteController(final MapActivity mapActivity, PointDescription pointDescription,
									final TransportStopRoute transportRoute) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.transportRoute = transportRoute;
		builder.setShowOnlinePhotos(false);
		toolbarController = new ContextMenuToolbarController(this);
		toolbarController.setTitle(getNameStr());
		toolbarController.setOnBackButtonClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.getContextMenu().backToolbarAction(TransportRouteController.this);
			}
		});
		toolbarController.setOnTitleClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showMenuAndRoute(getLatLon(), true);
			}
		});
		toolbarController.setOnCloseButtonClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapActivity.getContextMenu().closeToolbar(TransportRouteController.this);
			}
		});

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				final int previousStop = getPreviousStop();
				if (previousStop != -1) {
					showTransportStop(transportRoute.route.getForwardStops().get(previousStop));
				}
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_previous);
		updateLeftTitleButtonIcon();

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				final int nextStop = getNextStop();
				if (nextStop != -1) {
					showTransportStop(transportRoute.route.getForwardStops().get(nextStop));
				}
			}
		};
		rightTitleButtonController.caption = mapActivity.getString(R.string.shared_string_next);
		updateRightTitleButtonIcon();
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof TransportStopRoute) {
			this.transportRoute = (TransportStopRoute) object;
		}
	}

	@Override
	public boolean navigateInPedestrianMode() {
		return true;
	}

	@Override
	protected Object getObject() {
		return transportRoute;
	}

	@Override
	public int getRightIconId() {
		return this.transportRoute.type == null ?
				R.drawable.mx_public_transport :
				this.transportRoute.type.getTopResourceId();
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
	public boolean isClosable() {
		return false;
	}

	@Override
	public boolean buttonsVisible() {
		return false;
	}

	@Override
	public String getTypeStr() {
		return getPointDescription().getName();
	}

	private String getStopType() {
		return getMapActivity().getString(transportRoute.getTypeStrRes()) + " " + getMapActivity().getString(R.string.transport_Stop).toLowerCase();
	}

	@Override
	public void updateData() {
		super.updateData();
		updateControllers();
	}

	@Override
	public void onShow() {
		super.onShow();
		showRoute();
	}

	@Override
	public void onClose() {
		super.onClose();
		resetRoute();
	}

	private void updateLeftTitleButtonIcon() {
		leftTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_arrow_back, true);
	}

	private void updateRightTitleButtonIcon() {
		rightTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_arrow_forward, false);
	}

	public void onAcquireNewController(PointDescription pointDescription, Object object) {
		if (object instanceof TransportRouteStop) {
			resetRoute();
		}
	}

	private void updateControllers() {
		boolean previousStopEnabled = getPreviousStop() != -1;
		if (leftTitleButtonController.enabled != previousStopEnabled) {
			leftTitleButtonController.enabled = previousStopEnabled;
			updateLeftTitleButtonIcon();
		}

		boolean nextStopEnabled = getNextStop() != -1;
		if (rightTitleButtonController.enabled != nextStopEnabled) {
			rightTitleButtonController.enabled = nextStopEnabled;
			updateRightTitleButtonIcon();
		}
	}

	private void showTransportStop(TransportStop stop) {
		if (mapContextMenu != null) {
			transportRoute.stop = stop;
			transportRoute.refStop = stop;
			PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_ROUTE,
					transportRoute.getDescription(getMapActivity().getMyApplication(), false));

			updateControllers();

			LatLon stopLocation = stop.getLocation();
			if (mapContextMenu.isVisible()) {
				mapContextMenu.updateMapCenter(stopLocation);
			} else {
				mapContextMenu.setMapCenter(stopLocation);
				mapContextMenu.setMapPosition(getMapActivity().getMapView().getMapPosition());
			}
			mapContextMenu.setCenterMarker(true);
			mapContextMenu.setMapZoom(15);
			mapContextMenu.showOrUpdate(stopLocation, pd, transportRoute);
		}
	}

	private int getCurrentStop() {
		List<TransportStop> stops = transportRoute.route.getForwardStops();
		for (int i = 0; i < stops.size(); i++) {
			final TransportStop stop = stops.get(i);
			if (stop.getName().equals(transportRoute.stop.getName())) {
				return i;
			}
		}
		return -1;
	}

	private int getNextStop() {
		List<TransportStop> stops = transportRoute.route.getForwardStops();
		int currentPos = getCurrentStop();
		if (currentPos != -1 && currentPos + 1 < stops.size()) {
			return currentPos + 1;
		}
		return -1;
	}

	private int getPreviousStop() {
		int currentPos = getCurrentStop();
		if (currentPos > 0) {
			return currentPos - 1;
		}
		return -1;
	}

	@Override
	public String getNameStr() {
		if (transportRoute.refStop != null && !TextUtils.isEmpty(transportRoute.refStop.getName())) {
			return transportRoute.refStop.getName();
		} else if (transportRoute.stop != null && !TextUtils.isEmpty(transportRoute.stop.getName())) {
			return transportRoute.stop.getName();
		} else if (!TextUtils.isEmpty(getPointDescription().getTypeName())) {
			return getPointDescription().getTypeName();
		} else {
			return getStopType();
		}
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, final LatLon latLon) {
		super.addPlainMenuItems(typeStr, pointDescription, latLon);
		List<TransportStop> stops = transportRoute.route.getForwardStops();
		boolean useEnglishNames = getMapActivity().getMyApplication().getSettings().usingEnglishNames();
		int currentStop = getCurrentStop();
		int defaultIcon = transportRoute.type == null ? R.drawable.mx_route_bus_ref : transportRoute.type.getResourceId();
		int startPosition = 0;
		if (!transportRoute.showWholeRoute) {
			startPosition = (currentStop == -1 ? 0 : currentStop);
			if (currentStop > 0) {
				addPlainMenuItem(defaultIcon, getMapActivity().getString(R.string.shared_string_show),
						getMapActivity().getString(R.string.route_stops_before, currentStop),
						false, false, new OnClickListener() {

							@Override
							public void onClick(View arg0) {
								MapContextMenu menu = getMapActivity().getContextMenu();
								menu.showOrUpdate(latLon, getPointDescription(), transportRoute);
							}
						});
			}
		}
		for (int i = startPosition; i < stops.size(); i++) {
			final TransportStop stop = stops.get(i);
			String name = useEnglishNames ? stop.getEnName(true) : stop.getName();
			if (TextUtils.isEmpty(name)) {
				name = getStopType();
			}
			addPlainMenuItem(currentStop == i ? R.drawable.ic_action_marker_dark : defaultIcon,
					null, name, false, false, new OnClickListener() {

						@Override
						public void onClick(View arg0) {
							showTransportStop(stop);
							/*
							PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_STOP,
									getMapActivity().getString(R.string.transport_Stop), name);
							LatLon stopLocation = stop.getLocation();
							getMapActivity().getMyApplication().getSettings()
									.setMapLocationToShow(stopLocation.getLatitude(), stopLocation.getLongitude(),
									15, pd, false, transportRoute);
							MapActivity.launchMapActivityMoveToTop(getMapActivity());
							*/
						}
					});
		}
	}

	private void showMenuAndRoute(LatLon latLon, boolean centerMarker) {
		MapContextMenu menu = getMapActivity().getContextMenu();
		if (centerMarker) {
			menu.setCenterMarker(true);
		}
		menu.show(latLon, getPointDescription(), transportRoute);
	}

	private void showRoute() {
		transportRoute.showWholeRoute = true;
		TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
		int cz = transportRoute.calculateZoom(0, getMapActivity().getMapView().getCurrentRotatedTileBox());
		getMapActivity().changeZoom(cz - getMapActivity().getMapView().getZoom());
		stopsLayer.setRoute(transportRoute);
	}

	private void resetRoute() {
		TransportStopsLayer stopsLayer = getMapActivity().getMapLayers().getTransportStopsLayer();
		stopsLayer.setRoute(null);
	}
}
