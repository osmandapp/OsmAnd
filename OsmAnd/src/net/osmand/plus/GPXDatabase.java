package net.osmand.plus;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.IndexConstants;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GPXDatabase {

	private static final String DB_NAME = "gpx_database";
	private static final int DB_VERSION = 9;
	private static final String GPX_TABLE_NAME = "gpxTable";
	private static final String GPX_COL_NAME = "fileName";
	private static final String GPX_COL_DIR = "fileDir";
	private static final String GPX_INDEX_NAME_DIR = "indexNameDir";

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

	private static final String GPX_COL_COLOR = "color";
	private static final String GPX_COL_FILE_LAST_MODIFIED_TIME = "fileLastModifiedTime";

	private static final String GPX_COL_SPLIT_TYPE = "splitType";
	private static final String GPX_COL_SPLIT_INTERVAL = "splitInterval";

	private static final String GPX_COL_API_IMPORTED = "apiImported";

	private static final String GPX_COL_WPT_CATEGORY_NAMES = "wptCategoryNames";

	private static final String GPX_COL_SHOW_AS_MARKERS = "showAsMarkers";

	public static final int GPX_SPLIT_TYPE_NO_SPLIT = -1;
	public static final int GPX_SPLIT_TYPE_DISTANCE = 1;
	public static final int GPX_SPLIT_TYPE_TIME = 2;

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
			GPX_COL_WPT_POINTS + " int, " +
			GPX_COL_COLOR + " TEXT, " +
			GPX_COL_FILE_LAST_MODIFIED_TIME + " long, " +
			GPX_COL_SPLIT_TYPE + " int, " +
			GPX_COL_SPLIT_INTERVAL + " double, " +
			GPX_COL_API_IMPORTED + " int, " + // 1 = true, 0 = false
			GPX_COL_WPT_CATEGORY_NAMES + " TEXT, " +
			GPX_COL_SHOW_AS_MARKERS + " int);"; // 1 = true, 0 = false

	private static final String GPX_TABLE_SELECT = "SELECT " +
			GPX_COL_NAME + ", " +
			GPX_COL_DIR + "," +
			GPX_COL_TOTAL_DISTANCE + ", " +
			GPX_COL_TOTAL_TRACKS + ", " +
			GPX_COL_START_TIME + ", " +
			GPX_COL_END_TIME + ", " +
			GPX_COL_TIME_SPAN + ", " +
			GPX_COL_TIME_MOVING + ", " +
			GPX_COL_TOTAL_DISTANCE_MOVING + ", " +
			GPX_COL_DIFF_ELEVATION_UP + ", " +
			GPX_COL_DIFF_ELEVATION_DOWN + ", " +
			GPX_COL_AVG_ELEVATION + ", " +
			GPX_COL_MIN_ELEVATION + ", " +
			GPX_COL_MAX_ELEVATION + ", " +
			GPX_COL_MAX_SPEED + ", " +
			GPX_COL_AVG_SPEED + ", " +
			GPX_COL_POINTS + ", " +
			GPX_COL_WPT_POINTS + ", " +
			GPX_COL_COLOR + ", " +
			GPX_COL_FILE_LAST_MODIFIED_TIME + ", " +
			GPX_COL_SPLIT_TYPE + ", " +
			GPX_COL_SPLIT_INTERVAL + ", " +
			GPX_COL_API_IMPORTED + ", " +
			GPX_COL_WPT_CATEGORY_NAMES + ", " +
			GPX_COL_SHOW_AS_MARKERS +
			" FROM " +	GPX_TABLE_NAME;

	private static final String GPX_TABLE_UPDATE_ANALYSIS = "UPDATE " +
			GPX_TABLE_NAME + " SET " +
			GPX_COL_TOTAL_DISTANCE + " = ?, " +
			GPX_COL_TOTAL_TRACKS + " = ?, " +
			GPX_COL_START_TIME + " = ?, " +
			GPX_COL_END_TIME + " = ?, " +
			GPX_COL_TIME_SPAN + " = ?, " +
			GPX_COL_TIME_MOVING + " = ?, " +
			GPX_COL_TOTAL_DISTANCE_MOVING + " = ?, " +
			GPX_COL_DIFF_ELEVATION_UP + " = ?, " +
			GPX_COL_DIFF_ELEVATION_DOWN + " = ?, " +
			GPX_COL_AVG_ELEVATION + " = ?, " +
			GPX_COL_MIN_ELEVATION + " = ?, " +
			GPX_COL_MAX_ELEVATION + " = ?, " +
			GPX_COL_MAX_SPEED + " = ?, " +
			GPX_COL_AVG_SPEED + " = ?, " +
			GPX_COL_POINTS + " = ?, " +
			GPX_COL_WPT_POINTS + " = ?, " +
			GPX_COL_FILE_LAST_MODIFIED_TIME + " = ?, " +
			GPX_COL_WPT_CATEGORY_NAMES + " = ? ";

	private OsmandApplication context;

	public static class GpxDataItem {
		private File file;
		private GPXTrackAnalysis analysis;
		private int color;
		private long fileLastModifiedTime;
		private int splitType;
		private double splitInterval;
		private boolean apiImported;
		private boolean showAsMarkers;

		public GpxDataItem(File file, GPXTrackAnalysis analysis) {
			this.file = file;
			this.analysis = analysis;
		}

		public GpxDataItem(File file, int color) {
			this.file = file;
			this.color = color;
		}

		public File getFile() {
			return file;
		}

		@Nullable
		public GPXTrackAnalysis getAnalysis() {
			return analysis;
		}

		public int getColor() {
			return color;
		}

		public long getFileLastModifiedTime() {
			return fileLastModifiedTime;
		}

		public int getSplitType() {
			return splitType;
		}

		public double getSplitInterval() {
			return splitInterval;
		}

		public boolean isApiImported() {
			return apiImported;
		}

		public void setApiImported(boolean apiImported) {
			this.apiImported = apiImported;
		}

		public boolean isShowAsMarkers() {
			return showAsMarkers;
		}

		public void setShowAsMarkers(boolean showAsMarkers) {
			this.showAsMarkers = showAsMarkers;
		}
	}

	public GPXDatabase(OsmandApplication app) {
		context = app;
	}

	private SQLiteConnection openConnection(boolean readonly) {
		SQLiteConnection conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
		if (conn.getVersion() < DB_VERSION) {
			if (readonly) {
				conn.close();
				conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, false);
			}
			int version = conn.getVersion();
			conn.setVersion(DB_VERSION);
			if (version == 0) {
				onCreate(conn);
			} else {
				onUpgrade(conn, version, DB_VERSION);
			}
		}
		return conn;
	}

	private void onCreate(SQLiteConnection db) {
		db.execSQL(GPX_TABLE_CREATE);
	}

	private void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_COLOR + " TEXT");
		}
		if (oldVersion < 3) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_FILE_LAST_MODIFIED_TIME + " long");
		}
		
		if (oldVersion < 4) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_SPLIT_TYPE + " int");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_SPLIT_INTERVAL + " double");
		}

		if (oldVersion < 5) {
			boolean colorColumnExists = false;
			boolean fileLastModifiedTimeColumnExists = false;
			boolean splitTypeColumnExists = false;
			boolean splitIntervalColumnExists = false;
			SQLiteCursor cursor = db.rawQuery("PRAGMA table_info(" + GPX_TABLE_NAME + ")", null);
			if (cursor.moveToFirst()) {
				do {
					String columnName = cursor.getString(1);
					if (!colorColumnExists && columnName.equals(GPX_COL_COLOR)) {
						colorColumnExists = true;
					} else if (!fileLastModifiedTimeColumnExists && columnName.equals(GPX_COL_FILE_LAST_MODIFIED_TIME)) {
						fileLastModifiedTimeColumnExists = true;
					} else if (!splitTypeColumnExists && columnName.equals(GPX_COL_SPLIT_TYPE)) {
						splitTypeColumnExists = true;
					} else if (!splitIntervalColumnExists && columnName.equals(GPX_COL_SPLIT_INTERVAL)) {
						splitIntervalColumnExists = true;
					}
				} while (cursor.moveToNext());
			}
			cursor.close();
			if (!colorColumnExists) {
				db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_COLOR + " TEXT");
			}
			if (!fileLastModifiedTimeColumnExists) {
				db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_FILE_LAST_MODIFIED_TIME + " long");
				List<GpxDataItem> items = getItems();
				for (GpxDataItem item : items) {
					updateLastModifiedTime(item);
				}
			}
			if (!splitTypeColumnExists) {
				db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_SPLIT_TYPE + " int");
			}
			if (!splitIntervalColumnExists) {
				db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_SPLIT_INTERVAL + " double");
			}
		}

		if (oldVersion < 6) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_API_IMPORTED + " int");
			db.execSQL("UPDATE " + GPX_TABLE_NAME +
					" SET " + GPX_COL_API_IMPORTED + " = ? " +
					"WHERE " + GPX_COL_API_IMPORTED + " IS NULL", new Object[]{0});
		}

		if (oldVersion < 7) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_WPT_CATEGORY_NAMES + " TEXT");
		}

		if (oldVersion < 8) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_SHOW_AS_MARKERS + " int");
			db.execSQL("UPDATE " + GPX_TABLE_NAME +
					" SET " + GPX_COL_SHOW_AS_MARKERS + " = ? " +
					"WHERE " + GPX_COL_SHOW_AS_MARKERS + " IS NULL", new Object[]{0});
		}
		db.execSQL("CREATE INDEX IF NOT EXISTS " + GPX_INDEX_NAME_DIR + " ON " + GPX_TABLE_NAME + " (" + GPX_COL_NAME + ", " + GPX_COL_DIR + ");");
	}

	private boolean updateLastModifiedTime(GpxDataItem item) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				String fileName = getFileName(item.file);
				String fileDir = getFileDir(item.file);
				long fileLastModifiedTime = item.file.lastModified();
				db.execSQL("UPDATE " + GPX_TABLE_NAME + " SET " +
								GPX_COL_FILE_LAST_MODIFIED_TIME + " = ? " +
								" WHERE " + GPX_COL_NAME + " = ? AND " + GPX_COL_DIR + " = ?",
						new Object[] { fileLastModifiedTime, fileName, fileDir });
				item.fileLastModifiedTime = fileLastModifiedTime;
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean rename(File currentFile, File newFile) {
		SQLiteConnection db = openConnection(false);
		if (db != null){
			try {
				String newFileName = getFileName(newFile);
				String newFileDir = getFileDir(newFile);
				String fileName = getFileName(currentFile);
				String fileDir = getFileDir(currentFile);
				db.execSQL("UPDATE " + GPX_TABLE_NAME + " SET " +
								GPX_COL_NAME + " = ? " + ", " +
								GPX_COL_DIR + " = ? " +
						" WHERE " + GPX_COL_NAME + " = ? AND " + GPX_COL_DIR + " = ?",
						new Object[] { newFileName, newFileDir, fileName, fileDir });
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean updateColor(GpxDataItem item, int color) {
		SQLiteConnection db = openConnection(false);
		if (db != null){
			try {
				String fileName = getFileName(item.file);
				String fileDir = getFileDir(item.file);
				db.execSQL("UPDATE " + GPX_TABLE_NAME + " SET " +
								GPX_COL_COLOR + " = ? " +
								" WHERE " + GPX_COL_NAME + " = ? AND " + GPX_COL_DIR + " = ?",
						new Object[] { (color == 0 ? "" : Algorithms.colorToString(color)), fileName, fileDir });
				item.color = color;
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean updateShowAsMarkers(GpxDataItem item, boolean showAsMarkers) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				String fileName = getFileName(item.file);
				String fileDir = getFileDir(item.file);
				db.execSQL("UPDATE " + GPX_TABLE_NAME + " SET " +
								GPX_COL_SHOW_AS_MARKERS + " = ? " +
								" WHERE " + GPX_COL_NAME + " = ? AND " + GPX_COL_DIR + " = ?",
						new Object[]{showAsMarkers ? 1 : 0, fileName, fileDir});
				item.setShowAsMarkers(showAsMarkers);
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean updateSplit(@NonNull GpxDataItem item, int splitType, double splitInterval) {
		SQLiteConnection db = openConnection(false);
		if (db != null){
			try {
				String fileName = getFileName(item.file);
				String fileDir = getFileDir(item.file);
				db.execSQL("UPDATE " + GPX_TABLE_NAME + " SET " +
								GPX_COL_SPLIT_TYPE + " = ?, " +
								GPX_COL_SPLIT_INTERVAL + " = ? " +
								" WHERE " + GPX_COL_NAME + " = ? AND " + GPX_COL_DIR + " = ?",
						new Object[] { splitType, splitInterval, fileName, fileDir });
				item.splitType = splitType;
				item.splitInterval = splitInterval;
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean remove(File file) {
		SQLiteConnection db = openConnection(false);
		if (db != null){
			try {
				String fileName = getFileName(file);
				String fileDir = getFileDir(file);
				db.execSQL("DELETE FROM " + GPX_TABLE_NAME + " WHERE " + GPX_COL_NAME + " = ? AND " + GPX_COL_DIR + " = ?",
						new Object[] { fileName, fileDir });
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean remove(GpxDataItem item) {
		return remove(item.file);
	}

	public boolean add(GpxDataItem item) {
		SQLiteConnection db = openConnection(false);
		if (db != null){
			try {
				insert(item, db);
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	private String getFileName(File itemFile) {
		return itemFile.getName();
	}

	private String getFileDir(File itemFile) {
		return itemFile.getParentFile() == null ||
				itemFile.getParentFile().equals(context.getAppPath(IndexConstants.GPX_INDEX_DIR)) ?
				"" : itemFile.getParentFile().getName();
	}

	private void insert(GpxDataItem item, SQLiteConnection db) {
		String fileName = getFileName(item.file);
		String fileDir = getFileDir(item.file);
		GPXTrackAnalysis a = item.getAnalysis();
		String color;
		if (item.color == 0) {
			color = "";
		} else {
			color = Algorithms.colorToString(item.color);
		}
		if (a != null) {
			db.execSQL(
					"INSERT INTO " + GPX_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					new Object[]{ fileName, fileDir, a.totalDistance, a.totalTracks, a.startTime, a.endTime,
							a.timeSpan, a.timeMoving, a.totalDistanceMoving, a.diffElevationUp, a.diffElevationDown,
							a.avgElevation, a.minElevation, a.maxElevation, a.maxSpeed, a.avgSpeed, a.points, a.wptPoints,
							color, item.file.lastModified(), item.splitType, item.splitInterval, item.apiImported ? 1 : 0,
							Algorithms.encodeStringSet(item.analysis.wptCategoryNames), item.showAsMarkers ? 1 : 0});
		} else {
			db.execSQL("INSERT INTO " + GPX_TABLE_NAME + "(" +
							GPX_COL_NAME + ", " +
							GPX_COL_DIR + ", " +
							GPX_COL_COLOR + ", " +
							GPX_COL_FILE_LAST_MODIFIED_TIME + ", " +
							GPX_COL_SPLIT_TYPE + ", " +
							GPX_COL_SPLIT_INTERVAL + ", " +
							GPX_COL_API_IMPORTED + ", " +
							GPX_COL_SHOW_AS_MARKERS +
							") VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
					new Object[]{fileName, fileDir, color, 0, item.splitType, item.splitInterval, item.apiImported ? 1 : 0, item.showAsMarkers ? 1 : 0});
		}
	}

	public boolean updateAnalysis(GpxDataItem item, GPXTrackAnalysis a) {
		if (a == null) {
			return false;
		}
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				String fileName = getFileName(item.file);
				String fileDir = getFileDir(item.file);
				db.execSQL(GPX_TABLE_UPDATE_ANALYSIS + " WHERE " + GPX_COL_NAME + " = ? AND " + GPX_COL_DIR + " = ?",
						new Object[]{ a.totalDistance, a.totalTracks, a.startTime, a.endTime,
								a.timeSpan, a.timeMoving, a.totalDistanceMoving, a.diffElevationUp,
								a.diffElevationDown, a.avgElevation, a.minElevation, a.maxElevation,
								a.maxSpeed, a.avgSpeed, a.points, a.wptPoints, item.file.lastModified(),
								Algorithms.encodeStringSet(a.wptCategoryNames), fileName, fileDir });
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean clearAnalysis(GpxDataItem item) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				Object[] bindArgs = new Object[20];
				bindArgs[16] = 0;
				bindArgs[18] = getFileName(item.file);
				bindArgs[19] = getFileDir(item.file);
				db.execSQL(GPX_TABLE_UPDATE_ANALYSIS + " WHERE " + GPX_COL_NAME + " = ? AND " + GPX_COL_DIR + " = ?", bindArgs);
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	private GpxDataItem readItem(SQLiteCursor query) {
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
		String color = query.getString(18);
		long fileLastModifiedTime = query.getLong(19);
		int splitType = (int)query.getInt(20);
		double splitInterval = query.getDouble(21);
		boolean apiImported = query.getInt(22) == 1;
		String wptCategoryNames = query.getString(23);
		boolean showAsMarkers = query.getInt(24) == 1;

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
		a.minSpeed = maxSpeed;
		a.maxSpeed = maxSpeed;
		a.avgSpeed = avgSpeed;
		a.points = points;
		a.wptPoints = wptPoints;
		if (wptCategoryNames != null) {
			a.wptCategoryNames = Algorithms.decodeStringSet(wptCategoryNames);
		}

		File dir;
		if (!Algorithms.isEmpty(fileDir)) {
			dir = new File(context.getAppPath(IndexConstants.GPX_INDEX_DIR), fileDir);
		} else {
			dir = context.getAppPath(IndexConstants.GPX_INDEX_DIR);
		}
		GpxDataItem item = new GpxDataItem(new File(dir, fileName), a);
		try {
			item.color = Algorithms.isEmpty(color) ? 0 : Algorithms.parseColor(color);
		} catch (IllegalArgumentException e) {
			item.color = 0;
		}
		item.fileLastModifiedTime = fileLastModifiedTime;
		item.splitType = splitType;
		item.splitInterval = splitInterval;
		item.apiImported = apiImported;
		item.showAsMarkers = showAsMarkers;
		return item;
	}

	public List<GpxDataItem> getItems() {
		List<GpxDataItem> items = new ArrayList<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(GPX_TABLE_SELECT, null);
				if (query != null && query.moveToFirst()) {
					do {
						items.add(readItem(query));
					} while (query.moveToNext());
				}
				if (query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return items;
	}

	@Nullable
	public GpxDataItem getItem(File file) {
		GpxDataItem result = null;
		SQLiteConnection db = openConnection(true);
		if (db != null){
			try {
				String fileName = getFileName(file);
				String fileDir = getFileDir(file);
				SQLiteCursor query = db.rawQuery(GPX_TABLE_SELECT + " WHERE " + GPX_COL_NAME + " = ? AND " +
						GPX_COL_DIR + " = ?", new String[] { fileName, fileDir });
				if ( query != null && query.moveToFirst()) {
					result = readItem(query);
				}
				if (query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return result;
	}
}
