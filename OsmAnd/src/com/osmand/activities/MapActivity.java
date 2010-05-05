package com.osmand.activities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.SAXException;

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

import com.osmand.Algoritms;
import com.osmand.IMapLocationListener;
import com.osmand.LogUtil;
import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.data.DataTileManager;
import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings.OSMTagKey;
import com.osmand.osm.io.OsmBaseStorage;
import com.osmand.views.OsmandMapTileView;
import com.osmand.views.POIMapLayer;
import com.osmand.views.PointOfView;

public class MapActivity extends Activity implements LocationListener, IMapLocationListener {
    /** Called when the activity is first created. */
	private OsmandMapTileView mapView;
	
	private boolean linkLocationWithMap = true; 
	
	private Location lastKnownLocation = null;

	private ImageButton backToLocation;

	private PointOfView pointOfView;
	
	private static final String TILES_PATH = "osmand/tiles/";
	private static final String POI_PATH = "osmand/poi/";
	private static final org.apache.commons.logging.Log log = LogUtil.getLog(MapActivity.class);

	private DataTileManager<Node> indexPOI;

	private POIMapLayer poiMapLayer;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
//	     getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
//	                                WindowManager.LayoutParams.FLAG_FULLSCREEN); 
		
		
		setContentView(R.layout.main);
		mapView = (OsmandMapTileView) findViewById(R.id.MapView);
		mapView.setFileWithTiles(new File(Environment.getExternalStorageDirectory(), TILES_PATH));
		mapView.addMapLocationListener(this);
		
		ZoomControls zoomControls = (ZoomControls) findViewById(R.id.ZoomControls01);
		zoomControls.setOnZoomInClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapView.setZoom(mapView.getZoom() + 1);
				poiMapLayer.setCurrentLocationAndZoom(poiMapLayer.getCurrentLocation(), mapView.getZoom());
			}
		});
		zoomControls.setOnZoomOutClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mapView.setZoom(mapView.getZoom() - 1);
				poiMapLayer.setCurrentLocationAndZoom(poiMapLayer.getCurrentLocation(), mapView.getZoom());
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
		indexPOI = indexPOI();
		
		poiMapLayer = (POIMapLayer)findViewById(R.id.PoiMapLayer);
		poiMapLayer.setNodeManager(indexPOI);
		
	}
    
    private static final boolean indexPOIFlag = false;
    
    public DataTileManager<Node> indexPOI(){
    	File file = new File(Environment.getExternalStorageDirectory(), POI_PATH);
    	
    	DataTileManager<Node> r = new DataTileManager<Node>();
    	if(file.exists() && file.canRead() && indexPOIFlag){
    		for(File f : file.listFiles() ){
    			if(f.getName().endsWith(".bz2") || f.getName().endsWith(".osm") ){
    				if(log.isDebugEnabled()){
    					log.debug("Starting index POI " + f.getAbsolutePath());
    				}
    				boolean zipped = f.getName().endsWith(".bz2");
    				InputStream stream = null;
    				try {
    					OsmBaseStorage storage = new OsmBaseStorage();
    					stream = new FileInputStream(f);
    					stream = new BufferedInputStream(stream);
    					if (zipped) {
							if (stream.read() != 'B' || stream.read() != 'Z') {
								log.error("Can't read poi file " + f.getAbsolutePath()
										+ "The source stream must start with the characters BZ if it is to be read as a BZip2 stream.");
								continue;
							} else {
								stream = new CBZip2InputStream(stream);
							}
						}
    					storage.parseOSM(stream);
    					for(Entity e : storage.getRegisteredEntities().values()){
    						if(e instanceof Node && e.getTag(OSMTagKey.AMENITY) != null){
    							Node n = (Node) e;
    							r.registerObject(n.getLatitude(), n.getLongitude(), n);
    						}
    					}
    					if(log.isDebugEnabled()){
        					log.debug("Finishing index POI " + f.getAbsolutePath());
        				}
    				} catch(IOException e){
    					log.error("Can't read poi file " + f.getAbsolutePath(), e);
    				} catch (SAXException e) {
    					log.error("Can't read poi file " + f.getAbsolutePath(), e);
					} finally {
						Algoritms.closeStream(stream);
					}
    			}
    		}
    	}
    	return r; 
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
		if((poiMapLayer.getVisibility() == View.VISIBLE) != OsmandSettings.showPoiOverMap){
			if(OsmandSettings.showPoiOverMap){
				poiMapLayer.setVisibility(View.VISIBLE);
			} else {
				poiMapLayer.setVisibility(View.INVISIBLE);
			}
		}
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
	}


	@Override
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		// when user start dragging 
		if(source == mapView && lastKnownLocation != null){
			linkLocationWithMap = false;
			backToLocation.setVisibility(View.VISIBLE);
		}
		poiMapLayer.setCurrentLocationAndZoom(new LatLon(newLatitude, newLongitude), mapView.getZoom());
		
		validatePointOfView();
	}    
    
}