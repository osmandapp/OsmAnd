package com.osmand.activities;

import java.text.MessageFormat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.osmand.IMapLocationListener;
import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.views.OsmandMapTileView;
import com.osmand.views.POIMapLayer;
import com.osmand.views.PointLocationLayer;

public class MapActivity extends Activity implements LocationListener, IMapLocationListener {
	
	private static final String KEY_LAST_LAT = "KEY_LAST_LAT"; 
	private static final String KEY_LAST_LON = "KEY_LAST_LON";
	private static final String KEY_LAST_ZOOM = "KEY_LAST_ZOOM";
	
    /** Called when the activity is first created. */
	private OsmandMapTileView mapView;
	
	private boolean linkLocationWithMap = true; 
	
	private ImageButton backToLocation;

	private ImageButton backToMenu;
	
	private PointLocationLayer locationLayer;
	
	private POIMapLayer poiMapLayer;
	private WakeLock wakeLock;
	
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		
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
		poiMapLayer.setNodeManager(ResourceManager.getResourceManager().getPoiIndex());
		mapView.addLayer(poiMapLayer);
		locationLayer = new PointLocationLayer();
		mapView.addLayer(locationLayer);
		
		SharedPreferences prefs = getPreferences(MODE_WORLD_READABLE);
		if(prefs != null && prefs.contains(KEY_LAST_LAT)){
			mapView.setLatLon(prefs.getFloat(KEY_LAST_LAT, 0f), prefs.getFloat(KEY_LAST_LON, 0f));
			mapView.setZoom(prefs.getInt(KEY_LAST_ZOOM, 3));
		} else {
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
		backToLocation.setVisibility(linkLocationWithMap ? View.INVISIBLE : View.VISIBLE);
		backToLocation.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(!linkLocationWithMap){
					linkLocationWithMap = true;
					backToLocation.setVisibility(View.INVISIBLE);
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
				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
			}
		});
	}
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	MapTileDownloader.getInstance().removeDownloaderCallback(mapView);
    }
    
    public void setLocation(Location location){
    	locationLayer.setLastKnownLocation(location);
    	if (location != null) {
			if (linkLocationWithMap) {
				mapView.setLatLon(location.getLatitude(), location.getLongitude());
			} 
		} else {
			if(!linkLocationWithMap){
				backToLocation.setVisibility(View.VISIBLE);
			}
			
		}
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
		SharedPreferences prefs = getPreferences(MODE_WORLD_READABLE);
		Editor edit = prefs.edit();
		edit.putFloat(KEY_LAST_LAT, (float) mapView.getLatitude());
		edit.putFloat(KEY_LAST_LON, (float) mapView.getLongitude());
		edit.putInt(KEY_LAST_ZOOM, mapView.getZoom());
		edit.commit();
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
			linkLocationWithMap = false;
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
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == R.id.map_show_location) {
			float f = (Runtime.getRuntime().totalMemory()) / 1e6f;
			String text = MessageFormat.format("Latitude : {0}, longitude : {1}, zoom : {2}, memory : {3}", mapView.getLatitude(), mapView
					.getLongitude(), mapView.getZoom(), f);
			Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
			return true;
		} else if (item.getItemId() == R.id.map_show_settings) {
    		final Intent settings = new Intent(MapActivity.this, SettingsActivity.class);
			startActivity(settings);
    		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
}