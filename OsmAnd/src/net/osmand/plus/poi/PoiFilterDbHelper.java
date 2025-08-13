package net.osmand.plus.poi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.PlatformUtil;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.api.SQLiteAPI.SQLiteStatement;
import net.osmand.plus.backup.BackupUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class PoiFilterDbHelper {

	private static final Log LOG = PlatformUtil.getLog(PoiFilterDbHelper.class);

	private static final String UDF_CAR_AID = "car_aid";
	private static final String UDF_FOR_TOURISTS = "for_tourists";
	private static final String UDF_FOOD_SHOP = "food_shop";
	private static final String UDF_FUEL = "fuel";
	private static final String UDF_SIGHTSEEING = "sightseeing";
	private static final String UDF_EMERGENCY = "emergency";
	private static final String UDF_PUBLIC_TRANSPORT = "public_transport";
	private static final String UDF_ACCOMMODATION = "accommodation";
	private static final String UDF_RESTAURANTS = "restaurants";
	private static final String UDF_PARKING = "parking";

	private static final String[] OLD_FILTERS_TO_DELETE = {
			UDF_CAR_AID, UDF_FOR_TOURISTS, UDF_FOOD_SHOP, UDF_FUEL, UDF_SIGHTSEEING, UDF_EMERGENCY,
			UDF_PUBLIC_TRANSPORT, UDF_ACCOMMODATION, UDF_RESTAURANTS, UDF_PARKING
	};

	private static final int TRUE_INT = 1;
	private static final int FALSE_INT = 0;

	public static final String DATABASE_NAME = "poi_filters";
	private static final int DATABASE_VERSION = 7;

	private static final String FILTER_NAME = "poi_filters";
	private static final String FILTER_COL_NAME = "name";
	private static final String FILTER_COL_ID = "id";
	private static final String FILTER_COL_FILTERBYNAME = "filterbyname";
	private static final String FILTER_COL_HISTORY = "history";
	private static final String FILTER_COL_DELETED = "deleted";

	private static final String FILTER_TABLE_CREATE = "CREATE TABLE " +
			FILTER_NAME + " (" +
			FILTER_COL_NAME + ", " +
			FILTER_COL_ID + ", " +
			FILTER_COL_FILTERBYNAME + ", " +
			FILTER_COL_HISTORY + ", " +
			FILTER_COL_DELETED + ");";

	private static final String CATEGORIES_NAME = "categories";
	private static final String CATEGORIES_FILTER_ID = "filter_id";
	private static final String CATEGORIES_COL_CATEGORY = "category";
	private static final String CATEGORIES_COL_SUBCATEGORY = "subcategory";

	private static final String CATEGORIES_TABLE_CREATE = "CREATE TABLE " +
			CATEGORIES_NAME + " (" +
			CATEGORIES_FILTER_ID + ", " +
			CATEGORIES_COL_CATEGORY + ", " +
			CATEGORIES_COL_SUBCATEGORY + ");";

	private static final String POI_TYPES_CACHE_NAME = "poi_types_cache";
	private static final String MAP_FILE_NAME = "map_name";
	private static final String MAP_FILE_DATE = "map_date";
	private static final String CACHED_POI_CATEGORIES = "cached_categories";

	private static final String POI_CACHE_TABLE_CREATE = "CREATE TABLE " +
			POI_TYPES_CACHE_NAME + " (" +
			MAP_FILE_NAME + ", " +
			MAP_FILE_DATE + ", " +
			CACHED_POI_CATEGORIES + ");";

	private static final String CATEGORY_KEY = "category";
	private static final String SUB_CATEGORIES_KEY = "sub_categories";

	private static final String FILTERS_LAST_MODIFIED_NAME = "poi_filters";

	private final OsmandApplication app;
	private final MapPoiTypes poiTypes;

	private SQLiteConnection conn;

	PoiFilterDbHelper(@NonNull OsmandApplication app, @Nullable MapPoiTypes poiTypes) {
		this.app = app;
		this.poiTypes = poiTypes;
	}

	public SQLiteConnection getWritableDatabase() {
		return openConnection(false);
	}

	public void close() {
		if (conn != null) {
			conn.close();
			conn = null;
		}
	}

	public SQLiteConnection getReadableDatabase() {
		return openConnection(true);
	}

	private SQLiteConnection openConnection(boolean readonly) {
		conn = app.getSQLiteAPI().getOrCreateDatabase(DATABASE_NAME, readonly);
		if (conn != null && conn.getVersion() < DATABASE_VERSION) {
			if (readonly) {
				conn.close();
				conn = app.getSQLiteAPI().getOrCreateDatabase(DATABASE_NAME, false);
			}
			if (conn != null) {
				int version = conn.getVersion();
				conn.setVersion(DATABASE_VERSION);
				if (version == 0) {
					onCreate(conn);
				} else {
					onUpgrade(conn, version, DATABASE_VERSION);
				}
			}
		}
		return conn;
	}

	public void onCreate(SQLiteConnection conn) {
		conn.execSQL(FILTER_TABLE_CREATE);
		conn.execSQL(CATEGORIES_TABLE_CREATE);
		conn.execSQL(POI_CACHE_TABLE_CREATE);
	}


	public void onUpgrade(SQLiteConnection conn, int oldVersion, int newVersion) {
		boolean upgraded = false;
		if (newVersion <= 5) {
			deleteOldFilters(conn);
			upgraded = true;
		}
		if (oldVersion < 6) {
			conn.execSQL("ALTER TABLE " + FILTER_NAME + " ADD " + FILTER_COL_HISTORY + " int DEFAULT " + FALSE_INT);
			conn.execSQL("ALTER TABLE " + FILTER_NAME + " ADD " + FILTER_COL_DELETED + " int DEFAULT " + FALSE_INT);
			upgraded = true;
		}
		if (oldVersion < 7) {
			conn.execSQL(POI_CACHE_TABLE_CREATE);
			upgraded = true;
		}
		if (upgraded) {
			updateLastModifiedTime();
		}
	}

	public long getLastModifiedTime() {
		long lastModifiedTime = BackupUtils.getLastModifiedTime(app, FILTERS_LAST_MODIFIED_NAME);
		if (lastModifiedTime == 0) {
			File dbFile = app.getDatabasePath(DATABASE_NAME);
			lastModifiedTime = dbFile.exists() ? dbFile.lastModified() : 0;
			BackupUtils.setLastModifiedTime(app, FILTERS_LAST_MODIFIED_NAME, lastModifiedTime);
		}
		return lastModifiedTime;
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		BackupUtils.setLastModifiedTime(app, FILTERS_LAST_MODIFIED_NAME, lastModifiedTime);
	}

	private void updateLastModifiedTime() {
		BackupUtils.setLastModifiedTime(app, FILTERS_LAST_MODIFIED_NAME);
	}

	private void deleteOldFilters(SQLiteConnection conn) {
		if (conn != null) {
			for (String toDel : OLD_FILTERS_TO_DELETE) {
				deleteFilter(conn, "user_" + toDel);
			}
		}
	}

	void doDeletion() {
		SQLiteConnection conn = getWritableDatabase();
		if (conn != null) {
			String query = "SELECT " + FILTER_COL_ID + ", " + FILTER_COL_HISTORY + ", " + FILTER_COL_DELETED + " FROM " + FILTER_NAME;
			SQLiteCursor cursor = conn.rawQuery(query, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						if (cursor.getInt(1) == FALSE_INT && cursor.getInt(2) == TRUE_INT) {
							deleteFilter(conn, cursor.getString(0));
						}
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
		}
	}

	void markHistory(String filterId, boolean history) {
		SQLiteConnection conn = getWritableDatabase();
		if (conn != null) {
			conn.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_HISTORY + " = ? WHERE " + FILTER_COL_ID + " = ?",
					new Object[] {history ? TRUE_INT : FALSE_INT, filterId});
			updateLastModifiedTime();
		}
	}

	void clearHistory() {
		SQLiteConnection conn = getWritableDatabase();
		if (conn != null) {
			conn.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_HISTORY + " = ?", new Object[] {FALSE_INT});
			updateLastModifiedTime();
		}
	}

	protected boolean addFilter(PoiUIFilter p, SQLiteConnection db, boolean addOnlyCategories,
			boolean forHistory) {
		if (db != null) {
			if (!addOnlyCategories) {
				p.setDeleted(forHistory);
				int value = forHistory ? TRUE_INT : FALSE_INT;
				db.execSQL("INSERT INTO " + FILTER_NAME + " VALUES (?, ?, ?, ?, ?)",
						new Object[] {p.getName(), p.getFilterId(), p.getFilterByName(), value, value});
			}
			Map<PoiCategory, LinkedHashSet<String>> types = p.getAcceptedTypes();
			SQLiteStatement insertCategories = db.compileStatement("INSERT INTO " + CATEGORIES_NAME + " VALUES (?, ?, ?)");
			for (Map.Entry<PoiCategory, LinkedHashSet<String>> entry : types.entrySet()) {
				PoiCategory a = entry.getKey();
				if (entry.getValue() == null) {
					insertCategories.bindString(1, p.getFilterId());
					insertCategories.bindString(2, a.getKeyName());
					insertCategories.bindNull(3);
					insertCategories.execute();
				} else {
					for (String s : entry.getValue()) {
						insertCategories.bindString(1, p.getFilterId());
						insertCategories.bindString(2, a.getKeyName());
						insertCategories.bindString(3, s);
						insertCategories.execute();
					}
				}
			}
			insertCategories.close();
			updateLastModifiedTime();
			return true;
		}
		return false;
	}

	protected List<PoiUIFilter> getFilters(SQLiteConnection conn, boolean includeDeleted) {
		ArrayList<PoiUIFilter> list = new ArrayList<>();
		if (conn != null) {
			SQLiteCursor query = conn.rawQuery("SELECT " + CATEGORIES_FILTER_ID + ", " + CATEGORIES_COL_CATEGORY + "," + CATEGORIES_COL_SUBCATEGORY + " FROM " +
					CATEGORIES_NAME, null);
			Map<String, Map<PoiCategory, LinkedHashSet<String>>> map = new LinkedHashMap<>();
			if (query != null && query.moveToFirst()) {
				do {
					String filterId = query.getString(0);
					if (!map.containsKey(filterId)) {
						map.put(filterId, new LinkedHashMap<>());
					}
					Map<PoiCategory, LinkedHashSet<String>> m = map.get(filterId);
					PoiCategory a = poiTypes.getPoiCategoryByName(query.getString(1).toLowerCase(), false);
					String subCategory = query.getString(2);
					if (subCategory == null) {
						m.put(a, null);
					} else if (!poiTypes.isTypeForbidden(subCategory)) {
						if (m.get(a) == null) {
							m.put(a, new LinkedHashSet<String>());
						}
						m.get(a).add(subCategory);
					}
				} while (query.moveToNext());
			}
			if (query != null) {
				query.close();
			}

			query = conn.rawQuery("SELECT " +
					FILTER_COL_ID + ", " +
					FILTER_COL_NAME + ", " +
					FILTER_COL_FILTERBYNAME + ", " +
					FILTER_COL_DELETED +
					" FROM " + FILTER_NAME, null);
			if (query != null && query.moveToFirst()) {
				do {
					String filterId = query.getString(0);
					boolean deleted = query.getInt(3) == TRUE_INT;
					if (map.containsKey(filterId) && (includeDeleted || !deleted)) {
						String filterName = query.getString(1);
						String translation = app.getPoiTypes().getPoiTranslation(filterName);
						if (translation != null) {
							filterName = translation;
						}
						PoiUIFilter filter = new PoiUIFilter(filterName, filterId, map.get(filterId), app);
						filter.setSavedFilterByName(query.getString(2));
						filter.setDeleted(deleted);
						if (filter.getAcceptedTypesCount() > 0) {
							list.add(filter);
						}
					}
				} while (query.moveToNext());
			}
			if (query != null) {
				query.close();
			}
		}
		return list;
	}

	protected boolean editFilter(SQLiteConnection conn, PoiUIFilter filter) {
		if (conn != null) {
			conn.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " + CATEGORIES_FILTER_ID + " = ?",
					new Object[] {filter.getFilterId()});
			addFilter(filter, conn, true, false);
			updateName(conn, filter);
			updateLastModifiedTime();
			return true;
		}
		return false;
	}

	private void updateName(SQLiteConnection db, PoiUIFilter filter) {
		db.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_FILTERBYNAME + " = ?, " + FILTER_COL_NAME + " = ? " + " WHERE "
				+ FILTER_COL_ID + "= ?", new Object[] {filter.getFilterByName(), filter.getName(), filter.getFilterId()});
		updateLastModifiedTime();
	}

	protected boolean deleteFilter(SQLiteConnection db, PoiUIFilter p, boolean force) {
		if (db != null) {
			if (force) {
				deleteFilter(db, p.getFilterId());
			} else {
				db.execSQL("UPDATE " + FILTER_NAME + " SET " + FILTER_COL_DELETED + " = ? WHERE " + FILTER_COL_ID + " = ?",
						new Object[] {TRUE_INT, p.getFilterId()});
			}
			updateLastModifiedTime();
			return true;
		}
		return false;
	}

	private void deleteFilter(@NonNull SQLiteConnection db, String key) {
		db.execSQL("DELETE FROM " + FILTER_NAME + " WHERE " + FILTER_COL_ID + " = ?", new Object[] {key});
		db.execSQL("DELETE FROM " + CATEGORIES_NAME + " WHERE " + CATEGORIES_FILTER_ID + " = ?", new Object[] {key});
		updateLastModifiedTime();
	}

	@Nullable
	protected Pair<Long, Map<String, List<String>>> getCacheByResourceName(
			@NonNull SQLiteConnection db, String fileName) {
		Pair<Long, Map<String, List<String>>> cache = null;
		SQLiteCursor query = db.rawQuery("SELECT " +
				MAP_FILE_DATE + ", " +
				CACHED_POI_CATEGORIES +
				" FROM " +
				POI_TYPES_CACHE_NAME +
				" WHERE " + MAP_FILE_NAME + " = ?", new String[] {fileName});
		if (query != null && query.moveToFirst()) {
			long lastModified = query.getLong(0);
			Map<String, List<String>> categories = getCategories(query.getString(1));
			cache = new Pair<>(lastModified, categories);
		}
		if (query != null) {
			query.close();
		}
		db.close();
		return cache;
	}

	private Map<String, List<String>> getCategories(String json) {
		Map<String, List<String>> categories = new HashMap<>();
		try {
			JSONArray jsonArray = new JSONArray(json);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String category = jsonObject.optString(CATEGORY_KEY);
				List<String> subCategories = getSubCategories(jsonObject.optString(SUB_CATEGORIES_KEY));
				categories.put(category, subCategories);
			}
		} catch (JSONException e) {
			LOG.error("Error parsing categories: " + e);
		}
		return categories;
	}

	protected void updateCacheForResource(@NonNull SQLiteConnection db, String fileName,
			long lastModified, Map<String, List<String>> categories) {
		try {
			db.execSQL("UPDATE " + POI_TYPES_CACHE_NAME + " SET " +
							MAP_FILE_DATE + " = ?, " +
							CACHED_POI_CATEGORIES + " = ? " +
							"WHERE " + MAP_FILE_NAME + " = ?",
					new Object[] {lastModified, getCategoriesJson(categories), fileName});
		} catch (JSONException e) {
			LOG.error("Error converting category to json: " + e);
		}
	}

	protected void insertCacheForResource(@NonNull SQLiteConnection db, String fileName,
			long lastModified, Map<String, List<String>> categories) {
		try {
			db.execSQL("INSERT INTO " + POI_TYPES_CACHE_NAME + " VALUES(?,?,?)",
					new Object[] {fileName, lastModified, getCategoriesJson(categories)});
		} catch (JSONException e) {
			LOG.error("Error converting category to json: " + e);
		}
	}

	private String getCategoriesJson(
			Map<String, List<String>> categories) throws JSONException {
		JSONArray json = new JSONArray();
		for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
			JSONObject jsonObject = new JSONObject();
			JSONArray subCategories = new JSONArray();
			for (String subCategory : entry.getValue()) {
				subCategories.put(subCategory);
			}
			jsonObject.put(CATEGORY_KEY, entry.getKey());
			jsonObject.put(SUB_CATEGORIES_KEY, subCategories);
			json.put(jsonObject);
		}
		return json.toString();
	}

	private List<String> getSubCategories(@NonNull String json) throws JSONException {
		List<String> subCategories = new ArrayList<>();
		JSONArray jsonArray = new JSONArray(json);
		for (int i = 0; i < jsonArray.length(); i++) {
			subCategories.add(jsonArray.optString(i));
		}
		return subCategories;
	}
}
