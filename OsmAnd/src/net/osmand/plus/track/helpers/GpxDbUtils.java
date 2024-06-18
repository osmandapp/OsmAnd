package net.osmand.plus.track.helpers;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.gpx.GPXTrackAnalysis.ANALYSIS_VERSION;
import static net.osmand.gpx.GpxParameter.*;
import static net.osmand.plus.track.helpers.GPXDatabase.DB_VERSION;
import static net.osmand.plus.track.helpers.GPXDatabase.GPX_DIR_TABLE_NAME;
import static net.osmand.plus.track.helpers.GPXDatabase.GPX_TABLE_NAME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GpxParameter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
		SQLiteCursor gpxCursor = db.rawQuery("select * from " + GPX_TABLE_NAME + " limit 1", null);
		Set<String> columnNames = new TreeSet<>();
		for(String lc : gpxCursor.getColumnNames()){
			columnNames.add(lc.toLowerCase());
		}
		// temporary code to test failure
		addIfMissingGpxTableColumn(columnNames, db, FILE_NAME);
		addIfMissingGpxTableColumn(columnNames, db, FILE_DIR);
		addIfMissingGpxTableColumn(columnNames, db, TOTAL_DISTANCE);
		addIfMissingGpxTableColumn(columnNames, db, TOTAL_TRACKS);
		addIfMissingGpxTableColumn(columnNames, db, START_TIME);
		addIfMissingGpxTableColumn(columnNames, db, END_TIME);
		addIfMissingGpxTableColumn(columnNames, db, TIME_SPAN);
		addIfMissingGpxTableColumn(columnNames, db, TIME_MOVING);
		addIfMissingGpxTableColumn(columnNames, db, TOTAL_DISTANCE_MOVING);
		addIfMissingGpxTableColumn(columnNames, db, DIFF_ELEVATION_UP);
		addIfMissingGpxTableColumn(columnNames, db, DIFF_ELEVATION_DOWN);
		addIfMissingGpxTableColumn(columnNames, db, AVG_ELEVATION);
		addIfMissingGpxTableColumn(columnNames, db, MIN_ELEVATION);
		addIfMissingGpxTableColumn(columnNames, db, MAX_ELEVATION);
		addIfMissingGpxTableColumn(columnNames, db, MIN_SPEED);
		addIfMissingGpxTableColumn(columnNames, db, MAX_SPEED);
		addIfMissingGpxTableColumn(columnNames, db, AVG_SPEED);
		addIfMissingGpxTableColumn(columnNames, db, POINTS);
		addIfMissingGpxTableColumn(columnNames, db, WPT_POINTS);
		addIfMissingGpxTableColumn(columnNames, db, COLOR);
		addIfMissingGpxTableColumn(columnNames, db, FILE_LAST_MODIFIED_TIME);
		addIfMissingGpxTableColumn(columnNames, db, FILE_LAST_UPLOADED_TIME);
		addIfMissingGpxTableColumn(columnNames, db, FILE_CREATION_TIME);
		addIfMissingGpxTableColumn(columnNames, db, SPLIT_TYPE);
		addIfMissingGpxTableColumn(columnNames, db, SPLIT_INTERVAL);
		addIfMissingGpxTableColumn(columnNames, db, API_IMPORTED);
		addIfMissingGpxTableColumn(columnNames, db, WPT_CATEGORY_NAMES);
		addIfMissingGpxTableColumn(columnNames, db, SHOW_AS_MARKERS);
		addIfMissingGpxTableColumn(columnNames, db, JOIN_SEGMENTS);
		addIfMissingGpxTableColumn(columnNames, db, SHOW_ARROWS);
		addIfMissingGpxTableColumn(columnNames, db, SHOW_START_FINISH);
		addIfMissingGpxTableColumn(columnNames, db, TRACK_VISUALIZATION_TYPE);
		addIfMissingGpxTableColumn(columnNames, db, TRACK_3D_WALL_COLORING_TYPE);
		addIfMissingGpxTableColumn(columnNames, db, TRACK_3D_LINE_POSITION_TYPE);
		addIfMissingGpxTableColumn(columnNames, db, ADDITIONAL_EXAGGERATION);
		addIfMissingGpxTableColumn(columnNames, db, ELEVATION_METERS);
		addIfMissingGpxTableColumn(columnNames, db, WIDTH);
		addIfMissingGpxTableColumn(columnNames, db, COLORING_TYPE);
		addIfMissingGpxTableColumn(columnNames, db, COLOR_PALETTE);
		addIfMissingGpxTableColumn(columnNames, db, SMOOTHING_THRESHOLD);
		addIfMissingGpxTableColumn(columnNames, db, MIN_FILTER_SPEED);
		addIfMissingGpxTableColumn(columnNames, db, MAX_FILTER_SPEED);
		addIfMissingGpxTableColumn(columnNames, db, MIN_FILTER_ALTITUDE);
		addIfMissingGpxTableColumn(columnNames, db, MAX_FILTER_ALTITUDE);
		addIfMissingGpxTableColumn(columnNames, db, MAX_FILTER_HDOP);
		addIfMissingGpxTableColumn(columnNames, db, START_LAT);
		addIfMissingGpxTableColumn(columnNames, db, START_LON);
		addIfMissingGpxTableColumn(columnNames, db, NEAREST_CITY_NAME);
		addIfMissingGpxTableColumn(columnNames, db, MAX_SENSOR_TEMPERATURE);
		addIfMissingGpxTableColumn(columnNames, db, AVG_SENSOR_TEMPERATURE);
		addIfMissingGpxTableColumn(columnNames, db, MAX_SENSOR_SPEED);
		addIfMissingGpxTableColumn(columnNames, db, AVG_SENSOR_SPEED);
		addIfMissingGpxTableColumn(columnNames, db, MAX_SENSOR_POWER);
		addIfMissingGpxTableColumn(columnNames, db, AVG_SENSOR_POWER);
		addIfMissingGpxTableColumn(columnNames, db, MAX_SENSOR_CADENCE);
		addIfMissingGpxTableColumn(columnNames, db, AVG_SENSOR_CADENCE);
		addIfMissingGpxTableColumn(columnNames, db, MAX_SENSOR_HEART_RATE);
		addIfMissingGpxTableColumn(columnNames, db, AVG_SENSOR_HEART_RATE);
		addIfMissingGpxTableColumn(columnNames, db, DATA_VERSION);
		// temporary code to test failure
		for (GpxParameter parameter : GpxParameter.values()) {
			if (!columnNames.contains(parameter.getColumnName().toLowerCase())) {
				addGpxTableColumn(db, parameter);
			}
		}

		// we can always create table it has create table if not exists
