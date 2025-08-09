package net.osmand.plus.resources;

import static net.osmand.IndexConstants.SQLITE_EXT;
import static net.osmand.IndexConstants.TILES_INDEX_DIR;

import android.database.SQLException;
import android.database.sqlite.SQLiteDiskIOException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.QuadRect;
import net.osmand.map.ITileSource;
import net.osmand.map.ParameterType;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class SQLiteTileSource implements ITileSource {

	public static final String EXT = IndexConstants.SQLITE_EXT;
	private static final Log LOG = PlatformUtil.getLog(SQLiteTileSource.class);

	private static final String MIN_ZOOM = "minzoom";
	private static final String MAX_ZOOM = "maxzoom";
	private static final String URL = "url";
	private static final String RANDOMS = "randoms";
	private static final String ELLIPSOID = "ellipsoid";
	private static final String INVERTED_Y = "inverted_y";
	private static final String REFERER = "referer";
	private static final String USER_AGENT = "useragent";
	private static final String TIME_COLUMN = "timecolumn";
	private static final String EXPIRE_MINUTES = "expireminutes";
	private static final String RULE = "rule";
	private static final String TILENUMBERING = "tilenumbering";
	private static final String BIG_PLANET_TILE_NUMBERING = "BigPlanet";
	private static final String TILESIZE = "tilesize";
	private static final String TITLE = "title";

	private final OsmandApplication app;

	private ITileSource base;
	private String urlTemplate;
	private String fileName;
	private SQLiteConnection db;
	private File file;
	private String title;
	private int minZoom = 1;
	private int maxZoom = 17;
	private boolean inversiveZoom = true; // BigPlanet
	private boolean timeSupported;
	private long expirationTimeMillis = -1; // never
	private boolean isEllipsoid;
	private boolean invertedY;
	private String randoms;
	private String[] randomsArray;
	private String rule;
	private String referer;
	private String userAgent;

	private int tileSize = 256;
	private boolean tileSizeSpecified;
	private boolean onlyReadonlyAvailable;

	public SQLiteTileSource(@NonNull OsmandApplication app, @Nullable File file, @NonNull List<TileSourceTemplate> toFindUrl) {
		this.app = app;
		this.file = file;
		if (file != null) {
			int i = file.getName().lastIndexOf('.');
			fileName = file.getName().substring(0, i);
			i = fileName.lastIndexOf('.');
			if (i > 0) {
				String sourceName = fileName.substring(i + 1);
				setTileSourceTemplate(sourceName, toFindUrl);
			} else {
				setTileSourceTemplate(fileName, toFindUrl);
			}
		}
	}

	private void setTileSourceTemplate(@NonNull String sourceName, @NonNull List<TileSourceTemplate> toFindUrl) {
		for (TileSourceTemplate is : toFindUrl) {
			if (is.getName().equalsIgnoreCase(sourceName)) {
				base = is;
				title = is.getName();
				urlTemplate = is.getUrlTemplate();
				expirationTimeMillis = is.getExpirationTimeMillis();
				minZoom = is.getMinimumZoomSupported();
				maxZoom = is.getMaximumZoomSupported();
				inversiveZoom = is.getInversiveZoom();
				break;
			}
		}
	}

	public SQLiteTileSource(@NonNull OsmandApplication app, String name, int minZoom, int maxZoom, String urlTemplate,
	                        String randoms, boolean isEllipsoid, boolean invertedY, String referer, String userAgent,
	                        boolean timeSupported, long expirationTimeMillis, boolean inversiveZoom, String rule) {
		this.app = app;
		this.title = name;
		this.fileName = name;
		this.urlTemplate = urlTemplate;
		this.maxZoom = maxZoom;
		this.minZoom = minZoom;
		this.isEllipsoid = isEllipsoid;
		this.expirationTimeMillis = expirationTimeMillis;
		this.randoms = randoms;
		this.referer = referer;
		this.userAgent = userAgent;
		this.rule = rule;
		this.invertedY = invertedY;
		this.timeSupported = timeSupported;
		this.inversiveZoom = inversiveZoom;
	}

	public SQLiteTileSource(@NonNull SQLiteTileSource tileSource, @NonNull String name, @NonNull OsmandApplication app) {
		this.app = app;
		this.title = name;
		this.fileName = name;
		this.urlTemplate = tileSource.getUrlTemplate();
		this.maxZoom = tileSource.getMaximumZoomSupported();
		this.minZoom = tileSource.getMinimumZoomSupported();
		this.isEllipsoid = tileSource.isEllipticYTile();
		this.expirationTimeMillis = tileSource.getExpirationTimeMillis();
		this.randoms = tileSource.getRandoms();
		this.referer = tileSource.getReferer();
		this.userAgent = tileSource.getUserAgent();
		this.invertedY = tileSource.isInvertedYTile();
		this.timeSupported = tileSource.isTimeSupported();
		this.inversiveZoom = tileSource.getInversiveZoom();
	}

	public void createDataBase() {
		SQLiteConnection db = app.getSQLiteAPI().getOrCreateDatabase(
				app.getAppPath(TILES_INDEX_DIR).getAbsolutePath() + "/" + fileName + SQLITE_EXT, true);

		db.execSQL("CREATE TABLE IF NOT EXISTS tiles (x int, y int, z int, s int, image blob, time long, PRIMARY KEY (x,y,z,s))");
		db.execSQL("CREATE INDEX IF NOT EXISTS IND on tiles (x,y,z,s)");
		db.execSQL("CREATE TABLE IF NOT EXISTS info(tilenumbering,minzoom,maxzoom)");
		db.execSQL("INSERT INTO info (tilenumbering,minzoom,maxzoom) VALUES ('simple','" + minZoom + "','" + maxZoom + "');");

		addInfoColumn(db, TITLE, title);
		addInfoColumn(db, URL, urlTemplate);
		addInfoColumn(db, RANDOMS, randoms);
		addInfoColumn(db, ELLIPSOID, isEllipsoid ? "1" : "0");
		addInfoColumn(db, INVERTED_Y, invertedY ? "1" : "0");
		addInfoColumn(db, REFERER, referer);
		addInfoColumn(db, USER_AGENT, userAgent);
		addInfoColumn(db, TIME_COLUMN, timeSupported ? "yes" : "no");
		addInfoColumn(db, EXPIRE_MINUTES, String.valueOf(getExpirationTimeMinutes()));

		db.close();
	}

	@Override
	public int getBitDensity() {
		return base != null ? base.getBitDensity() : 16;
	}

	@Override
	public int getMaximumZoomSupported() {
		return base != null ? base.getMaximumZoomSupported() : maxZoom;
	}

	@Override
	public int getMinimumZoomSupported() {
		return base != null ? base.getMinimumZoomSupported() : minZoom;
	}

	@Override
	public String getName() {
		return fileName;
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
		if (db == null || db.isReadOnly() || urlTemplate == null) {
			return null;
		}
		if (invertedY) {
			y = (1 << zoom) - 1 - y;
		}
		return TileSourceTemplate.buildUrlToLoad(urlTemplate, randomsArray, x, y, zoom, null);
	}

	@Override
	public String getUrlTemplate() {
		if (this.urlTemplate != null) {
			return this.urlTemplate;
		} else {
			SQLiteConnection db = getDatabase();
			if (db == null || urlTemplate == null) {
				return null;
			} else {
				return this.urlTemplate;
			}
		}
	}

	@Nullable
	public String getTitle() {
		if (title == null) {
			getDatabase();
		}
		if (title != null) {
			return title;
		}
		if (fileName != null) {
			int index = fileName.lastIndexOf('.');
			return index > 0 ? fileName.substring(0, index) : fileName;
		}
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
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
		return Algorithms.stringsEqual(fileName, other.fileName);
	}

	public void initDatabaseIfNeeded() {
		getDatabase();
	}

	protected synchronized SQLiteConnection getDatabase() {
		if ((db == null || db.isClosed()) && file.exists()) {
			LOG.debug("Open " + file.getAbsolutePath());
			db = openDatabase(false);
			if (db == null) {
				db = openDatabase(true);
			}
			try {
				SQLiteCursor cursor = db.rawQuery("SELECT * FROM info", null);
				if (cursor.moveToFirst()) {
					String[] columnNames = cursor.getColumnNames();
					List<String> list = Arrays.asList(columnNames);
					int url = list.indexOf(URL);
					if (url != -1) {
						String template = cursor.getString(url);
						if (!Algorithms.isEmpty(template)) {
							urlTemplate = TileSourceTemplate.normalizeUrl(template);
						}
					}
					int titleId = list.indexOf(TITLE);
					if (titleId != -1) {
						title = cursor.getString(titleId);
					}
					int ruleId = list.indexOf(RULE);
					if (ruleId != -1) {
						rule = cursor.getString(ruleId);
					}
					int refererId = list.indexOf(REFERER);
					if (refererId != -1) {
						referer = cursor.getString(refererId);
					}
					int userAgentId = list.indexOf(USER_AGENT);
					if(userAgentId != -1) {
						userAgent = cursor.getString(userAgentId);
					}
					int tnumbering = list.indexOf(TILENUMBERING);
					if(tnumbering != -1) {
						inversiveZoom = BIG_PLANET_TILE_NUMBERING.equalsIgnoreCase(cursor.getString(tnumbering));
					} else {
						inversiveZoom = true;
						addInfoColumn(db, TILENUMBERING, BIG_PLANET_TILE_NUMBERING);
					}
					int timecolumn = list.indexOf(TIME_COLUMN);
					if (timecolumn != -1) {
						timeSupported = "yes".equalsIgnoreCase(cursor.getString(timecolumn));
					} else {
						timeSupported = hasTimeColumn(db);
						addInfoColumn(db, TIME_COLUMN, timeSupported ? "yes" : "no");
					}
					int expireminutes = list.indexOf(EXPIRE_MINUTES);
					this.expirationTimeMillis = -1;
					if(expireminutes != -1) {
						int minutes = cursor.getInt(expireminutes);
						if(minutes > 0) {
							this.expirationTimeMillis = minutes * 60 * 1000l;
						}
					} else {
						addInfoColumn(db, EXPIRE_MINUTES, "0");
					}
					int tsColumn = list.indexOf(TILESIZE);
					this.tileSizeSpecified = tsColumn != -1;
					if(tileSizeSpecified) {
						this.tileSize = cursor.getInt(tsColumn);
					}
					int ellipsoid = list.indexOf(ELLIPSOID);
					if(ellipsoid != -1) {
						int set = cursor.getInt(ellipsoid);
						if(set == 1){
							this.isEllipsoid = true;
						}
					}
					int invertedY = list.indexOf(INVERTED_Y);
					if(invertedY != -1) {
						int set = cursor.getInt(invertedY);
						if(set == 1){
							this.invertedY = true;
						}
					}
					int randomsId = list.indexOf(RANDOMS);
					if(randomsId != -1) {
						this.randoms = cursor.getString(randomsId);
						this.randomsArray = TileSourceTemplate.buildRandomsArray(this.randoms);
					}
					//boolean inversiveInfoZoom = tnumbering != -1 && "BigPlanet".equals(cursor.getString(tnumbering));
					boolean inversiveInfoZoom = inversiveZoom;
					int mnz = list.indexOf(MIN_ZOOM);
					if(mnz != -1) {
						minZoom = cursor.getInt(mnz);
					}
					int mxz = list.indexOf(MAX_ZOOM);
					if(mxz != -1) {
						maxZoom = cursor.getInt(mxz);
					}
					if(inversiveInfoZoom) {
						mnz = minZoom;
						minZoom = 17 - maxZoom;
						maxZoom = 17 - mnz;
					}
				}
				cursor.close();
			} catch (RuntimeException e) {
				LOG.error(e);
			}
		}
		return db;
	}

	private SQLiteConnection openDatabase(boolean readOnly) {
		try {
			onlyReadonlyAvailable = readOnly;
			return app.getSQLiteAPI().openByAbsolutePath(file.getAbsolutePath(), readOnly);
		} catch (RuntimeException e) {
			LOG.error(e);
		}
		return null;
	}

	public void updateFromTileSourceTemplate(TileSourceTemplate r) {
		boolean openedBefore = isDbOpened();
		SQLiteConnection db = getDatabase();
		boolean changed = false;
		if (!onlyReadonlyAvailable && db != null) {
			int maxZoom = r.getMaximumZoomSupported();
			int minZoom = r.getMinimumZoomSupported();
			if (inversiveZoom) {
				int mnz = minZoom;
				minZoom = 17 - maxZoom;
				maxZoom = 17 - mnz;
			}
			if (getUrlTemplate() != null && !Algorithms.stringsEqual(r.getUrlTemplate(), getUrlTemplate())) {
				db.execSQL("update info set " + URL + " = '" + r.getUrlTemplate() + "'");
				changed = true;
			}
			if (!Algorithms.stringsEqual(r.getName(), getTitle())) {
				db.execSQL("update info set " + TITLE + " = '" + r.getName() + "'");
				changed = true;
			}
			if (minZoom != this.minZoom) {
				db.execSQL("update info set " + MIN_ZOOM + " = '" + minZoom + "'");
				changed = true;
			}
			if (maxZoom != this.maxZoom) {
				db.execSQL("update info set " + MAX_ZOOM + " = '" + maxZoom + "'");
				changed = true;
			}
			if (r.isEllipticYTile() != isEllipticYTile()) {
				db.execSQL("update info set " + ELLIPSOID + " = '" + (r.isEllipticYTile() ? 1 : 0) + "'");
				changed = true;
			}
			if (r.getExpirationTimeMinutes() != getExpirationTimeMinutes()) {
				db.execSQL("update info set " + EXPIRE_MINUTES + " = '" + r.getExpirationTimeMinutes() + "'");
				changed = true;
			}
		}
		if (db != null && !openedBefore) {
			db.close();
		}
		if (changed) {
			file.setLastModified(System.currentTimeMillis());
		}
	}

	public synchronized boolean isDbOpened() {
		return db != null && !db.isClosed();
	}

	private void addInfoColumn(SQLiteConnection db, String columnName, String value) {
		if(!onlyReadonlyAvailable) {
			try {
				db.execSQL("alter table info add column " + columnName + " TEXT");
			} catch (SQLException e) {
				LOG.info("Error adding column " + e);
			}
			db.execSQL("update info set " + columnName + " = '" + value + "'");
		}
	}

	private boolean hasTimeColumn(SQLiteConnection db) {
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
			if (LOG.isDebugEnabled()) {
				long time = System.currentTimeMillis();
				LOG.debug("Checking tile existence x = " + x + " y = " + y + " z = " + zoom + " for " + (System.currentTimeMillis() - time)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
	}

	@Override
	public long getTileModifyTime(int x, int y, int zoom, String dirWithTiles) {
		long time = System.currentTimeMillis();
		SQLiteConnection db = getDatabase();
		if (db != null && zoom <= maxZoom && timeSupported) {
			String[] params = getTileDbParams(x, y, zoom);
			SQLiteCursor cursor = db.rawQuery("SELECT time FROM tiles WHERE x = ? AND y = ? AND z = ?", params);
			if (cursor.moveToFirst()) {
				time = cursor.getLong(0);
			}
			cursor.close();
		}
		return time;
	}

	@Nullable
	public byte[] getBytes(int x, int y, int zoom, String dirWithTiles, long[] timeHolder) throws IOException {
		SQLiteConnection db = getDatabase();
		if (db == null) {
			return null;
		}
		byte[] blob = null;
		long ts = System.currentTimeMillis();
		try {
			if (zoom <= maxZoom) {
				// return the normal tile if exists
				String[] params = getTileDbParams(x, y, zoom);
				boolean queryTime = timeHolder != null && timeHolder.length > 0 && timeSupported;
				SQLiteCursor cursor = db.rawQuery("SELECT image " + (queryTime ? ", time" : "")
						+ " FROM tiles WHERE x = ? AND y = ? AND z = ?", params);
				if (cursor.moveToFirst()) {
					blob = cursor.getBlob(0);
					if (queryTime) {
						timeHolder[0] = cursor.getLong(1);
					}
				}
				cursor.close();
			}
			return blob;
		} finally {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Load tile " + x + "/" + y + "/" + zoom + " for " + (System.currentTimeMillis() - ts) + " ms " + " loaded " + (blob != null));
			}
		}
	}

	@Override
	public byte[] getBytes(int x, int y, int zoom, String dirWithTiles) throws IOException {
		return getBytes(x, y, zoom, dirWithTiles, null);
	}

	public Bitmap getImage(int x, int y, int zoom, long[] timeHolder) {
		byte[] blob;
		try {
			blob = getBytes(x, y, zoom, null, timeHolder);
		} catch (IOException e) {
			return null;
		}
		String[] params = getTileDbParams(x, y, zoom);
		return blob != null ? getImage(blob, params) : null;
	}

	@Nullable
	public synchronized Bitmap getImage(@NonNull byte[] blob, @NonNull String[] params) {
		Bitmap bmp = BitmapFactory.decodeByteArray(blob, 0, blob.length);
		if (bmp == null) {
			SQLiteConnection db = getDatabase();
			if (db != null) {
				// Delete broken image
				db.execSQL("DELETE FROM tiles WHERE x = ? AND y = ? AND z = ?", params);
			}
		} else if (!tileSizeSpecified && tileSize != bmp.getWidth() && bmp.getWidth() > 0) {
			tileSize = bmp.getWidth();
			addInfoColumn(db, "tilesize", String.valueOf(tileSize));
			tileSizeSpecified = true;
		}
		return bmp;
	}

	@NonNull
	public String[] getTileDbParams(int x, int y, int zoom) {
		return new String[] {String.valueOf(x), String.valueOf(y), String.valueOf(getFileZoom(zoom))};
	}
	 
	public ITileSource getBase() {
		return base;
	}
	
	public QuadRect getRectBoundary(int coordinatesZoom, int minZ){
		SQLiteConnection db = getDatabase();
		if(db == null || coordinatesZoom > 25 ){
			return null;
		}
		SQLiteCursor cursor ;
		if (inversiveZoom) {
			int minZoom = (17 - minZ) + 1;
			// 17 - z = zoom, x << (25 - zoom) = 25th x tile = 8 + z,
			cursor = db.rawQuery("SELECT max(x << (8+z)), min(x << (8+z)), max(y << (8+z)), min(y << (8+z))" +
					" from tiles where z < "
					+ minZoom, new String[0]);
		} else {
			cursor = db.rawQuery("SELECT max(x << (25-z)), min(x << (25-z)), max(y << (25-z)), min(y << (25-z))"
					+ " from tiles where z > " + minZ,
					new String[0]);
		}
		cursor.moveToFirst();
		int right = cursor.getInt(0) >> (25 - coordinatesZoom);
		int left = cursor.getInt(1) >> (25 - coordinatesZoom);
		int top = cursor.getInt(3) >> (25 - coordinatesZoom);
		int bottom  = cursor.getInt(2) >> (25 - coordinatesZoom);

		cursor.close();

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
	
	public void insertImage(int x, int y, int zoom, File fileToSave) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate((int) fileToSave.length());
		FileInputStream is = new FileInputStream(fileToSave);
		int i = 0;
		byte[] b = new byte[BUF_SIZE];
		while ((i = is.read(b, 0, BUF_SIZE)) > -1) {
			buf.put(b, 0, i);
		}

		insertImage(x, y, zoom, buf.array());
		is.close();
	}

	@Override
	public void deleteTiles(String path) {
		SQLiteConnection db = getDatabase();
		if (db == null || db.isReadOnly() || onlyReadonlyAvailable) {
			return;
		}
		db.execSQL("DELETE FROM tiles");
		db.execSQL("VACUUM");
	}

	@Override
	public int getAvgSize() {
		return base != null ? base.getAvgSize() : -1;
	}

	@Override
	public String getRule() {
		return rule;
	}

	@Override
	public String getRandoms() {
		return randoms;
	}

	@Override
	public boolean isInvertedYTile() {
		return invertedY;
	}

	@Override
	public boolean isTimeSupported() {
		return timeSupported;
	}

	@Override
	public boolean getInversiveZoom() {
		return inversiveZoom;
	}

	/**
	 * Makes method synchronized to give a little more time for get methods and 
	 * let all writing attempts to wait outside of this method   
	 */
	public /*synchronized*/ void insertImage(int x, int y, int zoom, byte[] dataToSave) throws IOException {
		SQLiteConnection db = getDatabase();
		if (db == null || db.isReadOnly() || onlyReadonlyAvailable) {
			return;
		}
		/*There is no sense to download and do not save. If needed, check should perform before download 
		  if (exists(x, y, zoom)) {

			return;
		}*/
		
		String query = timeSupported ? "INSERT OR REPLACE INTO tiles(x,y,z,s,image,time) VALUES(?, ?, ?, ?, ?, ?)"
				: "INSERT OR REPLACE INTO tiles(x,y,z,s,image) VALUES(?, ?, ?, ?, ?)";
		net.osmand.plus.api.SQLiteAPI.SQLiteStatement statement = db.compileStatement(query); //$NON-NLS-1$
		statement.bindLong(1, x);
		statement.bindLong(2, y);
		statement.bindLong(3, getFileZoom(zoom));
		statement.bindLong(4, 0);
		statement.bindBlob(5, dataToSave);
		if (timeSupported) {
			statement.bindLong(6, System.currentTimeMillis());
		}
		statement.execute();
		statement.close();

	}

	private int getFileZoom(int zoom) {
		return inversiveZoom ? 17 - zoom : zoom;
	}
	
	public synchronized void closeDB(){
		LOG.debug("closeDB");
		if(timeSupported) {
			clearOld();
		}
		if(db != null){
			db.close();
			db = null;
		}
	}

	public void clearOld() {
		SQLiteConnection db = getDatabase();
		long expiration = getExpirationTimeMillis();
		if(db == null || db.isReadOnly() || expiration <= 0){
			return;
		}
		String sql = "DELETE FROM tiles WHERE time < "+
				(System.currentTimeMillis() - expiration);
		LOG.debug(sql);
		db.execSQL(sql);
		db.execSQL("VACUUM");
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
		return this.isEllipsoid;
		//return false;
	}

	public int getExpirationTimeMinutes() {
		if(expirationTimeMillis  < 0) {
			return -1;
		}
		return (int) (expirationTimeMillis / (60  * 1000));
	}
	
	public long getExpirationTimeMillis() {
		return expirationTimeMillis;
	}
	
	public String getReferer() {
		return referer;
	}

	public String getUserAgent() {
		return userAgent;
	}

	@Override
	public ParameterType getParamType() {
		return ParameterType.UNDEFINED;
	}

	@Override
	public long getParamMin() {
		return 0;
	}

	@Override
	public long getParamStep() {
		return 0;
	}

	@Override
	public long getParamMax() {
		return 0;
	}

	@Override
	public Map<String, String> getUrlParameters() {
		return null;
	}

	@Override
	public String getUrlParameter(String name) {
		return null;
	}

	@Override
	public void setUrlParameter(String name, String value) {
	}

	@Override
	public void resetUrlParameter(String name) {
	}

	@Override
	public void resetUrlParameters() {
	}

	public boolean isFileExist() {
		return file == null ? false : file.exists();
	}
}
