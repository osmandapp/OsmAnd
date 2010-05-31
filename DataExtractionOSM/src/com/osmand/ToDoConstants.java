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
//   25. POI search near to map location (show categories & type). Second cut. (implement incremental search)

//	  3. Revise osmand UI. Preparing new icons (revise UI 18, 2, ). Main application icon, back to location icon.
//	 13. Save point as favorite & introduce favorite points dialog
//	 14. Show zoom level on map

//   24. Implement ResourceManager, load cities/streets/buildings on Low memory (clear previous all addresses cities).
//	  5. Search for city/streets/buildings
 
//	  8. Enable change POI directly on map (requires OSM login)
//   16. Support open street bugs api.
//   20. Implement save track/route to gpx (?)
	
	// TODO search story : 
	// 1) Implement loading villages when user types more than 2 symbols
	// 2) Find intersection of streets
	// 3) Shows progress dialog (?)
	// 4) Implement finding buildings
	// 5) Show on map
	// 6) Show street on map
	// 7) Show distance to the village (to distinguish)
	
	// FIXME Bugs Android :
	// 1. When firstly run osmand navigation (from notification bar) show map & go to menu shows desktop.
	//      No chance to close application 
	// 6. Understand concept of application where to save/restore global setting. 
    //    (for example reset navigate to point, reset link map with location). It should be reset after user call exit.
	//     Call ResourceManager.close when it is needed.
	// 8. Introduce activity search by location (unify with existing dialog)
	// 9. When all features will be ready we can remove show location from context menu
	
	// Performance improvements Android :
	// 1. Introducing one place where refreshMap will be called using postMessage mechanism (delay more than > 50 ? ms)
	// 2. Introducing cache of file names that are on disk (creating new File() consumes a lot of memory)
	
	// TODO SWING: 
	// 2. Configure file log & see log from file (add uncaught exception handling)
	// 5. Implement suppress warning for duplicate id
	// 6. Implement renaming/deleting street/building/city
	// 8. Implement basic transliteration version
	// 7. Implement saving bundle of tiles in different folder
	// 1. Download tiles without using dir tiles
	
	
	// DONE ANDROID :
//	 15. Investigate interruption of any long running operation & implement where it is needed.
	//   ProgressDialogImplementation should support setOnCancelListener or obtain CANCEL message & 
	//   throw InterruptedException in all methods (remaining, progress, startTask, ...) when call it.
	//   Otherwise thread could be stopped however it is not good method.
//  21. Implement zooming tile (if tile doesn't exist local, we can zoom in previous tile).
//	11. Print out additional info speed, altitude, number of satellites
//  19. Show how map is rotated where north/south on map (do not consider compass)
//  23. Implement moving point from center to bottom (for rotating map)
//  17. Enable go to location by specifying coordinates
//	 9. Configure file log & see log from file (when exception happened to see from device)
//   2. Showing compass on the map : use device compass if exists(?)
//  18. Implement go to point

	// DONE SWING

}
