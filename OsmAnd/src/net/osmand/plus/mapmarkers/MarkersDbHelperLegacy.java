package net.osmand.plus.mapmarkers;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.osmand.plus.mapmarkers.MapMarkersDbHelper.MARKERS_COL_ACTIVE;
import static net.osmand.plus.mapmarkers.MapMarkersDbHelper.MARKERS_COL_DISABLED;
import static net.osmand.plus.mapmarkers.MapMarkersDbHelper.MARKERS_COL_GROUP_KEY;
import static net.osmand.plus.mapmarkers.MapMarkersDbHelper.MARKERS_TABLE_NAME;

public class MarkersDbHelperLegacy {

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

	public MarkersDbHelperLegacy(OsmandApplication app, MapMarkersDbHelper markersDbHelper) {
		this.app = app;
		this.markersDbHelper = markersDbHelper;
	}

	public void onCreate(SQLiteConnection db) {
		db.execSQL(GROUPS_TABLE_CREATE);
	}

	public void onUpgrade(SQLiteConnection db, int oldVersion, int newVersion) {
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
		return markersDbHelper.openConnection(readonly);
	}

	public void updateGroupDisabled(String id, boolean disabled) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("UPDATE " + GROUPS_TABLE_NAME +
						" SET " + GROUPS_COL_DISABLED + " = ? " +
						"WHERE " + GROUPS_COL_ID + " = ?", new Object[] {disabled ? 1 : 0, id});
				db.execSQL("UPDATE " + MARKERS_TABLE_NAME +
						" SET " + MARKERS_COL_DISABLED + " = ? " +
						"WHERE " + MARKERS_COL_GROUP_KEY + " = ?", new Object[] {disabled ? 1 : 0, id});
			} finally {
				db.close();
			}
		}
	}

	public void removeDisabledGroups() {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + GROUPS_TABLE_NAME + " WHERE " + GROUPS_COL_DISABLED + " = ? ", new Object[] {1});
				db.execSQL("DELETE FROM " + MARKERS_TABLE_NAME
						+ " WHERE " + MARKERS_COL_DISABLED + " = ? AND " + MARKERS_COL_ACTIVE + " = ?", new Object[] {1, 1});
			} finally {
				db.close();
			}
		}
	}

	public void addGroup(MapMarkersGroup group) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("INSERT INTO " + GROUPS_TABLE_NAME + " VALUES (?, ?, ?, ?, ?)",
						new Object[] {group.getId(), group.getName(), group.getType().getTypeId(), group.isDisabled(), group.getWptCategoriesString()});
			} finally {
				db.close();
			}
		}
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

	public void removeMarkersGroup(String id) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + GROUPS_TABLE_NAME + " WHERE " + GROUPS_COL_ID + " = ?", new Object[] {id});
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
						"WHERE " + GROUPS_COL_ID + " = ?", new Object[] {categories, id});
			} finally {
				db.close();
			}
		}
	}
}
