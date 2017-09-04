package net.osmand.plus.mapmarkers;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.MapMarkersHelper.MapMarker.DisplayPlace.TOPBAR;
import static net.osmand.plus.MapMarkersHelper.MapMarker.DisplayPlace.WIDGET;

public class MapMarkersDbHelper {

	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "map_markers_db";

	private static final String MARKERS_TABLE_NAME = "map_markers";
	private static final String MARKERS_COL_LAT = "marker_latitude";
	private static final String MARKERS_COL_LON = "marker_longitude";
	private static final String MARKERS_COL_DESCRIPTION = "marker_description";
	private static final String MARKERS_COL_ACTIVE = "marker_active";
	private static final String MARKERS_COL_ADDED = "marker_added";
	private static final String MARKERS_COL_VISITED = "marker_visited";
	private static final String MARKERS_COL_GROUP_KEY = "group_key";
	private static final String MARKERS_COL_COLOR = "marker_color";
	private static final String MARKERS_COL_DISPLAY_PLACE = "marker_display_place";
	private static final String MARKERS_COL_NEXT_KEY = "marker_next_key";

	private static final String MARKERS_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
			MARKERS_TABLE_NAME + " (" +
			MARKERS_COL_LAT + " double, " +
			MARKERS_COL_LON + " double, " +
			MARKERS_COL_DESCRIPTION + " TEXT, " +
			MARKERS_COL_ACTIVE + " int default 1, " + // 1 = true, 0 = false
			MARKERS_COL_ADDED + " long, " +
			MARKERS_COL_VISITED + " long, " +
			MARKERS_COL_GROUP_KEY + " int, " +
			MARKERS_COL_COLOR + " int, " +
			MARKERS_COL_DISPLAY_PLACE + " int, " +
			MARKERS_COL_NEXT_KEY + " int);";

	private static final String MARKERS_TABLE_SELECT = "SELECT " +
			MARKERS_COL_LAT + ", " +
			MARKERS_COL_LON + ", " +
			MARKERS_COL_DESCRIPTION + ", " +
			MARKERS_COL_ACTIVE + ", " +
			MARKERS_COL_ADDED + ", " +
			MARKERS_COL_VISITED + ", " +
			MARKERS_COL_GROUP_KEY + ", " +
			MARKERS_COL_COLOR + ", " +
			MARKERS_COL_DISPLAY_PLACE + ", " +
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

	private OsmandApplication context;

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
		//todo: load all existing markers
	}

	private void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {

	}

	public boolean add(MapMarker marker) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				insert(marker, db);
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	private void insert(MapMarker marker, SQLiteConnection db) {

	}

	public List<MapMarker> getMapMarkers() {
		return getItems(true);
	}

	public List<MapMarker> getMapMarkersHistory() {
		return getItems(false);
	}

	private List<MapMarker> getItems(boolean active) {
		List<MapMarker> result = new ArrayList<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(MARKERS_TABLE_SELECT + " WHERE " + MARKERS_COL_ACTIVE + " = ?",
						new String[]{String.valueOf(active ? 1 : 0)});
				if (query.moveToFirst()) {
					do {
						result.add(readItem(query));
					} while (query.moveToNext());
				}
				query.close();
			} finally {
				db.close();
			}
		}
		return result;
	}

	private MapMarker readItem(SQLiteCursor query) {
		double lat = query.getDouble(0);
		double lon = query.getDouble(1);
		String desc = query.getString(2);
		boolean active = query.getInt(3) == 1;
		long added = query.getLong(4);
		long visited = query.getLong(5);
		int groupKey = query.getInt(6);
		int colorIndex = query.getInt(7);
		int displayPlace = query.getInt(8);
		int nextKey = query.getInt(9);

		LatLon latLon = new LatLon(lat, lon);
		MapMarker marker = new MapMarker(latLon, PointDescription.deserializeFromString(desc, latLon),
				colorIndex, false, added, visited, displayPlace == 0 ? WIDGET : TOPBAR, 0);
		marker.history = !active;

		return marker;
	}
}
