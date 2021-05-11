package net.osmand.plus.mapmarkers;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static net.osmand.plus.mapmarkers.MapMarkersDbHelper.DB_NAME;
import static net.osmand.plus.mapmarkers.MapMarkersDbHelper.DB_VERSION;

public class MarkersDb39HelperLegacy {

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

	private final OsmandApplication app;
	private final MapMarkersDbHelper markersDbHelper;

	public MarkersDb39HelperLegacy(OsmandApplication app) {
		this.app = app;
		markersDbHelper = app.getMapMarkersHelper().getMarkersDbHelper();
	}

	public void migrateMarkersGroups() {
		List<MapMarkersGroup> markersGroups = loadGroupsLegacy();
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		markersHelper.getDataHelper().saveGroups(markersGroups);
	}

	public List<MapMarkersGroup> loadGroupsLegacy() {
		Map<String, MapMarkersGroup> groupsMap = getAllGroupsMap();
		Iterator<Entry<String, MapMarkersGroup>> iterator = groupsMap.entrySet().iterator();
		while (iterator.hasNext()) {
			MapMarkersGroup group = iterator.next().getValue();
			if (group.getType() == ItineraryType.TRACK && !new File(group.getId()).exists()) {
				iterator.remove();
			}
		}

		List<MapMarker> allMarkers = new ArrayList<>();
		allMarkers.addAll(markersDbHelper.getActiveMarkers(true));
		allMarkers.addAll(markersDbHelper.getMarkersHistory(true));

		MapMarkersGroup noGroup = null;
		for (MapMarker marker : allMarkers) {
			MapMarkersGroup group = groupsMap.get(marker.groupKey);
			if (group == null) {
				if (noGroup == null) {
					noGroup = new MapMarkersGroup();
					noGroup.setCreationDate(Long.MAX_VALUE);
					groupsMap.put(noGroup.getId(), noGroup);
				}
				noGroup.getMarkers().add(marker);
			} else {
				if (marker.creationDate < group.getCreationDate()) {
					group.setCreationDate(marker.creationDate);
				}
				group.getMarkers().add(marker);
			}
		}
		return new ArrayList<>(groupsMap.values());
	}

	public void onCreate(SQLiteConnection db) {
		markersDbHelper.onCreate(db);
		db.execSQL(GROUPS_TABLE_CREATE);
	}

	public void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
		markersDbHelper.onUpgrade(db, oldVersion, newVersion);
		if (oldVersion < 8) {
			db.execSQL("ALTER TABLE " + GROUPS_TABLE_NAME + " ADD " + GROUPS_COL_DISABLED + " int");
		}
		if (oldVersion < 9) {
			db.execSQL("UPDATE " + GROUPS_TABLE_NAME +
					" SET " + GROUPS_COL_DISABLED + " = ? " +
					"WHERE " + GROUPS_COL_DISABLED + " IS NULL", new Object[] {0});
		}
		if (oldVersion < 13) {
			db.execSQL("ALTER TABLE " + GROUPS_TABLE_NAME + " ADD " + GROUPS_COL_CATEGORIES + " TEXT");
		}
	}

	private SQLiteConnection openConnection(boolean readonly) {
		SQLiteConnection conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
		if (conn == null) {
			return null;
		}
		if (conn.getVersion() < DB_VERSION) {
			if (readonly) {
				conn.close();
				conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, false);
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

	public Map<String, MapMarkersGroup> getAllGroupsMap() {
		Map<String, MapMarkersGroup> res = new LinkedHashMap<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(GROUPS_TABLE_SELECT, null);
				if (query != null && query.moveToFirst()) {
					do {
						MapMarkersGroup group = readGroup(query);
						res.put(group.getId(), group);
					} while (query.moveToNext());
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

	private MapMarkersGroup readGroup(SQLiteCursor query) {
		String id = query.getString(0);
		String name = query.getString(1);
		int type = query.getInt(2);
		boolean disabled = query.getInt(3) == 1;
		String categories = query.getString(4);

		MapMarkersGroup res = new MapMarkersGroup(id, name, ItineraryType.findTypeForId(type));
		res.setDisabled(disabled);
		res.setWptCategories(categories == null ? null : Algorithms.decodeStringSet(categories));

		return res;
	}
}