package net.osmand.plus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
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

import android.database.sqlite.SQLiteDiskIOException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;


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
	private int baseZoom = 17; //Default base zoom

	final int margin = 1;
	final int tileSize = 256;
	final int minScaledSize = 8;
	private ClientContext ctx;
	
	public SQLiteTileSource(ClientContext ctx, File f, List<TileSourceTemplate> toFindUrl){
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
		if (zoom > baseZoom)
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
			db = ctx.getSQLiteAPI().openByAbsolutePath(file.getAbsolutePath(), false);
			try {
				String template = db.compileStatement("SELECT url FROM info").simpleQueryForString(); //$NON-NLS-1$
				if(!Algorithms.isEmpty(template)){
					urlTemplate = template;
				}
			} catch (RuntimeException e) {
			}
			try {
				long z;
				z = db.compileStatement("SELECT minzoom FROM info").simpleQueryForLong(); //$NON-NLS-1$
				if (z < 17 )
					baseZoom = 17 - (int)z; // sqlite base zoom, =11 for SRTM hillshade
				maxZoom = 24; // Cheat to have tiles request even if zoom level not in sqlite
				// decrease maxZoom if too much scaling would be required
				while ((tileSize >> (maxZoom - baseZoom)) < minScaledSize)
					maxZoom--;
				z = db.compileStatement("SELECT maxzoom FROM info").simpleQueryForLong(); //$NON-NLS-1$
				if (z < 17)
					minZoom = 17 - (int)z;
			} catch (RuntimeException e) {
			}
		}
		return db;
	}
	
	public boolean exists(int x, int y, int zoom, boolean exact) {
		SQLiteConnection db = getDatabase();
		if(db == null){
			return false;
		}
		long time = System.currentTimeMillis();
		if (exact || zoom <= baseZoom) {
			SQLiteCursor cursor = db.rawQuery("SELECT 1 FROM tiles WHERE x = ? AND y = ? AND z = ?", new String[] {x+"", y+"",(17 - zoom)+""});    //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
			try {
				boolean e = cursor.moveToFirst();
				cursor.close();
				if (log.isDebugEnabled()) {
					log.debug("Checking tile existance x = " + x + " y = " + y + " z = " + zoom + " for " + (System.currentTimeMillis() - time)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
				return e;
			} catch (SQLiteDiskIOException e) {
				return false;
			}
		} else {
			int n = zoom - baseZoom;
			int base_xtile = x >> n;
			int base_ytile = y >> n;
			SQLiteCursor cursor = db.rawQuery("SELECT 1 FROM tiles WHERE x = ? AND y = ? AND z = ?", new String[] {base_xtile+"", base_ytile+"",(17 - baseZoom)+""});    //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
			try {
				boolean e = cursor.moveToFirst();
				cursor.close();
				if (log.isDebugEnabled()) {
					log.debug("Checking parent tile existance x = " + base_xtile + " y = " + base_ytile + " z = " + baseZoom + " for " + (System.currentTimeMillis() - time)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
				return e;
			} catch (SQLiteDiskIOException e) {
				return false;
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

	private Bitmap getMetaTile(int x, int y, int zoom, int flags) {
		// return a (tileSize+2*margin)^2 tile around a given tile
		// based on its neighbor. This is needed to have a nice bilinear resampling
		// on tile edges. Margin of 1 is enough for bilinear resampling.

		SQLiteConnection db = getDatabase();
		if(db == null){
			return null;
		}
		
		Bitmap stitchedImage = Bitmap.createBitmap(tileSize + 2 * margin, tileSize + 2 * margin, Config.ARGB_8888);
		Canvas canvas = new Canvas(stitchedImage);

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if ((flags & (0x400 >> (4 * (dy + 1) + (dx + 1)))) == 0)
					continue;


				int xOff, yOff, w, h;
				int dstx, dsty;
				SQLiteCursor cursor = db.rawQuery(
						"SELECT image FROM tiles WHERE x = ? AND y = ? AND z = ?",
						new String[] {(x + dx) + "", (y + dy) + "", (17 - zoom) + ""});    //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				byte[] blob = null;
				if(cursor.moveToFirst()) {
					blob = cursor.getBlob(0);
				}
				cursor.close();
				if (dx < 0) xOff = tileSize - margin; else xOff = 0;
				if (dx == 0) w = tileSize; else w = margin;
				if (dy < 0) yOff = tileSize - margin; else yOff = 0;
				if (dy == 0) h = tileSize; else h = margin;
				dstx = dx * tileSize + xOff + margin;
				dsty = dy * tileSize + yOff + margin;
				if(blob != null){

					Bitmap Tile =  BitmapFactory.decodeByteArray(blob, 0, blob.length);
					blob = null;
					Rect src = new Rect(xOff, yOff, xOff + w, yOff + h);
					Rect dst = new Rect(dstx, dsty, dstx + w, dsty + h);
					canvas.drawBitmap(Tile, src, dst, null);
					Tile.recycle();
				}
			}
		}
		return stitchedImage; // return a tileSize+2*margin size image
		
	}
	
	public Bitmap getImage(int x, int y, int zoom) {
		SQLiteConnection db = getDatabase();
		if(db == null){
			return null;
		}
		if (zoom <= baseZoom) {
			// return the normal tile if exists
			SQLiteCursor cursor = db.rawQuery("SELECT image FROM tiles WHERE x = ? AND y = ? AND z = ?", new String[] {x+"", y+"",(17 - zoom)+""});    //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
			byte[] blob = null;
			if(cursor.moveToFirst()) {
				blob = cursor.getBlob(0);
			}
			cursor.close();
			if(blob != null){
				return BitmapFactory.decodeByteArray(blob, 0, blob.length);
			}
			return null;
		} else {
			// return a resampled tile from its last parent
			int n = zoom - baseZoom;
			int base_xtile = x >> n;
			int base_ytile = y >> n;

			int scaledSize= tileSize >> n;
			int offset_x=  x - (base_xtile << n);
			int offset_y=  y - (base_ytile << n);
			int flags = 0x020;

			if (scaledSize < minScaledSize)
				return null;

			if (offset_x == 0)
				flags |= 0x444;
			else if (offset_x == (1 << n) - 1)
				flags |= 0x111;
			if (offset_y == 0)
				flags |= 0x700;
			else if (offset_y == (1 << n) - 1)
				flags |= 0x007;

			Bitmap metaTile = getMetaTile(base_xtile, base_ytile, baseZoom, flags);

			if(metaTile != null){
				// in tile space:
				int delta_px = scaledSize * offset_x;
				int delta_py = scaledSize * offset_y;
				
				RectF src = new RectF(0.5f, 0.5f,
						scaledSize + 2 * margin - 0.5f, scaledSize + 2 * margin - 0.5f);
				RectF dest = new RectF(0, 0, tileSize, tileSize);
				Matrix m = new Matrix();
				m.setRectToRect(src, dest, Matrix.ScaleToFit.FILL);
				return Bitmap.createBitmap(metaTile, delta_px, delta_py,
						scaledSize + 2*margin-1, scaledSize + 2*margin-1, m, true);
			}
			return null;
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
		int minZoom = (17 - minZ) + 1; 
		// 17 - z = zoom, x << (25 - zoom) = 25th x tile = 8 + z,  
		
		SQLiteCursor q = db.rawQuery("SELECT max(x << (8+z)), min(x << (8+z)), max(y << (8+z)), min(y << (8+z))" + " from tiles where z < "
				+ minZoom,
				new String[0]);
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
		db.execSQL("DELETE FROM tiles WHERE x = ? AND y = ? AND z = ?", new String[] {x+"", y+"",(17 - zoom)+""});    //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
	}

	private final int BUF_SIZE = 1024;
	
	/**
	 * Makes method synchronized to give a little more time for get methods and 
	 * let all writing attempts to wait outside of this method   
	 */
	public synchronized void insertImage(int x, int y, int zoom, File fileToSave) throws IOException {
		SQLiteConnection db = getDatabase();
		if (db == null || db.isReadOnly()) {
			return;
		}
		if (exists(x, y, zoom, true)) {
			return;
		}
		ByteBuffer buf = ByteBuffer.allocate((int) fileToSave.length());
		FileInputStream is = new FileInputStream(fileToSave);
		int i = 0;
		byte[] b = new byte[BUF_SIZE];
		while ((i = is.read(b, 0, BUF_SIZE)) > -1) {
			buf.put(b, 0, i);
		}

		net.osmand.plus.api.SQLiteAPI.SQLiteStatement statement = db.compileStatement("INSERT INTO tiles VALUES(?, ?, ?, ?, ?)"); //$NON-NLS-1$
		statement.bindLong(1, x);
		statement.bindLong(2, y);
		statement.bindLong(3, 17 - zoom);
		statement.bindLong(4, 0);
		statement.bindBlob(5, buf.array());
		statement.execute();
		statement.close();
		is.close();

	}
	
	public void closeDB(){
		if(db != null){
			db.close();
			db = null;
		}
	}

	@Override
	public boolean couldBeDownloadedFromInternet() {
		if(getDatabase() == null || getDatabase().isReadOnly()){
			return false;
		}
		return urlTemplate != null;
	}

	@Override
	public boolean isEllipticYTile() {
		return false;
	}

}


