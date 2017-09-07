package net.osmand.plus.mapmarkers;

import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.helpers.SearchHistoryHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MapMarkersDbHelper {

	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "map_markers_db";

	private static final String MARKERS_TABLE_NAME = "map_markers";
	private static final String MARKERS_COL_ID = "marker_id";
	private static final String MARKERS_COL_LAT = "marker_latitude";
	private static final String MARKERS_COL_LON = "marker_longitude";
	private static final String MARKERS_COL_DESCRIPTION = "marker_description";
	private static final String MARKERS_COL_ACTIVE = "marker_active";
	private static final String MARKERS_COL_ADDED = "marker_added";
	private static final String MARKERS_COL_VISITED = "marker_visited";
	private static final String MARKERS_COL_GROUP_KEY = "group_key";
	private static final String MARKERS_COL_COLOR = "marker_color";
	private static final String MARKERS_COL_NEXT_KEY = "marker_next_key";

	private static final String MARKERS_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
			MARKERS_TABLE_NAME + " (" +
			MARKERS_COL_ID + " long PRIMARY KEY, " +
			MARKERS_COL_LAT + " double, " +
			MARKERS_COL_LON + " double, " +
			MARKERS_COL_DESCRIPTION + " TEXT, " +
			MARKERS_COL_ACTIVE + " int, " + // 1 = true, 0 = false
			MARKERS_COL_ADDED + " long, " +
			MARKERS_COL_VISITED + " long, " +
			MARKERS_COL_GROUP_KEY + " int, " +
			MARKERS_COL_COLOR + " int, " +
			MARKERS_COL_NEXT_KEY + " long);";

	private static final String MARKERS_TABLE_SELECT = "SELECT " +
			MARKERS_COL_ID + ", " +
			MARKERS_COL_LAT + ", " +
			MARKERS_COL_LON + ", " +
			MARKERS_COL_DESCRIPTION + ", " +
			MARKERS_COL_ACTIVE + ", " +
			MARKERS_COL_ADDED + ", " +
			MARKERS_COL_VISITED + ", " +
			MARKERS_COL_GROUP_KEY + ", " +
			MARKERS_COL_COLOR + ", " +
			MARKERS_COL_NEXT_KEY +
			" FROM " + MARKERS_TABLE_NAME;

	private static final String GROUPS_TABLE_NAME = "map_markers_groups";
	private static final String GROUPS_COL_NAME = "group_name";
	private static final String GROUPS_COL_TYPE = "group_type";
	private static final String GROUPS_COL_ADDED = "group_added";

	private static final String GROUPS_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
			GROUPS_TABLE_NAME + " (" +
			GROUPS_COL_NAME + " TEXT, " +
			GROUPS_COL_TYPE + " TEXT, " +
			GROUPS_COL_ADDED + " long);";

	private static final int TAIL_NEXT_VALUE = 0;
	private static final int HISTORY_NEXT_VALUE = -1;

	private final OsmandApplication context;

	public MapMarkersDbHelper(OsmandApplication context) {
		this.context = context;
	}

	private SQLiteConnection openConnection(boolean readonly) {
		SQLiteConnection conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
		int version = conn.getVersion();
		if (version == 0 || DB_VERSION != version) {
			if (readonly) {
				conn.close();
				conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, false);
			}
			version = conn.getVersion();
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
		saveExistingMarkersToDb();
	}

	private void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {

	}

	private void saveExistingMarkersToDb() {
		OsmandSettings settings = context.getSettings();

		List<LatLon> ips = settings.getMapMarkersPoints();
		List<String> desc = settings.getMapMarkersPointDescriptions(ips.size());
		List<Integer> colors = settings.getMapMarkersColors(ips.size());
		int colorIndex = 0;
		for (int i = 0; i < ips.size(); i++) {
			if (colors.size() > i) {
				colorIndex = colors.get(i);
			}
			MapMarker marker = new MapMarker(ips.get(i), PointDescription.deserializeFromString(desc.get(i), ips.get(i)),
					colorIndex, false, i);
			marker.history = false;
			addMarker(marker);
		}

		ips = settings.getMapMarkersHistoryPoints();
		desc = settings.getMapMarkersHistoryPointDescriptions(ips.size());
		colors = settings.getMapMarkersHistoryColors(ips.size());
		for (int i = 0; i < ips.size(); i++) {
			if (colors.size() > i) {
				colorIndex = colors.get(i);
			}
			MapMarker marker = new MapMarker(ips.get(i), PointDescription.deserializeFromString(desc.get(i), ips.get(i)),
					colorIndex, false, i);
			marker.history = true;
			addMarker(marker);
		}
	}

	public void reverseActiveMarkersOrder() {
		List<MapMarker> markers = getActiveMarkers();
		removeAllActiveMarkers();
		for (int i = markers.size() - 1; i >= 0; i--) {
			addMarker(markers.get(i));
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
		marker.id = Long.parseLong(String.valueOf(currentTime) + String.valueOf(new Random().nextInt(900) + 100));
		marker.creationDate = currentTime;
		double lat = marker.getLatitude();
		double lon = marker.getLongitude();
		String descr = PointDescription.serializeToString(marker.getOriginalPointDescription());
		int active = marker.history ? 0 : 1;
		long visited = 0;
		int groupKey = 0;
		int colorIndex = marker.colorIndex;

		PointDescription pointDescription = marker.getOriginalPointDescription();
		if (pointDescription != null && !pointDescription.isSearchingAddress(context)) {
			SearchHistoryHelper.getInstance(context)
					.addNewItemToHistory(marker.getLatitude(), marker.getLongitude(), pointDescription);
		}

		if (!marker.history) {
			db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " + MARKERS_COL_NEXT_KEY + " = ? " +
					"WHERE " + MARKERS_COL_NEXT_KEY + " = ?", new Object[]{marker.id, TAIL_NEXT_VALUE});
		}

		db.execSQL("INSERT INTO " + MARKERS_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[]{marker.id, lat, lon, descr, active, currentTime, visited, groupKey, colorIndex,
						marker.history ? HISTORY_NEXT_VALUE : TAIL_NEXT_VALUE});
	}

	public List<MapMarker> getActiveMarkers() {
		List<MapMarker> res = new LinkedList<>();
		LongSparseArray<MapMarker> markers = new LongSparseArray<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(MARKERS_TABLE_SELECT + " WHERE " + MARKERS_COL_ACTIVE + " = ?",
						new String[]{String.valueOf(1)});
				if (query.moveToFirst()) {
					do {
						MapMarker marker = readItem(query);
						markers.put(marker.nextKey, marker);
					} while (query.moveToNext());
				}
				query.close();
			} finally {
				db.close();
			}
			buildLinkedList(markers, res, markers.get(TAIL_NEXT_VALUE));
		}
		return res;
	}

	private MapMarker readItem(SQLiteCursor query) {
		long id = query.getLong(0);
		double lat = query.getDouble(1);
		double lon = query.getDouble(2);
		String desc = query.getString(3);
		boolean active = query.getInt(4) == 1;
		long added = query.getLong(5);
		long visited = query.getLong(6);
		int groupKey = query.getInt(7);
		int colorIndex = query.getInt(8);
		long nextKey = query.getLong(9);

		LatLon latLon = new LatLon(lat, lon);
		MapMarker marker = new MapMarker(latLon, PointDescription.deserializeFromString(desc, latLon),
				colorIndex, false, 0);
		marker.creationDate = added;
		marker.visitedDate = visited;
		marker.history = !active;
		marker.nextKey = nextKey;
		marker.id = id;

		return marker;
	}

	private void buildLinkedList(LongSparseArray<MapMarker> markers, List<MapMarker> res, MapMarker marker) {
		if (marker != null) {
			res.add(0, marker);
			MapMarker prev = markers.get(marker.id);
			if (prev != null) {
				buildLinkedList(markers, res, prev);
			}
		}
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
								MARKERS_COL_COLOR + " = ? " +
								"WHERE " + MARKERS_COL_ID + " = ?",
						new Object[]{marker.getLatitude(), marker.getLongitude(), descr, marker.colorIndex, marker.id});
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
						"WHERE " + MARKERS_COL_NEXT_KEY + " = ?", new Object[]{moved.nextKey, moved.id});

				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " + MARKERS_COL_NEXT_KEY + " = ? " +
						"WHERE " + MARKERS_COL_NEXT_KEY + " = ?", new Object[]{moved.id, next == null ? TAIL_NEXT_VALUE : next.id});

				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " + MARKERS_COL_NEXT_KEY + " = ? " +
						"WHERE " + MARKERS_COL_ID + " = ?", new Object[]{next == null ? TAIL_NEXT_VALUE : next.id, moved.id});
			} finally {
				db.close();
			}
		}
	}

	private void removeAllActiveMarkers() {
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + MARKERS_TABLE_NAME + " WHERE " + MARKERS_COL_ACTIVE + " = ?",
						new Object[]{1});
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

				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " + MARKERS_COL_NEXT_KEY + " = ? " +
						"WHERE " + MARKERS_COL_NEXT_KEY + " = ?", new Object[]{marker.nextKey, marker.id});

				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " +
						MARKERS_COL_ACTIVE + " = ?, " +
						MARKERS_COL_VISITED + " = ? " +
						"WHERE " + MARKERS_COL_ID + " = ?", new Object[]{0, marker.visitedDate, marker.id});
			} finally {
				db.close();
			}
		}
	}

	public void moveAllActiveMarkersToHistory() {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				long visitedDate = System.currentTimeMillis();
				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " +
						MARKERS_COL_ACTIVE + " = ?, " +
						MARKERS_COL_VISITED + " = ? " +
						"WHERE " + MARKERS_COL_ACTIVE + " = ?", new Object[]{0, visitedDate, 1});
			} finally {
				db.close();
			}
		}
	}

	public void restoreMapMarkerFromHistory(MapMarker marker) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				List<MapMarker> active = getActiveMarkers();
				db.execSQL("UPDATE " + MARKERS_TABLE_NAME + " SET " +
								MARKERS_COL_ACTIVE + " = ?, " +
								MARKERS_COL_NEXT_KEY + " = ? " +
								"WHERE " + MARKERS_COL_ID + " = ? " +
								"AND " + MARKERS_COL_ACTIVE + " = ?",
						new Object[]{1, active.size() > 0 ? active.get(0).id : TAIL_NEXT_VALUE, marker.id, 0});
			} finally {
				db.close();
			}
		}
	}

	public List<MapMarker> getMarkersHistory() {
		List<MapMarker> markers = new LinkedList<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(MARKERS_TABLE_SELECT + " WHERE " + MARKERS_COL_ACTIVE + " = ?",
						new String[]{String.valueOf(0)});
				if (query.moveToFirst()) {
					do {
						markers.add(readItem(query));
					} while (query.moveToNext());
				}
				query.close();
			} finally {
				db.close();
			}
		}
		return markers;
	}

	public void removeMarkerFromHistory(MapMarker marker) {
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + MARKERS_TABLE_NAME +
								" WHERE " + MARKERS_COL_ID + " = ?" +
								" AND " + MARKERS_COL_ACTIVE + " = ?",
						new Object[]{marker.id, 0});
			} finally {
				db.close();
			}
		}
	}

	public void clearAllMarkersHistory() {
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + MARKERS_TABLE_NAME + " WHERE " + MARKERS_COL_ACTIVE + " = ?",
						new Object[]{0});
			} finally {
				db.close();
			}
		}
	}
}
