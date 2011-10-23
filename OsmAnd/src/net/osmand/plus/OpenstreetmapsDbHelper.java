package net.osmand.plus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.OpenstreetmapPoint;
import net.osmand.OpenstreetmapRemoteUtil;
import net.osmand.osm.Node;
import net.osmand.osm.OSMSettings.OSMTagKey;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class OpenstreetmapsDbHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	public static final String OPENSTREETMAP_DB_NAME = "openstreetmap"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_TABLE_NAME = "openstreetmap"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_ID = "id"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_NAME = "name"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_TYPE = "type"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_SUBTYPE = "subtype"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_LAT = "latitude"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_LON = "longitude"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_ACTION = "action"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_COMMENT = "comment"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_OPENINGHOURS = "openinghours"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_TABLE_CREATE = "CREATE TABLE " + OPENSTREETMAP_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
			OPENSTREETMAP_COL_ID + " INTEGER, " + OPENSTREETMAP_COL_NAME + " TEXT, " + OPENSTREETMAP_COL_TYPE + " TEXT, " + OPENSTREETMAP_COL_SUBTYPE + " TEXT, " + //$NON-NLS-1$ //$NON-NLS-2$ 
			OPENSTREETMAP_COL_LAT + " double, " + OPENSTREETMAP_COL_LON + " double, " + //$NON-NLS-1$ //$NON-NLS-2$
			OPENSTREETMAP_COL_ACTION + " TEXT, " + OPENSTREETMAP_COL_COMMENT + " TEXT, " + OPENSTREETMAP_COL_OPENINGHOURS + " TEXT);"; //$NON-NLS-1$ //$NON-NLS-2$ 
	
	private List<OpenstreetmapPoint> cachedOpenstreetmapPoints = new ArrayList<OpenstreetmapPoint>();
	private final Context context;

	public OpenstreetmapsDbHelper(Context context) {
		super(context, OPENSTREETMAP_DB_NAME, null, DATABASE_VERSION);
		this.context = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(OPENSTREETMAP_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public List<OpenstreetmapPoint> getOpenstreetmapPoints() {
		checkOpenstreetmapPoints();
		return cachedOpenstreetmapPoints;
	}
	
	public boolean addOpenstreetmap(OpenstreetmapPoint p) {
		checkOpenstreetmapPoints();
		if(p.getName().equals("")){
			return true;
		}
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL("INSERT INTO " + OPENSTREETMAP_TABLE_NAME +
					" (" + OPENSTREETMAP_COL_ID + ", " + OPENSTREETMAP_COL_NAME + ", " + OPENSTREETMAP_COL_TYPE + ", " + OPENSTREETMAP_COL_SUBTYPE + ", " + OPENSTREETMAP_COL_LAT + "," + OPENSTREETMAP_COL_LON + "," + OPENSTREETMAP_COL_ACTION + "," + OPENSTREETMAP_COL_COMMENT + "," + OPENSTREETMAP_COL_OPENINGHOURS + ")" +
					   " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] { p.getId(), p.getName(), p.getType(), p.getSubtype(), p.getLatitude(), p.getLongitude(), OpenstreetmapRemoteUtil.stringAction.get(p.getAction()), p.getComment(), p.getOpeninghours() }); //$NON-NLS-1$ //$NON-NLS-2$
			cachedOpenstreetmapPoints.add(p);
			p.setStored(true);
			return true;
		}
		return false;
	}
	
	public boolean deleteOpenstreetmap(OpenstreetmapPoint p) {
		checkOpenstreetmapPoints();
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL("DELETE FROM " + OPENSTREETMAP_TABLE_NAME +
					" WHERE " + OPENSTREETMAP_COL_ID + " = ? AND " +
					   OPENSTREETMAP_COL_NAME + " = ? AND " +
					   OPENSTREETMAP_COL_TYPE + " = ? AND " +
					   OPENSTREETMAP_COL_SUBTYPE + " = ? AND " +
					   OPENSTREETMAP_COL_ACTION + " = ?",
					   new Object[] { p.getId(), p.getName(), p.getType(), p.getSubtype(), OpenstreetmapRemoteUtil.stringAction.get(p.getAction()) }); //$NON-NLS-1$ //$NON-NLS-2$
			cachedOpenstreetmapPoints.remove(p);
			p.setStored(false);
			return true;
		}
		return false;
	}
	
	private void checkOpenstreetmapPoints(){
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			Cursor query = db.rawQuery("SELECT " + OPENSTREETMAP_COL_ID + ", " + OPENSTREETMAP_COL_NAME + ", " + OPENSTREETMAP_COL_TYPE + ", " + OPENSTREETMAP_COL_SUBTYPE + ", " + OPENSTREETMAP_COL_LAT + "," + OPENSTREETMAP_COL_LON + "," + OPENSTREETMAP_COL_ACTION + "," + OPENSTREETMAP_COL_COMMENT + "," + OPENSTREETMAP_COL_OPENINGHOURS + " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
					OPENSTREETMAP_TABLE_NAME, null);
			cachedOpenstreetmapPoints.clear();
			if (query.moveToFirst()) {
				do {
					String name = query.getString(1);
					if (!name.equals("")) {
						OpenstreetmapPoint p = new OpenstreetmapPoint();
						Node entity = new Node(query.getDouble(4),
											   query.getDouble(5),
											   query.getLong(0));

						entity.putTag(query.getString(2), query.getString(3));
						entity.putTag(OSMTagKey.NAME.getValue(), name);
						String openingHours = query.getString(8);
						if (openingHours != null && openingHours.length() > 0)
							entity.putTag(OSMTagKey.OPENING_HOURS.getValue(), openingHours);
						p.setEntity(entity);
						p.setStored(true);
						p.setAction(query.getString(6));
						p.setComment(query.getString(7));
						cachedOpenstreetmapPoints.add(p);
					}
				} while (query.moveToNext());
			}
			query.close();
		}
	}

}