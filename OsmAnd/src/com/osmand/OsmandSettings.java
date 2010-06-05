package com.osmand;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.osmand.map.ITileSource;
import com.osmand.map.TileSourceManager;
import com.osmand.map.TileSourceManager.TileSourceTemplate;
import com.osmand.osm.LatLon;

public class OsmandSettings {
	
	// These settings are stored in SharedPreferences
	public static final String SHARED_PREFERENCES_NAME = "com.osmand.settings";
	
	public static final int CENTER_CONSTANT = 0;
	public static final int BOTTOM_CONSTANT = 1;

	// this value string is synchronized with settings_pref.xml preference name	
	public static final String USE_INTERNET_TO_DOWNLOAD_TILES = "use_internet_to_download_tiles";
	public static boolean isUsingInternetToDownloadTiles(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(USE_INTERNET_TO_DOWNLOAD_TILES, true);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_POI_OVER_MAP = "show_poi_over_map";
	public static boolean isShowingPoiOverMap(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(SHOW_POI_OVER_MAP, false);
	}
	
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_OSM_BUGS = "show_osm_bugs";
	public static boolean isShowingOsmBugs(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(SHOW_OSM_BUGS, false);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_VIEW_ANGLE = "show_view_angle";
	public static boolean isShowingViewAngle(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(SHOW_VIEW_ANGLE, true);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String ROTATE_MAP_TO_BEARING = "rotate_map_to_bearing";
	public static boolean isRotateMapToBearing(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(ROTATE_MAP_TO_BEARING, false);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String POSITION_ON_MAP = "position_on_map";
	public static int getPositionOnMap(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getInt(POSITION_ON_MAP, CENTER_CONSTANT);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String MAP_VIEW_3D = "map_view_3d";
	public static boolean isMapView3D(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(MAP_VIEW_3D, false);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String USE_ENGLISH_NAMES = "use_english_names";
	public static boolean usingEnglishNames(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(USE_ENGLISH_NAMES, false);
	}
	
	public static boolean setUseEnglishNames(Context ctx, boolean useEnglishNames){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(USE_ENGLISH_NAMES, useEnglishNames).commit();
	}
	
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String MAP_TILE_SOURCES = "map_tile_sources";
	public static ITileSource getMapTileSource(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		String tileName = prefs.getString(MAP_TILE_SOURCES, null);
		if(tileName != null){
			List<TileSourceTemplate> list = TileSourceManager.getKnownSourceTemplates();
			for(TileSourceTemplate l : list){
				if(l.getName().equals(tileName)){
					return l;
				}
			}
		}
		return TileSourceManager.getMapnikSource();
	}
	
	public static String getMapTileSourceName(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		String tileName = prefs.getString(MAP_TILE_SOURCES, null);
		if(tileName != null){
			return tileName;
		}
		return TileSourceManager.getMapnikSource().getName();
	}

	// This value is a key for saving last known location shown on the map
	public static final String LAST_KNOWN_MAP_LAT = "last_known_map_lat";
	public static final String LAST_KNOWN_MAP_LON = "last_known_map_lon";
	public static final String LAST_KNOWN_MAP_ZOOM = "last_known_map_zoom";
	public static LatLon getLastKnownMapLocation(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		float lat = prefs.getFloat(LAST_KNOWN_MAP_LAT, 0);
		float lon = prefs.getFloat(LAST_KNOWN_MAP_LON, 0);
		return new LatLon(lat, lon);
	}
	
	public static void setLastKnownMapLocation(Context ctx, double latitude, double longitude){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit();
		edit.putFloat(LAST_KNOWN_MAP_LAT, (float) latitude);
		edit.putFloat(LAST_KNOWN_MAP_LON, (float) longitude);
		edit.commit();
	}
	
	public static int getLastKnownMapZoom(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getInt(LAST_KNOWN_MAP_ZOOM, 3);
	}
	
	public static void setLastKnownMapZoom(Context ctx, int zoom){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit();
		edit.putInt(LAST_KNOWN_MAP_ZOOM, zoom);
		edit.commit();
	}
	
	public static final String LAST_SEARCHED_REGION = "last_searched_region";
	public static final String LAST_SEARCHED_CITY = "last_searched_city";
	public static final String LAST_SEARCHED_STREET = "last_searched_street";
	public static final String LAST_SEARCHED_BUILDING = "last_searched_building";
	public static final String LAST_SEARCHED_INTERSECTED_STREET = "last_searched_intersected_street";
	
	public static String getLastSearchedRegion(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(LAST_SEARCHED_REGION, "");
	}
	
	public static boolean setLastSearchedRegion(Context ctx, String region) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit().putString(LAST_SEARCHED_REGION, region).putLong(LAST_SEARCHED_CITY, -1).
							putString(LAST_SEARCHED_STREET,"").putString(LAST_SEARCHED_BUILDING, "");
		if (prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, "");
		}
		return edit.commit();
	}

	public static Long getLastSearchedCity(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getLong(LAST_SEARCHED_CITY, -1);
	}
	
	public static boolean setLastSearchedCity(Context ctx, Long cityId){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit().putLong(LAST_SEARCHED_CITY, cityId).putString(LAST_SEARCHED_STREET, "").
							putString(LAST_SEARCHED_BUILDING, "");
		if(prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)){
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, "");
		}
		return edit.commit();
	}

	public static String getLastSearchedStreet(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(LAST_SEARCHED_STREET, "");
	}
	
	public static boolean setLastSearchedStreet(Context ctx, String street){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit().putString(LAST_SEARCHED_STREET, street).putString(LAST_SEARCHED_BUILDING, "");
		if(prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)){
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, "");
		}
		return edit.commit();
	}
	
	public static String getLastSearchedBuilding(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(LAST_SEARCHED_BUILDING, "");
	}
	
	public static boolean setLastSearchedBuilding(Context ctx, String building){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(LAST_SEARCHED_BUILDING, building).remove(LAST_SEARCHED_INTERSECTED_STREET).commit();
	}
	
	
	public static String getLastSearchedIntersectedStreet(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		if(!prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)){
			return null;
		}
		return prefs.getString(LAST_SEARCHED_INTERSECTED_STREET, "");
	}
	
	public static boolean setLastSearchedIntersectedStreet(Context ctx, String street){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(LAST_SEARCHED_INTERSECTED_STREET, street).commit();
	}
	
	public static boolean removeLastSearchedIntersectedStreet(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().remove(LAST_SEARCHED_INTERSECTED_STREET).commit();
	}

	
}
