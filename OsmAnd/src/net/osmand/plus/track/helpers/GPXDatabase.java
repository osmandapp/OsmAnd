package net.osmand.plus.track.helpers;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.util.Pair;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GPXDatabase {

	private static final int DB_VERSION = 16;
	private static final String DB_NAME = "gpx_database";

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
	private static final String GPX_COL_FILE_LAST_UPLOADED_TIME = "fileLastUploadedTime";
	private static final String GPX_COL_FILE_CREATION_TIME = "fileCreationTime";

	private static final String GPX_COL_SPLIT_TYPE = "splitType";
	private static final String GPX_COL_SPLIT_INTERVAL = "splitInterval";

	private static final String GPX_COL_API_IMPORTED = "apiImported";

	private static final String GPX_COL_WPT_CATEGORY_NAMES = "wptCategoryNames";

	private static final String GPX_COL_SHOW_AS_MARKERS = "showAsMarkers";

	private static final String GPX_COL_JOIN_SEGMENTS = "joinSegments";

	private static final String GPX_COL_SHOW_ARROWS = "showArrows";

	private static final String GPX_COL_SHOW_START_FINISH = "showStartFinish";

	private static final String GPX_COL_WIDTH = "width";

	private static final String GPX_COL_GRADIENT_SPEED_COLOR = "gradientSpeedColor";

	private static final String GPX_COL_GRADIENT_ALTITUDE_COLOR = "gradientAltitudeColor";

	private static final String GPX_COL_GRADIENT_SLOPE_COLOR = "gradientSlopeColor";

	private static final String GPX_COL_COLORING_TYPE = "gradientScaleType";

	private static final String GPX_COL_SMOOTHING_THRESHOLD = "smoothingThreshold";
	private static final String GPX_COL_MIN_FILTER_SPEED = "minFilterSpeed";
	private static final String GPX_COL_MAX_FILTER_SPEED = "maxFilterSpeed";
	private static final String GPX_COL_MIN_FILTER_ALTITUDE = "minFilterAltitude";
	private static final String GPX_COL_MAX_FILTER_ALTITUDE = "maxFilterAltitude";
	private static final String GPX_COL_MAX_FILTER_HDOP = "maxFilterHdop";
	private static final String GPX_COL_START_LAT = "startLat";
	private static final String GPX_COL_START_LON = "startLon";
	private static final String GPX_COL_NEAREST_CITY_NAME = "nearestCityName";

	private static final String TMP_NAME_COLUMN_COUNT = "itemsCount";
	private static final String TMP_NAME_COLUMN_NOT_NULL = "nonnull";

	public static final long UNKNOWN_TIME_THRESHOLD = 10;

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
			GPX_COL_FILE_LAST_UPLOADED_TIME + " long, " +
			GPX_COL_FILE_CREATION_TIME + " long, " +
			GPX_COL_SPLIT_TYPE + " int, " +
			GPX_COL_SPLIT_INTERVAL + " double, " +
			GPX_COL_API_IMPORTED + " int, " + // 1 = true, 0 = false
			GPX_COL_WPT_CATEGORY_NAMES + " TEXT, " +
			GPX_COL_SHOW_AS_MARKERS + " int, " + // 1 = true, 0 = false
			GPX_COL_JOIN_SEGMENTS + " int, " + // 1 = true, 0 = false
			GPX_COL_SHOW_ARROWS + " int, " + // 1 = true, 0 = false
			GPX_COL_SHOW_START_FINISH + " int, " + // 1 = true, 0 = false
			GPX_COL_WIDTH + " TEXT, " +
			GPX_COL_GRADIENT_SPEED_COLOR + " TEXT, " +
			GPX_COL_GRADIENT_ALTITUDE_COLOR + " TEXT, " +
			GPX_COL_GRADIENT_SLOPE_COLOR + " TEXT, " +
			GPX_COL_COLORING_TYPE + " TEXT, " +
			GPX_COL_SMOOTHING_THRESHOLD + " double, " +
			GPX_COL_MIN_FILTER_SPEED + " double, " +
			GPX_COL_MAX_FILTER_SPEED + " double, " +
			GPX_COL_MIN_FILTER_ALTITUDE + " double, " +
			GPX_COL_MAX_FILTER_ALTITUDE + " double, " +
			GPX_COL_MAX_FILTER_HDOP + " double, " +
			GPX_COL_START_LAT + " double, " +
			GPX_COL_START_LON + " double, " +
			GPX_COL_NEAREST_CITY_NAME + " TEXT);";

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
			GPX_COL_FILE_LAST_UPLOADED_TIME + ", " +
			GPX_COL_FILE_CREATION_TIME + ", " +
			GPX_COL_SPLIT_TYPE + ", " +
			GPX_COL_SPLIT_INTERVAL + ", " +
			GPX_COL_API_IMPORTED + ", " +
			GPX_COL_WPT_CATEGORY_NAMES + ", " +
			GPX_COL_SHOW_AS_MARKERS + ", " +
			GPX_COL_JOIN_SEGMENTS + ", " +
			GPX_COL_SHOW_ARROWS + ", " +
			GPX_COL_SHOW_START_FINISH + ", " +
			GPX_COL_WIDTH + ", " +
			GPX_COL_GRADIENT_SPEED_COLOR + ", " +
			GPX_COL_GRADIENT_ALTITUDE_COLOR + ", " +
			GPX_COL_GRADIENT_SLOPE_COLOR + ", " +
			GPX_COL_COLORING_TYPE + ", " +
			GPX_COL_SMOOTHING_THRESHOLD + ", " +
			GPX_COL_MIN_FILTER_SPEED + ", " +
			GPX_COL_MAX_FILTER_SPEED + ", " +
			GPX_COL_MIN_FILTER_ALTITUDE + ", " +
			GPX_COL_MAX_FILTER_ALTITUDE + ", " +
			GPX_COL_MAX_FILTER_HDOP + ", " +
			GPX_COL_START_LAT + ", " +
			GPX_COL_START_LON + ", " +
			GPX_COL_NEAREST_CITY_NAME +
			" FROM " + GPX_TABLE_NAME;

	private static final String GPX_UPDATE_PARAMETERS_START = "UPDATE " + GPX_TABLE_NAME + " SET ";
	private static final String GPX_FIND_BY_NAME_AND_DIR = " WHERE " + GPX_COL_NAME + " = ? AND " + GPX_COL_DIR + " = ?";

	private static final String GPX_NEAREST_CITY_LIST = "SELECT DISTINCT " +
			GPX_COL_NEAREST_CITY_NAME + " FROM " + GPX_TABLE_NAME +
			" WHERE " + GPX_COL_NEAREST_CITY_NAME + " NOT NULL";

	private static final String GPX_TRACK_COLORS_LIST = "SELECT DISTINCT " +
			GPX_COL_COLOR + " FROM " + GPX_TABLE_NAME +
			" WHERE " + GPX_COL_COLOR + " <> ''";

	private static final String GPX_MIN_CREATE_DATE = "SELECT " +
			"MIN(" + GPX_COL_FILE_CREATION_TIME + ") " +
			" FROM " + GPX_TABLE_NAME + " WHERE " + GPX_COL_FILE_CREATION_TIME +
			" > " + UNKNOWN_TIME_THRESHOLD;

	private static final String GPX_MAX_TRACK_DURATION = "SELECT " +
			"MAX(" + GPX_COL_TOTAL_DISTANCE + ") " +
			" FROM " + GPX_TABLE_NAME;

	private static final String GPX_TRACK_FOLDERS_COLLECTION = "SELECT " +
			GPX_COL_DIR + ", count (*) as " + TMP_NAME_COLUMN_COUNT +
			" FROM " + GPX_TABLE_NAME +
			" group by " + GPX_COL_DIR +
			" ORDER BY " + GPX_COL_DIR + " ASC";

	private static final String GPX_TRACK_NEAREST_CITIES_COLLECTION = "SELECT " +
			GPX_COL_NEAREST_CITY_NAME + ", count (*) as " + TMP_NAME_COLUMN_COUNT +
			" FROM " + GPX_TABLE_NAME +
			" WHERE " + GPX_COL_NEAREST_CITY_NAME + " NOT NULL" + " AND " +
			GPX_COL_NEAREST_CITY_NAME + " <> '' " +
			" group by " + GPX_COL_NEAREST_CITY_NAME +
			" ORDER BY " + TMP_NAME_COLUMN_COUNT + " DESC";

	private static final String GPX_TRACK_COLORS_COLLECTION = "SELECT DISTINCT " +
			"case when " + GPX_COL_COLOR + " is null then '' else " + GPX_COL_COLOR + " end as " + TMP_NAME_COLUMN_NOT_NULL + ", " +
			"count (*) as " + TMP_NAME_COLUMN_COUNT +
			" FROM " + GPX_TABLE_NAME +
			" group by " + TMP_NAME_COLUMN_NOT_NULL +
			" ORDER BY " + TMP_NAME_COLUMN_COUNT + " DESC";

	private static final String GPX_TRACK_WIDTH_COLLECTION = "SELECT DISTINCT " +
			"case when " + GPX_COL_WIDTH + " is null then '' else " + GPX_COL_WIDTH + " end as " + TMP_NAME_COLUMN_NOT_NULL + ", " +
			"count (*) as " + TMP_NAME_COLUMN_COUNT +
			" FROM " + GPX_TABLE_NAME +
			" group by " + TMP_NAME_COLUMN_NOT_NULL +
			" ORDER BY " + TMP_NAME_COLUMN_COUNT + " DESC";

	private final OsmandApplication app;

	GPXDatabase(@NonNull OsmandApplication app) {
		this.app = app;
		// init database
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			db.close();
		}
	}

	SQLiteConnection openConnection(boolean readonly) {
		SQLiteConnection conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
		if (conn == null) {
			return null;
		}
		if (conn.getVersion() < DB_VERSION) {
			if (readonly) {
				conn.close();
				conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, false);
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
		db.execSQL("CREATE INDEX IF NOT EXISTS " + GPX_INDEX_NAME_DIR + " ON " + GPX_TABLE_NAME + " (" + GPX_COL_NAME + ", " + GPX_COL_DIR + ");");
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
					"WHERE " + GPX_COL_API_IMPORTED + " IS NULL", new Object[] {0});
		}

		if (oldVersion < 7) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_WPT_CATEGORY_NAMES + " TEXT");
		}

		if (oldVersion < 8) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_SHOW_AS_MARKERS + " int");
			db.execSQL("UPDATE " + GPX_TABLE_NAME +
					" SET " + GPX_COL_SHOW_AS_MARKERS + " = ? " +
					"WHERE " + GPX_COL_SHOW_AS_MARKERS + " IS NULL", new Object[] {0});
		}
		if (oldVersion < 10) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_JOIN_SEGMENTS + " int");
			db.execSQL("UPDATE " + GPX_TABLE_NAME +
					" SET " + GPX_COL_JOIN_SEGMENTS + " = ? " +
					"WHERE " + GPX_COL_JOIN_SEGMENTS + " IS NULL", new Object[] {0});
		}
		if (oldVersion < 11) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_SHOW_ARROWS + " int");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_SHOW_START_FINISH + " int");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_WIDTH + " TEXT");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_GRADIENT_SPEED_COLOR + " TEXT");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_GRADIENT_ALTITUDE_COLOR + " TEXT");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_GRADIENT_SLOPE_COLOR + " TEXT");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_COLORING_TYPE + " TEXT");

			db.execSQL(GPX_UPDATE_PARAMETERS_START + GPX_COL_SHOW_ARROWS + " = ? " +
					"WHERE " + GPX_COL_SHOW_ARROWS + " IS NULL", new Object[] {0});
			db.execSQL(GPX_UPDATE_PARAMETERS_START + GPX_COL_SHOW_START_FINISH + " = ? " +
					"WHERE " + GPX_COL_SHOW_START_FINISH + " IS NULL", new Object[] {1});
		}
		if (oldVersion < 12) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_FILE_LAST_UPLOADED_TIME + " long");
		}
		if (oldVersion < 13) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_SMOOTHING_THRESHOLD + " double");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_MIN_FILTER_SPEED + " double");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_MAX_FILTER_SPEED + " double");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_MIN_FILTER_ALTITUDE + " double");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_MAX_FILTER_ALTITUDE + " double");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_MAX_FILTER_HDOP + " double");
		}
		if (oldVersion < 14) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_START_LAT + " double");
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_START_LON + " double");
		}
		if (oldVersion < 15) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_NEAREST_CITY_NAME + " TEXT");
		}
		if (oldVersion < 16) {
			db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + GPX_COL_FILE_CREATION_TIME + " long");
		}
		db.execSQL("CREATE INDEX IF NOT EXISTS " + GPX_INDEX_NAME_DIR + " ON " + GPX_TABLE_NAME + " (" + GPX_COL_NAME + ", " + GPX_COL_DIR + ");");
	}

	private boolean updateGpxParameters(@NonNull Map<String, Object> rowsToUpdate, @NonNull Map<String, Object> rowsToSearch) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				return updateGpxParameters(db, rowsToUpdate, rowsToSearch);
			} finally {
				db.close();
			}
		}
		return false;
	}

	private boolean updateGpxParameters(@NonNull SQLiteConnection db, @NonNull Map<String, Object> rowsToUpdate, @NonNull Map<String, Object> rowsToSearch) {
		Pair<String, Object[]> pair = AndroidUtils.createDbUpdateQuery(GPX_TABLE_NAME, rowsToUpdate, rowsToSearch);
		db.execSQL(pair.first, pair.second);
		return true;
	}

	@NonNull
	private Map<String, Object> getRowsToSearch(@NonNull File file) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put(GPX_COL_NAME, getFileName(file));
		map.put(GPX_COL_DIR, getFileDir(file));
		return map;
	}

	private void updateLastModifiedTime(@NonNull GpxDataItem item) {
		File file = item.getFile();
		long lastModified = file.lastModified();

		Map<String, Object> map = Collections.singletonMap(GPX_COL_FILE_LAST_MODIFIED_TIME, lastModified);
		boolean success = updateGpxParameters(map, getRowsToSearch(file));
		if (success) {
			item.setFileLastModifiedTime(lastModified);
		}
	}

	public boolean updateLastUploadedTime(@NonNull GpxDataItem item, long lastUploadedTime) {
		Map<String, Object> map = Collections.singletonMap(GPX_COL_FILE_LAST_UPLOADED_TIME, lastUploadedTime);
		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setFileLastUploadedTime(lastUploadedTime);
		}
		return success;
	}

	public boolean updateCreateTime(@NonNull GpxDataItem item, long creationTime) {
		Map<String, Object> map = Collections.singletonMap(GPX_COL_FILE_CREATION_TIME, creationTime);
		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setFileCreationTime(creationTime);
		}
		return success;
	}

	public boolean rename(@NonNull File currentFile, @NonNull File newFile) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put(GPX_COL_NAME, getFileName(newFile));
		map.put(GPX_COL_DIR, getFileDir(newFile));

		return updateGpxParameters(map, getRowsToSearch(currentFile));
	}

	public boolean updateColor(@NonNull GpxDataItem item, @ColorInt int color) {
		Map<String, Object> map = Collections.singletonMap(GPX_COL_COLOR, (color == 0 ? "" : Algorithms.colorToString(color)));
		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setColor(color);
		}
		return success;
	}

	public boolean updateColoringType(@NonNull GpxDataItem item, @Nullable String coloringType) {
		Map<String, Object> map = Collections.singletonMap(GPX_COL_COLORING_TYPE, coloringType);
		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setColoringType(coloringType);
		}
		return success;
	}

	public boolean updateNearestCityName(@NonNull GpxDataItem item, @Nullable String cityName) {
		Map<String, Object> map = Collections.singletonMap(GPX_COL_NEAREST_CITY_NAME, cityName);
		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setNearestCityName(cityName);
		}
		return success;
	}

	public boolean updateShowArrows(GpxDataItem item, boolean showArrows) {
		Map<String, Object> map = Collections.singletonMap(GPX_COL_SHOW_ARROWS, showArrows ? 1 : 0);
		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setShowArrows(showArrows);
		}
		return success;
	}

	public boolean updateShowStartFinish(@NonNull GpxDataItem item, boolean showStartFinish) {
		Map<String, Object> map = Collections.singletonMap(GPX_COL_SHOW_START_FINISH, showStartFinish ? 1 : 0);
		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setShowStartFinish(showStartFinish);
		}
		return success;
	}

	public boolean updateWidth(@NonNull GpxDataItem item, @NonNull String width) {
		Map<String, Object> map = Collections.singletonMap(GPX_COL_WIDTH, width);
		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setWidth(width);
		}
		return success;
	}

	public boolean updateShowAsMarkers(@NonNull GpxDataItem item, boolean showAsMarkers) {
		Map<String, Object> map = Collections.singletonMap(GPX_COL_SHOW_AS_MARKERS, showAsMarkers ? 1 : 0);
		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setShowAsMarkers(showAsMarkers);
		}
		return success;
	}

	public boolean updateImportedByApi(@NonNull GpxDataItem item, boolean importedByApi) {
		Map<String, Object> map = Collections.singletonMap(GPX_COL_API_IMPORTED, importedByApi ? 1 : 0);
		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setImportedByApi(importedByApi);
		}
		return success;
	}

	public boolean updateJoinSegments(@NonNull GpxDataItem item, boolean joinSegments) {
		Map<String, Object> map = Collections.singletonMap(GPX_COL_JOIN_SEGMENTS, joinSegments ? 1 : 0);
		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setJoinSegments(joinSegments);
		}
		return success;
	}

	public boolean updateSplit(@NonNull GpxDataItem item, int splitType, double splitInterval) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put(GPX_COL_SPLIT_TYPE, splitType);
		map.put(GPX_COL_SPLIT_INTERVAL, splitInterval);

		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setSplitType(splitType);
			item.setSplitInterval(splitInterval);
		}
		return success;
	}

	public boolean updateGpsFiltersConfig(@NonNull GpxDataItem item, double smoothingThreshold,
	                                      double minSpeed, double maxSpeed, double minAltitude,
	                                      double maxAltitude, double maxHdop) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put(GPX_COL_SMOOTHING_THRESHOLD, smoothingThreshold);
		map.put(GPX_COL_MIN_FILTER_SPEED, minSpeed);
		map.put(GPX_COL_MAX_FILTER_SPEED, maxSpeed);
		map.put(GPX_COL_MIN_FILTER_ALTITUDE, minAltitude);
		map.put(GPX_COL_MAX_FILTER_ALTITUDE, maxAltitude);
		map.put(GPX_COL_MAX_FILTER_HDOP, maxHdop);

		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setSmoothingThreshold(smoothingThreshold);
			item.setMinFilterSpeed(minSpeed);
			item.setMaxFilterSpeed(maxSpeed);
			item.setMinFilterAltitude(minAltitude);
			item.setMaxFilterAltitude(maxAltitude);
			item.setMaxFilterHdop(maxHdop);
		}
		return success;
	}

	public boolean updateAppearance(@NonNull GpxDataItem item, int color, @NonNull String width,
	                                boolean showArrows, boolean showStartFinish, int splitType,
	                                double splitInterval, @Nullable String coloringType) {
		Map<String, Object> map = new LinkedHashMap<>();

		map.put(GPX_COL_COLOR, (color == 0 ? "" : Algorithms.colorToString(color)));
		map.put(GPX_COL_WIDTH, width);
		map.put(GPX_COL_SHOW_ARROWS, showArrows ? 1 : 0);
		map.put(GPX_COL_SHOW_START_FINISH, showStartFinish ? 1 : 0);
		map.put(GPX_COL_SPLIT_TYPE, splitType);
		map.put(GPX_COL_SPLIT_INTERVAL, splitInterval);
		map.put(GPX_COL_COLORING_TYPE, coloringType);

		boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setColor(color);
			item.setWidth(width);
			item.setShowArrows(showArrows);
			item.setShowStartFinish(showStartFinish);
			item.setSplitType(splitType);
			item.setSplitInterval(splitInterval);
			item.setColoringType(coloringType);
		}
		return success;
	}

	public boolean remove(@NonNull File file) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				String fileName = getFileName(file);
				String fileDir = getFileDir(file);
				db.execSQL("DELETE FROM " + GPX_TABLE_NAME + GPX_FIND_BY_NAME_AND_DIR,
						new Object[] {fileName, fileDir});
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean add(@NonNull GpxDataItem item) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				insert(item, db);
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	@NonNull
	private String getFileName(@NonNull File file) {
		return file.getName();
	}

	@NonNull
	private String getFileDir(@NonNull File file) {
		if (file.getParentFile() == null) {
			return "";
		}
		File gpxDir = app.getAppPath(GPX_INDEX_DIR);
		String fileDir = new File(file.getPath().replace(gpxDir.getPath() + "/", "")).getParent();
		return fileDir != null ? fileDir : "";
	}

	void insert(@NonNull GpxDataItem item, @NonNull SQLiteConnection db) {
		File file = item.getFile();
		String fileName = getFileName(file);
		String fileDir = getFileDir(file);
		GPXTrackAnalysis analysis = item.getAnalysis();
		String color = item.getColor() == 0 ? "" : Algorithms.colorToString(item.getColor());

		Map<String, Object> rowsMap = new LinkedHashMap<>();
		rowsMap.put(GPX_COL_NAME, fileName);
		rowsMap.put(GPX_COL_DIR, fileDir);
		rowsMap.put(GPX_COL_COLOR, color);
		rowsMap.put(GPX_COL_FILE_LAST_MODIFIED_TIME, file.lastModified());
		rowsMap.put(GPX_COL_FILE_LAST_UPLOADED_TIME, item.getFileLastUploadedTime());
		rowsMap.put(GPX_COL_FILE_CREATION_TIME, item.getFileCreationTime());
		rowsMap.put(GPX_COL_SPLIT_TYPE, item.getSplitType());
		rowsMap.put(GPX_COL_SPLIT_INTERVAL, item.getSplitInterval());
		rowsMap.put(GPX_COL_API_IMPORTED, item.isImportedByApi() ? 1 : 0);
		rowsMap.put(GPX_COL_SHOW_AS_MARKERS, item.isShowAsMarkers() ? 1 : 0);
		rowsMap.put(GPX_COL_JOIN_SEGMENTS, item.isJoinSegments() ? 1 : 0);
		rowsMap.put(GPX_COL_SHOW_ARROWS, item.isShowArrows() ? 1 : 0);
		rowsMap.put(GPX_COL_SHOW_START_FINISH, item.isShowStartFinish() ? 1 : 0);
		rowsMap.put(GPX_COL_WIDTH, item.getWidth());
		rowsMap.put(GPX_COL_COLORING_TYPE, item.getColoringType());
		rowsMap.put(GPX_COL_SMOOTHING_THRESHOLD, item.getSmoothingThreshold());
		rowsMap.put(GPX_COL_MIN_FILTER_SPEED, item.getMinFilterSpeed());
		rowsMap.put(GPX_COL_MAX_FILTER_SPEED, item.getMaxFilterSpeed());
		rowsMap.put(GPX_COL_MIN_FILTER_ALTITUDE, item.getMinFilterAltitude());
		rowsMap.put(GPX_COL_MAX_FILTER_ALTITUDE, item.getMaxFilterAltitude());
		rowsMap.put(GPX_COL_MAX_FILTER_HDOP, item.getMaxFilterHdop());
		rowsMap.put(GPX_COL_NEAREST_CITY_NAME, item.getNearestCityName());

		if (analysis != null) {
			rowsMap.put(GPX_COL_TOTAL_DISTANCE, analysis.totalDistance);
			rowsMap.put(GPX_COL_TOTAL_TRACKS, analysis.totalTracks);
			rowsMap.put(GPX_COL_START_TIME, analysis.startTime);
			rowsMap.put(GPX_COL_END_TIME, analysis.endTime);
			rowsMap.put(GPX_COL_TIME_SPAN, analysis.timeSpan);
			rowsMap.put(GPX_COL_TIME_MOVING, analysis.timeMoving);
			rowsMap.put(GPX_COL_TOTAL_DISTANCE_MOVING, analysis.totalDistanceMoving);
			rowsMap.put(GPX_COL_DIFF_ELEVATION_UP, analysis.diffElevationUp);
			rowsMap.put(GPX_COL_DIFF_ELEVATION_DOWN, analysis.diffElevationDown);
			rowsMap.put(GPX_COL_AVG_ELEVATION, analysis.avgElevation);
			rowsMap.put(GPX_COL_MIN_ELEVATION, analysis.minElevation);
			rowsMap.put(GPX_COL_MAX_ELEVATION, analysis.maxElevation);
			rowsMap.put(GPX_COL_MAX_SPEED, analysis.maxSpeed);
			rowsMap.put(GPX_COL_AVG_SPEED, analysis.avgSpeed);
			rowsMap.put(GPX_COL_POINTS, analysis.points);
			rowsMap.put(GPX_COL_WPT_POINTS, analysis.wptPoints);
			rowsMap.put(GPX_COL_WPT_CATEGORY_NAMES, Algorithms.encodeCollection(analysis.wptCategoryNames));
			rowsMap.put(GPX_COL_START_LAT, analysis.latLonStart != null ? analysis.latLonStart.getLatitude() : null);
			rowsMap.put(GPX_COL_START_LON, analysis.latLonStart != null ? analysis.latLonStart.getLongitude() : null);
		}

		db.execSQL(AndroidUtils.createDbInsertQuery(GPX_TABLE_NAME, rowsMap.keySet()), rowsMap.values().toArray());
	}

	public boolean updateAnalysis(@NonNull GpxDataItem item, @Nullable GPXTrackAnalysis analysis) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				return updateAnalysis(db, item, analysis);
			} finally {
				db.close();
			}
		}
		return false;
	}

	public boolean updateAnalysis(@NonNull SQLiteConnection db, @NonNull GpxDataItem item, @Nullable GPXTrackAnalysis analysis) {
		boolean hasAnalysis = analysis != null;
		long fileLastModifiedTime = hasAnalysis ? item.getFile().lastModified() : 0;

		Map<String, Object> map = new LinkedHashMap<>();
		map.put(GPX_COL_TOTAL_DISTANCE, hasAnalysis ? analysis.totalDistance : null);
		map.put(GPX_COL_TOTAL_TRACKS, hasAnalysis ? analysis.totalTracks : null);
		map.put(GPX_COL_START_TIME, hasAnalysis ? analysis.startTime : null);
		map.put(GPX_COL_END_TIME, hasAnalysis ? analysis.endTime : null);
		map.put(GPX_COL_TIME_SPAN, hasAnalysis ? analysis.timeSpan : null);
		map.put(GPX_COL_TIME_MOVING, hasAnalysis ? analysis.timeMoving : null);
		map.put(GPX_COL_TOTAL_DISTANCE_MOVING, hasAnalysis ? analysis.totalDistanceMoving : null);
		map.put(GPX_COL_DIFF_ELEVATION_UP, hasAnalysis ? analysis.diffElevationUp : null);
		map.put(GPX_COL_DIFF_ELEVATION_DOWN, hasAnalysis ? analysis.diffElevationDown : null);
		map.put(GPX_COL_AVG_ELEVATION, hasAnalysis ? analysis.avgElevation : null);
		map.put(GPX_COL_MIN_ELEVATION, hasAnalysis ? analysis.minElevation : null);
		map.put(GPX_COL_MAX_ELEVATION, hasAnalysis ? analysis.maxElevation : null);
		map.put(GPX_COL_MAX_SPEED, hasAnalysis ? analysis.maxSpeed : null);
		map.put(GPX_COL_AVG_SPEED, hasAnalysis ? analysis.avgSpeed : null);
		map.put(GPX_COL_POINTS, hasAnalysis ? analysis.points : null);
		map.put(GPX_COL_WPT_POINTS, hasAnalysis ? analysis.wptPoints : null);
		map.put(GPX_COL_FILE_LAST_MODIFIED_TIME, fileLastModifiedTime);
		map.put(GPX_COL_WPT_CATEGORY_NAMES, hasAnalysis ? Algorithms.encodeCollection(analysis.wptCategoryNames) : null);
		map.put(GPX_COL_START_LAT, hasAnalysis && analysis.latLonStart != null ? analysis.latLonStart.getLatitude() : null);
		map.put(GPX_COL_START_LON, hasAnalysis && analysis.latLonStart != null ? analysis.latLonStart.getLongitude() : null);

		boolean success = updateGpxParameters(db, map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setAnalysis(analysis);
			item.setFileLastModifiedTime(fileLastModifiedTime);
		}
		return success;
	}

	@NonNull
	private GpxDataItem readItem(SQLiteCursor query) {
		String fileName = query.getString(0);
		String fileDir = query.getString(1);
		float totalDistance = (float) query.getDouble(2);
		int totalTracks = query.getInt(3);
		long startTime = query.getLong(4);
		long endTime = query.getLong(5);
		long timeSpan = query.getLong(6);
		long timeMoving = query.getLong(7);
		float totalDistanceMoving = (float) query.getDouble(8);
		double diffElevationUp = query.getDouble(9);
		double diffElevationDown = query.getDouble(10);
		double avgElevation = query.getDouble(11);
		double minElevation = query.getDouble(12);
		double maxElevation = query.getDouble(13);
		float maxSpeed = (float) query.getDouble(14);
		float avgSpeed = (float) query.getDouble(15);
		int points = query.getInt(16);
		int wptPoints = query.getInt(17);
		String color = query.getString(18);
		long fileLastModifiedTime = query.getLong(19);
		long fileLastUploadedTime = query.getLong(20);
		long fileCreateTime = query.isNull(21) ? -1 : query.getLong(21);
		int splitType = query.getInt(22);
		double splitInterval = query.getDouble(23);
		boolean apiImported = query.getInt(24) == 1;
		String wptCategoryNames = query.getString(25);
		boolean showAsMarkers = query.getInt(26) == 1;
		boolean joinSegments = query.getInt(27) == 1;
		boolean showArrows = query.getInt(28) == 1;
		boolean showStartFinish = query.getInt(29) == 1;
		String width = query.getString(30);
		String coloringTypeName = query.getString(34);
		double smoothingThreshold = query.getDouble(35);
		double minFilterSpeed = query.getDouble(36);
		double maxFilterSpeed = query.getDouble(37);
		double minFilterAltitude = query.getDouble(38);
		double maxFilterAltitude = query.getDouble(39);
		double maxFilterHdop = query.getDouble(40);

		LatLon latLonStart = null;
		if (!query.isNull(41) && !query.isNull(42)) {
			double lat = query.getDouble(41);
			double lon = query.getDouble(42);
			latLonStart = new LatLon(lat, lon);
		}
		String nearestCityName = query.getString(43);

		GPXTrackAnalysis analysis = new GPXTrackAnalysis();
		analysis.totalDistance = totalDistance;
		analysis.totalTracks = totalTracks;
		analysis.startTime = startTime;
		analysis.endTime = endTime;
		analysis.timeSpan = timeSpan;
		analysis.timeMoving = timeMoving;
		analysis.totalDistanceMoving = totalDistanceMoving;
		analysis.diffElevationUp = diffElevationUp;
		analysis.diffElevationDown = diffElevationDown;
		analysis.avgElevation = avgElevation;
		analysis.minElevation = minElevation;
		analysis.maxElevation = maxElevation;
		analysis.minSpeed = maxSpeed;
		analysis.maxSpeed = maxSpeed;
		analysis.avgSpeed = avgSpeed;
		analysis.points = points;
		analysis.wptPoints = wptPoints;
		analysis.latLonStart = latLonStart;
		analysis.wptCategoryNames = wptCategoryNames != null ? Algorithms.decodeStringSet(wptCategoryNames) : null;

		File dir;
		if (Algorithms.isEmpty(fileDir)) {
			dir = app.getAppPath(GPX_INDEX_DIR);
		} else {
			dir = new File(app.getAppPath(GPX_INDEX_DIR), fileDir);
		}
		GpxDataItem item = new GpxDataItem(new File(dir, fileName));
		item.setAnalysis(analysis);
		item.setContainingFolder(fileDir);
		item.setColor(GPXUtilities.parseColor(color, 0));
		item.setFileLastModifiedTime(fileLastModifiedTime);
		item.setFileLastUploadedTime(fileLastUploadedTime);
		item.setFileCreationTime(fileCreateTime);
		item.setSplitType(splitType);
		item.setSplitInterval(splitInterval);
		item.setImportedByApi(apiImported);
		item.setShowAsMarkers(showAsMarkers);
		item.setJoinSegments(joinSegments);
		item.setShowArrows(showArrows);
		item.setShowStartFinish(showStartFinish);
		item.setWidth(width);
		item.setNearestCityName(nearestCityName);

		if (ColoringType.getNullableTrackColoringTypeByName(coloringTypeName) != null) {
			item.setColoringType(coloringTypeName);
		} else if (GradientScaleType.getGradientTypeByName(coloringTypeName) != null) {
			GradientScaleType scaleType = GradientScaleType.getGradientTypeByName(coloringTypeName);
			ColoringType coloringType = ColoringType.fromGradientScaleType(scaleType);
			item.setColoringType(coloringType == null ? null : coloringType.getName(null));
		}

		item.setSmoothingThreshold(smoothingThreshold);
		item.setMinFilterSpeed(minFilterSpeed);
		item.setMaxFilterSpeed(maxFilterSpeed);
		item.setMinFilterAltitude(minFilterAltitude);
		item.setMaxFilterAltitude(maxFilterAltitude);
		item.setMaxFilterHdop(maxFilterHdop);

		return item;
	}

	public long getTracksMinCreateDate() {
		long minDate = -1;
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(GPX_MIN_CREATE_DATE, null);
				if (query != null) {
					try {
						if (query.moveToFirst()) {
							minDate = query.getLong(0);
						}
					} finally {
						query.close();
					}
				}
			} finally {
				db.close();
			}
		}
		return minDate;
	}

	public double getTracksMaxDuration() {
		double maxLength = 0.0;
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(GPX_MAX_TRACK_DURATION, null);
				if (query != null) {
					try {
						if (query.moveToFirst()) {
							maxLength = query.getDouble(0);
						}
					} finally {
						query.close();
					}
				}
			} finally {
				db.close();
			}
		}
		return maxLength;
	}

	public List<Pair<String, Integer>> getTrackFolders() {
		return getStringIntItemsCollection(GPX_TRACK_FOLDERS_COLLECTION);
	}

	public List<Pair<String, Integer>> getNearestCityCollection() {
		return getStringIntItemsCollection(GPX_TRACK_NEAREST_CITIES_COLLECTION);
	}

	public List<Pair<String, Integer>> getTrackColorsCollection() {
		return getStringIntItemsCollection(GPX_TRACK_COLORS_COLLECTION);
	}

	public List<Pair<String, Integer>> getTrackWidthCollection() {
		return getStringIntItemsCollection(GPX_TRACK_WIDTH_COLLECTION);
	}

	public List<Pair<String, Integer>> getStringIntItemsCollection(String dataQuery) {
		ArrayList<Pair<String, Integer>> folderCollection = new ArrayList<>();
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(dataQuery, null);
				if (query != null) {
					try {
						if (query.moveToFirst()) {
							do {
								folderCollection.add(new Pair<>(query.getString(0), query.getInt(1)));
							} while (query.moveToNext());
						}
					} finally {
						query.close();
					}
				}
			} finally {
				db.close();
			}
		}
		return folderCollection;
	}

	@NonNull
	public List<GpxDataItem> getItems() {
		Set<GpxDataItem> items = new HashSet<>();
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(GPX_TABLE_SELECT, null);
				if (query != null) {
					try {
						if (query.moveToFirst()) {
							do {
								items.add(readItem(query));
							} while (query.moveToNext());
						}
					} finally {
						query.close();
					}
				}
			} finally {
				db.close();
			}
		}
		return new ArrayList<>(items);
	}

	@Nullable
	public GpxDataItem getItem(File file) {
		GpxDataItem result = null;
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				result = getItem(file, db);
			} finally {
				db.close();
			}
		}
		return result;
	}

	@Nullable
	public GpxDataItem getItem(File file, SQLiteConnection db) {
		GpxDataItem result = null;
		String fileName = getFileName(file);
		String fileDir = getFileDir(file);
		SQLiteCursor query = db.rawQuery(GPX_TABLE_SELECT + " WHERE " + GPX_COL_NAME + " = ? AND " +
				GPX_COL_DIR + " = ?", new String[] {fileName, fileDir});
		if (query != null) {
			try {
				if (query.moveToFirst()) {
					result = readItem(query);
				}
			} finally {
				query.close();
			}
		}
		return result;
	}

	@NonNull
	public List<GpxDataItem> getSplitItems() {
		List<GpxDataItem> items = new ArrayList<>();
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(GPX_TABLE_SELECT + " WHERE " + GPX_COL_SPLIT_TYPE + " != ?", new String[] {String.valueOf(0)});
				if (query != null) {
					try {
						if (query.moveToFirst()) {
							do {
								items.add(readItem(query));
							} while (query.moveToNext());
						}
					} finally {
						query.close();
					}
				}
			} finally {
				db.close();
			}
		}
		return items;
	}
}
