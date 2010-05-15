package com.osmand.swing;

import java.awt.Rectangle;
import java.io.File;
import java.util.prefs.Preferences;

import com.osmand.osm.LatLon;

public class DataExtractionSettings {

	private static DataExtractionSettings settings = null;
	public static DataExtractionSettings getSettings(){
		if(settings == null){
			settings = new DataExtractionSettings();
		}
		return settings;
		
	}
	
	Preferences preferences = Preferences.userRoot();

	
	public File getTilesDirectory(){
		return new File(getDefaultWorkingDir(), "tiles");
	}

	public File getDefaultWorkingDir(){
		String workingDir = preferences.get("working_dir", System.getProperty("user.home"));
		if(workingDir.equals(System.getProperty("user.home"))){
			workingDir += "/osmand";
			new File(workingDir).mkdir();
		}
		return new File(workingDir);
	}
	
	public void saveDefaultWorkingDir(File path){
		preferences.put("working_dir", path.getAbsolutePath());
	}
	
	public LatLon getDefaultLocation(){
		double lat = preferences.getDouble("default_lat",  53.9);
		double lon = preferences.getDouble("default_lon",  27.56);
		return new LatLon(lat, lon);
	}
	
	public void saveDefaultLocation(double lat, double lon){
		preferences.putDouble("default_lat",  lat);
		preferences.putDouble("default_lon",  lon);
	}
	
	public int getDefaultZoom(){
		return preferences.getInt("default_zoom",  5);
	}
	
	public void saveDefaultZoom(int zoom){
		preferences.putInt("default_zoom",  zoom);
	}
	
	public Rectangle getWindowBounds(){
		Rectangle r = new Rectangle();
		r.x = preferences.getInt("window_x",  0);
		r.y = preferences.getInt("window_y",  0);
		r.width = preferences.getInt("window_width",  800);
		r.height = preferences.getInt("window_height",  600);
		return r;
	}
	
	public void saveWindowBounds(Rectangle r) {
		preferences.putInt("window_x", r.x);
		preferences.putInt("window_y", r.y);
		preferences.putInt("window_width", r.width);
		preferences.putInt("window_height", r.height);
	}
	
	public boolean useInternetToLoadImages(){
		// TODO save the property if needed
		return true;
	}
	
	
}
