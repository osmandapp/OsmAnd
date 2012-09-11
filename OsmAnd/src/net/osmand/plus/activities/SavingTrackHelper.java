package net.osmand.plus.activities;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.LogUtil;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ResourceManager;

import org.apache.commons.logging.Log;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.text.format.DateFormat;

public class SavingTrackHelper extends SQLiteOpenHelper {
	
	public final static String DATABASE_NAME = "tracks"; //$NON-NLS-1$
	public final static int DATABASE_VERSION = 3;
	
	public final static String TRACK_NAME = "track"; //$NON-NLS-1$
	public final static String TRACK_COL_DATE = "date"; //$NON-NLS-1$
	public final static String TRACK_COL_LAT = "lat"; //$NON-NLS-1$
	public final static String TRACK_COL_LON = "lon"; //$NON-NLS-1$
	public final static String TRACK_COL_ALTITUDE = "altitude"; //$NON-NLS-1$
	public final static String TRACK_COL_SPEED = "speed"; //$NON-NLS-1$
	public final static String TRACK_COL_HDOP = "hdop"; //$NON-NLS-1$
	
	public final static String POINT_NAME = "point"; //$NON-NLS-1$
	public final static String POINT_COL_DATE = "date"; //$NON-NLS-1$
	public final static String POINT_COL_LAT = "lat"; //$NON-NLS-1$
	public final static String POINT_COL_LON = "lon"; //$NON-NLS-1$
	public final static String POINT_COL_DESCRIPTION = "description"; //$NON-NLS-1$
	
	public final static Log log = LogUtil.getLog(SavingTrackHelper.class);

	private String updateScript;
	private String updatePointsScript;
	
	private long lastTimeUpdated = 0;
	private final OsmandApplication ctx;

	private LatLon lastPoint;
	private float distance = 0;
	
	public SavingTrackHelper(OsmandApplication ctx){
		super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		this.ctx = ctx;
		updateScript = "INSERT INTO " + TRACK_NAME + 
		" (" +TRACK_COL_LAT +", " +TRACK_COL_LON+", " +TRACK_COL_ALTITUDE+", " +TRACK_COL_SPEED
			 +", " +TRACK_COL_HDOP+", " +TRACK_COL_DATE+ ")" +
		" VALUES (?, ?, ?, ?, ?, ?)"; //$NON-NLS-1$ //$NON-NLS-2$
		updatePointsScript = "INSERT INTO " + POINT_NAME + " VALUES (?, ?, ?, ?)"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTableForTrack(db);
		createTableForPoints(db);
	}
	
