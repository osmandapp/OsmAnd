package net.osmand.plus.osmedit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import net.osmand.osm.Node;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class OpenstreetmapsDbHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 4;
	public static final String OPENSTREETMAP_DB_NAME = "openstreetmap"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_TABLE_NAME = "openstreetmaptable"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_ID = "id"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_LAT= "lat"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_LON= "lon"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_TAGS = "tags"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_ACTION = "action"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_COMMENT = "comment"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_TABLE_CREATE = "CREATE TABLE " + OPENSTREETMAP_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
			OPENSTREETMAP_COL_ID + " bigint,"+
			OPENSTREETMAP_COL_LAT + " double," + OPENSTREETMAP_COL_LON + " double," +
			OPENSTREETMAP_COL_TAGS + " VARCHAR(2048)," +
			OPENSTREETMAP_COL_ACTION + " TEXT, " + OPENSTREETMAP_COL_COMMENT + " TEXT);"; //$NON-NLS-1$ //$NON-NLS-2$ 
	

	public OpenstreetmapsDbHelper(Context context) {
		super(context, OPENSTREETMAP_DB_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(OPENSTREETMAP_TABLE_CREATE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(newVersion == 4) {
			db.execSQL("DROP TABLE IF EXISTS " + OPENSTREETMAP_TABLE_NAME);
			db.execSQL(OPENSTREETMAP_TABLE_CREATE);	
		}
	}

	public List<OpenstreetmapPoint> getOpenstreetmapPoints() {
		return checkOpenstreetmapPoints();
	}
	
	public boolean addOpenstreetmap(OpenstreetmapPoint p) {
		checkOpenstreetmapPoints();
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			StringBuilder tags = new StringBuilder();
			Iterator<Entry<String, String>> eit = p.getEntity().getTags().entrySet().iterator();
			while(eit.hasNext()) {
				Entry<String, String> e = eit.next();
				tags.append(e.getKey()).append("$$$").append(e.getValue());
				if(eit.hasNext()) {
					tags.append("$$$");
				}
			}
			db.execSQL("INSERT INTO " + OPENSTREETMAP_TABLE_NAME +
					" (" + OPENSTREETMAP_COL_ID + ", " + OPENSTREETMAP_COL_LAT + ", " + OPENSTREETMAP_COL_LON + ", " + OPENSTREETMAP_COL_TAGS + ", " + OPENSTREETMAP_COL_ACTION + "," + OPENSTREETMAP_COL_COMMENT + ")" +
					   " VALUES (?, ?, ?, ?, ?, ?)",
					   new Object[] { p.getId(),p.getLatitude(), p.getLongitude(), tags.toString() , OsmPoint.stringAction.get(p.getAction()), p.getComment(),  }); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
		return false;
	}
	
	
	
	public boolean deletePOI(OpenstreetmapPoint p) {
		checkOpenstreetmapPoints();
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL("DELETE FROM " + OPENSTREETMAP_TABLE_NAME +
					" WHERE " + OPENSTREETMAP_COL_ID + " = ?", new Object[] { p.getId() }); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}
		return false;
	}
	

	private List<OpenstreetmapPoint> checkOpenstreetmapPoints(){
		SQLiteDatabase db = getWritableDatabase();
		List<OpenstreetmapPoint> cachedOpenstreetmapPoints = new ArrayList<OpenstreetmapPoint>();
		if (db != null) {
			Cursor query = db.rawQuery("SELECT " + OPENSTREETMAP_COL_ID + ", " + OPENSTREETMAP_COL_LAT + "," + OPENSTREETMAP_COL_LON + "," + OPENSTREETMAP_COL_ACTION + "," + OPENSTREETMAP_COL_COMMENT + "," + OPENSTREETMAP_COL_TAGS+ " FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
					OPENSTREETMAP_TABLE_NAME, null);
			if (query.moveToFirst()) {
				do {
					OpenstreetmapPoint p = new OpenstreetmapPoint();
					Node entity = new Node(query.getDouble(1),
										   query.getDouble(2),
										   query.getLong(0));
					String tags = query.getString(5);
					String[] split = tags.split("\\$\\$\\$");
					for(int i=0; i<split.length - 1; i+= 2){
						entity.putTag(split[i].trim(), split[i+1].trim());
					}
					p.setEntity(entity);
					p.setAction(query.getString(3));
					p.setComment(query.getString(4));
					cachedOpenstreetmapPoints.add(p);
				} while (query.moveToNext());
			}
			query.close();
		}
		return cachedOpenstreetmapPoints;
	}

	public long getMinID() {
		SQLiteDatabase db = getReadableDatabase();
		long minID = 0;
		if (db != null) {
			Cursor query = db.rawQuery("SELECT MIN(" + OPENSTREETMAP_COL_ID + ") FROM " + OPENSTREETMAP_TABLE_NAME, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			if (query.moveToFirst()) {
				minID = query.getLong(0);
			}
			query.close();
		}
		return minID;
	}

}