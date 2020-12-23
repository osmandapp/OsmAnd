package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS_FROM_SPACE;

public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);
	public static final String ROUTE_ARTICLE = "route_article";
	public static final int SEARCH_RADIUS = 100000;

	private final OsmandApplication app;
	private final Collator collator;

	private List<TravelArticle> popularArticles = new ArrayList<>();
	private final Map<String, TravelArticle> cachedArticles;
	private final TravelLocalDataHelper localDataHelper;

	public TravelObfHelper(OsmandApplication app) {
		this.app = app;
		collator = OsmAndCollator.primaryCollator();
		localDataHelper = new TravelLocalDataHelper(app);
		cachedArticles = new HashMap<>();
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
		String language = app.getLanguage();
		List<TravelArticle> popularArticles = new ArrayList<>();
		for (BinaryMapIndexReader travelBookReader : getTravelBookReaders()) {
			try {
				final LatLon location = app.getMapViewTrackingUtilities().getMapLocation();
				BinaryMapIndexReader.SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
						location, SEARCH_RADIUS, -1, getSearchRouteArticleFilter(), null);
				List<Amenity> amenities = travelBookReader.searchPoi(req);
				if (amenities.size() > 0) {
					for (Amenity a : amenities) {
						if (!Algorithms.isEmpty(a.getName(language))) {
							TravelArticle article = readArticle(a, language);
							popularArticles.add(article);
							cachedArticles.put(getCachedKeyForArticle(article.routeId, article.lang), article);
							if (popularArticles.size() >= 100) {
								break;
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
				LOG.error(e.getMessage());
			}
		}
		this.popularArticles = popularArticles;
		return popularArticles;
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

	private TravelArticle readArticle(@NonNull Amenity amenity, @Nullable String lang) {
		TravelArticle res = new TravelArticle();
		String title = Algorithms.isEmpty(amenity.getName(lang)) ? amenity.getName() : amenity.getName(lang);
		res.title = title;
		res.content = amenity.getDescription(lang);
		res.isPartOf = emptyIfNull(amenity.getTagContent(Amenity.IS_PART, lang));
		res.lat = amenity.getLocation().getLatitude();
		res.lon = amenity.getLocation().getLongitude();
		res.imageTitle = emptyIfNull(amenity.getTagContent(Amenity.IMAGE_TITLE, lang));
		res.routeId = getRouteId(amenity);
		res.originalId = 0;
		res.lang = lang;
		res.contentsJson = emptyIfNull(amenity.getTagContent(Amenity.CONTENT_JSON, lang));
		res.aggregatedPartOf = emptyIfNull(amenity.getTagContent(Amenity.IS_AGGR_PART, lang));
		return res;
	}

	private String emptyIfNull(String text) {
		return text == null ? "" : text;
	}

	private String getRouteId(Amenity amenity) {
		return amenity.getTagContent(Amenity.ROUTE_ID, null);
	}

	@Override
	public boolean isAnyTravelBookPresent() {
		return !Algorithms.isEmpty(getTravelBookReaders());
	}

	@NonNull
	@Override
	public List<WikivoyageSearchResult> search(@NonNull String searchQuery) {
		List<WikivoyageSearchResult> res = new ArrayList<>();
		List<Amenity> searchObjects = null;
		for (BinaryMapIndexReader reader : app.getResourceManager().getTravelRepositories()) {
			try {
				BinaryMapIndexReader.SearchRequest<Amenity> searchRequest = BinaryMapIndexReader.
						buildSearchPoiRequest(0, 0, searchQuery,
								0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, getSearchRouteArticleFilter(), null, null);

				searchObjects = reader.searchPoiByName(searchRequest);
			} catch (IOException e) {
				LOG.error(e);
			}
		}
		if (!Algorithms.isEmpty(searchObjects)) {
			for (Amenity obj : searchObjects) {
				WikivoyageSearchResult r = new WikivoyageSearchResult();
				Map<String, String> namesMap = obj.getNamesMap(true);
				Iterator<String> it = namesMap.keySet().iterator();
				TravelArticle article = null;
				while (it.hasNext()) {
					String currLang = it.next();
					article = readArticle(obj, currLang);
					r.articleTitles.add(article.title);
					r.isPartOf.add(article.isPartOf);
					r.langs.add(article.lang);
					r.routeId = getRouteId(obj);
					r.imageTitle = emptyIfNull(obj.getTagContent(Amenity.IMAGE_TITLE, currLang));
					cachedArticles.put(getCachedKeyForArticle(article.routeId, article.lang), article);
				}
				if (article != null) {
					res.add(r);
				}
			}
			sortSearchResults(res);
		}
		return res;
	}

	private String getCachedKeyForArticle(String routeId, String lang) {
		return routeId + lang;
	}

	private void sortSearchResults(@NonNull List<WikivoyageSearchResult> list) {
		Collections.sort(list, new Comparator<WikivoyageSearchResult>() {

			@Override
			public int compare(WikivoyageSearchResult res1, WikivoyageSearchResult res2) {
				return collator.compare(res1.articleTitles.get(0), res2.articleTitles.get(0));
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
		return Collections.emptyMap();
	}

	@Override
	public TravelArticle getArticleById(@NonNull String routeId, @NonNull String lang) {
		TravelArticle article = cachedArticles.get(getCachedKeyForArticle(routeId, lang));
		if (article != null) {
			return article;
		} else {
			return getArticleByIdFromTravelBooks(routeId, lang);
		}
	}

	private TravelArticle getArticleByIdFromTravelBooks(final String routeId, final String lang) {
		TravelArticle article = null;
		final List<Amenity> amenities = new ArrayList<>();
		for (BinaryMapIndexReader travelBookReader : getTravelBookReaders()) {
			try {
				BinaryMapIndexReader.SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, getSearchRouteArticleFilter(),
						new ResultMatcher<Amenity>() {
							boolean done = false;

							@Override
							public boolean publish(Amenity amenity) {
								if (getRouteId(amenity).equals(routeId)) {
									amenities.add(amenity);
									done = true;
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return done;
							}
						});

				travelBookReader.searchPoi(req);
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
			if (!amenities.isEmpty()) {
				article = readArticle(amenities.get(0), lang);
				cachedArticles.put(getCachedKeyForArticle(article.routeId, article.lang), article);
			}
		}
		return article;
	}

	@Nullable
	@Override
	public TravelArticle getArticleByTitle(@NonNull final String title, @NonNull final String lang) {
		TravelArticle article = null;
		final List<Amenity> amenities = new ArrayList<>();
		for (BinaryMapIndexReader travelBookReader : getTravelBookReaders()) {
			try {
				BinaryMapIndexReader.SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
						0, 0, title, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, getSearchRouteArticleFilter(),
						new ResultMatcher<Amenity>() {
							boolean done = false;

							@Override
							public boolean publish(Amenity amenity) {
								if (CollatorStringMatcher.cmatches(collator, title, amenity.getName(lang), CHECK_EQUALS_FROM_SPACE)) {
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

				travelBookReader.searchPoiByName(req);
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
			if (!amenities.isEmpty()) {
				article = readArticle(amenities.get(0), lang);
				cachedArticles.put(getCachedKeyForArticle(article.routeId, article.lang), article);
			}
		}
		return article;
	}

	private List<BinaryMapIndexReader> getTravelBookReaders() {
		if (!app.isApplicationInitializing()) {
			return app.getResourceManager().getTravelRepositories();
		} else {
			return new ArrayList<>();
		}
	}

	@NonNull
	@Override
	public String getArticleId(@NonNull String title, @NonNull String lang) {
		TravelArticle a = null;
		for (TravelArticle article : cachedArticles.values()) {
			if (article.getTitle().equals(title)) {
				a = article;
				break;
			}
		}
		if (a == null) {
			TravelArticle article = getArticleByTitle(title, lang);
			if (article != null) {
				a = article;
			}
		}
		return a != null && a.getRouteId() != null ? getCachedKeyForArticle(a.routeId, a.lang) : "";
	}

	@NonNull
	@Override
	public ArrayList<String> getArticleLangs(@NonNull String routeId) {
		ArrayList<String> res = new ArrayList<>();
		res.add("en");
		for (TravelArticle article : popularArticles) {
			if (article.getRouteId().equals(routeId)) {
				res.add(article.getLang());
			}
		}
		for (TravelArticle article : cachedArticles.values()) {
			if (article.getRouteId().equals(routeId)) {
				res.add(article.getLang());
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
}