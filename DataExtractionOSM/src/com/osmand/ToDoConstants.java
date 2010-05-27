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
//	 1. POI search near to map location (show categories & type). First cut. (implement incremental search)
//	 3. Revise osmand UI. Preparing new icons.
//	 2. Showing compass on the map : use device compass if exists(?)
//	 5. Search for city/streets/buildings
//	 9. Configure file log & see log from file (when exception happened to see from device)
//	 11. Print out additional info speed, altitude, number of satellites
//	 8. Enable change POI directly on map (requires OSM login)
//	 13. Save point as favorite & introduce favorite points dialog
//	 14. Show zoom level on map
//	 15. Investigate interruption of any long running operation & implement where it is needed
//   16. Support open street bugs api.
//   17. Enable go to location by specifying coordinates
//   18. Implement go to point	
//   19. Show how map is rotated where north/south on map (do not consider compass)
//   20. Implement save track/route to gpx (?)
//   21. Implement zooming tile (if tile doesn't exist local, we can zoom in previous tile).
//   23. Implement moving point from center to bottom (for rotating map). 
//       It is not very useful to see what was before.
	
//   24. Implement ResourceManager on Low memory (clear previous all addresses cities, remove all amenities cache)
//       Use async loading tile thread, to preload amenities also.
	
	// FIXME Bugs Android :
	// 0. FIX TODO for partial loading rotated map
	// 1. When firstly run osmand navigation (from notification bar) show map & go to menu shows desktop.
	//      No chance to close application 
	// 3. Fix progress information (loading indices) for android version
	// 4. Fix when POI selected & enable button backToLocation
	
	// TODO SWING: 
	// 1. Download tiles without using dir tiles
	// 2. Configure file log & see log from file
	// 5. Implement supress warning for duplicate id
	
	
	// DONE ANDROID :
//	 12. Show information of where are you going (the arrow on the map)
//	 10. Specify auto-rotating map (bearing of your direction)
//   22. Investigate 3D tile view (how it is done in osmand). Looking not very good, because of
//        angle of perspective (best perspective angle = 60) use 
//  	android.graphics.Camera.rotateX(60), getMatrix(m), canvas.concat(m) (find example in internet)
//      Problems : to calculate how to drag point on map, to calculate how many tiles are needed, is location visible .... 
//	 0. Minimize memory used for index & improve time for reading index  
	
	// DONE SWING
	// 3. Reinvent index mechanism (save in zip file with tile indexes, save city/town addresses separately, read partially !)
	// 4. Invent different file extensions for poi.index, address.index,...	

}
