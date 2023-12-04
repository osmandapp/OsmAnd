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
	private static final String GPX_FIND_BY_NAME_AND_DIR = " WHERE " + FILE_NAME.getColumnName() + " = ? AND " + FILE_DIR.getColumnName() + " = ?";

	private static final String GPX_MIN_CREATE_DATE = "SELECT " +
			"MIN(" + FILE_CREATION_TIME.getColumnName() + ") " +
			" FROM " + GPX_TABLE_NAME + " WHERE " + FILE_CREATION_TIME.getColumnName() +
			" > " + UNKNOWN_TIME_THRESHOLD;

	private static final String GPX_MAX_TRACK_DURATION = "SELECT " +
			"MAX(" + TOTAL_DISTANCE.getColumnName() + ") " +
			" FROM " + GPX_TABLE_NAME;

	private static final String GPX_TRACK_FOLDERS_COLLECTION = "SELECT " +
			FILE_DIR.getColumnName() + ", count (*) as " + TMP_NAME_COLUMN_COUNT +
			" FROM " + GPX_TABLE_NAME +
			" group by " + FILE_DIR.getColumnName() +
			" ORDER BY " + FILE_DIR.getColumnName() + " ASC";

	private static final String GPX_TRACK_NEAREST_CITIES_COLLECTION = "SELECT " +
			NEAREST_CITY_NAME.getColumnName() + ", count (*) as " + TMP_NAME_COLUMN_COUNT +
			" FROM " + GPX_TABLE_NAME +
			" WHERE " + NEAREST_CITY_NAME.getColumnName() + " NOT NULL" + " AND " +
			NEAREST_CITY_NAME.getColumnName() + " <> '' " +
			" group by " + NEAREST_CITY_NAME.getColumnName() +
			" ORDER BY " + TMP_NAME_COLUMN_COUNT + " DESC";

	private static final String GPX_TRACK_COLORS_COLLECTION = "SELECT DISTINCT " +
			"case when " + COLOR.getColumnName() + " is null then '' else " + COLOR.getColumnName() + " end as " + TMP_NAME_COLUMN_NOT_NULL + ", " +
			"count (*) as " + TMP_NAME_COLUMN_COUNT +
			" FROM " + GPX_TABLE_NAME +
			" group by " + TMP_NAME_COLUMN_NOT_NULL +
			" ORDER BY " + TMP_NAME_COLUMN_COUNT + " DESC";

	private static final String GPX_TRACK_WIDTH_COLLECTION = "SELECT DISTINCT " +
			"case when " + WIDTH.getColumnName() + " is null then '' else " + WIDTH.getColumnName() + " end as " + TMP_NAME_COLUMN_NOT_NULL + ", " +
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
			for (Map.Entry<GpxParameter<?>, Object> entry : rowsToUpdate.entrySet()) {
				item.setValue(entry.getKey(), entry.getValue());
			}
		}
		return success;
	}

	public boolean updateGpxParameter(@NonNull GpxDataItem item, @NonNull GpxParameter<?> parameter, @Nullable Object value) {
		if (parameter.isValidValue(value)) {
			Map<GpxParameter<?>, Object> map = Collections.singletonMap(parameter, value);
			boolean success = updateGpxParameters(map, getRowsToSearch(item.getFile()));
			if (success) {
				item.setValue(parameter, value);
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
		map.put(FILE_NAME.getColumnName(), file.getName());
		map.put(FILE_DIR.getColumnName(), GpxDbUtils.getGpxFileDir(app, file));
		return map;
	}

	public boolean rename(@NonNull File currentFile, @NonNull File newFile) {
		Map<GpxParameter<?>, Object> map = new LinkedHashMap<>();
		map.put(FILE_NAME, newFile.getName());
		map.put(FILE_DIR, GpxDbUtils.getGpxFileDir(app, newFile));

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
		String fileName = file.getName();
		String fileDir = GpxDbUtils.getGpxFileDir(app, file);
		GPXTrackAnalysis analysis = item.getAnalysis();

		Map<String, Object> rowsMap = new LinkedHashMap<>();
		rowsMap.put(FILE_NAME.getColumnName(), fileName);
		rowsMap.put(FILE_DIR.getColumnName(), fileDir);
		rowsMap.put(COLOR.getColumnName(), item.getValue(COLOR));
		rowsMap.put(FILE_LAST_MODIFIED_TIME.getColumnName(), file.lastModified());
		rowsMap.put(FILE_LAST_UPLOADED_TIME.getColumnName(), item.getValue(FILE_LAST_UPLOADED_TIME));
		rowsMap.put(FILE_CREATION_TIME.getColumnName(), item.getValue(FILE_CREATION_TIME));
		rowsMap.put(SPLIT_TYPE.getColumnName(), item.getValue(SPLIT_TYPE));
		rowsMap.put(SPLIT_INTERVAL.getColumnName(), item.getValue(SPLIT_INTERVAL));
		rowsMap.put(API_IMPORTED.getColumnName(), item.getValue(API_IMPORTED));
		rowsMap.put(SHOW_AS_MARKERS.getColumnName(), item.getValue(SHOW_AS_MARKERS));
		rowsMap.put(JOIN_SEGMENTS.getColumnName(), item.getValue(JOIN_SEGMENTS));
		rowsMap.put(SHOW_ARROWS.getColumnName(), item.getValue(SHOW_ARROWS));
		rowsMap.put(SHOW_START_FINISH.getColumnName(), item.getValue(SHOW_START_FINISH));
		rowsMap.put(WIDTH.getColumnName(), item.getValue(WIDTH));
		rowsMap.put(COLORING_TYPE.getColumnName(), item.getValue(COLORING_TYPE));
		rowsMap.put(SMOOTHING_THRESHOLD.getColumnName(), item.getValue(SMOOTHING_THRESHOLD));
		rowsMap.put(MIN_FILTER_SPEED.getColumnName(), item.getValue(MIN_FILTER_SPEED));
		rowsMap.put(MAX_FILTER_SPEED.getColumnName(), item.getValue(MAX_FILTER_SPEED));
		rowsMap.put(MIN_FILTER_ALTITUDE.getColumnName(), item.getValue(MIN_FILTER_ALTITUDE));
		rowsMap.put(MAX_FILTER_ALTITUDE.getColumnName(), item.getValue(MAX_FILTER_ALTITUDE));
		rowsMap.put(MAX_FILTER_HDOP.getColumnName(), item.getValue(MAX_FILTER_HDOP));
		rowsMap.put(NEAREST_CITY_NAME.getColumnName(), item.getValue(NEAREST_CITY_NAME));

		if (analysis != null) {
			rowsMap.put(TOTAL_DISTANCE.getColumnName(), analysis.totalDistance);
			rowsMap.put(TOTAL_TRACKS.getColumnName(), analysis.totalTracks);
			rowsMap.put(START_TIME.getColumnName(), analysis.startTime);
			rowsMap.put(END_TIME.getColumnName(), analysis.endTime);
			rowsMap.put(TIME_SPAN.getColumnName(), analysis.timeSpan);
			rowsMap.put(TIME_MOVING.getColumnName(), analysis.timeMoving);
			rowsMap.put(TOTAL_DISTANCE_MOVING.getColumnName(), analysis.totalDistanceMoving);
			rowsMap.put(DIFF_ELEVATION_UP.getColumnName(), analysis.diffElevationUp);
			rowsMap.put(DIFF_ELEVATION_DOWN.getColumnName(), analysis.diffElevationDown);
			rowsMap.put(AVG_ELEVATION.getColumnName(), analysis.avgElevation);
			rowsMap.put(MIN_ELEVATION.getColumnName(), analysis.minElevation);
			rowsMap.put(MAX_ELEVATION.getColumnName(), analysis.maxElevation);
			rowsMap.put(MAX_SPEED.getColumnName(), analysis.maxSpeed);
			rowsMap.put(AVG_SPEED.getColumnName(), analysis.avgSpeed);
			rowsMap.put(POINTS.getColumnName(), analysis.points);
			rowsMap.put(WPT_POINTS.getColumnName(), analysis.wptPoints);
			rowsMap.put(WPT_CATEGORY_NAMES.getColumnName(), Algorithms.encodeCollection(analysis.wptCategoryNames));
			rowsMap.put(START_LAT.getColumnName(), analysis.latLonStart != null ? analysis.latLonStart.getLatitude() : null);
			rowsMap.put(START_LON.getColumnName(), analysis.latLonStart != null ? analysis.latLonStart.getLongitude() : null);
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
		map.put(TOTAL_DISTANCE, hasAnalysis ? analysis.totalDistance : null);
		map.put(TOTAL_TRACKS, hasAnalysis ? analysis.totalTracks : null);
		map.put(START_TIME, hasAnalysis ? analysis.startTime : null);
		map.put(END_TIME, hasAnalysis ? analysis.endTime : null);
		map.put(TIME_SPAN, hasAnalysis ? analysis.timeSpan : null);
		map.put(TIME_MOVING, hasAnalysis ? analysis.timeMoving : null);
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
		map.put(FILE_LAST_MODIFIED_TIME, fileLastModifiedTime);
		map.put(WPT_CATEGORY_NAMES, hasAnalysis ? Algorithms.encodeCollection(analysis.wptCategoryNames) : null);
		map.put(START_LAT, hasAnalysis && analysis.latLonStart != null ? analysis.latLonStart.getLatitude() : null);
		map.put(START_LON, hasAnalysis && analysis.latLonStart != null ? analysis.latLonStart.getLongitude() : null);

		boolean success = updateGpxParameters(db, map, getRowsToSearch(item.getFile()));
		if (success) {
			item.setAnalysis(analysis);
			item.setValue(FILE_LAST_MODIFIED_TIME, fileLastModifiedTime);
		}
		return success;
	}

	@NonNull
	private GpxDataItem readItem(@NonNull SQLiteCursor query) {
		String fileDir = query.getString(FILE_DIR.getSelectColumnIndex());
		String fileName = query.getString(FILE_NAME.getSelectColumnIndex());

		File gpxDir = app.getAppPath(GPX_INDEX_DIR);
		File dir = Algorithms.isEmpty(fileDir) ? gpxDir : new File(gpxDir, fileDir);

		GpxDataItem item = new GpxDataItem(new File(dir, fileName));
		GPXTrackAnalysis analysis = new GPXTrackAnalysis();

		analysis.totalDistance = (float) query.getDouble(TOTAL_DISTANCE.getSelectColumnIndex());
		analysis.totalTracks = query.getInt(TOTAL_TRACKS.getSelectColumnIndex());
		analysis.startTime = query.getLong(START_TIME.getSelectColumnIndex());
		analysis.endTime = query.getLong(END_TIME.getSelectColumnIndex());
		analysis.timeSpan = query.getLong(TIME_SPAN.getSelectColumnIndex());
		analysis.timeMoving = query.getLong(TIME_MOVING.getSelectColumnIndex());
		analysis.totalDistanceMoving = (float) query.getDouble(TOTAL_DISTANCE_MOVING.getSelectColumnIndex());
		analysis.diffElevationUp = query.getDouble(DIFF_ELEVATION_UP.getSelectColumnIndex());
		analysis.diffElevationDown = query.getDouble(DIFF_ELEVATION_DOWN.getSelectColumnIndex());
		analysis.avgElevation = query.getDouble(AVG_ELEVATION.getSelectColumnIndex());
		analysis.minElevation = query.getDouble(MIN_ELEVATION.getSelectColumnIndex());
		analysis.maxElevation = query.getDouble(MAX_ELEVATION.getSelectColumnIndex());
		analysis.minSpeed = (float) query.getDouble(MAX_SPEED.getSelectColumnIndex());
		analysis.maxSpeed = (float) query.getDouble(MAX_SPEED.getSelectColumnIndex());
		analysis.avgSpeed = (float) query.getDouble(AVG_SPEED.getSelectColumnIndex());
		analysis.points = query.getInt(POINTS.getSelectColumnIndex());
		analysis.wptPoints = query.getInt(WPT_POINTS.getSelectColumnIndex());

		String names = query.getString(WPT_CATEGORY_NAMES.getSelectColumnIndex());
		analysis.wptCategoryNames = names != null ? Algorithms.decodeStringSet(names) : null;

		if (!query.isNull(START_LAT.getSelectColumnIndex()) && !query.isNull(START_LON.getSelectColumnIndex())) {
			double lat = query.getDouble(START_LAT.getSelectColumnIndex());
			double lon = query.getDouble(START_LON.getSelectColumnIndex());
			analysis.latLonStart = new LatLon(lat, lon);
		}
		item.setAnalysis(analysis);
		item.setValue(COLOR, GPXUtilities.parseColor(query.getString(COLOR.getSelectColumnIndex()), 0));
		item.setValue(FILE_LAST_MODIFIED_TIME, query.getLong(FILE_LAST_MODIFIED_TIME.getSelectColumnIndex()));
		item.setValue(FILE_LAST_UPLOADED_TIME, query.getLong(FILE_LAST_UPLOADED_TIME.getSelectColumnIndex()));
		item.setValue(FILE_CREATION_TIME, query.isNull(FILE_CREATION_TIME.getSelectColumnIndex()) ? -1 : query.getLong(FILE_CREATION_TIME.getSelectColumnIndex()));
		item.setValue(SPLIT_TYPE, query.getInt(SPLIT_TYPE.getSelectColumnIndex()));
		item.setValue(SPLIT_INTERVAL, query.getDouble(SPLIT_INTERVAL.getSelectColumnIndex()));
		item.setValue(API_IMPORTED, query.getInt(API_IMPORTED.getSelectColumnIndex()) == 1);
		item.setValue(SHOW_AS_MARKERS, query.getInt(SHOW_AS_MARKERS.getSelectColumnIndex()) == 1);
		item.setValue(JOIN_SEGMENTS, query.getInt(JOIN_SEGMENTS.getSelectColumnIndex()) == 1);
		item.setValue(SHOW_ARROWS, query.getInt(SHOW_ARROWS.getSelectColumnIndex()) == 1);
		item.setValue(SHOW_START_FINISH, query.getInt(SHOW_START_FINISH.getSelectColumnIndex()) == 1);
		item.setValue(WIDTH, query.getString(WIDTH.getSelectColumnIndex()));
		item.setValue(NEAREST_CITY_NAME, query.getString(NEAREST_CITY_NAME.getSelectColumnIndex()));
		item.setValue(SMOOTHING_THRESHOLD, query.getDouble(SMOOTHING_THRESHOLD.getSelectColumnIndex()));
		item.setValue(MIN_FILTER_SPEED, query.getDouble(MIN_FILTER_SPEED.getSelectColumnIndex()));
		item.setValue(MAX_FILTER_SPEED, query.getDouble(MAX_FILTER_SPEED.getSelectColumnIndex()));
		item.setValue(MIN_FILTER_ALTITUDE, query.getDouble(MIN_FILTER_ALTITUDE.getSelectColumnIndex()));
		item.setValue(MAX_FILTER_ALTITUDE, query.getDouble(MAX_FILTER_ALTITUDE.getSelectColumnIndex()));
		item.setValue(MAX_FILTER_HDOP, query.getDouble(MAX_FILTER_HDOP.getSelectColumnIndex()));

		String coloringTypeName = query.getString(COLORING_TYPE.getSelectColumnIndex());
		if (ColoringType.getNullableTrackColoringTypeByName(coloringTypeName) != null) {
			item.setValue(COLORING_TYPE, coloringTypeName);
		} else if (GradientScaleType.getGradientTypeByName(coloringTypeName) != null) {
			GradientScaleType scaleType = GradientScaleType.getGradientTypeByName(coloringTypeName);
			ColoringType coloringType = ColoringType.fromGradientScaleType(scaleType);
			item.setValue(COLORING_TYPE, coloringType == null ? null : coloringType.getName(null));
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
