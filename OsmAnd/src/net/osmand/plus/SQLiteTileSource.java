package net.osmand.plus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;

import org.apache.commons.logging.Log;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class SQLiteTileSource implements ITileSource {

	
	public static final String EXT = ".sqlitedb"; //$NON-NLS-1$
	private static final Log log = LogUtil.getLog(SQLiteTileSource.class); 
	
	private ITileSource base;
	private String urlTemplate = null;
	private String name;
	private SQLiteDatabase db;
	private final File file;
	private int minZoom = 1;
	private int maxZoom = 17;
	
	public SQLiteTileSource(File f, List<TileSourceTemplate> toFindUrl){
		this.file = f;
		int i = f.getName().lastIndexOf('.');
		name = f.getName().substring(0, i);
		i = name.lastIndexOf('.');
		if(i > 0){
			String sourceName = name.substring(i+1);
			for(TileSourceTemplate is : toFindUrl){
				if(is.getName().equalsIgnoreCase(sourceName)){
					base = is;
					urlTemplate = is.getUrlTemplate();
					break;
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
		return base != null ? base.getTileSize() : 256;
	}

	@Override
	public String getUrlToLoad(int x, int y, int zoom) {
		SQLiteDatabase db = getDatabase();
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
	
	private SQLiteDatabase getDatabase(){
		if(db == null && file.exists()){
			db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, 0);
			try {
				String template = db.compileStatement("SELECT url FROM info").simpleQueryForString(); //$NON-NLS-1$
				if(!Algoritms.isEmpty(template)){
					urlTemplate = template;
				}
			} catch (RuntimeException e) {
			}
			try {
				long z;
				z = db.compileStatement("SELECT minzoom FROM info").simpleQueryForLong(); //$NON-NLS-1$
				if (z < 17)
					maxZoom = 17 - (int)z;
				z = db.compileStatement("SELECT maxzoom FROM info").simpleQueryForLong(); //$NON-NLS-1$
				if (z < 17)
					minZoom = 17 - (int)z;
			} catch (RuntimeException e) {
			}
		}
		return db;
	}
	
	public boolean exists(int zoom) {
		SQLiteDatabase db = getDatabase();
		if(db == null){
			return false;
		}
		Cursor cursor = db.rawQuery("SELECT 1 FROM tiles WHERE z = ? LIMIT 1", new String[] {(17 - zoom)+""});    //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
		try {
			boolean e = cursor.moveToFirst();
			cursor.close();
			return e;
		} catch (SQLiteDiskIOException e) {
			return false;
		}
	}
	
	public boolean exists(int x, int y, int zoom) {
		SQLiteDatabase db = getDatabase();
		if(db == null){
			return false;
		}
		long time = System.currentTimeMillis();
		Cursor cursor = db.rawQuery("SELECT 1 FROM tiles WHERE x = ? AND y = ? AND z = ?", new String[] {x+"", y+"",(17 - zoom)+""});    //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
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
	}
	
	public boolean isLocked() {
		SQLiteDatabase db = getDatabase();
		if(db == null){
			return false;
		}
		return db.isDbLockedByOtherThreads();
	}

	public Bitmap getImage(int x, int y, int zoom) {
		SQLiteDatabase db = getDatabase();
		if(db == null){
			return null;
		}
		Cursor cursor = db.rawQuery("SELECT image FROM tiles WHERE x = ? AND y = ? AND z = ?", new String[] {x+"", y+"",(17 - zoom)+""});    //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
		byte[] blob = null;
		if(cursor.moveToFirst()) {
			blob = cursor.getBlob(0);
		}
		cursor.close();
		if(blob != null){
			return BitmapFactory.decodeByteArray(blob, 0, blob.length);
		}
		return null;
	}
	
	public ITileSource getBase() {
		return base;
	}

	public void deleteImage(int x, int y, int zoom) {
		SQLiteDatabase db = getDatabase();
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
		SQLiteDatabase db = getDatabase();
		if (db == null || db.isReadOnly()) {
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

		SQLiteStatement statement = db.compileStatement("INSERT INTO tiles VALUES(?, ?, ?, ?, ?)"); //$NON-NLS-1$
		statement.bindLong(1, x);
		statement.bindLong(2, y);
		statement.bindLong(3, 17 - zoom);
		statement.bindLong(4, 0);
		statement.bindBlob(5, buf.array());
		statement.execute();
		statement.close();

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


