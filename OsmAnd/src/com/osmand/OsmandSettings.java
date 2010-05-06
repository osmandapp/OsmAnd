package com.osmand;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;

import com.osmand.map.ITileSource;
import com.osmand.map.TileSourceManager;
import com.osmand.map.TileSourceManager.TileSourceTemplate;

public class OsmandSettings {
	
	// These settings are stored in SharedPreferences
	public static final String SHARED_PREFERENCES_NAME = "com.osmand.settings";

	// this value string is synchronized with android.xml preference name	
	public static final String USE_INTERNET_TO_DOWNLOAD_TILES = "use_internet_to_download_tiles";
	public static boolean isUsingInternetToDownloadTiles(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(USE_INTERNET_TO_DOWNLOAD_TILES, true);
	}
	
	// this value string is synchronized with android.xml preference name
	public static final String SHOW_POI_OVER_MAP = "show_poi_over_map";
	public static boolean isShowingPoiOverMap(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(SHOW_POI_OVER_MAP, false);
	}
	
	// this value string is synchronized with android.xml preference name
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
		return DefaultLauncherConstants.MAP_defaultTileSource;
	}
	
	public static String getMapTileSourceName(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		String tileName = prefs.getString(MAP_TILE_SOURCES, null);
		if(tileName != null){
			return tileName;
		}
		return DefaultLauncherConstants.MAP_defaultTileSource.getName();
	}
	
	
	

}
