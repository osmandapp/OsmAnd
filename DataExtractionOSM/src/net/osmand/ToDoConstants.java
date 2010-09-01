package net.osmand;



/**
 * This class is designed to put all to do's and link them with code.
 */
public class ToDoConstants {
	
	// TODO swing
	// ! 12. Reinvent UI of swing app (remove Region object and clear other MapObject) use indexes to show results
	
	// TODO max 87
	//  ! 81. Add some objects to POI category (1) to add them into OSM 2) to help navigation)
	//  highway (?), traffic_calming (?), barrier(?), military(?-), landuse (?), office(?), man_made(?), power(?),
	//  railway( station, subway?) - issue 17
	//   86. Allow to add/edit custom tags to POI objects.
	//   87. Use network availability for defining loading tiles from internet.
	
	
	// TODO's in IndexCreator and other files!
	// TODO order in nodes.tmp.odb !!! fix for future 
	
	// TODO BUGS/Improvements:
	//  1! VELCOM
	//  2. rotate map gps without location
	//  3! Transport redesign call UI (enable context menu call, switch go to goal/not) 
	//  4. recalculating route when location is far from !
	//  5. keyboard (issue 43 )?
	//  6. Do not upload empty files (transport, poi... ). Implement auto-delete from site.
	//  7. In all places verify to use float lat/lon improve usage indexes !!!
	
	//  8. Download with wget
	//  9. progress while map is loading
	
	// Unscheduled (complex)
	//   65. Intermediate points - for better control routing, to avoid traffic jams ...(?)
	//   40. Support simple vector road rendering (require new index file) (?)
	//   63. Support simple offline routing(require new index file) (?)
	
	// Not clear if it is really needed 
	//   69. Add phone information to POI
	//   70. Show building numbers over map (require changing address index - index 2 more columns lat/lon for fast search)
	//   66. Transport routing (show next stop, total distance, show stop get out, voice) (needed ?).
	//   85. Enable on/off screen for bike navigation (?)
	//   83. Add monitoring service to send locations to internet (?)

	// DONE ANDROID :
	// 82. Rotate map according compass
	// 85. Remove context menu on long press map ! Accumulate actions and show label (+)
	
	// DONE SWING
 	// 10. Improve address indexing (use relations). (+)
    //	  use relation "a6" (to accumulate streets!),  "a3" to read all cities & define boundaries for city (& define that street in city).
	// ! 9. Fix issues with big files (such as netherlands) - save memory (!)
	// Current result : for big file (1 - task  60-80% time, 90% memory) (?) (+)
	//   11. Index buildings using interpolations (from nodes) (+)

	
}
