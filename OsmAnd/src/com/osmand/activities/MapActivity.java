package com.osmand.activities;

import java.text.MessageFormat;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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
import com.osmand.osm.MapUtils;
import com.osmand.views.OsmandMapTileView;
import com.osmand.views.POIMapLayer;
import com.osmand.views.PointOfView;

public class MapActivity extends Activity implements LocationListener, IMapLocationListener {
    /** Called when the activity is first created. */
	private OsmandMapTileView mapView;
	
	private boolean linkLocationWithMap = true; 
	
	private Location lastKnownLocation = null;

	private ImageButton backToLocation;

	private ImageButton backToMenu;
	
	private PointOfView pointOfView;
	
	private POIMapLayer poiMapLayer;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
//	     getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
//	                                WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		
		
		setContentView(R.layout.main);
		mapView = (OsmandMapTileView) findViewById(R.id.MapView);
		mapView.setMapLocationListener(this);
		poiMapLayer = new POIMapLayer();
		poiMapLayer.setNodeManager(ResourceManager.getResourceManager().getPoiIndex());
		mapView.addLayer(poiMapLayer);
		
		
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
		
		pointOfView = (PointOfView)findViewById(R.id.PointOfView);
		
		backToLocation = (ImageButton)findViewById(R.id.BackToLocation);
		backToLocation.setVisibility(linkLocationWithMap ? View.INVISIBLE : View.VISIBLE);
		backToLocation.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(!linkLocationWithMap){
					linkLocationWithMap = true;
					backToLocation.setVisibility(View.INVISIBLE);
					if(lastKnownLocation != null){
						mapView.setLatLon(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
					}
				}
			}
			
		});
		
		
		backToMenu = (ImageButton)findViewById(R.id.BackToMenu);
		backToMenu.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
		           Intent intent = new Intent();
	                setResult(RESULT_OK, intent);
	                finish();
			}
			
		});
			
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		service.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
		
		
		
	}
    


	@Override
	public void onLocationChanged(Location location) {
		lastKnownLocation = location;
		if(linkLocationWithMap){
			mapView.setLatLon(location.getLatitude(), location.getLongitude());
		} 
		validatePointOfView();
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO when provider disabled reset lastKnownLocation!
		
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO when provider disabled reset lastKnownLocation!
	}
	
	public void validatePointOfView(){
		if(lastKnownLocation == null){
			if(pointOfView.isVisible()){
				pointOfView.setLocationX(-1);
				pointOfView.setLocationY(-1);
				pointOfView.setAreaRadius(0);
				pointOfView.invalidate();
			}
		} else {
			int newX = MapUtils.getPixelShiftX(mapView.getZoom(), 
					lastKnownLocation.getLongitude(), mapView.getLongitude(), mapView.getTileSize()) + 
					mapView.getWidth()/2;
			int newY = MapUtils.getPixelShiftY(mapView.getZoom(), 
					lastKnownLocation.getLatitude(), mapView.getLatitude() , mapView.getTileSize()) + 
					mapView.getHeight()/2;
			// TODO 	specify bearing!
			int radius = MapUtils.getLengthXFromMeters(mapView.getZoom(), mapView.getLatitude(), mapView.getLongitude(), 
					lastKnownLocation.getAccuracy(), mapView.getTileSize(), mapView.getWidth());
			
			pointOfView.setLocationX(newX);
			pointOfView.setLocationY(newY);
			pointOfView.setAreaRadius(radius);
			pointOfView.invalidate();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}
	
	
	@Override
	protected void onPause() {
		// TODO switch off gps
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		// TODO switch on gps
		super.onResume();
		if(mapView.getMap() != OsmandSettings.tileSource){
			mapView.setMap(OsmandSettings.tileSource);
		}
		if(mapView.getLayers().contains(poiMapLayer) != OsmandSettings.showPoiOverMap){
			if(OsmandSettings.showPoiOverMap){
				mapView.addLayer(poiMapLayer);
			} else {
				mapView.removeLayer(poiMapLayer);
			}
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
		if(lastKnownLocation != null){
			linkLocationWithMap = false;
			backToLocation.setVisibility(View.VISIBLE);
		}
		validatePointOfView();
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.getItemId() == R.id.map_show_location){
    		float f=  (Runtime.getRuntime().totalMemory())/ 1e6f;
    		String text = MessageFormat.format("Latitude : {0}, longitude : {1}, zoom : {2}, memory : {3}", mapView.getLatitude(), 
    				mapView.getLongitude(), mapView.getZoom(), f);
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