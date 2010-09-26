package net.osmand;



/**
 * This class is designed to put all to do's and link them with code.
 */
public class ToDoConstants {
	
	
	// TODO max 94
	
	// FOR 0.4 beta RELEASE
	//  ! 81. Add some objects to POI category (1) to add them into OSM 2) to help navigation)
	//  highway (?), traffic_calming (?), barrier(?), military(?-), landuse (?), office(?), man_made(?), power(?),
	//  railway( station, subway?) - issue 17
	//  ! 87. Use network availability for defining loading tiles from internet.
	//  ! 89. Transport redesign UI (enable run from context menu, switch go to goal/not) !
	//  ! 95. Show progress while map rendered and loaded
	//    90. Use Junidecode library on the client for english translation (for map rendering and other save disk space)
	
	// 	outside base 0.4 release
	//   86. Allow to add/edit custom tags to POI objects.
	//   91. Invent binary format (minimize disk space, maximize speed) 
	//   92. Replace poi index with standard map index and unify POI categories
	//   93. Implement multitype vector objects (?) - building with fence, road & tram ... (binary format)
	//   94. Revise index to decrease their size (especially address) - replace to float lat/lon

	
	// TODO small improvements for release :
	//  19. colors for road trunk and motorway
	//  12. Fix : find proper location for streets ! centralize them (when create index)?
	//  24. +! define clockwise/anticlockwise on android for closed path to understand area outised or inside
	//      fix Rendering for incompleted rings, 
	//      fix Index Creator : 1) determine clockwise area (inverse if needed)+ 2) index low level multipolygon pass param +
	// 							3) pass inverse to low level indexes+ 4) coastline / add area + 
	//							5) identify case when there is only inner boundary (!!!!) +
	//  26. Move leisure below forest and grass layer (footway/cycleway below unspecified)
	//  25. Add all attributes needed for routing (highway attributes, turn_restrictions)
	//  27. Fix bug with some buildings in AMS (WTC) that have fence(?)
	//  28. Fix freeze while map downloading
	
	// TODO Improvements :
	//  1! VELCOM
	// +17. Implement multipolygons to polygons (!?) + coastline - (todo indexCreator) 
	// +23! Remove one bit from type!
	//+-18. Fix loading map data in rotated mode (check properly boundaries)   
	//  22. Verify all POI has a point_type (in order to search them)

	
	// Unscheduled (complex)
	//   65. Intermediate points - for better control routing, to avoid traffic jams ...(?)
	//   63. Support simple offline routing(require new index file) (?)
	
	// Not clear if it is really needed 
	//   69. Add phone information to POI
	//   66. Transport routing (show next stop, total distance, show stop get out, voice) (needed ?).
	//   85. Enable on/off screen for bike navigation (?)
	//   83. Add monitoring service to send locations to internet (?)

	// DONE ANDROID :
	
	// DONE SWING
	//  12. Reinvent UI of swing app (remove Region object and clear other MapObject)  	
	
}
