package com.osmand;

import com.osmand.map.ITileSource;
import com.osmand.map.TileSourceManager;

/**
 * This is temp class where all path & machine specific properties are written  
 */
public abstract class DefaultLauncherConstants {
	
	// External files
	public static String pathToTestDataDir = "E:\\Information\\OSM maps\\";
	
	public static String pathToOsmFile =  pathToTestDataDir + "minsk.osm";
	public static String pathToOsmBz2File =  pathToTestDataDir + "belarus_2010_04_01.osm.bz2";
	public static String pathToDirWithTiles = pathToTestDataDir +"MinskTiles";
	
	public static String writeTestOsmFile = "C:\\1_tmp.osm"; // could be null - wo writing
	
	// Initial map settings
	public static double MAP_startMapLongitude = 27.56;
	public static double MAP_startMapLatitude = 53.9;
	public static int MAP_startMapZoom = 15;
	public static int MAP_divNonLoadedImage = 8;
	public static boolean loadMissingImages = true;
	public static ITileSource MAP_defaultTileSource = TileSourceManager.getMapnikSource();
	public static boolean showGPSCoordinates = true;
	
	
	// Application constants
	public static String APP_NAME = "OsmAnd";
	public static String APP_VERSION = "0.1";
	
	
	// Download manager tile settings
	public static int TILE_DOWNLOAD_THREADS = 4;
	public static int TILE_DOWNLOAD_SECONTS_TO_WORK = 25;
	public static final int TILE_DOWNLOAD_MAX_ERRORS = -1;	
	
	
	
	
}
