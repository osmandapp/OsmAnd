package net.osmand.plus.track.helpers;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.track.helpers.GpxParameter.*;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.plus.utils.AndroidDbUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GPXDatabase {

	public static final Log LOG = PlatformUtil.getLog(GPXDatabase.class);

	private static final int DB_VERSION = 16;
	private static final String DB_NAME = "gpx_database";

	protected static final String GPX_TABLE_NAME = "gpxTable";

	private static final String TMP_NAME_COLUMN_COUNT = "itemsCount";
	private static final String TMP_NAME_COLUMN_NOT_NULL = "nonnull";

	public static final long UNKNOWN_TIME_THRESHOLD = 10;

	protected static final String GPX_UPDATE_PARAMETERS_START = "UPDATE " + GPX_TABLE_NAME + " SET ";
	private static final String GPX_FIND_BY_NAME_AND_DIR = " WHERE " + GPX_COL_NAME.getColumnName() + " = ? AND " + GPX_COL_DIR.getColumnName() + " = ?";

	private static final String GPX_MIN_CREATE_DATE = "SELECT " +
			"MIN(" + GPX_COL_FILE_CREATION_TIME.getColumnName() + ") " +
			" FROM " + GPX_TABLE_NAME + " WHERE " + GPX_COL_FILE_CREATION_TIME.getColumnName() +
			" > " + UNKNOWN_TIME_THRESHOLD;

	private static final String GPX_MAX_TRACK_DURATION = "SELECT " +
			"MAX(" + GPX_COL_TOTAL_DISTANCE.getColumnName() + ") " +
			" FROM " + GPX_TABLE_NAME;

	private static final String GPX_TRACK_FOLDERS_COLLECTION = "SELECT " +
			GPX_COL_DIR.getColumnName() + ", count (*) as " + TMP_NAME_COLUMN_COUNT +
			" FROM " + GPX_TABLE_NAME +
			" group by " + GPX_COL_DIR.getColumnName() +
			" ORDER BY " + GPX_COL_DIR.getColumnName() + " ASC";

	private static final String GPX_TRACK_NEAREST_CITIES_COLLECTION = "SELECT " +
			GPX_COL_NEAREST_CITY_NAME.getColumnName() + ", count (*) as " + TMP_NAME_COLUMN_COUNT +
			" FROM " + GPX_TABLE_NAME +
			" WHERE " + GPX_COL_NEAREST_CITY_NAME.getColumnName() + " NOT NULL" + " AND " +
			GPX_COL_NEAREST_CITY_NAME.getColumnName() + " <> '' " +
			" group by " + GPX_COL_NEAREST_CITY_NAME.getColumnName() +
			" ORDER BY " + TMP_NAME_COLUMN_COUNT + " DESC";

	private static final String GPX_TRACK_COLORS_COLLECTION = "SELECT DISTINCT " +
			"case when " + GPX_COL_COLOR.getColumnName() + " is null then '' else " + GPX_COL_COLOR.getColumnName() + " end as " + TMP_NAME_COLUMN_NOT_NULL + ", " +
			"count (*) as " + TMP_NAME_COLUMN_COUNT +
			" FROM " + GPX_TABLE_NAME +
			" group by " + TMP_NAME_COLUMN_NOT_NULL +
			" ORDER BY " + TMP_NAME_COLUMN_COUNT + " DESC";

	private static final String GPX_TRACK_WIDTH_COLLECTION = "SELECT DISTINCT " +
			"case when " + GPX_COL_WIDTH.getColumnName() + " is null then '' else " + GPX_COL_WIDTH.getColumnName() + " end as " + TMP_NAME_COLUMN_NOT_NULL + ", " +
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
			if (conn == null) {
				return null;
			}
			int version = conn.getVersion();
			conn.setVersion(DB_VERSION);
			if (version == 0) {
				GpxDbUtils.onCreate(conn);
			} else {
				GpxDbUtils.onUpgrade(this, conn, version, DB_VERSION);
			}
		}
		return conn;
	}

	public boolean updateGpxParameters(@NonNull GpxDataItem item, @NonNull Map<GpxParameter<?>, Object> rowsToUpdate) {
		boolean success = updateGpxParameters(rowsToUpdate, getRowsToSearch(item.getFile()));
		if (success) {
			GpxData data = item.getGpxData();
			for (Map.Entry<GpxParameter<?>, Object> entry : rowsToUpdate.entrySet()) {
				data.setValue(entry.getKey(), entry.getValue());
			}
		}
		return success;
	}

	public boolean updateGpxParameter(@NonNull GpxDataItem item, @NonNull GpxParameter<?> parameter, @Nullable Object value) {
		if (parameter.isValidValue(value)) {
			Map<GpxParameter<?>, Object> map = Collections.singletonMap(parameter, value);
			boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
			if (success) {
				item.getGpxData().setValue(parameter, value);
			}
			return success;
		} else {
			LOG.error("Invalid value " + value + " for parameter " + parameter);
		}
		return false;
	}

	private boolean updateGpxParameters(@NonNull Map<GpxParameter<?>, Object> rowsToUpdate, @NonNull Map<String, Object> rowsToSearch) {
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

	private boolean updateGpxParameters(@NonNull SQLiteConnection db, @NonNull Map<GpxParameter<?>, Object> rowsToUpdate, @NonNull Map<String, Object> rowsToSearch) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (Map.Entry<GpxParameter<?>, Object> entry : rowsToUpdate.entrySet()) {
			GpxParameter<?> parameter = entry.getKey();
			Object value = parameter.convertToDbValue(entry.getValue());
			map.put(parameter.getColumnName(), value);
		}
		Pair<String, Object[]> pair = AndroidDbUtils.createDbUpdateQuery(GPX_TABLE_NAME, map, rowsToSearch);
		db.execSQL(pair.first, pair.second);
		return true;
	}

	@NonNull
	private Map<String, Object> getRowsToSearch(@NonNull File file) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put(GPX_COL_NAME.getColumnName(), file.getName());
		map.put(GPX_COL_DIR.getColumnName(), GpxDbUtils.getGpxFileDir(app, file));
		return map;
	}

	public boolean rename(@NonNull File currentFile, @NonNull File newFile) {
		Map<GpxParameter<?>, Object> map = new LinkedHashMap<>();
		map.put(GPX_COL_NAME, newFile.getName());
		map.put(GPX_COL_DIR, GpxDbUtils.getGpxFileDir(app, newFile));

		return updateGpxParameters(map, getRowsToSearch(currentFile));
	}

	public boolean remove(@NonNull File file) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				String fileName = file.getName();
				String fileDir = GpxDbUtils.getGpxFileDir(app, file);
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

	void insert(@NonNull GpxDataItem item, @NonNull SQLiteConnection db) {
		File file = item.getFile();
		GpxData data = item.getGpxData();
		String fileName = file.getName();
		String fileDir = GpxDbUtils.getGpxFileDir(app, file);
		GPXTrackAnalysis analysis = data.getAnalysis();

		Map<String, Object> rowsMap = new LinkedHashMap<>();
		rowsMap.put(GPX_COL_NAME.getColumnName(), fileName);
		rowsMap.put(GPX_COL_DIR.getColumnName(), fileDir);
		rowsMap.put(GPX_COL_COLOR.getColumnName(), data.getValue(GPX_COL_COLOR));
		rowsMap.put(GPX_COL_FILE_LAST_MODIFIED_TIME.getColumnName(), file.lastModified());
		rowsMap.put(GPX_COL_FILE_LAST_UPLOADED_TIME.getColumnName(), data.getValue(GPX_COL_FILE_LAST_UPLOADED_TIME));
		rowsMap.put(GPX_COL_FILE_CREATION_TIME.getColumnName(), data.getValue(GPX_COL_FILE_CREATION_TIME));
		rowsMap.put(GPX_COL_SPLIT_TYPE.getColumnName(), data.getValue(GPX_COL_SPLIT_TYPE));
		rowsMap.put(GPX_COL_SPLIT_INTERVAL.getColumnName(), data.getValue(GPX_COL_SPLIT_INTERVAL));
		rowsMap.put(GPX_COL_API_IMPORTED.getColumnName(), data.getValue(GPX_COL_API_IMPORTED));
		rowsMap.put(GPX_COL_SHOW_AS_MARKERS.getColumnName(), data.getValue(GPX_COL_SHOW_AS_MARKERS));
		rowsMap.put(GPX_COL_JOIN_SEGMENTS.getColumnName(), data.getValue(GPX_COL_JOIN_SEGMENTS));
		rowsMap.put(GPX_COL_SHOW_ARROWS.getColumnName(), data.getValue(GPX_COL_SHOW_ARROWS));
		rowsMap.put(GPX_COL_SHOW_START_FINISH.getColumnName(), data.getValue(GPX_COL_SHOW_START_FINISH));
		rowsMap.put(GPX_COL_WIDTH.getColumnName(), data.getValue(GPX_COL_WIDTH));
		rowsMap.put(GPX_COL_COLORING_TYPE.getColumnName(), data.getValue(GPX_COL_COLORING_TYPE));
		rowsMap.put(GPX_COL_SMOOTHING_THRESHOLD.getColumnName(), data.getValue(GPX_COL_SMOOTHING_THRESHOLD));
		rowsMap.put(GPX_COL_MIN_FILTER_SPEED.getColumnName(), data.getValue(GPX_COL_MIN_FILTER_SPEED));
		rowsMap.put(GPX_COL_MAX_FILTER_SPEED.getColumnName(), data.getValue(GPX_COL_MAX_FILTER_SPEED));
		rowsMap.put(GPX_COL_MIN_FILTER_ALTITUDE.getColumnName(), data.getValue(GPX_COL_MIN_FILTER_ALTITUDE));
		rowsMap.put(GPX_COL_MAX_FILTER_ALTITUDE.getColumnName(), data.getValue(GPX_COL_MAX_FILTER_ALTITUDE));
		rowsMap.put(GPX_COL_MAX_FILTER_HDOP.getColumnName(), data.getValue(GPX_COL_MAX_FILTER_HDOP));
		rowsMap.put(GPX_COL_NEAREST_CITY_NAME.getColumnName(), data.getValue(GPX_COL_NEAREST_CITY_NAME));

		if (analysis != null) {
			rowsMap.put(GPX_COL_TOTAL_DISTANCE.getColumnName(), analysis.totalDistance);
			rowsMap.put(GPX_COL_TOTAL_TRACKS.getColumnName(), analysis.totalTracks);
			rowsMap.put(GPX_COL_START_TIME.getColumnName(), analysis.startTime);
			rowsMap.put(GPX_COL_END_TIME.getColumnName(), analysis.endTime);
			rowsMap.put(GPX_COL_TIME_SPAN.getColumnName(), analysis.timeSpan);
			rowsMap.put(GPX_COL_TIME_MOVING.getColumnName(), analysis.timeMoving);
			rowsMap.put(GPX_COL_TOTAL_DISTANCE_MOVING.getColumnName(), analysis.totalDistanceMoving);
			rowsMap.put(GPX_COL_DIFF_ELEVATION_UP.getColumnName(), analysis.diffElevationUp);
			rowsMap.put(GPX_COL_DIFF_ELEVATION_DOWN.getColumnName(), analysis.diffElevationDown);
			rowsMap.put(GPX_COL_AVG_ELEVATION.getColumnName(), analysis.avgElevation);
			rowsMap.put(GPX_COL_MIN_ELEVATION.getColumnName(), analysis.minElevation);
			rowsMap.put(GPX_COL_MAX_ELEVATION.getColumnName(), analysis.maxElevation);
			rowsMap.put(GPX_COL_MAX_SPEED.getColumnName(), analysis.maxSpeed);
			rowsMap.put(GPX_COL_AVG_SPEED.getColumnName(), analysis.avgSpeed);
			rowsMap.put(GPX_COL_POINTS.getColumnName(), analysis.points);
			rowsMap.put(GPX_COL_WPT_POINTS.getColumnName(), analysis.wptPoints);
			rowsMap.put(GPX_COL_WPT_CATEGORY_NAMES.getColumnName(), Algorithms.encodeCollection(analysis.wptCategoryNames));
			rowsMap.put(GPX_COL_START_LAT.getColumnName(), analysis.latLonStart != null ? analysis.latLonStart.getLatitude() : null);
			rowsMap.put(GPX_COL_START_LON.getColumnName(), analysis.latLonStart != null ? analysis.latLonStart.getLongitude() : null);
		}
		db.execSQL(AndroidDbUtils.createDbInsertQuery(GPX_TABLE_NAME, rowsMap.keySet()), rowsMap.values().toArray());
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

		Map<GpxParameter<?>, Object> map = new LinkedHashMap<>();
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
			GpxData data = item.getGpxData();
			data.setAnalysis(analysis);
			data.setValue(GPX_COL_FILE_LAST_MODIFIED_TIME, fileLastModifiedTime);
		}
		return success;
	}

	@NonNull
	private GpxDataItem readItem(@NonNull SQLiteCursor query) {
		String fileDir = query.getString(GPX_COL_DIR.getSelectColumnIndex());
		String fileName = query.getString(GPX_COL_NAME.getSelectColumnIndex());

		File gpxDir = app.getAppPath(GPX_INDEX_DIR);
		File dir = Algorithms.isEmpty(fileDir) ? gpxDir : new File(gpxDir, fileDir);

		GpxDataItem item = new GpxDataItem(new File(dir, fileName));
		GpxData data = item.getGpxData();

		GPXTrackAnalysis analysis = new GPXTrackAnalysis();
		analysis.totalDistance = (float) query.getDouble(GPX_COL_TOTAL_DISTANCE.getSelectColumnIndex());
		analysis.totalTracks = query.getInt(GPX_COL_TOTAL_TRACKS.getSelectColumnIndex());
		analysis.startTime = query.getLong(GPX_COL_START_TIME.getSelectColumnIndex());
		analysis.endTime = query.getLong(GPX_COL_END_TIME.getSelectColumnIndex());
		analysis.timeSpan = query.getLong(GPX_COL_TIME_SPAN.getSelectColumnIndex());
		analysis.timeMoving = query.getLong(GPX_COL_TIME_MOVING.getSelectColumnIndex());
		analysis.totalDistanceMoving = (float) query.getDouble(GPX_COL_TOTAL_DISTANCE_MOVING.getSelectColumnIndex());
		analysis.diffElevationUp = query.getDouble(GPX_COL_DIFF_ELEVATION_UP.getSelectColumnIndex());
		analysis.diffElevationDown = query.getDouble(GPX_COL_DIFF_ELEVATION_DOWN.getSelectColumnIndex());
		analysis.avgElevation = query.getDouble(GPX_COL_AVG_ELEVATION.getSelectColumnIndex());
		analysis.minElevation = query.getDouble(GPX_COL_MIN_ELEVATION.getSelectColumnIndex());
		analysis.maxElevation = query.getDouble(GPX_COL_MAX_ELEVATION.getSelectColumnIndex());
		analysis.minSpeed = (float) query.getDouble(GPX_COL_MAX_SPEED.getSelectColumnIndex());
		analysis.maxSpeed = (float) query.getDouble(GPX_COL_MAX_SPEED.getSelectColumnIndex());
		analysis.avgSpeed = (float) query.getDouble(GPX_COL_AVG_SPEED.getSelectColumnIndex());
		analysis.points = query.getInt(GPX_COL_POINTS.getSelectColumnIndex());
		analysis.wptPoints = query.getInt(GPX_COL_WPT_POINTS.getSelectColumnIndex());

		String names = query.getString(GPX_COL_WPT_CATEGORY_NAMES.getSelectColumnIndex());
		analysis.wptCategoryNames = names != null ? Algorithms.decodeStringSet(names) : null;

		if (!query.isNull(GPX_COL_START_LAT.getSelectColumnIndex()) && !query.isNull(GPX_COL_START_LON.getSelectColumnIndex())) {
			double lat = query.getDouble(GPX_COL_START_LAT.getSelectColumnIndex());
			double lon = query.getDouble(GPX_COL_START_LON.getSelectColumnIndex());
			analysis.latLonStart = new LatLon(lat, lon);
		}
		data.setAnalysis(analysis);
		data.setValue(GPX_COL_COLOR, GPXUtilities.parseColor(query.getString(GPX_COL_COLOR.getSelectColumnIndex()), 0));
		data.setValue(GPX_COL_FILE_LAST_MODIFIED_TIME, query.getLong(GPX_COL_FILE_LAST_MODIFIED_TIME.getSelectColumnIndex()));
		data.setValue(GPX_COL_FILE_LAST_UPLOADED_TIME, query.getLong(GPX_COL_FILE_LAST_UPLOADED_TIME.getSelectColumnIndex()));
		data.setValue(GPX_COL_FILE_CREATION_TIME, query.isNull(GPX_COL_FILE_CREATION_TIME.getSelectColumnIndex()) ? -1 : query.getLong(GPX_COL_FILE_CREATION_TIME.getSelectColumnIndex()));
		data.setValue(GPX_COL_SPLIT_TYPE, query.getInt(GPX_COL_SPLIT_TYPE.getSelectColumnIndex()));
		data.setValue(GPX_COL_SPLIT_INTERVAL, query.getDouble(GPX_COL_SPLIT_INTERVAL.getSelectColumnIndex()));
		data.setValue(GPX_COL_API_IMPORTED, query.getInt(GPX_COL_API_IMPORTED.getSelectColumnIndex()) == 1);
		data.setValue(GPX_COL_SHOW_AS_MARKERS, query.getInt(GPX_COL_SHOW_AS_MARKERS.getSelectColumnIndex()) == 1);
		data.setValue(GPX_COL_JOIN_SEGMENTS, query.getInt(GPX_COL_JOIN_SEGMENTS.getSelectColumnIndex()) == 1);
		data.setValue(GPX_COL_SHOW_ARROWS, query.getInt(GPX_COL_SHOW_ARROWS.getSelectColumnIndex()) == 1);
		data.setValue(GPX_COL_SHOW_START_FINISH, query.getInt(GPX_COL_SHOW_START_FINISH.getSelectColumnIndex()) == 1);
		data.setValue(GPX_COL_WIDTH, query.getString(GPX_COL_WIDTH.getSelectColumnIndex()));
		data.setValue(GPX_COL_NEAREST_CITY_NAME, query.getString(GPX_COL_NEAREST_CITY_NAME.getSelectColumnIndex()));
		data.setValue(GPX_COL_SMOOTHING_THRESHOLD, query.getDouble(GPX_COL_SMOOTHING_THRESHOLD.getSelectColumnIndex()));
		data.setValue(GPX_COL_MIN_FILTER_SPEED, query.getDouble(GPX_COL_MIN_FILTER_SPEED.getSelectColumnIndex()));
		data.setValue(GPX_COL_MAX_FILTER_SPEED, query.getDouble(GPX_COL_MAX_FILTER_SPEED.getSelectColumnIndex()));
		data.setValue(GPX_COL_MIN_FILTER_ALTITUDE, query.getDouble(GPX_COL_MIN_FILTER_ALTITUDE.getSelectColumnIndex()));
		data.setValue(GPX_COL_MAX_FILTER_ALTITUDE, query.getDouble(GPX_COL_MAX_FILTER_ALTITUDE.getSelectColumnIndex()));
		data.setValue(GPX_COL_MAX_FILTER_HDOP, query.getDouble(GPX_COL_MAX_FILTER_HDOP.getSelectColumnIndex()));

		String coloringTypeName = query.getString(GPX_COL_COLORING_TYPE.getSelectColumnIndex());
		if (ColoringType.getNullableTrackColoringTypeByName(coloringTypeName) != null) {
			data.setValue(GPX_COL_COLORING_TYPE, coloringTypeName);
		} else if (GradientScaleType.getGradientTypeByName(coloringTypeName) != null) {
			GradientScaleType scaleType = GradientScaleType.getGradientTypeByName(coloringTypeName);
			ColoringType coloringType = ColoringType.fromGradientScaleType(scaleType);
			data.setValue(GPX_COL_COLORING_TYPE, coloringType == null ? null : coloringType.getName(null));
		}
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

	@NonNull
	public List<Pair<String, Integer>> getTrackFolders() {
		return getStringIntItemsCollection(GPX_TRACK_FOLDERS_COLLECTION);
	}

	@NonNull
	public List<Pair<String, Integer>> getNearestCityCollection() {
		return getStringIntItemsCollection(GPX_TRACK_NEAREST_CITIES_COLLECTION);
	}

	@NonNull
	public List<Pair<String, Integer>> getTrackColorsCollection() {
		return getStringIntItemsCollection(GPX_TRACK_COLORS_COLLECTION);
	}

	@NonNull
	public List<Pair<String, Integer>> getTrackWidthCollection() {
		return getStringIntItemsCollection(GPX_TRACK_WIDTH_COLLECTION);
	}

	@NonNull
	public List<Pair<String, Integer>> getStringIntItemsCollection(@NonNull String dataQuery) {
		List<Pair<String, Integer>> folderCollection = new ArrayList<>();
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
				SQLiteCursor query = db.rawQuery(GpxDbUtils.getSelectQuery(), null);
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
	public GpxDataItem getItem(@NonNull File file) {
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
	public GpxDataItem getItem(@NonNull File file, @NonNull SQLiteConnection db) {
		String fileName = file.getName();
		String fileDir = GpxDbUtils.getGpxFileDir(app, file);
		SQLiteCursor query = db.rawQuery(GpxDbUtils.getSelectQuery() + GPX_FIND_BY_NAME_AND_DIR, new String[] {fileName, fileDir});
		if (query != null) {
			try {
				if (query.moveToFirst()) {
					return readItem(query);
				}
			} finally {
				query.close();
			}
		}
		return null;
	}
}
