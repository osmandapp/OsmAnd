package com.osmand.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.osmand.activities.FavouritesActivity.FavouritePoint;
import com.osmand.activities.FavouritesActivity.FavouritesDbHelper;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.map.IMapLocationListener;
import com.osmand.osm.LatLon;
import com.osmand.views.MapInfoLayer;
import com.osmand.views.OsmandMapTileView;
import com.osmand.views.POIMapLayer;
import com.osmand.views.PointLocationLayer;
import com.osmand.views.PointNavigationLayer;

public class MapActivity extends Activity implements LocationListener, IMapLocationListener, SensorEventListener {
	
	
    /** Called when the activity is first created. */
	private OsmandMapTileView mapView;
	
	private ImageButton backToLocation;
	private ImageButton backToMenu;
	
	private PointLocationLayer locationLayer;
	private PointNavigationLayer navigationLayer;
	private POIMapLayer poiMapLayer;
	private MapInfoLayer mapInfoLayer;
	
	private WakeLock wakeLock;
	private boolean sensorRegistered = false;
	private Handler sensorHandler = new Handler();

	private final static String BACK_TO_LOCATION = "BACK_TO_LOCATION";
	private final static String POINT_NAVIGATE_LAT = "POINT_NAVIGATE_LAT";
	private final static String POINT_NAVIGATE_LON = "POINT_NAVIGATE_LON";
	
	
	private boolean isMapLinkedToLocation(){
		return getPreferences(MODE_WORLD_READABLE).getBoolean(BACK_TO_LOCATION, true);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
//	     getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
//	                                WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		
		setContentView(R.layout.main);
		mapView = (OsmandMapTileView) findViewById(R.id.MapView);
		MapTileDownloader.getInstance().addDownloaderCallback(mapView);
		mapView.setMapLocationListener(this);
		poiMapLayer = new POIMapLayer();
		mapView.addLayer(poiMapLayer);
		navigationLayer = new PointNavigationLayer();
		mapView.addLayer(navigationLayer);
		locationLayer = new PointLocationLayer();
		mapView.addLayer(locationLayer);
		mapInfoLayer = new MapInfoLayer(this);
		mapView.addLayer(mapInfoLayer);
		
		
		
		SharedPreferences lprefs = getPreferences(MODE_WORLD_READABLE);
		if(lprefs.contains(POINT_NAVIGATE_LAT)){
			navigationLayer.setPointToNavigate(new LatLon(lprefs.getFloat(POINT_NAVIGATE_LAT, 0), 
					lprefs.getFloat(POINT_NAVIGATE_LON, 0)));
		}
		
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_READABLE);
		if(prefs == null || !prefs.contains(OsmandSettings.LAST_KNOWN_MAP_LAT)){
			LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
			Location location = service.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if(location != null){
				mapView.setLatLon(location.getLatitude(), location.getLongitude());
				mapView.setZoom(14);
			}
		}
		
		
		
