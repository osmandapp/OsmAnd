package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
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
import java.util.List;
import java.util.Map;

public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);
	public static final String ROUTE_ARTICLE = "route_article";
	public static final int SEARCH_RADIUS = 100000;

	private final OsmandApplication app;

	private List<TravelArticle> popularArticles = new ArrayList<>();
	private final Map<String, TravelArticle> cachedArticles;
	private final TravelLocalDataHelper localDataHelper;

	public TravelObfHelper(OsmandApplication app) {
		this.app = app;
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
		popularArticles.clear();
		final List<Amenity> amenities = new ArrayList<>();
		for (BinaryMapIndexReader travelBookReader : getTravelBookReaders()) {
			try {
				if (travelBookReader == null) {
					popularArticles = new ArrayList<>();
					return popularArticles;
				}
				final LatLon location = app.getMapViewTrackingUtilities().getMapLocation();
				BinaryMapIndexReader.SearchRequest<Amenity> req =
						BinaryMapIndexReader.buildSearchPoiRequest(location, SEARCH_RADIUS, -1,
								BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER,
								new ResultMatcher<Amenity>() {
									@Override
									public boolean publish(Amenity amenity) {
										if (amenity.getSubType().equals(ROUTE_ARTICLE)) {
											amenities.add(amenity);
										}
										return false;
									}

									@Override
									public boolean isCancelled() {
										return false;
									}
								});
				travelBookReader.searchPoi(req);

				if (amenities.size() > 0) {
					for (Amenity a : amenities) {
						if (!Algorithms.isEmpty(a.getName(language))) {
							TravelArticle article = readArticle(a, language);
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
		return popularArticles;
	}

	private TravelArticle readArticle(Amenity amenity, String lang) {
		TravelArticle res = new TravelArticle();
		res.title = Algorithms.isEmpty(amenity.getName(lang)) ? amenity.getName() : amenity.getName(lang);
		res.content = amenity.getDescription(lang);
		res.isPartOf = emptyIfNull(amenity.getTagContent(Amenity.IS_PART, lang));
		res.lat = amenity.getLocation().getLatitude();
		res.lon = amenity.getLocation().getLongitude();
		res.imageTitle = emptyIfNull(amenity.getTagContent(Amenity.IMAGE_TITLE, lang));
		res.routeId = getRouteId(amenity);
		res.originalId = 0; //?
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
	public List<WikivoyageSearchResult> search(String searchQuery) {
		return null;
	}

	@NonNull
	@Override
	public List<TravelArticle> getPopularArticles() {
		return popularArticles;
	}

	@Override
	public Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> getNavigationMap(TravelArticle article) {
		return null;
	}

	@Override
	public TravelArticle getArticleById(String routeId, String lang) {
		return cachedArticles.get(routeId);
	}

	@Override
	public TravelArticle getArticleByTitle(final String title, final String lang) {
		TravelArticle article = null;
		final List<Amenity> amenities = new ArrayList<>();
		for (BinaryMapIndexReader travelBookReader : getTravelBookReaders()) {
			try {
				int left = 0;
				int right = Integer.MAX_VALUE;
				int top = 0;
				int bottom = Integer.MAX_VALUE;
				BinaryMapIndexReader.SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
						0, 0, title, left, right, top, bottom,
						new ResultMatcher<Amenity>() {
							@Override
							public boolean publish(Amenity amenity) {
								if (title.equalsIgnoreCase(amenity.getName(lang))
										&& amenity.getSubType().equals(ROUTE_ARTICLE)) {
									amenities.add(amenity);
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						});

				travelBookReader.searchPoiByName(req);
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
			if (!amenities.isEmpty()) {
				article = readArticle(amenities.get(0), lang);
				cachedArticles.put(article.routeId, article);
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

	@Override
	public String getArticleId(String title, String lang) {
		for (TravelArticle article : cachedArticles.values()) {
			if (article.getTitle().equals(title)) {
				return article.getRouteId();
			}
		}
		TravelArticle article = getArticleByTitle(title, lang);
		if (article != null) {
			return article.getRouteId();
		}
		return null;
	}

	@Override
	public ArrayList<String> getArticleLangs(String routeId) {
		ArrayList<String> res = new ArrayList<>();
		res.add("en");
		for (TravelArticle article : popularArticles) {
			if (article.getRouteId().equals(routeId)) {
				res.add(article.getLang());
			}
		}
		return res;
	}

	@Override
	public String getGPXName(TravelArticle article) {
		return article.getTitle().replace('/', '_').replace('\'', '_')
				.replace('\"', '_') + IndexConstants.GPX_FILE_EXT;
	}

	@Override
	public File createGpxFile(TravelArticle article) {
		final GPXUtilities.GPXFile gpx = article.getGpxFile();
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