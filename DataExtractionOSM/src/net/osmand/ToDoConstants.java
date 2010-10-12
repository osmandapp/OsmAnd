package net.osmand;



/**
 * This class is designed to put all to do's and link them with code.
 */
public class ToDoConstants {
	
	// TODO max 99
	// FOR 0.4 beta RELEASE
	// Profile 
	// Try/catch for all databases operation (disk I/O exceptions, do not have crash)
	// Fix downloader for broken connection !


	// 	Outside base 0.4 release
	//   69. Add phone and site information to POI (enable call to POI and open site)
	//   86. Allow to add/edit custom tags to POI objects (Issue)
	//   91. Invent binary format (minimize disk space, maximize speed) 
	//   92. Replace poi index with standard map index and unify POI categories
	//   94. Revise index to decrease their size (especially address) - replace to float lat/lon and remove for POI 
	//		 remove en_names from POI (possibly from address)
	//   97. For voice navigation consider current speed of vehicle. Especially when speed > 50 pronounce more than 200 m
	//   98. Implement rendering of different app mode. For Car render streets name with large font.
	//   96. Introduce settings for MPH, imperial units

	// 	_19. colors for road trunk and motorway
	// 	_12. Fix : find proper location for streets ! centralize them (when create index)?
	// 	_28. Fix freeze while map downloading (?)
	
	/////////////////////////////  UNKNOWN STATE  ////////////////////
	// Unscheduled (complex)
	//   65. Intermediate points - for better control routing, to avoid traffic jams ...(?)
	//   63. Support simple offline routing(require index file with turn restrictions etc) (?)
	
	// Not clear if it is really needed 
	//   66. Transport routing (show next stop, total distance, show stop get out, voice) (needed ?).
	//   85. Enable on/off screen for bike navigation (?)
	//   83. Add monitoring service to send locations to internet (?)

	
	///////////////////////////   DONE //////////////////////////////
	// DONE ANDROID :
	//  93. Implement multitype vector objects - building with fence, road & tram ...
	//  90. Use Junidecode library on the client for english translation for map rendering
	//  96. Download voice data through UI interface (Issue)
	//  95. Show progress while map rendered and loaded (Issue)
	//  87. Use network availability for defining loading tiles from internet.
	//  89. Transport redesign UI (enable run from context menu, switch go to goal/not) !
	//  81. Add some objects to POI category (1) to add them into OSM 2) to help navigation)
	//  	highway (?), traffic_calming (?), barrier(?), military(?-), landuse (?), office(?), man_made(?), power(?),
	//  	railway( station, subway?) - issue 17
	
	// DONE SWING
	//  12. Reinvent UI of swing app (remove Region object and clear other MapObject)
	//  13. Accept pdf files for map creation
	
	
}
