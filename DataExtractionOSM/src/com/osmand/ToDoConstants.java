package com.osmand;


/**
 * This class is designed to put all to do's and link them with code.
 * The whole methods could be paste or just constants.
 * Do not worry to put ugly code here (just a little piece)
 */
public class ToDoConstants {
	
	// TODO ANDROID
	//   for 0.3
	//   Improvement : Show stops in the transport route on the map 
	// 	 Improvement : show favorites on the map?
	//   Improvement : hard to press on the POI/....
	//   Improvement : show detailed route on the map with turns and show route information directly (like in gmaps)
	//   Improvement : redesign poi selecting (show on map )
	//   Improvement : progress while loading tiles
	//   Improvement : download with wget
	//   Yandex traffic : http://jgo.maps.yandex.net/tiles?l=trf
	//   Improvement : show route info after route is calculated and/or calculate route from context menu
	//					(continue follow previous route)
	//   Improvement : show route info (directly after route is calc & do not show if it is not calc)

	
	// FIXME 
	// 2. Bug with network location while routing (?) - fixed ?
	// 5. After return if there was previous route (continue follow)
	// 6. Bug 13.
 

	
	// Not clear if it is really needed 
	//   69. Add phone information to POI
	//   70. Show building numbers over map


	// Unscheduled (complex)
	//   66. Transport routing (show next stop, total distance, show stop get out) (?).
	//   64. Traffic information (?) - rmaps?
	//   65. Intermediate points - for better control routing, to avoid traffic jams ...(?)
	//   40. Support simple vector road rendering (require new index file) (?)
	//   63. Support simple offline routing(require new index file) (?)


	// BUGS Android
	
	// TODO swing
	// 9. Fix issues with big files (such as netherlands) - save memory (!) - very slow due to transport index !
	// Current result : for big file (1 - task  60-80% time, 90% memory) (?)
	// 1. Download tiles without using dir tiles (?) 
 	// 10. Improve address indexing (use relations). (?)
    //	  use relation "a6" (to accumulate streets!),  "a3" to read all cities & define boundaries for city (& define that street in city). 
	
	// BUGS Swing
	
	//  DONE ANDROID :
	//   68. Implement service to app work with screen offline 
	//		 (audio guidance & add new item to status bar & introduce error interval for gps in buildings)
	//   71. Implement different mechanism for tiles (big sqlite planet see rmaps)

	
	// DONE SWING

}
