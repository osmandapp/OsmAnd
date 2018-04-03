package net.osmand.plus.wikivoyage.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gnu.trove.map.hash.TLongObjectHashMap;

public class WikivoyageDbHelper {

	private static final String DB_NAME = "wikivoyage.sqlite";

	private static final String ARTICLES_TABLE_NAME = "wikivoyage_articles";
	private static final String ARTICLES_COL_ID = "article_id";
	private static final String ARTICLES_COL_TITLE = "title";
	private static final String ARTICLES_COL_CONTENT = "content_gz";
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

	private static final String SEARCH_QUERY = "SELECT " +
			SEARCH_COL_SEARCH_TERM + ", " +
			SEARCH_TABLE_NAME + "." + SEARCH_COL_CITY_ID + ", " +
			SEARCH_COL_ARTICLE_TITLE + ", " +
			SEARCH_TABLE_NAME + "." + SEARCH_COL_LANG + ", " +
			ARTICLES_COL_IS_PART_OF + ", " +
			ARTICLES_COL_IMAGE_TITLE +
			" FROM " + SEARCH_TABLE_NAME +
			" JOIN " + ARTICLES_TABLE_NAME +
			" ON " + SEARCH_TABLE_NAME + "." + SEARCH_COL_ARTICLE_TITLE + " = " + ARTICLES_TABLE_NAME + "." + ARTICLES_COL_TITLE +
			" AND " + SEARCH_TABLE_NAME + "." + SEARCH_COL_LANG + " = " + ARTICLES_TABLE_NAME + "." + ARTICLES_COL_LANG +
			" WHERE " + SEARCH_TABLE_NAME + "." + SEARCH_COL_CITY_ID +
			" IN (SELECT " + SEARCH_TABLE_NAME + "." + SEARCH_COL_CITY_ID +
			" FROM " + SEARCH_TABLE_NAME +
			" WHERE " + SEARCH_COL_SEARCH_TERM + " LIKE ?)";

	private final OsmandApplication application;

	private Collator collator;

	public WikivoyageDbHelper(OsmandApplication application) {
		this.application = application;
		collator = OsmAndCollator.primaryCollator();
	}

	@NonNull
	public List<WikivoyageSearchResult> search(final String searchQuery) {
		List<WikivoyageSearchResult> res = new ArrayList<>();
		SQLiteConnection conn = openConnection();
		if (conn != null) {
			try {
				SQLiteCursor cursor = conn.rawQuery(SEARCH_QUERY, new String[]{searchQuery + "%"});
				if (cursor.moveToFirst()) {
					do {
						res.add(readSearchResult(cursor));
					} while (cursor.moveToNext());
				}
			} finally {
				conn.close();
			}
		}

		List<WikivoyageSearchResult> list = new ArrayList<>(groupSearchResultsByCityId(res));
		Collections.sort(list, new Comparator<WikivoyageSearchResult>() {
			@Override
			public int compare(WikivoyageSearchResult o1, WikivoyageSearchResult o2) {
				boolean c1 = CollatorStringMatcher.cmatches(collator, searchQuery, o1.articleTitles.get(0),
						StringMatcherMode.CHECK_ONLY_STARTS_WITH);
				boolean c2 = CollatorStringMatcher.cmatches(collator, searchQuery, o2.articleTitles.get(0),
						StringMatcherMode.CHECK_ONLY_STARTS_WITH);
				if (c1 == c2) {
					return collator.compare(o1.articleTitles.get(0), o2.articleTitles.get(0));
				} else if (c1) {
					return -1;
				} else if (c2) {
					return 1;
				}
				return 0;
			}
		});

		return list;
	}

	private Collection<WikivoyageSearchResult> groupSearchResultsByCityId(List<WikivoyageSearchResult> res) {
		String baseLng = application.getLanguage();
		TLongObjectHashMap<WikivoyageSearchResult> wikivoyage = new TLongObjectHashMap<>();
		for (WikivoyageSearchResult rs : res) {
			WikivoyageSearchResult prev = wikivoyage.get(rs.cityId);
			if (prev != null) {
				int insInd = prev.langs.size();
				if (rs.langs.get(0).equals(baseLng)) {
					insInd = 0;
				} else if (rs.langs.get(0).equals("en")) {
					if (!prev.langs.get(0).equals(baseLng)) {
						insInd = 0;
					} else {
						insInd = 1;
					}
				}
				prev.articleTitles.add(insInd, rs.articleTitles.get(0));
				prev.langs.add(insInd, rs.langs.get(0));
				prev.searchTerms.add(insInd, rs.searchTerms.get(0));
			} else {
				wikivoyage.put(rs.cityId, rs);
			}
		}
		return wikivoyage.valueCollection();
	}

	@Nullable
	public WikivoyageArticle getArticle(long cityId, String lang) {
		WikivoyageArticle res = null;
		SQLiteConnection conn = openConnection();
		if (conn != null) {
			try {
				SQLiteCursor cursor = conn.rawQuery(ARTICLES_TABLE_SELECT + " WHERE " +
								ARTICLES_COL_CITY_ID + " = ? AND " +
								ARTICLES_COL_LANG + " = ?",
						new String[]{String.valueOf(cityId), lang});
				if (cursor.moveToFirst()) {
					res = readArticle(cursor);
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
		String path = getDbFile(application).getAbsolutePath();
		return application.getSQLiteAPI().openByAbsolutePath(path, true);
	}

	@NonNull
	private WikivoyageSearchResult readSearchResult(SQLiteCursor cursor) {
		WikivoyageSearchResult res = new WikivoyageSearchResult();

		res.searchTerms.add(cursor.getString(0));
		res.cityId = cursor.getLong(1);
		res.articleTitles.add(cursor.getString(2));
		res.langs.add(cursor.getString(3));
		res.isPartOf = cursor.getString(4);
		res.imageTitle = cursor.getString(5);

		return res;
	}

	@NonNull
	private WikivoyageArticle readArticle(SQLiteCursor cursor) {
		WikivoyageArticle res = new WikivoyageArticle();

		res.id = cursor.getString(0);
		res.title = cursor.getString(1);
		try {
			res.content = Algorithms.gzipToString(cursor.getBlob(2));
		} catch (IOException e) {
			e.printStackTrace();
		}
		res.isPartOf = cursor.getString(3);
		res.lat = cursor.getDouble(4);
		res.lon = cursor.getDouble(5);
		res.imageTitle = cursor.getString(6);
		byte[] gpxFileBlob = cursor.getBlob(7);
		res.cityId = cursor.getLong(8);
		res.originalId = cursor.getLong(9);
		res.lang = cursor.getString(10);

		return res;
	}

	public static boolean isDbFileExists(OsmandApplication app) {
		return getDbFile(app).exists();
	}

	@NonNull
	private static File getDbFile(OsmandApplication app) {
		return app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR + DB_NAME);
	}
}
