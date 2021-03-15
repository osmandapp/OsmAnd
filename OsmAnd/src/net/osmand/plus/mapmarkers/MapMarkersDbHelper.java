package net.osmand.plus.mapmarkers;

import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.itinerary.ItineraryGroup;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MapMarkersDbHelper {

	private static final int DB_VERSION = 13;
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

	private static final String GROUPS_TABLE_NAME = "map_markers_groups";
	private static final String GROUPS_COL_ID = "group_id";
	private static final String GROUPS_COL_NAME = "group_name";
	private static final String GROUPS_COL_TYPE = "group_type";
	private static final String GROUPS_COL_DISABLED = "group_disabled";
	private static final String GROUPS_COL_CATEGORIES = "group_categories";

	private static final String GROUPS_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
			GROUPS_TABLE_NAME + " (" +
			GROUPS_COL_ID + " TEXT PRIMARY KEY, " +
			GROUPS_COL_NAME + " TEXT, " +
			GROUPS_COL_TYPE + " int, " +
			GROUPS_COL_DISABLED + " int, " + // 1 = true, 0 = false
			GROUPS_COL_CATEGORIES + " TEXT);";

	private static final String GROUPS_TABLE_SELECT = "SELECT " +
			GROUPS_COL_ID + ", " +
			GROUPS_COL_NAME + ", " +
			GROUPS_COL_TYPE + ", " +
			GROUPS_COL_DISABLED + ", " +
			GROUPS_COL_CATEGORIES +
			" FROM " + GROUPS_TABLE_NAME;

	public static final String TAIL_NEXT_VALUE = "tail_next";
	public static final String HISTORY_NEXT_VALUE = "history_next";

	private final OsmandApplication context;

	public MapMarkersDbHelper(OsmandApplication context) {
		this.context = context;
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
		db.execSQL(MARKERS_TABLE_CREATE);
		db.execSQL(GROUPS_TABLE_CREATE);
	}

	private void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
		if (oldVersion < 8) {
			db.execSQL("ALTER TABLE " + MARKERS_TABLE_NAME + " ADD " + MARKERS_COL_DISABLED + " int");
			db.execSQL("ALTER TABLE " + GROUPS_TABLE_NAME + " ADD " + GROUPS_COL_DISABLED + " int");
		}
		if (oldVersion < 9) {
			db.execSQL("UPDATE " + GROUPS_TABLE_NAME +
					" SET " + GROUPS_COL_DISABLED + " = ? " +
					"WHERE " + GROUPS_COL_DISABLED + " IS NULL", new Object[]{0});
			db.execSQL("UPDATE " + MARKERS_TABLE_NAME +
					" SET " + MARKERS_COL_DISABLED + " = ? " +
					"WHERE " + MARKERS_COL_DISABLED + " IS NULL", new Object[]{0});
		}
		if (oldVersion < 10) {
			db.execSQL("ALTER TABLE " + MARKERS_TABLE_NAME + " ADD " + MARKERS_COL_SELECTED + " int");
		}
		if (oldVersion < 11) {
			db.execSQL("UPDATE " + MARKERS_TABLE_NAME +
					" SET " + MARKERS_COL_SELECTED + " = ? " +
					"WHERE " + MARKERS_COL_SELECTED + " IS NULL", new Object[]{0});
		}
		if (oldVersion < 12) {
			db.execSQL("ALTER TABLE " + MARKERS_TABLE_NAME + " ADD " + MARKERS_COL_MAP_OBJECT_NAME + " TEXT");
		}
		if (oldVersion < 13) {
			db.execSQL("ALTER TABLE " + GROUPS_TABLE_NAME + " ADD " + GROUPS_COL_CATEGORIES + " TEXT");
		}
	}

	public void addGroup(ItineraryGroup group) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("INSERT INTO " + GROUPS_TABLE_NAME + " VALUES (?, ?, ?, ?, ?)",
						new Object[]{group.getId(), group.getName(), group.getType(), group.isDisabled(), group.getWptCategoriesString()});
			} finally {
				db.close();
			}
		}
	}

	public Map<String, ItineraryGroup> getAllGroupsMap() {
		Map<String, ItineraryGroup> res = new LinkedHashMap<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(GROUPS_TABLE_SELECT, null);
				if (query != null && query.moveToFirst()) {
					do {
						ItineraryGroup group = readGroup(query);
						res.put(group.getId(), group);
					} while (query.moveToNext());
				}
				if(query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return res;
	}

	private ItineraryGroup readGroup(SQLiteCursor query) {
		String id = query.getString(0);
		String name = query.getString(1);
		int type = query.getInt(2);
		boolean disabled = query.getInt(3) == 1;
		String categories = query.getString(4);

		ItineraryGroup res = new ItineraryGroup(id, name, type);
		res.setDisabled(disabled);
		res.setWptCategories(categories == null ? null : Algorithms.decodeStringSet(categories));

		return res;
	}

	public void removeMarkersGroup(String id) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + GROUPS_TABLE_NAME + " WHERE " + GROUPS_COL_ID + " = ?", new Object[]{id});
			} finally {
				db.close();
			}
		}
	}

	public void removeActiveMarkersFromGroup(String groupId) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + MARKERS_TABLE_NAME +
								" WHERE " + MARKERS_COL_GROUP_KEY + " = ?" +
								" AND " + MARKERS_COL_ACTIVE + " = ?",
						new Object[]{groupId, 1});
			} finally {
				db.close();
			}
		}
	}

	public void updateGroupDisabled(String id, boolean disabled) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("UPDATE " + GROUPS_TABLE_NAME +
						" SET " + GROUPS_COL_DISABLED + " = ? " +
						"WHERE " + GROUPS_COL_ID + " = ?", new Object[]{disabled ? 1 : 0, id});
				db.execSQL("UPDATE " + MARKERS_TABLE_NAME +
						" SET " + MARKERS_COL_DISABLED + " = ? " +
						"WHERE " + MARKERS_COL_GROUP_KEY + " = ?", new Object[]{disabled ? 1 : 0, id});
			} finally {
				db.close();
			}
		}
	}

	public void updateGroupCategories(String id, String categories) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("UPDATE " + GROUPS_TABLE_NAME +
						" SET " + GROUPS_COL_CATEGORIES + " = ? " +
						"WHERE " + GROUPS_COL_ID + " = ?", new Object[]{categories, id});
			} finally {
				db.close();
			}
		}
	}

	public void removeDisabledGroups() {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + GROUPS_TABLE_NAME + " WHERE " + GROUPS_COL_DISABLED + " = ? ", new Object[]{1});
				db.execSQL("DELETE FROM " + MARKERS_TABLE_NAME
						+ " WHERE " + MARKERS_COL_DISABLED + " = ? AND " + MARKERS_COL_ACTIVE + " = ?", new Object[]{1, 1});
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
		long currentTime = System.currentTimeMillis();
		if (marker.id == null) {
			marker.id = String.valueOf(currentTime) + String.valueOf(new Random().nextInt(900) + 100);
		}
		marker.creationDate = currentTime;
		String descr = PointDescription.serializeToString(marker.getOriginalPointDescription());
		int active = marker.history ? 0 : 1;

		PointDescription pointDescription = marker.getOriginalPointDescription();
		if (pointDescription != null && !pointDescription.isSearchingAddress(context)) {
			SearchHistoryHelper.getInstance(context)
					.addNewItemToHistory(marker.getLatitude(), marker.getLongitude(), pointDescription);
		}

		if (!marker.history) {
			db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " + MARKERS_COL_NEXT_KEY + " = ? " +
					"WHERE " + MARKERS_COL_NEXT_KEY + " = ?", new Object[]{marker.id, TAIL_NEXT_VALUE});
		}

		db.execSQL("INSERT INTO " + MARKERS_TABLE_NAME + " (" +
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
						MARKERS_COL_MAP_OBJECT_NAME + ") " +
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[]{marker.id, marker.getLatitude(), marker.getLongitude(), descr, active,
						currentTime, marker.visitedDate, marker.groupName, marker.groupKey, marker.colorIndex,
						marker.history ? HISTORY_NEXT_VALUE : TAIL_NEXT_VALUE, 0, 0, marker.mapObjectName});
	}

	@Nullable
	public MapMarker getMarker(String id) {
		MapMarker res = null;
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(MARKERS_TABLE_SELECT + " WHERE " + MARKERS_COL_ID + " = ?", new String[]{id});
				if (query != null && query.moveToFirst()) {
					res = readItem(query);
				}
				if(query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return res;
	}

	public List<MapMarker> getActiveMarkers() {
		Map<String, MapMarker> markers = new LinkedHashMap<>();
		Set<String> nextKeys = new HashSet<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(MARKERS_TABLE_SELECT + " WHERE " + MARKERS_COL_ACTIVE + " = ?",
						new String[]{String.valueOf(1)});
				if (query != null && query.moveToFirst()) {
					do {
						MapMarker marker = readItem(query);
						markers.put(marker.id, marker);
						nextKeys.add(marker.nextKey);
					} while (query.moveToNext());
				}
				if(query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return buildLinkedList(markers, nextKeys);
	}

	private MapMarker readItem(SQLiteCursor query) {
		String id = query.getString(0);
		double lat = query.getDouble(1);
		double lon = query.getDouble(2);
		String desc = query.getString(3);
		boolean active = query.getInt(4) == 1;
		long added = query.getLong(5);
		long visited = query.getLong(6);
		String groupName = query.getString(7);
		String groupKey = query.getString(8);
		int colorIndex = query.getInt(9);
		String nextKey = query.getString(10);
		boolean selected = query.getInt(12) == 1;
		String mapObjectName = query.getString(13);

		LatLon latLon = new LatLon(lat, lon);
		MapMarker marker = new MapMarker(latLon, PointDescription.deserializeFromString(desc, latLon),
				colorIndex, false, 0);
		marker.id = id;
		marker.history = !active;
		marker.creationDate = added;
		marker.visitedDate = visited;
		marker.groupName = groupName;
		marker.groupKey = groupKey;
		marker.nextKey = nextKey;
		marker.selected = selected;
		marker.mapObjectName = mapObjectName;

		return marker;
	}

	private List<MapMarker> buildLinkedList(Map<String, MapMarker> markers, Set<String> nextKeys) {
		List<MapMarker> res = new ArrayList<>(markers.size());

		while (!markers.isEmpty()) {
			MapMarker head = null;

			Iterator<MapMarker> iterator = markers.values().iterator();
			while (iterator.hasNext()) {
				MapMarker marker = iterator.next();
				if (!nextKeys.contains(marker.id) || !iterator.hasNext()) {
					head = marker;
					break;
				}
			}

			if (head == null) {
				break;
			}

			do {
				res.add(head);
				markers.remove(head.id);
			} while ((head = markers.get(head.nextKey)) != null);
		}

		return res;
	}

	public void updateMarker(MapMarker marker) {
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
						new Object[]{marker.getLatitude(), marker.getLongitude(), descr, marker.colorIndex, marker.selected, marker.id});
			} finally {
				db.close();
			}
		}
	}

	public void changeActiveMarkerPosition(MapMarker moved, @Nullable MapMarker next) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " + MARKERS_COL_NEXT_KEY + " = ? " +
						"WHERE " + MARKERS_COL_ID + " = ?", new Object[]{next == null ? TAIL_NEXT_VALUE : next.id, moved.id});
			} finally {
				db.close();
			}
		}
	}

	public void moveMarkerToHistory(MapMarker marker) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				marker.visitedDate = System.currentTimeMillis();

				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " +
						MARKERS_COL_ACTIVE + " = ?, " +
						MARKERS_COL_VISITED + " = ?, " +
						MARKERS_COL_NEXT_KEY + " = ? " +
						"WHERE " + MARKERS_COL_ID + " = ?", new Object[]{0, marker.visitedDate, HISTORY_NEXT_VALUE, marker.id});
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
						MARKERS_COL_VISITED + " = ?, " +
						MARKERS_COL_NEXT_KEY + " = ? " +
						"WHERE " + MARKERS_COL_ACTIVE + " = ?", new Object[]{0, timestamp, HISTORY_NEXT_VALUE, 1});
			} finally {
				db.close();
			}
		}
	}

	public void restoreMapMarkerFromHistory(MapMarker marker) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " +
								MARKERS_COL_ACTIVE + " = ? " +
								"WHERE " + MARKERS_COL_ID + " = ? " +
								"AND " + MARKERS_COL_ACTIVE + " = ?",
						new Object[]{1, marker.id, 0});
			} finally {
				db.close();
			}
		}
	}

	public List<MapMarker> getMarkersHistory() {
		List<MapMarker> markers = new ArrayList<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(MARKERS_TABLE_SELECT + " WHERE " + MARKERS_COL_ACTIVE + " = ?",
						new String[]{String.valueOf(0)});
				if (query != null && query.moveToFirst()) {
					do {
						markers.add(readItem(query));
					} while (query.moveToNext());
				}
				if(query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return markers;
	}

	public void removeMarker(MapMarker marker) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + MARKERS_TABLE_NAME +
								" WHERE " + MARKERS_COL_ID + " = ?" +
								" AND " + MARKERS_COL_ACTIVE + " = ?",
						new Object[]{marker.id, marker.history ? 0 : 1});
			} finally {
				db.close();
			}
		}
	}
}
