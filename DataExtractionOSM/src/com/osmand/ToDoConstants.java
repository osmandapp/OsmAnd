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
	
	// TODO ANDROID
//	 0. Minimize memory used for index & improve time for reading index  
//	 1. POI search near to map location (show categories & type). First cut. (implement incremental search)
//	 3. Revise osmand UI. Preparing new icons.
//	 2. Showing compass on the map : use device compass if exists(?)
//	 5. Search for city/streets/buildings
//	 9. Config file log & see log from file (when exception happened to see from device)
//	 11. Print out additional info speed, altitude, number of satellites
//	 8. Enable change POI directly on map (requires OSM login)
//	 13. Save point as favourite & introduce favourite points dialog
//	 14. Show zoom level on map
//	 15. Investigate interruption of any long running operation & implement where it is needed
//   16. Support open street bugs api.
//   17. Enable go to location specifying coordinates
//   18. Implement go to point	
//   19. Show how map is rotated where north/south on map (do not consider compass)
//   20. Implement save track/route to gpx (?)	
	
	// FIXME Bugs Androd :
	// 0. FIX TODO for partial loading rotated map
	// 1. When firstly run osmand navigation (from notification bar) show map & go to menu shows desktop.
	//      No chance to close application 
	// 3. Fix progress information (loading indices) for android version
	// 4. Fix when POI selected & enable button backToLocation
	
	// TODO SWING: 
	// 1. download tiles without using dir tiles
	// 2. Config file log & see log from file
	// 3. Reinvent index mechanism (save in zip file with tile indexes, save city/town addresses separately, read partially !)
	// 4. Invent different file extensions for poi.index, address.index,...
	

	// Max letter : 
	// 1. Fix bug 1
	// 2. Create for each screen activity
	// 3. Implement incremental search (reduce first time display to 10 & depth 2)
	// 4. Improve navigate back/forward between screens
	// 5. Implement exit confirmation
	
	
	// DONE ANDROID :
//	 12. Show information of where are you going (the arrow on the map)
//	 10. Specify auto-rotating map (bearing of your direction)
	
	// DONE SWING
	

}
