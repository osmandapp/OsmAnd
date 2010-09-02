package net.osmand.render;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.data.index.IndexConstants;
import net.osmand.osm.MapRenderObject;

import org.apache.commons.logging.Log;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class RenderMapsRepositories {
	
	private final static Log log = LogUtil.getLog(RenderMapsRepositories.class);
	private Connection conn;
	private double cTopLatitude;
	private double cBottomLatitude;
	private double cLeftLongitude;
	private double cRightLongitude;
	
	private int cZoom;
	private List<MapRenderObject> cObjects = new LinkedList<MapRenderObject>();
	private RectF cachedWaysLoc = new RectF();
	private PreparedStatement pStatement;
	
	private OsmandRenderer renderer = new OsmandRenderer();


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
	
	public List<MapRenderObject> getCache() {
		return cObjects;
	}
	
	// if cache was changed different instance will be returned
	public RectF getCachedWaysLoc() {
		return cachedWaysLoc;
	}
	
	
	public void clearAllResources(){
		clearCache();
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
	public boolean updateMapIsNotNeeded(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom){
		if (conn == null) {
			return true;
		}
		boolean inside = cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
				&& cBottomLatitude <= bottomLatitude && cZoom == zoom;
		return inside;
	}


	private static String loadMapQuery = "SELECT "+IndexConstants.IndexMapRenderObject.ID +", " + IndexConstants.IndexMapRenderObject.NODES +", " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										IndexConstants.IndexMapRenderObject.NAME + ", " + IndexConstants.IndexMapRenderObject.TYPE + //$NON-NLS-1$
										" FROM " + IndexConstants.IndexMapRenderObject.getTable() +	" WHERE "+IndexConstants.IndexMapRenderObject.ID+  //$NON-NLS-1$//$NON-NLS-2$
											" IN (SELECT id FROM "+IndexConstants.indexMapLocationsTable +   //$NON-NLS-1$
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
			
			List<MapRenderObject> local = new LinkedList<MapRenderObject>();
			try {
				int count = 0;
				while (result.next()) {
					long id = result.getLong(1);
					MapRenderObject obj = new MapRenderObject(id);
					obj.setData(result.getBytes(2));
					obj.setName(result.getString(3));
					obj.setType(result.getInt(4));
					count++;
					local.add(obj);
				}

				cObjects = local;
				// create new instance to distinguish that cache was changed
				cachedWaysLoc = new RectF((float)cLeftLongitude, (float)cTopLatitude, (float)cRightLongitude, (float)cBottomLatitude);
				log.info(String.format("Search has been done in %s ms. %s results were found.", System.currentTimeMillis() - now, count)); //$NON-NLS-1$
			} finally {
				result.close();
			}
		} catch (java.sql.SQLException e) {
			log.debug("Search failed", e); //$NON-NLS-1$
		}
		renderer.generateNewBitmap(cachedWaysLoc, cObjects, cZoom, 0);
	}
	
	public Bitmap getBitmap() {
		return renderer.getBitmap();
	}
	
	
	public void clearCache() {
		cObjects.clear();
		renderer.clearBitmap();
	}

}
