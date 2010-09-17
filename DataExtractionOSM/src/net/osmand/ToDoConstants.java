package net.osmand;



/**
 * This class is designed to put all to do's and link them with code.
 */
public class ToDoConstants {
	
	// TODO swing
	// !!! 12. Reinvent UI of swing app (remove Region object and clear other MapObject) use indexes to show results
	
	// TODO max 87
	//  ! 81. Add some objects to POI category (1) to add them into OSM 2) to help navigation)
	//  highway (?), traffic_calming (?), barrier(?), military(?-), landuse (?), office(?), man_made(?), power(?),
	//  railway( station, subway?) - issue 17
	//   86. Allow to add/edit custom tags to POI objects.
	//   87. Use network availability for defining loading tiles from internet.

	//   89. Transport redesign UI (enable run from context menu, switch go to goal/not) !
	//   90. Use Junidecode library on the client for fast english translation
	//   94. Implement navigate using selected gpx track
	
	//   91. Invent binary format (minimize disk space, maximize speed) 
	//   92. Replace poi index with standard map index and unify POI categories
	//   93. Implement multytype vector objects (?) - building with fence, road & tram ... (binary format)
	
	// TODO small improvements for release :
	// 1. If select vector map, notice if there are no loaded maps. 
	
	// TODO Improvements :
	//  1! VELCOM
	//  +2. rotate map gps without location
	//  4. recalculating route when location is far from ! (error)
	//  5. keyboard (issue 43 )?
	//  6. Do not upload empty files (transport, poi... ). 
	//  7. Implement auto-delete from site (done, should be ported from C#)
	//  8. In all places verify to use float lat/lon improve disk space for indexes !!!
	
	//  +13! Support multiple database for map rendering
	//  16. Internet access bits
	//  17. Implement multipolygons to polygons (!?) + coastline
	//  18. Fix loading map data in rotated mode (check properly boundaries)   
	//  20. Add text to rendering (area, point, polyline) 
	//			features_text.xml, main_text.xml, layer-placenames.xml.inc
	//  21. Shield, ref.
	//  22. Verify all POI has a point_type (in order to search them)

	//  19. colors for road trunk and motorway
	//  12. Fix : find proper location for streets ! centralize them (when create index)?
	
	// TODO Check 
	// 1. check postal_code if the building was registered by relation!
	// 2. TEST after refactoring : poi custom filters
	
	//  8. Download with wget
	//  9. progress while map is loading
	
	// Unscheduled (complex)
	//   65. Intermediate points - for better control routing, to avoid traffic jams ...(?)
	//   40. Support simple vector road rendering (require new index file) (?)
	//   63. Support simple offline routing(require new index file) (?)
	
	// Not clear if it is really needed 
	//   69. Add phone information to POI
	//   66. Transport routing (show next stop, total distance, show stop get out, voice) (needed ?).
	//   85. Enable on/off screen for bike navigation (?)
	//   83. Add monitoring service to send locations to internet (?)

	// DONE ANDROID :
	// 70. Show building numbers over map (require changing address index - index 2 more columns lat/lon for fast search). 
	//	  (Not needed, because of vector rendering)
	// 82. Rotate map according compass
	// 85. Remove context menu on long press map ! Accumulate actions and show label (+)
	// 88. Implement show gpx track from folder
	
	// DONE SWING
 	// 10. Improve address indexing (use relations). (+)
    //	  use relation "a6" (to accumulate streets!),  "a3" to read all cities & define boundaries for city (& define that street in city).
	// ! 9. Fix issues with big files (such as netherlands) - save memory (!)
	// Current result : for big file (1 - task  60-80% time, 90% memory) (?) (+)
	//   11. Index buildings using interpolations (from nodes) (+)

	
}
