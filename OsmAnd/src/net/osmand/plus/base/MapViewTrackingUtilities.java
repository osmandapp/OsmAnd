package net.osmand.plus.base;

import android.content.Context;
import android.support.v4.util.Pair;
import android.view.WindowManager;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.ValueHolder;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.WorldRegion;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarkerChangedListener;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.io.IOException;

public class MapViewTrackingUtilities implements OsmAndLocationListener, IMapLocationListener,
		OsmAndCompassListener, IRouteInformationListener, MapMarkerChangedListener {
	private static final int AUTO_FOLLOW_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 4;

	private long lastTimeAutoZooming = 0;
	private boolean sensorRegistered = false;
	private OsmandMapTileView mapView;
	private DashboardOnMap dashboard;
	private MapContextMenu contextMenu;
	private OsmandSettings settings;
	private OsmandApplication app;
	private boolean isMapLinkedToLocation = true;
	private boolean followingMode;
	private boolean routePlanningMode;
	private boolean showViewAngle = false;
	private boolean isUserZoomed = false;
	private String locationProvider;
	private boolean showRouteFinishDialog = false;
	private Location myLocation;
	private Float heading;
	private boolean drivingRegionUpdated = false;
	private boolean movingToMyLocation = false;

	public MapViewTrackingUtilities(OsmandApplication app){
		this.app = app;
		settings = app.getSettings();
		myLocation = app.getLocationProvider().getLastKnownLocation();
		app.getLocationProvider().addLocationListener(this);
		app.getLocationProvider().addCompassListener(this);
		addTargetPointListener(app);
		addMapMarkersListener(app);
		app.getRoutingHelper().addListener(this);
	}

	public void resetDrivingRegionUpdate() {
		drivingRegionUpdated = false;
	}

	private void addTargetPointListener(OsmandApplication app) {
		app.getTargetPointsHelper().addListener(new StateChangedListener<Void>() {

			@Override
			public void stateChanged(Void change) {
				if(mapView != null) {
					mapView.refreshMap();
				}
			}
		});
	}

	private void addMapMarkersListener(OsmandApplication app) {
		app.getMapMarkersHelper().addListener(this);
	}

	@Override
	public void onMapMarkerChanged(MapMarkersHelper.MapMarker mapMarker) {
	}

	@Override
	public void onMapMarkersChanged() {
		if (mapView != null) {
			mapView.refreshMap();
		}
	}

	public void setMapView(OsmandMapTileView mapView) {
		this.mapView = mapView;
		if(mapView != null) {
			WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
			int orientation = wm.getDefaultDisplay().getOrientation();
			app.getLocationProvider().updateScreenOrientation(orientation);
			mapView.setMapLocationListener(this);
		}
	}

	public Location getMyLocation() {
		return myLocation;
	}

	public Float getHeading() {
		return heading;
	}

	public String getLocationProvider() {
		return locationProvider;
	}

	@Override
	public void updateCompassValue(float val) {
		heading = val;
		if (mapView != null) {
			if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_COMPASS && !routePlanningMode) {
				if (Math.abs(MapUtils.degreesDiff(mapView.getRotate(), -val)) > 1) {
					mapView.setRotate(-val);
				}
			} else if (showViewAngle) {
				mapView.refreshMap();
			}
		}
		if(dashboard != null) {
			dashboard.updateCompassValue(val);
		}
		if(contextMenu != null) {
			contextMenu.updateCompassValue(val);
		}
	}

	public void setDashboard(DashboardOnMap dashboard) {
		this.dashboard = dashboard;
	}

	public void setContextMenu(MapContextMenu contextMenu) {
		this.contextMenu = contextMenu;
	}

	public boolean isMovingToMyLocation() {
		return movingToMyLocation;
	}

	@Override
	public void updateLocation(Location location) {
		myLocation = location;
		showViewAngle = false;
		if (location != null) {
			locationProvider = location.getProvider();
			if (settings.DRIVING_REGION_AUTOMATIC.get() && !drivingRegionUpdated) {
				try {
					BinaryMapDataObject o = app.getRegions().findBinaryMapDataObject(
							new LatLon(location.getLatitude(), location.getLongitude()));
					if (o != null) {
						String fullName = app.getRegions().getFullName(o);
						WorldRegion worldRegion = app.getRegions().getRegionData(fullName);
						if (worldRegion != null) {
							app.setupDrivingRegion(worldRegion);
						}
					}
					drivingRegionUpdated = true;

				} catch (IOException e) {
					// ignore
				}
			}
		}
		if (mapView != null) {
			RotatedTileBox tb = mapView.getCurrentRotatedTileBox();
			if (isMapLinkedToLocation() && location != null) {
				Pair<Integer, Double> zoom = null;
				Float rotation = null;
				if (settings.AUTO_ZOOM_MAP.get()) {
					zoom = autozoom(location);
				}
				int currentMapRotation = settings.ROTATE_MAP.get();
				boolean smallSpeedForCompass = isSmallSpeedForCompass(location);
				boolean smallSpeedForAnimation = isSmallSpeedForAnimation(location);
				// boolean virtualBearing = fMode && settings.SNAP_TO_ROAD.get();
				showViewAngle = (!location.hasBearing() || smallSpeedForCompass) && (tb != null &&
						tb.containsLatLon(location.getLatitude(), location.getLongitude()));
				if (currentMapRotation == OsmandSettings.ROTATE_MAP_BEARING) {
					if (location.hasBearing() && !smallSpeedForCompass) {
						// special case when bearing equals to zero (we don't change anything)
						if (location.getBearing() != 0f) {
							rotation = -location.getBearing();
						}
					}
				} else if(currentMapRotation == OsmandSettings.ROTATE_MAP_COMPASS) {
					showViewAngle = routePlanningMode; // disable compass rotation in that mode
				}
				registerUnregisterSensor(location);
				if (settings.ANIMATE_MY_LOCATION.get() && !smallSpeedForAnimation && !movingToMyLocation &&
						settings.WAKE_ON_VOICE_INT.get() == 0) {
					mapView.getAnimatedDraggingThread().startMoving(
							location.getLatitude(), location.getLongitude(), zoom, rotation, false);
				} else {
					if (zoom != null) {
						mapView.getAnimatedDraggingThread().startZooming(zoom.first, zoom.second, false);
					}
					if (rotation != null) {
						mapView.setRotate(rotation);
					}
					mapView.setLatLon(location.getLatitude(), location.getLongitude());
				}
			} else if(location != null) {
				showViewAngle = (!location.hasBearing() || isSmallSpeedForCompass(location)) && (tb != null &&
						tb.containsLatLon(location.getLatitude(), location.getLongitude()));
				registerUnregisterSensor(location);
			}
			RoutingHelper routingHelper = app.getRoutingHelper();
			followingMode = routingHelper.isFollowingMode();
			if(routePlanningMode != routingHelper.isRoutePlanningMode()) {
				switchToRoutePlanningMode();
			}
			// When location is changed we need to refresh map in order to show movement!
			mapView.refreshMap();
		}

		if(dashboard != null) {
			dashboard.updateMyLocation(location);
		}
		if(contextMenu != null) {
			contextMenu.updateMyLocation(location);
		}
	}

	public static boolean isSmallSpeedForCompass(Location location) {
		return !location.hasSpeed() || location.getSpeed() < 0.5;
	}

	public static boolean isSmallSpeedForAnimation(Location location) {
		return !location.hasSpeed() || location.getSpeed() < 1.5;
	}

	public boolean isShowViewAngle() {
		return showViewAngle;
	}


	public void switchToRoutePlanningMode() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		routePlanningMode = routingHelper.isRoutePlanningMode();
		updateSettings();
		if(!routePlanningMode && followingMode) {
			backToLocationImpl();
		}

	}

	public void updateSettings(){
		if (mapView != null) {
			if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_NONE || routePlanningMode) {
				mapView.setRotate(0);
			}
			mapView.setMapPosition(settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING
					&& !routePlanningMode
					&& !settings.CENTER_POSITION_ON_MAP.get() ?
					OsmandSettings.BOTTOM_CONSTANT : OsmandSettings.CENTER_CONSTANT);
		}
		registerUnregisterSensor(app.getLocationProvider().getLastKnownLocation());
	}

	private void registerUnregisterSensor(net.osmand.Location location) {

		int currentMapRotation = settings.ROTATE_MAP.get();
		boolean registerCompassListener = ((showViewAngle || contextMenu != null) && location != null)
				|| (currentMapRotation == OsmandSettings.ROTATE_MAP_COMPASS && !routePlanningMode);
		// show point view only if gps enabled
		if(sensorRegistered != registerCompassListener) {
			app.getLocationProvider().registerOrUnregisterCompassListener(registerCompassListener);
		}
	}

	private float defineZoomFromSpeed(RotatedTileBox tb, float speed) {
		if (speed < 7f / 3.6) {
			return 0;
		}
		double visibleDist = tb.getDistance(tb.getCenterPixelX(), 0, tb.getCenterPixelX(), tb.getCenterPixelY());
		float time = 75f; // > 83 km/h show 75 seconds 
		if (speed < 83f / 3.6) {
			time = 60f;
		}
		time /= settings.AUTO_ZOOM_MAP_SCALE.get().coefficient;
		double distToSee = speed * time;
		float zoomDelta = (float) (Math.log(visibleDist / distToSee) / Math.log(2.0f));
		// check if 17, 18 is correct?
		return zoomDelta;
	}

	public Pair<Integer, Double> autozoom(Location location) {
		if (location.hasSpeed()) {
			long now = System.currentTimeMillis();
			final RotatedTileBox tb = mapView.getCurrentRotatedTileBox();
			float zdelta = defineZoomFromSpeed(tb, location.getSpeed());
			if (Math.abs(zdelta) >= 0.5/*?Math.sqrt(0.5)*/) {
				// prevent ui hysteresis (check time interval for autozoom)
				if (zdelta >= 2) {
					// decrease a bit
					zdelta -= 1;
				} else if (zdelta <= -2) {
					// decrease a bit
					zdelta += 1;
				}
				double targetZoom = Math.min(tb.getZoom() + tb.getZoomFloatPart() + zdelta, settings.AUTO_ZOOM_MAP_SCALE.get().maxZoom);
				int threshold = settings.AUTO_FOLLOW_ROUTE.get();
				if (now - lastTimeAutoZooming > 4500 && (now - lastTimeAutoZooming > threshold || !isUserZoomed)) {
					isUserZoomed = false;
					lastTimeAutoZooming = now;
//					double settingsZoomScale = Math.log(mapView.getSettingsMapDensity()) / Math.log(2.0f);
//					double zoomScale = Math.log(tb.getMapDensity()) / Math.log(2.0f);
//					double complexZoom = tb.getZoom() + zoomScale + zdelta;
					// round to 0.33
					targetZoom = Math.round(targetZoom * 3) / 3f;
					int newIntegerZoom = (int)Math.round(targetZoom);
					double zPart = targetZoom - newIntegerZoom;
					return new Pair<>(newIntegerZoom, zPart);
				}
			}
		}
		return null;
	}

	public void backToLocationImpl() {
		backToLocationImpl(15);
	}

	public void backToLocationImpl(int zoom) {
		if (mapView != null) {
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			if (!isMapLinkedToLocation()) {
				setMapLinkedToLocation(true);
				net.osmand.Location lastKnownLocation = locationProvider.getLastKnownLocation();
				if (lastKnownLocation != null) {
					AnimateDraggingMapThread thread = mapView.getAnimatedDraggingThread();
					int fZoom = mapView.getZoom() < zoom ? zoom : mapView.getZoom();
					movingToMyLocation = true;
					thread.startMoving(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
							fZoom, false, new Runnable() {
								@Override
								public void run() {
									movingToMyLocation = false;
								}
							});
				}
				mapView.refreshMap();
			}
			if (locationProvider.getLastKnownLocation() == null) {
				app.showToastMessage(R.string.unknown_location);
			}
		}
	}

	private void backToLocationWithDelay(int delay) {
		app.runMessageInUIThreadAndCancelPrevious(AUTO_FOLLOW_MSG_ID, new Runnable() {
			@Override
			public void run() {
				if (mapView != null && !isMapLinkedToLocation() && contextMenu == null) {
					app.showToastMessage(R.string.auto_follow_location_enabled);
					backToLocationImpl();
				}
			}
		}, delay * 1000);
	}

	public boolean isMapLinkedToLocation(){
		return isMapLinkedToLocation;
	}

	public void setMapLinkedToLocation(boolean isMapLinkedToLocation) {
		if (!isMapLinkedToLocation) {
			int autoFollow = settings.AUTO_FOLLOW_ROUTE.get();
			if (autoFollow > 0 && app.getRoutingHelper().isFollowingMode() && !routePlanningMode) {
				backToLocationWithDelay(autoFollow);
			}
		}
		this.isMapLinkedToLocation = isMapLinkedToLocation;
	}

	@Override
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		// when user start dragging 
		setMapLinkedToLocation(false);
	}

	public void switchRotateMapMode(){
		String rotMode = app.getString(R.string.rotate_map_none_opt);
		if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_NONE && mapView.getRotate() != 0) {
			// reset manual rotation
		} else {
			int vl = (settings.ROTATE_MAP.get() + 1) % 3;
			settings.ROTATE_MAP.set(vl);

			if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
				rotMode = app.getString(R.string.rotate_map_bearing_opt);
			} else if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_COMPASS) {
				rotMode = app.getString(R.string.rotate_map_compass_opt);
			}
		}
		rotMode = app.getString(R.string.rotate_map_to_bearing) + ":\n" + rotMode;
		app.showShortToastMessage(rotMode);
		updateSettings();
		if(mapView != null) {
			mapView.refreshMap();
		}
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
	}

	@Override
	public void routeWasCancelled() {
	}

	@Override
	public void routeWasFinished() {
		showRouteFinishDialog = (mapView == null);
	}

	public boolean getShowRouteFinishDialog() {
		return showRouteFinishDialog;
	}

	public void setShowRouteFinishDialog(boolean showRouteFinishDialog) {
		this.showRouteFinishDialog = showRouteFinishDialog;
	}

	public void setZoomTime(long time) {
		lastTimeAutoZooming = time;
		isUserZoomed = true;
	}

}
