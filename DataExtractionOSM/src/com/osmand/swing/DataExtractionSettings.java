package com.osmand.swing;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
	
	public boolean getLoadEntityInfo(){
		return preferences.getBoolean("load_entity_info",  true);
	}
	
	public void setLoadEntityInfo(boolean loadEntityInfo){
		preferences.putBoolean("load_entity_info",  loadEntityInfo);
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
		return preferences.getBoolean("use_internet", true);
	}
	
	public void setUseInterentToLoadImages(boolean b){
		preferences.putBoolean("use_internet", b);
	}
	
	public boolean isSupressWarningsForDuplicatedId(){
		return preferences.getBoolean("supress_duplicated_id", true);
	}
	
	public void setSupressWarningsForDuplicatedId(boolean b){
		preferences.putBoolean("supress_duplicated_id", b);
	}
	
	
	String[] SUFFIXES = new String[] {"av.", "avenue", "просп.", "пер.", "пр.","заул.", "проспект", "переул.", "бул.", "бульвар", "тракт"};
	String[] DEFAUTL_SUFFIXES = new String[] {"str.", "street", "улица", "ул."};
	public String[] getSuffixesToNormalizeStreets(){
		String s = preferences.get("suffixes_normalize_streets", null);
		if(s == null){
			return SUFFIXES;
		}
		List<String> l = new ArrayList<String>();
		int i = 0;
		int nextI = 0;
		while((nextI=s.indexOf(',',i)) >= 0){
			String t = s.substring(i, nextI).trim();
			if(t.length() > 0){
				l.add(t);
			}
			i = nextI + 1;
		}
		return l.toArray(new String[l.size()]);
	}
	
	public String[] getDefaultSuffixesToNormalizeStreets(){
		String s = preferences.get("default_suffixes_normalize_streets", null);
		if(s == null){
			return DEFAUTL_SUFFIXES;
		}
		List<String> l = new ArrayList<String>();
		int i = 0;
		int nextI = 0;
		while((nextI=s.indexOf(',',i)) >= 0){
			String t = s.substring(i, nextI).trim();
			if(t.length() > 0){
				l.add(t);
			}
			i = nextI + 1;
		}
		return l.toArray(new String[l.size()]);
	}
	
	public String getSuffixesToNormalizeStreetsString(){
		String s = preferences.get("suffixes_normalize_streets", null);
		if(s == null){
			StringBuilder b = new StringBuilder();
			for(String st : SUFFIXES){
				b.append(st).append(", ");
			}
			return b.toString();
		}
		return s;
	}
	
	public String getDefaultSuffixesToNormalizeStreetsString(){
		String s = preferences.get("default_suffixes_normalize_streets", null);
		if(s == null){
			StringBuilder b = new StringBuilder();
			for(String st : DEFAUTL_SUFFIXES){
				b.append(st).append(", ");
			}
			return b.toString();
		}
		return s;
	}
	
	public void setDefaultSuffixesToNormalizeStreets(String s){
		preferences.put("default_suffixes_normalize_streets", s);
	}
	
	public void setSuffixesToNormalizeStreets(String s){
		preferences.put("suffixes_normalize_streets", s);
	}
	
}
