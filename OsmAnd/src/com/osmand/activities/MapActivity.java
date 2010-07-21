package com.osmand.activities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.FloatMath;
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

import com.osmand.Algoritms;
import com.osmand.AmenityIndexRepository;
import com.osmand.LogUtil;
import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.SQLiteTileSource;
import com.osmand.Version;
import com.osmand.activities.FavouritesActivity.FavouritePoint;
import com.osmand.activities.FavouritesActivity.FavouritesDbHelper;
import com.osmand.activities.search.SearchActivity;
import com.osmand.activities.search.SearchPoiFilterActivity;
import com.osmand.activities.search.SearchTransportActivity;
import com.osmand.data.Amenity;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.data.preparation.MapTileDownloader.DownloadRequest;
import com.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
import com.osmand.map.IMapLocationListener;
import com.osmand.map.ITileSource;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.views.AnimateDraggingMapThread;
import com.osmand.views.MapInfoLayer;
import com.osmand.views.OsmBugsLayer;
import com.osmand.views.OsmandMapTileView;
import com.osmand.views.POIMapLayer;
import com.osmand.views.PointLocationLayer;
import com.osmand.views.PointNavigationLayer;
import com.osmand.views.RouteLayer;
import com.osmand.views.TransportStopsLayer;

public class MapActivity extends Activity implements IMapLocationListener, SensorEventListener {

	private static final String GPS_STATUS_ACTIVITY = "com.eclipsim.gpsstatus2.GPSStatus"; //$NON-NLS-1$
	private static final String GPS_STATUS_COMPONENT = "com.eclipsim.gpsstatus2"; //$NON-NLS-1$
	
	// stupid error but anyway hero 2.1 : always lost gps signal (temporarily unavailable) for timeout = 2000
	private static final int GPS_TIMEOUT_REQUEST = 1000;
	private static final int GPS_DIST_REQUEST = 5;
	
	private boolean providerSupportsBearing = false;
	@SuppressWarnings("unused")
	private boolean providerSupportsSpeed = false;
	private String currentLocationProvider = null;
	
    /** Called when the activity is first created. */
	private OsmandMapTileView mapView;
	
	private ImageButton backToLocation;
	private ImageButton backToMenu;
	
	// the order of layer should be preserved ! when you are inserting new layer
	private RouteLayer routeLayer;
	private OsmBugsLayer osmBugsLayer;
	private POIMapLayer poiMapLayer;
	private TransportStopsLayer transportStopsLayer;
	private PointLocationLayer locationLayer;
	private PointNavigationLayer navigationLayer;
	private MapInfoLayer mapInfoLayer;
	
	private SavingTrackHelper savingTrackHelper;
	private RoutingHelper routingHelper;
	
	
	private WakeLock wakeLock;
	private boolean sensorRegistered = false;

	private MenuItem navigateToPointMenu;
	private MenuItem muteMenu;
	private NotificationManager mNotificationManager;
	private Handler mapPositionHandler = null;
	private int APP_NOTIFICATION_ID;
	
	

	private boolean isMapLinkedToLocation(){
		return OsmandSettings.isMapSyncToGpsLocation(this);
	}
	
