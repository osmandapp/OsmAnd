package com.osmand;
/**
 * This class is designed to put all to do's and link them with code.
 * The whole methods could be paste or just constants.
 * Do not worry to put ugly code here (just a little piece)
 */
public class ToDoConstants {
	
	
	/**
	 * Write activity to show something about authors / donation ....
	 */
	public int DESCRIBE_ABOUT_AUTHORS = 8;
	
	// TODO ANDROID
//  25. POI search near to map location (show categories & type). Second cut. (implement incremental search)

//	 3. Revise osmand UI. Preparing new icons (revise UI 18, 2, ).
//	 13. Save point as favorite & introduce favorite points dialog
//	 14. Show zoom level on map

//   24. Implement ResourceManager, load cities/streets/buildings on Low memory (clear previous all addresses cities).
//	  5. Search for city/streets/buildings
//	  9. Configure file log & see log from file (when exception happened to see from device)
//	 15. Investigate interruption of any long running operation & implement where it is needed
	
//   17. Enable go to location by specifying coordinates
//	 11. Print out additional info speed, altitude, number of satellites
//   19. Show how map is rotated where north/south on map (do not consider compass)
//   23. Implement moving point from center to bottom (for rotating map). (+)
	
//   21. Implement zooming tile (if tile doesn't exist local, we can zoom in previous tile).


//	  8. Enable change POI directly on map (requires OSM login)
//   16. Support open street bugs api.
//   20. Implement save track/route to gpx (?)
	
	// FIXME Bugs Android :
	// 0. FIX TODO for partial loading rotated map
	// 1. When firstly run osmand navigation (from notification bar) show map & go to menu shows desktop.
	//      No chance to close application 
	// 3. Fix progress information (loading indices) for android version
	// 4. Fix when POI selected & enable button backToLocation
	// 5. Call ResourceManager.close when it is needed
	// 6. Understand concept of application where to save/restore global setting 
	//     (for example reset navigate to point, reset link map with location). It should be reset after user call exit.
	// 7. Implement search amenities by type (!). 
	//     Rewrite search activity in order to limit amenities not to all types.
	
	// TODO SWING: 
	// 1. Download tiles without using dir tiles
	// 2. Configure file log & see log from file
	// 5. Implement supress warning for duplicate id
	
	
	// DONE ANDROID :
//  18. Implement go to point
//   2. Showing compass on the map : use device compass if exists(?)
	
	// DONE SWING

}
