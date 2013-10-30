package net.osmand.plus.base;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.AutoZoomMap;
import net.osmand.plus.R;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;
import android.content.Context;
import android.view.WindowManager;

public class MapViewTrackingUtilities implements OsmAndLocationListener, IMapLocationListener, OsmAndCompassListener {
	private static final int AUTO_FOLLOW_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 4; 
	
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
				if (settings.AUTO_ZOOM_MAP.get() != AutoZoomMap.NONE) {
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

	private float defineZoomFromSpeed(RotatedTileBox tb, float speed) {
		if (speed < 7f / 3.6) {
			return 0;
		}
		double visibleDist = tb.getDistance(tb.getCenterPixelX(), 0, tb.getCenterPixelX(), tb.getCenterPixelY());
		float time = 75f; // > 83 km/h show 75 seconds 
		if (speed < 83f / 3.6) {
			time = 60f;
		}
		time /= settings.AUTO_ZOOM_MAP.get().coefficient;
		double distToSee = speed * time;
		float zoomDelta = (float) (Math.log(visibleDist / distToSee) / Math.log(2.0f));
		// check if 17, 18 is correct?
		return zoomDelta;
	}
	
	public void autozoom(Location location) {
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
				if (now - lastTimeAutoZooming > 4500) {
					lastTimeAutoZooming = now;
					float settingsZoomScale = mapView.getSettingsZoomScale();
					float complexZoom = tb.getZoom() + tb.getZoomScale() + zdelta;
					// round to 0.33
					float newZoom = Math.round((complexZoom - settingsZoomScale) * 3) / 3f;
					int nz = (int)Math.round(newZoom);
					float nzscale = newZoom - nz + settingsZoomScale;
					mapView.setComplexZoom(nz, nzscale);
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
					int fZoom = mapView.getZoom() < 15 ? 15 : mapView.getZoom();
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
		int resId = R.string.rotate_map_none_opt;
		if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_NONE && mapView.getRotate() != 0) {
			// reset manual rotation
		} else {
			int vl = (settings.ROTATE_MAP.get() + 1) % 3;
			settings.ROTATE_MAP.set(vl);

			if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_COMPASS) {
				resId = R.string.rotate_map_compass_opt;
			} else if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING) {
				resId = R.string.rotate_map_bearing_opt;
			}
		}
		app.showShortToastMessage(resId);
		updateSettings();
		if(mapView != null) {
			mapView.refreshMap();
		}
	}

}
