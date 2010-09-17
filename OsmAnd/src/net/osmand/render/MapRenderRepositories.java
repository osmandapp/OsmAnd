package net.osmand.render;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.data.index.IndexConstants;
import net.osmand.osm.MapRenderObject;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.FloatMath;

public class MapRenderRepositories {
	
	private final static Log log = LogUtil.getLog(MapRenderRepositories.class);
	private final Context context;
	private Map<String, Connection> files = new LinkedHashMap<String, Connection>();
	private Map<Connection, RectF> connections = new LinkedHashMap<Connection, RectF>();
	private Map<Connection, PreparedStatement> pZoom0 = new LinkedHashMap<Connection, PreparedStatement>();
	private Map<Connection, PreparedStatement> pZoom1 = new LinkedHashMap<Connection, PreparedStatement>();
	private Map<Connection, PreparedStatement> pZoom2 = new LinkedHashMap<Connection, PreparedStatement>();
	private OsmandRenderer renderer;
	
	private double cTopY;
	private double cBottomY;
	private double cLeftX;
	private double cRightX;
	private int cZoom;
	private float cRotate;
	
	// cached objects in order to rotate without 
	private List<MapRenderObject> cObjects = new LinkedList<MapRenderObject>();
	private RectF cachedWaysLoc = new RectF();
	private float cachedRotate = 0;
	private Bitmap bmp;
	
	
	
	public MapRenderRepositories(Context context){
		this.context = context;
		this.renderer = new OsmandRenderer(context);
	}
	
	public Context getContext() {
		return context;
	}
	
