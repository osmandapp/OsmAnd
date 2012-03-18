package net.osmand.swing;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import net.osmand.data.preparation.IndexCreator;
import net.osmand.data.preparation.MapZooms;
import net.osmand.osm.LatLon;


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
	
	
	public File[] getDefaultRoutingFile(){
		String routingFile = preferences.get("routing_file", null);
		if(routingFile == null){
			return null;
		}
		String[] files = routingFile.split(",");
		List<File> fs = new ArrayList<File>();
		for(String f : files){
			if(new File(f.trim()).exists()){
				fs.add(new File(f.trim()));
			}
		}
		
		return fs.toArray(new File[fs.size()]);
	}
	
    public String getDefaultRoutingFilePath(){
    	File[] file = getDefaultRoutingFile();
		String path = "";
		if (file != null) {
			for (int i = 0; i < file.length; i++) {
				if (i > 0) {
					path += ", ";
				}
				path += file[i].getAbsolutePath();
			}
		}
		return path;
    }
	
	public void setDefaultRoutingPath(String path){
		preferences.put("routing_file", path);
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
	
	public MapZooms getMapZooms(){
		String value = preferences.get("map_zooms", MapZooms.MAP_ZOOMS_DEFAULT);
		return MapZooms.parseZooms(value);
	}
	
	public String getMapZoomsValue(){
		return preferences.get("map_zooms", MapZooms.MAP_ZOOMS_DEFAULT);
	}
	
	public void setMapZooms(String zooms){
		// check string
		MapZooms.parseZooms(zooms);
		preferences.put("map_zooms", zooms);
	}
	
	public String getLineSmoothness(){
		return preferences.get("line_smoothness", "2");
	}
	
	public void setLineSmoothness(String smooth){
		// check string
		Integer.parseInt(smooth);
		preferences.put("line_smoothness", smooth);
	}
	
	
	public String getMapRenderingTypesFile(){
		return preferences.get("rendering_types_file", "");
	}
	
	
	public void setMapRenderingTypesFile(String fileName){
		preferences.put("rendering_types_file", fileName);
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
	
	public String getCityAdminLevel(){
		return preferences.get("cityAdminLevel", "" + IndexCreator.DEFAULT_CITY_ADMIN_LEVEL);
	}
	
	public void setCityAdminLevel(String s){
		preferences.put("cityAdminLevel", s);
	}
	
	public String getOsrmServerAddress(){
		return preferences.get("osrmServerAddress", "http://127.0.0.1:5000");
	}
	
	public void setOsrmServerAddress(String s){
		preferences.put("osrmServerAddress", s);
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
