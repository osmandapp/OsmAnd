package net.osmand.plus.mapcontextmenu.controllers;

import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;

import net.osmand.plus.utils.AndroidUtils;
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
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.TransportStopsLayer;

import java.util.List;

public class TransportRouteController extends MenuController {

	private TransportStopRoute transportRoute;

	public TransportRouteController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription,
									@NonNull TransportStopRoute transportRoute) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.transportRoute = transportRoute;
		builder.setShowOnlinePhotos(false);
		int navigationIconResId = AndroidUtils.getNavigationIconResId(mapActivity);
		toolbarController = new ContextMenuToolbarController(this);
		toolbarController.setTitle(getNameStr());
		toolbarController.setBackBtnIconIds(navigationIconResId, navigationIconResId);
		toolbarController.setOnBackButtonClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					activity.getContextMenu().backToolbarAction(TransportRouteController.this);
				}
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
				MapActivity activity = getMapActivity();
				if (activity != null) {
					activity.getContextMenu().closeToolbar(TransportRouteController.this);
				}
			}
		});

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				int previousStop = getPreviousStop();
				if (previousStop != -1) {
					showTransportStop(getTransportRoute().route.getForwardStops().get(previousStop), true, previousStop);
				}
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_previous);

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				int nextStop = getNextStop();
				if (nextStop != -1) {
					showTransportStop(getTransportRoute().route.getForwardStops().get(nextStop), true, nextStop);
				}
			}
		};
		rightTitleButtonController.caption = mapActivity.getString(R.string.shared_string_next);

		if (AndroidUtils.isLayoutRtl(mapActivity)) {
			leftTitleButtonController.endIconId = R.drawable.ic_arrow_forward;
			rightTitleButtonController.startIconId = R.drawable.ic_arrow_back;
		} else {
			leftTitleButtonController.startIconId = R.drawable.ic_arrow_back;
			rightTitleButtonController.endIconId = R.drawable.ic_arrow_forward;
		}
	}

	@NonNull
	public TransportStopRoute getTransportRoute() {
		return transportRoute;
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

	@NonNull
	@Override
	public String getTypeStr() {
		return getPointDescription().getName();
	}

	private String getStopType() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(transportRoute.getTypeStrRes()) + " " + mapActivity.getString(R.string.transport_Stop).toLowerCase();
		} else {
			return "";
		}
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

	public void onAcquireNewController(PointDescription pointDescription, Object object) {
		if (object instanceof TransportRouteStop) {
			resetRoute();
		}
	}

	private void updateControllers() {
		boolean previousStopEnabled = getPreviousStop() != -1;
		if (leftTitleButtonController.enabled != previousStopEnabled) {
			leftTitleButtonController.enabled = previousStopEnabled;
		}

		boolean nextStopEnabled = getNextStop() != -1;
		if (rightTitleButtonController.enabled != nextStopEnabled) {
			rightTitleButtonController.enabled = nextStopEnabled;
		}
	}

	private void showTransportStop(TransportStop stop, boolean movingBetweenStops, int stopIndex) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && mapContextMenu != null) {
			transportRoute.stop = stop;
			transportRoute.setStopIndex(stopIndex);
			transportRoute.refStop = stop;
			PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_ROUTE,
					transportRoute.getDescription(mapActivity.getMyApplication(), false));

			updateControllers();

			LatLon stopLocation = stop.getLocation();
			if (mapContextMenu.isVisible()) {
				mapContextMenu.updateMapCenter(stopLocation);
			} else {
				mapContextMenu.setMapCenter(stopLocation);
			}
			mapContextMenu.setCenterMarker(true);
			mapContextMenu.setZoomOutOnly(movingBetweenStops);
			mapContextMenu.setMapZoom(15);
			mapContextMenu.showOrUpdate(stopLocation, pd, transportRoute);
		}
	}

	private int getNextStop() {
		List<TransportStop> stops = transportRoute.route.getForwardStops();
		int currentPos = transportRoute.getStopIndex();
		if (currentPos != -1 && currentPos + 1 < stops.size()) {
			return currentPos + 1;
		}
		return -1;
	}

	private int getPreviousStop() {
		int currentPos = transportRoute.getStopIndex();
		if (currentPos > 0) {
			return currentPos - 1;
		}
		return -1;
	}

	@NonNull
	@Override
	public String getNameStr() {
		if (transportRoute.refStop != null && !TextUtils.isEmpty(transportRoute.refStop.getName())) {
			return transportRoute.refStop.getName(getPreferredMapLang(), isTransliterateNames());
		} else if (transportRoute.stop != null && !TextUtils.isEmpty(transportRoute.stop.getName())) {
			return transportRoute.stop.getName(getPreferredMapLang(), isTransliterateNames());
		} else if (!TextUtils.isEmpty(getPointDescription().getTypeName())) {
			return getPointDescription().getTypeName();
		} else {
			return getStopType();
		}
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		super.addPlainMenuItems(typeStr, pointDescription, latLon);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		List<TransportStop> stops = transportRoute.route.getForwardStops();
		boolean useEnglishNames = mapActivity.getMyApplication().getSettings().usingEnglishNames();
		int currentStop = transportRoute.getStopIndex();
		int defaultIcon = transportRoute.type == null ? R.drawable.mx_route_bus_ref : transportRoute.type.getResourceId();
		int startPosition = 0;
		if (!transportRoute.showWholeRoute) {
			startPosition = (currentStop == -1 ? 0 : currentStop);
			if (currentStop > 0) {
				addPlainMenuItem(defaultIcon, mapActivity.getString(R.string.shared_string_show),
						mapActivity.getString(R.string.route_stops_before, String.valueOf(currentStop)),
						false, false, new OnClickListener() {

							@Override
							public void onClick(View arg0) {
								MapActivity activity = getMapActivity();
								if (activity != null) {
									MapContextMenu menu = activity.getContextMenu();
									menu.showOrUpdate(latLon, getPointDescription(), transportRoute);
								}
							}
						});
			}
		}
		for (int i = startPosition; i < stops.size(); i++) {
			TransportStop stop = stops.get(i);
			String name = useEnglishNames ? stop.getEnName(true) : stop.getName();
			if (TextUtils.isEmpty(name)) {
				name = getStopType();
			}
			addPlainMenuItem(currentStop == i ? R.drawable.ic_action_marker_dark : defaultIcon,
					null, name, false, false, new OnClickListener() {

						@Override
						public void onClick(View arg0) {
							showTransportStop(stop, false, -1);
						}
					});
		}
	}

	private void showMenuAndRoute(LatLon latLon, boolean centerMarker) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapContextMenu menu = mapActivity.getContextMenu();
			if (centerMarker) {
				menu.setCenterMarker(true);
			}
			menu.show(latLon, getPointDescription(), transportRoute);
		}
	}

	private void showRoute() {
		transportRoute.showWholeRoute = true;
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			TransportStopsLayer stopsLayer = mapActivity.getMapLayers().getTransportStopsLayer();
			OsmandMapTileView mapView = mapActivity.getMapView();
			int zoom = transportRoute.calculateZoom(0, mapView.getCurrentRotatedTileBox());
			mapView.setIntZoom(zoom);
			stopsLayer.setRoute(transportRoute);
		}
	}

	private void resetRoute() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			TransportStopsLayer stopsLayer = mapActivity.getMapLayers().getTransportStopsLayer();
			stopsLayer.setRoute(null);
		}
	}
}
