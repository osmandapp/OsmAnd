package net.osmand.plus.wikivoyage.data;

import android.support.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gnu.trove.map.hash.TLongObjectHashMap;

public class WikivoyageSearchHistoryHelper {

	private static WikivoyageSearchHistoryHelper instance;

	private WikivoyageSearchHistoryDbHelper dbHelper;

	private TLongObjectHashMap<WikivoyageSearchHistoryItem> historyMap = new TLongObjectHashMap<>();
	private List<WikivoyageSearchHistoryItem> historyItems;

	private WikivoyageSearchHistoryHelper(OsmandApplication app) {
		dbHelper = new WikivoyageSearchHistoryDbHelper(app);
		loadHistory();
	}

	public static WikivoyageSearchHistoryHelper getInstance(OsmandApplication app) {
		if (instance == null) {
			instance = new WikivoyageSearchHistoryHelper(app);
		}
		return instance;
	}

	public List<WikivoyageSearchHistoryItem> getAllHistory() {
		return new ArrayList<>(historyItems);
	}

	public void addToHistory(WikivoyageArticle article) {
		addToHistory(article.getCityId(), article.getTitle(), article.getLang(), article.getIsPartOf());
	}

	public void addToHistory(long cityId, String title, String lang, String isPartOf) {
		WikivoyageSearchHistoryItem item = historyMap.get(cityId);
		boolean newItem = item == null;
		if (newItem) {
			item = new WikivoyageSearchHistoryItem();
			item.cityId = cityId;
		}
		item.articleTitle = title;
		item.lang = lang;
		item.isPartOf = isPartOf;
		item.lastAccessed = System.currentTimeMillis();
		if (newItem) {
			dbHelper.add(item);
			historyItems.add(item);
			historyMap.put(item.cityId, item);
		} else {
			dbHelper.update(item);
		}
		sortHistory();
	}

	private void loadHistory() {
		historyItems = dbHelper.getAllHistory();
		sortHistory();
		for (WikivoyageSearchHistoryItem item : historyItems) {
			historyMap.put(item.cityId, item);
		}
	}

	private void sortHistory() {
		Collections.sort(historyItems, new Comparator<WikivoyageSearchHistoryItem>() {
			@Override
			public int compare(WikivoyageSearchHistoryItem item1, WikivoyageSearchHistoryItem item2) {
				if (item1.lastAccessed > item2.lastAccessed) {
					return -1;
				} else if (item1.lastAccessed == item2.lastAccessed) {
					return 0;
				}
				return 1;
			}
		});
	}

	private static class WikivoyageSearchHistoryDbHelper {

		private static final int DB_VERSION = 1;
		private static final String DB_NAME = "wikivoyage_search_history";

		private static final String HISTORY_TABLE_NAME = "history_recents";
		private static final String HISTORY_COL_CITY_ID = "city_id";
		private static final String HISTORY_COL_ARTICLE_TITLE = "article_title";
		private static final String HISTORY_COL_LANG = "lang";
		private static final String HISTORY_COL_IS_PART_OF = "is_part_of";
		private static final String HISTORY_COL_LAST_ACCESSED = "last_accessed";

		private static final String HISTORY_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
				HISTORY_TABLE_NAME + " (" +
				HISTORY_COL_CITY_ID + " long, " +
				HISTORY_COL_ARTICLE_TITLE + " TEXT, " +
				HISTORY_COL_LANG + " TEXT, " +
				HISTORY_COL_IS_PART_OF + " TEXT, " +
				HISTORY_COL_LAST_ACCESSED + " long);";

		private static final String HISTORY_TABLE_SELECT = "SELECT " +
				HISTORY_COL_CITY_ID + ", " +
				HISTORY_COL_ARTICLE_TITLE + ", " +
				HISTORY_COL_LANG + ", " +
				HISTORY_COL_IS_PART_OF + ", " +
				HISTORY_COL_LAST_ACCESSED +
				" FROM " + HISTORY_TABLE_NAME;

		private final OsmandApplication context;

		private WikivoyageSearchHistoryDbHelper(OsmandApplication context) {
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

		private void onCreate(SQLiteConnection conn) {
			conn.execSQL(HISTORY_TABLE_CREATE);
		}

		@SuppressWarnings("unused")
		private void onUpgrade(SQLiteConnection conn, int oldVersion, int newVersion) {

		}

		@NonNull
		List<WikivoyageSearchHistoryItem> getAllHistory() {
			List<WikivoyageSearchHistoryItem> res = new ArrayList<>();
			SQLiteConnection conn = openConnection(true);
			if (conn != null) {
				try {
					SQLiteCursor cursor = conn.rawQuery(HISTORY_TABLE_SELECT, null);
					if (cursor.moveToFirst()) {
						do {
							res.add(readItem(cursor));
						} while (cursor.moveToNext());
					}
				} finally {
					conn.close();
				}
			}
			return res;
		}

		void add(WikivoyageSearchHistoryItem item) {
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					conn.execSQL("INSERT INTO " + HISTORY_TABLE_NAME + " VALUES (?, ?, ?, ?, ?)",
							new Object[]{item.cityId, item.articleTitle, item.lang, item.isPartOf, item.lastAccessed});
				} finally {
					conn.close();
				}
			}
		}

		void update(WikivoyageSearchHistoryItem item) {
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					conn.execSQL("UPDATE " + HISTORY_TABLE_NAME + " SET " +
									HISTORY_COL_ARTICLE_TITLE + " = ?, " +
									HISTORY_COL_LANG + " = ?, " +
									HISTORY_COL_IS_PART_OF + " = ?, " +
									HISTORY_COL_LAST_ACCESSED + " = ? " +
									"WHERE " + HISTORY_COL_CITY_ID + " = ?",
							new Object[]{item.articleTitle, item.lang, item.isPartOf, item.lastAccessed, item.cityId});
				} finally {
					conn.close();
				}
			}
		}

		private WikivoyageSearchHistoryItem readItem(SQLiteCursor cursor) {
			WikivoyageSearchHistoryItem res = new WikivoyageSearchHistoryItem();

			res.cityId = cursor.getLong(0);
			res.articleTitle = cursor.getString(1);
			res.lang = cursor.getString(2);
			res.isPartOf = cursor.getString(3);
			res.lastAccessed = cursor.getLong(4);

			return res;
		}
	}
}
