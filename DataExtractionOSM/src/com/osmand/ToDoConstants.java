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
	//   42. Revise UI (icons/layouts). Support different devices. Add inactive/focus(!) icon versions.
	//		 Some icons are not fine (as back menu from map - it is blured).
	
	//   46. Implement downloading strategy for tiles : select max zoom to download [16,15,14,...]
	// 		 That means you can save internet because from [16 -> zoom -> 18], [14 -> zoom -> 16 - suitable for speed > 40], ...
	
	//   61. Provide route information for YOURS (calclate turns/angle/expected time)
    //   60. Audio guidance for routing
	//   58. Upload/Download zip-index from site & unzip them on phone
	//   50. Invent opening hours editor in order to edit POI hours better on device
	//   53. Add progress bars : to internet communication activities [editing/commiting/deleting poi], do not hide edit poi dialog if operation failed 
	//		 [move close buttons from alertdialog to own view]
	//   55. Update POI data from internet for selected area [suggest to create new POI index or extend exising of none exist]
    //   43. Enable poi filter by name
	//   45. Get clear <Use internet> settings. Move that setting on top settings screen. 
	//       That setting should rule all activities that use internet. It should ask whenever internet is used 
	//		(would you like to use internet for that operation - if using internet is not checked). 
	//		Internet using now for : edit POI osm, show osm bugs layer, download tiles.
	
	//   33. Build transport locations. Create transport index (transport-stops) (investigate)
	// 		 DONE: Load transport routes in swing.	
	//		 IDEA TO HAVE :   
	//   47. Internet connectivity could be checked before trying to use (?)
    //   40. Support simple vector road rendering (require new index file) (?)
    //   26. Show the whole street on map (when it is chosen in search activity). Possibly extend that story to show layer with streets. (?)

	
	// BUGS Android
	
	// TODO swing
	// 9. Fix issues with big files (such as netherlands) - save memory (!) - very slow due to transport index !
	// Current result : for big file (1 - task  60-80% time, 90% memory)
	// 1. Download tiles without using dir tiles (?)
 	// 10. Improve address indexing (use relations). 
    //	  use relation "a6" (to accumulate streets!),  "a3" to read all cities & define boundaries for city (& define that street in city). 
	
	// BUGS Swing
	
	// DONE ANDROID :
	//   56. Add usage of CloudMade API for calculating route (show next turn & distance to it instead of mini map).
	//   57. Implement routing information about expected time arriving
	//   58. Implement difference about show route/follow route (show travel time/arrival time, show mini map/next turn, etc)
	//   59. Show route information (directions/time, ....). Now is shown in context menu route (about route)
	
	// DONE SWING

}