	private void createTableForTrack(SQLiteDatabase db){
		db.execSQL("CREATE TABLE " + TRACK_NAME+ " ("+TRACK_COL_LAT +" double, " + TRACK_COL_LON+" double, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$  
				+ TRACK_COL_ALTITUDE+" double, " + TRACK_COL_SPEED+" double, "  //$NON-NLS-1$ //$NON-NLS-2$
				+ TRACK_COL_HDOP +" double, " + TRACK_COL_DATE +" long )" ); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private void createTableForPoints(SQLiteDatabase db){
		try {
			db.execSQL("CREATE TABLE " + POINT_NAME+ " ("+POINT_COL_LAT +" double, " + POINT_COL_LON+" double, " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$  
					+ POINT_COL_DATE+" long, " + POINT_COL_DESCRIPTION+" text)" ); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (RuntimeException e) {
			// ignore if already exists
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion < 2){
			createTableForPoints(db);
		}
		if(oldVersion < 3){
			db.execSQL("ALTER TABLE " + TRACK_NAME +  " ADD " + TRACK_COL_HDOP + " double");
		}
	}
	
	
	
		
	public boolean hasDataToSave() {
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			Cursor q = db.query(false, TRACK_NAME, new String[0], null, null, null, null, null, null);
			boolean has = q.moveToFirst();
			q.close();
			if (has) {
				return true;
			}
			q = db.query(false, POINT_NAME, new String[0], null, null, null, null, null, null);
			has = q.moveToFirst();
			q.close();
			if (has) {
				return true;
			}
		}

		return false;
	}
	
	/**
	 * @return warnings
	 */
	public List<String> saveDataToGpx() {
		List<String> warnings = new ArrayList<String>();
		File dir = ((OsmandApplication) ctx.getApplicationContext()).getSettings().getExternalStorageDirectory();
		if (dir.canWrite()) {
			dir = new File(dir, ResourceManager.GPX_PATH);
			dir.mkdirs();
			if (dir.exists()) {

				Map<String, GPXFile> data = collectRecordedData();

				// save file
				for (final String f : data.keySet()) {
					File fout = new File(dir, f + ".gpx"); //$NON-NLS-1$
					if (!data.get(f).isEmpty()) {
						WptPt pt = data.get(f).findPointToShow();
						String fileName = f + "_" + new SimpleDateFormat("HH-mm_EEE").format(new Date(pt.time)); //$NON-NLS-1$						
						fout = new File(dir, fileName + ".gpx"); //$NON-NLS-1$
						int ind = 1;
						while (fout.exists()) {
							fout = new File(dir, fileName + "_" + (++ind) + ".gpx"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}

					String warn = GPXUtilities.writeGpxFile(fout, data.get(f), ctx);
					if (warn != null) {
						warnings.add(warn);
						return warnings;
					}
				}
			}
		}

		SQLiteDatabase db = getWritableDatabase();
		if (db != null && warnings.isEmpty()) {
			// remove all from db
			db.execSQL("DELETE FROM " + TRACK_NAME + " WHERE " + TRACK_COL_DATE + " <= ?", new Object[] { System.currentTimeMillis() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			db.execSQL("DELETE FROM " + POINT_NAME + " WHERE " + POINT_COL_DATE + " <= ?", new Object[] { System.currentTimeMillis() }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			// delete all
//			db.execSQL("DELETE FROM " + TRACK_NAME + " WHERE 1 = 1", new Object[] { }); //$NON-NLS-1$ //$NON-NLS-2$
//			db.execSQL("DELETE FROM " + POINT_NAME + " WHERE 1 = 1", new Object[] { }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		distance = 0;
		return warnings;
	}

	public Map<String, GPXFile> collectRecordedData() {
		Map<String, GPXFile> data = new LinkedHashMap<String, GPXFile>();
		SQLiteDatabase db = getReadableDatabase();
		if(db != null) {
			collectDBPoints(db, data);
			collectDBTracks(db, data);
		}
		return data;
	}

	private void collectDBPoints(SQLiteDatabase db, Map<String, GPXFile> dataTracks) {
		Cursor query = db.rawQuery("SELECT " + POINT_COL_LAT + "," + POINT_COL_LON + "," + POINT_COL_DATE + "," //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ POINT_COL_DESCRIPTION + " FROM " + POINT_NAME+" ORDER BY " + TRACK_COL_DATE +" ASC", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		if (query.moveToFirst()) {
			do {
				WptPt pt = new WptPt();
				pt.lat = query.getDouble(0);
				pt.lon = query.getDouble(1);
				long time = query.getLong(2);
				pt.time = time;
				pt.name = query.getString(3);
				
				String date = DateFormat.format("yyyy-MM-dd", time).toString(); //$NON-NLS-1$
				GPXFile gpx;
				if (dataTracks.containsKey(date)) {
					gpx = dataTracks.get(date);
				} else {
					gpx  = new GPXFile();
					dataTracks.put(date, gpx);
				}
				gpx.points.add(pt);

			} while (query.moveToNext());
		}
		query.close();
	}
	
	private void collectDBTracks(SQLiteDatabase db, Map<String, GPXFile> dataTracks) {
		Cursor query = db.rawQuery("SELECT " + TRACK_COL_LAT + "," + TRACK_COL_LON + "," + TRACK_COL_ALTITUDE + "," //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ TRACK_COL_SPEED + "," + TRACK_COL_HDOP + "," + TRACK_COL_DATE + " FROM " + TRACK_NAME +" ORDER BY " + TRACK_COL_DATE +" ASC", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		long previousTime = 0;
		long previousInterval = 0;
		TrkSegment segment = null;
		Track track = null;
		if (query.moveToFirst()) {
			do {
				WptPt pt = new WptPt();
				pt.lat = query.getDouble(0);
				pt.lon = query.getDouble(1);
				pt.ele = query.getDouble(2);
				pt.speed = query.getDouble(3);
				pt.hdop = query.getDouble(4);
				long time = query.getLong(5);
				pt.time = time;
				long currentInterval = Math.abs(time - previousTime);
				boolean newInterval = pt.lat == 0 && pt.lon == 0;
				
				if (track != null && !newInterval && (currentInterval < 6 * 60 * 1000 || currentInterval < 10 * previousInterval)) {
					// 6 minute - same segment
					segment.points.add(pt);
				} else if (track != null && currentInterval < 2 * 60 * 60 * 1000) {
					// 2 hour - same track
					segment = new TrkSegment();
					if(!newInterval) {
						segment.points.add(pt);
					}
					track.segments.add(segment);
				} else {
					// check if date the same - new track otherwise new file  
					track = new Track();
					segment = new TrkSegment();
					track.segments.add(segment);
					if(!newInterval) {
						segment.points.add(pt);
					}
					String date = DateFormat.format("yyyy-MM-dd", time).toString(); //$NON-NLS-1$
					if (dataTracks.containsKey(date)) {
						GPXFile gpx = dataTracks.get(date);
						gpx.tracks.add(track);
					} else {
						GPXFile file = new GPXFile();
						file.tracks.add(track);
						dataTracks.put(date, file);
					}
				}
				previousInterval = currentInterval;
				previousTime = time;
			} while (query.moveToNext());
		}
		query.close();
	}
	
	public void startNewSegment() {
		lastTimeUpdated = 0;
		lastPoint = null;
		execWithClose(updateScript, new Object[] { 0, 0, 0, 0, 0, System.currentTimeMillis()});
		addTrackPoint(null, true);
	}
	
	public void insertData(double lat, double lon, double alt, double speed, double hdop, long time, OsmandSettings settings){
		//* 1000 in next line seems to be wrong with new IntervalChooseDialog
		//if (time - lastTimeUpdated > settings.SAVE_TRACK_INTERVAL.get() * 1000) {
		if (time - lastTimeUpdated > settings.SAVE_TRACK_INTERVAL.get()) {
			execWithClose(updateScript, new Object[] { lat, lon, alt, speed, hdop, time });
			boolean newSegment = false;
			if (lastPoint == null || (time - lastTimeUpdated) > 180 * 1000) {
				lastPoint = new LatLon(lat, lon);
				newSegment = true;
			} else {
				float[] lastInterval = new float[1];
				Location.distanceBetween(lat, lon, lastPoint.getLatitude(), lastPoint.getLongitude(), lastInterval);
				distance += lastInterval[0];
				lastPoint = new LatLon(lat, lon);
			}
			lastTimeUpdated = time;
			if (settings.SHOW_CURRENT_GPX_TRACK.get()) {
				WptPt pt = new GPXUtilities.WptPt(lat, lon, time,
						alt, speed, hdop);
				addTrackPoint(pt, newSegment);
			}
		}
	}
	
	private void addTrackPoint(WptPt pt, boolean newSegment) {
		GPXFile file = ctx.getGpxFileToDisplay();
		if (file != null && ctx.getSettings().SHOW_CURRENT_GPX_TRACK.get()) {
			List<List<WptPt>> points = file.processedPointsToDisplay;
			if (points.size() == 0 || newSegment) {
				points.add(new ArrayList<WptPt>());
			}
			if (pt != null) {
				List<WptPt> last = points.get(points.size() - 1);
				last.add(pt);
			}
		}
	}
	
	public void insertPointData(double lat, double lon, long time, String description) {
		execWithClose(updatePointsScript, new Object[] { lat, lon, time, description });
	}
	
	private void execWithClose(String script, Object[] objects) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			if (db != null) {
				db.execSQL(script, objects);
			}
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}
	
	public float getDistance() {
		return distance;
	}
	
	public long getLastTimeUpdated() {
		return lastTimeUpdated;
	}

}
