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
//   31. Translation.
//	     GOT : olga
	
//   42. Revise UI (icons/layouts). Support different devices. Add inactive/focus(!) icon versions.
	
//   32. Introduce POI predefined filters (car filter(other-fuel, transportation-car_wash, show-car) and others)
// 		 DONE : back end (POI filter object, save, delete, read) 
//	     TODO : activity to create/edit new index, activity to read both user defined/osm standard, add actions to remove/create

//	  8. Enable change POI directly on map (requires OSM login)
//	     GOT : Victor
//	     DONE : Victor investigation
//		 TODO:
	
//   33. Build transport locations. Create transport index (transport-stops) (investigate)
//       GOT : Victor	
//		 DONE: Load transport routes in swing. 
//	     TODO: Create transport index, create transport activity
	
//   44. Introduce settings presets (car/bicycle/pedestrian/default) - show different icons for car (bigger), possibly change fonts, position
//	     DONE : Introduce settings property in settings screen
//       TODO : check if all is correct, change visible icon over map for car
	
//   45. Autozoom feature (for car navigation) 
//	     DONE : settings preferences done
//	     TODO : add to Map activity algorithm of auto zooming in setLocation method 

//   36. Postcode search
	
//   37. Get rid of exit button (!). Think about when notification should go & how clear resources if it is necessary
//       DONE : 	
//	     TODO : add to app settings preference (Refresh indexes).
	
//   34. Suppport navigation for calculated route (example of get route from internet is in swing app).
//	     IDEA : Victor have ideas
	
	
	
	// FUTURE RELEASES 
//   43. Enable poi filter by name (?)
//   40. Support simple vector road rendering (require new index file) (?)
//   26. Show the whole street on map (when it is chosen in search activity). Possibly extend that story to show layer with streets. (?)
	
	// BUGS Android
	//  5. Improvement : Implement caching files existing on FS, implement specific method in RM
	//     Introducing cache of file names that are on disk (creating new File() consumes a lot of memory)
	
 
	
	// TODO swing
	// 4. Fix issues with big files (such as netherlands) - save memory (!)
	// Current result : for big file (1 - task  60-80% time, 90% memory)
	// 1. Download tiles without using dir tiles (?)
	
	// BUGS Swing
	
	// DONE ANDROID :
    //  20. Implement save track/route to gpx
	
	// DONE SWING

}
