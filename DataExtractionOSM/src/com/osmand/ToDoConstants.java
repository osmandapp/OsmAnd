package com.osmand;


/**
 * This class is designed to put all to do's and link them with code.
 * The whole methods could be paste or just constants.
 * Do not worry to put ugly code here (just a little piece)
 */
public class ToDoConstants {
	// FIXES for 0.1 versions : 
	// 1. Map tile downloader : 
//	while(!threadPoolExecutor.getQueue().isEmpty()){
//		try {
//			threadPoolExecutor.getQueue().take();
//		} catch (InterruptedException e) {
//		}
//	}
	
	
	
	/**
	 * Write activity to show something about authors / donation ....
	 */
	public int DESCRIBE_ABOUT_AUTHORS = 8;
	
	 // TODO ANDROID 
//	  8. Enable change POI directly on map (requires OSM login)
//   20. Implement save track/route to gpx (?)
//   26. Show the whole street on map (when it is chosen in search activity). Possibly extend that story to show layer with streets.
//   30. Performance issue with map drawing : 
//       introduce one place where refreshMap will be called using postMessage mechanism (delay more than > 50 ? ms).
//       Introducing cache of file names that are on disk (creating new File() consumes a lot of memory) 	
//   31. Translation.	
//   32. Introduce POI predefined filters (car filter(other-fuel, transportation-car_wash, show-car) and others)
//   33. Build transport locations (investigate)
//   34. Investigate routing (bicycle, car)
//   35. Enable trackball navigation in android
//   36. Postcode search
	
	
	// BUGS Android
	//  1. Fix bug with navigation layout (less zoom controls) (fixed).
    //  3. Implement clear existing area with tiles (update map)
	//  2. Include to amenity index : historic, sport, ....
	//  4. Fix layout problems with add comment
	
	
	// TODO swing
	// 2. Internal (Simplify MapPanel - introduce layers for it)ю
	// 3. Implement clear progress.
	// 1. Download tiles without using dir tiles



	
	
	// DONE ANDROID :
//  16. Support open street bugs api (supports viewing, deleting).
//  13. Save point as favorite & introduce favorite points dialog
//  29. Show opened/closed amenities (in search poi).
//   3. Revise osmand UI. Preparing new icons (revise UI 18, 2, ). Main application icon, back to location icon.
//	14. Show zoom level on map
	
	// DONE SWING
}
