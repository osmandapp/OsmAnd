package net.osmand.plus.mapmarkers;

import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.backup.BackupUtils;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.utils.AndroidDbUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapMarkersDbHelper {

	protected static final int DB_VERSION = 13;
	public static final String DB_NAME = "map_markers_db";

	private static final String MARKERS_TABLE_NAME = "map_markers";
	private static final String MARKERS_COL_ID = "marker_id";
	private static final String MARKERS_COL_LAT = "marker_lat";
	private static final String MARKERS_COL_LON = "marker_lon";
	private static final String MARKERS_COL_DESCRIPTION = "marker_description";
	private static final String MARKERS_COL_ACTIVE = "marker_active";
	private static final String MARKERS_COL_ADDED = "marker_added";
	private static final String MARKERS_COL_VISITED = "marker_visited";
	private static final String MARKERS_COL_GROUP_NAME = "group_name";
	private static final String MARKERS_COL_GROUP_KEY = "group_key";
	private static final String MARKERS_COL_COLOR = "marker_color";
	private static final String MARKERS_COL_NEXT_KEY = "marker_next_key";
	private static final String MARKERS_COL_DISABLED = "marker_disabled";
	private static final String MARKERS_COL_SELECTED = "marker_selected";
	private static final String MARKERS_COL_MAP_OBJECT_NAME = "marker_map_object_name";

	private static final String MARKERS_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
			MARKERS_TABLE_NAME + " (" +
			MARKERS_COL_ID + " TEXT PRIMARY KEY, " +
			MARKERS_COL_LAT + " double, " +
			MARKERS_COL_LON + " double, " +
			MARKERS_COL_DESCRIPTION + " TEXT, " +
			MARKERS_COL_ACTIVE + " int, " + // 1 = true, 0 = false
			MARKERS_COL_ADDED + " long, " +
			MARKERS_COL_VISITED + " long, " +
			MARKERS_COL_GROUP_NAME + " TEXT, " +
			MARKERS_COL_GROUP_KEY + " TEXT, " +
			MARKERS_COL_COLOR + " int, " +
			MARKERS_COL_NEXT_KEY + " TEXT, " +
			MARKERS_COL_DISABLED + " int, " + // 1 = true, 0 = false
			MARKERS_COL_SELECTED + " int, " + // 1 = true, 0 = false
			MARKERS_COL_MAP_OBJECT_NAME + " TEXT);";

	private static final String MARKERS_TABLE_SELECT = "SELECT " +
			MARKERS_COL_ID + ", " +
			MARKERS_COL_LAT + ", " +
			MARKERS_COL_LON + ", " +
			MARKERS_COL_DESCRIPTION + ", " +
			MARKERS_COL_ACTIVE + ", " +
			MARKERS_COL_ADDED + ", " +
			MARKERS_COL_VISITED + ", " +
			MARKERS_COL_GROUP_NAME + ", " +
			MARKERS_COL_GROUP_KEY + ", " +
			MARKERS_COL_COLOR + ", " +
			MARKERS_COL_NEXT_KEY + ", " +
			MARKERS_COL_DISABLED + ", " +
			MARKERS_COL_SELECTED + ", " +
			MARKERS_COL_MAP_OBJECT_NAME +
			" FROM " + MARKERS_TABLE_NAME;

	private static final String MARKERS_LAST_MODIFIED_NAME = "map_markers";
	private static final String MARKERS_HISTORY_LAST_MODIFIED_NAME = "map_markers_history";

	private final OsmandApplication context;

	public MapMarkersDbHelper(OsmandApplication context) {
		this.context = context;
	}

	public long getMarkersLastModifiedTime() {
		long lastModifiedTime = BackupUtils.getLastModifiedTime(context, MARKERS_LAST_MODIFIED_NAME);
		if (lastModifiedTime == 0) {
			lastModifiedTime = getDBLastModifiedTime();
			BackupUtils.setLastModifiedTime(context, MARKERS_LAST_MODIFIED_NAME, lastModifiedTime);
		}
		return lastModifiedTime;
	}

	public long getMarkersHistoryLastModifiedTime() {
		long lastModifiedTime = BackupUtils.getLastModifiedTime(context, MARKERS_HISTORY_LAST_MODIFIED_NAME);
		if (lastModifiedTime == 0) {
			lastModifiedTime = getDBLastModifiedTime();
			BackupUtils.setLastModifiedTime(context, MARKERS_HISTORY_LAST_MODIFIED_NAME, lastModifiedTime);
		}
		return lastModifiedTime;
	}

	public void setMarkersLastModifiedTime(long lastModifiedTime) {
		BackupUtils.setLastModifiedTime(context, MARKERS_LAST_MODIFIED_NAME, lastModifiedTime);
	}

	public void setMarkersHistoryLastModifiedTime(long lastModifiedTime) {
		BackupUtils.setLastModifiedTime(context, MARKERS_HISTORY_LAST_MODIFIED_NAME, lastModifiedTime);
	}

	private void updateMarkersLastModifiedTime() {
		BackupUtils.setLastModifiedTime(context, MARKERS_LAST_MODIFIED_NAME);
	}

	private void updateMarkersHistoryLastModifiedTime() {
		BackupUtils.setLastModifiedTime(context, MARKERS_HISTORY_LAST_MODIFIED_NAME);
	}

	private long getDBLastModifiedTime() {
		File dbFile = context.getDatabasePath(DB_NAME);
		return dbFile.exists() ? dbFile.lastModified() : 0;
	}

	private SQLiteConnection openConnection(boolean readonly) {
		SQLiteConnection conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
		if (conn == null) {
			return null;
		}
		if (conn.getVersion() < DB_VERSION) {
			if (readonly) {
				conn.close();
				conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, false);
			}
			if (conn != null) {
				int version = conn.getVersion();
				conn.setVersion(DB_VERSION);
				if (version == 0) {
					onCreate(conn);
				} else {
					onUpgrade(conn, version, DB_VERSION);
				}
			}
		}
		return conn;
	}

	protected void onCreate(SQLiteConnection db) {
		db.execSQL(MARKERS_TABLE_CREATE);
	}

	protected void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
		boolean upgraded = false;
		if (oldVersion < 8) {
			db.execSQL("ALTER TABLE " + MARKERS_TABLE_NAME + " ADD " + MARKERS_COL_DISABLED + " int");
			upgraded = true;
		}
		if (oldVersion < 9) {
			db.execSQL("UPDATE " + MARKERS_TABLE_NAME +
					" SET " + MARKERS_COL_DISABLED + " = ? " +
					"WHERE " + MARKERS_COL_DISABLED + " IS NULL", new Object[] {0});
			upgraded = true;
		}
		if (oldVersion < 10) {
			db.execSQL("ALTER TABLE " + MARKERS_TABLE_NAME + " ADD " + MARKERS_COL_SELECTED + " int");
			upgraded = true;
		}
		if (oldVersion < 11) {
			db.execSQL("UPDATE " + MARKERS_TABLE_NAME +
					" SET " + MARKERS_COL_SELECTED + " = ? " +
					"WHERE " + MARKERS_COL_SELECTED + " IS NULL", new Object[] {0});
			upgraded = true;
		}
		if (oldVersion < 12) {
			db.execSQL("ALTER TABLE " + MARKERS_TABLE_NAME + " ADD " + MARKERS_COL_MAP_OBJECT_NAME + " TEXT");
			upgraded = true;
		}
		if (upgraded) {
			updateMarkersLastModifiedTime();
			updateMarkersHistoryLastModifiedTime();
		}
	}

	public void removeActiveMarkersFromGroup(String groupId) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + MARKERS_TABLE_NAME +
								" WHERE " + MARKERS_COL_GROUP_KEY + " = ?" +
								" AND " + MARKERS_COL_ACTIVE + " = ?",
						new Object[] {groupId, 1});

				updateMarkersLastModifiedTime();
			} finally {
				db.close();
			}
		}
	}

	public void addMarkers(List<MapMarker> markers) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				for (MapMarker marker : markers) {
					insertLast(db, marker);
				}
			} finally {
				db.close();
			}
		}
	}

	public void addMarker(MapMarker marker) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				insertLast(db, marker);
			} finally {
				db.close();
			}
		}
	}

	private void insertLast(SQLiteConnection db, MapMarker marker) {
		if (isMarkerFromDefaultGroup(marker)) {
			return;
		}
		if (marker.id == null) {
			marker.id = ItineraryDataHelper.getMarkerId(context, marker, null);
		}
		marker.creationDate = System.currentTimeMillis();
		String descr = PointDescription.serializeToString(marker.getOriginalPointDescription());
		int active = marker.history ? 0 : 1;

		PointDescription pointDescription = marker.getOriginalPointDescription();
		if (pointDescription != null && !pointDescription.isSearchingAddress(context)) {
			SearchHistoryHelper.getInstance(context)
					.addNewItemToHistory(marker.getLatitude(), marker.getLongitude(), pointDescription, HistorySource.SEARCH);
		}

		Map<String, Object> rowsMap = new HashMap<>();
		rowsMap.put(MARKERS_COL_ID, marker.id);
		rowsMap.put(MARKERS_COL_LAT, marker.getLatitude());
		rowsMap.put(MARKERS_COL_LON, marker.getLongitude());
		rowsMap.put(MARKERS_COL_DESCRIPTION, descr);
		rowsMap.put(MARKERS_COL_ACTIVE, active);
		rowsMap.put(MARKERS_COL_ADDED, marker.creationDate);
		rowsMap.put(MARKERS_COL_VISITED, marker.visitedDate);
		rowsMap.put(MARKERS_COL_GROUP_NAME, marker.groupName);
		rowsMap.put(MARKERS_COL_GROUP_KEY, marker.groupKey);
		rowsMap.put(MARKERS_COL_COLOR, marker.colorIndex);
		rowsMap.put(MARKERS_COL_DISABLED, 0);
		rowsMap.put(MARKERS_COL_SELECTED, 0);
		rowsMap.put(MARKERS_COL_MAP_OBJECT_NAME, marker.mapObjectName);

		db.execSQL(AndroidDbUtils.createDbInsertQuery(MARKERS_TABLE_NAME, rowsMap.keySet()), rowsMap.values().toArray());

		if (marker.history) {
			updateMarkersHistoryLastModifiedTime();
		} else {
			updateMarkersLastModifiedTime();
		}
	}

	@Nullable
	public MapMarker getMarker(String id) {
		return getMarker(id, false);
	}

	@Nullable
	public MapMarker getMarker(String id, boolean legacy) {
		MapMarker res = null;
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(MARKERS_TABLE_SELECT + " WHERE " + MARKERS_COL_ID + " = ?", new String[] {id});
				if (query != null && query.moveToFirst()) {
					res = readItem(query, legacy);
				}
				if (query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return res;
	}

	public List<MapMarker> getActiveMarkers() {
		return getActiveMarkers(false);
	}

	public List<MapMarker> getActiveMarkers(boolean legacy) {
		List<MapMarker> markers = new ArrayList<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(MARKERS_TABLE_SELECT + " WHERE " + MARKERS_COL_ACTIVE + " = ?",
						new String[] {String.valueOf(1)});
				if (query != null && query.moveToFirst()) {
					do {
						MapMarker marker = readItem(query, legacy);
						if (marker != null) {
							markers.add(marker);
						}
					} while (query.moveToNext());
				}
				if (query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return markers;
	}

	@Nullable
	private MapMarker readItem(SQLiteCursor query, boolean legacy) {
		String groupKey = query.getString(8);
		if (groupKey != null && !legacy) {
			return null;
		}

		String id = query.getString(0);
		double lat = query.getDouble(1);
		double lon = query.getDouble(2);
		String desc = query.getString(3);
		boolean active = query.getInt(4) == 1;
		long added = query.getLong(5);
		long visited = query.getLong(6);
		String groupName = query.getString(7);
		int colorIndex = query.getInt(9);
		boolean selected = query.getInt(12) == 1;
		String mapObjectName = query.getString(13);

		LatLon latLon = new LatLon(lat, lon);
		PointDescription description = PointDescription.deserializeFromString(desc, latLon);
		MapMarker marker = new MapMarker(latLon, description, colorIndex);
		marker.id = id;
		marker.history = !active;
		marker.creationDate = added;
		marker.visitedDate = visited;
		marker.groupName = groupName;
		marker.groupKey = groupKey;
		marker.selected = selected;
		marker.mapObjectName = mapObjectName;

		return marker;
	}

	public void updateMarker(MapMarker marker) {
		if (isMarkerFromDefaultGroup(marker)) {
			return;
		}
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				String descr = PointDescription.serializeToString(marker.getOriginalPointDescription());
				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " +
								MARKERS_COL_LAT + " = ?, " +
								MARKERS_COL_LON + " = ?, " +
								MARKERS_COL_DESCRIPTION + " = ?, " +
								MARKERS_COL_COLOR + " = ?, " +
								MARKERS_COL_SELECTED + " = ? " +
								"WHERE " + MARKERS_COL_ID + " = ?",
						new Object[] {marker.getLatitude(), marker.getLongitude(), descr, marker.colorIndex, marker.selected, marker.id});

				updateMarkersLastModifiedTime();
			} finally {
				db.close();
			}
		}
	}

	public void moveMarkerToHistory(MapMarker marker) {
		if (isMarkerFromDefaultGroup(marker)) {
			return;
		}
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " +
						MARKERS_COL_ACTIVE + " = ?, " +
						MARKERS_COL_VISITED + " = ? " +
						"WHERE " + MARKERS_COL_ID + " = ?", new Object[] {0, marker.visitedDate, marker.id});
				updateMarkersLastModifiedTime();
				updateMarkersHistoryLastModifiedTime();
			} finally {
				db.close();
			}
		}
	}

	public void moveAllActiveMarkersToHistory(long timestamp) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " +
						MARKERS_COL_ACTIVE + " = ?, " +
						MARKERS_COL_VISITED + " = ? " +
						"WHERE " + MARKERS_COL_ACTIVE + " = ?", new Object[] {0, timestamp, 1});
				updateMarkersLastModifiedTime();
				updateMarkersHistoryLastModifiedTime();
			} finally {
				db.close();
			}
		}
	}

	public void restoreMapMarkerFromHistory(MapMarker marker) {
		if (isMarkerFromDefaultGroup(marker)) {
			return;
		}
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " +
								MARKERS_COL_ACTIVE + " = ? " +
								"WHERE " + MARKERS_COL_ID + " = ? " +
								"AND " + MARKERS_COL_ACTIVE + " = ?",
						new Object[] {1, marker.id, 0});
				updateMarkersLastModifiedTime();
				updateMarkersHistoryLastModifiedTime();
			} finally {
				db.close();
			}
		}
	}

	public List<MapMarker> getMarkersHistory() {
		return getMarkersHistory(false);
	}

	public List<MapMarker> getMarkersHistory(boolean legacy) {
		List<MapMarker> markers = new ArrayList<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(MARKERS_TABLE_SELECT + " WHERE " + MARKERS_COL_ACTIVE + " = ?",
						new String[] {String.valueOf(0)});
				if (query != null && query.moveToFirst()) {
					do {
						MapMarker marker = readItem(query, legacy);
						if (marker != null) {
							markers.add(marker);
						}
					} while (query.moveToNext());
				}
				if (query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return markers;
	}

	public void removeMarker(MapMarker marker) {
		if (isMarkerFromDefaultGroup(marker)) {
			return;
		}
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + MARKERS_TABLE_NAME +
								" WHERE " + MARKERS_COL_ID + " = ?" +
								" AND " + MARKERS_COL_ACTIVE + " = ?",
						new Object[] {marker.id, marker.history ? 0 : 1});
				if (marker.history) {
					updateMarkersHistoryLastModifiedTime();
				} else {
					updateMarkersLastModifiedTime();
				}
			} finally {
				db.close();
			}
		}
	}

	private boolean isMarkerFromDefaultGroup(MapMarker marker) {
		return marker.groupKey != null;
	}
}