	private RectF getBoundsForIndex(Statement stat, String tableName) throws SQLException{
		ResultSet rs = stat.executeQuery("SELECT MIN(minLon), MAX(maxLat), MAX(maxLon), MIN(minLat) FROM " + IndexConstants.indexMapLocationsTable); //$NON-NLS-1$
		RectF bounds = new RectF();
		if(rs.next()){
			bounds.set(rs.getFloat(1), rs.getFloat(2), rs.getFloat(3), rs.getFloat(4));
		}
		rs.close();
		return bounds;
	}
	
	
	public boolean initializeNewResource(final IProgress progress, File file) {
		long start = System.currentTimeMillis();
		Connection conn = null;
		if(files.containsKey(file.getAbsolutePath())){
			closeConnection(files.get(file.getAbsolutePath()), file.getAbsolutePath());
		}
		try {
			try {
				Class.forName("org.sqlite.JDBC"); //$NON-NLS-1$
			} catch (Exception e) {
				log.error("Could not load driver", e); //$NON-NLS-1$
				return false;
			}
			conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath()); //$NON-NLS-1$
			PreparedStatement pStatement = conn.prepareStatement(loadMapQuery);
			PreparedStatement pStatement2 = conn.prepareStatement(loadMapQuery2);
			PreparedStatement pStatement3 = conn.prepareStatement(loadMapQuery3);
			Statement stat = conn.createStatement();
			ResultSet rs = stat.executeQuery("PRAGMA user_version"); //$NON-NLS-1$
			int v = rs.getInt(1);
			rs.close();
			if(v != IndexConstants.MAP_TABLE_VERSION){
				return false;
			}
			RectF bounds = foundBounds(stat);
			
			
			stat.close();
			connections.put(conn, bounds);
			files.put(file.getAbsolutePath(), conn);
			pZoom0.put(conn, pStatement);
			pZoom1.put(conn, pStatement2);
			pZoom2.put(conn, pStatement3);
			
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

	private RectF foundBounds(Statement stat) throws SQLException {
		String metaTable = "loc_meta_locations"; //$NON-NLS-1$
		ResultSet rs = stat.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='"+metaTable+"'");  //$NON-NLS-1$//$NON-NLS-2$
		boolean dbExist = rs.next();
		rs.close();
		boolean found = false;
		boolean write = true;
		RectF bounds = new RectF();
		if(dbExist){
			rs = stat.executeQuery("SELECT MAX_LAT, MIN_LON, MIN_LAT, MAX_LON  FROM " +metaTable); //$NON-NLS-1$
			if(rs.next()){
				bounds.set(rs.getFloat(2), rs.getFloat(1), rs.getFloat(4), rs.getFloat(3));
				found = true;
			} else {
				found = false;
			}
			rs.close();
		} else {
			try {
				stat.execute("CREATE TABLE " + metaTable + " (MAX_LAT DOUBLE, MIN_LON DOUBLE, MIN_LAT DOUBLE, MAX_LON DOUBLE)"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (RuntimeException e) {
				// case when database is in readonly mode
				write = false;
			}
		}
		
		if (!found) {
			bounds = getBoundsForIndex(stat, IndexConstants.indexMapLocationsTable);
			if(bounds.left == bounds.right || bounds.bottom == bounds.top){
				bounds = getBoundsForIndex(stat, IndexConstants.indexMapLocationsTable2);
				if(bounds.left == bounds.right || bounds.bottom == bounds.top){
					bounds = getBoundsForIndex(stat, IndexConstants.indexMapLocationsTable3);
				}
			}
			if (write) {
				stat.execute("INSERT INTO " + metaTable + " VALUES ("+Double.toString(bounds.top)+ ", "+Double.toString(bounds.left)+ ", " +  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						Double.toString(bounds.bottom)+ ", "+Double.toString(bounds.right)+ ")");  //$NON-NLS-1$//$NON-NLS-2$
			}
		}
		
		return bounds;
	}
	
	
	// if cache was changed different instance will be returned
	public RectF getCachedWaysLoc() {
		return cachedWaysLoc;
	}
	
	public float getCachedRotate() {
		return cachedRotate;
	}
	
	protected void closeConnection(Connection c, String file){
		files.remove(c);
		connections.remove(c);
		pZoom0.remove(c);
		pZoom1.remove(c);
		pZoom2.remove(c);
		try {
			c.close();
		} catch (java.sql.SQLException e) {
		}
	}
	
	public void clearAllResources(){
		clearCache();
		for(String f : files.keySet()){
			closeConnection(files.get(f), f);
		}
	}
	
	
	public boolean updateMapIsNeeded(RectF tileRect, int zoom, float rotate){
		if (connections.isEmpty()) {
			return false;
		}
		boolean inside = insideBox(tileRect.top, tileRect.left, tileRect.bottom,  tileRect.right, zoom);
		if(rotate < 0){
			rotate += 360;
		} 

		return !inside || Math.abs(rotate - cRotate) > 30;
		
	}

	private boolean insideBox(double topY, double leftX, double bottomY, double rightX, int zoom) {
		boolean inside = cZoom == zoom && cTopY <= topY && cLeftX <= leftX && cRightX >= rightX
				&& cBottomY >= bottomY;
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
	
	public synchronized void loadMap(RectF tileRect, RectF boundsTileRect, int zoom, float rotate) {
		// currently doesn't work properly (every rotate bounds will be outside)
		boolean inside = insideBox(boundsTileRect.top, boundsTileRect.left, boundsTileRect.bottom, boundsTileRect.right, zoom);
		cRotate = rotate < 0 ? rotate + 360 : rotate;
		if (!inside) {
			cTopY = boundsTileRect.top;
			cLeftX = boundsTileRect.left;
			cRightX = boundsTileRect.right;
			cBottomY = boundsTileRect.bottom;
			double cBottomLatitude = MapUtils.getLatitudeFromTile(zoom, cBottomY);
			double cTopLatitude = MapUtils.getLatitudeFromTile(zoom, cTopY);
			double cLeftLongitude = MapUtils.getLongitudeFromTile(zoom, cLeftX);
			double cRightLongitude = MapUtils.getLongitudeFromTile(zoom, cRightX);
			cZoom = zoom;

			log.info(String.format("BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", //$NON-NLS-1$
					cBottomLatitude, cTopLatitude, cLeftLongitude, cRightLongitude, cZoom)); 

			long now = System.currentTimeMillis();


				
			if (connections.isEmpty()) {
				cObjects = new ArrayList<MapRenderObject>();
				// keep old results
				return;
			}
			try {
				int count = 0;
				List<MapRenderObject> local = new ArrayList<MapRenderObject>();
				Set<Long> ids = new LinkedHashSet<Long>();
				for (Connection c : connections.keySet()) {
					RectF r = connections.get(c);
					boolean intersects = r.top >= cBottomLatitude  && r.left <= cRightLongitude && r.right >= cLeftLongitude &&
											r.bottom <= cTopLatitude;
					if(!intersects){
						continue;
					}
					
					PreparedStatement statement = null;
					if (zoom >= 15) {
						statement = pZoom0.get(c);
					} else if (zoom >= 10) {
						statement = pZoom1.get(c);
					} else if (zoom >= 6) {
						statement = pZoom2.get(c);
					}
					statement.setDouble(1, cBottomLatitude);
					statement.setDouble(2, cTopLatitude);
					statement.setDouble(3, cLeftLongitude);
					statement.setDouble(4, cRightLongitude);
					ResultSet result = statement.executeQuery();


					try {
						while (result.next()) {
							long id = result.getLong(1);
							if(ids.contains(id)){
								// do not add object twice
								continue;
							}
							ids.add(id);
							MapRenderObject obj = new MapRenderObject(id);
							obj.setData(result.getBytes(2));
							obj.setName(result.getString(3));
							obj.setType(result.getInt(4));
							count++;
							local.add(obj);
						}

					} finally {
						result.close();
					}
				}
				cObjects = local;
				log.info(String
						.format("Search has been done in %s ms. %s results were found.", System.currentTimeMillis() - now, count)); //$NON-NLS-1$
			} catch (java.sql.SQLException e) {
				log.debug("Search failed", e); //$NON-NLS-1$
			}
		}
		
		// create new instance to distinguish that cache was changed
		RectF newLoc = new RectF((float)MapUtils.getLongitudeFromTile(zoom, tileRect.left), (float)MapUtils.getLatitudeFromTile(zoom, tileRect.top),
				(float)MapUtils.getLongitudeFromTile(zoom, tileRect.right), (float)MapUtils.getLatitudeFromTile(zoom, tileRect.bottom));
		
		int width = (int) calcDiffPixelX(cRotate, tileRect.right - tileRect.left, tileRect.bottom - tileRect.top);
		int height = (int) calcDiffPixelY(cRotate, tileRect.right - tileRect.left, tileRect.bottom - tileRect.top);
		Bitmap bmp = renderer.generateNewBitmap(width, height, tileRect.left, tileRect.top, cObjects, cZoom, cRotate);
		Bitmap oldBmp = this.bmp;
		this.bmp = bmp;
		cachedWaysLoc = newLoc;
		cachedRotate = cRotate;
		if(oldBmp != null){
			oldBmp.recycle();
		}
		
	}

	public Bitmap getBitmap() {
		return bmp;
	}
	
	public float calcDiffPixelX(float rotate, float dTileX, float dTileY){
		float rad = (float) Math.toRadians(rotate);
		return (FloatMath.cos(rad) * dTileX - FloatMath.sin(rad) * dTileY) * OsmandRenderer.TILE_SIZE;
	}
	
	public float calcDiffPixelY(float rotate, float dTileX, float dTileY){
		float rad = (float) Math.toRadians(rotate);
		return (FloatMath.sin(rad) * dTileX + FloatMath.cos(rad) * dTileY) * OsmandRenderer.TILE_SIZE;
	}
	
	public synchronized void clearCache() {
		cObjects.clear();
		cBottomY = cLeftX = cRightX = cTopY = cRotate = cZoom = 0;
		if(bmp != null){
			bmp.recycle();
			bmp = null;
		}
		cachedWaysLoc = new RectF();
	}

}
