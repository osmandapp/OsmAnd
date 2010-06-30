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
	//       Got by Andrei
	//   50. Invent opening hours editor in order to edit POI hours better on device
	//		 GOT by Olga
	
	// TODO sort hamlets by distance
    //   60. Audio guidance for routing
	//   61. Provide route information for YOURS (calclate turns/angle/expected time). 
	//		 Fix some missing turns in CloudMade (for secondary roads wo name). Add them (if dist to prev/next turn > 150m) [dacha]
	//   33. Build transport locations. Create transport index (transport-stops) (investigate)
	// 		 DONE: Load transport routes in swing.	
	//		 IDEA TO HAVE :

	
    //   43. Enable poi filter by name
	//   58. Upload/Download zip-index from site & unzip them on phone
	//   45. Get clear <Use internet> settings. Move that setting on top settings screen. 
	//       That setting should rule all activities that use internet. It should ask whenever internet is used 
	//		(would you like to use internet for that operation - if using internet is not checked). 
	//		Internet using now for : edit POI osm, show osm bugs layer, download tiles.
	   
	//   40. Support simple vector road rendering (require new index file) (?)
	//   63. Support simple offline routing(require new index file) (?)

	// BUGS Android
	//  FIXME !!!! Check agains ID is not unique ! (for relation/node/way - it could be the same)
	
	// TODO swing
	// 9. Fix issues with big files (such as netherlands) - save memory (!) - very slow due to transport index !
	// Current result : for big file (1 - task  60-80% time, 90% memory)
	// 1. Download tiles without using dir tiles (?)
 	// 10. Improve address indexing (use relations). 
    //	  use relation "a6" (to accumulate streets!),  "a3" to read all cities & define boundaries for city (& define that street in city). 
	
	// BUGS Swing
	
	//  DONE ANDROID :
	//   55. Update POI data from internet for selected area (do not suggest to create or extend POI index)  
	//   62. History of searched points	 (once point was selected to go/to show it is saved in history db and could be shown)
	//   47. Internet connectivity could be checked before trying to use [merged with 45]
    //   26. Show the whole street on map (when it is chosen in search activity). Possibly extend that story to show layer with streets. 
	// 		 [Closed : because it is not necessary]
	//   53. Add progress bars : to internet communication activities [editing/commiting/deleting poi], do not hide edit poi dialog if operation failed
	//   63. Implement internet search address [OSM Nominatim]
	//   56. Add usage of CloudMade API for calculating route (show next turn & distance to it instead of mini map).
	//   57. Implement routing information about expected time arriving
	//   58. Implement difference about show route/follow route (show travel time/arrival time, show mini map/next turn, etc)
	//   59. Show route information (directions/time, ....). Now is shown in context menu route (about route)
	//   46. Implement downloading strategy for tiles : select max zoom to download [16,15,14,...]
	// 		 That means you can save internet because from [16 -> zoom -> 18], [14 -> zoom -> 16 - suitable for speed > 40], ...
	
	// DONE SWING

}
