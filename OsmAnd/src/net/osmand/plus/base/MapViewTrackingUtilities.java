package net.osmand.plus.base;

import android.content.Context;
import android.view.WindowManager;
import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.map.IMapLocationListener;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

public class MapViewTrackingUtilities implements OsmAndLocationListener, IMapLocationListener, OsmAndCompassListener {
	private static final int AUTO_FOLLOW_MSG_ID = 8; 
	
	private long lastTimeAutoZooming = 0;
	private long lastTimeSensorMapRotation = 0;
	private boolean sensorRegistered = false;
	private OsmandMapTileView mapView;
	private OsmandSettings settings;
	private OsmandApplication app;
	// by default turn off causing unexpected movements due to network establishing
	private boolean isMapLinkedToLocation = false;
	private boolean followingMode;

	
	
	public MapViewTrackingUtilities(OsmandApplication app){
		this.app = app;
		settings = app.getSettings();
		app.getLocationProvider().addLocationListener(this);
		app.getLocationProvider().addCompassListener(this);
		addTargetPointListener(app);
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
	
	public void setMapView(OsmandMapTileView mapView) {
		this.mapView = mapView;
		if(mapView != null) {
			WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
			int orientation = wm.getDefaultDisplay().getOrientation();
			app.getLocationProvider().updateScreenOrientation(orientation);
			mapView.setMapLocationListener(this);
		}
	}
	
	@Override
	public void updateCompassValue(float val) {
		if (mapView != null) {
			if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_COMPASS) {
				if (Math.abs(MapUtils.degreesDiff(mapView.getRotate(), -val)) > 1) {
					mapView.setRotate(-val);
				}
			} else if (settings.SHOW_VIEW_ANGLE.get()) {
				mapView.refreshMap();
			}
		}
	}
	
