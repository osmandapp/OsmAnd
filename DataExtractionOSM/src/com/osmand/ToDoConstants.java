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

	// TODO team : 
	// 1. write letters (UI/strings)
	
	// TODO ANDROID
//   25. POI search near to map location (show categories & type). Second cut. (implement incremental search)
//	 13. Save point as favorite & introduce favorite points dialog
	
//	  3. Revise osmand UI. Preparing new icons (revise UI 18, 2, ). Main application icon, back to location icon.
//	 14. Show zoom level on map

//   24. Implement ResourceManager, load cities/streets/buildings on Low memory (clear previous all addresses cities).

//   25. Show opened/closed amenities.
//   27. Search intersection of streets.	
//   26. Show the whole street on map (when it is chosen in search activity). Possibly extend that story to show layer with streets.
	
//	  8. Enable change POI directly on map (requires OSM login)
//   16. Support open street bugs api.
//   20. Implement save track/route to gpx (?)

	
	// FIXME Bugs Android :
	// 1. When firstly run osmand navigation (from notification bar) show map & go to menu shows desktop.
	//      No chance to close application 
	// 6. Understand concept of application where to save/restore global setting. 
    //    (for example reset navigate to point, reset link map with location). It should be reset after user call exit.
	//     Call ResourceManager.close when it is needed.
	// 8. Introduce activity search by location (unify with existing dialog)
	// 9. When all features will be ready we can remove show location from context menu
	// 10. Notification is gone after clear all notifications
	
	// Performance improvements Android :
	// 1. Introducing one place where refreshMap will be called using postMessage mechanism (delay more than > 50 ? ms)
	// 2. Introducing cache of file names that are on disk (creating new File() consumes a lot of memory)
	
	// TODO SWING: 
	// 2. Configure file log & see log from file (add uncaught exception handling)
	// 5. Implement suppress warning for duplicate id
	// 6. Implement renaming/deleting street/building/city
	// 8. Implement basic transliteration version
	// 7. Implement saving bundle of tiles in different folder
	// 9. Using Collator in all TreeSet/TreeMap/Comparators
	// 1. Download tiles without using dir tiles
	
	
	// DONE ANDROID :
//	  5. Search for city/streets/buildings
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
