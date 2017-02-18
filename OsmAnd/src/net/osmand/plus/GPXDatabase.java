package net.osmand.plus;

import net.osmand.IndexConstants;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.api.SQLiteAPI;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GPXDatabase {

	private static final String DB_NAME = "gpx_database";
	private static final int DB_VERSION = 1;
	private static final String GPX_TABLE_NAME = "gpxTable";
	private static final String GPX_COL_NAME = "fileName";
	private static final String GPX_COL_DIR = "fileDir";
	private static final String GPX_COL_TOTAL_DISTANCE = "totalDistance";
	private static final String GPX_COL_TOTAL_TRACKS = "totalTracks";
	private static final String GPX_COL_START_TIME = "startTime";
	private static final String GPX_COL_END_TIME = "endTime";
	private static final String GPX_COL_TIME_SPAN = "timeSpan";
	private static final String GPX_COL_TIME_MOVING = "timeMoving";
	private static final String GPX_COL_TOTAL_DISTANCE_MOVING = "totalDistanceMoving";

	private static final String GPX_COL_DIFF_ELEVATION_UP = "diffElevationUp";
	private static final String GPX_COL_DIFF_ELEVATION_DOWN = "diffElevationDown";
	private static final String GPX_COL_AVG_ELEVATION = "avgElevation";
	private static final String GPX_COL_MIN_ELEVATION = "minElevation";
	private static final String GPX_COL_MAX_ELEVATION = "maxElevation";

	private static final String GPX_COL_MAX_SPEED = "maxSpeed";
	private static final String GPX_COL_AVG_SPEED = "avgSpeed";

	private static final String GPX_COL_POINTS = "points";
	private static final String GPX_COL_WPT_POINTS = "wptPoints";

	private static final String GPX_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + GPX_TABLE_NAME + " (" +
			GPX_COL_NAME + " TEXT, " +
			GPX_COL_DIR + " TEXT, " +
			GPX_COL_TOTAL_DISTANCE + " double, " +
			GPX_COL_TOTAL_TRACKS + " int, " +
			GPX_COL_START_TIME + " long, " +
			GPX_COL_END_TIME + " long, " +
			GPX_COL_TIME_SPAN + " long, " +
			GPX_COL_TIME_MOVING + " long, " +
			GPX_COL_TOTAL_DISTANCE_MOVING + " double, " +

			GPX_COL_DIFF_ELEVATION_UP + " double, " +
			GPX_COL_DIFF_ELEVATION_DOWN + " double, " +
			GPX_COL_AVG_ELEVATION + " double, " +
			GPX_COL_MIN_ELEVATION + " double, " +
			GPX_COL_MAX_ELEVATION + " double, " +

			GPX_COL_MAX_SPEED + " double, " +
			GPX_COL_AVG_SPEED + " double, " +

			GPX_COL_POINTS + " int, " +
			GPX_COL_WPT_POINTS + " int);";

	private OsmandApplication context;

	public static class GpxDataItem {
		private File file;
		private GPXTrackAnalysis analysis;

		public GpxDataItem(File file, GPXTrackAnalysis analysis) {
			this.file = file;
			this.analysis = analysis;
		}

		public File getFile() {
			return file;
		}

		public GPXTrackAnalysis getAnalysis() {
			return analysis;
		}
	}

	public GPXDatabase(OsmandApplication app) {
		context = app;
	}

	private SQLiteAPI.SQLiteConnection openConnection(boolean readonly) {
		SQLiteAPI.SQLiteConnection conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
		if (conn.getVersion() == 0 || DB_VERSION != conn.getVersion()) {
			if (readonly) {
				conn.close();
				conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
			}
			if (conn.getVersion() == 0) {
				onCreate(conn);
			} else {
				onUpgrade(conn, conn.getVersion(), DB_VERSION);
			}
			conn.setVersion(DB_VERSION);

		}
		return conn;
	}

	public void onCreate(SQLiteAPI.SQLiteConnection db) {
		db.execSQL(GPX_TABLE_CREATE);
	}

	public void onUpgrade(SQLiteAPI.SQLiteConnection db, int oldVersion, int newVersion) {
		/*
		if (newVersion == 2) {
			db.execSQL(GPX_TABLE_CREATE);
			//...
		}
		*/
	}

	public boolean remove(GpxDataItem item) {
		SQLiteAPI.SQLiteConnection db = openConnection(false);
		if(db != null){
			try {
				db.execSQL("DELETE FROM " + GPX_TABLE_NAME + " WHERE " + GPX_COL_NAME + " = ?, " + GPX_COL_DIR + " = ?",
						new Object[] { item.file.getName(), item.file.getParentFile().getName() });
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean add(GpxDataItem item) {
		SQLiteAPI.SQLiteConnection db = openConnection(false);
		if(db != null){
			try {
				insert(item, db);
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	private void insert(GpxDataItem item, SQLiteAPI.SQLiteConnection db) {
		String fileName = item.file.getName();
		String fileDir = item.file.getParentFile().getName();
		GPXTrackAnalysis a = item.getAnalysis();
		db.execSQL(
				"INSERT INTO " + GPX_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[] { fileName, fileDir, a.totalDistance, a.totalTracks, a.startTime, a.endTime,
						a.timeSpan, a.timeMoving, a.totalDistanceMoving, a.diffElevationUp, a.diffElevationDown,
						a.avgElevation, a.minElevation, a.maxElevation, a.maxSpeed, a.avgSpeed, a.points, a.wptPoints });
	}

	public List<GpxDataItem> getItems() {
		List<GpxDataItem> items = new ArrayList<>();
		SQLiteAPI.SQLiteConnection db = openConnection(true);
		if(db != null){
			try {
				SQLiteAPI.SQLiteCursor query = db.rawQuery(
						"SELECT " + GPX_COL_NAME + ", " + GPX_COL_DIR + "," + GPX_COL_TOTAL_DISTANCE + ", " +
								GPX_COL_TOTAL_TRACKS + ", " + GPX_COL_START_TIME + ", " + GPX_COL_END_TIME + ", " +
								GPX_COL_TIME_SPAN + ", " + GPX_COL_TIME_MOVING + ", " + GPX_COL_TOTAL_DISTANCE_MOVING + ", " +
								GPX_COL_DIFF_ELEVATION_UP + ", " + GPX_COL_DIFF_ELEVATION_DOWN + ", " + GPX_COL_AVG_ELEVATION + ", " +
								GPX_COL_MIN_ELEVATION + ", " + GPX_COL_MAX_ELEVATION + ", " + GPX_COL_MAX_SPEED + ", " +
								GPX_COL_AVG_SPEED + ", " + GPX_COL_POINTS + ", " + GPX_COL_WPT_POINTS +
								" FROM " +	GPX_TABLE_NAME , null);

				if (query.moveToFirst()) {
					do {
						String fileName = query.getString(0);
						String fileDir = query.getString(1);
						float totalDistance = (float)query.getDouble(2);
						int totalTracks = (int)query.getInt(3);
						long startTime = query.getLong(4);
						long endTime = query.getLong(5);
						long timeSpan = query.getLong(6);
						long timeMoving = query.getLong(7);
						float totalDistanceMoving = (float)query.getDouble(8);
						double diffElevationUp = query.getDouble(9);
						double diffElevationDown = query.getDouble(10);
						double avgElevation = query.getDouble(11);
						double minElevation = query.getDouble(12);
						double maxElevation = query.getDouble(13);
						float maxSpeed = (float)query.getDouble(14);
						float avgSpeed = (float)query.getDouble(15);
						int points = (int)query.getInt(16);
						int wptPoints = (int)query.getInt(17);

						GPXTrackAnalysis a = new GPXTrackAnalysis();
						a.totalDistance = totalDistance;
						a.totalTracks = totalTracks;
						a.startTime = startTime;
						a.endTime = endTime;
						a.timeSpan = timeSpan;
						a.timeMoving = timeMoving;
						a.totalDistanceMoving = totalDistanceMoving;
						a.diffElevationUp = diffElevationUp;
						a.diffElevationDown = diffElevationDown;
						a.avgElevation = avgElevation;
						a.minElevation = minElevation;
						a.maxElevation = maxElevation;
						a.maxSpeed = maxSpeed;
						a.avgSpeed = avgSpeed;
						a.points = points;
						a.wptPoints = wptPoints;

						File dir;
						if (!Algorithms.isEmpty(fileDir)) {
							dir = new File(context.getAppPath(IndexConstants.GPX_INDEX_DIR), fileDir);
						} else {
							dir = context.getAppPath(IndexConstants.GPX_INDEX_DIR);
						}
						GpxDataItem item = new GpxDataItem(new File(dir, fileName), a);
						items.add(item);
					} while (query.moveToNext());

				}
				query.close();
			} finally {
				db.close();
			}
		}
		return items;
	}
}
