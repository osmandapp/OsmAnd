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

//   42. Revise UI (icons/layouts). Support different devices. Add inactive/focus(!) icon versions.
//		 Some icons are not fine (as back menu from map - it is blured).
	
//   32. Introduce POI predefined filters (car filter(other-fuel, transportation-car_wash, show-car) and others)
// 		 DONE : back end (POI filter object, save, delete, read) 
//	     TODO : activity to create/edit new index, activity to read both user defined/osm standard, add actions to remove/create

//   33. Build transport locations. Create transport index (transport-stops) (investigate)
//       GOT : Victor	
//		 DONE: Load transport routes in swing. 
//	     TODO: Create transport index, create transport activity

	
	
	// FUTURE RELEASES
	//   49. Calculate route from specified point (not from your location only)
	//   48. Enable change favorite point :  (for example fav - "car") means last point you left car. It is not static point, 
	//		 you can always use the same name for different locations.
	//   50. Invent opening hours editor in order to edit POI hours better on device
	//   48. Implement console application that prepare indexes to upload on server... 
	// 		 0) run in background 1) download from internet 2) generates indices for Europe (take care about memory) 3) upload?
    //   43. Enable poi filter by name
	//   45. Get clear <Use internet> settings. Move that setting on top settings screen. 
	//       That setting should rule all activities that use internet. It should ask whenever internet is used 
	//		(would you like to use internet for that operation - if using internet is not checked). 
	//		Internet using now for : edit POI osm, show osm bugs layer, download tiles.
	//   47. Internet connectivity could be checked before trying to use
	//   46. Implement downloading strategy for tiles (do not load 17 zoom, load only 16 for example) - try to scale 15 zoom for 17 (?)
    //   40. Support simple vector road rendering (require new index file) (?)
    //   26. Show the whole street on map (when it is chosen in search activity). Possibly extend that story to show layer with streets. (?)

	
	// BUGS Android
	//  5. Improvement : Implement caching files existing on FS, implement specific method in RM
	//     Introducing cache of file names that are on disk (creating new File() consumes a lot of memory)
	
	
	// TODO swing
	// 9. Fix issues with big files (such as netherlands) - save memory (!) - very slow due to transport index !
	// Current result : for big file (1 - task  60-80% time, 90% memory)
	// 1. Download tiles without using dir tiles (?)
 	// 10. Improve address indexing (use relations). 
    //	  use relation "a6" (to accumulate streets!),  "a3" to read all cities & define boundaries for city (& define that street in city). 
	
	// BUGS Swing
	
	// DONE ANDROID :
    //  37. Get rid of exit button (!). Think about when notification should go & how clear resources if it is necessary
    //      DONE : add to app settings preference (Refresh indexes).
	//  31. Translation.
	//  34. Suppport navigation for calculated route (example of get route from internet is in swing app).
	//    DONE : MiniMap done, Routing settings done, RouteLayer done, RoutingHelper done.
	//  44. Show gps status (possibly open existing gps-compass app (free) or suggest to install it - no sense to write own activity)

	
	// DONE SWING

}
