package com.osmand;


/**
 * This class is designed to put all to do's and link them with code.
 * The whole methods could be paste or just constants.
 * Do not worry to put ugly code here (just a little piece)
 */
public class ToDoConstants {
	

	
	// TODO
	
	//   Improvements
	//   5. Download with wget
	//   6. progress while map is loading
	
	// Not clear if it is really needed 
	//   69. Add phone information to POI
	//   70. Show building numbers over map (require changin address index - index 2 more columns lat/lon for fast search)

	// Unscheduled (complex)
	//   66. Transport routing (show next stop, total distance, show stop get out, voice) (?).
	//   64. Traffic information (?) - rmaps (http://jgo.maps.yandex.net/tiles?l=trf)?
	//   65. Intermediate points - for better control routing, to avoid traffic jams ...(?)
	//   40. Support simple vector road rendering (require new index file) (?)
	//   63. Support simple offline routing(require new index file) (?)


	// BUGS Android
	
	// TODO swing
	// 9. Fix issues with big files (such as netherlands) - save memory (!) - very slow due to transport index !
	// Current result : for big file (1 - task  60-80% time, 90% memory) (?)
 	// 10. Improve address indexing (use relations). (?)
    //	  use relation "a6" (to accumulate streets!),  "a3" to read all cities & define boundaries for city (& define that street in city). 
	
	// BUGS Swing
	
	//  DONE ANDROID :
	//   68. Implement service to app work with screen offline 
	//		 (audio guidance & add new item to status bar & introduce error interval for gps in buildings)
	//   71. Implement different mechanism for tiles (big sqlite planet see rmaps)
	//   72. Implement layers menu in map view (select/unselect vector layers to show)
	//   73. Implement addition POI filter to search online without sd indexes
	//   74. Implement preview route : show next turn & description (in "show only" route mode) & preview transport route.

	// DONE SWING

}
