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

	// Prepare update : android description, sites, screenshots
	
	 // TODO ANDROID 
	//   42. Revise UI (icons/layouts). Support different devices. Add inactive/focus(!) icon versions.
	//		 Some icons are not fine (as back menu from map - it is blured).


    //   60. Audio guidance for routing
	//   61. Provide route information for YOURS (calclate turns/angle/expected time). 
	//		 Fix some missing turns in CloudMade (for secondary roads wo name). Add them (if dist to prev/next turn > 150m) [dacha]
	
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
	//  double tap to zoom [done]
	//  forbid rotate map to landscape [ done]
	// 	REFACTOR Settings activity ( for check box properties!) [partially done]
	//  show POI choose near by or last map selection [done] 
	//  Show poi direction (using sensor) [done]
	
	//  hide center point (enable only when trackball using)?
	//  Fix bugs with test data (bug with follow turn / left time / add turn)
	//  Improvement : Show stops in the transport route
	
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
	//   50. Invent opening hours editor in order to edit POI hours better on device
	
	// DONE SWING

}
