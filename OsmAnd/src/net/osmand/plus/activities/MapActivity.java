package net.osmand.plus.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities.GPXFileResult;
import net.osmand.LogUtil;
import net.osmand.Version;
import net.osmand.data.MapTileDownloader;
import net.osmand.data.MapTileDownloader.DownloadRequest;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.map.IMapLocationListener;
import net.osmand.osm.LatLon;
import net.osmand.plus.BusyIndicator;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchPoiFilterActivity;
import net.osmand.plus.activities.search.SearchTransportActivity;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointLocationLayer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MapActivity extends Activity implements IMapLocationListener, SensorEventListener {

	private static final String GPS_STATUS_ACTIVITY = "com.eclipsim.gpsstatus2.GPSStatus"; //$NON-NLS-1$
	private static final String GPS_STATUS_COMPONENT = "com.eclipsim.gpsstatus2"; //$NON-NLS-1$
	
	// stupid error but anyway hero 2.1 : always lost gps signal (temporarily unavailable) for timeout = 2000
	private static final int GPS_TIMEOUT_REQUEST = 1000;
	private static final int GPS_DIST_REQUEST = 5;
	// use only gps (not network) for 12 seconds 
	private static final int USE_ONLY_GPS_INTERVAL = 12000; 
	
	private static final int SHOW_POSITION_MSG_ID = 7;
	private static final int SHOW_POSITION_DELAY = 2500;
	
	
	private String currentLocationProvider = null;
	private boolean providerSupportsBearing = false;
	@SuppressWarnings("unused")
	private boolean providerSupportsSpeed = false;
	
	
	private long lastTimeAutoZooming = 0;
	
	private long lastTimeGPSLocationFixed = 0;
	
    /** Called when the activity is first created. */
	private OsmandMapTileView mapView;
	private MapActivityActions mapActions = new MapActivityActions(this);
	private MapActivityLayers mapLayers = new MapActivityLayers(this);
	
	private ImageButton backToLocation;
	
	private SavingTrackHelper savingTrackHelper;
	private RoutingHelper routingHelper;
	
	private WakeLock wakeLock;
	private boolean sensorRegistered = false;

	// Notification status
	private NotificationManager mNotificationManager;
	private int APP_NOTIFICATION_ID;
	// handler to show/hide trackball position 
	private Handler mapPositionHandler = null;
	// Current screen orientation
	private int currentScreenOrientation;
	// 
	private Dialog progressDlg = null;
	// App settings
	private OsmandSettings settings;
	// Store previous map rotation settings for rotate button
	private Integer previousMapRotate = null;

	private boolean isMapLinkedToLocation = false;
	private ProgressDialog startProgressDialog;
	
	private boolean isMapLinkedToLocation(){
		return isMapLinkedToLocation;
	}
	
	private Notification getNotification(){
		Intent notificationIndent = new Intent(this, MapActivity.class);
		notificationIndent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
		settings = getMyApplication().getSettings();		
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		// Full screen is not used here
//	     getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);
		startProgressDialog = new ProgressDialog(this);
		startProgressDialog.setCancelable(true);
		((OsmandApplication) getApplication()).checkApplicationIsBeingInitialized(this, startProgressDialog);
		// Do some action on close
		startProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				OsmandApplication app = ((OsmandApplication) getApplication());
				if (settings.MAP_VECTOR_DATA.get() && app.getResourceManager().getRenderer().isEmpty()) {
					Toast.makeText(MapActivity.this, getString(R.string.no_vector_map_loaded), Toast.LENGTH_LONG).show();
				}
			}
		});
		parseLaunchIntentLocation();
		
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
					ResourceManager mgr = getMyApplication().getResourceManager();
					mgr.tileDownloaded(request);
				}
				mapView.tileDownloaded(request);
				
			}
		});
		
				
		savingTrackHelper = new SavingTrackHelper(this);
		routingHelper = getMyApplication().getRoutingHelper();
		routingHelper.getVoiceRouter().onActivityInit(this);
		
		LatLon pointToNavigate = settings.getPointToNavigate();
		// This situtation could be when navigation suddenly crashed and after restarting
		// it tries to continue the last route
		if(!Algoritms.objectEquals(routingHelper.getFinalLocation(), pointToNavigate)){
			routingHelper.setFollowingMode(false);
			routingHelper.setFinalAndCurrentLocation(pointToNavigate, null);

		}
		if(settings.FOLLOW_TO_THE_ROUTE.get()){
			if(pointToNavigate == null){
				settings.FOLLOW_TO_THE_ROUTE.set(false);
			} else if(!routingHelper.isRouteCalculated()){
				Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.continue_follow_previous_route);
				builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						routingHelper.setFollowingMode(true);
						getMyApplication().showDialogInitializingCommandPlayer(MapActivity.this);
					}
				});
				builder.setNegativeButton(R.string.default_buttons_no, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						settings.APPLICATION_MODE.set(ApplicationMode.DEFAULT);
						updateApplicationModeSettings();
						settings.FOLLOW_TO_THE_ROUTE.set(false);
						routingHelper.setFinalAndCurrentLocation(null, null);
						mapView.refreshMap();
					}
				});
				builder.show();
			}
		}
		
		mapView.setMapLocationListener(this);
		mapLayers.createLayers(mapView);
		
		if(!settings.isLastKnownMapLocation()){
			LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
			Location location = null;
			try {
				location = service.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			}
			catch(IllegalArgumentException e) {
				Log.d(LogUtil.TAG, "GPS location provider not available"); //$NON-NLS-1$
			}

			if(location == null){
				try {
					location = service.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				} catch(IllegalArgumentException e) {
					Log.d(LogUtil.TAG, "Network location provider not available"); //$NON-NLS-1$
				}
			}
			if(location != null){
				mapView.setLatLon(location.getLatitude(), location.getLongitude());
				mapView.setZoom(14);
			}
		}
		
		
		backToLocation = (ImageButton)findViewById(R.id.BackToLocation);
		backToLocation.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				backToLocationImpl();
			}
			
		});
		
	}

	private OsmandApplication getMyApplication() {
		return ((OsmandApplication) getApplication());
	}
    
    @Override
	protected Dialog onCreateDialog(int id) {
		if(id == OsmandApplication.PROGRESS_DIALOG){
			return startProgressDialog;
		}
		return super.onCreateDialog(id);
	}
    
    public void changeZoom(int newZoom){
    	boolean changeLocation = settings.AUTO_ZOOM_MAP.get();
		mapView.getAnimatedDraggingThread().startZooming(newZoom, changeLocation);
		showAndHideMapPosition();
    }
    
    
    
    
	public void backToMainMenu() {
		final Dialog dlg = new Dialog(this, R.style.Dialog_Fullscreen);
		final View menuView = (View) getLayoutInflater().inflate(R.layout.menu, null);
		menuView.setBackgroundColor(Color.argb(200, 150, 150, 150));
		dlg.setContentView(menuView);
		MainMenuActivity.onCreateMainMenu(dlg.getWindow(), this);
		Animation anim = new Animation() {
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t) {
				ColorDrawable colorDraw = ((ColorDrawable) menuView.getBackground());
				colorDraw.setAlpha((int) (interpolatedTime * 200));
			}
		};
		anim.setDuration(700);
		anim.setInterpolator(new AccelerateInterpolator());
		menuView.setAnimation(anim);

		View showMap = dlg.findViewById(R.id.MapButton);
		showMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dlg.dismiss();
			}
		});
		View settingsButton = dlg.findViewById(R.id.SettingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent settings = new Intent(MapActivity.this, SettingsActivity.class);
				MapActivity.this.startActivity(settings);
				dlg.dismiss();
			}
		});

		View favouritesButton = dlg.findViewById(R.id.FavoritesButton);
		favouritesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent favorites = new Intent(MapActivity.this, FavouritesActivity.class);
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				MapActivity.this.startActivity(favorites);
				dlg.dismiss();
			}
		});

		View closeButton = dlg.findViewById(R.id.CloseButton);
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dlg.dismiss();

				getMyApplication().closeApplication();
				// 1. Work for almost all cases when user open apps from main menu
				Intent newIntent = new Intent(MapActivity.this, MainMenuActivity.class);
				newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				newIntent.putExtra(MainMenuActivity.APP_EXIT_KEY, MainMenuActivity.APP_EXIT_CODE);
				startActivity(newIntent);
				// 2. good analogue but user will come back to the current activity onResume()
				// so application is not reloaded !!!
				// moveTaskToBack(true);
				// 3. bad results if user comes from favorites
				// MapActivity.this.setResult(MainMenuActivity.APP_EXIT_CODE);
				// MapActivity.this.finish();
			}
		});

		View searchButton = dlg.findViewById(R.id.SearchButton);
		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent search = new Intent(MapActivity.this, SearchActivity.class);
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				MapActivity.this.startActivity(search);
				dlg.dismiss();
			}
		});
		menuView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dlg.dismiss();
			}
		});

		dlg.show();
		// Intent newIntent = new Intent(MapActivity.this, MainMenuActivity.class);
		// newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// startActivity(newIntent);
	}
 
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
			Intent newIntent = new Intent(MapActivity.this, SearchActivity.class);
			newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(newIntent);
            return true;
		} else if (!routingHelper.isFollowingMode()) {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.getRepeatCount() == 0) {
				if (mapView.isZooming()) {
					changeZoom(mapView.getZoom() + 2);
				} else {
					changeZoom(mapView.getZoom() + 1);
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.getRepeatCount() == 0) {
				changeZoom(mapView.getZoom() - 1);
				return true;
			}
		}
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
    	if(event.getAction() == MotionEvent.ACTION_MOVE && settings.USE_TRACKBALL_FOR_MOVEMENTS.get()){
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
    
    protected void setProgressDlg(Dialog progressDlg) {
		this.progressDlg = progressDlg;
	}
    
    protected Dialog getProgressDlg() {
		return progressDlg;
	}
    
    @Override
    protected void onStop() {
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
    	routingHelper.getVoiceRouter().onActivityStop(this);
    }
    
    
    
    private void registerUnregisterSensor(Location location){
    	boolean currentShowingAngle = settings.SHOW_VIEW_ANGLE.get(); 
    	int currentMapRotation = settings.ROTATE_MAP.get();
    	boolean show = (currentShowingAngle && location != null) || currentMapRotation == OsmandSettings.ROTATE_MAP_COMPASS;
    	// show point view only if gps enabled
		if (sensorRegistered && !show) {
			Log.d(LogUtil.TAG, "Disable sensor"); //$NON-NLS-1$
			((SensorManager) getSystemService(SENSOR_SERVICE)).unregisterListener(this);
			sensorRegistered = false;
			mapLayers.getLocationLayer().setHeading(null);
		} else if (!sensorRegistered && show) {
			Log.d(LogUtil.TAG, "Enable sensor"); //$NON-NLS-1$
			SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
			Sensor s = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			if (s != null) {
				sensorMgr.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
			}
			sensorRegistered = true;
		}
    }
    
    protected void backToLocationImpl() {
		backToLocation.setVisibility(View.INVISIBLE);
		PointLocationLayer locationLayer = mapLayers.getLocationLayer();
		if(!isMapLinkedToLocation()){
			isMapLinkedToLocation = true;
			if(locationLayer.getLastKnownLocation() != null){
				Location lastKnownLocation = locationLayer.getLastKnownLocation();
				AnimateDraggingMapThread thread = mapView.getAnimatedDraggingThread();
				int fZoom = mapView.getZoom() < 14 ? 14 : mapView.getZoom();
				thread.startMoving( lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), fZoom, false);
			}
		}
		if(locationLayer.getLastKnownLocation() == null){
			Toast.makeText(this, R.string.unknown_location, Toast.LENGTH_LONG).show();
		}
	}
    
    private void updateSpeedBearing(Location location) {
		// For network/gps it's bad way (not accurate). It's widely used for testing purposes
    	// possibly keep using only for emulator case
    	PointLocationLayer locationLayer = mapLayers.getLocationLayer();
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
				// Be aware only for emulator ! code is incorrect in case of airplane
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
    	if(location != null && settings.SAVE_TRACK_TO_GPX.get()){
			savingTrackHelper.insertData(location.getLatitude(), location.getLongitude(), 
					location.getAltitude(), location.getSpeed(), location.getTime(), settings);
		}
    	registerUnregisterSensor(location);
    	updateSpeedBearing(location);
    	mapLayers.getLocationLayer().setLastKnownLocation(location);
    	if(routingHelper.isFollowingMode()){
    		routingHelper.setCurrentLocation(location);
    	}
    	if (location != null) {
			if (isMapLinkedToLocation()) {
				if(settings.AUTO_ZOOM_MAP.get() && location.hasSpeed()){
	    			int z = defineZoomFromSpeed(location.getSpeed(), mapView.getZoom());
	    			if(mapView.getZoom() != z && !mapView.mapIsAnimating()){
	    				long now = System.currentTimeMillis();
	    				// prevent ui hysterisis (check time interval for autozoom)
	    				if(Math.abs(mapView.getZoom() - z) > 1 || (lastTimeAutoZooming - now) > 6500){
	    					lastTimeAutoZooming = now;
	    					mapView.setZoom(z);
	    				}
	    			}
	    		}
		    	int currentMapRotation = settings.ROTATE_MAP.get();
				if (location.hasBearing() && currentMapRotation == OsmandSettings.ROTATE_MAP_BEARING) {
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
    	// When location is changed we need to refresh map in order to show movement! 
    	mapView.refreshMap();
    }
    
    public int defineZoomFromSpeed(float speed, int currentZoom){
    	speed *= 3.6;
    	if(speed < 4){
    		return currentZoom;
    	} else if(speed < 33){
    		// less than 33 - show 17 
    		return 17;
    	} else if(speed < 53){
    		return 16;
    	} else if(speed < 83){
    		return 15;
    	}
    	// more than 80 - show 14 (it is slow)
    	return 14;
    }

	public void navigateToPoint(LatLon point){
		if(point != null){
			settings.setPointToNavigate(point.getLatitude(), point.getLongitude(), null);
		} else {
			settings.clearPointToNavigate();
		}
		routingHelper.setFinalAndCurrentLocation(point, routingHelper.getCurrentLocation(), routingHelper.getCurrentGPXRoute());
		mapLayers.getNavigationLayer().setPointToNavigate(point);
	}
	
	public Location getLastKnownLocation(){
		return mapLayers.getLocationLayer().getLastKnownLocation();
	}
	
	public LatLon getMapLocation(){
		return new LatLon(mapView.getLatitude(), mapView.getLongitude());
	}
	
	public LatLon getPointToNavigate(){
		return mapLayers.getNavigationLayer().getPointToNavigate();
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
	
	private boolean useOnlyGPS() {
		return (routingHelper != null && routingHelper.isFollowingMode())
				|| (System.currentTimeMillis() - lastTimeGPSLocationFixed) < USE_ONLY_GPS_INTERVAL || isRunningOnEmulator();
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
			if (location != null) {
				lastTimeGPSLocationFixed = location.getTime();
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
		routingHelper.setUiActivity(null);
		routingHelper.getVoiceRouter().onActivityStop(this);
		
		getMyApplication().getDaynightHelper().onMapPause();
		
		settings.setLastKnownMapLocation((float) mapView.getLatitude(), (float) mapView.getLongitude());
		AnimateDraggingMapThread animatedThread = mapView.getAnimatedDraggingThread();
		if(animatedThread.isAnimating() && animatedThread.getTargetZoom() != 0){
			settings.setMapLocationToShow(animatedThread.getTargetLatitude(), animatedThread.getTargetLongitude(), 
					animatedThread.getTargetZoom());
		}
		
		settings.setLastKnownMapZoom(mapView.getZoom());
		if (wakeLock != null) {
			wakeLock.release();
			wakeLock = null;
		}
		settings.MAP_ACTIVITY_ENABLED.set(false);
		getMyApplication().getResourceManager().interruptRendering();
		getMyApplication().getResourceManager().setBusyIndicator(null);
	}
	
	public void updateApplicationModeSettings(){
		boolean currentShowingAngle = settings.SHOW_VIEW_ANGLE.get(); 
    	int currentMapRotation = settings.ROTATE_MAP.get();
		if(currentMapRotation == OsmandSettings.ROTATE_MAP_NONE){
			mapView.setRotate(0);
		}
		if(!currentShowingAngle){
			mapLayers.getLocationLayer().setHeading(null);
		}
		routingHelper.setAppMode(settings.getApplicationMode());
		mapView.setMapPosition(settings.POSITION_ON_MAP.get());
		registerUnregisterSensor(getLastKnownLocation());
		mapLayers.updateLayers(mapView);
	}
	
	
	public void switchRotateMapMode(){
		if(settings.ROTATE_MAP.get() != OsmandSettings.ROTATE_MAP_COMPASS){
			previousMapRotate = settings.ROTATE_MAP.get();
			settings.ROTATE_MAP.set(OsmandSettings.ROTATE_MAP_COMPASS);
		} else if(previousMapRotate != null){
			settings.ROTATE_MAP.set(previousMapRotate);
		} else {
			settings.ROTATE_MAP.set(settings.ROTATE_MAP.getProfileDefaultValue());
		}
		registerUnregisterSensor(getLastKnownLocation());
		if(settings.ROTATE_MAP.get() != OsmandSettings.ROTATE_MAP_COMPASS){
			mapView.setRotate(0);
		}
		mapView.refreshMap();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(settings.MAP_SCREEN_ORIENTATION.get() != getRequestedOrientation()){
			setRequestedOrientation(settings.MAP_SCREEN_ORIENTATION.get());
			// can't return from this method we are not sure if activity will be recreated or not
		}
		mapLayers.getNavigationLayer().setPointToNavigate(settings.getPointToNavigate());
		
		currentScreenOrientation = getWindow().getWindowManager().getDefaultDisplay().getOrientation();
		
		// for voice navigation
		if(settings.AUDIO_STREAM_GUIDANCE.get() != null){
			setVolumeControlStream(settings.AUDIO_STREAM_GUIDANCE.get());
		} else {
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
		}
		
		mapLayers.updateMapSource(mapView, null);
		updateApplicationModeSettings();
		
		mapLayers.getPoiMapLayer().setFilter(settings.getPoiFilterForMap((OsmandApplication) getApplication()));
		
		backToLocation.setVisibility(View.INVISIBLE);
		isMapLinkedToLocation = false;
		if(routingHelper.isFollowingMode()){
			// by default turn off causing unexpected movements due to network establishing
			// best to show previous location
			isMapLinkedToLocation = true;
		}
		
		routingHelper.setUiActivity(this);
		routingHelper.getVoiceRouter().onActivityInit(this);
		if(routingHelper.isFollowingMode() && !Algoritms.objectEquals(settings.getPointToNavigate(), routingHelper.getFinalLocation())){
			routingHelper.setFinalAndCurrentLocation(settings.getPointToNavigate(), routingHelper.getCurrentLocation());
		}
		
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		try {
			service.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, gpsListener);
			currentLocationProvider = LocationManager.GPS_PROVIDER;
		} catch (IllegalArgumentException e) {
			Log.d(LogUtil.TAG, "GPS location provider not available"); //$NON-NLS-1$
		}
		if(!useOnlyGPS()){
			// try to always  ask for network provide : it is faster way to find location
			try {
				service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, networkListener);
				currentLocationProvider = LocationManager.NETWORK_PROVIDER;
			} catch(IllegalArgumentException e) {
				Log.d(LogUtil.TAG, "Network location provider not available"); //$NON-NLS-1$
			}
		}
		
		LocationProvider  prov = service.getProvider(currentLocationProvider); 
		providerSupportsBearing = prov == null ? false : prov.supportsBearing() && !isRunningOnEmulator();
		providerSupportsSpeed = prov == null ? false : prov.supportsSpeed() && !isRunningOnEmulator();
		
		if(settings != null && settings.isLastKnownMapLocation()){
			LatLon l = settings.getLastKnownMapLocation();
			mapView.setLatLon(l.getLatitude(), l.getLongitude());
			mapView.setZoom(settings.getLastKnownMapZoom());
		}
		
		
		if (wakeLock == null) {
			PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "net.osmand.map"); //$NON-NLS-1$
			wakeLock.acquire();
		}
		
		settings.MAP_ACTIVITY_ENABLED.set(true);
		checkExternalStorage();
		showAndHideMapPosition();
		
		LatLon cur = new LatLon(mapView.getLatitude(), mapView.getLongitude());
		LatLon latLon = settings.getAndClearMapLocationToShow();
		if (latLon != null && !latLon.equals(cur)) {
			mapView.getAnimatedDraggingThread().startMoving(latLon.getLatitude(),
					latLon.getLongitude(), settings.getMapZoomToShow(),  true);
		}
		
		
		View progress = findViewById(R.id.ProgressBar);
		if(progress != null){
			getMyApplication().getResourceManager().setBusyIndicator(new BusyIndicator(this, progress));
		}
		
		getMyApplication().getDaynightHelper().onMapResume();
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
	
	
	public void showAndHideMapPosition() {
		mapView.setShowMapPosition(true);
		if (mapPositionHandler == null) {
			mapPositionHandler = new Handler();
		}
		Message msg = Message.obtain(mapPositionHandler, new Runnable() {
			@Override
			public void run() {
				if (mapView.isShowMapPosition()) {
					mapView.setShowMapPosition(false);
					mapView.refreshMap();
				}
			}

		});
		msg.what = SHOW_POSITION_MSG_ID;
		mapPositionHandler.removeMessages(SHOW_POSITION_MSG_ID);
		mapPositionHandler.sendMessageDelayed(msg, SHOW_POSITION_DELAY);
	}
	
	

	@Override
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		// when user start dragging 
		if(mapLayers.getLocationLayer().getLastKnownLocation() != null){
			if(isMapLinkedToLocation()){
				isMapLinkedToLocation = false;
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
		if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_COMPASS) {
			mapView.setRotate(-val);
		}
		if(settings.SHOW_VIEW_ANGLE.get().booleanValue()){
			if(mapLayers.getLocationLayer().getHeading() == null || Math.abs(mapLayers.getLocationLayer().getHeading() - val) > 10){
				mapLayers.getLocationLayer().setHeading(val);
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
			if (settings.getPointToNavigate() != null) {
				navigateToPointMenu.setTitle(routingHelper.isRouteCalculated() ? R.string.stop_routing : R.string.stop_navigation);
				navigateToPointMenu.setVisible(true);
			} else {
				navigateToPointMenu.setVisible(false);
			}
		}
		MenuItem muteMenu = menu.findItem(R.id.map_mute); 
		if(muteMenu != null){
			if (routingHelper.getFinalLocation() != null && routingHelper.isFollowingMode()) {
				muteMenu.setTitle(routingHelper.getVoiceRouter().isMute() ? R.string.menu_mute_on : R.string.menu_mute_off);
				muteMenu.setVisible(true);
			} else {
				muteMenu.setVisible(false);
			}
		}
		return val;
	}
	
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.map_show_settings:
			final Intent intentSettings = new Intent(MapActivity.this,
					SettingsActivity.class);
			startActivity(intentSettings);
			return true;
		case R.id.map_where_am_i:
			backToLocationImpl();
			return true;
		case R.id.map_show_gps_status:
			startGpsStatusIntent();
			return true;
		case R.id.map_get_directions:
			Location loc = getLastKnownLocation();
			if(loc != null){
				getDirections(loc.getLatitude(), loc.getLongitude(), true);
			} else {
				getDirections(mapView.getLatitude(), mapView.getLongitude(), true);
			}
			return true;
		case R.id.map_layers:
			mapLayers.openLayerSelectionDialog(mapView);
			return true;
		case R.id.map_specify_point:
			// next 2 lines replaced for Issue 493, replaced by new 3 lines
			// NavigatePointActivity dlg = new NavigatePointActivity(this);
			// dlg.showDialog();
			Intent newIntent = new Intent(MapActivity.this, SearchActivity.class);
			newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(newIntent);
			return true;
		case R.id.map_mute:
			routingHelper.getVoiceRouter().setMute(
					!routingHelper.getVoiceRouter().isMute());
			return true;
		case R.id.map_navigate_to_point:
			if (mapLayers.getNavigationLayer().getPointToNavigate() != null) {
				navigateToPoint(null);
			} else {
    			navigateToPoint(new LatLon(mapView.getLatitude(), mapView.getLongitude()));
    		}
			mapView.refreshMap();
			return true;
		case R.id.map_gpx_routing:
			useGPXRouting();
			return true;
		case R.id.map_show_point_options:
			contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void startGpsStatusIntent() {
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(GPS_STATUS_COMPONENT,
				GPS_STATUS_ACTIVITY));
		ResolveInfo resolved = getPackageManager().resolveActivity(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		if (resolved != null) {
			startActivity(intent);
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.gps_status_app_not_found));
			builder.setPositiveButton(
					getString(R.string.default_buttons_yes),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							Intent intent = new Intent(Intent.ACTION_VIEW,
									Uri.parse("market://search?q=pname:"
											+ GPS_STATUS_COMPONENT));
							try {
								startActivity(intent);
							} catch (ActivityNotFoundException e) {
							}
						}
					});
			builder.setNegativeButton(
					getString(R.string.default_buttons_no), null);
			builder.show();
		}
	}
	
    private ApplicationMode getAppMode(ToggleButton[] buttons){
    	for(int i=0; i<buttons.length; i++){
    		if(buttons[i] != null && buttons[i].isChecked() && i < ApplicationMode.values().length){
    			return ApplicationMode.values()[i];
    		}
    	}
    	return settings.getApplicationMode();
    }
    
    
    protected void getDirections(final double lat, final double lon, boolean followEnabled){
    	if(mapLayers.getNavigationLayer().getPointToNavigate() == null){
			Toast.makeText(this, R.string.mark_final_location_first, Toast.LENGTH_LONG).show();
			return;
		}
    	Builder builder = new AlertDialog.Builder(this);
    	
    	View view = getLayoutInflater().inflate(R.layout.calculate_route, null);
    	final ToggleButton[] buttons = new ToggleButton[ApplicationMode.values().length];
    	buttons[ApplicationMode.CAR.ordinal()] = (ToggleButton) view.findViewById(R.id.CarButton);
    	buttons[ApplicationMode.BICYCLE.ordinal()] = (ToggleButton) view.findViewById(R.id.BicycleButton);
    	buttons[ApplicationMode.PEDESTRIAN.ordinal()] = (ToggleButton) view.findViewById(R.id.PedestrianButton);
    	ApplicationMode appMode = settings.getApplicationMode();
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
				Location location = new Location("map"); //$NON-NLS-1$
				location.setLatitude(lat);
				location.setLongitude(lon);
				routingHelper.setAppMode(mode);
				settings.FOLLOW_TO_THE_ROUTE.set(false);
				routingHelper.setFollowingMode(false);
				routingHelper.setFinalAndCurrentLocation(getPointToNavigate(), location);
			}
    	};
    	
    	DialogInterface.OnClickListener followCall = new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ApplicationMode mode = getAppMode(buttons);
				// change global settings
				boolean changed = settings.APPLICATION_MODE.set(mode);
				if (changed) {
					updateApplicationModeSettings();	
					mapView.refreshMap();
				}
				
				Location location = getLocationToStartFrom(lat, lon); 
				routingHelper.setAppMode(mode);
				settings.FOLLOW_TO_THE_ROUTE.set(true);
				routingHelper.setFollowingMode(true);
				routingHelper.setFinalAndCurrentLocation(getPointToNavigate(), location);
				dialog.dismiss();
				getMyApplication().showDialogInitializingCommandPlayer(MapActivity.this);
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
    
    protected void parseLaunchIntentLocation(){
    	Intent intent = getIntent();
    	if(intent != null && intent.getData() != null){
    		Uri data = intent.getData();
    		if("http".equalsIgnoreCase(data.getScheme()) && "download.osmand.net".equals(data.getHost()) &&
    				"/go".equals( data.getPath())) {
    			String lat = data.getQueryParameter("lat");
    			String lon = data.getQueryParameter("lon");
				if (lat != null && lon != null) {
					try {
						double lt = Double.parseDouble(lat);
						double ln = Double.parseDouble(lon);
						settings.setLastKnownMapLocation((float) lt, (float) ln);
						String zoom = data.getQueryParameter("z");
						if(zoom != null){
							settings.setLastKnownMapZoom(Integer.parseInt(zoom));
						}
					} catch (NumberFormatException e) {
					}
				}
    		}
    	}
    }
	
	public FavouritesDbHelper getFavoritesHelper() {
		return getMyApplication().getFavorites();
	}
	
	private void useGPXRouting() {
		final LatLon endForRouting = getPointToNavigate();
		mapLayers.selectGPXFileLayer(new CallbackWithObject<GPXFileResult>() {
			
			@Override
			public boolean processResult(final GPXFileResult result) {
				Builder builder = new AlertDialog.Builder(MapActivity.this);
				final boolean[] props = new boolean[]{false, false, false};
				builder.setMultiChoiceItems(new String[] { getString(R.string.gpx_option_reverse_route),
						getString(R.string.gpx_option_destination_point), getString(R.string.gpx_option_from_start_point) }, props,
						new OnMultiChoiceClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which, boolean isChecked) {
								props[which] = isChecked;
							}
						});
				builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						boolean reverse = props[0];
						boolean passWholeWay = props[2];
						ArrayList<List<Location>> locations = result.locations;
						List<Location> l = new ArrayList<Location>();
						for(List<Location> s : locations){
							l.addAll(s);
						}
						if(reverse){
							Collections.reverse(l);
						}
						
						Location loc = getLastKnownLocation();
						if(passWholeWay && loc != null){
							l.add(0, loc);
						}
						Location startForRouting = getLastKnownLocation();
						if(startForRouting == null && !l.isEmpty()){
							startForRouting = l.get(0);
						}
						LatLon endPoint = endForRouting;
						if((endPoint == null || !props[1]) && !l.isEmpty()){
							LatLon point = new LatLon(l.get(l.size() - 1).getLatitude(), l.get(l.size() - 1).getLongitude());
							settings.setPointToNavigate(point.getLatitude(), point.getLongitude(), null);
							endPoint = point;
							mapLayers.getNavigationLayer().setPointToNavigate(point);
						}
						mapView.refreshMap();
						if(endPoint != null){
							settings.FOLLOW_TO_THE_ROUTE.set(true);
							routingHelper.setFollowingMode(true);
							routingHelper.setFinalAndCurrentLocation(endPoint, startForRouting, l);
							getMyApplication().showDialogInitializingCommandPlayer(MapActivity.this);
						}
					}
				});
				builder.setNegativeButton(R.string.default_buttons_cancel, null);
				builder.show();
				return true;
			}
		});
	}
		
	public void contextMenuPoint(final double latitude, final double longitude){
		contextMenuPoint(latitude, longitude, null, null);
	}
	
    public void contextMenuPoint(final double latitude, final double longitude, List<String> additionalItems, 
    		final DialogInterface.OnClickListener additionalActions){
    	Builder builder = new AlertDialog.Builder(this);
    	final int sizeAdditional = additionalActions == null || additionalItems == null ? 0 : additionalItems.size();
    	List<String> actions = new ArrayList<String>();
    	if(sizeAdditional > 0){
    		actions.addAll(additionalItems);
    	}
    	final int[] contextMenuStandardActions = new int[]{
    			R.string.context_menu_item_navigate_point,
    			R.string.context_menu_item_show_route,
    			R.string.context_menu_item_search_poi,
    			R.string.context_menu_item_search_transport,
    			R.string.context_menu_item_add_favorite,
    			R.string.context_menu_item_share_location,
    			R.string.context_menu_item_create_poi,
    			R.string.context_menu_item_add_waypoint,
    			R.string.context_menu_item_open_bug,
    			R.string.context_menu_item_update_map,
    			R.string.context_menu_item_download_map
    	};
    	for(int j = 0; j<contextMenuStandardActions.length; j++){
    		actions.add(getResources().getString(contextMenuStandardActions[j]));
    	}
    	
    	builder.setItems(actions.toArray(new String[actions.size()]), new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which < sizeAdditional){
					additionalActions.onClick(dialog, which);
					return;
				}
				int standardId = contextMenuStandardActions[which - sizeAdditional];
				if(standardId == R.string.context_menu_item_navigate_point){
					navigateToPoint(new LatLon(latitude, longitude));
				} else if(standardId == R.string.context_menu_item_show_route){
					getDirections(latitude, longitude, false);
				} else if(standardId == R.string.context_menu_item_search_poi){
					Intent intent = new Intent(MapActivity.this, SearchPoiFilterActivity.class);
					intent.putExtra(SearchPoiFilterActivity.SEARCH_LAT, latitude);
					intent.putExtra(SearchPoiFilterActivity.SEARCH_LON, longitude);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				} else if(standardId == R.string.context_menu_item_search_transport){
					Intent intent = new Intent(MapActivity.this, SearchTransportActivity.class);
					intent.putExtra(SearchTransportActivity.LAT_KEY, latitude);
					intent.putExtra(SearchTransportActivity.LON_KEY, longitude);
					startActivity(intent);
				} else if(standardId == R.string.context_menu_item_add_favorite){
					mapActions.addFavouritePoint(latitude, longitude);
				} else if(standardId == R.string.context_menu_item_share_location){
					mapActions.shareLocation(latitude, longitude, mapView.getZoom());
				} else if(standardId == R.string.context_menu_item_create_poi){
					EditingPOIActivity activity = new EditingPOIActivity(MapActivity.this, (OsmandApplication) getApplication(), mapView);
					activity.showCreateDialog(latitude, longitude);
				} else if(standardId == R.string.context_menu_item_add_waypoint){
					mapActions.addWaypoint(latitude, longitude, savingTrackHelper);
				} else if(standardId == R.string.context_menu_item_open_bug){
					mapLayers.getOsmBugsLayer().openBug(MapActivity.this, getLayoutInflater(), mapView, latitude, longitude);
				} else if(standardId == R.string.context_menu_item_update_map){
					mapActions.reloadTile(mapView.getZoom(), latitude, longitude);
				} else if(standardId == R.string.context_menu_item_download_map){
					DownloadTilesDialog dlg = new DownloadTilesDialog(MapActivity.this, 
							(OsmandApplication) getApplication(), mapView);
					dlg.openDialog();
				}
			}
    	});
		builder.create().show();
    }
    
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
	
	public static void launchMapActivityMoveToTop(Activity activity){
		Intent newIntent = new Intent(activity, MapActivity.class);
		newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		activity.startActivity(newIntent);
	}

	private Location getLocationToStartFrom(final double lat, final double lon) {
		Location location = getLastKnownLocation();
		if(location == null){
			location = new Location("map"); //$NON-NLS-1$
			location.setLatitude(lat);
			location.setLongitude(lon);
		}
		return location;
	}

}
