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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS_FROM_SPACE;

public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);
	public static final String ROUTE_ARTICLE = "route_article";
	public static final int RADIUS_100_KM = 100000;
	public static final int RADIUS_50_KM = 50000;

	private static final String WORLD_WIKIVOYAGE_FILE_NAME = "World_wikivoyage.travel.obf";

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
		for (BinaryMapIndexReader reader : getTravelBookReaders()) {
			try {
				final LatLon location = app.getMapViewTrackingUtilities().getMapLocation();
				BinaryMapIndexReader.SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
						location, RADIUS_100_KM, -1, getSearchRouteArticleFilter(), null);
				List<Amenity> amenities = reader.searchPoi(req);
				if (amenities.size() > 0) {
					for (Amenity a : amenities) {
						if (!Algorithms.isEmpty(a.getName(language))) {
							TravelArticle article = readArticle(a, language, reader.getFile());
							popularArticles.add(article);
							cachedArticles.put(article.routeId, article);
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

	private TravelArticle readArticle(@NonNull Amenity amenity, @Nullable String lang, @NonNull File file) {
		TravelArticle res = new TravelArticle();
		String title = Algorithms.isEmpty(amenity.getName(lang)) ? amenity.getName() : amenity.getName(lang);
		if (Algorithms.isEmpty(title)) {
			Map<String, String> namesMap = amenity.getNamesMap(true);
			if (!namesMap.isEmpty()) {
				lang = namesMap.keySet().iterator().next();
				title = amenity.getName(lang);
			}
		}
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
		res.setTravelBook(file.getName());
		res.setLastModified(file.lastModified());
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
		String baseLng = app.getLanguage();
		for (BinaryMapIndexReader reader : getTravelBookReaders()) {
			try {
				BinaryMapIndexReader.SearchRequest<Amenity> searchRequest = BinaryMapIndexReader.
						buildSearchPoiRequest(0, 0, searchQuery,
								0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, getSearchRouteArticleFilter(), null, null);

				searchObjects = reader.searchPoiByName(searchRequest);
				for (Amenity obj : searchObjects) {
					WikivoyageSearchResult r = new WikivoyageSearchResult();
					TravelArticle article = readArticle(obj, baseLng, reader.getFile());
					r.articleTitles = new ArrayList<>(Collections.singletonList(article.title));
					r.imageTitle = article.imageTitle;
					r.routeId = article.routeId;
					r.isPartOf = new ArrayList<>(Collections.singletonList(article.isPartOf));
					r.langs = new ArrayList<>(Collections.singletonList(baseLng));
					res.add(r);
					cachedArticles.put(article.routeId, article);
				}
			} catch (IOException e) {
				LOG.error(e);
			}
		}
		if (!Algorithms.isEmpty(res)) {
			res = new ArrayList<>(groupSearchResultsByRouteId(res));
			sortSearchResults(res);
		}
		return res;
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
	private Collection<WikivoyageSearchResult> groupSearchResultsByRouteId(@NonNull List<WikivoyageSearchResult> res) {
		String baseLng = app.getLanguage();
		Map<String, WikivoyageSearchResult> wikivoyage = new HashMap<>();
		for (WikivoyageSearchResult rs : res) {
			WikivoyageSearchResult prev = wikivoyage.get(rs.routeId);
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
				prev.isPartOf.add(insInd, rs.isPartOf.get(0));
			} else {
				wikivoyage.put(rs.routeId, rs);
			}
		}
		return wikivoyage.values();
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
		TravelArticle article = cachedArticles.get(routeId);
		if (article != null) {
			return article;
		}
		article = getArticleByIdFromTravelBooks(routeId, lang);
		if (article != null) {
			return getArticleByIdFromTravelBooks(routeId, lang);
		}
		return localDataHelper.getSavedArticle(routeId, lang);
	}

	private TravelArticle getArticleByIdFromTravelBooks(final String routeId, final String lang) {
		return getArticleByIdAndLatLon(routeId, lang, null);
	}

	public TravelArticle getArticleByIdAndLatLon(final String routeId, final String lang, LatLon latLon) {
		final TravelArticle[] article = {null};
		for (final BinaryMapIndexReader reader : getTravelBookReaders()) {
			try {
				BinaryMapIndexReader.SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, getSearchRouteArticleFilter(),
						new ResultMatcher<Amenity>() {
							boolean done = false;

							@Override
							public boolean publish(Amenity amenity) {
								if (getRouteId(amenity).equals(routeId)) {
									article[0] = readArticle(amenity, lang, reader.getFile());
									cachedArticles.put(article[0].routeId, article[0]);
									done = true;
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return done;
							}
						});
				if (latLon != null) {
					req.setBBoxRadius(latLon.getLatitude(), latLon.getLongitude(), RADIUS_50_KM);
				}
				reader.searchPoi(req);
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
		}
		return article[0];
	}

	@Nullable
	@Override
	public TravelArticle getArticleByTitle(@NonNull final String title, @NonNull final String lang) {
		final TravelArticle[] article = {null};
		for (final BinaryMapIndexReader reader : getTravelBookReaders()) {
			try {
				BinaryMapIndexReader.SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
						0, 0, title, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, getSearchRouteArticleFilter(),
						new ResultMatcher<Amenity>() {
							boolean done = false;

							@Override
							public boolean publish(Amenity amenity) {
								if (CollatorStringMatcher.cmatches(collator, title, amenity.getName(lang), CHECK_EQUALS_FROM_SPACE)) {
									article[0] = readArticle(amenity, lang, reader.getFile());
									cachedArticles.put(article[0].routeId, article[0]);
									done = true;
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return done;
							}
						}, null);

				reader.searchPoiByName(req);
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
		}
		return article[0];
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
		return a != null && a.getRouteId() != null ? a.getRouteId() : "";
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

	@NonNull
	@Override
	public TravelArticle getUpdatedArticle(@NonNull TravelArticle article) {
		TravelArticle newArticle = getArticleByTitle(article.title, article.lang);
		if (newArticle == null) {
			newArticle = getArticleByIdAndLatLon(article.getRouteId(), article.lang, new LatLon(article.lat, article.lon));
		}
		if (newArticle == null) {
			newArticle = article;
		}
		return newArticle;
	}
}