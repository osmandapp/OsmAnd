package net.osmand.activities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.osmand.Algoritms;
import net.osmand.AmenityIndexRepository;
import net.osmand.LogUtil;
import net.osmand.OsmandSettings;
import net.osmand.PoiFilter;
import net.osmand.PoiFiltersHelper;
import net.osmand.R;
import net.osmand.ResourceManager;
import net.osmand.SQLiteTileSource;
import net.osmand.Version;
import net.osmand.OsmandSettings.ApplicationMode;
import net.osmand.activities.FavouritesActivity.FavouritePoint;
import net.osmand.activities.FavouritesActivity.FavouritesDbHelper;
import net.osmand.activities.search.SearchActivity;
import net.osmand.activities.search.SearchPoiFilterActivity;
import net.osmand.activities.search.SearchTransportActivity;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.data.preparation.MapTileDownloader;
import net.osmand.data.preparation.MapTileDownloader.DownloadRequest;
import net.osmand.data.preparation.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.ITileSource;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.render.RendererLayer;
import net.osmand.views.AnimateDraggingMapThread;
import net.osmand.views.ContextMenuLayer;
import net.osmand.views.FavoritesLayer;
import net.osmand.views.MapInfoLayer;
import net.osmand.views.OsmBugsLayer;
import net.osmand.views.OsmandMapTileView;
import net.osmand.views.POIMapLayer;
import net.osmand.views.PointLocationLayer;
import net.osmand.views.PointNavigationLayer;
import net.osmand.views.RouteInfoLayer;
import net.osmand.views.RouteLayer;
import net.osmand.views.TransportInfoLayer;
import net.osmand.views.TransportStopsLayer;
import net.osmand.views.YandexTrafficLayer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
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
import android.media.AudioManager;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ZoomControls;

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
	private RendererLayer rendererLayer;
	private YandexTrafficLayer trafficLayer;
	private OsmBugsLayer osmBugsLayer;
	private POIMapLayer poiMapLayer;
	private FavoritesLayer favoritesLayer;
	private TransportStopsLayer transportStopsLayer;
	private TransportInfoLayer transportInfoLayer;
	private PointLocationLayer locationLayer;
	private PointNavigationLayer navigationLayer;
	private MapInfoLayer mapInfoLayer;
	private ContextMenuLayer contextMenuLayer;
	private RouteInfoLayer routeInfoLayer;
	
	private SavingTrackHelper savingTrackHelper;
	private RoutingHelper routingHelper;
	
	
	private WakeLock wakeLock;
	private boolean sensorRegistered = false;

	private NotificationManager mNotificationManager;
	private Handler mapPositionHandler = null;
	private int APP_NOTIFICATION_ID;
	private int currentScreenOrientation;
	private int currentMapRotation;
	private boolean currentShowingAngle;
	
	private Dialog progressDlg = null;
	
	

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
				
		// for voice navigation
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
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
		mapView.addLayer(routeLayer, 1);
		rendererLayer = new RendererLayer();
		mapView.addLayer(rendererLayer, 1.1f);
		
		
		// 1.5. traffic layer
		trafficLayer = new YandexTrafficLayer();
		mapView.addLayer(trafficLayer, 1.5f);
		
		
		// 2. osm bugs layer
		osmBugsLayer = new OsmBugsLayer(this);
		// 3. poi layer
		poiMapLayer = new POIMapLayer();
		// 4. favorites layer
		favoritesLayer = new FavoritesLayer();
		// 5. transport layer
		transportStopsLayer = new TransportStopsLayer();
		// 5.5 transport info layer 
		transportInfoLayer = new TransportInfoLayer(TransportRouteHelper.getInstance());
		mapView.addLayer(transportInfoLayer, 5.5f);
		// 6. point navigation layer
		navigationLayer = new PointNavigationLayer();
		mapView.addLayer(navigationLayer, 6);
		// 7. point location layer 
		locationLayer = new PointLocationLayer();
		mapView.addLayer(locationLayer, 7);
		// 8. map info layer
		mapInfoLayer = new MapInfoLayer(this, routeLayer);
		mapView.addLayer(mapInfoLayer, 8);
		// 9. context menu layer 
		contextMenuLayer = new ContextMenuLayer(this);
		mapView.addLayer(contextMenuLayer, 9);
		// 10. route info layer
		routeInfoLayer = new RouteInfoLayer(routingHelper, (LinearLayout) findViewById(R.id.RouteLayout));
		mapView.addLayer(routeInfoLayer, 9);
		

		
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
		
		// Possibly use that method instead of 
		/*mapView.setOnLongClickListener(new OsmandMapTileView.OnLongClickListener(){
			@Override
			public boolean onLongPressEvent(PointF point) {
				LatLon l = mapView.getLatLonFromScreenPoint(point.x, point.y);
				return true;
			}
		});*/
		
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
    	// TODO keep this check?
    	if(routingHelper.isFollowingMode()){
    		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    		mNotificationManager.notify(APP_NOTIFICATION_ID, getNotification());
    	}
		if(progressDlg != null){
			progressDlg.dismiss();
			progressDlg = null;
		}
    	super.onStop();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	savingTrackHelper.close();
    	if(mNotificationManager != null){
    		mNotificationManager.cancel(APP_NOTIFICATION_ID);
    	}
    	MapTileDownloader.getInstance().removeDownloaderCallback(mapView);
    }
    
    
    
    private void registerUnregisterSensor(Location location){
    	boolean show = currentShowingAngle || currentMapRotation == OsmandSettings.ROTATE_MAP_COMPASS;
    	// show point view only if gps enabled
		if (sensorRegistered && (location == null || !show)) {
			Log.d(LogUtil.TAG, "Disable sensor"); //$NON-NLS-1$
			((SensorManager) getSystemService(SENSOR_SERVICE)).unregisterListener(this);
			sensorRegistered = false;
			locationLayer.setHeading(null);
		} else if (!sensorRegistered && (location != null && show)) {
			Log.d(LogUtil.TAG, "Enable sensor"); //$NON-NLS-1$
			SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
			Sensor s = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			if (s != null) {
				sensorMgr.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
			}
			sensorRegistered = true;
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
	    			if(mapView.getZoom() != z && !mapView.mapIsAnimating()){
	    				mapView.setZoom(z);
	    			}
	    		}
				if (location.hasBearing() && currentMapRotation == OsmandSettings.ROTATE_MAP_BEARING && !mapView.mapIsAnimating()) {
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
			OsmandSettings.setFollowingByRoute(MapActivity.this, false);
		}
		navigationLayer.setPointToNavigate(point);
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
			// that strange situation but it could happen?
			if(!Algoritms.objectEquals(currentLocationProvider, LocationManager.GPS_PROVIDER) &&  
					!useOnlyGPS()){
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
	
	private void updateApplicationModeSettings(){
		currentMapRotation = OsmandSettings.getRotateMap(this);
		currentShowingAngle = OsmandSettings.isShowingViewAngle(this);
		if(currentMapRotation == OsmandSettings.ROTATE_MAP_NONE){
			mapView.setRotate(0);
		}
		if(!currentShowingAngle){
			locationLayer.setHeading(null);
		}
		locationLayer.setAppMode(OsmandSettings.getApplicationMode(this));
		routingHelper.setAppMode(OsmandSettings.getApplicationMode(this));
		mapView.setMapPosition(OsmandSettings.getPositionOnMap(this));
		updateLayers();
	}
	
	private void updateLayers(){
		if(mapView.getLayers().contains(transportStopsLayer) != OsmandSettings.isShowingTransportOverMap(this)){
			if(OsmandSettings.isShowingTransportOverMap(this)){
				mapView.addLayer(transportStopsLayer, 5);
			} else {
				mapView.removeLayer(transportStopsLayer);
			}
		}
		if(mapView.getLayers().contains(osmBugsLayer) != OsmandSettings.isShowingOsmBugs(this)){
			if(OsmandSettings.isShowingOsmBugs(this)){
				mapView.addLayer(osmBugsLayer, 2);
			} else {
				mapView.removeLayer(osmBugsLayer);
			}
		}

		if(mapView.getLayers().contains(poiMapLayer) != OsmandSettings.isShowingPoiOverMap(this)){
			if(OsmandSettings.isShowingPoiOverMap(this)){
				mapView.addLayer(poiMapLayer, 3);
			} else {
				mapView.removeLayer(poiMapLayer);
			}
		}
		
		if(mapView.getLayers().contains(favoritesLayer) != OsmandSettings.isShowingFavorites(this)){
			if(OsmandSettings.isShowingFavorites(this)){
				mapView.addLayer(favoritesLayer, 4);
			} else {
				mapView.removeLayer(favoritesLayer);
			}
		}
		trafficLayer.setVisible(OsmandSettings.isShowingYandexTraffic(this));
	}
	
	private void updateMapSource(ITileSource newSource){
		if(mapView.getMap() instanceof SQLiteTileSource){
			((SQLiteTileSource)mapView.getMap()).closeDB();
		}
		ResourceManager.getResourceManager().setMapSource(newSource);
		mapView.setMap(newSource);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// TODO not commit it 
//		rendererLayer.setVisible(true);
		
		if(OsmandSettings.getMapOrientation(this) != getRequestedOrientation()){
			setRequestedOrientation(OsmandSettings.getMapOrientation(this));
		}
		currentScreenOrientation = getWindow().getWindowManager().getDefaultDisplay().getOrientation();
		
		// routing helper with current activity
		routingHelper = RoutingHelper.getInstance(this);
		ITileSource source = OsmandSettings.getMapTileSource(this);
		if(!Algoritms.objectEquals(mapView.getMap(), source)){
			updateMapSource(source);
		}
		
		updateApplicationModeSettings();

		favoritesLayer.reloadFavorites(this);
		poiMapLayer.setFilter(OsmandSettings.getPoiFilterForMap(this));
		backToLocation.setVisibility(View.INVISIBLE);
		
		

		
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
			wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "net.osmand.map"); //$NON-NLS-1$
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
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
	    	return true;
		}
		return false;
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
		float val = event.values[0];
		if(currentScreenOrientation == 1){
			val += 90;
		}
		if (currentMapRotation == OsmandSettings.ROTATE_MAP_COMPASS && !mapView.mapIsAnimating()) {
			if(Math.abs(mapView.getRotate() + val) > 10){
				mapView.setRotate(-val);
			}
		}
		if(currentShowingAngle){
			if(locationLayer.getHeading() == null || Math.abs(locationLayer.getHeading() - val) > 10){
				locationLayer.setHeading(val);
			}
		}
		
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean val = super.onPrepareOptionsMenu(menu);
		MenuItem navigateToPointMenu = menu.findItem(R.id.map_navigate_to_point);
		if (navigateToPointMenu != null) {
			navigateToPointMenu.setTitle(routingHelper.isRouteCalculated() ? R.string.stop_routing : R.string.stop_navigation);
			if (OsmandSettings.getPointToNavigate(this) != null) {
				navigateToPointMenu.setVisible(true);
			} else {
				navigateToPointMenu.setVisible(false);
			}
		}
		MenuItem muteMenu = menu.findItem(R.id.map_mute); 
		if(muteMenu != null){
			muteMenu.setTitle(routingHelper.getVoiceRouter().isMute() ? R.string.menu_mute_on : R.string.menu_mute_off);
			if (routingHelper.getFinalLocation() != null && routingHelper.isFollowingMode()) {
				muteMenu.setVisible(true);
			} else {
				muteMenu.setVisible(false);
			}
		}
		
		return val;
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
			Intent intent = new Intent(this, SearchTransportActivity.class);
			intent.putExtra(SearchTransportActivity.LAT_KEY, mapView.getLatitude());
			intent.putExtra(SearchTransportActivity.LON_KEY, mapView.getLongitude());
			startActivity(intent);
			return true;
//		} else if (item.getItemId() == R.id.map_mark_point) {
//			contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
//			return true;
		} else if (item.getItemId() == R.id.map_get_directions) {
			getDirections(mapView.getLatitude(), mapView.getLongitude(), true);
			return true;
		} else if (item.getItemId() == R.id.map_layers) {
			openLayerSelectionDialog();
			return true;
		} else if (item.getItemId() == R.id.map_specify_point) {
			openChangeLocationDialog();
			return true;
		} else if (item.getItemId() == R.id.map_mute) {
			routingHelper.getVoiceRouter().setMute(!routingHelper.getVoiceRouter().isMute());
			return true;
    	}  else if (item.getItemId() == R.id.map_navigate_to_point) {
    		if(navigationLayer.getPointToNavigate() != null){
    			if(routingHelper.isRouteCalculated()){
    				routingHelper.setFinalAndCurrentLocation(null, null);
    				OsmandSettings.setFollowingByRoute(MapActivity.this, false);
    				routingHelper.setFollowingMode(false);
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
    
    private ApplicationMode getAppMode(ToggleButton[] buttons){
    	for(int i=0; i<buttons.length; i++){
    		if(buttons[i] != null && buttons[i].isChecked() && i < ApplicationMode.values().length){
    			return ApplicationMode.values()[i];
    		}
    	}
    	return OsmandSettings.getApplicationMode(this);
    }
    
    protected void getDirections(final double lat, final double lon, boolean followEnabled){
    	if(navigationLayer.getPointToNavigate() == null){
			Toast.makeText(this, R.string.mark_final_location_first, Toast.LENGTH_LONG).show();
			return;
		}
    	Builder builder = new AlertDialog.Builder(this);
    	
    	View view = getLayoutInflater().inflate(R.layout.calculate_route, null);
    	final ToggleButton[] buttons = new ToggleButton[ApplicationMode.values().length];
    	buttons[ApplicationMode.CAR.ordinal()] = (ToggleButton) view.findViewById(R.id.CarButton);
    	buttons[ApplicationMode.BICYCLE.ordinal()] = (ToggleButton) view.findViewById(R.id.BicycleButton);
    	buttons[ApplicationMode.PEDESTRIAN.ordinal()] = (ToggleButton) view.findViewById(R.id.PedestrianButton);
    	ApplicationMode appMode = OsmandSettings.getApplicationMode(this);
    	for(int i=0; i< buttons.length; i++){
    		if(buttons[i] != null){
    			final int ind = i;
    			ToggleButton b = buttons[i];
    			b.setChecked(appMode == ApplicationMode.values()[i]);
    			b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if(isChecked){
							for (int j = 0; j < buttons.length; j++) {
								if (buttons[j] != null) {
									if(buttons[j].isChecked() != (ind == j)){
										buttons[j].setChecked(ind == j);
									}
								}
							}
						} else {
							// revert state
							boolean revert = true;
							for (int j = 0; j < buttons.length; j++) {
								if (buttons[j] != null) {
									if(buttons[j].isChecked()){
										revert = false;
										break;
									}
								}
							}
							if (revert){ 
								buttons[ind].setChecked(true);
							}
						}
					}
    			});
    		}
    	}
    	
    	DialogInterface.OnClickListener onlyShowCall = new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				ApplicationMode mode = getAppMode(buttons);
				Location map = new Location("map"); //$NON-NLS-1$
				map.setLatitude(lat);
				map.setLongitude(lon);
				routingHelper.setAppMode(mode);
				routingHelper.setFollowingMode(false);
				routingHelper.setFinalAndCurrentLocation(navigationLayer.getPointToNavigate(), map);
			}
    	};
    	
    	DialogInterface.OnClickListener followCall = new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ApplicationMode mode = getAppMode(buttons);
				// change global settings
				if (OsmandSettings.getApplicationMode(MapActivity.this) != mode) {
					Editor edit = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_WRITEABLE).edit();
					edit.putString(OsmandSettings.APPLICATION_MODE, mode.name());
					SettingsActivity.setAppMode(mode, edit);
					edit.commit();
					updateApplicationModeSettings();	
					mapView.refreshMap();
				}
				
				Location location = locationLayer.getLastKnownLocation();
				if(location == null){
					location = new Location("map"); //$NON-NLS-1$
					location.setLatitude(lat);
					location.setLongitude(lon);
				} 
				routingHelper.setAppMode(mode);
				routingHelper.setFollowingMode(true);
				routingHelper.setFinalAndCurrentLocation(navigationLayer.getPointToNavigate(), location);
				OsmandSettings.setFollowingByRoute(MapActivity.this, true);
			}
    	};
    	DialogInterface.OnClickListener showRoute = new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(MapActivity.this, ShowRouteInfoActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		};
    	
    	builder.setView(view);
    	if (followEnabled) {
    		builder.setTitle(R.string.follow_route);
			builder.setPositiveButton(R.string.follow, followCall);
			if (routingHelper.isRouterEnabled() && routingHelper.isRouteCalculated()) {
				builder.setNeutralButton(R.string.route_about, showRoute);
			}
			builder.setNegativeButton(R.string.only_show, onlyShowCall);
		} else {
			builder.setTitle(R.string.show_route);
			view.findViewById(R.id.TextView).setVisibility(View.GONE);
    		builder.setPositiveButton(R.string.show_route, onlyShowCall);
    		builder.setNegativeButton(R.string.default_buttons_cancel, null);
    	}
    	builder.show();
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
    	
    	progressDlg = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading_data));
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
				} finally {
					if(progressDlg !=null){
						progressDlg.dismiss();
						progressDlg = null;
					}
				}
			}
    	}, "LoadingPOI").start(); //$NON-NLS-1$
    	
    }
    
	private void openLayerSelectionDialog(){
		List<String> layersList = new ArrayList<String>();
		layersList.add(getString(R.string.layer_map));
		layersList.add(getString(R.string.layer_poi));
		layersList.add(getString(R.string.layer_transport));
		layersList.add(getString(R.string.layer_osm_bugs));
		layersList.add(getString(R.string.layer_favorites));
		final int routeInfoInd = routeInfoLayer.couldBeVisible() ? layersList.size() : -1;
		if(routeInfoLayer.couldBeVisible()){
			layersList.add(getString(R.string.layer_route));
		}
		final int transportRouteInfoInd = !TransportRouteHelper.getInstance().routeIsCalculated() ? - 1 : layersList.size(); 
		if(transportRouteInfoInd > -1){
			layersList.add(getString(R.string.layer_transport_route));
		}
		final int trafficInd = layersList.size();
		layersList.add(getString(R.string.layer_yandex_traffic));
		
		final boolean[] selected = new boolean[layersList.size()];
		Arrays.fill(selected, true);
		selected[1] = OsmandSettings.isShowingPoiOverMap(this);
		selected[2] = OsmandSettings.isShowingTransportOverMap(this);
		selected[3] = OsmandSettings.isShowingOsmBugs(this);
		selected[4] = OsmandSettings.isShowingFavorites(this);
		selected[trafficInd] = trafficLayer.isVisible();
		if(routeInfoInd != -1){
			selected[routeInfoInd] = routeInfoLayer.isUserDefinedVisible(); 
		}
		if(transportRouteInfoInd != -1){
			selected[transportRouteInfoInd] = transportInfoLayer.isVisible(); 
		}
		
		Builder builder = new AlertDialog.Builder(this);
		builder.setMultiChoiceItems(layersList.toArray(new String[layersList.size()]), selected, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item, boolean isChecked) {
				if (item == 0) {
					dialog.dismiss();
					selectMapLayer();
				} else if(item == 1){
					if(isChecked){
						selectPOIFilterLayer();
					}
					OsmandSettings.setShowPoiOverMap(MapActivity.this, isChecked);
				} else if(item == 2){
					OsmandSettings.setShowTransortOverMap(MapActivity.this, isChecked);
				} else if(item == 3){
					OsmandSettings.setShowingOsmBugs(MapActivity.this, isChecked);
				} else if(item == 4){
					OsmandSettings.setShowingFavorites(MapActivity.this, isChecked);
				} else if(item == routeInfoInd){
					routeInfoLayer.setVisible(isChecked);
				} else if(item == transportRouteInfoInd){
					transportInfoLayer.setVisible(isChecked);
				} else if(item == trafficInd){
					OsmandSettings.setShowingYandexTraffic(MapActivity.this, isChecked);
				}
				updateLayers();
				mapView.refreshMap();
			}
		});
		builder.show();
	}
	
	private void selectPOIFilterLayer(){
		final List<PoiFilter> userDefined = new ArrayList<PoiFilter>();
		List<String> list = new ArrayList<String>();
		list.add(getString(R.string.any_poi));
		for(PoiFilter f : PoiFiltersHelper.getUserDefinedPoiFilters(this)){
			if (!f.getFilterId().equals(PoiFilter.CUSTOM_FILTER_ID)) {
				userDefined.add(f);
				list.add(f.getName());
			}
		}
		for(AmenityType t : AmenityType.values()){
			list.add(AmenityType.toPublicString(t));
		}
		Builder builder = new AlertDialog.Builder(this);
		builder.setItems(list.toArray(new String[list.size()]), new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String filterId;
				if (which == 0) {
					filterId = PoiFiltersHelper.getOsmDefinedFilterId(null);
				} else if (which <= userDefined.size()) {
					filterId = userDefined.get(which - 1).getFilterId();
				} else {
					filterId = PoiFiltersHelper.getOsmDefinedFilterId(AmenityType.values()[which - userDefined.size() - 1]);
				}
				OsmandSettings.setPoiFilterForMap(MapActivity.this, filterId);
				poiMapLayer.setFilter(PoiFiltersHelper.getFilterById(MapActivity.this, filterId));
				mapView.refreshMap();
			}
			
		});
		builder.show();
	}
	
	private void selectMapLayer(){
		Map<String, String> entriesMap = SettingsActivity.getTileSourceEntries();
		Builder builder = new AlertDialog.Builder(this);
		final ArrayList<String> keys = new ArrayList<String>(entriesMap.keySet());
		builder.setItems(entriesMap.values().toArray(new String[entriesMap.size()]), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Editor edit = OsmandSettings.getWriteableEditor(MapActivity.this);
				edit.putString(OsmandSettings.MAP_TILE_SOURCES, keys.get(which));
				edit.commit();
				updateMapSource(OsmandSettings.getMapTileSource(MapActivity.this));
				mapView.refreshMap();
			}
			
		});
		builder.show();
	}

    
	public void contextMenuPoint(final double latitude, final double longitude){
		contextMenuPoint(latitude, longitude, null, null);
	}
	
    public void contextMenuPoint(final double latitude, final double longitude, List<String> additionalItems, 
    		final DialogInterface.OnClickListener additionalActions){
    	Builder builder = new AlertDialog.Builder(this);
    	Resources resources = this.getResources();
    	final int sizeAdditional = additionalActions == null || additionalItems == null ? 0 : additionalItems.size();
    	List<String> actions = new ArrayList<String>();
    	if(sizeAdditional > 0){
    		actions.addAll(additionalItems);
    	}
    	actions.add(resources.getString(R.string.context_menu_item_navigate_point));
    	actions.add(resources.getString(R.string.context_menu_item_search_poi));
		actions.add(resources.getString(R.string.context_menu_item_show_route));
		actions.add(resources.getString(R.string.context_menu_item_add_favorite));
		actions.add(resources.getString(R.string.context_menu_item_create_poi));
		actions.add(resources.getString(R.string.context_menu_item_open_bug));
		actions.add(resources.getString(R.string.context_menu_item_update_map));
    	builder.setItems(actions.toArray(new String[actions.size()]), new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which < sizeAdditional){
					additionalActions.onClick(dialog, which);
					return;
				}
				which -= sizeAdditional;
				if(which == 0){
					navigateToPoint(new LatLon(latitude, longitude));
				} else if(which == 1){
					Intent intent = new Intent(MapActivity.this, SearchPoiFilterActivity.class);
					intent.putExtra(SearchPoiFilterActivity.SEARCH_LAT, latitude);
					intent.putExtra(SearchPoiFilterActivity.SEARCH_LON, longitude);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				} else if(which == 2){
					getDirections(latitude, longitude, false);
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
				for (int i = 0; i < ar.length; i++) {
					ar[i] = points.get(i).getName();
				}
				b.setItems(ar, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(helper.editFavourite(points.get(which), latitude, longitude)){
							Toast.makeText(MapActivity.this, getString(R.string.fav_points_edited), Toast.LENGTH_SHORT).show();
						}
						helper.close();
						favoritesLayer.reloadFavorites(MapActivity.this);
						mapView.refreshMap();
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
				favoritesLayer.reloadFavorites(MapActivity.this);
				mapView.refreshMap();
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