//		if (oldVersion < 24) {
		db.execSQL(getCreateGpxDirTableQuery());
//		}
		SQLiteCursor gpxDirCursor = db.rawQuery("select * from " + GPX_DIR_TABLE_NAME + " limit 1", null);
		columnNames = new TreeSet<>();
		for(String lc : gpxDirCursor.getColumnNames()){
			columnNames.add(lc.toLowerCase());
		}
		for (GpxParameter parameter : GpxParameter.getGpxDirParameters()) {
			if (!columnNames.contains(parameter.getColumnName().toLowerCase())) {
				addGpxDirTableColumn(db, parameter);
			}
		}
		db.execSQL(getGpxIndexQuery());
		db.execSQL(getGpxDirIndexQuery());
	}

	private static void addIfMissingGpxTableColumn(Set<String> columnNamesLC, SQLiteConnection db, GpxParameter p) {
		if (columnNamesLC.contains(p.getColumnName().toLowerCase())) {
			return;
		}
		columnNamesLC.add(p.getColumnName().toLowerCase());
		addGpxTableColumn(db, p);
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
					|| item.getAnalysis().getWptCategoryNames() == null
					|| item.getAnalysis().getLatLonStart() == null && item.getAnalysis().getPoints() > 0
					|| (long) item.requireParameter(FILE_LAST_MODIFIED_TIME) != item.getFile().lastModified()
					|| (long) item.requireParameter(FILE_CREATION_TIME) <= 0
					|| createDataVersion(ANALYSIS_VERSION) > (int) item.requireParameter(DATA_VERSION);
		}
		return true;
	}

	@NonNull
	public static String getGpxFileDir(@NonNull OsmandApplication app, @NonNull File file) {
		if (file.getParentFile() == null) {
			return "";
		}
		File gpxDir = app.getAppPath(GPX_INDEX_DIR);
		if (file.equals(gpxDir)) {
			return "";
		}
		File relativePath = new File(file.getPath().replace(gpxDir.getPath() + "/", ""));
		String fileDir = file.isDirectory() ? relativePath.getPath() : relativePath.getParent();
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
			case "bigint":
			case "long":
				return query.getLong(index);
		}
		throw new IllegalArgumentException("Unknown column type " + parameter.getColumnType());
	}

	public static int createDataVersion(int analysisVersion) {
		return (DB_VERSION << 10) + analysisVersion;
	}
}
