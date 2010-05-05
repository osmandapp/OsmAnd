package com.osmand;


/**
 * This class is designed to put all to do's and link them with code.
 * The whole methods could be paste or just constants.
 * Do not worry to put ugly code here (just a little piece)
 *
 */
public class ToDoConstants {
	
	public int SAVE_SETTINGS_IN_ANDROID_BETWEEN_SESSION = 2;
	
	// First of all switch off gps listener should be implemented
	public int IMPLEMENT_ON_STOP_RESUME_ACTIVITY = 3;
	
	// OsmandMapTileView.java have problem with class loading (LogFactory, MapTileDownloader) - 
	// it is not editable in editor ?
	public int MAKE_MAP_PANEL_EDITABLE_IN_EDITOR = 4;
	
	// common parts : work with cache on file system & in memory
	public int EXTRACT_COMMON_PARTS_FROM_MAPPANEL_AND_OSMMAPVIEW = 5;

	
	public int REVISE_MAP_ACTIVITY_HOLD_ALL_ZOOM_LATLON_IN_ONEPLACE = 6;
	
	
	/**
	 * Write activity to show something about authors / donation ....
	 */
	public int DESCRIBE_ABOUT_AUTHORS = 8;
	

}
