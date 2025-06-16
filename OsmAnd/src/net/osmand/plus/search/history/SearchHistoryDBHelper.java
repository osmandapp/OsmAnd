package net.osmand.plus.search.history;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.backup.BackupUtils;
import net.osmand.plus.settings.enums.HistorySource;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SearchHistoryDBHelper {

	private static final Log log = PlatformUtil.getLog(SearchHistoryDBHelper.class);

	private static final String DB_NAME = "search_history";
	private static final int DB_VERSION = 3;
	private static final String HISTORY_TABLE_NAME = "history_recents";
	private static final String HISTORY_COL_NAME = "name";
	private static final String HISTORY_COL_TIME = "time";
	private static final String HISTORY_COL_FREQ_INTERVALS = "freq_intervals";
	private static final String HISTORY_COL_FREQ_VALUES = "freq_values";
	private static final String HISTORY_COL_LAT = "latitude";
	private static final String HISTORY_COL_LON = "longitude";
	private static final String HISTORY_COL_SOURCE = "source";
	private static final String HISTORY_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + HISTORY_TABLE_NAME + " (" +
			HISTORY_COL_NAME + " TEXT, " +
			HISTORY_COL_TIME + " long, " +
			HISTORY_COL_FREQ_INTERVALS + " TEXT, " +
			HISTORY_COL_FREQ_VALUES + " TEXT, " +
			HISTORY_COL_LAT + " double, " + HISTORY_COL_LON + " double, " + HISTORY_COL_SOURCE + " TEXT);";

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
		}
		if (oldVersion < 3) {
			db.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_SOURCE + " TEXT");
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
								HISTORY_COL_LAT + " = ? AND " + HISTORY_COL_LON + " = ?",
						new Object[] {entry.getSerializedName(), entry.getLat(), entry.getLon()});
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
								", " + HISTORY_COL_FREQ_VALUES + "= ? WHERE " +
								HISTORY_COL_NAME + " = ? AND " +
								HISTORY_COL_LAT + " = ? AND " + HISTORY_COL_LON + " = ? AND " + HISTORY_COL_SOURCE + " = ?",
						new Object[] {entry.getLastAccessTime(), entry.getIntervals(), entry.getIntervalsValues(),
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
				"INSERT INTO " + HISTORY_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?)",
				new Object[] {entry.getSerializedName(), entry.getLastAccessTime(), entry.getIntervals(),
						entry.getIntervalsValues(), entry.getLat(), entry.getLon(), entry.getSource().name()});
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
								" FROM " + HISTORY_TABLE_NAME, null);
				Map<PointDescription, HistoryEntry> st = new HashMap<>();
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

						PointDescription pd = PointDescription.deserializeFromString(name, new LatLon(lat, lon));
						if (app.getPoiTypes().isTypeForbidden(pd.getName())) {
							query.moveToNext();
						}
						HistoryEntry entry = new HistoryEntry(lat, lon, pd, source);
						entry.setLastAccessTime(lastAccessedTime);
						entry.setFrequency(frequencyIntervals, frequencyValues);
						if (st.containsKey(pd)) {
							reinsert = true;
						}
						entries.add(entry);
						st.put(pd, entry);
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
}
