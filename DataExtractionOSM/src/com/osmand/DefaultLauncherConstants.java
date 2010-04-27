package com.osmand;

/**
 * This is temp class where all path & machine specific properties are written  
 */
public abstract class DefaultLauncherConstants {
	
	public static String pathToTestDataDir = "E:\\Information\\OSM maps\\";
	
	public static String pathToOsmFile =  pathToTestDataDir + "minsk.osm";
	public static String pathToOsmBz2File =  pathToTestDataDir + "belarus_2010_04_01.osm.bz2";
	
	public static String pathToDirWithTiles = pathToTestDataDir +"MinskTiles";
	
	public static String writeTestOsmFile = "C:\\1_tmp.osm"; // could be null - wo writing
}
