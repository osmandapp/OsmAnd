package net.osmand;



/**
 * This class is designed to put all to do's and link them with code.
 */
public class ToDoConstants {
	
	
	// TODO max 97
	// FOR 0.4 beta RELEASE
	//  ! 89. Transport redesign UI (enable run from context menu, switch go to goal/not) !

	//  !_25. Add all attributes needed for routing (highway attributes, turn_restrictions)
	//  !_22. Verify all POI has a point_type (in order to search them) and fix POI issues (!)

	//  !_29. Fix memory for netherlands map creator

	
	///  PROFILE AND REVIEW Rendering !!!
	//   - Review Ref on the road on low zooms
	//   - Fix broken multipolygon
	
	//+-!_28. Rotate crash (progress dialog) [not reproducible]
	//+- _18. Fix loading map data in rotated mode (check properly boundaries) 
	//+ !_1 . VELCOM animation
	//+  _30. About screen (Issue)
	//+  _30. Bug with landscape (?)
	
	// not required
	//  ! 81. Add some objects to POI category (1) to add them into OSM 2) to help navigation)
	//  highway (?), traffic_calming (?), barrier(?), military(?-), landuse (?), office(?), man_made(?), power(?),
	//  railway( station, subway?) - issue 17
	//  ! 87. Use network availability for defining loading tiles from internet.
	//    96. Introduce settings for MPH, imperial units 
	
	// 	_19. colors for road trunk and motorway
	// 	_12. Fix : find proper location for streets ! centralize them (when create index)?
	// 	_28. Fix freeze while map downloading (?)

	
	// 	Outside base 0.4 release
	//   86. Allow to add/edit custom tags to POI objects.
	//   91. Invent binary format (minimize disk space, maximize speed) 
	//   92. Replace poi index with standard map index and unify POI categories
	//   94. Revise index to decrease their size (especially address) - replace to float lat/lon and remove for POI 
	//		 remove en_names from POI (possibly from address)


	/////////////////////////////  UNKNOWN STATE  ////////////////////
	// Unscheduled (complex)
	//   65. Intermediate points - for better control routing, to avoid traffic jams ...(?)
	//   63. Support simple offline routing(require index file with turn restrictions etc) (?)
	
	// Not clear if it is really needed 
	//   69. Add phone information to POI
	//   66. Transport routing (show next stop, total distance, show stop get out, voice) (needed ?).
	//   85. Enable on/off screen for bike navigation (?)
	//   83. Add monitoring service to send locations to internet (?)

	// DONE ANDROID :
	//  93. Implement multitype vector objects - building with fence, road & tram ...
	//  90. Use Junidecode library on the client for english translation for map rendering
	//  96. Download voice data through UI interface (Issue)
	//  95. Show progress while map rendered and loaded (Issue)
	
	// DONE SWING
	//  12. Reinvent UI of swing app (remove Region object and clear other MapObject)
	
	
}
