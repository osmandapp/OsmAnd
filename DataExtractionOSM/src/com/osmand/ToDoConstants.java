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

	
	// TODO ANDROID in release 0.1
//   29. Show opened/closed amenities (in search poi).
//	 13. Save point as favorite & introduce favorite points dialog
//	  3. Revise osmand UI. Preparing new icons (revise UI 18, 2, ). Main application icon, back to location icon.
//	 14. Show zoom level on map

	 // NOT in release 0.1
//	  8. Enable change POI directly on map (requires OSM login)
//   16. Support open street bugs api.
//   20. Implement save track/route to gpx (?)
//   26. Show the whole street on map (when it is chosen in search activity). Possibly extend that story to show layer with streets.
//   30. Performance issue : introduce one place where refreshMap will be called using postMessage mechanism (delay more than > 50 ? ms).	

	
	// FIXME Bugs Android :
	// 6. Understand concept of application where to save/restore global setting. 
    //    (for example reset navigate to point, reset link map with location). It should be reset after user call exit.
	//     Call ResourceManager.close when it is needed (+)
	// 10. Notification is gone after clear all notifications
	// Performance improvements Android :
	// 2. Introducing cache of file names that are on disk (creating new File() consumes a lot of memory) (+)

	
	// TODO swing NOT in release 0.1
	// 1. Download tiles without using dir tiles
	
	
	// DONE ANDROID :

	// DONE SWING
}
