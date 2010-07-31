package com.osmand;


/**
 * This class is designed to put all to do's and link them with code.
 * The whole methods could be paste or just constants.
 * Do not worry to put ugly code here (just a little piece)
 */
public class ToDoConstants {
	
	// TODO max 85
	//  ! 81. Add some objects to POI category (1) to add them into OSM 2) to help navigation)
	//  highway (?), traffic_calming (?), barrier(?), military(?-), landuse (?), office(?), man_made(?), power(?),
	//  railway( station, subway?) - issue 17

	
	// TODO BUGS Android
	// ! 3. different screens better support
	
	// Improvements
	// ! Download with wget
	// ! progress while map is loading
	
	// Not clear if it is really needed 
	//   69. Add phone information to POI
	//   70. Show building numbers over map (require changing address index - index 2 more columns lat/lon for fast search)
	//   66. Transport routing (show next stop, total distance, show stop get out, voice) (needed ?).
	//   85. Enable on/off screen for bike navigation (?)
	//   83. Add monitoring service to send locations to internet (?)

	// Unscheduled (complex)
	//   65. Intermediate points - for better control routing, to avoid traffic jams ...(?)
	//   40. Support simple vector road rendering (require new index file) (?)
	//   63. Support simple offline routing(require new index file) (?)

	
	// TODO swing
	// 9. Fix issues with big files (such as netherlands) - save memory (!) - very slow due to transport index !
	// Current result : for big file (1 - task  60-80% time, 90% memory) (?)
 	// ! 10. Improve address indexing (use relations). (?) // SLOBODSKAYA 157, 95
    //	  use relation "a6" (to accumulate streets!),  "a3" to read all cities & define boundaries for city (& define that street in city). 
	
	// BUGS Swing
	
	// DONE ANDROID :
	//   80. Export/import favorite points
	//   84. Send letter to developer when app crashes 
	//   78. Add ruler to the main tile view (100m, 200m,...) (+)
	//   82. Add overzoom +2 for Mapnik 
	//   64. Traffic information  - yandex traffic
	//   79. Download any WMS layer and add to swing version (add tile manager ${x}, ${y}, ${z} to swing and to android)
	//   77. Implement upload gps track onto osm server (? not implemented yet on OSM?) - 
	//		 not really needed, because gps tracks should be prepared before loading to OSM (OSM is not ready for it)
	
	// DONE SWING

}
