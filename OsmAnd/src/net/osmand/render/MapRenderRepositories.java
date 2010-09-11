package net.osmand.render;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.data.index.IndexConstants;
import net.osmand.osm.MapRenderObject;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;

public class MapRenderRepositories {
	
	private final static Log log = LogUtil.getLog(MapRenderRepositories.class);
	private final Context context;
	private Connection conn;
	private PreparedStatement pStatement;
	private PreparedStatement pStatement2;
	private PreparedStatement pStatement3;
	private OsmandRenderer renderer;
	
	private double cTopLatitude;
	private double cBottomLatitude;
	private double cLeftLongitude;
	private double cRightLongitude;
	private int cZoom;
	private float cRotate;
	
	// cached objects in order to rotate without 
	private List<MapRenderObject> cObjects = new LinkedList<MapRenderObject>();
	private RectF cachedWaysLoc = new RectF();
	private Bitmap bmp;
	
	
	
	public MapRenderRepositories(Context context){
		this.context = context;
		this.renderer = new OsmandRenderer(context);
	}
	
	public Context getContext() {
		return context;
	}
	
	public boolean initializeNewResource(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		try {
			if (conn != null) {
				// close previous db
				conn.close();
				conn = null;
				pStatement = null;
				pStatement2 = null;
				pStatement3 = null;
			}
			try {
				Class.forName("org.sqlite.JDBC"); //$NON-NLS-1$
			} catch (Exception e) {
				log.error("Could not load driver", e); //$NON-NLS-1$
				return false;
			}
			conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath()); //$NON-NLS-1$
			pStatement = conn.prepareStatement(loadMapQuery);
			pStatement2 = conn.prepareStatement(loadMapQuery2);
			pStatement3 = conn.prepareStatement(loadMapQuery3);
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
			pStatement2 = null;
			pStatement3 = null;
		}
	}
	
	
	/**
	 * @return true if no need to reevaluate map
	 */
	public boolean updateMapIsNotNeeded(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, float rotate){
		if (conn == null) {
			return true;
		}
		boolean inside = insideBox(topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom);
		if(rotate < 0){
			rotate += 360;
		} 

		return inside && Math.abs(rotate - cRotate) < 30;
		
	}

	private boolean insideBox(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom) {
		boolean inside = cTopLatitude >= topLatitude && cLeftLongitude <= leftLongitude && cRightLongitude >= rightLongitude
				&& cBottomLatitude <= bottomLatitude && cZoom == zoom;
		return inside;
	}


	private static String loadMapQuery = "SELECT "+IndexConstants.IndexMapRenderObject.ID +", " + IndexConstants.IndexMapRenderObject.NODES +", " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										IndexConstants.IndexMapRenderObject.NAME + ", " + IndexConstants.IndexMapRenderObject.TYPE + //$NON-NLS-1$
										" FROM " + IndexConstants.IndexMapRenderObject.getTable() +	" WHERE "+IndexConstants.IndexMapRenderObject.ID+  //$NON-NLS-1$//$NON-NLS-2$
											" IN (SELECT id FROM "+IndexConstants.indexMapLocationsTable +   //$NON-NLS-1$
												" WHERE ? <  maxLat AND ? > minLat AND maxLon > ? AND minLon  < ?)"; //$NON-NLS-1$
	
	private static String loadMapQuery2 = "SELECT "+IndexConstants.IndexMapRenderObject.ID +", " + IndexConstants.IndexMapRenderObject.NODES +", " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										  IndexConstants.IndexMapRenderObject.NAME + ", " + IndexConstants.IndexMapRenderObject.TYPE + //$NON-NLS-1$
										  " FROM " + IndexConstants.IndexMapRenderObject.getTable() +	" WHERE "+IndexConstants.IndexMapRenderObject.ID+  //$NON-NLS-1$//$NON-NLS-2$
										  " IN (SELECT id FROM "+IndexConstants.indexMapLocationsTable2 +   //$NON-NLS-1$
										  " WHERE ? <  maxLat AND ? > minLat AND maxLon > ? AND minLon  < ?)"; //$NON-NLS-1$
	
	
	private static String loadMapQuery3 = "SELECT "+IndexConstants.IndexMapRenderObject.ID +", " + IndexConstants.IndexMapRenderObject.NODES +", " +  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										  IndexConstants.IndexMapRenderObject.NAME + ", " + IndexConstants.IndexMapRenderObject.TYPE + //$NON-NLS-1$
										  " FROM " + IndexConstants.IndexMapRenderObject.getTable() +	" WHERE "+IndexConstants.IndexMapRenderObject.ID +  //$NON-NLS-1$//$NON-NLS-2$
										  " IN (SELECT id FROM "+IndexConstants.indexMapLocationsTable3 +   //$NON-NLS-1$
										  " WHERE ? <  maxLat AND ? > minLat AND maxLon > ? AND minLon  < ?)"; //$NON-NLS-1$
	
	public synchronized void loadMap(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, float rotate) {
		boolean inside = insideBox(topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom);
		cRotate = rotate < 0 ? rotate + 360 : rotate;
		if (!inside) {
			// that usable for portrait view
			cBottomLatitude = bottomLatitude - (topLatitude - bottomLatitude) / 2;
			cTopLatitude = topLatitude + (topLatitude - bottomLatitude) / 2;
			cLeftLongitude = leftLongitude - (rightLongitude - leftLongitude);
			cRightLongitude = rightLongitude + (rightLongitude - leftLongitude);
			cZoom = zoom;

			log.info(String.format("BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", //$NON-NLS-1$
					cBottomLatitude, cTopLatitude, cLeftLongitude, cRightLongitude, cZoom)); 

			long now = System.currentTimeMillis();

			PreparedStatement statement = null;
			if(zoom >= 15){
				statement = pStatement;
			} else if(zoom >= 10){
				statement = pStatement2;
			} else if(zoom >= 6) {
				statement = pStatement3;
			}
				
			if (statement == null || conn == null) {
				cObjects = new ArrayList<MapRenderObject>();
				// keep old results
				return;
			}
			try {
				statement.setDouble(1, cBottomLatitude);
				statement.setDouble(2, cTopLatitude);
				statement.setDouble(3, cLeftLongitude);
				statement.setDouble(4, cRightLongitude);
				ResultSet result = statement.executeQuery();

				List<MapRenderObject> local = new ArrayList<MapRenderObject>();
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
					log.info(String
							.format("Search has been done in %s ms. %s results were found.", System.currentTimeMillis() - now, count)); //$NON-NLS-1$
				} finally {
					result.close();
				}
			} catch (java.sql.SQLException e) {
				log.debug("Search failed", e); //$NON-NLS-1$
			}
		}
		
		// create new instance to distinguish that cache was changed
		RectF newLoc = new RectF((float)cLeftLongitude, (float)cTopLatitude, (float)cRightLongitude, (float)cBottomLatitude);
		Bitmap bmp = renderer.generateNewBitmap(newLoc, cObjects, cZoom, cRotate);
		Bitmap oldBmp = this.bmp;
		this.bmp = bmp;
		cachedWaysLoc = newLoc;
		if(oldBmp != null){
			oldBmp.recycle();
		}
		
	}

	
	public Bitmap getBitmap() {
		return bmp;
	}
	
	
	public synchronized void clearCache() {
		cObjects.clear();
		cBottomLatitude = cLeftLongitude = cRightLongitude = cTopLatitude = cRotate = cZoom = 0;
		if(bmp != null){
			bmp.recycle();
			bmp = null;
		}
		cachedWaysLoc = new RectF();
	}

}
