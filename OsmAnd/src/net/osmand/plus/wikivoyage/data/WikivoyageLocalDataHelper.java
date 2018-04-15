package net.osmand.plus.wikivoyage.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gnu.trove.map.hash.TLongObjectHashMap;

public class WikivoyageLocalDataHelper {

	private static final int HISTORY_ITEMS_LIMIT = 300;

	private WikivoyageLocalDataDbHelper dbHelper;

	private TLongObjectHashMap<WikivoyageSearchHistoryItem> historyMap;
	private List<WikivoyageArticle> savedArticles;

	private Listener listener;

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	protected WikivoyageLocalDataHelper(OsmandApplication app) {
		dbHelper = new WikivoyageLocalDataDbHelper(app);
		refreshHistoryArticles();
	}

	public void refreshHistoryArticles() {
		historyMap = dbHelper.getAllHistoryMap();
		savedArticles = dbHelper.getSavedArticles();
	}


	public List<WikivoyageSearchHistoryItem> getAllHistory() {
		List<WikivoyageSearchHistoryItem> res = new ArrayList<>(historyMap.valueCollection());
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

	public void addToHistory(@NonNull WikivoyageArticle article) {
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
			dbHelper.addHistoryItem(item);
			historyMap.put(item.cityId, item);
		} else {
			dbHelper.updateHistoryItem(item);
		}
		if (historyMap.size() > HISTORY_ITEMS_LIMIT) {
			List<WikivoyageSearchHistoryItem> allHistory = getAllHistory();
			WikivoyageSearchHistoryItem lastItem = allHistory.get(allHistory.size() - 1);
			dbHelper.removeHistoryItem(lastItem);
			historyMap.remove(lastItem.cityId);
		}
	}

	@NonNull
	public List<WikivoyageArticle> getSavedArticles() {
		return new ArrayList<>(savedArticles);
	}

	public void addArticleToSaved(@NonNull WikivoyageArticle article) {
		if (!isArticleSaved(article)) {
			WikivoyageArticle saved = new WikivoyageArticle();
			saved.cityId = article.cityId;
			saved.title = article.title;
			saved.lang = article.lang;
			saved.aggregatedPartOf = article.aggregatedPartOf;
			saved.imageTitle = article.imageTitle;
			saved.content = article.getPartialContent();
			savedArticles.add(saved);
			dbHelper.addSavedArticle(saved);
			notifySavedUpdated();
		}
	}

	public void restoreSavedArticle(@NonNull WikivoyageArticle article) {
		if (!isArticleSaved(article)) {
			savedArticles.add(article);
			dbHelper.addSavedArticle(article);
			notifySavedUpdated();
		}
	}

	public void removeArticleFromSaved(@NonNull WikivoyageArticle article) {
		WikivoyageArticle savedArticle = getArticle(article.cityId, article.lang);
		if (savedArticle != null) {
			savedArticles.remove(savedArticle);
			dbHelper.removeSavedArticle(savedArticle);
			notifySavedUpdated();
		}
	}

	public boolean isArticleSaved(@NonNull WikivoyageArticle article) {
		return getArticle(article.cityId, article.lang) != null;
	}

	private void notifySavedUpdated() {
		if (listener != null) {
			listener.savedArticlesUpdated();
		}
	}

	@Nullable
	private WikivoyageArticle getArticle(long cityId, String lang) {
		for (WikivoyageArticle article : savedArticles) {
			if (article.cityId == cityId && article.lang != null && article.lang.equals(lang)) {
				return article;
			}
		}
		return null;
	}

	public interface Listener {

		void savedArticlesUpdated();
	}

	private static class WikivoyageLocalDataDbHelper {

		private static final int DB_VERSION = 3;
		private static final String DB_NAME = "wikivoyage_local_data";

		private static final String HISTORY_TABLE_NAME = "wikivoyage_search_history";
		private static final String HISTORY_COL_CITY_ID = "city_id";
		private static final String HISTORY_COL_ARTICLE_TITLE = "article_title";
		private static final String HISTORY_COL_LANG = "lang";
		private static final String HISTORY_COL_IS_PART_OF = "is_part_of";
		private static final String HISTORY_COL_LAST_ACCESSED = "last_accessed";
		private static final String HISTORY_COL_TRAVEL_BOOK = "travel_book";

		private static final String HISTORY_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
				HISTORY_TABLE_NAME + " (" +
				HISTORY_COL_CITY_ID + " long, " +
				HISTORY_COL_ARTICLE_TITLE + " TEXT, " +
				HISTORY_COL_LANG + " TEXT, " +
				HISTORY_COL_IS_PART_OF + " TEXT, " +
				HISTORY_COL_LAST_ACCESSED + " long, " +
				HISTORY_COL_TRAVEL_BOOK + " TEXT);";

