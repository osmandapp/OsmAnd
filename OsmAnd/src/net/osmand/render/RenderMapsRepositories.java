package net.osmand.render;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.data.index.IndexConstants;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

import org.apache.commons.logging.Log;

import android.graphics.RectF;

public class RenderMapsRepositories {
	
	private final static Log log = LogUtil.getLog(RenderMapsRepositories.class);
	private Connection conn;
	private double cTopLatitude;
	private double cBottomLatitude;
	private int cZoom;
	private double cLeftLongitude;
	private double cRightLongitude;
	private List<Way> cWays = new LinkedList<Way>();
	private RectF cachedWaysLoc = new RectF();
	private PreparedStatement pStatement;


	public boolean initializeNewResource(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		try {
			// TODO should support multiple db
			if (conn != null) {
				// close previous db
				conn.close();
				conn = null;
				pStatement = null;
			}
			try {
				Class.forName("org.sqlite.JDBC"); //$NON-NLS-1$
			} catch (Exception e) {
				log.error("Could not load driver", e); //$NON-NLS-1$
				return false;
			}
			conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath()); //$NON-NLS-1$
			pStatement = conn.prepareStatement(loadMapQuery);
			Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery("PRAGMA user_version"); //$NON-NLS-1$
			int v = rs.getInt(1);
			rs.close();
			if(v != IndexConstants.MAP_TABLE_VERSION){
				return false;
			}
			stat.close();

		} catch (Exception e) {
			log.error("No connection", e); //$NON-NLS-1$
			if(conn != null){
				try {
					conn.close();
				} catch (java.sql.SQLException e1) {
				}
				conn = null;
			}
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
	
	// if cache was changed different instance will be returned
	public RectF getCachedWaysLoc() {
		return cachedWaysLoc;
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


	private static String loadMapQuery = "SELECT "+IndexConstants.IndexMapWays.ID +", " + IndexConstants.IndexMapWays.NODES +", " + IndexConstants.IndexMapWays.TAGS + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						" FROM " + IndexConstants.IndexMapWays.getTable() + " WHERE "+IndexConstants.IndexMapWays.ID+" IN (SELECT id FROM "+IndexConstants.indexMapLocationsTable +   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
											" WHERE ? <  maxLat AND ? > minLat AND maxLon > ? AND minLon  < ?)"; //$NON-NLS-1$
	
	
	public void loadMap(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom) {
		cBottomLatitude = bottomLatitude - (topLatitude - bottomLatitude) / 2;
		cTopLatitude = topLatitude + (topLatitude - bottomLatitude) / 2; 
		cLeftLongitude = leftLongitude - (rightLongitude - leftLongitude);
		cRightLongitude = rightLongitude + (rightLongitude - leftLongitude);
		cZoom = zoom;
		
		
		log.info(String.format(
				"BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", cBottomLatitude, cTopLatitude, cLeftLongitude, cRightLongitude, cZoom)); //$NON-NLS-1$
		
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
					byte[] bytes= result.getBytes(2);
					for (int i = 0; i < bytes.length; i += 8) {
						int l2 = Algoritms.parseIntFromBytes(bytes, i);
						int l3 = Algoritms.parseIntFromBytes(bytes, i + 4);
						Node node = new Node(Float.intBitsToFloat(l2), Float.intBitsToFloat(l3), -1);
						way.addNode(node);
					}
					
					count++;
					local.add(way);
				}

				cWays = local;
				// create new instance to distinguish that cache was changed
				cachedWaysLoc = new RectF();
				cachedWaysLoc.set((float)cLeftLongitude, (float)cTopLatitude, (float)cRightLongitude, (float)cBottomLatitude);
				log.info(String.format("Search has been done in %s ms. %s results were found.", System.currentTimeMillis() - now, count)); //$NON-NLS-1$
			} finally {
				result.close();
			}
		} catch (java.sql.SQLException e) {
			log.debug("Search failed", e); //$NON-NLS-1$
		}
		
	}
	
	
	public void clearCache() {
		cWays.clear();
	}

}
