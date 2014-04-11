package net.osmand.plus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.apache.http.client.protocol.ClientContext;

import android.database.sqlite.SQLiteDiskIOException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


public class SQLiteTileSource implements ITileSource {

	
	public static final String EXT = IndexConstants.SQLITE_EXT;
	private static final Log log = PlatformUtil.getLog(SQLiteTileSource.class);
	
	private ITileSource base;
	private String urlTemplate = null;
	private String name;
	private SQLiteConnection db;
	private final File file;
	private int minZoom = 1;
	private int maxZoom = 17; 
	private boolean inversiveZoom = true; // BigPlanet
	private boolean timeSupported = false;
	private int expirationTimeMillis = -1; // never

	static final int tileSize = 256;
	private OsmandApplication ctx;
	private boolean onlyReadonlyAvailable = false;
	
	
	
	public SQLiteTileSource(OsmandApplication ctx, File f, List<TileSourceTemplate> toFindUrl){
		this.ctx = ctx;
		this.file = f;
		if (f != null) {
			int i = f.getName().lastIndexOf('.');
			name = f.getName().substring(0, i);
			i = name.lastIndexOf('.');
			if (i > 0) {
				String sourceName = name.substring(i + 1);
				for (TileSourceTemplate is : toFindUrl) {
					if (is.getName().equalsIgnoreCase(sourceName)) {
						base = is;
						urlTemplate = is.getUrlTemplate();
						break;
					}
				}
			}
		}
		
	}
	
	@Override
	public int getBitDensity() {
		return base != null ? base.getBitDensity() : 16;
	}

	@Override
	public int getMaximumZoomSupported() {
		getDatabase();
		return base != null ? base.getMaximumZoomSupported() : maxZoom;
	}