		private static final String HISTORY_TABLE_SELECT = "SELECT " +
				HISTORY_COL_CITY_ID + ", " +
				HISTORY_COL_ARTICLE_TITLE + ", " +
				HISTORY_COL_LANG + ", " +
				HISTORY_COL_IS_PART_OF + ", " +
				HISTORY_COL_LAST_ACCESSED +
				" FROM " + HISTORY_TABLE_NAME;

		private static final String BOOKMARKS_TABLE_NAME = "wikivoyage_saved_articles";
		private static final String BOOKMARKS_COL_CITY_ID = "city_id";
		private static final String BOOKMARKS_COL_ARTICLE_TITLE = "article_title";
		private static final String BOOKMARKS_COL_LANG = "lang";
		private static final String BOOKMARKS_COL_IS_PART_OF = "is_part_of";
		private static final String BOOKMARKS_COL_IMAGE_TITLE = "image_title";
		private static final String BOOKMARKS_COL_PARTIAL_CONTENT = "partial_content";
		private static final String BOOKMARKS_COL_TRAVEL_BOOK = "travel_book";

		private static final String BOOKMARKS_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " +
				BOOKMARKS_TABLE_NAME + " (" +
				BOOKMARKS_COL_CITY_ID + " long, " +
				BOOKMARKS_COL_ARTICLE_TITLE + " TEXT, " +
				BOOKMARKS_COL_LANG + " TEXT, " +
				BOOKMARKS_COL_IS_PART_OF + " TEXT, " +
				BOOKMARKS_COL_IMAGE_TITLE + " TEXT, " +
				BOOKMARKS_COL_PARTIAL_CONTENT + " TEXT, " +
				BOOKMARKS_COL_TRAVEL_BOOK + " TEXT);";

		private static final String BOOKMARKS_TABLE_SELECT = "SELECT " +
				BOOKMARKS_COL_CITY_ID + ", " +
				BOOKMARKS_COL_ARTICLE_TITLE + ", " +
				BOOKMARKS_COL_LANG + ", " +
				BOOKMARKS_COL_IS_PART_OF + ", " +
				BOOKMARKS_COL_IMAGE_TITLE + ", " +
				BOOKMARKS_COL_PARTIAL_CONTENT +
				" FROM " + BOOKMARKS_TABLE_NAME;

		private final OsmandApplication context;

		WikivoyageLocalDataDbHelper(OsmandApplication context) {
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
			conn.execSQL(BOOKMARKS_TABLE_CREATE);
		}

		@SuppressWarnings("unused")
		private void onUpgrade(SQLiteConnection conn, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				conn.execSQL(BOOKMARKS_TABLE_CREATE);
			}
			if (oldVersion < 3) {
				conn.execSQL("ALTER TABLE " + HISTORY_TABLE_NAME + " ADD " + HISTORY_COL_TRAVEL_BOOK + " TEXT");
				conn.execSQL("ALTER TABLE " + BOOKMARKS_TABLE_NAME + " ADD " + BOOKMARKS_COL_TRAVEL_BOOK + " TEXT");
				Object[] args = new Object[]{context.getWikivoyageDbHelper().getSelectedTravelBook().getName()};
				conn.execSQL("UPDATE " + HISTORY_TABLE_NAME + " SET " + HISTORY_COL_TRAVEL_BOOK + " = ?", args);
				conn.execSQL("UPDATE " + BOOKMARKS_TABLE_NAME + " SET " + BOOKMARKS_COL_TRAVEL_BOOK + " = ?", args);
			}
		}

		@NonNull
		TLongObjectHashMap<WikivoyageSearchHistoryItem> getAllHistoryMap() {
			TLongObjectHashMap<WikivoyageSearchHistoryItem> res = new TLongObjectHashMap<>();
			SQLiteConnection conn = openConnection(true);
			if (conn != null) {
				try {
					String query = HISTORY_TABLE_SELECT + " WHERE " + HISTORY_COL_TRAVEL_BOOK + " = ?";
					String travelBook = context.getSettings().SELECTED_TRAVEL_BOOK.get();
					SQLiteCursor cursor = conn.rawQuery(query, new String[]{travelBook});
					if (cursor.moveToFirst()) {
						do {
							WikivoyageSearchHistoryItem item = readHistoryItem(cursor);
							res.put(item.cityId, item);
						} while (cursor.moveToNext());
					}
				} finally {
					conn.close();
				}
			}
			return res;
		}

