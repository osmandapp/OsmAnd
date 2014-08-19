package net.osmand.plus.base;

import java.util.List;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.AutoZoomMap;
import net.osmand.plus.R;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;
import android.content.Context;
import android.view.WindowManager;

public class MapViewTrackingUtilities implements OsmAndLocationListener, IMapLocationListener, OsmAndCompassListener, IRouteInformationListener {
	private static final int AUTO_FOLLOW_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 4; 
	
	private long lastTimeAutoZooming = 0;
	private boolean sensorRegistered = false;
	private OsmandMapTileView mapView;
	private OsmandSettings settings;
	private OsmandApplication app;
	// by default turn off causing unexpected movements due to network establishing
	private boolean isMapLinkedToLocation = false;
	private boolean followingMode;
	private boolean routePlanningMode;
	private boolean showViewAngle = false;
	
	public MapViewTrackingUtilities(OsmandApplication app){
		this.app = app;
		settings = app.getSettings();
		app.getLocationProvider().addLocationListener(this);
		app.getLocationProvider().addCompassListener(this);
		addTargetPointListener(app);
		app.getRoutingHelper().addListener(this);
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
			if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_COMPASS && !routePlanningMode) {
				if (Math.abs(MapUtils.degreesDiff(mapView.getRotate(), -val)) > 1) {
					mapView.setRotate(-val);
				}
			} else if (showViewAngle) {
				mapView.refreshMap();
			}
		}
	}
	
	@Override
	public void updateLocation(Location location) {
		showViewAngle = false;
		if (mapView != null) {
			RotatedTileBox tb = mapView.getCurrentRotatedTileBox();
			if (isMapLinkedToLocation() && location != null) {
				if (settings.AUTO_ZOOM_MAP.get() != AutoZoomMap.NONE) {
					autozoom(location);
				}
				int currentMapRotation = settings.ROTATE_MAP.get();
				boolean smallSpeed = isSmallSpeedForCompass(location);
				// boolean virtualBearing = fMode && settings.SNAP_TO_ROAD.get();
				showViewAngle = (!location.hasBearing() || smallSpeed) && (tb != null && 
						tb.containsLatLon(location.getLatitude(), location.getLongitude()));
				if (currentMapRotation == OsmandSettings.ROTATE_MAP_BEARING) {
					if (location.hasBearing() && !smallSpeed) {
						// special case when bearing equals to zero (we don't change anything)
						if (location.getBearing() != 0f) {
							mapView.setRotate(-location.getBearing());
						}
					}
				} else if(currentMapRotation == OsmandSettings.ROTATE_MAP_COMPASS) {
					showViewAngle = routePlanningMode; // disable compass rotation in that mode
				}
				registerUnregisterSensor(location);
				mapView.setLatLon(location.getLatitude(), location.getLongitude());
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
	}

	private boolean isSmallSpeedForCompass(Location location) {
		return !location.hasSpeed() || location.getSpeed() < 0.5;
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
			if (settings.ROTATE_MAP.get() != OsmandSettings.ROTATE_MAP_COMPASS || routePlanningMode) {
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
		boolean registerCompassListener = (showViewAngle && location != null)
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
			if(autoFollow > 0 && app.getRoutingHelper().isFollowingMode() && !routePlanningMode){
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

	@Override
	public void newRouteIsCalculated(boolean newRoute) {
		RoutingHelper rh = app.getRoutingHelper();
		if(newRoute && rh.isRoutePlanningMode() && mapView != null) {
			RotatedTileBox rt = mapView.getCurrentRotatedTileBox();
			Location lt = rh.getLastProjection();
			if(lt != null) {
				double left = lt.getLongitude(), right = lt.getLongitude();
				double top = lt.getLatitude(), bottom = lt.getLatitude();
				List<LatLon> list = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
				for(LatLon l : list) {
					left = Math.min(left, l.getLongitude());
					right = Math.max(right, l.getLongitude());
					top = Math.max(top, l.getLatitude());
					bottom = Math.min(bottom, l.getLatitude());
				}
				RotatedTileBox tb = new RotatedTileBox(rt);
				tb.setPixelDimensions(3 * tb.getPixWidth() / 4, 3 * tb.getPixHeight() / 4);
				double clat = bottom / 2 + top / 2;
				double clon = left / 2 + right / 2;
				tb.setLatLonCenter(clat, clon);
				while(tb.getZoom() >= 7 && (!tb.containsLatLon(top, left) || !tb.containsLatLon(bottom, right))) {
					tb.setZoom(tb.getZoom() - 1);
				}
				mapView.getAnimatedDraggingThread().startMoving(clat, clon, tb.getZoom(),
						true);
			}
		}
	}

	@Override
	public void routeWasCancelled() {
	}


}
