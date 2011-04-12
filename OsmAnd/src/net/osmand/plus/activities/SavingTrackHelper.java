package net.osmand.plus.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.plus.GPXUtilities;
import net.osmand.plus.LogUtil;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.GPXUtilities.TrkPt;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.format.DateFormat;

public class SavingTrackHelper extends SQLiteOpenHelper {
	public final static String TRACKS_PATH = "tracks"; //$NON-NLS-1$
	
	public final static String DATABASE_NAME = "tracks"; //$NON-NLS-1$
	public final static int DATABASE_VERSION = 1;
	
	public final static String TRACK_NAME = "track"; //$NON-NLS-1$
	public final static String TRACK_COL_DATE = "date"; //$NON-NLS-1$
	public final static String TRACK_COL_LAT = "lat"; //$NON-NLS-1$
	public final static String TRACK_COL_LON = "lon"; //$NON-NLS-1$
	public final static String TRACK_COL_ALTITUDE = "altitude"; //$NON-NLS-1$
	public final static String TRACK_COL_SPEED = "speed"; //$NON-NLS-1$
	
	public final static Log log = LogUtil.getLog(SavingTrackHelper.class);
	
	
	

	private String updateScript;
	private long lastTimeUpdated = 0;
	private final Context ctx;
	
	public SavingTrackHelper(Context ctx){
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		this.ctx = ctx;
		updateScript = "INSERT INTO " + TRACK_NAME + " VALUES (?, ?, ?, ?, ?)"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TRACK_NAME+ " ("+TRACK_COL_LAT +" double, " + TRACK_COL_LON+" double, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$  
				+ TRACK_COL_ALTITUDE+" double, " + TRACK_COL_SPEED+" double, "  //$NON-NLS-1$ //$NON-NLS-2$
				+ TRACK_COL_DATE +" long )" ); //$NON-NLS-1$
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	
	
		
	public boolean hasDataToSave(){
		SQLiteDatabase db = getReadableDatabase();
		if(db != null){
			Cursor q = db.query(false, TRACK_NAME, new String[0], null, null, null, null, null, null);
			boolean m = q.moveToFirst();
			q.close();
			return m;
		}
		
		return false;
	}
	
	public List<String> saveDataToGpx(){
		SQLiteDatabase db = getReadableDatabase();
		List<String> warnings = new ArrayList<String>();
		File file = Environment.getExternalStorageDirectory();
		if(db != null && file.canWrite()){
			file = new File(file, ResourceManager.APP_DIR + TRACKS_PATH);
			file.mkdirs();
			if (file.exists()) {
				Cursor query = db.rawQuery("SELECT " + TRACK_COL_LAT + "," + TRACK_COL_LON + "," + TRACK_COL_ALTITUDE + "," //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						+ TRACK_COL_SPEED + "," + TRACK_COL_DATE + " FROM " + TRACK_NAME +" ORDER BY " + TRACK_COL_DATE +" ASC", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				long previousTime = 0;
				Map<String, List<List<TrkPt>>> data = new LinkedHashMap<String, List<List<TrkPt>>>();
				List<TrkPt> segment = new ArrayList<TrkPt>();
				List<List<TrkPt>> track = new ArrayList<List<TrkPt>>();
				track.add(segment);
				if (query.moveToFirst()) {
					do {
						TrkPt pt = new TrkPt();
						pt.lat = query.getDouble(0);
						pt.lon = query.getDouble(1);
						pt.ele = query.getDouble(2);
						pt.speed = query.getDouble(3);
						long time = query.getLong(4);
						pt.time = time;
						
						if (previousTime == 0) {
							data.put(DateFormat.format("yyyy-MM-dd", time).toString(), track); //$NON-NLS-1$
							segment.add(pt);
						} else if (Math.abs(time - previousTime) < 60000) {
							// 1 hour - same segment
							segment.add(pt);
						} else if (Math.abs(time - previousTime) < 3600000) {
							// 1 hour - same track
							segment = new ArrayList<TrkPt>();
							segment.add(pt);
							track.add(segment);
						} else {
							// check day (possibly better create new track (not new segment)
							String date = DateFormat.format("yyyy-MM-dd", time).toString(); //$NON-NLS-1$
							if (data.containsKey(date)) {
								track = data.get(date);
							} else {
								track = new ArrayList<List<TrkPt>>();
								data.put(date, track);
							}
							segment = new ArrayList<TrkPt>();
							segment.add(pt);
							track.add(segment);
						}

						previousTime = time;
					} while (query.moveToNext());
				}
				query.close();
				String w = GPXUtilities.saveToXMLFiles(file, data, ctx);
				if(w != null){
					warnings.add(w);
				}
			}
		}
		
		db = getWritableDatabase();
		if(db != null){
//			Calendar cal = Calendar.getInstance();
//			cal.setTime(new java.util.Date());
//			cal.set(Calendar.HOUR_OF_DAY, 0);
//			cal.set(Calendar.MINUTE, 0);
//			cal.set(Calendar.SECOND, 0);
//			cal.set(Calendar.MILLISECOND, 0);
			// remove all from db
			db.execSQL("DELETE FROM " + TRACK_NAME+ " WHERE " + TRACK_COL_DATE + " <= ?", new Object[]{System.currentTimeMillis()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return warnings;
	}
	
	public void insertData(double lat, double lon, double alt, double speed, long time, SharedPreferences settings){
		if (time - lastTimeUpdated > OsmandSettings.getSavingTrackInterval(settings)*1000) {
			SQLiteDatabase db = getWritableDatabase();
			if (db != null) {
				db.execSQL(updateScript, new Object[] { lat, lon, alt, speed, time });
			}
			lastTimeUpdated = time;
		}
	}
	

}