		ZoomControls zoomControls = (ZoomControls) findViewById(R.id.ZoomControls01);
		zoomControls.setOnZoomInClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapView.setZoom(mapView.getZoom() + 1);
			}
		});
		zoomControls.setOnZoomOutClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapView.setZoom(mapView.getZoom() - 1);
			}
		});
		backToLocation = (ImageButton)findViewById(R.id.BackToLocation);
		backToLocation.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				backToLocation.setVisibility(View.INVISIBLE);
				if(!isMapLinkedToLocation()){
					getPreferences(MODE_WORLD_READABLE).edit().putBoolean(BACK_TO_LOCATION, true).commit();
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
				newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(newIntent);
			}
		});
	}
    
 
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
//			Intent newIntent = new Intent(MapActivity.this, MainMenuActivity.class);
//			newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			startActivity(newIntent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	MapTileDownloader.getInstance().removeDownloaderCallback(mapView);
    }
    
    public void setLocation(Location location){
    	// Do very strange manipulation to call redraw only once
    	
    	// show point view only if gps enabled
    	if(location == null){
    		if(sensorRegistered) {
    			Log.d(LogUtil.TAG, "Disable sensor");
    			((SensorManager) getSystemService(SENSOR_SERVICE)).unregisterListener(this);
    			sensorRegistered = false;
    			locationLayer.setHeading(null, true);
    		}
    	} else {
    		if(!sensorRegistered && OsmandSettings.isShowingViewAngle(this)){
    			Log.d(LogUtil.TAG, "Enable sensor");
    			SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
    			Sensor s = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    			if (s != null) {
    				sensorMgr.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
    			}
    			sensorRegistered = true;
    		}
    	}
    	Log.d(LogUtil.TAG, "Location changed");
    	locationLayer.setLastKnownLocation(location, true);
    	if (location != null) {
			if (isMapLinkedToLocation()) {
				if (location.hasBearing() && OsmandSettings.isRotateMapToBearing(this)) {
					mapView.setRotateWithLocation(-location.getBearing(), location.getLatitude(), location.getLongitude());
				} else {
					mapView.setLatLon(location.getLatitude(), location.getLongitude());
				}
			} else {
				mapView.refreshMap();
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

	public void navigateToPoint(LatLon point){
		navigationLayer.setPointToNavigate(point);
	}
	
	public Location getLastKnownLocation(){
		return locationLayer.getLastKnownLocation();
	}
	
	public LatLon getPointToNavigate(){
		return navigationLayer.getPointToNavigate();
	}
    

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
		if(LocationProvider.OUT_OF_SERVICE == status){
			setLocation(null);
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
			locationLayer.setHeading(null, true);
		}
		mapView.setMapPosition(OsmandSettings.getPositionOnMap(this));
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_READABLE);
		if(prefs != null && prefs.contains(OsmandSettings.LAST_KNOWN_MAP_LAT)){
			LatLon l = OsmandSettings.getLastKnownMapLocation(this);
			mapView.setLatLon(l.getLatitude(), l.getLongitude());
			mapView.setZoom(OsmandSettings.getLastKnownMapZoom(this));
		}
		if(getLastKnownLocation() != null && !isMapLinkedToLocation()){
			backToLocation.setVisibility(View.VISIBLE);
		} else {
			backToLocation.setVisibility(View.INVISIBLE);
		}
		

		if(mapView.getLayers().contains(poiMapLayer) != OsmandSettings.isShowingPoiOverMap(this)){
			if(OsmandSettings.isShowingPoiOverMap(this)){
				mapView.addLayer(poiMapLayer);
			} else {
				mapView.removeLayer(poiMapLayer);
			}
		}
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		service.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, this);
		
		
		if (wakeLock == null) {
			PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "com.osmand.map");
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
				getPreferences(MODE_WORLD_READABLE).edit().putBoolean(BACK_TO_LOCATION, false).commit();
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
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		MenuItem item = menu.findItem(R.id.map_navigate_to_point);
		if (item != null) {
			if (getPreferences(MODE_WORLD_READABLE).contains(POINT_NAVIGATE_LAT)) {
				item.setTitle(R.string.stop_navigation);
			} else {
				item.setTitle(R.string.navigate_to_point);
			}
		}
		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.map_show_settings) {
    		final Intent settings = new Intent(MapActivity.this, SettingsActivity.class);
			startActivity(settings);
    		return true;
		} else if (item.getItemId() == R.id.map_add_to_favourite) {
			addFavouritePoint();
			return true;
		} else if (item.getItemId() == R.id.map_specify_point) {
			openChangeLocationDialog();
			return true;
    	}  else if (item.getItemId() == R.id.map_navigate_to_point) {
    		if(navigationLayer.getPointToNavigate() != null){
    			getPreferences(MODE_WORLD_READABLE).edit().remove(POINT_NAVIGATE_LAT).remove(POINT_NAVIGATE_LON).commit();
    			item.setTitle(R.string.navigate_to_point);
    			navigateToPoint(null);
    		} else {
    			getPreferences(MODE_WORLD_READABLE).edit().
    				putFloat(POINT_NAVIGATE_LAT, (float) mapView.getLatitude()).
    				putFloat(POINT_NAVIGATE_LON, (float) mapView.getLongitude()).commit();
    			item.setTitle(R.string.stop_navigation);
    			navigateToPoint(new LatLon(mapView.getLatitude(), mapView.getLongitude()));
    		}
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    private void addFavouritePoint(){
    	final FavouritePoint p = new FavouritesActivity.FavouritePoint();
    	p.setLatitude(mapView.getLatitude());
    	p.setLongitude(mapView.getLongitude());
    	p.setName("Favourite");
    	
    	Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Input name of favourite point");
		final EditText editText = new EditText(this);
		builder.setView(editText);
		builder.setNegativeButton("Cancel", null);
		builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FavouritesDbHelper helper = new FavouritesActivity.FavouritesDbHelper(MapActivity.this);
				p.setName(editText.getText().toString());
				boolean added = helper.addFavourite(p);
				if (added) {
					Toast.makeText(MapActivity.this, "Favourite point " + p.getName() + " was succesfully added.", Toast.LENGTH_SHORT)
							.show();
				}
				helper.close();
			}
		});
		builder.create().show();
    }

	private void openChangeLocationDialog() {
		NavigatePointActivity dlg = new NavigatePointActivity(this, mapView);
		dlg.showDialog();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// using that strange technique because sensor produces a lot of events & hangs the system
		locationLayer.setHeading(event.values[0], true);
		if(!sensorHandler.hasMessages(1) && locationLayer.isLocationVisible(locationLayer.getLastKnownLocation())){
			Message m = Message.obtain(sensorHandler, new Runnable(){
				@Override
				public void run() {
					mapView.refreshMap();
				}
			});
			m.what = 1;
			sensorHandler.sendMessage(m);
		}
	}
	
    
}