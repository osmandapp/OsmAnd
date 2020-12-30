package net.osmand.plus.wikivoyage.data;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);
	private static final String WORLD_WIKIVOYAGE_FILE_NAME = "World_wikivoyage.travel.obf";
	public static final String ROUTE_ARTICLE = "route_article";
	public static final int POPULAR_ARTICLES_SEARCH_RADIUS = 100000;
	public static final int ARTICLE_SEARCH_RADIUS = 50000;
	public static final int MAX_POPULAR_ARTICLES_COUNT = 100;

	private final OsmandApplication app;
	private final Collator collator;

	private List<TravelArticle> popularArticles = new ArrayList<>();
	private Map<TravelArticleIdentifier, Map<String, TravelArticle>> cachedArticles = new ConcurrentHashMap<>();
	private final TravelLocalDataHelper localDataHelper;

	public TravelObfHelper(OsmandApplication app) {
		this.app = app;
		collator = OsmAndCollator.primaryCollator();
		localDataHelper = new TravelLocalDataHelper(app);
	}

	@Override
	public TravelLocalDataHelper getBookmarksHelper() {
		return localDataHelper;
	}

	@Override
	public void initializeDataOnAppStartup() {
	}

	@Override
	public void initializeDataToDisplay() {
		localDataHelper.refreshCachedData();
		loadPopularArticles();
	}

	@NonNull
	public List<TravelArticle> loadPopularArticles() {
		String lang = app.getLanguage();
		List<TravelArticle> popularArticles = new ArrayList<>();
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				final LatLon location = app.getMapViewTrackingUtilities().getMapLocation();
				SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
						location, POPULAR_ARTICLES_SEARCH_RADIUS, -1, getSearchRouteArticleFilter(), null);
				List<Amenity> amenities = reader.searchPoi(req);
				if (amenities.size() > 0) {
					for (Amenity amenity : amenities) {
						if (!Algorithms.isEmpty(amenity.getName(lang))) {
							TravelArticle article = cacheTravelArticles(reader.getFile(), amenity, lang);
							if (article != null) {
								popularArticles.add(article);
								if (popularArticles.size() >= MAX_POPULAR_ARTICLES_COUNT) {
									break;
								}
							}
						}
					}
					Collections.sort(popularArticles, new Comparator<TravelArticle>() {
						@Override
						public int compare(TravelArticle article1, TravelArticle article2) {
							int d1 = (int) (MapUtils.getDistance(article1.getLat(), article1.getLon(),
									location.getLatitude(), location.getLongitude()));
							int d2 = (int) (MapUtils.getDistance(article2.getLat(), article2.getLon(),
									location.getLatitude(), location.getLongitude()));
							return d1 < d2 ? -1 : (d1 == d2 ? 0 : 1);
						}
					});
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
		this.popularArticles = popularArticles;
		return popularArticles;
	}

	@Nullable
	private TravelArticle cacheTravelArticles(File file, Amenity amenity, String lang) {
		TravelArticle article = null;
		Map<String, TravelArticle> articles = readArticles(file, amenity);
		if (!Algorithms.isEmpty(articles)) {
			TravelArticleIdentifier newArticleId = articles.values().iterator().next().generateIdentifier();
			cachedArticles.put(newArticleId, articles);
			article = getCachedArticle(newArticleId, lang);
		}
		return article;
	}

	SearchPoiTypeFilter getSearchRouteArticleFilter() {
		return new SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				return subcategory.equals(ROUTE_ARTICLE);
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};
	}

	@NonNull
	private Map<String, TravelArticle> readArticles(@NonNull File file, @NonNull Amenity amenity) {
		Map<String, TravelArticle> articles = new HashMap<>();
		Set<String> langs = getLanguages(amenity);
		for (String lang : langs) {
			articles.put(lang, readArticle(file, amenity, lang));
		}
		return articles;
	}

	@NonNull
	private TravelArticle readArticle(@NonNull File file, @NonNull Amenity amenity, @Nullable String lang) {
		TravelArticle res = new TravelArticle();
		res.file = file;
		String title = amenity.getName(lang);
		res.title = Algorithms.isEmpty(title) ? amenity.getName() : title;
		res.content = amenity.getDescription(lang);
		res.isPartOf = emptyIfNull(amenity.getTagContent(Amenity.IS_PART, lang));
		res.lat = amenity.getLocation().getLatitude();
		res.lon = amenity.getLocation().getLongitude();
		res.imageTitle = emptyIfNull(amenity.getTagContent(Amenity.IMAGE_TITLE, null));
		res.routeId = emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID, null));
		res.routeSource = emptyIfNull(amenity.getTagContent(Amenity.ROUTE_SOURCE, null));
		res.originalId = 0;
		res.lang = lang;
		res.contentsJson = emptyIfNull(amenity.getTagContent(Amenity.CONTENT_JSON, lang));
		res.aggregatedPartOf = emptyIfNull(amenity.getTagContent(Amenity.IS_AGGR_PART, lang));
		return res;
	}

	private String emptyIfNull(String text) {
		return text == null ? "" : text;
	}

	@Override
	public boolean isAnyTravelBookPresent() {
		return !Algorithms.isEmpty(getReaders());
	}

	@NonNull
	@Override
	public List<WikivoyageSearchResult> search(@NonNull String searchQuery) {
		List<WikivoyageSearchResult> res = new ArrayList<>();
		Map<File, List<Amenity>> amenityMap = new HashMap<>();
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				SearchRequest<Amenity> searchRequest = BinaryMapIndexReader.buildSearchPoiRequest(0, 0, searchQuery,
								0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, getSearchRouteArticleFilter(), null, null);

				List<Amenity> amenities = reader.searchPoiByName(searchRequest);
				if (!Algorithms.isEmpty(amenities)) {
					amenityMap.put(reader.getFile(), amenities);
				}
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		if (!Algorithms.isEmpty(amenityMap)) {
			String appLang = app.getLanguage();
			for (Entry<File, List<Amenity>> entry : amenityMap.entrySet()) {
				File file = entry.getKey();
				for (Amenity amenity : entry.getValue()) {
					Set<String> nameLangs = getLanguages(amenity);
					if (nameLangs.contains(appLang)) {
						TravelArticle article = readArticle(file, amenity, appLang);
						WikivoyageSearchResult r = new WikivoyageSearchResult(article, new ArrayList<>(nameLangs));
						res.add(r);
					}
				}
			}
			sortSearchResults(res);
		}
		return res;
	}

	private Set<String> getLanguages(@NonNull Amenity amenity) {
		Set<String> langs = new HashSet<>();
		String descrStart = Amenity.DESCRIPTION + ":";
		String partStart = Amenity.IS_PART + ":";
		for (String infoTag : amenity.getAdditionalInfoKeys()) {
			if (infoTag.startsWith(descrStart)) {
				if (infoTag.length() > descrStart.length()) {
					langs.add(infoTag.substring(descrStart.length()));
				}
			} else if (infoTag.startsWith(partStart)) {
				if (infoTag.length() > partStart.length()) {
					langs.add(infoTag.substring(partStart.length()));
				}
			}
		}
		return langs;
	}

	private void sortSearchResults(@NonNull List<WikivoyageSearchResult> list) {
		Collections.sort(list, new Comparator<WikivoyageSearchResult>() {
			@Override
			public int compare(WikivoyageSearchResult res1, WikivoyageSearchResult res2) {
				return collator.compare(res1.articleId.title, res2.articleId.title);
			}
		});
	}

	@NonNull
	@Override
	public List<TravelArticle> getPopularArticles() {
		return popularArticles;
	}

	@NonNull
	@Override
	public Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> getNavigationMap(@NonNull final TravelArticle article) {
		final String lang = article.getLang();
		final String title = article.getTitle();
		if (TextUtils.isEmpty(lang) || TextUtils.isEmpty(title)) {
			return Collections.emptyMap();
		}
		final String[] parts;
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
		} else {
			parts = null;
		}
		Map<String, List<WikivoyageSearchResult>> navMap = new HashMap<>();
		Set<String> headers = new LinkedHashSet<String>();
		Map<String, WikivoyageSearchResult> headerObjs = new HashMap<>();
		Map<File, List<Amenity>> amenityMap = new HashMap<>();
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(0,
						Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, getSearchRouteArticleFilter(), new ResultMatcher<Amenity>() {

							@Override
							public boolean publish(Amenity amenity) {
								String isPartOf = amenity.getTagContent(Amenity.IS_PART, lang);
								if (Algorithms.stringsEqual(title, isPartOf)) {
									return true;
								} else if (parts != null && parts.length > 0) {
									String title = amenity.getName(lang);
									title = Algorithms.isEmpty(title) ? amenity.getName() : title;
									for (int i = 0; i < parts.length; i++) {
										String part = parts[i];
										if (i == 0 && Algorithms.stringsEqual(part, title) || Algorithms.stringsEqual(part, isPartOf)) {
											return true;
										}
									}
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						});
				List<Amenity> amenities = reader.searchPoi(req);
				if (!Algorithms.isEmpty(amenities)) {
					amenityMap.put(reader.getFile(), amenities);
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
		if (parts != null && parts.length > 0) {
			headers.addAll(Arrays.asList(parts));
			headers.add(title);
		}
		if (!Algorithms.isEmpty(amenityMap)) {
			for (Entry<File, List<Amenity>> entry : amenityMap.entrySet()) {
				File file = entry.getKey();
				for (Amenity amenity : entry.getValue()) {
					Set<String> nameLangs = getLanguages(amenity);
					if (nameLangs.contains(lang)) {
						TravelArticle a = readArticle(file, amenity, lang);
						WikivoyageSearchResult rs = new WikivoyageSearchResult(a, new ArrayList<>(nameLangs));
						List<WikivoyageSearchResult> l = navMap.get(rs.isPartOf);
						if (l == null) {
							l = new ArrayList<>();
							navMap.put(rs.isPartOf, l);
						}
						l.add(rs);
						if (headers != null && headers.contains(a.getTitle())) {
							headerObjs.put(a.getTitle(), rs);
						}
					}
				}
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
	public TravelArticle getArticleById(@NonNull TravelArticleIdentifier articleId, @NonNull String lang) {
		TravelArticle article = getCachedArticle(articleId, lang);
		return article == null ? findArticleById(articleId, lang) : article;
	}

	@Nullable
	private TravelArticle getCachedArticle(@NonNull TravelArticleIdentifier articleId, @NonNull String lang) {
		TravelArticle article = null;
		Map<String, TravelArticle> articles = cachedArticles.get(articleId);
		if (articles != null) {
			if (Algorithms.isEmpty(lang)) {
				Collection<TravelArticle> ac = articles.values();
				if (!ac.isEmpty()) {
					article = ac.iterator().next();
				}
			} else {
				article = articles.get(lang);
				if (article == null) {
					article = articles.get("");
				}
			}
		}
		return article == null ? findArticleById(articleId, lang) : article;
	}

	private TravelArticle findArticleById(@NonNull final TravelArticleIdentifier articleId, final String lang) {
		TravelArticle article = null;
		final List<Amenity> amenities = new ArrayList<>();
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				if (articleId.file != null && !articleId.file.equals(reader.getFile())) {
					continue;
				}
				SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
						Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
						getSearchRouteArticleFilter(), new ResultMatcher<Amenity>() {
							boolean done = false;

							@Override
							public boolean publish(Amenity amenity) {
								if (Algorithms.stringsEqual(articleId.routeId, Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID, null)))
										&& Algorithms.stringsEqual(articleId.routeSource, Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_SOURCE, null)))) {
									amenities.add(amenity);
									done = true;
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return done;
							}
						}, null);

				if (!Double.isNaN(articleId.lat)) {
					req.setBBoxRadius(articleId.lat, articleId.lon, ARTICLE_SEARCH_RADIUS);
					if (!Algorithms.isEmpty(articleId.title)) {
						reader.searchPoiByName(req);
					} else {
						reader.searchPoi(req);
					}
				} else {
					reader.searchPoi(req);
				}
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
			if (!amenities.isEmpty()) {
				article = cacheTravelArticles(reader.getFile(), amenities.get(0), lang);
			}
		}
		return article;
	}

	@Nullable
	@Override
	public TravelArticle getArticleByTitle(@NonNull final String title, @NonNull final String lang) {
		return getArticleByTitle(title, new QuadRect(), lang);
	}

	@Nullable
	@Override
	public TravelArticle getArticleByTitle(@NonNull final String title, @NonNull LatLon latLon, @NonNull final String lang) {
		QuadRect rect = latLon != null ? MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), ARTICLE_SEARCH_RADIUS) : new QuadRect();
		return getArticleByTitle(title, rect, lang);
	}

	@Nullable
	@Override
	public TravelArticle getArticleByTitle(@NonNull final String title, @NonNull QuadRect rect, @NonNull final String lang) {
		TravelArticle article = null;
		List<Amenity> amenities = null;
		int x = 0;
		int y = 0;
		int left = 0;
		int right = Integer.MAX_VALUE;
		int top = 0;
		int bottom = Integer.MAX_VALUE;
		if (rect.height() > 0 && rect.width() > 0) {
			x = (int) rect.centerX();
			y = (int) rect.centerY();
			left = (int) rect.left;
			right = (int) rect.right;
			top = (int) rect.top;
			bottom = (int) rect.bottom;
		}
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
						x, y, title, left, right, top, bottom, getSearchRouteArticleFilter(), null, null);
				amenities = reader.searchPoiByName(req);
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
			if (!Algorithms.isEmpty(amenities)) {
				article = cacheTravelArticles(reader.getFile(), amenities.get(0), lang);
			}
		}
		return article;
	}

	private List<BinaryMapIndexReader> getReaders() {
		if (!app.isApplicationInitializing()) {
			return app.getResourceManager().getTravelRepositories();
		} else {
			return new ArrayList<>();
		}
	}

	@Nullable
	@Override
	public TravelArticleIdentifier getArticleId(@NonNull String title, @NonNull String lang) {
		TravelArticle a = null;
		for (Map<String, TravelArticle> articles : cachedArticles.values()) {
			for (TravelArticle article : articles.values()) {
				if (article.getTitle().equals(title)) {
					a = article;
					break;
				}
			}
		}
		if (a == null) {
			TravelArticle article = getArticleByTitle(title, lang);
			if (article != null) {
				a = article;
			}
		}
		return a != null ? a.generateIdentifier() : null;
	}

	@NonNull
	@Override
	public ArrayList<String> getArticleLangs(@NonNull TravelArticleIdentifier articleId) {
		ArrayList<String> res = new ArrayList<>();
		TravelArticle article = getArticleById(articleId, "");
		if (article != null) {
			Map<String, TravelArticle> articles = cachedArticles.get(articleId);
			if (articles != null) {
				res.addAll(articles.keySet());
			}
		}
		return res;
	}

	@NonNull
	@Override
	public String getGPXName(@NonNull final TravelArticle article) {
		return article.getTitle().replace('/', '_').replace('\'', '_')
				.replace('\"', '_') + IndexConstants.GPX_FILE_EXT;
	}

	@NonNull
	@Override
	public File createGpxFile(@NonNull final TravelArticle article) {
		final GPXFile gpx = article.getGpxFile();
		File file = app.getAppPath(IndexConstants.GPX_TRAVEL_DIR + getGPXName(article));
		if (!file.exists()) {
			GPXUtilities.writeGpxFile(file, gpx);
		}
		return file;
	}

	@Override
	public String getSelectedTravelBookName() {
		return "";
	}

	@Override
	public String getWikivoyageFileName() {
		return WORLD_WIKIVOYAGE_FILE_NAME;
	}
}