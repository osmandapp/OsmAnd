package net.osmand.plus.osmedit;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.osmand.AndroidUtils;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

public class OsmBugsDbHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	public static final String OSMBUGS_DB_NAME = "osmbugs"; //$NON-NLS-1$
	private static final String OSMBUGS_TABLE_NAME = "osmbugs"; //$NON-NLS-1$
	private static final String OSMBUGS_COL_ID = "id"; //$NON-NLS-1$
	private static final String OSMBUGS_COL_TEXT = "text"; //$NON-NLS-1$
	private static final String OSMBUGS_COL_LAT = "latitude"; //$NON-NLS-1$
	private static final String OSMBUGS_COL_LON = "longitude"; //$NON-NLS-1$
	private static final String OSMBUGS_COL_ACTION = "action"; //$NON-NLS-1$
	private static final String OSMBUGS_COL_AUTHOR = "author"; //$NON-NLS-1$
	private static final String OSMBUGS_TABLE_CREATE = "CREATE TABLE " + OSMBUGS_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
			OSMBUGS_COL_ID + " INTEGER, " + OSMBUGS_COL_TEXT + " TEXT,  " + //$NON-NLS-1$ //$NON-NLS-2$
			OSMBUGS_COL_LAT + " double, " + OSMBUGS_COL_LON + " double, " + //$NON-NLS-1$ //$NON-NLS-2$
			OSMBUGS_COL_ACTION + " TEXT, " + OSMBUGS_COL_AUTHOR + " TEXT);"; //$NON-NLS-1$ //$NON-NLS-2$

	private static final String OSMBUGS_DB_LAST_MODIFIED_NAME = "osmbugs";

	private final Context context;
	List<OsmNotesPoint> cache = null;

	public OsmBugsDbHelper(@NonNull Context context) {
		super(context, OSMBUGS_DB_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(OSMBUGS_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public long getLastModifiedTime() {
		long lastModifiedTime = BackupHelper.getLastModifiedTime(context, OSMBUGS_DB_LAST_MODIFIED_NAME);
		if (lastModifiedTime == 0) {
			File dbFile = context.getDatabasePath(OSMBUGS_DB_NAME);
			lastModifiedTime = dbFile.exists() ? dbFile.lastModified() : 0;
			BackupHelper.setLastModifiedTime(context, OSMBUGS_DB_LAST_MODIFIED_NAME, lastModifiedTime);
		}
		return lastModifiedTime;
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		BackupHelper.setLastModifiedTime(context, OSMBUGS_DB_LAST_MODIFIED_NAME, lastModifiedTime);
	}

	private void updateLastModifiedTime() {
		BackupHelper.setLastModifiedTime(context, OSMBUGS_DB_LAST_MODIFIED_NAME);
	}

	public List<OsmNotesPoint> getOsmbugsPoints() {
		if (cache == null) {
			SQLiteDatabase db = getReadableDatabase();
			List<OsmNotesPoint> res = checkOsmbugsPoints(db);
			db.close();
			return res;
		}
		return cache;
	}

	public boolean updateOsmBug(long id, String text) {
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL("UPDATE " + OSMBUGS_TABLE_NAME +
							" SET " + OSMBUGS_COL_TEXT + " = ? " +
							"WHERE " + OSMBUGS_COL_ID + " = ?", new Object[]{text, id});
			checkOsmbugsPoints(db);
			updateLastModifiedTime();
			db.close();
			return true;
		}
		return false;
	}

	public boolean addOsmbugs(OsmNotesPoint p) {
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			Map<String, Object> rowsMap = new HashMap<>();
			rowsMap.put(OSMBUGS_COL_ID, p.getId());
			rowsMap.put(OSMBUGS_COL_TEXT, p.getText());
			rowsMap.put(OSMBUGS_COL_LAT, p.getLatitude());
			rowsMap.put(OSMBUGS_COL_LON, p.getLongitude());
			rowsMap.put(OSMBUGS_COL_ACTION, OsmPoint.stringAction.get(p.getAction()));
			rowsMap.put(OSMBUGS_COL_AUTHOR, p.getAuthor());

			db.execSQL(AndroidUtils.createDbInsertQuery(OSMBUGS_TABLE_NAME, rowsMap.keySet()),
					rowsMap.values().toArray());
			checkOsmbugsPoints(db);
			updateLastModifiedTime();
			db.close();
			return true;
		}
		return false;
	}

	public boolean deleteAllBugModifications(OsmNotesPoint p) {
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL("DELETE FROM " + OSMBUGS_TABLE_NAME +
					" WHERE " + OSMBUGS_COL_ID + " = ?", new Object[] { p.getId() }); //$NON-NLS-1$ //$NON-NLS-2$
			checkOsmbugsPoints(db);
			updateLastModifiedTime();
			db.close();
			return true;
		}
		return false;
	}

	private List<OsmNotesPoint> checkOsmbugsPoints(SQLiteDatabase db){
		List<OsmNotesPoint> cachedOsmbugsPoints = new ArrayList<OsmNotesPoint>();
		if (db != null) {
			Cursor query = db.rawQuery("SELECT " + OSMBUGS_COL_ID + ", " + OSMBUGS_COL_TEXT + ", " + OSMBUGS_COL_LAT + "," + OSMBUGS_COL_LON + "," + OSMBUGS_COL_ACTION + "," + OSMBUGS_COL_AUTHOR + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
					OSMBUGS_TABLE_NAME, null);
			
			if (query.moveToFirst()) {
				do {
					boolean isValidId = Algorithms.isInt(query.getString(0));
					if (!isValidId) {
						continue;
					}

					OsmNotesPoint p = new OsmNotesPoint();
					p.setId(query.getLong(0));
					p.setText(query.getString(1));
					p.setLatitude(query.getDouble(2));
					p.setLongitude(query.getDouble(3));
					p.setAction(query.getString(4));
					p.setAuthor(query.getString(5));

					cachedOsmbugsPoints.add(p);
				} while (query.moveToNext());
			}
			query.close();
			cache = cachedOsmbugsPoints;
		}
		return cachedOsmbugsPoints;
	}

	public long getMinID() {
		SQLiteDatabase db = getReadableDatabase();
		long minID = 0;
		if (db != null) {
			Cursor query = db.rawQuery("SELECT MIN(" + OSMBUGS_COL_ID + ") FROM " + OSMBUGS_TABLE_NAME, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			if (query.moveToFirst()) {
				minID = query.getLong(0);
			}
			query.close();
		}
		return minID;
	}

}