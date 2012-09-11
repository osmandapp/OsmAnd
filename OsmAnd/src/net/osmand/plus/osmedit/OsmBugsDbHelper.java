package net.osmand.plus.osmedit;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

	public OsmBugsDbHelper(Context context) {
		super(context, OSMBUGS_DB_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(OSMBUGS_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public List<OsmbugsPoint> getOsmbugsPoints() {
		return checkOsmbugsPoints();
	}

	public boolean addOsmbugs(OsmbugsPoint p) {
		checkOsmbugsPoints();
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL(
					"INSERT INTO " + OSMBUGS_TABLE_NAME + " (" + OSMBUGS_COL_ID + ", " + OSMBUGS_COL_TEXT + ", " + OSMBUGS_COL_LAT + ","
							+ OSMBUGS_COL_LON + "," + OSMBUGS_COL_ACTION + "," + OSMBUGS_COL_AUTHOR + ")" + " VALUES (?, ?, ?, ?, ?, ?)", new Object[] { p.getId(), p.getText(), p.getLatitude(), p.getLongitude(), OsmPoint.stringAction.get(p.getAction()), p.getAuthor() }); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
		return false;
	}

	public boolean deleteAllBugModifications(OsmbugsPoint p) {
		checkOsmbugsPoints();
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL("DELETE FROM " + OSMBUGS_TABLE_NAME +
					" WHERE " + OSMBUGS_COL_ID + " = ?", new Object[] { p.getId() }); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
		return false;
	}

	private List<OsmbugsPoint> checkOsmbugsPoints(){
		SQLiteDatabase db = getWritableDatabase();
		List<OsmbugsPoint> cachedOsmbugsPoints = new ArrayList<OsmbugsPoint>();
		if (db != null) {
			Cursor query = db.rawQuery("SELECT " + OSMBUGS_COL_ID + ", " + OSMBUGS_COL_TEXT + ", " + OSMBUGS_COL_LAT + "," + OSMBUGS_COL_LON + "," + OSMBUGS_COL_ACTION + "," + OSMBUGS_COL_AUTHOR + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
					OSMBUGS_TABLE_NAME, null);
			
			if (query.moveToFirst()) {
				do {
					OsmbugsPoint p = new OsmbugsPoint();

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