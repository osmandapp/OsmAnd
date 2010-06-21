package com.osmand.activities;

import java.text.MessageFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.osmand.LogUtil;
import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.Version;
import com.osmand.activities.FavouritesActivity.FavouritePoint;
import com.osmand.activities.FavouritesActivity.FavouritesDbHelper;
import com.osmand.activities.search.SearchActivity;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.data.preparation.MapTileDownloader.DownloadRequest;
import com.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
import com.osmand.map.IMapLocationListener;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.views.MapInfoLayer;
import com.osmand.views.OsmBugsLayer;
import com.osmand.views.OsmandMapTileView;
import com.osmand.views.POIMapLayer;
import com.osmand.views.PointLocationLayer;
import com.osmand.views.PointNavigationLayer;
import com.osmand.views.RouteLayer;

public class MapActivity extends Activity implements LocationListener, IMapLocationListener, SensorEventListener {

	private static final String GPS_STATUS_ACTIVITY = "com.eclipsim.gpsstatus2.GPSStatus"; //$NON-NLS-1$
	private static final String GPS_STATUS_COMPONENT = "com.eclipsim.gpsstatus2"; //$NON-NLS-1$
	
	private static final int GPS_TIMEOUT_REQUEST = 2000;
	private static final int GPS_DIST_REQUEST = 5;
	
	private boolean providerSupportsBearing = false;
	private boolean providerSupportsSpeed = false;
	
    /** Called when the activity is first created. */
	private OsmandMapTileView mapView;
	
	private ImageButton backToLocation;
	private ImageButton backToMenu;
	
	private PointLocationLayer locationLayer;
	private PointNavigationLayer navigationLayer;
	private POIMapLayer poiMapLayer;
	private MapInfoLayer mapInfoLayer;
	private OsmBugsLayer osmBugsLayer;
	private SavingTrackHelper savingTrackHelper;
	private RoutingHelper routingHelper;
	private boolean calculateRouteOnGps = false;
	private RouteLayer routeLayer;
	
	private WakeLock wakeLock;
	private boolean sensorRegistered = false;

	private MenuItem navigateToPointMenu;
	private NotificationManager mNotificationManager;
	private int APP_NOTIFICATION_ID;
	

	

	private boolean isMapLinkedToLocation(){
		return OsmandSettings.isMapSyncToGpsLocation(this);
	}
	
	private Notification getNotification(){
		Intent notificationIndent = new Intent(this, MapActivity.class);
		notificationIndent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		Notification notification = new Notification(R.drawable.icon, "", //$NON-NLS-1$
				System.currentTimeMillis());
		notification.setLatestEventInfo(this, Version.APP_NAME,
				getString(R.string.go_back_to_osmand), PendingIntent.getActivity(
						this.getBaseContext(), 0, notificationIndent,
						PendingIntent.FLAG_UPDATE_CURRENT));
		return notification;
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
//	     getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
//	                                WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		
		setContentView(R.layout.main);
		
		mapView = (OsmandMapTileView) findViewById(R.id.MapView);
		MapTileDownloader.getInstance().addDownloaderCallback(new IMapDownloaderCallback(){
			@Override
			public void tileDownloaded(DownloadRequest request) {
				if(request != null && !request.error && request.fileToSave != null){
					ResourceManager mgr = ResourceManager.getResourceManager();
					String tile = mgr.calculateTileId(mapView.getMap(), request.xTile, request.yTile, request.zoom);
					mgr.tileDownloaded(tile);
				}
				mapView.tileDownloaded(request);
			}
		});
		
		mapView.setMapLocationListener(this);
		poiMapLayer = new POIMapLayer();
		mapView.addLayer(poiMapLayer);
		routingHelper = new RoutingHelper(this);
		routeLayer = new RouteLayer(routingHelper);
		mapView.addLayer(routeLayer);
		osmBugsLayer = new OsmBugsLayer(this);
		mapInfoLayer = new MapInfoLayer(this, routeLayer);
		mapView.addLayer(mapInfoLayer);
		navigationLayer = new PointNavigationLayer();
		mapView.addLayer(navigationLayer);
		locationLayer = new PointLocationLayer();
		mapView.addLayer(locationLayer);
		
		savingTrackHelper = new SavingTrackHelper(this);
		
		
		
		
		locationLayer.setAppMode(OsmandSettings.getApplicationMode(this));
		
		LatLon pointToNavigate = OsmandSettings.getPointToNavigate(this);
		routingHelper.setAppMode(OsmandSettings.getApplicationMode(this));
		routingHelper.setFinalAndCurrentLocation(pointToNavigate, null);
		navigationLayer.setPointToNavigate(pointToNavigate);
		
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_READABLE);
		if(prefs == null || !prefs.contains(OsmandSettings.LAST_KNOWN_MAP_LAT)){
			LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
			Location location = service.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if(location != null){
				mapView.setLatLon(location.getLatitude(), location.getLongitude());
				mapView.setZoom(14);
			}
		}
		
		
		
