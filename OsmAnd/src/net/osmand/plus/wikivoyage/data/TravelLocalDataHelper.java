package net.osmand.plus.wikivoyage.data;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.wikipedia.WikiArticleHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class TravelLocalDataHelper {

	private static final int HISTORY_ITEMS_LIMIT = 300;

	private WikivoyageLocalDataDbHelper dbHelper;

	private Map<String, WikivoyageSearchHistoryItem> historyMap = new HashMap<>();
	private List<TravelArticle> savedArticles = new ArrayList<>();

	private Set<Listener> listeners = new HashSet<>();

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	TravelLocalDataHelper(OsmandApplication app) {
		dbHelper = new WikivoyageLocalDataDbHelper(app);
	}

	void refreshCachedData() {
		historyMap = dbHelper.getAllHistoryMap();
		savedArticles = dbHelper.readSavedArticles();
	}

	public List<WikivoyageSearchHistoryItem> getAllHistory() {
		List<WikivoyageSearchHistoryItem> res = new ArrayList<>(historyMap.values());
		Collections.sort(res, new Comparator<WikivoyageSearchHistoryItem>() {
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
		return res;
	}

	public void clearHistory() {
		historyMap.clear();
		dbHelper.clearAllHistory();
	}

	public void addToHistory(@NonNull TravelArticle article) {
		addToHistory(article.getTitle(), article.getLang(), article.getIsPartOf());
	}

	public void addToHistory(String title, String lang, String isPartOf) {
		String key = getHistoryKey(lang, title);
		WikivoyageSearchHistoryItem item = historyMap.get(key);
		boolean newItem = item == null;
		if (newItem) {
			item = new WikivoyageSearchHistoryItem();
		}
		item.articleTitle = title;
		item.lang = lang;
		item.isPartOf = isPartOf;
		item.lastAccessed = System.currentTimeMillis();
		if (newItem) {
			dbHelper.addHistoryItem(item);
			historyMap.put(key, item);
		} else {
			dbHelper.updateHistoryItem(item);
		}
		if (historyMap.size() > HISTORY_ITEMS_LIMIT) {
			List<WikivoyageSearchHistoryItem> allHistory = getAllHistory();
			WikivoyageSearchHistoryItem lastItem = allHistory.get(allHistory.size() - 1);
			dbHelper.removeHistoryItem(lastItem);
			historyMap.remove(key);
		}
	}

	static String getHistoryKey(String lang, String title) {
		return lang + ":"+title;
	}

	@NonNull
	public List<TravelArticle> getSavedArticles() {
		return new ArrayList<>(savedArticles);
	}

	public void addArticleToSaved(@NonNull TravelArticle article) {
		if (!isArticleSaved(article)) {
			TravelArticle saved = new TravelArticle();
			saved.title = article.title;
			saved.lang = article.lang;
			saved.aggregatedPartOf = article.aggregatedPartOf;
			saved.imageTitle = article.imageTitle;
			saved.content = WikiArticleHelper.getPartialContent(article.getContent());
			saved.lat = article.lat;
			saved.lon = article.lon;
			saved.routeId = article.routeId;
			saved.fullContent = article.getContent();
			saved.contentsJson = article.contentsJson;
			savedArticles.add(saved);
			dbHelper.addSavedArticle(saved);
			notifySavedUpdated();
		}
	}

	public void restoreSavedArticle(@NonNull TravelArticle article) {
		if (!isArticleSaved(article)) {
			savedArticles.add(article);
			dbHelper.addSavedArticle(article);
			notifySavedUpdated();
		}
	}

	public void removeArticleFromSaved(@NonNull TravelArticle article) {
		TravelArticle savedArticle = getArticle(article.title, article.lang);
		if (savedArticle != null) {
			savedArticles.remove(savedArticle);
			dbHelper.removeSavedArticle(savedArticle);
			notifySavedUpdated();
		}
	}

	public boolean isArticleSaved(@NonNull TravelArticle article) {
		return getArticle(article.title, article.lang) != null;
	}

	private void notifySavedUpdated() {
		for (Listener listener : listeners) {
			listener.savedArticlesUpdated();
		}
	}

	@Nullable
	private TravelArticle getArticle(String title, String lang) {
		for (TravelArticle article : savedArticles) {
			if (article.title != null && article.title.equals(title) && article.lang != null && article.lang.equals(lang)) {
				return article;
			}
		}
		return null;
	}

	@Nullable
	public TravelArticle getSavedArticle(String routeId, String lang) {
		for (TravelArticle article : savedArticles) {
			if (article.routeId != null && article.routeId.equals(routeId)
					&& article.lang != null && article.lang.equals(lang)) {
				article.content = article.fullContent;
				return article;
			}
		}
		return null;
	}

	public interface Listener {

		void savedArticlesUpdated();
	}

	private static class WikivoyageLocalDataDbHelper {

		private static final int DB_VERSION = 6;
		private static final String DB_NAME = "wikivoyage_local_data";

		private static final String HISTORY_TABLE_NAME = "wikivoyage_search_history";
		private static final String HISTORY_COL_ARTICLE_TITLE = "article_title";
		private static final String HISTORY_COL_LANG = "lang";
		private static final String HISTORY_COL_IS_PART_OF = "is_part_of";
		private static final String HISTORY_COL_LAST_ACCESSED = "last_accessed";
		private static final String HISTORY_COL_TRAVEL_BOOK = "travel_book";

		private static final String HISTORY_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
				HISTORY_TABLE_NAME + " (" +
				HISTORY_COL_ARTICLE_TITLE + " TEXT, " +
				HISTORY_COL_LANG + " TEXT, " +
				HISTORY_COL_IS_PART_OF + " TEXT, " +
				HISTORY_COL_LAST_ACCESSED + " long, " +
				HISTORY_COL_TRAVEL_BOOK + " TEXT);";

		private static final String HISTORY_TABLE_SELECT = "SELECT " +
				HISTORY_COL_ARTICLE_TITLE + ", " +
				HISTORY_COL_LANG + ", " +
				HISTORY_COL_IS_PART_OF + ", " +
				HISTORY_COL_LAST_ACCESSED +
				" FROM " + HISTORY_TABLE_NAME;

		private static final String BOOKMARKS_TABLE_NAME = "wikivoyage_saved_articles";
		private static final String BOOKMARKS_COL_ARTICLE_TITLE = "article_title";
		private static final String BOOKMARKS_COL_LANG = "lang";
		private static final String BOOKMARKS_COL_IS_PART_OF = "is_part_of";
		private static final String BOOKMARKS_COL_IMAGE_TITLE = "image_title";
		private static final String BOOKMARKS_COL_PARTIAL_CONTENT = "partial_content";
		private static final String BOOKMARKS_COL_TRAVEL_BOOK = "travel_book";
		private static final String BOOKMARKS_COL_LAT = "lat";
		private static final String BOOKMARKS_COL_LON = "lon";
		private static final String BOOKMARKS_COL_ROUTE_ID = "route_id";
		private static final String BOOKMARKS_COL_CONTENT_JSON = "content_json";
		private static final String BOOKMARKS_COL_CONTENT = "content";

		private static final String BOOKMARKS_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
				BOOKMARKS_TABLE_NAME + " (" +
				BOOKMARKS_COL_ARTICLE_TITLE + " TEXT, " +
				BOOKMARKS_COL_LANG + " TEXT, " +
				BOOKMARKS_COL_IS_PART_OF + " TEXT, " +
				BOOKMARKS_COL_IMAGE_TITLE + " TEXT, " +
				BOOKMARKS_COL_PARTIAL_CONTENT + " TEXT, " +
				BOOKMARKS_COL_TRAVEL_BOOK + " TEXT, " +
				BOOKMARKS_COL_LAT + " double, " +
				BOOKMARKS_COL_LON + " double, " +
				BOOKMARKS_COL_ROUTE_ID + " TEXT, " +
				BOOKMARKS_COL_CONTENT_JSON + " TEXT, " +
				BOOKMARKS_COL_CONTENT + " TEXT" + ");";

		private static final String BOOKMARKS_TABLE_SELECT = "SELECT " +
				BOOKMARKS_COL_ARTICLE_TITLE + ", " +
				BOOKMARKS_COL_LANG + ", " +
				BOOKMARKS_COL_IS_PART_OF + ", " +
				BOOKMARKS_COL_IMAGE_TITLE + ", " +
				BOOKMARKS_COL_PARTIAL_CONTENT + ", " +
				BOOKMARKS_COL_LAT + ", " +
				BOOKMARKS_COL_LON + ", " +
				BOOKMARKS_COL_ROUTE_ID + ", " +
				BOOKMARKS_COL_CONTENT_JSON + ", " +
				BOOKMARKS_COL_CONTENT +
				" FROM " + BOOKMARKS_TABLE_NAME;

		private final OsmandApplication context;

		WikivoyageLocalDataDbHelper(OsmandApplication context) {
			this.context = context;
		}

		private SQLiteConnection openConnection(boolean readonly) {
			SQLiteConnection conn = context.getSQLiteAPI().getOrCreateDatabase(DB_NAME, readonly);
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

		private void onCreate(SQLiteConnection conn) {
			conn.execSQL(HISTORY_TABLE_CREATE);
			conn.execSQL(BOOKMARKS_TABLE_CREATE);
		}

		private void onUpgrade(SQLiteConnection conn, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				conn.execSQL(BOOKMARKS_TABLE_CREATE);
			}
			if (oldVersion < 3) {
				conn.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_TRAVEL_BOOK + " TEXT");
				conn.execSQL("ALTER TABLE " + BOOKMARKS_TABLE_NAME + " ADD " + BOOKMARKS_COL_TRAVEL_BOOK + " TEXT");
				String selectedTravelBookName = getSelectedTravelBookName();
				if (selectedTravelBookName != null) {
					Object[] args = new Object[]{selectedTravelBookName};
					conn.execSQL("UPDATE " + HISTORY_TABLE_NAME + " SET " + HISTORY_COL_TRAVEL_BOOK + " = ?", args);
					conn.execSQL("UPDATE " + BOOKMARKS_TABLE_NAME + " SET " + BOOKMARKS_COL_TRAVEL_BOOK + " = ?", args);
				}
			}
			if (oldVersion < 4) {
				conn.execSQL("ALTER TABLE " + BOOKMARKS_TABLE_NAME + " ADD " + BOOKMARKS_COL_LAT + " double");
				conn.execSQL("ALTER TABLE " + BOOKMARKS_TABLE_NAME + " ADD " + BOOKMARKS_COL_LON + " double");
			}
			if (oldVersion < 5) {
				conn.execSQL("ALTER TABLE " + BOOKMARKS_TABLE_NAME + " ADD " + BOOKMARKS_COL_ROUTE_ID + " TEXT");
			}
			if (oldVersion < 6) {
				conn.execSQL("ALTER TABLE " + BOOKMARKS_TABLE_NAME + " ADD " + BOOKMARKS_COL_CONTENT_JSON + " TEXT");
				conn.execSQL("ALTER TABLE " + BOOKMARKS_TABLE_NAME + " ADD " + BOOKMARKS_COL_CONTENT + " TEXT");
			}
		}

		@NonNull
		Map<String, WikivoyageSearchHistoryItem> getAllHistoryMap() {
			Map<String, WikivoyageSearchHistoryItem> res = new LinkedHashMap<>();
			String travelBook = getSelectedTravelBookName();
			if (travelBook == null) {
				return res;
			}
			SQLiteConnection conn = openConnection(true);
			if (conn != null) {
				try {
					String query = HISTORY_TABLE_SELECT + " WHERE " + HISTORY_COL_TRAVEL_BOOK + " = ?";
					SQLiteCursor cursor = conn.rawQuery(query, new String[]{travelBook});
					if (cursor != null) {
						if (cursor.moveToFirst()) {
							do {
								WikivoyageSearchHistoryItem item = readHistoryItem(cursor);
								res.put(item.getKey(), item);
							} while (cursor.moveToNext());
						}
					}
					cursor.close();
				} finally {
					conn.close();
				}
			}
			return res;
		}

		void addHistoryItem(WikivoyageSearchHistoryItem item) {
			String travelBook = getSelectedTravelBookName();
			if (travelBook == null) {
				return;
			}
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					conn.execSQL("INSERT INTO " + HISTORY_TABLE_NAME + "(" + HISTORY_COL_ARTICLE_TITLE + ", "
							+ HISTORY_COL_LANG + ", " + HISTORY_COL_IS_PART_OF + ", " + HISTORY_COL_LAST_ACCESSED
							+ ", " + HISTORY_COL_TRAVEL_BOOK + ") VALUES (?, ?, ?, ?, ?)", new Object[] {
							item.articleTitle, item.lang, item.isPartOf, item.lastAccessed, travelBook });
				} finally {
					conn.close();
				}
			}
		}

		void updateHistoryItem(WikivoyageSearchHistoryItem item) {
			String travelBook = getSelectedTravelBookName();
			if (travelBook == null) {
				return;
			}
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					conn.execSQL("UPDATE " + HISTORY_TABLE_NAME + " SET " +
									HISTORY_COL_IS_PART_OF + " = ?, " +
									HISTORY_COL_LAST_ACCESSED + " = ? " +
									"WHERE " + HISTORY_COL_ARTICLE_TITLE + " = ? " +
									" AND " + HISTORY_COL_LANG + " = ?" +
									" AND " + HISTORY_COL_TRAVEL_BOOK + " = ?",
							new Object[]{item.isPartOf, item.lastAccessed, 
								item.articleTitle, item.lang, travelBook});
				} finally {
					conn.close();
				}
			}
		}

		void removeHistoryItem(WikivoyageSearchHistoryItem item) {
			String travelBook = getSelectedTravelBookName();
			if (travelBook == null) {
				return;
			}
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					conn.execSQL("DELETE FROM " + HISTORY_TABLE_NAME +
									" WHERE " + HISTORY_COL_ARTICLE_TITLE+ " = ?" +
									" AND " + HISTORY_COL_LANG + " = ?" +
									" AND " + HISTORY_COL_TRAVEL_BOOK + " = ?",
							new Object[]{item.articleTitle, item.lang, travelBook});
				} finally {
					conn.close();
				}
			}
		}

		void clearAllHistory() {
			String travelBook = getSelectedTravelBookName();
			if (travelBook == null) {
				return;
			}
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					conn.execSQL("DELETE FROM " + HISTORY_TABLE_NAME +
									" WHERE " + HISTORY_COL_TRAVEL_BOOK + " = ?",
							new Object[]{travelBook});
				} finally {
					conn.close();
				}
			}
		}

		@NonNull
		List<TravelArticle> readSavedArticles() {
			List<TravelArticle> res = new ArrayList<>();
			String travelBook = getSelectedTravelBookName();
			if (travelBook == null) {
				return res;
			}
			SQLiteConnection conn = openConnection(true);
			if (conn != null) {
				try {
					String query = BOOKMARKS_TABLE_SELECT + " WHERE " + BOOKMARKS_COL_TRAVEL_BOOK + " = ?";
					SQLiteCursor cursor = conn.rawQuery(query, new String[]{travelBook});
					if (cursor != null) {
						if (cursor.moveToFirst()) {
							do {
								res.add(readSavedArticle(cursor));
							} while (cursor.moveToNext());
						}
						cursor.close();
					}
				} finally {
					conn.close();
				}
			}
			return res;
		}

		void addSavedArticle(TravelArticle article) {
			String travelBook = getSelectedTravelBookName();
			if (travelBook == null) {
				return;
			}
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					String query = "INSERT INTO " + BOOKMARKS_TABLE_NAME + " (" +
							BOOKMARKS_COL_ARTICLE_TITLE + ", " +
							BOOKMARKS_COL_LANG + ", " +
							BOOKMARKS_COL_IS_PART_OF + ", " +
							BOOKMARKS_COL_IMAGE_TITLE + ", " +
							BOOKMARKS_COL_PARTIAL_CONTENT + ", " +
							BOOKMARKS_COL_TRAVEL_BOOK + ", " +
							BOOKMARKS_COL_LAT + ", " +
							BOOKMARKS_COL_LON + ", " +
							BOOKMARKS_COL_ROUTE_ID + ", " +
							BOOKMARKS_COL_CONTENT_JSON + ", " +
							BOOKMARKS_COL_CONTENT +
							") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
					conn.execSQL(query, new Object[]{article.title, article.lang,
							article.aggregatedPartOf, article.imageTitle, article.content,
							travelBook, article.lat, article.lon, article.routeId, article.contentsJson,
							article.fullContent});
				} finally {
					conn.close();
				}
			}
		}

		void removeSavedArticle(TravelArticle article) {
			String travelBook = getSelectedTravelBookName();
			if (travelBook == null) {
				return;
			}
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					conn.execSQL("DELETE FROM " + BOOKMARKS_TABLE_NAME +
									" WHERE " + BOOKMARKS_COL_ARTICLE_TITLE + " = ?" +
									" AND " + BOOKMARKS_COL_LANG + " = ?" +
									" AND " + BOOKMARKS_COL_TRAVEL_BOOK + " = ?",
							new Object[]{article.title, article.lang, travelBook});
				} finally {
					conn.close();
				}
			}
		}

		@Nullable
		private String getSelectedTravelBookName() {
			return context.getTravelHelper().getSelectedTravelBookName();
		}

		private WikivoyageSearchHistoryItem readHistoryItem(SQLiteCursor cursor) {
			WikivoyageSearchHistoryItem res = new WikivoyageSearchHistoryItem();
			res.articleTitle = cursor.getString(cursor.getColumnIndex(HISTORY_COL_ARTICLE_TITLE));
			res.lang = cursor.getString(cursor.getColumnIndex(HISTORY_COL_LANG));
			res.isPartOf = cursor.getString(cursor.getColumnIndex(HISTORY_COL_IS_PART_OF));
			res.lastAccessed = cursor.getLong(cursor.getColumnIndex(HISTORY_COL_LAST_ACCESSED));

			return res;
		}

		private TravelArticle readSavedArticle(SQLiteCursor cursor) {
			TravelArticle res = new TravelArticle();

			res.title = cursor.getString(cursor.getColumnIndex(BOOKMARKS_COL_ARTICLE_TITLE));
			res.lang = cursor.getString(cursor.getColumnIndex(BOOKMARKS_COL_LANG));
			res.aggregatedPartOf = cursor.getString(cursor.getColumnIndex(BOOKMARKS_COL_IS_PART_OF));
			res.imageTitle = cursor.getString(cursor.getColumnIndex(BOOKMARKS_COL_IMAGE_TITLE));
			res.content = cursor.getString(cursor.getColumnIndex(BOOKMARKS_COL_PARTIAL_CONTENT));
			res.lat = cursor.getDouble(cursor.getColumnIndex(BOOKMARKS_COL_LAT));
			res.lon = cursor.getDouble(cursor.getColumnIndex(BOOKMARKS_COL_LON));
			res.routeId = cursor.getString(cursor.getColumnIndex(BOOKMARKS_COL_ROUTE_ID));
			res.contentsJson = cursor.getString(cursor.getColumnIndex(BOOKMARKS_COL_CONTENT_JSON));
			res.fullContent = cursor.getString(cursor.getColumnIndex(BOOKMARKS_COL_CONTENT));

			return res;
		}
	}
}