	@Override
	public int getMinimumZoomSupported() {
		getDatabase();
		return base != null ? base.getMinimumZoomSupported() : minZoom;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getTileFormat() {
		return base != null ? base.getTileFormat() : ".png"; //$NON-NLS-1$
	}

	@Override
	public int getTileSize() {
		return base != null ? base.getTileSize() : tileSize;
	}

	@Override
	public String getUrlToLoad(int x, int y, int zoom) {
		if (zoom > maxZoom)
			return null;
		SQLiteConnection db = getDatabase();
		if(db == null || db.isReadOnly() || urlTemplate == null){
			return null;
		}
		return MessageFormat.format(urlTemplate, zoom+"", x+"", y+"");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SQLiteTileSource other = (SQLiteTileSource) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	protected SQLiteConnection getDatabase(){
		if((db == null || db.isClosed()) && file.exists() ){
			try {
				onlyReadonlyAvailable = false;
				db = ctx.getSQLiteAPI().openByAbsolutePath(file.getAbsolutePath(), false);
			} catch(RuntimeException e) {
				onlyReadonlyAvailable = true;
				db = ctx.getSQLiteAPI().openByAbsolutePath(file.getAbsolutePath(), true);
			}
			try {
				SQLiteCursor cursor = db.rawQuery("SELECT * FROM info", null);
				if(cursor.moveToFirst()) {
					String[] columnNames = cursor.getColumnNames();
					List<String> list = Arrays.asList(columnNames);
					int url = list.indexOf("url");
					if(url != -1) {
						String template = cursor.getString(url);
						if(!Algorithms.isEmpty(template)){
							urlTemplate = template;
						}
					}
					int tnumbering = list.indexOf("tilenumbering");
					if(tnumbering != -1) {
						inversiveZoom = "BigPlanet".equalsIgnoreCase(cursor.getString(tnumbering));
					} else {
						inversiveZoom = true;
						addInfoColumn("tilenumbering", "BigPlanet");
					}
					int timecolumn = list.indexOf("timecolumn");
					if (timecolumn != -1) {
						timeSupported = "yes".equalsIgnoreCase(cursor.getString(timecolumn));
					} else {
						timeSupported = hasTimeColumn();
						addInfoColumn("timecolumn", timeSupported?"yes" : "no");
					}
					int expireminutes = list.indexOf("expireminutes");
					this.expirationTimeMillis = -1;
					if(expireminutes != -1) {
						int minutes = (int) cursor.getInt(expireminutes);
						if(minutes > 0) {
							this.expirationTimeMillis = minutes * 60 * 1000;
						}
					} else {
						addInfoColumn("expireminutes", "0");
					}
					//boolean inversiveInfoZoom = tnumbering != -1 && "BigPlanet".equals(cursor.getString(tnumbering));
					boolean inversiveInfoZoom = inversiveZoom;
					int mnz = list.indexOf("minzoom");
					if(mnz != -1) {
						minZoom = (int) cursor.getInt(mnz);
					}
					int mxz = list.indexOf("maxzoom");
					if(mxz != -1) {
						maxZoom = (int) cursor.getInt(mxz);
					}
					if(inversiveInfoZoom) {
						mnz = minZoom;
						minZoom = 17 - maxZoom;
						maxZoom = 17 - mnz;
					}
				}
				cursor.close();
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
		return db;
	}

	private void addInfoColumn(String columnName, String value) {
		if(!onlyReadonlyAvailable) {
			db.execSQL("alter table info add column "+columnName+" TEXT");
			db.execSQL("update info set "+columnName+" = '"+value+"'");
		}
	}

	private boolean hasTimeColumn() {
		SQLiteCursor cursor;
		cursor = db.rawQuery("SELECT * FROM tiles", null);
		cursor.moveToFirst();
		List<String> cols = Arrays.asList(cursor.getColumnNames());
		boolean timeSupported = cols.contains("time");
		cursor.close();
		return timeSupported;
	}
	
	public boolean exists(int x, int y, int zoom) {
		SQLiteConnection db = getDatabase();
		if (db == null) {
			return false;
		}
		long time = System.currentTimeMillis();
		try {
			int z = getFileZoom(zoom);
			SQLiteCursor cursor = db.rawQuery(
					"SELECT 1 FROM tiles WHERE x = ? AND y = ? AND z = ?", new String[] { x + "", y + "", z + "" }); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
			try {
				boolean e = cursor.moveToFirst();
				cursor.close();

				return e;
			} catch (SQLiteDiskIOException e) {
				return false;
			}
		} finally {
			if (log.isDebugEnabled()) {
				log.debug("Checking tile existance x = " + x + " y = " + y + " z = " + zoom + " for " + (System.currentTimeMillis() - time)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
	}
	
	public boolean isLocked() {
		SQLiteConnection db = getDatabase();
		if(db == null){
			return false;
		}
		return db.isDbLockedByOtherThreads();
	}

	public Bitmap getImage(int x, int y, int zoom, long[] timeHolder) {
		SQLiteConnection db = getDatabase();
		if(db == null){
			return null;
		}
		long ts = System.currentTimeMillis();
		try {
			if (zoom <= maxZoom) {
				// return the normal tile if exists
				String[] params = new String[] { x + "", y + "", getFileZoom(zoom) + "" };
				boolean queryTime = timeHolder != null && timeHolder.length > 0 && timeSupported;
				SQLiteCursor cursor = db.rawQuery("SELECT image " +(queryTime?", time":"")+"  FROM tiles WHERE x = ? AND y = ? AND z = ?",
						params); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				byte[] blob = null;
				if (cursor.moveToFirst()) {
					blob = cursor.getBlob(0);
					if(queryTime) {
						timeHolder[0] = cursor.getLong(1);
					}
				}
				cursor.close();
				if (blob != null) {
					Bitmap bmp = null;
					bmp = BitmapFactory.decodeByteArray(blob, 0, blob.length);
					if(bmp == null) {
						// broken image delete it
						db.execSQL("DELETE FROM tiles WHERE x = ? AND y = ? AND z = ?", params); 
					}
					return bmp;
				}
			}
			return null;
		} finally {
			if(log.isDebugEnabled()) {
				log.debug("Load tile " + x + "/" + y + "/" + zoom + " for " + (System.currentTimeMillis() - ts)
					+ " ms ");
			}
		}
	}
	 
	public ITileSource getBase() {
		return base;
	}
	
	public QuadRect getRectBoundary(int coordinatesZoom, int minZ){
		SQLiteConnection db = getDatabase();
		if(db == null || coordinatesZoom > 25 ){
			return null;
		}
		SQLiteCursor q ;
		if (inversiveZoom) {
			int minZoom = (17 - minZ) + 1;
			// 17 - z = zoom, x << (25 - zoom) = 25th x tile = 8 + z,
			q = db.rawQuery("SELECT max(x << (8+z)), min(x << (8+z)), max(y << (8+z)), min(y << (8+z))" +
					" from tiles where z < "
					+ minZoom, new String[0]);
		} else {
			q = db.rawQuery("SELECT max(x << (25-z)), min(x << (25-z)), max(y << (25-z)), min(y << (25-z))"
					+ " from tiles where z > " + minZ,
					new String[0]);
		}
		q.moveToFirst();
		int right = (int) (q.getInt(0) >> (25 - coordinatesZoom));
		int left = (int) (q.getInt(1) >> (25 - coordinatesZoom));
		int top = (int) (q.getInt(3) >> (25 - coordinatesZoom));
		int bottom  = (int) (q.getInt(2) >> (25 - coordinatesZoom));
		return new QuadRect(left, top, right, bottom);
		
		
	}

	public void deleteImage(int x, int y, int zoom) {
		SQLiteConnection db = getDatabase();
		if(db == null || db.isReadOnly()){
			return;
		}
		db.execSQL("DELETE FROM tiles WHERE x = ? AND y = ? AND z = ?", new String[] {x+"", y+"", getFileZoom(zoom)+""});    //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
	}

	private static final int BUF_SIZE = 1024;
	
	/**
	 * Makes method synchronized to give a little more time for get methods and 
	 * let all writing attempts to wait outside of this method   
	 */
	public synchronized void insertImage(int x, int y, int zoom, File fileToSave) throws IOException {
		SQLiteConnection db = getDatabase();
		if (db == null || db.isReadOnly() || onlyReadonlyAvailable) {
			return;
		}
		if (exists(x, y, zoom)) {
			return;
		}
		ByteBuffer buf = ByteBuffer.allocate((int) fileToSave.length());
		FileInputStream is = new FileInputStream(fileToSave);
		int i = 0;
		byte[] b = new byte[BUF_SIZE];
		while ((i = is.read(b, 0, BUF_SIZE)) > -1) {
			buf.put(b, 0, i);
		}

		
		String query = timeSupported ? "INSERT INTO tiles(x,y,z,s,image,time) VALUES(?, ?, ?, ?, ?, ?)"
				: "INSERT INTO tiles(x,y,z,s,image) VALUES(?, ?, ?, ?, ?)";
		net.osmand.plus.api.SQLiteAPI.SQLiteStatement statement = db.compileStatement(query); //$NON-NLS-1$
		statement.bindLong(1, x);
		statement.bindLong(2, y);
		statement.bindLong(3, getFileZoom(zoom));
		statement.bindLong(4, 0);
		statement.bindBlob(5, buf.array());
		if (timeSupported) {
			statement.bindLong(6, System.currentTimeMillis());
		}
		statement.execute();
		statement.close();
		is.close();

	}

	private int getFileZoom(int zoom) {
		return inversiveZoom ? 17 - zoom : zoom;
	}
	
	public void closeDB(){
		if(db != null){
			db.close();
			db = null;
		}
	}

	@Override
	public boolean couldBeDownloadedFromInternet() {
		if(getDatabase() == null || getDatabase().isReadOnly() || onlyReadonlyAvailable){
			return false;
		}
		return urlTemplate != null;
	}

	@Override
	public boolean isEllipticYTile() {
		return false;
	}

	public int getExpirationTimeMinutes() {
		if(expirationTimeMillis  < 0) {
			return -1;
		}
		return expirationTimeMillis / (60  * 1000);
	}
	
	public int getExpirationTimeMillis() {
		return expirationTimeMillis;
	}
	

}


