package net.osmand.plus.track.helpers;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.gpx.GpxParameter.FILE_CREATION_TIME;
import static net.osmand.gpx.GpxParameter.FILE_DIR;
import static net.osmand.gpx.GpxParameter.FILE_NAME;
import static net.osmand.gpx.GpxParameter.values;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GpxParameter;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GPXDatabase {

	public static final Log LOG = PlatformUtil.getLog(GPXDatabase.class);

	private static final int DB_VERSION = 18;
	private static final String DB_NAME = "gpx_database";

	protected static final String GPX_TABLE_NAME = "gpxTable";

	private static final String TMP_NAME_COLUMN_COUNT = "itemsCount";

	public static final long UNKNOWN_TIME_THRESHOLD = 10;

	protected static final String GPX_UPDATE_PARAMETERS_START = "UPDATE " + GPX_TABLE_NAME + " SET ";
	private static final String GPX_FIND_BY_NAME_AND_DIR = " WHERE " + FILE_NAME.getColumnName() + " = ? AND " + FILE_DIR.getColumnName() + " = ?";

	private static final String GPX_MIN_CREATE_DATE = "SELECT " +
			"MIN(" + FILE_CREATION_TIME.getColumnName() + ") " +
			" FROM " + GPX_TABLE_NAME + " WHERE " + FILE_CREATION_TIME.getColumnName() +
			" > " + UNKNOWN_TIME_THRESHOLD;

	private static final String GPX_MAX_COLUMN_VALUE = "SELECT " +
			"MAX(%s) " +
			" FROM " + GPX_TABLE_NAME;

	private static final String CHANGE_NULL_TO_EMPTY_STRING_QUERY_PART = "case when %1$s is null then '' else %1$s end as %1$s";
	private static final String INCLUDE_NON_NULL_COLUMN_CONDITION = " WHERE %1$s NOT NULL AND %1$s <> '' ";
	private static final String GET_ITEM_COUNT_COLLECTION_BASE = "SELECT " +
			"%s, " +
			"count (*) as " + TMP_NAME_COLUMN_COUNT +
			" FROM " + GPX_TABLE_NAME +
			"%s" +
			" group by %s" +
			" ORDER BY %s %s";

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

	public boolean updateDataItem(@NonNull GpxDataItem item) {
		Map<GpxParameter, Object> map = GpxDbUtils.getItemParameters(item);
		return updateGpxParameters(map, GpxDbUtils.getItemRowsToSearch(app, item.getFile()));
	}

	private boolean updateGpxParameters(@NonNull Map<GpxParameter, Object> rowsToUpdate, @NonNull Map<String, Object> rowsToSearch) {
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

	private boolean updateGpxParameters(@NonNull SQLiteConnection db, @NonNull Map<GpxParameter, Object> rowsToUpdate, @NonNull Map<String, Object> rowsToSearch) {
		Map<String, Object> map = GpxDbUtils.convertGpxParameters(rowsToUpdate);
		Pair<String, Object[]> pair = AndroidDbUtils.createDbUpdateQuery(GPX_TABLE_NAME, map, rowsToSearch);
		//todo (done in ui, move to worker)
		db.execSQL(pair.first, pair.second);
		return true;
	}

	public boolean rename(@NonNull File currentFile, @NonNull File newFile) {
		Map<GpxParameter, Object> map = new LinkedHashMap<>();
		map.put(FILE_NAME, newFile.getName());
		map.put(FILE_DIR, GpxDbUtils.getGpxFileDir(app, newFile));

		return updateGpxParameters(map, GpxDbUtils.getItemRowsToSearch(app, currentFile));
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
		Map<String, Object> map = GpxDbUtils.convertGpxParameters(GpxDbUtils.getItemParameters(item));
		db.execSQL(AndroidDbUtils.createDbInsertQuery(GPX_TABLE_NAME, map.keySet()), map.values().toArray());
	}

	@NonNull
	private GpxDataItem readItem(@NonNull SQLiteCursor query) {
		String fileDir = query.getString(FILE_DIR.getSelectColumnIndex());
		String fileName = query.getString(FILE_NAME.getSelectColumnIndex());

		File gpxDir = app.getAppPath(GPX_INDEX_DIR);
		File dir = Algorithms.isEmpty(fileDir) ? gpxDir : new File(gpxDir, fileDir);

		GpxDataItem item = new GpxDataItem(app, new File(dir, fileName));
		GPXTrackAnalysis analysis = new GPXTrackAnalysis();

		for (GpxParameter parameter : values()) {
			Object value = queryColumnValue(query, parameter);
			if (parameter.isAnalysisParameter()) {
				analysis.setGpxParameter(parameter, value);
			} else {
				if (parameter == COLOR) {
					value = GPXUtilities.parseColor((String) value, 0);
				} else if (parameter == COLORING_TYPE) {
					String coloringTypeName = (String) value;
					if (ColoringType.getNullableTrackColoringTypeByName(coloringTypeName) == null &&
							GradientScaleType.getGradientTypeByName(coloringTypeName) != null) {
						GradientScaleType scaleType = GradientScaleType.getGradientTypeByName(coloringTypeName);
						ColoringType coloringType = ColoringType.fromGradientScaleType(scaleType);
						value = coloringType == null ? null : coloringType.getName(null);
					} else {
						continue;
					}
				}
				item.setParameter(parameter, value);
			}
		}
		item.setAnalysis(analysis);
		return item;
	}

	private Object queryColumnValue(@NonNull SQLiteCursor query, GpxParameter gpxParameter) {
		switch (gpxParameter.getColumnType()) {
			case "TEXT":
				return query.getString(gpxParameter.getSelectColumnIndex());
			case "double":
				return query.getDouble(gpxParameter.getSelectColumnIndex());
			case "int":
				int value = query.getInt(gpxParameter.getSelectColumnIndex());
				return gpxParameter.getTypeClass() == Boolean.class ? value == 1 : value;
			case "long":
				return query.getLong(gpxParameter.getSelectColumnIndex());
		}
		throw new IllegalArgumentException("Unknown column type " + gpxParameter.getColumnType());
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

	public String getColumnMaxValue(GpxParameter parameter) {
		String maxValue = "";
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				String queryString = String.format(GPX_MAX_COLUMN_VALUE, parameter.getColumnName());
				SQLiteCursor query = db.rawQuery(queryString, null);
				if (query != null) {
					try {
						if (query.moveToFirst()) {
							maxValue = query.getString(0);
						}
					} finally {
						query.close();
					}
				}
			} finally {
				db.close();
			}
		}
		return maxValue;
	}

	@NonNull
	public List<Pair<String, Integer>> getStringIntItemsCollection(@NonNull String columnName,
	                                                               boolean includeEmptyValues,
	                                                               boolean sortByName,
	                                                               boolean sortDescending) {
		String column1 = includeEmptyValues ? String.format(CHANGE_NULL_TO_EMPTY_STRING_QUERY_PART, columnName) : columnName;
		String includeEmptyValuesPart = includeEmptyValues ? "" : String.format(INCLUDE_NON_NULL_COLUMN_CONDITION, columnName);
		String orderBy = sortByName ? columnName : TMP_NAME_COLUMN_COUNT;
		String sortDirection = sortDescending ? "DESC" : "ASC";
		String query = String.format(GET_ITEM_COUNT_COLLECTION_BASE, column1, includeEmptyValuesPart, columnName, orderBy, sortDirection);
		return getStringIntItemsCollection(query);
	}

	@NonNull
	private List<Pair<String, Integer>> getStringIntItemsCollection(@NonNull String dataQuery) {
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

	public static int createDataVersion(int analysisVersion) {
		return (DB_VERSION << 10) + analysisVersion;
	}
}