		void addHistoryItem(WikivoyageSearchHistoryItem item) {
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					String travelBook = context.getSettings().SELECTED_TRAVEL_BOOK.get();
					conn.execSQL("INSERT INTO " + HISTORY_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?)",
							new Object[]{item.cityId, item.articleTitle, item.lang,
									item.isPartOf, item.lastAccessed, travelBook});
				} finally {
					conn.close();
				}
			}
		}

		void updateHistoryItem(WikivoyageSearchHistoryItem item) {
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					String travelBook = context.getSettings().SELECTED_TRAVEL_BOOK.get();
					conn.execSQL("UPDATE " + HISTORY_TABLE_NAME + " SET " +
									HISTORY_COL_ARTICLE_TITLE + " = ?, " +
									HISTORY_COL_LANG + " = ?, " +
									HISTORY_COL_IS_PART_OF + " = ?, " +
									HISTORY_COL_LAST_ACCESSED + " = ? " +
									"WHERE " + HISTORY_COL_CITY_ID + " = ? " +
									"AND " + HISTORY_COL_TRAVEL_BOOK + " = ?",
							new Object[]{item.articleTitle, item.lang, item.isPartOf,
									item.lastAccessed, item.cityId, travelBook});
				} finally {
					conn.close();
				}
			}
		}

		void removeHistoryItem(WikivoyageSearchHistoryItem item) {
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					String travelBook = context.getSettings().SELECTED_TRAVEL_BOOK.get();
					conn.execSQL("DELETE FROM " + HISTORY_TABLE_NAME +
									" WHERE " + HISTORY_COL_CITY_ID + " = ?" +
									" AND " + HISTORY_COL_TRAVEL_BOOK + " = ?",
							new Object[]{item.cityId, travelBook});
				} finally {
					conn.close();
				}
			}
		}

		void clearAllHistory() {
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					String travelBook = context.getSettings().SELECTED_TRAVEL_BOOK.get();
					conn.execSQL("DELETE FROM " + HISTORY_TABLE_NAME +
									" WHERE " + HISTORY_COL_TRAVEL_BOOK + " = ?",
							new Object[]{travelBook});
				} finally {
					conn.close();
				}
			}
		}

		@NonNull
		List<WikivoyageArticle> getSavedArticles() {
			List<WikivoyageArticle> res = new ArrayList<>();
			SQLiteConnection conn = openConnection(true);
			if (conn != null) {
				try {
					String query = BOOKMARKS_TABLE_SELECT + " WHERE " + BOOKMARKS_COL_TRAVEL_BOOK + " = ?";
					String travelBook = context.getSettings().SELECTED_TRAVEL_BOOK.get();
					SQLiteCursor cursor = conn.rawQuery(query, new String[]{travelBook});
					if (cursor.moveToFirst()) {
						do {
							res.add(readSavedArticle(cursor));
						} while (cursor.moveToNext());
					}
					cursor.close();
				} finally {
					conn.close();
				}
			}
			return res;
		}

		void addSavedArticle(WikivoyageArticle article) {
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					String travelBook = context.getSettings().SELECTED_TRAVEL_BOOK.get();
					conn.execSQL("INSERT INTO " + BOOKMARKS_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?)",
							new Object[]{article.cityId, article.title, article.lang,
									article.aggregatedPartOf, article.imageTitle, article.content, travelBook});
				} finally {
					conn.close();
				}
			}
		}

		void removeSavedArticle(WikivoyageArticle article) {
			SQLiteConnection conn = openConnection(false);
			if (conn != null) {
				try {
					String travelBook = context.getSettings().SELECTED_TRAVEL_BOOK.get();
					conn.execSQL("DELETE FROM " + BOOKMARKS_TABLE_NAME +
									" WHERE " + BOOKMARKS_COL_CITY_ID + " = ?" +
									" AND " + BOOKMARKS_COL_LANG + " = ?" +
									" AND " + BOOKMARKS_COL_TRAVEL_BOOK + " = ?",
							new Object[]{article.cityId, article.lang, travelBook});
				} finally {
					conn.close();
				}
			}
		}

		private WikivoyageSearchHistoryItem readHistoryItem(SQLiteCursor cursor) {
			WikivoyageSearchHistoryItem res = new WikivoyageSearchHistoryItem();

			res.cityId = cursor.getLong(0);
			res.articleTitle = cursor.getString(1);
			res.lang = cursor.getString(2);
			res.isPartOf = cursor.getString(3);
			res.lastAccessed = cursor.getLong(4);

			return res;
		}

		private WikivoyageArticle readSavedArticle(SQLiteCursor cursor) {
			WikivoyageArticle res = new WikivoyageArticle();

			res.cityId = cursor.getLong(0);
			res.title = cursor.getString(1);
			res.lang = cursor.getString(2);
			res.aggregatedPartOf = cursor.getString(3);
			res.imageTitle = cursor.getString(4);
			res.content = cursor.getString(5);

			return res;
		}
	}
}
