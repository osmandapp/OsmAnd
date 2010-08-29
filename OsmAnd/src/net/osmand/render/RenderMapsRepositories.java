package net.osmand.render;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.data.index.IndexConstants;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;

import android.database.SQLException;

public class RenderMapsRepositories {
	
	private final static Log log = LogUtil.getLog(RenderMapsRepositories.class);
//	private SQLiteDatabase db;
	private Connection conn;
	private double cTopLatitude;
	private double cBottomLatitude;
	private int cZoom;
	private double cLeftLongitude;
	private double cRightLongitude;
	private List<Way> cWays = new LinkedList<Way>();
	private PreparedStatement pStatement;


	public boolean initializeNewResource(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		try {
			// db = SQLiteDatabase.openOrCreateDatabase(file, null);
			//			
			// if(db.getVersion() != IndexConstants.MAP_TABLE_VERSION){
			// db.close();
			// db = null;
			// return false;
			// }

			// TODO should support multiple db
			if (conn != null) {
				// close previous db
				conn.close();
				conn = null;
				pStatement = null;
			}
			try {
				Class.forName("org.sqlite.JDBC");
			} catch (Exception e) {
				log.error("Could not load driver", e);
				return false;
			}
			conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
			pStatement = conn.prepareStatement(loadMapQuery);

		} catch (SQLException e) {
			log.error("No connection", e);
			return false;
		} catch (java.sql.SQLException e) {
			log.error("No connection", e);
			return false;
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
		if(conn != null){
			try {
				conn.close();
			} catch (java.sql.SQLException e) {
			}
			conn = null;
			pStatement = null;
		}
//		if(db != null){
//			// close previous db
//			db.close();
//			db = null;
//		}
	}
	
	/**
	 * @return true if no need to reevaluate map
	 */
	public boolean updateMap(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom){
		if (conn == null) {
			return true;
		}
		boolean inside = cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
				&& cBottomLatitude <= bottomLatitude && cZoom == zoom;
		return inside;
	}


	private static String loadMapQuery = "SELECT "+IndexConstants.IndexMapWays.ID +", " + IndexConstants.IndexMapWays.NODES +", " + IndexConstants.IndexMapWays.TAGS +
						" FROM " + IndexConstants.IndexMapWays.getTable() + " WHERE "+IndexConstants.IndexMapWays.ID+" IN (SELECT id FROM "+IndexConstants.indexMapLocationsTable +   //$NON-NLS-1$
											" WHERE ? <  maxLat AND ? > minLat AND maxLon > ? AND minLon  < ?)"; //$NON-NLS-1$
	
	public void loadMap(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom) {
		cBottomLatitude = bottomLatitude - (topLatitude - bottomLatitude) / 2;
		cTopLatitude = topLatitude + (topLatitude - bottomLatitude) / 2; 
		cLeftLongitude = leftLongitude - (rightLongitude - leftLongitude) / 2;
		cRightLongitude = rightLongitude + (rightLongitude - leftLongitude) / 2;
		cZoom = zoom;
		
		
		log.info(String.format(
				"BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", cBottomLatitude, cTopLatitude, cLeftLongitude, cRightLongitude, zoom)); //$NON-NLS-1$
		
		long now = System.currentTimeMillis();
		
		if(pStatement == null){
			return;
		}
		try {
			pStatement.setDouble(1, cBottomLatitude);
			pStatement.setDouble(2, cTopLatitude);
			pStatement.setDouble(3, cLeftLongitude);
			pStatement.setDouble(4, cRightLongitude);
			ResultSet result = pStatement.executeQuery();
			
			List<Way> local = new LinkedList<Way>();
			try {
				int count = 0;
				while (result.next()) {
					long id = result.getLong(1);
					Way way = new Way(id);
					JSONArray nodes;
					try {
						nodes = new JSONArray(result.getString(2));
						for (int i = 0; i < nodes.length(); i++) {
							JSONArray obj = nodes.getJSONArray(i);
							Node node = new Node(obj.getDouble(1), obj.getDouble(2), obj.getLong(0));
							way.addNode(node);
						}

					} catch (JSONException e) {
					}
					count++;
					local.add(way);
				}

				cWays = local;
				log.info(String.format("Search has been done in %s ms. %s results were found.", System.currentTimeMillis() - now, count)); //$NON-NLS-1$
			} finally {
				result.close();
			}
		} catch (java.sql.SQLException e) {
			log.debug("Search failed", e);
		}
		
	}
	
	
	public void clearCache() {
		cWays.clear();
	}

}
