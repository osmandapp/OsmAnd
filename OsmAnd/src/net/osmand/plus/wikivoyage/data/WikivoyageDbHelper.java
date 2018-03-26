package net.osmand.plus.wikivoyage.data;

import android.support.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;

import java.util.ArrayList;
import java.util.List;

public class WikivoyageDbHelper {

	private static final String DB_NAME = "wikivoyage.sqlite";

	private static final String ARTICLES_TABLE_NAME = "wikivoyage_articles";
	private static final String ARTICLES_COL_ID = "article_id";
	private static final String ARTICLES_COL_TITLE = "title";
	private static final String ARTICLES_COL_CONTENT = "content_gzblob";
	private static final String ARTICLES_COL_IS_PART_OF = "is_part_of";
	private static final String ARTICLES_COL_LAT = "lat";
	private static final String ARTICLES_COL_LON = "lon";
	private static final String ARTICLES_COL_IMAGE_TITLE = "image_title";
	private static final String ARTICLES_COL_GPX_GZ = "gpx_gz";
	private static final String ARTICLES_COL_CITY_ID = "city_id";
	private static final String ARTICLES_COL_ORIGINAL_ID = "original_id";
	private static final String ARTICLES_COL_LANG = "lang";

	private static final String ARTICLES_TABLE_SELECT = "SELECT " +
			ARTICLES_COL_ID + ", " +
			ARTICLES_COL_TITLE + ", " +
			ARTICLES_COL_CONTENT + ", " +
			ARTICLES_COL_IS_PART_OF + ", " +
			ARTICLES_COL_LAT + ", " +
			ARTICLES_COL_LON + ", " +
			ARTICLES_COL_IMAGE_TITLE + ", " +
			ARTICLES_COL_GPX_GZ + ", " +
			ARTICLES_COL_CITY_ID + ", " +
			ARTICLES_COL_ORIGINAL_ID + ", " +
			ARTICLES_COL_LANG +
			" FROM " + ARTICLES_TABLE_NAME;

	private static final String SEARCH_TABLE_NAME = "wikivoyage_search";
	private static final String SEARCH_COL_SEARCH_TERM = "search_term";
	private static final String SEARCH_COL_CITY_ID = "city_id";
	private static final String SEARCH_COL_ARTICLE_TITLE = "article_title";
	private static final String SEARCH_COL_LANG = "lang";

	private static final String SEARCH_TABLE_SELECT = "SELECT " +
			SEARCH_COL_SEARCH_TERM + ", " +
			SEARCH_COL_CITY_ID + ", " +
			SEARCH_COL_ARTICLE_TITLE + ", " +
			SEARCH_COL_LANG +
			" FROM " + SEARCH_TABLE_NAME;

	private final OsmandApplication application;

	public WikivoyageDbHelper(OsmandApplication application) {
		this.application = application;
	}

	public List<SearchResult> search(String searchQuery) {
		List<SearchResult> res = new ArrayList<>();
		SQLiteConnection conn = openConnection();
		if (conn != null) {
			try {
				String dbQuery = SEARCH_TABLE_SELECT + " WHERE " + SEARCH_COL_SEARCH_TERM + " LIKE " + "'%" + searchQuery + "%'";
				SQLiteCursor cursor = conn.rawQuery(dbQuery, null);
				if (cursor.moveToFirst()) {
					do {
						res.add(readSearchResult(cursor));
					} while (cursor.moveToNext());
				}
				cursor.close();
			} finally {
				conn.close();
			}
		}
		return res;
	}

	@Nullable
	private SQLiteConnection openConnection() {
		String path = application.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR + DB_NAME).getAbsolutePath();
		return application.getSQLiteAPI().openByAbsolutePath(path, true);
	}

	private SearchResult readSearchResult(SQLiteCursor query) {
		SearchResult res = new SearchResult();

		res.searchTerm = query.getString(0);
		res.cityId = query.getLong(1);
		res.articleTitle = query.getString(2);
		res.lang = query.getString(3);

		return res;
	}

	public static class SearchResult {

		private String searchTerm;
		private long cityId;
		private String articleTitle;
		private String lang;

		public String getSearchTerm() {
			return searchTerm;
		}

		public long getCityId() {
			return cityId;
		}

		public String getArticleTitle() {
			return articleTitle;
		}

		public String getLang() {
			return lang;
		}
	}
}
