package com.osmand.activities;

import java.text.MessageFormat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.data.preparation.MapTileDownloader;
import com.osmand.map.IMapLocationListener;
import com.osmand.osm.LatLon;
import com.osmand.views.OsmandMapTileView;
import com.osmand.views.POIMapLayer;
import com.osmand.views.PointLocationLayer;
import com.osmand.views.PointNavigationLayer;

public class MapActivity extends Activity implements LocationListener, IMapLocationListener {
	
	
    /** Called when the activity is first created. */
	private OsmandMapTileView mapView;
	
	private boolean linkLocationWithMap = true; 
	private ImageButton backToLocation;
	private ImageButton backToMenu;
	
	private PointLocationLayer locationLayer;
	private PointNavigationLayer navigationLayer;
	private POIMapLayer poiMapLayer;
	
	private WakeLock wakeLock;
	
	
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
		
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_READABLE);
		if(prefs != null && prefs.contains(OsmandSettings.LAST_KNOWN_MAP_LAT)){
			LatLon l = OsmandSettings.getLastKnownMapLocation(this);
			mapView.setLatLon(l.getLatitude(), l.getLongitude());
			mapView.setZoom(OsmandSettings.getLastKnownMapZoom(this));
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
    	// Do very strange manipulation to call redraw only once
    	locationLayer.setLastKnownLocation(location, true);
    	if (location != null) {
			if (linkLocationWithMap) {
				if (location.hasBearing() && OsmandSettings.isRotateMapToBearing(this)) {
					mapView.setRotateWithLocation(-location.getBearing(), location.getLatitude(), location.getLongitude());
				} else {
					mapView.setLatLon(location.getLatitude(), location.getLongitude());
				}
			} else {
				mapView.prepareImage();
			}
		} else {
			if (!linkLocationWithMap) {
				backToLocation.setVisibility(View.VISIBLE);
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
    	}  else if (item.getItemId() == R.id.map_navigate_to_point) {
    		if(navigationLayer.getPointToNavigate() != null){
    			item.setTitle(R.string.navigate_to_point);
    			navigateToPoint(null);
    		} else {
    			item.setTitle(R.string.stop_navigation);
    			navigateToPoint(new LatLon(mapView.getLatitude(), mapView.getLongitude()));
    		}
    	}
    	return super.onOptionsItemSelected(item);
    }
    
}