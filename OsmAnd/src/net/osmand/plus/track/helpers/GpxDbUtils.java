package net.osmand.plus.track.helpers;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.track.helpers.GPXDatabase.GPX_TABLE_NAME;
import static net.osmand.plus.track.helpers.GPXDatabase.GPX_UPDATE_PARAMETERS_START;
import static net.osmand.plus.track.helpers.GpxParameter.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import org.bouncycastle.util.Arrays;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class GpxDbUtils {

	private static final String GPX_INDEX_NAME_DIR = "indexNameDir";

	@NonNull
	public static String getCreateTableQuery() {
		StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS " + GPX_TABLE_NAME + " (");
		Iterator<GpxParameter> iterator = new Arrays.Iterator<>(GpxParameter.values());
		while (iterator.hasNext()) {
			GpxParameter parameter = iterator.next();
			builder.append(" ").append(parameter.getColumnName()).append(" ").append(parameter.getColumnType());

			if (iterator.hasNext()) {
				builder.append(", ");
			} else {
				builder.append(");");
			}
		}
		return builder.toString();
	}

	@NonNull
	public static String getSelectQuery() {
		StringBuilder builder = new StringBuilder("SELECT ");
		Iterator<GpxParameter> iterator = new Arrays.Iterator<>(GpxParameter.values());
		while (iterator.hasNext()) {
			GpxParameter parameter = iterator.next();
			builder.append(parameter.getColumnName());

			if (iterator.hasNext()) {
				builder.append(", ");
			} else {
				builder.append(" FROM ").append(GPX_TABLE_NAME);
			}
		}
		return builder.toString();
	}

	@NonNull
	public static String getIndexQuery() {
		return "CREATE INDEX IF NOT EXISTS " + GPX_INDEX_NAME_DIR + " ON " + GPX_TABLE_NAME
				+ " (" + FILE_NAME.getColumnName() + ", " + FILE_DIR.getColumnName() + ");";
	}

	protected static void onCreate(@NonNull SQLiteConnection db) {
		db.execSQL(getCreateTableQuery());
		db.execSQL(getIndexQuery());
	}

	protected static void onUpgrade(@NonNull GPXDatabase database, @NonNull SQLiteConnection db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			addTableColumn(db, COLOR);
		}
		if (oldVersion < 3) {
			addTableColumn(db, FILE_LAST_MODIFIED_TIME);
		}
		if (oldVersion < 4) {
			addTableColumn(db, SPLIT_TYPE);
			addTableColumn(db, SPLIT_INTERVAL);
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
					if (!colorColumnExists && columnName.equals(COLOR.getColumnName())) {
						colorColumnExists = true;
					} else if (!fileLastModifiedTimeColumnExists && columnName.equals(FILE_LAST_MODIFIED_TIME.getColumnName())) {
						fileLastModifiedTimeColumnExists = true;
					} else if (!splitTypeColumnExists && columnName.equals(SPLIT_TYPE.getColumnName())) {
						splitTypeColumnExists = true;
					} else if (!splitIntervalColumnExists && columnName.equals(SPLIT_INTERVAL.getColumnName())) {
						splitIntervalColumnExists = true;
					}
				} while (cursor.moveToNext());
			}
			cursor.close();
			if (!colorColumnExists) {
				addTableColumn(db, COLOR);
			}
			if (!fileLastModifiedTimeColumnExists) {
				addTableColumn(db, FILE_LAST_MODIFIED_TIME);
				for (GpxDataItem item : database.getItems()) {
					item.setParameter(FILE_LAST_MODIFIED_TIME, item.getFile().lastModified());
					database.updateDataItem(item);
				}
			}
			if (!splitTypeColumnExists) {
				addTableColumn(db, SPLIT_TYPE);
			}
			if (!splitIntervalColumnExists) {
				addTableColumn(db, SPLIT_INTERVAL);
			}
		}
		if (oldVersion < 6) {
			addTableColumn(db, API_IMPORTED);
			db.execSQL("UPDATE " + GPX_TABLE_NAME +
					" SET " + API_IMPORTED.getColumnName() + " = ? " +
					"WHERE " + API_IMPORTED.getColumnName() + " IS NULL", new Object[] {0});
		}
		if (oldVersion < 7) {
			addTableColumn(db, WPT_CATEGORY_NAMES);
		}
		if (oldVersion < 8) {
			addTableColumn(db, SHOW_AS_MARKERS);
			db.execSQL("UPDATE " + GPX_TABLE_NAME +
					" SET " + SHOW_AS_MARKERS.getColumnName() + " = ? " +
					"WHERE " + SHOW_AS_MARKERS.getColumnName() + " IS NULL", new Object[] {0});
		}
		if (oldVersion < 10) {
			addTableColumn(db, JOIN_SEGMENTS);
			db.execSQL("UPDATE " + GPX_TABLE_NAME +
					" SET " + JOIN_SEGMENTS.getColumnName() + " = ? " +
					"WHERE " + JOIN_SEGMENTS.getColumnName() + " IS NULL", new Object[] {0});
		}
		if (oldVersion < 11) {
			addTableColumn(db, SHOW_ARROWS);
			addTableColumn(db, SHOW_START_FINISH);
			addTableColumn(db, WIDTH);
			addTableColumn(db, "gradientSpeedColor", "TEXT");
			addTableColumn(db, "gradientAltitudeColor", "TEXT");
			addTableColumn(db, "gradientSlopeColor", "TEXT");
			addTableColumn(db, COLORING_TYPE);

			db.execSQL(GPX_UPDATE_PARAMETERS_START + SHOW_ARROWS.getColumnName() + " = ? " +
					"WHERE " + SHOW_ARROWS.getColumnName() + " IS NULL", new Object[] {0});
			db.execSQL(GPX_UPDATE_PARAMETERS_START + SHOW_START_FINISH.getColumnName() + " = ? " +
					"WHERE " + SHOW_START_FINISH.getColumnName() + " IS NULL", new Object[] {1});
		}
		if (oldVersion < 12) {
			addTableColumn(db, FILE_LAST_UPLOADED_TIME);
		}
		if (oldVersion < 13) {
			addTableColumn(db, SMOOTHING_THRESHOLD);
			addTableColumn(db, MIN_FILTER_SPEED);
			addTableColumn(db, MAX_FILTER_SPEED);
			addTableColumn(db, MIN_FILTER_ALTITUDE);
			addTableColumn(db, MAX_FILTER_ALTITUDE);
			addTableColumn(db, MAX_FILTER_HDOP);
		}
		if (oldVersion < 14) {
			addTableColumn(db, START_LAT);
			addTableColumn(db, START_LON);
		}
		if (oldVersion < 15) {
			addTableColumn(db, NEAREST_CITY_NAME);
		}
		if (oldVersion < 16) {
			addTableColumn(db, FILE_CREATION_TIME);
		}
		if (oldVersion < 17) {
			addTableColumn(db, EXPECTED_ROUTE_DURATION);
		}
		db.execSQL(getIndexQuery());
	}

	private static void addTableColumn(@NonNull SQLiteConnection db, @NonNull GpxParameter parameter) {
		addTableColumn(db, parameter.getColumnName(), parameter.getColumnType());
	}

	private static void addTableColumn(@NonNull SQLiteConnection db, @NonNull String columnName, @NonNull String columnType) {
		db.execSQL("ALTER TABLE " + GPX_TABLE_NAME + " ADD " + columnName + " " + columnType);
	}

	public static boolean isAnalyseNeeded(@NonNull File file, @Nullable GpxDataItem item) {
		if (item != null) {
			GPXTrackAnalysis analysis = item.getAnalysis();
			return !item.hasData() || analysis == null
					|| analysis.wptCategoryNames == null
					|| analysis.latLonStart == null && analysis.points > 0
					|| (long) item.getParameter(FILE_LAST_MODIFIED_TIME) != file.lastModified()
					|| (long) item.getParameter(FILE_CREATION_TIME) <= 0
					|| (long) item.getParameter(EXPECTED_ROUTE_DURATION) < 0;
		}
		return true;
	}

	public static boolean isCitySearchNeeded(@Nullable GpxDataItem item) {
		if (item != null) {
			GPXTrackAnalysis analysis = item.getAnalysis();
			return item.getParameter(NEAREST_CITY_NAME) == null && analysis != null && analysis.latLonStart != null;
		}
		return true;
	}

	@NonNull
	public static String getGpxFileDir(@NonNull OsmandApplication app, @NonNull File file) {
		if (file.getParentFile() == null) {
			return "";
		}
		File dir = app.getAppPath(GPX_INDEX_DIR);
		String fileDir = new File(file.getPath().replace(dir.getPath() + "/", "")).getParent();
		return fileDir != null ? fileDir : "";
	}

	@NonNull
	public static Map<GpxParameter, Object> getItemParameters(@NonNull OsmandApplication app, @NonNull GpxDataItem item) {
		File file = item.getFile();

		Map<GpxParameter, Object> map = new LinkedHashMap<>();
		map.put(FILE_NAME, file.getName());
		map.put(FILE_DIR, GpxDbUtils.getGpxFileDir(app, file));
		map.put(COLOR, item.getParameter(COLOR));
		map.put(FILE_LAST_MODIFIED_TIME, file.lastModified());
		map.put(FILE_LAST_UPLOADED_TIME, item.getParameter(FILE_LAST_UPLOADED_TIME));
		map.put(FILE_CREATION_TIME, item.getParameter(FILE_CREATION_TIME));
		map.put(SPLIT_TYPE, item.getParameter(SPLIT_TYPE));
		map.put(SPLIT_INTERVAL, item.getParameter(SPLIT_INTERVAL));
		map.put(API_IMPORTED, item.getParameter(API_IMPORTED));
		map.put(SHOW_AS_MARKERS, item.getParameter(SHOW_AS_MARKERS));
		map.put(JOIN_SEGMENTS, item.getParameter(JOIN_SEGMENTS));
		map.put(SHOW_ARROWS, item.getParameter(SHOW_ARROWS));
		map.put(SHOW_START_FINISH, item.getParameter(SHOW_START_FINISH));
		map.put(WIDTH, item.getParameter(WIDTH));
		map.put(COLORING_TYPE, item.getParameter(COLORING_TYPE));
		map.put(SMOOTHING_THRESHOLD, item.getParameter(SMOOTHING_THRESHOLD));
		map.put(MIN_FILTER_SPEED, item.getParameter(MIN_FILTER_SPEED));
		map.put(MAX_FILTER_SPEED, item.getParameter(MAX_FILTER_SPEED));
		map.put(MIN_FILTER_ALTITUDE, item.getParameter(MIN_FILTER_ALTITUDE));
		map.put(MAX_FILTER_ALTITUDE, item.getParameter(MAX_FILTER_ALTITUDE));
		map.put(MAX_FILTER_HDOP, item.getParameter(MAX_FILTER_HDOP));
		map.put(NEAREST_CITY_NAME, item.getParameter(NEAREST_CITY_NAME));

		GPXTrackAnalysis analysis = item.getAnalysis();
		boolean hasAnalysis = analysis != null;
		map.put(TOTAL_DISTANCE, hasAnalysis ? analysis.totalDistance : null);
		map.put(TOTAL_TRACKS, hasAnalysis ? analysis.totalTracks : null);
		map.put(START_TIME, hasAnalysis ? analysis.startTime : null);
		map.put(END_TIME, hasAnalysis ? analysis.endTime : null);
		map.put(TIME_SPAN, hasAnalysis ? analysis.timeSpan : null);
		map.put(TIME_MOVING, hasAnalysis ? analysis.timeMoving : null);
		map.put(EXPECTED_ROUTE_DURATION, hasAnalysis ? analysis.expectedRouteDuration : null);
		map.put(TOTAL_DISTANCE_MOVING, hasAnalysis ? analysis.totalDistanceMoving : null);
		map.put(DIFF_ELEVATION_UP, hasAnalysis ? analysis.diffElevationUp : null);
		map.put(DIFF_ELEVATION_DOWN, hasAnalysis ? analysis.diffElevationDown : null);
		map.put(AVG_ELEVATION, hasAnalysis ? analysis.avgElevation : null);
		map.put(MIN_ELEVATION, hasAnalysis ? analysis.minElevation : null);
		map.put(MAX_ELEVATION, hasAnalysis ? analysis.maxElevation : null);
		map.put(MAX_SPEED, hasAnalysis ? analysis.maxSpeed : null);
		map.put(AVG_SPEED, hasAnalysis ? analysis.avgSpeed : null);
		map.put(POINTS, hasAnalysis ? analysis.points : null);
		map.put(WPT_POINTS, hasAnalysis ? analysis.wptPoints : null);
		map.put(WPT_CATEGORY_NAMES, hasAnalysis ? Algorithms.encodeCollection(analysis.wptCategoryNames) : null);
		map.put(START_LAT, hasAnalysis && analysis.latLonStart != null ? analysis.latLonStart.getLatitude() : null);
		map.put(START_LON, hasAnalysis && analysis.latLonStart != null ? analysis.latLonStart.getLongitude() : null);

		return map;
	}

	@NonNull
	public static Map<String, Object> convertGpxParameters(@NonNull Map<GpxParameter, Object> parameters) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (Map.Entry<GpxParameter, Object> entry : parameters.entrySet()) {
			GpxParameter parameter = entry.getKey();
			Object value = parameter.convertToDbValue(entry.getValue());
			map.put(parameter.getColumnName(), value);
		}
		return map;
	}

	@NonNull
	public static Map<String, Object> getItemRowsToSearch(@NonNull OsmandApplication app, @NonNull File file) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put(FILE_NAME.getColumnName(), file.getName());
		map.put(FILE_DIR.getColumnName(), GpxDbUtils.getGpxFileDir(app, file));
		return map;
	}
}
