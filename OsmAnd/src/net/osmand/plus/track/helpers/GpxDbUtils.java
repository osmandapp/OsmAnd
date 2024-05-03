package net.osmand.plus.track.helpers;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.gpx.GPXTrackAnalysis.ANALYSIS_VERSION;
import static net.osmand.gpx.GpxParameter.*;
import static net.osmand.plus.track.helpers.GPXDatabase.DB_VERSION;
import static net.osmand.plus.track.helpers.GPXDatabase.GPX_DIR_TABLE_NAME;
import static net.osmand.plus.track.helpers.GPXDatabase.GPX_TABLE_NAME;
import static net.osmand.plus.track.helpers.GPXDatabase.GPX_UPDATE_PARAMETERS_START;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GpxParameter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GpxDbUtils {

	private static final String GPX_TABLE_INDEX = "indexNameDir";
	private static final String GPX_DIR_TABLE_INDEX = "gpxDirIndexNameDir";

	@NonNull
	public static String getCreateGpxTableQuery() {
		return getCreateTableQuery(Arrays.asList(GpxParameter.values()), GPX_TABLE_NAME);
	}

	public static String getCreateGpxDirTableQuery() {
		return getCreateTableQuery(GpxParameter.getGpxDirParameters(), GPX_DIR_TABLE_NAME);
	}

	@NonNull
	public static String getSelectGpxQuery() {
		return getSelectQuery(Arrays.asList(GpxParameter.values()), GPX_TABLE_NAME);
	}

	@NonNull
	public static String getSelectGpxDirQuery() {
		return getSelectQuery(GpxParameter.getGpxDirParameters(), GPX_DIR_TABLE_NAME);
	}

	@NonNull
	public static String getCreateTableQuery(@NonNull List<GpxParameter> parameters, @NonNull String tableName) {
		StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");
		Iterator<GpxParameter> iterator = parameters.iterator();
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
	public static String getSelectQuery(@NonNull List<GpxParameter> parameters, @NonNull String tableName) {
		StringBuilder builder = new StringBuilder("SELECT ");
		Iterator<GpxParameter> iterator = parameters.iterator();
		while (iterator.hasNext()) {
			GpxParameter parameter = iterator.next();
			builder.append(parameter.getColumnName());

			if (iterator.hasNext()) {
				builder.append(", ");
			} else {
				builder.append(" FROM ").append(tableName);
			}
		}
		return builder.toString();
	}

	@NonNull
	public static String getGpxIndexQuery() {
		return "CREATE INDEX IF NOT EXISTS " + GPX_TABLE_INDEX + " ON " + GPX_TABLE_NAME
				+ " (" + FILE_NAME.getColumnName() + ", " + FILE_DIR.getColumnName() + ");";
	}

	@NonNull
	public static String getGpxDirIndexQuery() {
		return "CREATE INDEX IF NOT EXISTS " + GPX_DIR_TABLE_INDEX + " ON " + GPX_DIR_TABLE_NAME
				+ " (" + FILE_NAME.getColumnName() + ", " + FILE_DIR.getColumnName() + ");";
	}

	protected static void onCreate(@NonNull SQLiteConnection db) {
		db.execSQL(getCreateGpxTableQuery());
		db.execSQL(getGpxIndexQuery());
		db.execSQL(getCreateGpxDirTableQuery());
		db.execSQL(getGpxDirIndexQuery());
	}

	protected static void onUpgrade(@NonNull GPXDatabase database, @NonNull SQLiteConnection db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			addGpxTableColumn(db, COLOR);
		}
		if (oldVersion < 3) {
			addGpxTableColumn(db, FILE_LAST_MODIFIED_TIME);
		}
		if (oldVersion < 4) {
			addGpxTableColumn(db, SPLIT_TYPE);
			addGpxTableColumn(db, SPLIT_INTERVAL);
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
				addGpxTableColumn(db, COLOR);
			}
			if (!fileLastModifiedTimeColumnExists) {
				addGpxTableColumn(db, FILE_LAST_MODIFIED_TIME);
				for (GpxDataItem item : database.getGpxDataItems()) {
					item.setParameter(FILE_LAST_MODIFIED_TIME, item.getFile().lastModified());
					database.updateDataItem(item);
				}
			}
			if (!splitTypeColumnExists) {
				addGpxTableColumn(db, SPLIT_TYPE);
			}
			if (!splitIntervalColumnExists) {
				addGpxTableColumn(db, SPLIT_INTERVAL);
			}
		}
		if (oldVersion < 6) {
			addGpxTableColumn(db, API_IMPORTED);
			db.execSQL("UPDATE " + GPX_TABLE_NAME +
					" SET " + API_IMPORTED.getColumnName() + " = ? " +
					"WHERE " + API_IMPORTED.getColumnName() + " IS NULL", new Object[] {0});
		}
		if (oldVersion < 7) {
			addGpxTableColumn(db, WPT_CATEGORY_NAMES);
		}
		if (oldVersion < 8) {
			addGpxTableColumn(db, SHOW_AS_MARKERS);
			db.execSQL("UPDATE " + GPX_TABLE_NAME +
					" SET " + SHOW_AS_MARKERS.getColumnName() + " = ? " +
					"WHERE " + SHOW_AS_MARKERS.getColumnName() + " IS NULL", new Object[] {0});
		}
		if (oldVersion < 10) {
			addGpxTableColumn(db, JOIN_SEGMENTS);
			db.execSQL("UPDATE " + GPX_TABLE_NAME +
					" SET " + JOIN_SEGMENTS.getColumnName() + " = ? " +
					"WHERE " + JOIN_SEGMENTS.getColumnName() + " IS NULL", new Object[] {0});
		}
		if (oldVersion < 11) {
			addGpxTableColumn(db, SHOW_ARROWS);
			addGpxTableColumn(db, SHOW_START_FINISH);
			addGpxTableColumn(db, WIDTH);
			addTableColumn(db, GPX_TABLE_NAME, "gradientSpeedColor", "TEXT");
			addTableColumn(db, GPX_TABLE_NAME, "gradientAltitudeColor", "TEXT");
			addTableColumn(db, GPX_TABLE_NAME, "gradientSlopeColor", "TEXT");
			addGpxTableColumn(db, COLORING_TYPE);

			db.execSQL(GPX_UPDATE_PARAMETERS_START + SHOW_ARROWS.getColumnName() + " = ? " +
					"WHERE " + SHOW_ARROWS.getColumnName() + " IS NULL", new Object[] {0});
			db.execSQL(GPX_UPDATE_PARAMETERS_START + SHOW_START_FINISH.getColumnName() + " = ? " +
					"WHERE " + SHOW_START_FINISH.getColumnName() + " IS NULL", new Object[] {1});
		}
		if (oldVersion < 12) {
			addGpxTableColumn(db, FILE_LAST_UPLOADED_TIME);
		}
		if (oldVersion < 13) {
			addGpxTableColumn(db, SMOOTHING_THRESHOLD);
			addGpxTableColumn(db, MIN_FILTER_SPEED);
			addGpxTableColumn(db, MAX_FILTER_SPEED);
			addGpxTableColumn(db, MIN_FILTER_ALTITUDE);
			addGpxTableColumn(db, MAX_FILTER_ALTITUDE);
			addGpxTableColumn(db, MAX_FILTER_HDOP);
		}
		if (oldVersion < 14) {
			addGpxTableColumn(db, START_LAT);
			addGpxTableColumn(db, START_LON);
		}
		if (oldVersion < 15) {
			addGpxTableColumn(db, NEAREST_CITY_NAME);
		}
		if (oldVersion < 16) {
			addGpxTableColumn(db, FILE_CREATION_TIME);
		}
		SQLiteCursor gpxCursor = db.rawQuery("select * from " + GPX_TABLE_NAME + " limit 0", null);
		for (GpxParameter parameter : GpxParameter.values()) {
			if (gpxCursor.getColumnIndex(parameter.getColumnName()) == -1) {
				addGpxTableColumn(db, parameter);
			}
		}
		if (oldVersion < 19) {
			db.execSQL(getCreateGpxDirTableQuery());
		}
		SQLiteCursor gpxDirCursor = db.rawQuery("select * from " + GPX_DIR_TABLE_NAME + " limit 0", null);
		for (GpxParameter parameter : GpxParameter.getGpxDirParameters()) {
			if (gpxDirCursor.getColumnIndex(parameter.getColumnName()) == -1) {
				addGpxDirTableColumn(db, parameter);
			}
		}
		db.execSQL(getGpxIndexQuery());
		db.execSQL(getGpxDirIndexQuery());
	}

	private static void addGpxTableColumn(@NonNull SQLiteConnection db, @NonNull GpxParameter parameter) {
		addTableColumn(db, GPX_TABLE_NAME, parameter.getColumnName(), parameter.getColumnType());
	}

	private static void addGpxDirTableColumn(@NonNull SQLiteConnection db, @NonNull GpxParameter parameter) {
		addTableColumn(db, GPX_DIR_TABLE_NAME, parameter.getColumnName(), parameter.getColumnType());
	}

	private static void addTableColumn(@NonNull SQLiteConnection db, @NonNull String tableName,
	                                   @NonNull String columnName, @NonNull String columnType) {
		db.execSQL("ALTER TABLE " + tableName + " ADD " + columnName + " " + columnType);
	}

	public static boolean isAnalyseNeeded(@Nullable GpxDataItem item) {
		if (item != null) {
			return !item.hasData() || item.getAnalysis() == null
					|| Algorithms.isEmpty(item.getAnalysis().getWptCategoryNames())
					|| item.getAnalysis().getLatLonStart() == null && item.getAnalysis().getPoints() > 0
					|| (long) item.requireParameter(FILE_LAST_MODIFIED_TIME) != item.getFile().lastModified()
					|| (long) item.requireParameter(FILE_CREATION_TIME) <= 0
					|| (long) item.requireParameter(EXPECTED_ROUTE_DURATION) < 0
					|| createDataVersion(ANALYSIS_VERSION) > (int) item.requireParameter(DATA_VERSION);
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
	public static Map<GpxParameter, Object> getItemParameters(@NonNull DataItem item) {
		if (item instanceof GpxDataItem) {
			return getItemParameters((GpxDataItem) item);
		} else if (item instanceof GpxDirItem) {
			return getItemParameters((GpxDirItem) item);
		}
		return Collections.emptyMap();
	}

	@NonNull
	public static Map<GpxParameter, Object> getItemParameters(@NonNull GpxDataItem item) {
		Map<GpxParameter, Object> map = new LinkedHashMap<>();
		GPXTrackAnalysis analysis = item.getAnalysis();
		boolean hasAnalysis = analysis != null;
		for (GpxParameter parameter : GpxParameter.values()) {
			if (parameter.isAnalysisParameter()) {
				map.put(parameter, hasAnalysis ? analysis.getGpxParameter(parameter) : null);
			} else {
				map.put(parameter, item.getParameter(parameter));
			}
		}
		return map;
	}

	@NonNull
	public static Map<GpxParameter, Object> getItemParameters(@NonNull GpxDirItem item) {
		Map<GpxParameter, Object> map = new LinkedHashMap<>();
		for (GpxParameter parameter : GpxParameter.getGpxDirParameters()) {
			map.put(parameter, item.getParameter(parameter));
		}
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

	@NonNull
	public static String getTableName(@NonNull File file) {
		if (file.exists()) {
			return !file.isDirectory() ? GPX_TABLE_NAME : GPX_DIR_TABLE_NAME;
		}
		return GpxUiHelper.isGpxFile(file) ? GPX_TABLE_NAME : GPX_DIR_TABLE_NAME;
	}

	@Nullable
	public static Object queryColumnValue(@NonNull SQLiteCursor query, @NonNull GpxParameter parameter) {
		int index = query.getColumnIndex(parameter.getColumnName());
		if (query.isNull(index)) {
			return null;
		}
		switch (parameter.getColumnType()) {
			case "TEXT":
				return query.getString(index);
			case "double":
				return query.getDouble(index);
			case "int":
				int value = query.getInt(index);
				return parameter.getTypeClass() == Boolean.class ? value == 1 : value;
			case "long":
				return query.getLong(index);
		}
		throw new IllegalArgumentException("Unknown column type " + parameter.getColumnType());
	}

	public static int createDataVersion(int analysisVersion) {
		return (DB_VERSION << 10) + analysisVersion;
	}
}