		ZoomControls zoomControls = (ZoomControls) findViewById(R.id.ZoomControls);
		zoomControls.setOnZoomInClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapView.setZoom(mapView.getZoom() + 1);
				// user can preview map manually switch off auto zoom while user don't press back to location
				if(OsmandSettings.isAutoZoomEnabled(MapActivity.this)){
					locationChanged(mapView.getLatitude(), mapView.getLongitude(), null);
				}
			}
		});
		zoomControls.setOnZoomOutClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapView.setZoom(mapView.getZoom() - 1);
				// user can preview map manually switch off auto zoom while user don't press back to location
				if(OsmandSettings.isAutoZoomEnabled(MapActivity.this)){
					locationChanged(mapView.getLatitude(), mapView.getLongitude(), null);
				}
			}
		});
		backToLocation = (ImageButton)findViewById(R.id.BackToLocation);
		backToLocation.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				backToLocation.setVisibility(View.INVISIBLE);
				if(!isMapLinkedToLocation()){
					OsmandSettings.setSyncMapToGpsLocation(MapActivity.this, true);
					if(locationLayer.getLastKnownLocation() != null){
						Location lastKnownLocation = locationLayer.getLastKnownLocation();
						mapView.setLatLon(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
					}
				}
			}
			
		});
		
		
		backToMenu = (ImageButton)findViewById(R.id.BackToMenu);
		backToMenu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent newIntent = new Intent(MapActivity.this, MainMenuActivity.class);
				newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(newIntent);
			}
		});
		
		mapView.setOnLongClickListener(new OsmandMapTileView.OnLongClickListener(){

			@Override
			public boolean onLongPressEvent(PointF point) {
				float dx = point.x - mapView.getCenterPointX();
				float dy = point.y - mapView.getCenterPointY();
				float fy = mapView.calcDiffTileY(dx, dy);
				float fx = mapView.calcDiffTileX(dx, dy);
				double latitude = MapUtils.getLatitudeFromTile(mapView.getZoom(), mapView.getYTile() + fy);
				double longitude = MapUtils.getLongitudeFromTile(mapView.getZoom(), mapView.getXTile() + fx);
				contextMenuPoint(latitude, longitude, false);
				return true;
			}
			
		});
		
	}
    
 
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
			Intent newIntent = new Intent(MapActivity.this, SearchActivity.class);
			startActivity(newIntent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
    	if(event.getAction() == MotionEvent.ACTION_MOVE){
    		float x = event.getX();
    		float y = event.getY();
    		x = (float) (MapUtils.getTileNumberX(mapView.getZoom() , mapView.getLongitude()) + x / 3);
    		y = (float) (MapUtils.getTileNumberY(mapView.getZoom(), mapView.getLatitude()) + y / 3);
    		double lat = MapUtils.getLatitudeFromTile(mapView.getZoom(), y);
    		double lon = MapUtils.getLongitudeFromTile(mapView.getZoom(), x);
    		setMapLocation(lat, lon);
    		return true;
    		// that doesn't work for now
//    	} else if(event.getAction() == MotionEvent.ACTION_UP){
//    		contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
//    		return true;
    	}
    	return super.onTrackballEvent(event);
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	
    }
    
    @Override
    protected void onStop() {
    	mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.notify(APP_NOTIFICATION_ID, getNotification());
    	super.onStop();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	savingTrackHelper.close();
    	mNotificationManager.cancel(APP_NOTIFICATION_ID);
    	MapTileDownloader.getInstance().removeDownloaderCallback(mapView);
    }
    
    
    
    private void registerUnregisterSensor(Location location){
    	// show point view only if gps enabled
    	if(location == null){
    		if(sensorRegistered) {
    			Log.d(LogUtil.TAG, "Disable sensor"); //$NON-NLS-1$
    			((SensorManager) getSystemService(SENSOR_SERVICE)).unregisterListener(this);
    			sensorRegistered = false;
    			locationLayer.setHeading(null);
    		}
    	} else {
    		if(!sensorRegistered && OsmandSettings.isShowingViewAngle(this)){
    			Log.d(LogUtil.TAG, "Enable sensor"); //$NON-NLS-1$
    			SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
    			Sensor s = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    			if (s != null) {
    				sensorMgr.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
    			}
    			sensorRegistered = true;
    		}
    	}
    }
    
    private void updateSpeedBearing(Location location) {
		// For gps it's bad way. It's widely used for testing purposes
    	if(!providerSupportsSpeed && locationLayer.getLastKnownLocation() != null){
    		if (locationLayer.getLastKnownLocation().distanceTo(location) > 3) {
				float d = location.distanceTo(locationLayer.getLastKnownLocation());
				if (d > 100) {
					d = 100;
				}
				location.setSpeed(d);
			}
    	}
    	if(!providerSupportsBearing && locationLayer.getLastKnownLocation() != null){
    		if(locationLayer.getLastKnownLocation().distanceTo(location) > 10){
    			location.setBearing(locationLayer.getLastKnownLocation().bearingTo(location));
    		}
    	}
	}
    
    public void setLocation(Location location){
    	
    	registerUnregisterSensor(location);
    	updateSpeedBearing(location);
    	
    	locationLayer.setLastKnownLocation(location);
    	if(calculateRouteOnGps){
    		routingHelper.setCurrentLocation(location);
    	}
    	if (location != null) {
			if (isMapLinkedToLocation()) {
				if(OsmandSettings.isAutoZoomEnabled(this) && location.hasSpeed()){
	    			int z = defineZoomFromSpeed(location.getSpeed(), mapView.getZoom());
	    			if(mapView.getZoom() != z){
	    				mapView.setZoom(z);
	    			}
	    		}
				if (location.hasBearing() && OsmandSettings.isRotateMapToBearing(this)) {
					mapView.setRotate(-location.getBearing());
				}
				mapView.setLatLon(location.getLatitude(), location.getLongitude());
			} else {
				if(backToLocation.getVisibility() != View.VISIBLE){
					backToLocation.setVisibility(View.VISIBLE);
				}
			}
		} else {
			if(backToLocation.getVisibility() != View.INVISIBLE){
				backToLocation.setVisibility(View.INVISIBLE);
			}
		}
    }
    
    public int defineZoomFromSpeed(float speed, int currentZoom){
    	speed *= 3.6;
    	if(speed < 4){
    		return currentZoom;
    	} else if(speed < 30){
    		// less than 30 - show 17 
    		return 17;
    	} else if(speed < 50){
    		return 16;
    	} else if(speed < 80){
    		return 15;
    	}
    	// more than 80 - show 14 (it is slow)
    	return 14;
    }

	public void navigateToPoint(LatLon point){
		if(point != null){
			OsmandSettings.setPointToNavigate(this, point.getLatitude(), point.getLongitude());
		} else {
			OsmandSettings.clearPointToNavigate(this);
		}
		routingHelper.setFinalAndCurrentLocation(point, null);
		if(point == null){
			calculateRouteOnGps = false;
		}
		navigationLayer.setPointToNavigate(point);
		updateNavigateToPointMenu();
	}
	
	public Location getLastKnownLocation(){
		return locationLayer.getLastKnownLocation();
	}
	
	public LatLon getMapLocation(){
		return new LatLon(mapView.getLatitude(), mapView.getLongitude());
	}
	
	public LatLon getPointToNavigate(){
		return navigationLayer.getPointToNavigate();
	}
	
	public RoutingHelper getRoutingHelper() {
		return routingHelper;
	}
    

	@Override
	public void onLocationChanged(Location location) {
		if(location != null && OsmandSettings.isSavingTrackToGpx(this)){
			savingTrackHelper.insertData(location.getLatitude(), location.getLongitude(), 
					location.getAltitude(), location.getSpeed(), location.getTime());
		}
		setLocation(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
		setLocation(null);
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	
	private boolean isRunningOnEmulator(){
		if (Build.DEVICE.equals("generic")) { //$NON-NLS-1$ 
			return true;
		}  
		return false;
	}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (LocationManager.GPS_PROVIDER.equals(provider)) {
			LocationProvider prov = service.getProvider(LocationManager.NETWORK_PROVIDER);
			if (LocationProvider.OUT_OF_SERVICE == status || LocationProvider.TEMPORARILY_UNAVAILABLE == status) {
				if(LocationProvider.OUT_OF_SERVICE == status){
					setLocation(null);
				}
				service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, this);
				providerSupportsBearing = prov == null ? false : prov.supportsBearing() && !isRunningOnEmulator();
				providerSupportsSpeed = prov == null ? false : prov.supportsSpeed() && !isRunningOnEmulator();
			} else if (LocationProvider.AVAILABLE == status) {
				service.removeUpdates(this);
				service.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, this);
				prov = service.getProvider(LocationManager.GPS_PROVIDER);
				providerSupportsBearing = prov == null ? false : prov.supportsBearing() && !isRunningOnEmulator();
				providerSupportsSpeed = prov == null ? false : prov.supportsSpeed() && !isRunningOnEmulator();
			}
		}
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		service.removeUpdates(this);
		
		SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorMgr.unregisterListener(this);
		sensorRegistered = false;
		
		OsmandSettings.setLastKnownMapLocation(this, (float) mapView.getLatitude(), (float) mapView.getLongitude());
		OsmandSettings.setLastKnownMapZoom(this, mapView.getZoom());
		if (wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(mapView.getMap() != OsmandSettings.getMapTileSource(this)){
			mapView.setMap(OsmandSettings.getMapTileSource(this));
		}
		if(!OsmandSettings.isRotateMapToBearing(this)){
			mapView.setRotate(0);
		}
		if(!OsmandSettings.isShowingViewAngle(this)){
			locationLayer.setHeading(null);
		}
		poiMapLayer.setFilter(OsmandSettings.getPoiFilterForMap(this));
		mapView.setMapPosition(OsmandSettings.getPositionOnMap(this));
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_READABLE);
		if(prefs != null && prefs.contains(OsmandSettings.LAST_KNOWN_MAP_LAT)){
			LatLon l = OsmandSettings.getLastKnownMapLocation(this);
			mapView.setLatLon(l.getLatitude(), l.getLongitude());
			mapView.setZoom(OsmandSettings.getLastKnownMapZoom(this));
		}
		backToLocation.setVisibility(View.INVISIBLE);
		
		

		if(mapView.getLayers().contains(poiMapLayer) != OsmandSettings.isShowingPoiOverMap(this)){
			if(OsmandSettings.isShowingPoiOverMap(this)){
				mapView.addLayer(poiMapLayer);
			} else {
				mapView.removeLayer(poiMapLayer);
			}
		}
		if(mapView.getLayers().contains(osmBugsLayer) != OsmandSettings.isShowingOsmBugs(this)){
			if(OsmandSettings.isShowingOsmBugs(this)){
				mapView.addLayer(osmBugsLayer);
			} else {
				mapView.removeLayer(osmBugsLayer);
			}
		}
		
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		service.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, this);
		LocationProvider prov = service.getProvider(LocationManager.GPS_PROVIDER);
		if(!service.isProviderEnabled(LocationManager.GPS_PROVIDER)){
			service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, this);
			prov = service.getProvider(LocationManager.NETWORK_PROVIDER);
		}
		
		providerSupportsBearing = prov == null ? false : prov.supportsBearing() && !isRunningOnEmulator();
		providerSupportsSpeed = prov == null ? false : prov.supportsSpeed() && !isRunningOnEmulator();
		
		
		if (wakeLock == null) {
			PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "com.osmand.map"); //$NON-NLS-1$
			wakeLock.acquire();
		}
	}
	
	
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		ResourceManager.getResourceManager().onLowMemory();
	}


	@Override
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		// when user start dragging 
		if(locationLayer.getLastKnownLocation() != null){
			if(isMapLinkedToLocation()){
				OsmandSettings.setSyncMapToGpsLocation(MapActivity.this, false);
			}
			if (backToLocation.getVisibility() != View.VISIBLE) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						backToLocation.setVisibility(View.VISIBLE);
					}
				});
			}
		}
	}
	
	public void setMapLocation(double lat, double lon){
		mapView.setLatLon(lat, lon);
		locationChanged(lat, lon, this);
	}
	
	public OsmandMapTileView getMapView() {
		return mapView;
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		// Attention : sensor produces a lot of events & can hang the system
		locationLayer.setHeading(event.values[0]);
	}
	
	private void updateNavigateToPointMenu(){
		if (navigateToPointMenu != null) {
			if (OsmandSettings.getPointToNavigate(this) != null) {
				navigateToPointMenu.setVisible(true);
			} else {
				navigateToPointMenu.setVisible(false);
			}
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		navigateToPointMenu = menu.findItem(R.id.map_navigate_to_point);
		updateNavigateToPointMenu();
		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.map_show_settings) {
    		final Intent settings = new Intent(MapActivity.this, SettingsActivity.class);
			startActivity(settings);
    		return true;
		} else if (item.getItemId() == R.id.map_show_gps_status) {
			Intent intent = new Intent();
			intent.setComponent(new ComponentName(GPS_STATUS_COMPONENT, GPS_STATUS_ACTIVITY));
			ResolveInfo resolved = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
			if(resolved != null){
				startActivity(intent);
			} else {
				Toast.makeText(this, getString(R.string.gps_status_app_not_found), Toast.LENGTH_LONG).show();
			}
			return true;
		} else if (item.getItemId() == R.id.map_mark_point) {
			contextMenuPoint(mapView.getLatitude(), mapView.getLongitude(), true);
			return true;
		} else if (item.getItemId() == R.id.map_get_directions) {
			getDirections(mapView.getLatitude(), mapView.getLongitude());
			return true;
		} else if (item.getItemId() == R.id.map_specify_point) {
			openChangeLocationDialog();
			return true;
    	}  else if (item.getItemId() == R.id.map_navigate_to_point) {
    		if(navigationLayer.getPointToNavigate() != null){
    			navigateToPoint(null);
    		} else {
    			navigateToPoint(new LatLon(mapView.getLatitude(), mapView.getLongitude()));
    		}
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    protected void getDirections(final double lat, final double lon){
    	if(navigationLayer.getPointToNavigate() == null){
			Toast.makeText(this, R.string.mark_final_location_first, Toast.LENGTH_LONG).show();
			return;
		}
    	Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.follow_route);
    	builder.setMessage(R.string.recalculate_route_to_your_location);
    	builder.setPositiveButton(R.string.follow, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Location map = new Location("map"); //$NON-NLS-1$
				map.setLatitude(lat);
				map.setLongitude(lon);
				calculateRouteOnGps = true;
				routingHelper.setFinalAndCurrentLocation(navigationLayer.getPointToNavigate(), map);
			}
    	});
    	builder.setNegativeButton(R.string.only_show, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				Location map = new Location("map"); //$NON-NLS-1$
				map.setLatitude(lat);
				map.setLongitude(lon);
				calculateRouteOnGps = false;
				routingHelper.setFinalAndCurrentLocation(navigationLayer.getPointToNavigate(), map);
			}
    	});
    	builder.show();
    }
    
    protected void reloadTile(final int zoom, final double latitude, final double longitude){
    	Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(R.string.context_menu_item_update_map_confirm);
    	builder.setNegativeButton(R.string.default_buttons_cancel, null);
    	builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int x = (int) MapUtils.getTileNumberX(zoom, longitude);
				int y = (int) MapUtils.getTileNumberY(zoom, latitude);
				ResourceManager.getResourceManager().clearTileImageForMap(null, mapView.getMap(), x, y, zoom);
				mapView.refreshMap();
			}
    	});
		builder.create().show();
    }
    
    protected void contextMenuPoint(final double latitude, final double longitude, boolean menu){
    	Builder builder = new AlertDialog.Builder(this);
    	Resources resources = this.getResources();
    	String[] res = new String[]{
        			resources.getString(R.string.context_menu_item_navigate_point),
        			resources.getString(R.string.context_menu_item_add_favorite),
        			resources.getString(R.string.context_menu_item_open_bug),
        			resources.getString(R.string.context_menu_item_create_poi),
        			resources.getString(R.string.context_menu_item_update_map),
        	};
    	builder.setItems(res, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0){
					navigateToPoint(new LatLon(latitude, longitude));
				} else if(which == 1){
					addFavouritePoint(latitude, longitude);
				} else if(which == 2){
					osmBugsLayer.openBug(MapActivity.this, getLayoutInflater(), mapView, latitude, longitude);
				} else if(which == 3){
					EditingPOIActivity activity = new EditingPOIActivity(MapActivity.this);
					activity.showCreateDialog(latitude, longitude);
				} else if(which == 4){
					reloadTile(mapView.getZoom(), latitude, longitude);
				}
			}
    	});
		builder.create().show();
    }
    
    
    protected void addFavouritePoint(double latitude, double longitude){
    	final Resources resources = this.getResources();
    	final FavouritePoint p = new FavouritesActivity.FavouritePoint();
    	p.setLatitude(latitude);
    	p.setLongitude(longitude);
    	p.setName(resources.getString(R.string.add_favorite_dialog_default_favourite_name));
    	
    	Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.add_favorite_dialog_top_text);
		final EditText editText = new EditText(this);
		builder.setView(editText);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FavouritesDbHelper helper = new FavouritesActivity.FavouritesDbHelper(MapActivity.this);
				p.setName(editText.getText().toString());
				boolean added = helper.addFavourite(p);
				if (added) {
					Toast.makeText(MapActivity.this, MessageFormat.format(resources.getString(R.string.add_favorite_dialog_favourite_added_template), p.getName()), Toast.LENGTH_SHORT)
							.show();
				}
				helper.close();
			}
		});
		builder.create().show();
    }

	private void openChangeLocationDialog() {
		NavigatePointActivity dlg = new NavigatePointActivity(this);
		dlg.showDialog();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}


    
}