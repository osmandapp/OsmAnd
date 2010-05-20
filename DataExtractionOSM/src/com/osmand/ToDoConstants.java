package com.osmand;


/**
 * This class is designed to put all to do's and link them with code.
 * The whole methods could be paste or just constants.
 * Do not worry to put ugly code here (just a little piece)
 *
 */
public class ToDoConstants {
	
	
	// OsmandMapTileView.java have problem with class loading (LogFactory, MapTileDownloader) - 
	// it is not editable in editor ?
	public int MAKE_MAP_PANEL_EDITABLE_IN_EDITOR = 4;
	
	/**
	 * Write activity to show something about authors / donation ....
	 */
	public int DESCRIBE_ABOUT_AUTHORS = 8;
	
	// TODO see all calculations x, y for layers & for MapView

	// 0. Minimize memory used for index & improve time for read index  
	//// TODO for releasing version
	// 1. POI SEARCH NEAR TO YOU
	// 2. FIX BACK TO your location & gps & point of view (may be compass)
	// 3. Revise UI icons/layout
	// 5. Enable city/streets/buildings index
	// 7. Search for city/streets/buildings!
	// 8. Enable change POI directly on map
	// 9. Log to see when exception occurred (android)
	// 10. Specify auto-rotating map (compass).
	// 11. Print out additional info speed, altitude, number of satellites
	// 12. Show point where are you going (the arrow not the point)
	// 13. Save point as favorite
	// 14. Show zoom level directly on map
	// 15. Investigate interruption of progress (is it available & how to support it) 
	// -------------------
	
	// BUGS Androd :
	// 1. When firstly run osmand navigation (from notification bar) show map & go to menu shows desktop.
	//      No chance to close application 
	
	
	/// SWING version : 
	// TODO : 
	// 1. Predefine before file loading what should be extracted from osm (building, poi or roads) !
	// 2. Fix TODO in files (accept amenity - way)
	
	// 3. download tiles without using dir tiles
	// 4. Config file log & see log from file
	// 5. Reinvent index mechanism (save in zip file with tile indexes, save city/town addresses separately, read partially !)
	// 6. Invent different file extensions for poi.index, address.index,...

}
