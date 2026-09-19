package net.osmand.plus.wikivoyage.data;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class TravelDbHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelDbHelper.class);

	private static final String WORLD_WIKIVOYAGE_FILE_NAME = "World_wikivoyage.sqlite";
	private static final String ARTICLES_TABLE_NAME = "travel_articles";
	private static final String POPULAR_TABLE_NAME = "popular_articles";
	private static final String ARTICLES_POP_INDEX = "popularity_index";
	private static final String ARTICLES_POP_ORDER = "order_index";
	private static final String ARTICLES_COL_TITLE = "title";
	private static final String ARTICLES_COL_CONTENT = "content_gz";
	private static final String ARTICLES_COL_IS_PART_OF = "is_part_of";
	private static final String ARTICLES_COL_LAT = "lat";
	private static final String ARTICLES_COL_LON = "lon";
	private static final String ARTICLES_COL_IMAGE_TITLE = "image_title";
	private static final String ARTICLES_COL_GPX_GZ = "gpx_gz";
	private static final String ARTICLES_COL_TRIP_ID = "trip_id";
	private static final String ARTICLES_COL_ORIGINAL_ID = "original_id";
	private static final String ARTICLES_COL_LANG = "lang";
	private static final String ARTICLES_COL_CONTENTS_JSON = "contents_json";
	private static final String ARTICLES_COL_AGGREGATED_PART_OF = "aggregated_part_of";

	private static final String ARTICLES_TABLE_SELECT = "SELECT " +
			ARTICLES_COL_TITLE + ", " +
			ARTICLES_COL_CONTENT + ", " +
			ARTICLES_COL_IS_PART_OF + ", " +
			ARTICLES_COL_LAT + ", " +
			ARTICLES_COL_LON + ", " +
			ARTICLES_COL_IMAGE_TITLE + ", " +
			ARTICLES_COL_GPX_GZ + ", " +
			ARTICLES_COL_TRIP_ID + ", " +
			ARTICLES_COL_ORIGINAL_ID + ", " +
			ARTICLES_COL_LANG + ", " +
			ARTICLES_COL_CONTENTS_JSON + ", " +
			ARTICLES_COL_AGGREGATED_PART_OF +
			" FROM " + ARTICLES_TABLE_NAME;
	
	private static final String POP_ARTICLES_TABLE_SELECT = "SELECT " +
			ARTICLES_COL_TITLE + ", " +
			ARTICLES_COL_LAT + ", " +
			ARTICLES_COL_LON + ", " +
			ARTICLES_COL_TRIP_ID + ", " +
			ARTICLES_COL_LANG + ", " +
			ARTICLES_POP_ORDER + ", " +
			ARTICLES_POP_INDEX +
			" FROM " + POPULAR_TABLE_NAME;

	private static final String SEARCH_TABLE_NAME = "travel_search";
	private static final String SEARCH_COL_SEARCH_TERM = "search_term";
	private static final String SEARCH_COL_trip_id = "trip_id";
	private static final String SEARCH_COL_ARTICLE_TITLE = "article_title";
	private static final String SEARCH_COL_LANG = "lang";

	private static final int POPULAR_LIMIT = 25;

	private final OsmandApplication application;

	private final TravelLocalDataHelper localDataHelper;
	private final Collator collator;

	private SQLiteConnection connection;

	private File selectedTravelBook;
	private final List<File> existingTravelBooks = new ArrayList<>();
	private List<TravelArticle> popularArticles = new ArrayList<>();
	
	
	public TravelDbHelper(OsmandApplication application) {
		this.application = application;
		collator = OsmAndCollator.primaryCollator();
		localDataHelper = new TravelLocalDataHelper(application);
	}

	public static boolean checkIfDbFileExists(OsmandApplication app) {
		File[] files = app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.getName().endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public TravelLocalDataHelper getBookmarksHelper() {
		return localDataHelper;
	}

	@Override
	public boolean isAnyTravelBookPresent() {
		return selectedTravelBook != null;
	}

	@Override
	public void initializeDataOnAppStartup() {
		List<File> files = getPossibleFiles();
		String travelBook = application.getSettings().SELECTED_TRAVEL_BOOK.get();
		existingTravelBooks.clear();
		if (files != null && !files.isEmpty()) {
			for (File f : files) {
				existingTravelBooks.add(f);
				if (selectedTravelBook == null) {
					selectedTravelBook = f;
				} else if (Algorithms.objectEquals(travelBook, f.getName())) {
					selectedTravelBook = f;
				}
			}
		} else {
			selectedTravelBook = null;
		}
	}

	@Nullable
	private List<File> getPossibleFiles() {
		File[] files = application.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).listFiles();
		if (files != null) {
			List<File> res = new ArrayList<>();
			for (File file : files) {
				if (file.getName().endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT)) {
					res.add(file);
				}
			}
			return res;
		}
		return null;
	}

	@Override
	public void initializeDataToDisplay(boolean resetData) {
		localDataHelper.refreshCachedData();
		loadPopularArticles();
	}


	@Override
	public String getSelectedTravelBookName() {
		if (selectedTravelBook != null) {
			return selectedTravelBook.getName();
		}
		return null;
	}

	@Override
	public String getWikivoyageFileName() {
		return WORLD_WIKIVOYAGE_FILE_NAME;
	}

	@Override
	public void saveOrRemoveArticle(@NonNull TravelArticle article, boolean save) {
		if (save) {
			localDataHelper.addArticleToSaved(article);
		} else {
			localDataHelper.removeArticleFromSaved(article);
		}
	}


	public List<File> getExistingTravelBooks() {
		return existingTravelBooks;
	}

	@Nullable
	private SQLiteConnection openConnection() {
		if (connection == null && selectedTravelBook != null) {
			application.getSettings().SELECTED_TRAVEL_BOOK.set(selectedTravelBook.getName());
			connection = application.getSQLiteAPI().openByAbsolutePath(selectedTravelBook.getAbsolutePath(), true);
		}
		return connection;
	}

	public void selectTravelBook(File f) {
		closeConnection();
		if (f.exists()) {
			connection = application.getSQLiteAPI().openByAbsolutePath(f.getAbsolutePath(), true);
			selectedTravelBook = f;
			application.getSettings().SELECTED_TRAVEL_BOOK.set(selectedTravelBook.getName());
		}
	}

	private void closeConnection() {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}

	@Override
	@NonNull
	public List<WikivoyageSearchResult> search(@NonNull String searchQuery, int reqNumber) {
		List<WikivoyageSearchResult> res = new ArrayList<>();
		SQLiteConnection conn = openConnection();
		String[] queries = searchQuery.replace('_', ' ').replace('/', ' ').split(" ");
		if (conn != null) {
			List<String> params = new ArrayList<>();
			String query = "SELECT  distinct wa.trip_id, wa.title, wa.lang, wa.is_part_of, wa.image_title "
					+ "FROM travel_articles wa WHERE wa.trip_id in "
					+ " (SELECT trip_id FROM travel_search WHERE search_term LIKE";
			for (String q : queries) {
				if (q.trim().length() > 0) {
					if (params.size() > 5) {
						// don't explode the query search much
						break;
					}
					if (params.size() > 0) {
						query += " AND trip_id IN (SELECT trip_id FROM travel_search WHERE search_term LIKE ?) ";
					} else {
						query += "?";
					}
					params.add(q.trim() + "%");
				}
			}
			query += ") ";
			if (params.size() > 0) {
				SQLiteCursor cursor = conn.rawQuery(query, params.toArray(new String[0]));
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						do {
							String routeId = cursor.getLong(0) + "";
							String articleTitle = cursor.getString(1);
							String lang = cursor.getString(2);
							String isPartOf = cursor.getString(3);
							String imageTitle = cursor.getString(4);
							List<String> langs = new ArrayList<>();
							langs.add(lang);
							WikivoyageSearchResult rs = new WikivoyageSearchResult(routeId, articleTitle,
									isPartOf, imageTitle, langs);
							res.add(rs);
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
			}
		}

		List<WikivoyageSearchResult> list = new ArrayList<>(groupSearchResultsByRouteId(res));
		sortSearchResults(searchQuery, list);

		return list;
	}

	@Override
	@NonNull
	public List<TravelArticle> getPopularArticles() {
		return popularArticles;
	}

	@NonNull
	public List<TravelArticle> loadPopularArticles() {
		String language = application.getLanguage();
		SQLiteConnection conn = openConnection();
		if (conn == null) {
			popularArticles = new ArrayList<>();
			return popularArticles;
		}
		String LANG_WHERE = " WHERE " + ARTICLES_COL_LANG + " = '" + language + "'";
		SQLiteCursor cursor = conn.rawQuery(POP_ARTICLES_TABLE_SELECT + LANG_WHERE, null);
		if (cursor == null) {
			return popularArticles;
		}
		// read popular articles
		List<PopularArticle> popReadArticlesOrder = new ArrayList<>();
		List<PopularArticle> popReadArticlesLocation = new ArrayList<>();
		List<PopularArticle> popReadArticles = new ArrayList<>();
		if (cursor.moveToFirst()) {
			do {
				PopularArticle travelArticle = PopularArticle.readArticle(cursor);
				if (language.equals(travelArticle.lang)) {
					if(travelArticle.order != -1) {
						popReadArticlesOrder.add(travelArticle);
					} if(travelArticle.isLocationSpecified()) {
						popReadArticlesLocation.add(travelArticle);
					} else {
						popReadArticles.add(travelArticle);
					}
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		// shuffle, sort & mix
		Random rm = new Random();
		Collections.shuffle(popReadArticles, rm);
		Collections.sort(popReadArticlesOrder, new Comparator<PopularArticle>() {
			@Override
			public int compare(PopularArticle article1, PopularArticle article2) {
				return Integer.compare(article1.order, article2.order);
			}
		});
		sortPopArticlesByDistance(popReadArticlesLocation);
		List<String> resArticleOrder = new ArrayList<String>();
		Iterator<PopularArticle> orderIterator = popReadArticlesOrder.iterator();
		Iterator<PopularArticle> locIterator = popReadArticlesLocation.iterator();
		Iterator<PopularArticle> otherIterator = popReadArticles.iterator();
		int initialLocationArticles = 2;
		for (int i = 0; i < POPULAR_LIMIT; i++) {
			PopularArticle pa = null;
			if(orderIterator.hasNext()) {
				pa = orderIterator.next();
			} else if(initialLocationArticles-- > 0 && locIterator.hasNext()) {
				// first 2 by location
				pa = locIterator.next();
			} else if((!otherIterator.hasNext() || (rm.nextDouble() > 0.4)) && locIterator.hasNext()) {
				// 60% case we select location iterator
				pa = locIterator.next();
			} else if(otherIterator.hasNext()){
				pa = otherIterator.next();
			}
			if (pa == null) {
				break;
			} else {
				resArticleOrder.add(pa.tripId);
			}
		}
		
		
		Map<String, TravelArticle> ts = readTravelArticles(conn, LANG_WHERE, resArticleOrder);
		popularArticles = sortArticlesToInitialOrder(resArticleOrder, ts);
		return popularArticles;
	}

	private Map<String, TravelArticle> readTravelArticles(SQLiteConnection conn, String whereCondition,
			List<String> articleIds) {
		SQLiteCursor cursor;
		StringBuilder bld = new StringBuilder();
		bld.append(ARTICLES_TABLE_SELECT).append(whereCondition)
				.append(" and ").append(ARTICLES_COL_TRIP_ID).append(" IN (");
		for (int i = 0; i < articleIds.size(); i++) {
			if (i > 0) {
				bld.append(", ");
			}
			bld.append(articleIds.get(i));
		}
		bld.append(")");
		cursor = conn.rawQuery(bld.toString(), null);
		Map<String, TravelArticle> ts = new HashMap<String, TravelArticle>();
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					TravelArticle travelArticle = readArticle(cursor);
					ts.put(travelArticle.routeId, travelArticle);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		return ts;
	}

	private List<TravelArticle> sortArticlesToInitialOrder(List<String> resArticleOrder, Map<String, TravelArticle> ts) {
		List<TravelArticle> res = new ArrayList<>();
		for (int i = 0; i < resArticleOrder.size(); i++) {
			TravelArticle ta = ts.get(resArticleOrder.get(i));
			if(ta != null) {
				res.add(ta);
			}
		}
		return res;
	}

	private void sortSearchResults(@NonNull String searchQuery, @NonNull List<WikivoyageSearchResult> list) {
		Collections.sort(list, new Comparator<WikivoyageSearchResult>() {
			@Override
			public int compare(WikivoyageSearchResult o1, WikivoyageSearchResult o2) {
				boolean c1 = CollatorStringMatcher.cmatches(collator, searchQuery, o1.getArticleTitle(),
						StringMatcherMode.CHECK_ONLY_STARTS_WITH);
				boolean c2 = CollatorStringMatcher.cmatches(collator, searchQuery, o2.getArticleTitle(),
						StringMatcherMode.CHECK_ONLY_STARTS_WITH);
				if (c1 == c2) {
					return collator.compare(o1.getArticleTitle(), o2.getArticleTitle());
				} else if (c1) {
					return -1;
				} else if (c2) {
					return 1;
				}
				return 0;
			}
		});
	}

	
	private void sortPopArticlesByDistance(List<PopularArticle> list) {
		Location location = application.getLocationProvider().getLastKnownLocation();
		LatLon loc ;
		if(location == null) {
			loc = application.getSettings().getLastKnownMapLocation();
		} else {
			loc = new LatLon(location.getLatitude(), location.getLongitude());
		}
		if (loc != null) {
			Collections.sort(list, new Comparator<PopularArticle>() {
				@Override
				public int compare(PopularArticle article1, PopularArticle article2) {
					return Double.compare(MapUtils.getDistance(loc, article1.lat, article1.lon), 
							MapUtils.getDistance(loc, article2.lat, article2.lon));
				}
			});
		}
	}

	private Collection<WikivoyageSearchResult> groupSearchResultsByRouteId(List<WikivoyageSearchResult> res) {
		String baseLng = application.getLanguage();
		Map<String, WikivoyageSearchResult> wikivoyage = new HashMap<>();
		for (WikivoyageSearchResult rs : res) {
			WikivoyageSearchResult prev = wikivoyage.get(rs.getArticleRouteId());
			if (prev != null) {
				boolean matchLang = false;
				if (rs.langs.get(0).equals(baseLng)) {
					matchLang = true;
				} else if (rs.langs.get(0).equals("en")) {
					if (!prev.langs.get(0).equals(baseLng)) {
						matchLang = true;
					}
				}
				if (matchLang) {
					prev.articleId.title = rs.getArticleTitle();
					prev.isPartOf = rs.getIsPartOf();
				}
				prev.langs.add(matchLang ? 0 : 1, rs.langs.get(0));
			} else {
				wikivoyage.put(rs.getArticleRouteId(), rs);
			}
		}
		return wikivoyage.values();
	}

	@NonNull
	@Override
	public Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> getNavigationMap(@NonNull TravelArticle article) {
		String lang = article.getLang();
		String title = article.getTitle();
		if (TextUtils.isEmpty(lang) || TextUtils.isEmpty(title)) {
			return Collections.emptyMap();
		}
		String[] parts = null;
		if (!TextUtils.isEmpty(article.getAggregatedPartOf())) {
			String[] originalParts = article.getAggregatedPartOf().split(",");
			if (originalParts.length > 1) {
				parts = new String[originalParts.length];
				for (int i = 0; i < originalParts.length; i++) {
					parts[i] = originalParts[originalParts.length - i - 1];
				}
			} else {
				parts = originalParts;
			}
		}
		Map<String, List<WikivoyageSearchResult>> navMap = new HashMap<>();
		SQLiteConnection conn = openConnection();
		Set<String> headers = new LinkedHashSet<String>();
		Map<String, WikivoyageSearchResult> headerObjs = new HashMap<>();
		if (conn != null) {
			List<String> params = new ArrayList<>();
			StringBuilder query = new StringBuilder("SELECT a.trip_id, a.title, a.lang, a.is_part_of "
					+ "FROM travel_articles a WHERE is_part_of = ? and lang = ? ");
			params.add(title);
			params.add(lang);
			
			if (parts != null && parts.length > 0) {
				headers.addAll(Arrays.asList(parts));
				headers.add(title);
				query.append("UNION SELECT a.trip_id, a.title, a.lang, a.is_part_of "
						+ "FROM travel_articles a WHERE title = ? and lang = ? ");
				params.add(parts[0]);
				params.add(lang);
				for (String part : parts) {
					query.append("UNION SELECT a.trip_id, a.title, a.lang, a.is_part_of "
							+ "FROM travel_articles a WHERE is_part_of = ? and lang = ? ");
					params.add(part);
					params.add(lang);
				}
			}
			SQLiteCursor cursor = conn.rawQuery(query.toString(), params.toArray(new String[0]));
			if (cursor != null && cursor.moveToFirst()) {
				do {
					String routeId = cursor.getLong(0) + "";
					String articleTitle = cursor.getString(1);
					String articleLang = cursor.getString(2);
					String isPartOf = cursor.getString(3);
					WikivoyageSearchResult rs = new WikivoyageSearchResult(routeId, articleTitle,
							isPartOf, null, Collections.singletonList(articleLang));
					List<WikivoyageSearchResult> l = navMap.get(rs.isPartOf);
					if (l == null) {
						l = new ArrayList<>();
						navMap.put(rs.isPartOf, l);
					}
					l.add(rs);
					if (headers != null && headers.contains(articleTitle)) {
						headerObjs.put(articleTitle, rs);
					}
				} while (cursor.moveToNext());
			}
			if (cursor != null) {
				cursor.close();
			}
		}
		LinkedHashMap<WikivoyageSearchResult, List<WikivoyageSearchResult>> res = new LinkedHashMap<>();
		for (String header : headers) {
			WikivoyageSearchResult searchResult = headerObjs.get(header);
			List<WikivoyageSearchResult> results = navMap.get(header);
			if (results != null) {
				Collections.sort(results, new Comparator<WikivoyageSearchResult>() {
					@Override
					public int compare(WikivoyageSearchResult o1, WikivoyageSearchResult o2) {
						return collator.compare(o1.getArticleTitle(), o2.getArticleTitle());
					}
				});
				WikivoyageSearchResult emptyResult = new WikivoyageSearchResult("", header, null, null, null);
				searchResult = searchResult != null ? searchResult : emptyResult;
				res.put(searchResult, results);
			}
		}
		return res;
	}

	@Override
	@Nullable
	public TravelArticle getArticleById(@NonNull TravelArticleIdentifier articleId, @Nullable String lang, boolean readGpx, @Nullable GpxReadCallback callback) {
		TravelArticle res = null;
		SQLiteConnection conn = openConnection();
		String routeId = articleId.routeId;
		if (conn != null && !Algorithms.isEmpty(routeId) && lang != null) {
			SQLiteCursor cursor = conn.rawQuery(ARTICLES_TABLE_SELECT + " WHERE " + ARTICLES_COL_TRIP_ID + " = ? AND "
					+ ARTICLES_COL_LANG + " = ?", new String[]{routeId, lang});
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					res = readArticle(cursor);
				}
				cursor.close();
			}
		}
		if (res == null) {
			res = localDataHelper.getSavedArticle(articleId.file, articleId.routeId, lang);
		}
		if (res != null && callback != null) {
			callback.onGpxFileRead(res.gpxFile);
		}
		return res;
	}

	@Nullable
	@Override
	public TravelArticle getArticleByTitle(@NonNull String title, @NonNull String lang, boolean readGpx, @Nullable GpxReadCallback callback) {
		return getArticleByTitle(title, new QuadRect(), lang, readGpx, callback);
	}

	@Nullable
	@Override
	public TravelArticle getArticleByTitle(@NonNull String title, @NonNull LatLon latLon, @NonNull String lang, boolean readGpx, @Nullable GpxReadCallback callback) {
		return getArticleByTitle(title, new QuadRect(), lang, readGpx, callback);
	}

	@Nullable
	@Override
	public TravelArticle getArticleByTitle(@NonNull String title, @NonNull QuadRect rect, @NonNull String lang, boolean readGpx, @Nullable GpxReadCallback callback) {
		TravelArticle res = null;
		SQLiteConnection conn = openConnection();
		if (conn != null) {
			SQLiteCursor cursor = conn.rawQuery(ARTICLES_TABLE_SELECT + " WHERE " + ARTICLES_COL_TITLE + " = ? AND "
					+ ARTICLES_COL_LANG + " = ?", new String[]{title, lang});
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					res = readArticle(cursor);
				}
				cursor.close();
			}
		}
		if (res != null && callback != null) {
			callback.onGpxFileRead(res.gpxFile);
		}
		return res;
	}

	@Override
	public TravelArticle findSavedArticle(@NonNull TravelArticle savedArticle) {
		return savedArticle;
	}

	@Nullable
	@Override
	public TravelArticleIdentifier getArticleId(@NonNull String title, @NonNull String lang) {
		TravelArticle article = null;
		SQLiteConnection conn = openConnection();
		if (conn != null) {
			SQLiteCursor cursor = conn.rawQuery(ARTICLES_TABLE_SELECT + " WHERE " + ARTICLES_COL_TITLE + " = ? AND "
					+ ARTICLES_COL_LANG + " = ?", new String[]{title, lang});
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					article = readArticle(cursor);
				}
				cursor.close();
			}
		}
		return article != null ? article.generateIdentifier() : null;
	}

	@NonNull
	@Override
	public ArrayList<String> getArticleLangs(@NonNull TravelArticleIdentifier articleId) {
		ArrayList<String> res = new ArrayList<>();
		SQLiteConnection conn = openConnection();
		if (conn != null) {
			SQLiteCursor cursor = conn.rawQuery("SELECT " + ARTICLES_COL_LANG + " FROM " + ARTICLES_TABLE_NAME
					+ " WHERE " + ARTICLES_COL_TRIP_ID + " = ?", new String[]{articleId.routeId});
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					String baseLang = application.getLanguage();
					do {
						String lang = cursor.getString(0);
						if (lang.equals(baseLang)) {
							res.add(0, lang);
						} else if (lang.equals("en")) {
							if (res.size() > 0 && res.get(0).equals(baseLang)) {
								res.add(1, lang);
							} else {
								res.add(0, lang);
							}
						} else {
							res.add(lang);
						}
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
		}
		if (res.isEmpty()) {
			List<TravelArticle> articles = localDataHelper.getSavedArticles(articleId.file, articleId.routeId);
			for (TravelArticle a : articles) {
				res.add(a.getLang());
			}
		}
		return res;
	}

	@NonNull
	private TravelArticle readArticle(SQLiteCursor cursor) {
		TravelArticle res = new TravelArticle();
		res.file = selectedTravelBook;
		res.title = cursor.getString(0);
		try {
			res.content = Algorithms.gzipToString(cursor.getBlob(1)).trim();
		} catch (IllegalStateException e) {
			LOG.error(e.getMessage(), e);
		}
		res.isPartOf = cursor.getString(2);
		res.lat = cursor.isNull(3) ? Double.NaN : cursor.getDouble(3);
		res.lon = cursor.isNull(4) ? Double.NaN : cursor.getDouble(4);
		res.imageTitle = cursor.getString(5);
		res.routeId = cursor.getLong(7) + "";
		res.originalId = cursor.isNull(8) ? 0 : cursor.getLong(8);
		res.lang = cursor.getString(9);
		res.contentsJson = cursor.getString(10);
		res.aggregatedPartOf = cursor.getString(11);
		try {
			String gpxContent = Algorithms.gzipToString(cursor.getBlob(6));
			res.gpxFile = SharedUtil.loadGpxFile(new ByteArrayInputStream(gpxContent.getBytes("UTF-8")));
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return res;
	}

	public String formatTravelBookName(File tb) {
		if (tb == null) {
			return application.getString(R.string.shared_string_none);
		}
		String nm = tb.getName();
		return nm.substring(0, nm.indexOf('.')).replace('_', ' ');
	}

	@Nullable
	@Override
	public TravelGpx searchGpx(@NonNull LatLon location, @Nullable String fileName, @Nullable String ref) {
		return null;
	}

	@Override
	public void openTrackMenu(@NonNull TravelArticle article, @NonNull MapActivity mapActivity,
							  @NonNull String gpxFileName, @NonNull LatLon location) {

	}

	@NonNull
	@Override
	public String getGPXName(@NonNull TravelArticle article) {
		return article.getTitle().replace('/', '_').replace('\'', '_')
				.replace('\"', '_') + IndexConstants.GPX_FILE_EXT;
	}

	@NonNull
	@Override
	public File createGpxFile(@NonNull TravelArticle article) {
		GpxFile gpx = article.getGpxFile();
		File file = application.getAppPath(IndexConstants.GPX_TRAVEL_DIR + getGPXName(article));
		if (!file.exists()) {
			SharedUtil.writeGpxFile(file, gpx);
		}
		return file;
	}
	
	protected static class PopularArticle {
		String tripId;
		String title;
		String lang;
		int popIndex;
		int order;
		double lat;
		double lon;

		public boolean isLocationSpecified() {
			return !Double.isNaN(lat) && !Double.isNaN(lon);
		}

		public static PopularArticle readArticle(SQLiteCursor cursor) {
			PopularArticle res = new PopularArticle();
			res.title = cursor.getString(0);
			res.lat = cursor.isNull(1) ? Double.NaN : cursor.getDouble(1);
			res.lon = cursor.isNull(2) ? Double.NaN : cursor.getDouble(2);
			res.tripId = cursor.getLong(3) + "";
			res.lang = cursor.getString(4);
			res.order = cursor.isNull(5) ? -1 : cursor.getInt(5);
			res.popIndex = cursor.isNull(6) ? 0 : cursor.getInt(6);
			return res;
		}

		public static PopularArticle readArticleFromAmenity(Amenity a, String lang) {
			PopularArticle res = new PopularArticle();
			res.title = a.getName(lang);
			res.lat = a.getLocation().getLatitude();
			res.lon = a.getLocation().getLongitude();
			res.tripId = a.getId() + "";
			res.lang = lang;
			res.order = -1;
			res.popIndex = 0;
			return res;
		}
	}
}
