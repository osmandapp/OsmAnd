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

	// pinch zoom, fix bugs with test data
	 
	
	// TODO ANDROID
	// Prepare update v 0.2.1:  screenshots, android description, sites(osmand/wiki), US/canada indexes & poi/transport.index

	//   61. Provide route information for YOURS (calclate turns/angle/expected time). 
	//		 Fix some missing turns in CloudMade (for secondary roads wo name). Add them (if dist to prev/next turn > 150m) [dacha]
    //   60. Audio guidance for routing
	
    //   43. Enable poi filter by name
	//   58. Upload/Download zip-index from site & unzip them on phone
	//   45. Get clear <Use internet> settings. Move that setting on top settings screen. 
	//       That setting should rule all activities that use internet. It should ask whenever internet is used 
	//		(would you like to use internet for that operation - if using internet is not checked). 
	//		Internet using now for : edit POI osm, show osm bugs layer, download tiles.

	//   66. Transport routing (show next stop, total distance, show stop get out) (?).
	//   64. Traffic information (?)
	//   65. Intermediate points (?)
	//   40. Support simple vector road rendering (require new index file) (?)
	//   63. Support simple offline routing(require new index file) (?)


	// FIXME BUGS Android
	// 1. Fix bugs with test data (bug with follow turn / left time / add turn)
	// 2. Improvement : Show stops in the transport route
	// 3. Pinch zoom
	
	// TODO swing
	// 9. Fix issues with big files (such as netherlands) - save memory (!) - very slow due to transport index !
	// Current result : for big file (1 - task  60-80% time, 90% memory)
	// 1. Download tiles without using dir tiles (?)
 	// 10. Improve address indexing (use relations). 
    //	  use relation "a6" (to accumulate streets!),  "a3" to read all cities & define boundaries for city (& define that street in city). 
	
	// BUGS Swing
	
	//  DONE ANDROID :
	//   33. Build transport locations. Create transport index (transport-stops) (investigate)
	// 		 Not implemented  : show key/transit stops on map, follow mode (show next stop)

	
	// DONE SWING

}