	@Override
	public void updateLocation(Location location) {
		if (mapView != null) {
			if (isMapLinkedToLocation() && location != null) {
				if (settings.AUTO_ZOOM_MAP.get()) {
					autozoom(location);
				}
				int currentMapRotation = settings.ROTATE_MAP.get();
				boolean enableCompass = false;
				if (currentMapRotation == OsmandSettings.ROTATE_MAP_BEARING) {
					boolean smallSpeed = !location.hasSpeed() || location.getSpeed() < 0.5;
					boolean fMode = app.getRoutingHelper().isFollowingMode();
					// boolean virtualBearing = fMode && settings.SNAP_TO_ROAD.get();
					enableCompass = (!location.hasBearing() || smallSpeed)
							&& fMode && settings.USE_COMPASS_IN_NAVIGATION.get();
					if (location.hasBearing() && !smallSpeed) {
						// special case when bearing equals to zero (we don't change anything)
						if (location.getBearing() != 0f) {
							mapView.setRotate(-location.getBearing());
						}
					} else if (enableCompass) {
						long now = System.currentTimeMillis();
						OsmAndLocationProvider provider = app.getLocationProvider();
						Float lastSensorRotation = provider.getHeading();
						if (lastSensorRotation != null && Math.abs(MapUtils.degreesDiff(mapView.getRotate(), -lastSensorRotation)) > 15) {
							if (now - lastTimeSensorMapRotation > 3500) {
								lastTimeSensorMapRotation = now;
								mapView.setRotate(-lastSensorRotation);
							}
						}
					}
				}
				registerUnregisterSensor(location, enableCompass);
				mapView.setLatLon(location.getLatitude(), location.getLongitude());
			}
			RoutingHelper routingHelper = app.getRoutingHelper();
			// we arrived at destination finished
			if (!routingHelper.isFollowingMode() && followingMode) {
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						settings.APPLICATION_MODE.set(settings.DEFAULT_APPLICATION_MODE.get());
					}
				});
				
			}
			followingMode = routingHelper.isFollowingMode();
			
			// When location is changed we need to refresh map in order to show movement!
			mapView.refreshMap();
		}
		
	}
	
	public void updateSettings(){
		if (mapView != null) {
			if (settings.ROTATE_MAP.get() != OsmandSettings.ROTATE_MAP_COMPASS) {
				mapView.setRotate(0);
			}
			mapView.setMapPosition(settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING ? OsmandSettings.BOTTOM_CONSTANT
					: OsmandSettings.CENTER_CONSTANT);
		}
		registerUnregisterSensor(app.getLocationProvider().getLastKnownLocation(), false);
	}
	
	private void registerUnregisterSensor(net.osmand.Location location, boolean overruleRegister) {
		boolean currentShowingAngle = settings.SHOW_VIEW_ANGLE.get();
		int currentMapRotation = settings.ROTATE_MAP.get();
		boolean registerCompassListener = overruleRegister || (currentShowingAngle && location != null)
				|| currentMapRotation == OsmandSettings.ROTATE_MAP_COMPASS;
		// show point view only if gps enabled
		if(sensorRegistered != registerCompassListener) {
			app.getLocationProvider().registerOrUnregisterCompassListener(registerCompassListener);
		}
	}

	private float defineZoomFromSpeed(float speed) {
		if (speed < 7f / 3.6) {
			return 0;
		}
		double topLat = mapView.calcLatitude(-mapView.getCenterPointY());
		double cLat = mapView.calcLatitude(0);
		double visibleDist = MapUtils.getDistance(cLat, mapView.getLongitude(), topLat, mapView.getLongitude());
		float time = 75f;
		if (speed < 83f / 3.6) {
			time = 60f;
		}
		double distToSee = speed * time;
		float zoomDelta = (float) (Math.log(visibleDist / distToSee) / Math.log(2.0f));
		zoomDelta = Math.round(zoomDelta * OsmandMapTileView.ZOOM_DELTA) * OsmandMapTileView.ZOOM_DELTA_1;
		// check if 17, 18 is correct?
		if (zoomDelta + mapView.getFloatZoom() > 18 - OsmandMapTileView.ZOOM_DELTA_1) {
			return 18 - OsmandMapTileView.ZOOM_DELTA_1 - mapView.getFloatZoom();
		}
		return zoomDelta;
	}
	
	public void autozoom(Location location) {
		if (location.hasSpeed()) {
			long now = System.currentTimeMillis();
			float zdelta = defineZoomFromSpeed(location.getSpeed());
			if (Math.abs(zdelta) >= OsmandMapTileView.ZOOM_DELTA_1) {
				// prevent ui hysteresis (check time interval for autozoom)
				if (zdelta >= 2) {
					// decrease a bit
					zdelta -= 3 * OsmandMapTileView.ZOOM_DELTA_1;
				} else if (zdelta <= -2) {
					// decrease a bit
					zdelta += 3 * OsmandMapTileView.ZOOM_DELTA_1;
				}
				if (now - lastTimeAutoZooming > 4500) {
					lastTimeAutoZooming = now;
					float newZoom = Math.round((mapView.getFloatZoom() + zdelta) * OsmandMapTileView.ZOOM_DELTA)
							* OsmandMapTileView.ZOOM_DELTA_1;
					mapView.setZoom(newZoom);
					// mapView.getAnimatedDraggingThread().startZooming(mapView.getFloatZoom() + zdelta, false);
				}
			}
		}
	}
	
	public void backToLocationImpl() {
		if (mapView != null) {
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			if (!isMapLinkedToLocation()) {
				setMapLinkedToLocation(true);
				if (locationProvider.getLastKnownLocation() != null) {
					net.osmand.Location lastKnownLocation = locationProvider.getLastKnownLocation();
					AnimateDraggingMapThread thread = mapView.getAnimatedDraggingThread();
					float fZoom = mapView.getFloatZoom() < 15 ? 15 : mapView.getFloatZoom();
					thread.startMoving(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), fZoom, false);
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
				if (mapView != null && !isMapLinkedToLocation()) {
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
		if(!isMapLinkedToLocation){
			int autoFollow = settings.AUTO_FOLLOW_ROUTE.get();
			if(autoFollow > 0 && app.getRoutingHelper().isFollowingMode()){
				backToLocationWithDelay(autoFollow);
			}
		}
		this.isMapLinkedToLocation = isMapLinkedToLocation;
	}
	
	@Override
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		// when user start dragging 
		if(app.getLocationProvider().getLastKnownLocation() != null){
			setMapLinkedToLocation(false);
		}
	}
	
	public void switchRotateMapMode(){
		int vl = (settings.ROTATE_MAP.get() + 1) % 3;
		settings.ROTATE_MAP.set(vl);
		int resId = R.string.rotate_map_none_opt;
		if(settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_COMPASS){
			resId = R.string.rotate_map_compass_opt;
		} else if(settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING){
			resId = R.string.rotate_map_bearing_opt;
		}
		app.showShortToastMessage(resId);
		updateSettings();
		mapView.refreshMap();
	}

}