	private Notification getNotification(){
		Intent notificationIndent = new Intent(this, MapActivity.class);
		notificationIndent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		Notification notification = new Notification(R.drawable.icon, "", //$NON-NLS-1$
				System.currentTimeMillis());
		notification.setLatestEventInfo(this, Version.APP_NAME,
				getString(R.string.go_back_to_osmand), PendingIntent.getActivity(
						this, 0, notificationIndent,
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
		mapView.setTrackBallDelegate(new OsmandMapTileView.OnTrackBallListener(){
			@Override
			public boolean onTrackBallEvent(MotionEvent e) {
				showAndHideMapPosition();
				return MapActivity.this.onTrackballEvent(e);
			}

			@Override
			public boolean onTrackBallPressed() {
				contextMenuPoint(mapView.getLatitude(), mapView.getLongitude(), true);
	        	return true;
			}
		});
		MapTileDownloader.getInstance().addDownloaderCallback(new IMapDownloaderCallback(){
			@Override
			public void tileDownloaded(DownloadRequest request) {
				if(request != null && !request.error && request.fileToSave != null){
					ResourceManager mgr = ResourceManager.getResourceManager();
					mgr.tileDownloaded(request);
				}
				mapView.tileDownloaded(request);
				
			}
		});
		
		mapView.setMapLocationListener(this);
		routingHelper = RoutingHelper.getInstance(this);
		// 1. route layer
		routeLayer = new RouteLayer(routingHelper);
		mapView.addLayer(routeLayer);
		// 2. osm bugs layer
		osmBugsLayer = new OsmBugsLayer(this);
		// 3. poi layer
		poiMapLayer = new POIMapLayer();
		// 4. transport layer
		transportStopsLayer = new TransportStopsLayer();
		// 5. point navigation layer
		navigationLayer = new PointNavigationLayer();
		mapView.addLayer(navigationLayer);
		// 6. point location layer 
		locationLayer = new PointLocationLayer();
		mapView.addLayer(locationLayer);
		// 7. map info layer
		mapInfoLayer = new MapInfoLayer(this, routeLayer);
		mapView.addLayer(mapInfoLayer);

		
		savingTrackHelper = new SavingTrackHelper(this);
		
		LatLon pointToNavigate = OsmandSettings.getPointToNavigate(this);
		
		
		if(!Algoritms.objectEquals(routingHelper.getFinalLocation(), pointToNavigate)){
			// there is no way how to clear mode. Only user can do : clear point to navigate, exit from app & set up new point.
			// that case help to not calculate route at all.
			routingHelper.setFollowingMode(false);
			routingHelper.setFinalAndCurrentLocation(pointToNavigate, null);

		}
		if(OsmandSettings.isFollowingByRoute(this)){
			if(pointToNavigate == null){
				OsmandSettings.setFollowingByRoute(this, false);
			} else if(!routingHelper.isRouteCalculated()){
				Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.continue_follow_previous_route);
				builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						routingHelper.setFollowingMode(true);
						
					}
				});
				builder.setNegativeButton(R.string.default_buttons_no, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						OsmandSettings.setFollowingByRoute(MapActivity.this, false);
						routingHelper.setFinalLocation(null);
						mapView.refreshMap();
					}
				});
				builder.show();
			}
		}
		
		navigationLayer.setPointToNavigate(pointToNavigate);
		
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_READABLE);
		if(prefs == null || !prefs.contains(OsmandSettings.LAST_KNOWN_MAP_LAT)){
			LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
			Location location = service.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if(location == null){
				location = service.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
			if(location != null){
				mapView.setLatLon(location.getLatitude(), location.getLongitude());
				mapView.setZoom(14);
			}
		}
		
		
		
		ZoomControls zoomControls = (ZoomControls) findViewById(R.id.ZoomControls);
		zoomControls.setOnZoomInClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapView.getAnimatedDraggingThread().stopAnimatingSync();
				mapView.getAnimatedDraggingThread().startZooming(mapView.getZoom(), mapView.getZoom() + 1);
				showAndHideMapPosition();
				// user can preview map manually switch off auto zoom while user don't press back to location
				if(OsmandSettings.isAutoZoomEnabled(MapActivity.this)){
					locationChanged(mapView.getLatitude(), mapView.getLongitude(), null);
				}
			}
		});
		zoomControls.setOnZoomOutClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapView.getAnimatedDraggingThread().stopAnimatingSync();
				mapView.getAnimatedDraggingThread().startZooming(mapView.getZoom(), mapView.getZoom() - 1);
				showAndHideMapPosition();
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
				backToLocationImpl();
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
				LatLon l = mapView.getLatLonFromScreenPoint(point.x, point.y);
				contextMenuPoint(l.getLatitude(), l.getLongitude(), false);
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
    	if(event.getAction() == MotionEvent.ACTION_MOVE && OsmandSettings.isUsingTrackBall(this)){
    		float x = event.getX();
    		float y = event.getY();
    		LatLon l = mapView.getLatLonFromScreenPoint(mapView.getCenterPointX() + x * 15, mapView.getCenterPointY() + y * 15);
    		setMapLocation(l.getLatitude(), l.getLongitude());
    		return true;
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
		// For network/gps it's bad way (not accurate). It's widely used for testing purposes
    	// possibly keep using only for emulator case
//		if (!providerSupportsSpeed
    	if (isRunningOnEmulator()
    			&& locationLayer.getLastKnownLocation() != null && location != null) {
			if (locationLayer.getLastKnownLocation().distanceTo(location) > 3) {
				float d = location.distanceTo(locationLayer.getLastKnownLocation());
				long time = location.getTime() - locationLayer.getLastKnownLocation().getTime();
				float speed;
				if (time == 0) {
					speed = 0;
				} else {
					speed = ((float) d * 1000) / time ;
				}

				if (speed > 100) {
					speed = 100;
				}
				location.setSpeed(speed);
			}
		}
    	if(!providerSupportsBearing && locationLayer.getLastKnownLocation() != null && location != null){
    		if(locationLayer.getLastKnownLocation().distanceTo(location) > 10){
    			location.setBearing(locationLayer.getLastKnownLocation().bearingTo(location));
    		}
    	}
	}
    
    public void setLocation(Location location){
    	if(Log.isLoggable(LogUtil.TAG, Log.DEBUG)){
    		Log.d(LogUtil.TAG, "Location changed " + location.getProvider()); //$NON-NLS-1$
    	}
    	if(location != null && OsmandSettings.isSavingTrackToGpx(this)){
			savingTrackHelper.insertData(location.getLatitude(), location.getLongitude(), 
					location.getAltitude(), location.getSpeed(), location.getTime());
		}
    	registerUnregisterSensor(location);
    	updateSpeedBearing(location);
    	
    	locationLayer.setLastKnownLocation(location);
    	if(routingHelper.isFollowingMode()){
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
			routingHelper.setFollowingMode(false);
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
	
	private boolean isRunningOnEmulator(){
		if (Build.DEVICE.equals("generic")) { //$NON-NLS-1$ 
			return true;
		}  
		return false;
	}
	
	private boolean useOnlyGPS(){
		return (routingHelper != null && routingHelper.isFollowingMode()) || isRunningOnEmulator();
	}
    

	// Working with location listeners
	private LocationListener networkListener = new LocationListener(){
		
		@Override
		public void onLocationChanged(Location location) {
			// double check about use only gps
			if(!useOnlyGPS()){
				setLocation(location);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			if(!useOnlyGPS()){
				setLocation(null);
			}
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			if(LocationProvider.OUT_OF_SERVICE == status && !useOnlyGPS()){
				setLocation(null);
			}
		}
	};
	private LocationListener gpsListener = new LocationListener(){
		@Override
		public void onLocationChanged(Location location) {
			setLocation(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
			setLocation(null);
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
			LocationProvider prov = service.getProvider(LocationManager.NETWORK_PROVIDER);
			// do not change provider for temporarily unavailable (possible bug for htc hero 2.1 ?)
			if (LocationProvider.OUT_OF_SERVICE == status /*|| LocationProvider.TEMPORARILY_UNAVAILABLE == status*/) {
				if(LocationProvider.OUT_OF_SERVICE == status){
					setLocation(null);
				}
				// do not use it in routing
				if (!useOnlyGPS() &&  
						service.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					if (!Algoritms.objectEquals(currentLocationProvider, LocationManager.NETWORK_PROVIDER)) {
						currentLocationProvider = LocationManager.NETWORK_PROVIDER;
						service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, this);
						providerSupportsBearing = prov == null ? false : prov.supportsBearing() && !isRunningOnEmulator();
						providerSupportsSpeed = prov == null ? false : prov.supportsSpeed() && !isRunningOnEmulator();
					}
				}
			} else if (LocationProvider.AVAILABLE == status) {
				if (!Algoritms.objectEquals(currentLocationProvider, LocationManager.GPS_PROVIDER)) {
					currentLocationProvider = LocationManager.GPS_PROVIDER;
					service.removeUpdates(networkListener);
					prov = service.getProvider(LocationManager.GPS_PROVIDER);
					providerSupportsBearing = prov == null ? false : prov.supportsBearing() && !isRunningOnEmulator();
					providerSupportsSpeed = prov == null ? false : prov.supportsSpeed() && !isRunningOnEmulator();
				}
			}

		}
	};
	

	
	@Override
	protected void onPause() {
		super.onPause();
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		service.removeUpdates(gpsListener);
		service.removeUpdates(networkListener);
		
		SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorMgr.unregisterListener(this);
		sensorRegistered = false;
		currentLocationProvider = null;
		
		OsmandSettings.setLastKnownMapLocation(this, (float) mapView.getLatitude(), (float) mapView.getLongitude());
		OsmandSettings.setLastKnownMapZoom(this, mapView.getZoom());
		if (wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}
		OsmandSettings.setMapActivityEnabled(this, false);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if(OsmandSettings.getMapOrientation(this) != getRequestedOrientation()){
			setRequestedOrientation(OsmandSettings.getMapOrientation(this));
		}
		
		// routing helper with current activity
		routingHelper = RoutingHelper.getInstance(this);
		ITileSource source = OsmandSettings.getMapTileSource(this);
		if(!Algoritms.objectEquals(mapView.getMap(), source)){
			if(mapView.getMap() instanceof SQLiteTileSource){
				((SQLiteTileSource)mapView.getMap()).closeDB();
			}
			ResourceManager.getResourceManager().setMapSource(source);
			mapView.setMap(source);
		}
		if(!OsmandSettings.isRotateMapToBearing(this)){
			mapView.setRotate(0);
		}
		if(!OsmandSettings.isShowingViewAngle(this)){
			locationLayer.setHeading(null);
		}
		locationLayer.setAppMode(OsmandSettings.getApplicationMode(this));
		routingHelper.setAppMode(OsmandSettings.getApplicationMode(this));
		
		
		poiMapLayer.setFilter(OsmandSettings.getPoiFilterForMap(this));
		mapView.setMapPosition(OsmandSettings.getPositionOnMap(this));
		
		backToLocation.setVisibility(View.INVISIBLE);
		
		
		if(mapView.getLayers().contains(transportStopsLayer) != OsmandSettings.isShowingTransportOverMap(this)){
			if(OsmandSettings.isShowingTransportOverMap(this)){
				mapView.addLayer(transportStopsLayer, routeLayer);
			} else {
				mapView.removeLayer(transportStopsLayer);
			}
		}

		if(mapView.getLayers().contains(poiMapLayer) != OsmandSettings.isShowingPoiOverMap(this)){
			if(OsmandSettings.isShowingPoiOverMap(this)){
				mapView.addLayer(poiMapLayer, routeLayer);
			} else {
				mapView.removeLayer(poiMapLayer);
			}
		}
		if(mapView.getLayers().contains(osmBugsLayer) != OsmandSettings.isShowingOsmBugs(this)){
			if(OsmandSettings.isShowingOsmBugs(this)){
				mapView.addLayer(osmBugsLayer, routeLayer);
			} else {
				mapView.removeLayer(osmBugsLayer);
			}
		}
		
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		service.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, gpsListener);
		currentLocationProvider = LocationManager.GPS_PROVIDER;
		if(!useOnlyGPS()){
			// try to always  ask for network provide : it is faster way to find location
			service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, networkListener);
			currentLocationProvider = LocationManager.NETWORK_PROVIDER;
		}
		
		LocationProvider  prov = service.getProvider(currentLocationProvider); 
		providerSupportsBearing = prov == null ? false : prov.supportsBearing() && !isRunningOnEmulator();
		providerSupportsSpeed = prov == null ? false : prov.supportsSpeed() && !isRunningOnEmulator();
		
		
		if (wakeLock == null) {
			PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "com.osmand.map"); //$NON-NLS-1$
			wakeLock.acquire();
		}
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_READABLE);
		if(prefs != null && prefs.contains(OsmandSettings.LAST_KNOWN_MAP_LAT)){
			LatLon l = OsmandSettings.getLastKnownMapLocation(this);
			mapView.setLatLon(l.getLatitude(), l.getLongitude());
			mapView.setZoom(OsmandSettings.getLastKnownMapZoom(this));
			LatLon latLon = OsmandSettings.getAndClearMapLocationToShow(this);
			LatLon cur = new LatLon(mapView.getLatitude(), mapView.getLongitude());
			if(latLon != null && !latLon.equals(cur)){
				mapView.getAnimatedDraggingThread().startMoving(cur.getLatitude(), cur.getLongitude(), 
						latLon.getLatitude(), latLon.getLongitude(), 
						mapView.getZoom(), OsmandSettings.getMapZoomToShow(this), 
						mapView.getSourceTileSize(), mapView.getRotate(), true);
			}
		}
		OsmandSettings.setMapActivityEnabled(this, true);
		checkExternalStorage();
		showAndHideMapPosition();
	}
	
	public void checkExternalStorage(){
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(state)){
			// ok
		} else if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)){
			Toast.makeText(this, R.string.sd_mounted_ro, Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, R.string.sd_unmounted, Toast.LENGTH_LONG).show();
		}
	}
	
	
	public void showAndHideMapPosition(){
		mapView.setShowMapPosition(true);
		if(mapPositionHandler == null){
			mapPositionHandler = new Handler();
		}
		Message msg = Message.obtain(mapPositionHandler, new Runnable(){
			@Override
			public void run() {
				if(mapView.isShowMapPosition()){
					mapView.setShowMapPosition(false);
					mapView.refreshMap();
				}
			}
			
		});
		msg.what = 7;
		mapPositionHandler.removeMessages(7);
		mapPositionHandler.sendMessageDelayed(msg, 2500);
		
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
			navigateToPointMenu.setTitle(routingHelper.isRouteCalculated() ? R.string.stop_routing : R.string.stop_navigation);
			if (OsmandSettings.getPointToNavigate(this) != null) {
				navigateToPointMenu.setVisible(true);
			} else {
				navigateToPointMenu.setVisible(false);
			}
		}
		if(muteMenu != null){
			muteMenu.setTitle(routingHelper.getVoiceRouter().isMute() ? R.string.menu_mute_on : R.string.menu_mute_off);
			if (routingHelper.getFinalLocation() != null && routingHelper.isFollowingMode()) {
				muteMenu.setVisible(true);
			} else {
				muteMenu.setVisible(false);
			}
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		navigateToPointMenu = menu.findItem(R.id.map_navigate_to_point);
		muteMenu = menu.findItem(R.id.map_mute);
		updateNavigateToPointMenu();
		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.map_show_settings) {
    		final Intent settings = new Intent(MapActivity.this, SettingsActivity.class);
			startActivity(settings);
    		return true;
		} else if (item.getItemId() == R.id.map_where_am_i) {
			backToLocationImpl();
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
		} else if (item.getItemId() == R.id.map_transport) {
			startActivity(new Intent(this, SearchTransportActivity.class));
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
		} else if (item.getItemId() == R.id.map_mute) {
			routingHelper.getVoiceRouter().setMute(!routingHelper.getVoiceRouter().isMute());
			updateNavigateToPointMenu();
			return true;
    	}  else if (item.getItemId() == R.id.map_navigate_to_point) {
    		if(navigationLayer.getPointToNavigate() != null){
    			if(routingHelper.isRouteCalculated()){
    				routingHelper.setFinalAndCurrentLocation(null, null);
    				routingHelper.setFollowingMode(false);
    				updateNavigateToPointMenu();
    			} else {
    				navigateToPoint(null);
    			}
    		} else {
    			navigateToPoint(new LatLon(mapView.getLatitude(), mapView.getLongitude()));
    		}
			mapView.refreshMap();
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
				Location location = locationLayer.getLastKnownLocation();
				if(location == null){
					location = new Location("map"); //$NON-NLS-1$
					location.setLatitude(lat);
					location.setLongitude(lon);
				} 
				routingHelper.setFollowingMode(true);
				routingHelper.setFinalAndCurrentLocation(navigationLayer.getPointToNavigate(), location);
				OsmandSettings.setFollowingByRoute(MapActivity.this, true);
				updateNavigateToPointMenu();
			}
    	});
    	if(routingHelper.isRouterEnabled() && routingHelper.isRouteCalculated()){
    		builder.setNeutralButton(R.string.route_about, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(MapActivity.this, ShowRouteInfoActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				}
    		});
    	}
    	builder.setNegativeButton(R.string.only_show, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				showRoute(lat, lon);
			}
    	});
    	builder.show();
    }
    
    private void showRoute(double lat, double lon){
    	Location map = new Location("map"); //$NON-NLS-1$
		map.setLatitude(lat);
		map.setLongitude(lon);
		routingHelper.setFollowingMode(false);
		updateNavigateToPointMenu();
		routingHelper.setFinalAndCurrentLocation(navigationLayer.getPointToNavigate(), map);
    }
    
    protected void reloadTile(final int zoom, final double latitude, final double longitude){
    	Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(R.string.context_menu_item_update_map_confirm);
    	builder.setNegativeButton(R.string.default_buttons_cancel, null);
    	builder.setPositiveButton(R.string.context_menu_item_update_map, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Rect pixRect = new Rect(0, 0, mapView.getWidth(), mapView.getHeight());
		    	RectF tilesRect = new RectF();
		    	mapView.calculateTileRectangle(pixRect, mapView.getCenterPointX(), mapView.getCenterPointY(), 
		    			mapView.getXTile(), mapView.getYTile(), tilesRect);
		    	int left = (int) FloatMath.floor(tilesRect.left);
				int top = (int) FloatMath.floor(tilesRect.top);
				int width = (int) (FloatMath.ceil(tilesRect.right) - left);
				int height = (int) (FloatMath.ceil(tilesRect.bottom) - top);
				for (int i = 0; i <width; i++) {
					for (int j = 0; j< height; j++) {
						ResourceManager.getResourceManager().clearTileImageForMap(null, mapView.getMap(), i + left, j + top, zoom);	
					}
				}
				
				
				mapView.refreshMap();
			}
    	});
    	builder.setNeutralButton(R.string.context_menu_item_update_poi, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				updatePoiDb(zoom, latitude, longitude);
				
			}
    	});
		builder.create().show();
    }
    
    protected void showToast(final String msg){
    	runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(MapActivity.this, msg, Toast.LENGTH_LONG).show();
			}
    	});
    }
    
    protected void updatePoiDb(int zoom, double latitude, double longitude){
    	if(zoom < 15){
    		Toast.makeText(this, getString(R.string.update_poi_is_not_available_for_zoom), Toast.LENGTH_SHORT).show();
    		return;
    	}
    	final List<AmenityIndexRepository> repos = ResourceManager.getResourceManager().searchAmenityRepositories(latitude, longitude);
    	if(repos.isEmpty()){
    		Toast.makeText(this, getString(R.string.update_poi_no_offline_poi_index), Toast.LENGTH_SHORT).show();
    		return;
    	}
    	Rect pixRect = new Rect(-mapView.getWidth()/2, -mapView.getHeight()/2, 3*mapView.getWidth()/2, 3*mapView.getHeight()/2);
    	RectF tileRect = new RectF();
    	mapView.calculateTileRectangle(pixRect, mapView.getCenterPointX(), mapView.getCenterPointY(), 
    			mapView.getXTile(), mapView.getYTile(), tileRect);
    	final double leftLon = MapUtils.getLongitudeFromTile(zoom, tileRect.left); 
    	final double topLat = MapUtils.getLatitudeFromTile(zoom, tileRect.top);
		final double rightLon = MapUtils.getLongitudeFromTile(zoom, tileRect.right);
		final double bottomLat = MapUtils.getLatitudeFromTile(zoom, tileRect.bottom);
    	
    	final ProgressDialog dlg = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading_data));
    	new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					List<Amenity> amenities = new ArrayList<Amenity>();
					boolean loadingPOIs = AmenityIndexRepository.loadingPOIs(amenities, leftLon, topLat, rightLon, bottomLat);
					if(!loadingPOIs){
						showToast(getString(R.string.update_poi_error_loading));
					} else {
						for(AmenityIndexRepository r  : repos){
							r.updateAmenities(amenities, leftLon, topLat, rightLon, bottomLat);
						}
						showToast(MessageFormat.format(getString(R.string.update_poi_success), amenities.size()));
						mapView.refreshMap();
					}
				} catch(Exception e) {
					Log.e(LogUtil.TAG, "Error updating local data", e); //$NON-NLS-1$
					showToast(getString(R.string.update_poi_error_local));
				}finally {
					dlg.dismiss();
				}
			}
    	}, "LoadingPOI").start(); //$NON-NLS-1$
    	
    }
    
    protected void contextMenuPoint(final double latitude, final double longitude, boolean menu){
    	Builder builder = new AlertDialog.Builder(this);
    	Resources resources = this.getResources();
    	String[] res = new String[]{
        			resources.getString(R.string.context_menu_item_navigate_point),
        			resources.getString(R.string.context_menu_item_search_poi),
        			resources.getString(R.string.context_menu_item_show_route),
        			resources.getString(R.string.context_menu_item_add_favorite),
        			resources.getString(R.string.context_menu_item_create_poi),
        			resources.getString(R.string.context_menu_item_open_bug),
        			resources.getString(R.string.context_menu_item_update_map)
        	};
    	builder.setItems(res, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0){
					navigateToPoint(new LatLon(latitude, longitude));
				} else if(which == 1){
					Intent intent = new Intent(MapActivity.this, SearchPoiFilterActivity.class);
					intent.putExtra(SearchPoiFilterActivity.SEARCH_LAT, latitude);
					intent.putExtra(SearchPoiFilterActivity.SEARCH_LON, longitude);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				} else if(which == 2){
					showRoute(latitude, longitude);
				} else if(which == 3){
					addFavouritePoint(latitude, longitude);
				} else if(which == 4){
					EditingPOIActivity activity = new EditingPOIActivity(MapActivity.this, mapView);
					activity.showCreateDialog(latitude, longitude);
				} else if(which == 5){
					osmBugsLayer.openBug(MapActivity.this, getLayoutInflater(), mapView, latitude, longitude);
				} else if(which == 6){
					reloadTile(mapView.getZoom(), latitude, longitude);
				}
			}
    	});
		builder.create().show();
    }
    
    
    protected void addFavouritePoint(final double latitude, final double longitude){
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
		builder.setNeutralButton(R.string.update_existing, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Builder b = new AlertDialog.Builder(MapActivity.this);
				final FavouritesDbHelper helper = new FavouritesActivity.FavouritesDbHelper(MapActivity.this);
				final List<FavouritePoint> points = helper.getFavouritePoints();
				final String[] ar = new String[points.size()];
				for(int i=0;i<ar.length; i++){	
					ar[i]=points.get(i).getName();
				}
				b.setItems(ar, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(helper.editFavourite(points.get(which), latitude, longitude)){
							Toast.makeText(MapActivity.this, getString(R.string.fav_points_edited), Toast.LENGTH_SHORT).show();
						}
						helper.close();
					}
				});
				if(ar.length == 0){
					Toast.makeText(MapActivity.this, getString(R.string.fav_points_not_exist), Toast.LENGTH_SHORT).show();
					helper.close();
				}  else {
					b.show();
				}
			}
			
		});
		builder.setPositiveButton(R.string.default_buttons_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FavouritesDbHelper helper = new FavouritesActivity.FavouritesDbHelper(MapActivity.this);
				p.setName(editText.getText().toString());
				boolean added = helper.addFavourite(p);
				if (added) {
					Toast.makeText(MapActivity.this, MessageFormat.format(getString(R.string.add_favorite_dialog_favourite_added_template), p.getName()), Toast.LENGTH_SHORT)
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

	protected void backToLocationImpl() {
		backToLocation.setVisibility(View.INVISIBLE);
		if(!isMapLinkedToLocation()){
			OsmandSettings.setSyncMapToGpsLocation(MapActivity.this, true);
			if(locationLayer.getLastKnownLocation() != null){
				Location lastKnownLocation = locationLayer.getLastKnownLocation();
				AnimateDraggingMapThread thread = mapView.getAnimatedDraggingThread();
				int fZoom = mapView.getZoom() < 15 ? 15 : mapView.getZoom();
				thread.startMoving(mapView.getLatitude(), mapView.getLongitude(), 
						lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), mapView.getZoom(), fZoom, 
						mapView.getSourceTileSize(), mapView.getRotate(), false);
			}
		}
	}


}
