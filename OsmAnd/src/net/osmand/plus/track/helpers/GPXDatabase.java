package net.osmand.plus.track.helpers;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.gpx.GpxParameter.FILE_CREATION_TIME;
import static net.osmand.gpx.GpxParameter.FILE_DIR;
import static net.osmand.gpx.GpxParameter.FILE_NAME;
import static net.osmand.plus.card.color.ColoringPurpose.TRACK;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GPXDatabase {

	public static final Log LOG = PlatformUtil.getLog(GPXDatabase.class);

	protected static final int DB_VERSION = 27;
	private static final String DB_NAME = "gpx_database";

	protected static final String GPX_TABLE_NAME = "gpxTable";
	protected static final String GPX_DIR_TABLE_NAME = "gpxDirTable";

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
	private static final String GET_ITEM_COUNT_COLLECTION_BASE = "SELECT " + "%s, " + "count (*) as "
			+ TMP_NAME_COLUMN_COUNT + " FROM " + GPX_TABLE_NAME + "%s" + " group by %s" + " ORDER BY %s %s";

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
			conn.setVersion(DB_VERSION); // not correct version but dangerous for crash loop
			if (version == 0) {
				GpxDbUtils.onCreate(conn);
			} else {
				GpxDbUtils.onUpgrade(this, conn, version, DB_VERSION);
			}
//			conn.setVersion(DB_VERSION); // correct version but dangerous for crash loop
		}
		return conn;
	}

	public boolean updateDataItem(@NonNull DataItem item) {
		File file = item.getFile();
		String tableName = GpxDbUtils.getTableName(file);
		Map<GpxParameter, Object> map = GpxDbUtils.getItemParameters(item);

		return updateGpxParameters(map, tableName, GpxDbUtils.getItemRowsToSearch(app, file));
	}

	private boolean updateGpxParameters(@NonNull Map<GpxParameter, Object> rowsToUpdate,
	                                    @NonNull String tableName, @NonNull Map<String, Object> rowsToSearch) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				return updateGpxParameters(db, tableName, rowsToUpdate, rowsToSearch);
			} finally {
				db.close();
			}
		}
		return false;
	}

	private boolean updateGpxParameters(@NonNull SQLiteConnection db, @NonNull String tableName,
	                                    @NonNull Map<GpxParameter, Object> rowsToUpdate,
	                                    @NonNull Map<String, Object> rowsToSearch) {
		Map<String, Object> map = GpxDbUtils.convertGpxParameters(rowsToUpdate);
		Pair<String, Object[]> pair = AndroidDbUtils.createDbUpdateQuery(tableName, map, rowsToSearch);
		//todo (done in ui, move to worker)
		db.execSQL(pair.first, pair.second);
		return true;
	}

	public boolean rename(@NonNull File currentFile, @NonNull File newFile) {
		Map<GpxParameter, Object> map = new LinkedHashMap<>();
		map.put(FILE_NAME, newFile.getName());
		map.put(FILE_DIR, GpxDbUtils.getGpxFileDir(app, newFile));

		String tableName = GpxDbUtils.getTableName(currentFile);
		return updateGpxParameters(map, tableName, GpxDbUtils.getItemRowsToSearch(app, currentFile));
	}

	public boolean remove(@NonNull File file) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				String fileName = file.getName();
				String fileDir = GpxDbUtils.getGpxFileDir(app, file);
				String tableName = GpxDbUtils.getTableName(file);
				db.execSQL("DELETE FROM " + tableName + GPX_FIND_BY_NAME_AND_DIR, new Object[] {fileName, fileDir});
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean add(@NonNull DataItem item) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				insertItem(item, db);
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	void insertItem(@NonNull DataItem item, @NonNull SQLiteConnection db) {
		File file = item.getFile();
		String tableName = GpxDbUtils.getTableName(file);
		Map<String, Object> map = GpxDbUtils.convertGpxParameters(GpxDbUtils.getItemParameters(item));
		db.execSQL(AndroidDbUtils.createDbInsertQuery(tableName, map.keySet()), map.values().toArray());
	}

	@NonNull
	private GpxDataItem readGpxDataItem(@NonNull SQLiteCursor query) {
		File file = readItemFile(query);
		GpxDataItem item = new GpxDataItem(app, file);
		GPXTrackAnalysis analysis = new GPXTrackAnalysis();
		processItemParameters(item, query, Arrays.asList(GpxParameter.values()), analysis);
		item.setAnalysis(analysis);

		return item;
	}

	@NonNull
	private File readItemFile(@NonNull SQLiteCursor query) {
		String fileDir = query.getString(query.getColumnIndex(FILE_DIR.getColumnName()));
		String fileName = query.getString(query.getColumnIndex(FILE_NAME.getColumnName()));

		File gpxDir = app.getAppPath(GPX_INDEX_DIR);
		if ((fileName + "/").equalsIgnoreCase(GPX_INDEX_DIR)) {
			return gpxDir;
		}
		fileDir = fileDir.replace(gpxDir.getPath(), "");
		fileDir = fileDir.replace(app.getAppPath(null).getPath(), "");
		File dir = Algorithms.isEmpty(fileDir) ? gpxDir : new File(gpxDir, fileDir);
		return new File(dir, fileName);
	}

	private void processItemParameters(@NonNull DataItem item, @NonNull SQLiteCursor query,
	                                   @NonNull List<GpxParameter> parameters,
	                                   @Nullable GPXTrackAnalysis analysis) {
		for (GpxParameter parameter : parameters) {
			Object value = GpxDbUtils.queryColumnValue(query, parameter);
			if (value == null && !parameter.isAppearanceParameter()) {
				value = parameter.getDefaultValue();
			}
			if (parameter.isAnalysisParameter()) {
				if (analysis != null) {
					analysis.setGpxParameter(parameter, value);
				}
			} else {
				if (parameter == COLOR) {
					value = GPXUtilities.parseColor((String) value);
				} else if (parameter == COLORING_TYPE) {
					String type = (String) value;
					ColoringType coloringType = ColoringType.valueOf(TRACK, type);
					GradientScaleType scaleType = GradientScaleType.getGradientTypeByName(type);
					if (coloringType == null && scaleType != null) {
						coloringType = ColoringType.valueOf(scaleType);
						value = coloringType == null ? null : coloringType.getName(null);
					}
				}
				item.setParameter(parameter, value);
			}
		}
	}

	@NonNull
	private GpxDirItem readGpxDirItem(@NonNull SQLiteCursor query) {
		File file = readItemFile(query);
		GpxDirItem item = new GpxDirItem(app, file);
		processItemParameters(item, query, GpxParameter.getGpxDirParameters(), null);

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

	public String getColumnMaxValue(@NonNull GpxParameter parameter) {
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
	public List<GpxDataItem> getGpxDataItems() {
		Set<GpxDataItem> items = new HashSet<>();
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(GpxDbUtils.getSelectGpxQuery(), null);
				if (query != null) {
					try {
						if (query.moveToFirst()) {
							do {
								items.add(readGpxDataItem(query));
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

	@NonNull
	public List<GpxDirItem> getGpxDirItems() {
		Set<GpxDirItem> items = new HashSet<>();
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(GpxDbUtils.getSelectGpxDirQuery(), null);
				if (query != null) {
					try {
						if (query.moveToFirst()) {
							do {
								items.add(readGpxDirItem(query));
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
	public GpxDataItem getGpxDataItem(@NonNull File file) {
		if (GpxUiHelper.isGpxFile(file)) {
			return (GpxDataItem) getDataItem(file);
		}
		return null;
	}

	@Nullable
	public GpxDirItem getGpxDirItem(@NonNull File file) {
		if (file.isDirectory()) {
			return (GpxDirItem) getDataItem(file);
		}
		return null;
	}

	@Nullable
	private DataItem getDataItem(@NonNull File file) {
		DataItem result = null;
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				result = getDataItem(file, db);
			} finally {
				db.close();
			}
		}
		return result;
	}

	@Nullable
	public DataItem getDataItem(@NonNull File file, @NonNull SQLiteConnection db) {
		String name = file.getName();
		String dir = GpxDbUtils.getGpxFileDir(app, file);
		boolean gpxFile = GpxUiHelper.isGpxFile(file);

		String selectQuery = gpxFile ? GpxDbUtils.getSelectGpxQuery() : GpxDbUtils.getSelectGpxDirQuery();
		SQLiteCursor query = db.rawQuery(selectQuery + GPX_FIND_BY_NAME_AND_DIR, new String[] {name, dir});
		if (query != null) {
			try {
				if (query.moveToFirst()) {
					return gpxFile ? readGpxDataItem(query) : readGpxDirItem(query);
				}
			} finally {
				query.close();
			}
		}
		return null;
	}
}
