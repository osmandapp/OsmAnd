package net.osmand;

import java.io.File;
import java.sql.SQLException;

import net.osmand.data.preparation.IndexCreator;


/**
 * This class is designed to put all to do's and link them with code.
 */
public class ToDoConstants {
	
	// TODO swing
	// ! 9. Fix issues with big files (such as netherlands) - save memory (!)
	// Current result : for big file (1 - task  60-80% time, 90% memory) (?) (+)
	//   11. Index buildings using interpolations (from nodes) (+)
	// ! 12. Reinvent UI of swing app (remove Region object and clear other MapObject) use indexes to show results
	//   13. Automative to index way nodes or not.
	
	// TODO max 86
	//  ! 81. Add some objects to POI category (1) to add them into OSM 2) to help navigation)
	//  highway (?), traffic_calming (?), barrier(?), military(?-), landuse (?), office(?), man_made(?), power(?),
	//  railway( station, subway?) - issue 17
	
	// TODO check network availability
	// TODO in IndexCreator and other files!
	
	
	// TODO BUGS:
	//  	USA indexes
	//  ! VELCOM - competition (ppt)
	//  ! search poi without gps !
	//    rotate map gps without location
	//    recalculating route when location is far from !
	// ----- from site ---
	// - 5 (?)
	// - menu order (41)
	// - landscape view not reachable (39)
	// - 81. (17)
	// -- house tagging (31)
	// --- go back to osmand (23)
	// --- keyboard (43)
	// --- add poi tags (44)

	
	//  PRESENTS : mediamarkt, parfum, coffee, (al?), (om?), olia?
	
 

	// Unscheduled (complex)
	//   65. Intermediate points - for better control routing, to avoid traffic jams ...(?)
	//   40. Support simple vector road rendering (require new index file) (?)
	//   63. Support simple offline routing(require new index file) (?)
	
	// Improvements
	// ! Download with wget
	// ! progress while map is loading
	
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
	
	public static void main(String[] args) throws SQLException, ClassNotFoundException {
		File dir = new File("e:\\Information\\OSM maps\\osm_batch_ind\\");
		Class.forName("org.sqlite.JDBC");
		for(File f : dir.listFiles()){
			if(f.getName().endsWith(".odb")){
				IndexCreator.removeWayNodes(f);
			}
		}
	}

}
