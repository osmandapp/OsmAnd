package net.osmand.plus.search.history;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.backup.BackupUtils;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.search.core.ObjectType;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SearchHistoryDBHelper {

	private static final Log log = PlatformUtil.getLog(SearchHistoryDBHelper.class);

	private static final String DB_NAME = "search_history";
	private static final int DB_VERSION = 4;
	private static final String HISTORY_TABLE_NAME = "history_recents";
	private static final String HISTORY_COL_NAME = "name";
	private static final String HISTORY_COL_TIME = "time";
	private static final String HISTORY_COL_FREQ_INTERVALS = "freq_intervals";
	private static final String HISTORY_COL_FREQ_VALUES = "freq_values";
	private static final String HISTORY_COL_LAT = "latitude";
	private static final String HISTORY_COL_LON = "longitude";
	private static final String HISTORY_COL_SOURCE = "source";
	private static final String HISTORY_COL_OBJECT_TYPE = "object_type";
	private static final String HISTORY_COL_CITY_TYPE = "city_type";
	private static final String HISTORY_COL_DISPLAY_NAME = "display_name";
	private static final String HISTORY_COL_POI_CATEGORY_KEY = "poi_category_key";
	private static final String HISTORY_COL_POI_SUBTYPE_KEY = "poi_subtype_key";
	private static final String HISTORY_COL_TYPE_NAME = "type_name";
	private static final String HISTORY_COL_ADDRESS = "address";
	private static final String HISTORY_COL_RELATED_OBJECT_NAME = "related_object_name";
	private static final String HISTORY_COL_OPENING_HOURS = "opening_hours";
	private static final String HISTORY_COL_ALTERNATE_NAME = "alternate_name";
	private static final String HISTORY_COL_PHOTO_URL = "photo_url";
	private static final String HISTORY_COL_OSM_ID = "osm_id";
	private static final String HISTORY_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + HISTORY_TABLE_NAME + " (" +
			HISTORY_COL_NAME + " TEXT, " +
			HISTORY_COL_TIME + " long, " +
			HISTORY_COL_FREQ_INTERVALS + " TEXT, " +
			HISTORY_COL_FREQ_VALUES + " TEXT, " +
			HISTORY_COL_LAT + " double, " + HISTORY_COL_LON + " double, " + HISTORY_COL_SOURCE + " TEXT, " +
			HISTORY_COL_OBJECT_TYPE + " TEXT, " +
			HISTORY_COL_CITY_TYPE + " TEXT, " +
			HISTORY_COL_DISPLAY_NAME + " TEXT, " +
			HISTORY_COL_POI_CATEGORY_KEY + " TEXT, " +
			HISTORY_COL_POI_SUBTYPE_KEY + " TEXT, " +
			HISTORY_COL_TYPE_NAME + " TEXT, " +
			HISTORY_COL_ADDRESS + " TEXT, " +
			HISTORY_COL_RELATED_OBJECT_NAME + " TEXT, " +
			HISTORY_COL_OPENING_HOURS + " TEXT, " +
			HISTORY_COL_ALTERNATE_NAME + " TEXT, " +
			HISTORY_COL_PHOTO_URL + " TEXT, " +
			HISTORY_COL_OSM_ID + " long);";

	private static final String HISTORY_LAST_MODIFIED_NAME = "history_recents";

	private final OsmandApplication app;

	SearchHistoryDBHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	private SQLiteConnection openConnection(boolean readonly) {
		SQLiteConnection conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
		if (conn != null && conn.getVersion() < DB_VERSION) {
			if (readonly) {
				conn.close();
				conn = app.getSQLiteAPI().getOrCreateDatabase(DB_NAME, false);
			}
			if (conn != null) {
				int version = conn.getVersion();
				if (version == 0) {
					onCreate(conn);
				} else {
					onUpgrade(conn, version, DB_VERSION);
				}
				conn.setVersion(DB_VERSION);
			}
		}
		return conn;
	}

	public void onCreate(@NonNull SQLiteConnection db) {
		db.execSQL(HISTORY_TABLE_CREATE);
	}

	public void onUpgrade(@NonNull SQLiteConnection db, int oldVersion, int newVersion) {
		boolean upgraded = false;
		if (oldVersion < 2) {
			db.execSQL("DROP TABLE IF EXISTS " + HISTORY_TABLE_NAME);
			onCreate(db);
			upgraded = true;
		} else {
			if (oldVersion < 3) {
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_SOURCE + " TEXT");
				upgraded = true;
			}
			if (oldVersion < 4) {
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_OBJECT_TYPE + " TEXT");
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_DISPLAY_NAME + " TEXT");
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_POI_CATEGORY_KEY + " TEXT");
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_POI_SUBTYPE_KEY + " TEXT");
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_TYPE_NAME + " TEXT");
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_ADDRESS + " TEXT");
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_OPENING_HOURS + " TEXT");
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_ALTERNATE_NAME + " TEXT");
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_PHOTO_URL + " TEXT");
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_OSM_ID + " long");
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_RELATED_OBJECT_NAME + " TEXT");
				db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_CITY_TYPE + " TEXT");
				upgraded = true;
			}
		}
		if (upgraded) {
			updateLastModifiedTime();
		}
	}

	public long getLastModifiedTime() {
		long lastModifiedTime = BackupUtils.getLastModifiedTime(app, HISTORY_LAST_MODIFIED_NAME);
		if (lastModifiedTime == 0) {
			File dbFile = app.getDatabasePath(DB_NAME);
			lastModifiedTime = dbFile.exists() ? dbFile.lastModified() : 0;
			BackupUtils.setLastModifiedTime(app, HISTORY_LAST_MODIFIED_NAME, lastModifiedTime);
		}
		return lastModifiedTime;
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		BackupUtils.setLastModifiedTime(app, HISTORY_LAST_MODIFIED_NAME, lastModifiedTime);
	}

	private void updateLastModifiedTime() {
		BackupUtils.setLastModifiedTime(app, HISTORY_LAST_MODIFIED_NAME);
	}

	public boolean remove(@NonNull HistoryEntry entry) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME + " WHERE " +
								HISTORY_COL_NAME + " = ? AND " +
								HISTORY_COL_LAT + " = ? AND " + HISTORY_COL_LON + " = ? AND " +
								HISTORY_COL_SOURCE + " = ?",
						new Object[] {entry.getSerializedName(), entry.getLat(), entry.getLon(), entry.getSource().name()});
				updateLastModifiedTime();
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean removeAll() {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME);
				updateLastModifiedTime();
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean update(@NonNull HistoryEntry entry) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				db.execSQL(
						"UPDATE " + HISTORY_TABLE_NAME + " SET " + HISTORY_COL_TIME + "= ? " +
								", " + HISTORY_COL_FREQ_INTERVALS + " = ? " +
								", " + HISTORY_COL_FREQ_VALUES + "= ? " +
								", " + HISTORY_COL_OBJECT_TYPE + "= ? " +
								", " + HISTORY_COL_CITY_TYPE + "= ? " +
								", " + HISTORY_COL_DISPLAY_NAME + "= ? " +
								", " + HISTORY_COL_POI_CATEGORY_KEY + "= ? " +
								", " + HISTORY_COL_POI_SUBTYPE_KEY + "= ? " +
								", " + HISTORY_COL_TYPE_NAME + "= ? " +
								", " + HISTORY_COL_ADDRESS + "= ? " +
								", " + HISTORY_COL_RELATED_OBJECT_NAME + "= ? " +
								", " + HISTORY_COL_OPENING_HOURS + "= ? " +
								", " + HISTORY_COL_ALTERNATE_NAME + "= ? " +
								", " + HISTORY_COL_PHOTO_URL + "= ? " +
								", " + HISTORY_COL_OSM_ID + "= ? WHERE " +
								HISTORY_COL_NAME + " = ? AND " +
								HISTORY_COL_LAT + " = ? AND " + HISTORY_COL_LON + " = ? AND " + HISTORY_COL_SOURCE + " = ?",
						new Object[] {entry.getLastAccessTime(), entry.getIntervals(), entry.getIntervalsValues(),
								getObjectTypeName(entry), getCityTypeName(entry), entry.getDisplayName(), entry.getPoiCategoryKey(),
								entry.getPoiSubtypeKey(), entry.getTypeName(), entry.getAddress(),
								entry.getRelatedObjectName(), entry.getOpeningHours(), entry.getAlternateName(),
								entry.getPhotoUrl(), entry.getOsmId(),
								entry.getSerializedName(), entry.getLat(), entry.getLon(), entry.getSource().name()});
				updateLastModifiedTime();
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	public boolean add(@NonNull HistoryEntry entry) {
		SQLiteConnection db = openConnection(false);
		if (db != null) {
			try {
				insert(entry, db);
			} finally {
				db.close();
			}
			return true;
		}
		return false;
	}

	private void insert(@NonNull HistoryEntry entry, @NonNull SQLiteConnection db) {
		db.execSQL(
				"INSERT INTO " + HISTORY_TABLE_NAME + " (" +
						HISTORY_COL_NAME + ", " + HISTORY_COL_TIME + ", " + HISTORY_COL_FREQ_INTERVALS + ", " +
						HISTORY_COL_FREQ_VALUES + ", " + HISTORY_COL_LAT + ", " + HISTORY_COL_LON + ", " +
						HISTORY_COL_SOURCE + ", " + HISTORY_COL_OBJECT_TYPE + ", " + HISTORY_COL_CITY_TYPE + ", " +
						HISTORY_COL_DISPLAY_NAME + ", " + HISTORY_COL_POI_CATEGORY_KEY + ", " +
						HISTORY_COL_POI_SUBTYPE_KEY + ", " + HISTORY_COL_TYPE_NAME + ", " + HISTORY_COL_ADDRESS + ", " +
						HISTORY_COL_RELATED_OBJECT_NAME + ", " + HISTORY_COL_OPENING_HOURS + ", " +
						HISTORY_COL_ALTERNATE_NAME + ", " + HISTORY_COL_PHOTO_URL + ", " +
						HISTORY_COL_OSM_ID + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[] {entry.getSerializedName(), entry.getLastAccessTime(), entry.getIntervals(),
						entry.getIntervalsValues(), entry.getLat(), entry.getLon(), entry.getSource().name(),
						getObjectTypeName(entry), getCityTypeName(entry), entry.getDisplayName(), entry.getPoiCategoryKey(),
						entry.getPoiSubtypeKey(), entry.getTypeName(), entry.getAddress(), entry.getRelatedObjectName(),
						entry.getOpeningHours(), entry.getAlternateName(), entry.getPhotoUrl(), entry.getOsmId()});
		updateLastModifiedTime();
	}

	@NonNull
	public List<HistoryEntry> getEntries() {
		List<HistoryEntry> entries = new ArrayList<>();
		SQLiteConnection db = openConnection(true);
		if (db != null) {
			try {
				SQLiteCursor query = db.rawQuery(
						"SELECT " + HISTORY_COL_NAME + ", " + HISTORY_COL_LAT + "," + HISTORY_COL_LON + ", " +
								HISTORY_COL_TIME + ", " + HISTORY_COL_FREQ_INTERVALS + ", " + HISTORY_COL_FREQ_VALUES + ", " + HISTORY_COL_SOURCE +
								", " + HISTORY_COL_OBJECT_TYPE + ", " + HISTORY_COL_CITY_TYPE + ", " +
								HISTORY_COL_DISPLAY_NAME + ", " + HISTORY_COL_POI_CATEGORY_KEY + ", " +
								HISTORY_COL_POI_SUBTYPE_KEY + ", " + HISTORY_COL_TYPE_NAME + ", " + HISTORY_COL_ADDRESS + ", " +
								HISTORY_COL_RELATED_OBJECT_NAME + ", " + HISTORY_COL_OPENING_HOURS +
								", " + HISTORY_COL_ALTERNATE_NAME + ", " + HISTORY_COL_PHOTO_URL + ", " + HISTORY_COL_OSM_ID +
								" FROM " + HISTORY_TABLE_NAME, null);
				Map<HistoryEntryKey, HistoryEntry> st = new HashMap<>();
				if (query != null && query.moveToFirst()) {
					boolean reinsert = false;
					do {
						String name = query.getString(0);
						double lat = query.getDouble(1);
						double lon = query.getDouble(2);
						long lastAccessedTime = query.getLong(3);
						String frequencyIntervals = query.getString(4);
						String frequencyValues = query.getString(5);
						HistorySource source = HistorySource.getHistorySourceByName(query.getString(6));
						String objectTypeName = query.getString(7);
						String cityTypeName = query.getString(8);

						PointDescription pd = PointDescription.deserializeFromString(name, new LatLon(lat, lon));
						if (app.getPoiTypes().isTypeForbidden(pd.getName())) {
							query.moveToNext();
						}
						HistoryEntry entry = new HistoryEntry(lat, lon, pd, source);
						entry.setLastAccessTime(lastAccessedTime);
						entry.setFrequency(frequencyIntervals, frequencyValues);
						entry.setObjectType(getObjectType(objectTypeName));
						entry.setCityType(getCityType(cityTypeName));
						entry.setDisplayName(query.getString(9));
						entry.setPoiCategoryKey(query.getString(10));
						entry.setPoiSubtypeKey(query.getString(11));
						entry.setTypeName(query.getString(12));
						entry.setAddress(query.getString(13));
						entry.setRelatedObjectName(query.getString(14));
						entry.setOpeningHours(query.getString(15));
						entry.setAlternateName(query.getString(16));
						entry.setPhotoUrl(query.getString(17));
						entry.setOsmId(query.isNull(18) ? null : query.getLong(18));
						HistoryEntryKey key = new HistoryEntryKey(entry);
						if (st.containsKey(key)) {
							reinsert = true;
						}
						entries.add(entry);
						st.put(key, entry);
					} while (query.moveToNext());
					if (reinsert) {
						log.error("Reinsert all values for search history");
						db.execSQL("DELETE FROM " + HISTORY_TABLE_NAME);
						entries.clear();
						entries.addAll(st.values());
						for (HistoryEntry he : entries) {
							insert(he, db);
						}
						updateLastModifiedTime();
					}
				}
				if (query != null) {
					query.close();
				}
			} finally {
				db.close();
			}
		}
		return entries;
	}

	@Nullable
	private String getObjectTypeName(@NonNull HistoryEntry entry) {
		ObjectType objectType = entry.getObjectType();
		return objectType != null ? objectType.name() : null;
	}

	@Nullable
	private String getCityTypeName(@NonNull HistoryEntry entry) {
		CityType cityType = entry.getCityType();
		return cityType != null ? cityType.name() : null;
	}

	@Nullable
	private ObjectType getObjectType(@Nullable String objectTypeName) {
		if (objectTypeName != null) {
			try {
				return ObjectType.valueOf(objectTypeName);
			} catch (IllegalArgumentException e) {
				log.warn("Unsupported history object type: " + objectTypeName);
			}
		}
		return null;
	}

	@Nullable
	private CityType getCityType(@Nullable String cityTypeName) {
		if (cityTypeName != null) {
			try {
				return CityType.valueOf(cityTypeName);
			} catch (IllegalArgumentException e) {
				log.warn("Unsupported history city type: " + cityTypeName);
			}
		}
		return null;
	}
}
