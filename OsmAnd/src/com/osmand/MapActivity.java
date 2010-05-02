package com.osmand;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ZoomControls;

import com.osmand.osm.MapUtils;

public class MapActivity extends Activity implements LocationListener, IMapLocationListener {
    /** Called when the activity is first created. */
	private OsmandMapTileView mapView;
	
	private boolean linkLocationWithMap = true; 
	
	private Location lastKnownLocation = null;

	private ImageButton backToLocation;

	private PointOfView pointOfView;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
//	     getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
//	                                WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		
		
		setContentView(R.layout.main);
		mapView = (OsmandMapTileView) findViewById(R.id.MapView);
		mapView.setFileWithTiles(new File(Environment.getExternalStorageDirectory(), "osmand/tiles/"));
		mapView.addMapLocationListener(this);
		
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
		
		Button goToSettings = (Button)findViewById(R.id.GoToSettingsButton);
		goToSettings.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				final Intent settings = new Intent(MapActivity.this, SettingsActivity.class);
				startActivity(settings);
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
	protected void onPause() {
		// TODO switch off gps
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		// TODO switch on gps
		super.onResume();
	}


	@Override
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		// when user 
		if(source == mapView && lastKnownLocation != null){
			linkLocationWithMap = false;
			backToLocation.setVisibility(View.VISIBLE);
		}
		validatePointOfView();
	}    
    
}