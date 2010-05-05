package com.osmand;


/**
 * This class is designed to put all to do's and link them with code.
 * The whole methods could be paste or just constants.
 * Do not worry to put ugly code here (just a little piece)
 *
 */
public class ToDoConstants {
	
	
	// use unknown implementation (not written)? How to see debug msgs?
	// Explanation of how it works 
	// The task 
	public int CONFIG_COMMONS_LOGGING_IN_ANDROID = 1;
	
	public int SAVE_SETTINGS_IN_ANDROID_BETWEEN_SESSION = 2;
	
	public int IMPLEMENT_ON_STOP_RESUME_ACTIVITY = 3;
	
	// OsmandMapTileView.java have problem with class loading (LogFactory, MapTileDownloader) - 
	// it is not editable in editor
	public int MAKE_MAP_PANEL_EDITABLE_IN_EDITOR = 4;
	
	// common parts : work with cache on file system & in memory
	public int EXTRACT_COMMON_PARTS_FROM_MAPPANEL_AND_OSMMAPVIEW = 5;

	
	public int REVISE_MAP_ACTIVITY_HOLD_ALL_ZOOM_LATLON_IN_ONEPLACE = 6;
	
	/**
	 * Resource should cache all resources & free them
	 * if there is no enough memory @see tile cache in tile view
	 * @see poi index in map activity
	 */
	public int INTRODUCE_RESOURCE_MANAGER = 7;
	

}
