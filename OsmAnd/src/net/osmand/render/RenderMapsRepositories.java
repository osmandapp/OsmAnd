package net.osmand.render;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.data.index.IndexConstants;
import net.osmand.osm.Way;

import org.apache.commons.logging.Log;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class RenderMapsRepositories {
	
	private final static Log log = LogUtil.getLog(RenderMapsRepositories.class);
	private SQLiteDatabase db;
	private double cTopLatitude;
	private double cBottomLatitude;
	private int cZoom;
	private double cLeftLongitude;
	private double cRightLongitude;
	private List<Way> cWays = new LinkedList<Way>();


	public boolean initializeNewResource(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		try {
		// TODO should support multiple db
		if(db != null){
			// close previous db
			db.close();
		}
		db = SQLiteDatabase.openOrCreateDatabase(file, null);
		
		if(db.getVersion() != IndexConstants.MAP_TABLE_VERSION){
			db.close();
			db = null;
			return false;
		}
		
			
		} catch(SQLException e){
			
		}
		if (log.isDebugEnabled()) {
			log.debug("Initializing db " + file.getAbsolutePath() + " " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return true;
	}
	
	public List<Way> getCache() {
		return cWays;
	}
	
	
	public void clearAllResources(){
		if(db != null){
			// close previous db
			db.close();
			db = null;
		}
	}
	
	/**
	 * @return true if no need to reevaluate map
	 */
	public boolean updateMap(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom){
		if (db == null) {
			return true;
		}
		boolean inside = cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
				&& cBottomLatitude <= bottomLatitude && cZoom == zoom;
		return inside;
	}

	
	public void loadMap(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom) {
		cBottomLatitude = bottomLatitude - (topLatitude - bottomLatitude) / 2;
		cTopLatitude = topLatitude + (topLatitude - bottomLatitude) / 2; 
		cLeftLongitude = leftLongitude - (rightLongitude - leftLongitude) / 2;
		cRightLongitude = rightLongitude + (rightLongitude - leftLongitude) / 2;
		cZoom = zoom;
		
//		String query = "SELECT ways.id way, node.id node, node.latitude, node.longitude FROM (" + //$NON-NLS-1$
//					   "SELECT DISTINCT ways.id id FROM ways JOIN " + //$NON-NLS-1$
//					   "(SELECT id, latitude, longitude FROM node WHERE ?<  latitude AND latitude < ? AND ? < longitude AND longitude < ?)  A "+ //$NON-NLS-1$
//					   "ON  A.id = ways.node) B "+ //$NON-NLS-1$
//					   "JOIN  ways ON B.id=ways.id JOIN node ON ways.node = node.id"; //$NON-NLS-1$
		
		log.info(String.format(
				"BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", cBottomLatitude, cTopLatitude, cLeftLongitude, cRightLongitude, zoom)); //$NON-NLS-1$
		
		long now = System.currentTimeMillis();
		
//		String query = "SELECT id, tags, nodes FROM ways WHERE ? <  latitude AND latitude < ? AND ? < longitude AND longitude < ? "; //$NON-NLS-1$
		String query = "SELECT id FROM "+IndexConstants.indexMapLocationsTable +   //$NON-NLS-1$
					" WHERE ? <  maxLat AND ? > minLat AND maxLon > ? AND minLon  < ?"; //$NON-NLS-1$
		
		Cursor result = db.rawQuery(query, new String[]{Double.toString(cBottomLatitude),Double.toString(cTopLatitude),
										Double.toString(cLeftLongitude), Double.toString(cRightLongitude)});
		
		List<Way> local = new LinkedList<Way>();
		try {
			int count = 0;
			if (result.moveToFirst()) {
				do {
					long id = result.getLong(0);
					Way way = new Way(id);
//					JSONArray nodes;
//					try {
//						nodes = new JSONArray(result.getString(2));
//						for (int i = 0; i < nodes.length(); i++) {
//							JSONArray obj = nodes.getJSONArray(i);
//							Node node = new Node(obj.getDouble(1), obj.getDouble(2), obj.getLong(0));
//							way.addNode(node);
//						}
//						
//					} catch (JSONException e) {
//					}
					count++;
//					local.add(way);
				} while (result.moveToNext());
				
				cWays = local;
			}
			log.info(String.format("Search has been done in %s ms. %s results were found.", System.currentTimeMillis()-now, count)); //$NON-NLS-1$
		} finally {
			result.close();
		}
		
	}
	
	
	public void clearCache() {
		cWays.clear();
	}

}
