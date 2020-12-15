package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.Location;
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
	private List<BinaryMapIndexReader> travelBookReaders;

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
		travelBookReaders = app.getResourceManager().getTravelRepositories();
	}

	@Override
	public void initializeDataToDisplay() {
		localDataHelper.refreshCachedData();
		loadPopularArticles();
	}

	//TODO for now it reads any articles, since we didn't have popular articles in the obf
	@NonNull
	public List<TravelArticle> loadPopularArticles() {
		String language = app.getLanguage();
		final List<Amenity> articles = new ArrayList<>();
		for (BinaryMapIndexReader travelBookReader : travelBookReaders) {
			try {
				if (travelBookReader == null) {
					popularArticles = new ArrayList<>();
					return popularArticles;
				}
				Location myLocation = app.getLocationProvider().getLastKnownLocation();
				LatLon ll;
				if (myLocation != null) {
					ll = new LatLon(myLocation.getLatitude(), myLocation.getLongitude());
				} else {
					ll = app.getMapViewTrackingUtilities().getMapLocation();
				}
				BinaryMapIndexReader.SearchRequest<Amenity> req =
						BinaryMapIndexReader.buildSearchPoiRequest(ll, SEARCH_RADIUS,-1,
								BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER,
								new ResultMatcher<Amenity>() {
									int count = 0;

									@Override
									public boolean publish(Amenity object) {
										//TODO need more logical way to filter results
										if (object.getSubType().equals(ROUTE_ARTICLE)) {
											articles.add(object);
										}
										return false;
									}

									@Override
									public boolean isCancelled() {
										return false;
									}
								});
				req.setBBoxRadius(ll.getLatitude(),ll.getLongitude(),100000);
				travelBookReader.searchPoi(req);
				travelBookReader.close();

				if (articles.size() > 0) {
					for (Amenity a : articles) {
						if (!a.getName(language).equals("")) {
							TravelArticle article = readArticle(a, language);
							popularArticles.add(article);
							cachedArticles.put(article.routeId, article);
						}
					}
				}
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
		}
		return popularArticles;
	}

	private TravelArticle readArticle(Amenity amenity, String lang) {
		TravelArticle res = new TravelArticle();
		res.title = amenity.getName(lang).equals("") ? amenity.getName() : amenity.getName(lang);
		res.content = amenity.getDescription(lang);
		res.isPartOf = amenity.getTagContent(Amenity.IS_PART, lang) == null ? "" : amenity.getTagContent(Amenity.IS_PART, lang);
		res.lat = amenity.getLocation().getLatitude();
		res.lon = amenity.getLocation().getLongitude();
		res.imageTitle = amenity.getTagContent(Amenity.IMAGE_TITLE, lang) == null ? "" : amenity.getTagContent(Amenity.IMAGE_TITLE, lang);
		res.routeId = getRouteId(amenity);
		res.originalId = 0; //?
		res.lang = lang;
		res.contentsJson = amenity.getTagContent(Amenity.CONTENT_JSON, lang) == null ? "" : amenity.getTagContent(Amenity.CONTENT_JSON, lang);
		res.aggregatedPartOf = amenity.getTagContent(Amenity.IS_AGGR_PART, lang) == null ? "" : amenity.getTagContent(Amenity.IS_AGGR_PART, lang);
		return res;
	}

	private String getRouteId(Amenity amenity) {
		return amenity.getTagContent(Amenity.ROUTE_ID, null);
	}

	@Override
	public boolean isAnyTravelBookPresent() {
		return !Algorithms.isEmpty(travelBookReaders);
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
		TravelArticle res = null;
		List<Amenity> amenities = Collections.emptyList();
		for (BinaryMapIndexReader travelBookReader : travelBookReaders) {
			try {
				if (travelBookReader != null) {
					int left = 0;
					int top = 0;
					int right = Integer.MAX_VALUE;
					int bottom = Integer.MAX_VALUE;
					LatLon ll = app.getMapViewTrackingUtilities().getMapLocation();
					BinaryMapIndexReader.SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
							MapUtils.get31TileNumberX(ll.getLongitude()),
							MapUtils.get31TileNumberY(ll.getLatitude()), title,
							left, top, right, bottom,
							new ResultMatcher<Amenity>() {
								@Override
								public boolean publish(Amenity object) {
									if (object.getName(lang).equals(title)) {
										return true;
									}
									return false;
								}

								@Override
								public boolean isCancelled() {
									return false;
								}
							});

					amenities = travelBookReader.searchPoiByName(req);
				}
			} catch (IOException e) {
				//todo
			}
			if (!amenities.isEmpty()) {
				for (Amenity a : amenities) {
					LOG.debug("searched article: " + a);
				}
			}
		}
		return res;
	}

	@Override
	public String getArticleId(String title, String lang) {
		for (TravelArticle article : popularArticles) {
			if (article.getTitle().equals(title)) {
				return article.getRouteId();
			}
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
