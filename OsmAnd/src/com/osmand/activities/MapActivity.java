package com.osmand.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
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
import com.osmand.activities.FavouritesActivity.FavouritePoint;
import com.osmand.activities.FavouritesActivity.FavouritesDbHelper;
import com.osmand.activities.search.SearchActivity;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.map.IMapLocationListener;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.views.MapInfoLayer;
import com.osmand.views.OsmBugsLayer;
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
	private OsmBugsLayer osmBugsLayer;
	
	private WakeLock wakeLock;
	private boolean sensorRegistered = false;

	private MenuItem navigateToPointMenu;

	private boolean isMapLinkedToLocation(){
		return OsmandSettings.isMapSyncToGpsLocation(this);
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
		osmBugsLayer = new OsmBugsLayer(this);
		
		
		LatLon pointToNavigate = OsmandSettings.getPointToNavigate(this);
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
				contextMenuPoint(latitude, longitude);
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
    			locationLayer.setHeading(null);
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
    	locationLayer.setLastKnownLocation(location);
    	if (location != null) {
			if (isMapLinkedToLocation()) {
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

	public void navigateToPoint(LatLon point){
		if(point != null){
			OsmandSettings.setPointToNavigate(this, point.getLatitude(), point.getLongitude());
		} else {
			OsmandSettings.clearPointToNavigate(this);
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
			locationLayer.setHeading(null);
		}
		mapView.setMapPosition(OsmandSettings.getPositionOnMap(this));
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_READABLE);
		if(prefs != null && prefs.contains(OsmandSettings.LAST_KNOWN_MAP_LAT)){
			LatLon l = OsmandSettings.getLastKnownMapLocation(this);
			mapView.setLatLon(l.getLatitude(), l.getLongitude());
			mapView.setZoom(OsmandSettings.getLastKnownMapZoom(this));
		}
		backToLocation.setVisibility(View.INVISIBLE);
		poiMapLayer.setFilter(OsmandSettings.getPoiFilterForMap(this));
		

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
		} else if (item.getItemId() == R.id.map_mark_point) {
			contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
			return true;
		} else if (item.getItemId() == R.id.map_reload_tile) {
			reloadTile(mapView.getZoom(), mapView.getLatitude(), mapView.getLongitude());
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
    
    protected void reloadTile(final int zoom, final double latitude, final double longitude){
    	Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage("Tile image will be removed from file system. Do you want to reload tile from internet?");
    	builder.setNegativeButton("Cancel", null);
    	builder.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int x = (int) MapUtils.getTileNumberX(zoom, longitude);
				int y = (int) MapUtils.getTileNumberY(zoom, latitude);
				ResourceManager.getResourceManager().clearTileImageForMap(mapView.getMap(), x, y, zoom);
				mapView.refreshMap();
			}
    	});
		builder.create().show();
    }
    
    protected void contextMenuPoint(final double latitude, final double longitude){
    	Builder builder = new AlertDialog.Builder(this);
    	builder.setItems(new String[]{"Navigate to point", "Add to favourites", "Update map",  "Open osm bug"}, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0){
					navigateToPoint(new LatLon(latitude, longitude));
				} else if(which == 1){
					addFavouritePoint(latitude, longitude);
				} else if(which == 2){
					reloadTile(mapView.getZoom(), latitude, longitude);
				} else if(which == 3){
					osmBugsLayer.openBug(MapActivity.this, getLayoutInflater(), mapView, latitude, longitude);
				}
			}
    	});
		builder.create().show();
    }
    
    
    protected void addFavouritePoint(double latitude, double longitude){
    	final FavouritePoint p = new FavouritesActivity.FavouritePoint();
    	p.setLatitude(latitude);
    	p.setLongitude(longitude);
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
		NavigatePointActivity dlg = new NavigatePointActivity(this);
		dlg.showDialog();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}


    
}