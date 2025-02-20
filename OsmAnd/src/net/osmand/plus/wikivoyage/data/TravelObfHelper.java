package net.osmand.plus.wikivoyage.data;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.data.Amenity.REF;
import static net.osmand.data.Amenity.ROUTE;
import static net.osmand.data.Amenity.ROUTE_ID;
import static net.osmand.osm.MapPoiTypes.ROUTES_PREFIX;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;
import static net.osmand.plus.wikivoyage.data.PopularArticles.ARTICLES_PER_PAGE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ROUTE_ACTIVITY_TYPE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.AVERAGE_ELEVATION;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DIFF_ELEVATION_DOWN;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DIFF_ELEVATION_UP;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DISTANCE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ELE_GRAPH;
import static net.osmand.plus.wikivoyage.data.TravelGpx.MAX_ELEVATION;
import static net.osmand.plus.wikivoyage.data.TravelGpx.MIN_ELEVATION;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ROUTE_BBOX_RADIUS;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ROUTE_TYPE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.START_ELEVATION;
import static net.osmand.plus.wikivoyage.data.TravelGpx.USER;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_BACKGROUNDS;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_COLORS;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_DELIMITER;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_EMPTY_NAME_STUB;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_ICONS;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_NAMES;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_PREFIX;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_FIRST_DIST;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_FIRST_LETTER;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_MULT_1;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_MULT_2;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.Collator;
import net.osmand.binary.BinaryMapPoiReaderAdapter;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.HeightDataLoader.InterfaceIsCancelled;
import net.osmand.plus.Version;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.resources.AmenityIndexRepository;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.wikivoyage.WikivoyageUtils;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.core.SearchSettings;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.shared.gpx.primitives.Link;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.util.KAlgorithms;
import net.osmand.shared.util.KMapAlgorithms;
import net.osmand.util.Algorithms;
import net.osmand.util.GpxTrackProcessor;
import net.osmand.util.MapUtils;
import net.osmand.util.OverlappedSegmentsMerger;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.reflect.Type;
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
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);
	private static final String WORLD_WIKIVOYAGE_FILE_NAME = "World_wikivoyage.travel.obf";
	private static final int ARTICLE_SEARCH_RADIUS = 50 * 1000;
	private static final int SAVED_ARTICLE_SEARCH_RADIUS = 30 * 1000;
	private static final int MAX_SEARCH_RADIUS = 800 * 1000;

	private final OsmandApplication app;
	private final Collator collator;

	private PopularArticles popularArticles = new PopularArticles();
	private final Map<TravelArticleIdentifier, Map<String, TravelArticle>> cachedArticles = new ConcurrentHashMap<>();
	private final TravelLocalDataHelper localDataHelper;
	private int searchRadius = ARTICLE_SEARCH_RADIUS;
	private int foundAmenitiesIndex;
	private final List<Pair<File, Amenity>> foundAmenities = new ArrayList<>();
	public volatile int requestNumber = 0;

	// Do not clutter GPX with tags that are always generated.
	private static final Set<String> doNotSaveAmenityGpxTags = Set.of(
			"date", "distance", "route_name", "route_bbox_radius",
			"avg_ele", "min_ele", "max_ele", "start_ele", "ele_graph", "diff_ele_up", "diff_ele_down",
			"avg_speed", "min_speed", "max_speed", "time_moving", "time_moving_no_gaps", "time_span", "time_span_no_gaps"
	);

	public static final String TAG_URL = "url";
	public static final String TAG_URL_TEXT = "url_text";
	public static final String WPT_EXTRA_TAGS = "wpt_extra_tags";
	private static final String METADATA_EXTRA_TAGS = "metadata_extra_tags";
	private static final String EXTENSIONS_EXTRA_TAGS = "extensions_extra_tags";

	public TravelObfHelper(@NonNull OsmandApplication app) {
		this.app = app;
		collator = OsmAndCollator.primaryCollator();
		localDataHelper = new TravelLocalDataHelper(app);
	}

	@NonNull
	@Override
	public TravelLocalDataHelper getBookmarksHelper() {
		return localDataHelper;
	}

	@Override
	public void initializeDataOnAppStartup() {
	}

	@Override
	public void initializeDataToDisplay(boolean resetData) {
		if (resetData) {
			foundAmenities.clear();
			foundAmenitiesIndex = 0;
			popularArticles.clear();
			searchRadius = ARTICLE_SEARCH_RADIUS;
		}
		localDataHelper.refreshCachedData();
		loadPopularArticles();
	}

	private synchronized void loadPopularArticles() {
		String lang = app.getLanguage();
		PopularArticles popularArticles = loadPopularArticlesForLang(lang);
		if (popularArticles.isEmpty()) {
			popularArticles = loadPopularArticlesForLang("en");
		}
		this.popularArticles = popularArticles;
	}

	@NonNull
	private synchronized PopularArticles loadPopularArticlesForLang(@NonNull String lang) {
		PopularArticles popularArticles = new PopularArticles(this.popularArticles);
		if (isAnyTravelBookPresent()) {
			boolean articlesLimitReached = false;
			do {
				if (foundAmenities.size() - foundAmenitiesIndex < ARTICLES_PER_PAGE) {
					LatLon location = app.getMapViewTrackingUtilities().getMapLocation();
					for (AmenityIndexRepository repo : getWikivoyageRepositories()) {
						searchAmenity(foundAmenities, location, repo, searchRadius, -1, ROUTE_ARTICLE, lang);
						searchAmenity(foundAmenities, location, repo, searchRadius / 5, 15, ROUTE_TRACK, null);
					}
					if (foundAmenities.size() > 0) {
						Collections.sort(foundAmenities, new Comparator<Pair<File, Amenity>>() {
							@Override
							public int compare(Pair article1, Pair article2) {
								Amenity amenity1 = (Amenity) article1.second;
								double d1 = MapUtils.getDistance(amenity1.getLocation(), location)
										/ (ROUTE_ARTICLE.equals(amenity1.getSubType()) ? 5 : 1);
								Amenity amenity2 = (Amenity) article2.second;
								double d2 = MapUtils.getDistance(amenity2.getLocation(), location)
										/ (ROUTE_ARTICLE.equals(amenity2.getSubType()) ? 5 : 1);
								return Double.compare(d1, d2);
							}
						});
					}
					searchRadius *= 2;
				}
				while (foundAmenitiesIndex < foundAmenities.size() - 1) {
					Pair<File, Amenity> fileAmenity = foundAmenities.get(foundAmenitiesIndex);
					File file = fileAmenity.first;
					Amenity amenity = fileAmenity.second;
					if (!Algorithms.isEmpty(amenity.getName(lang))) {
						String routeId = amenity.getAdditionalInfo(Amenity.ROUTE_ID);
						if (!popularArticles.containsByRouteId(routeId)) {
							TravelArticle article = cacheTravelArticles(file, amenity, lang, false, null);
							if (article != null && !popularArticles.contains(article)) {
								if (!popularArticles.add(article)) {
									articlesLimitReached = true;
									break;
								}
							}
						}
					}
					foundAmenitiesIndex++;
				}
			} while (!articlesLimitReached && searchRadius < MAX_SEARCH_RADIUS);
		}
		return popularArticles;
	}

	@Override
	public boolean isTravelGpxTags(@NonNull Map<String, String> tags) {
		return tags.containsKey(ROUTE_ID) && "segment".equals(tags.get(ROUTE));
	}

	@Nullable
	public synchronized TravelGpx searchTravelGpx(@NonNull LatLon location, @Nullable String routeId) {
		final String lcSearchRouteId = routeId != null ? routeId.toLowerCase() : null;
		if (Algorithms.isEmpty(lcSearchRouteId)) {
			LOG.error(String.format("searchTravelGpx(%s, null) failed due to empty routeId", location));
			return null;
		}
		List<Pair<File, Amenity>> foundAmenities = new ArrayList<>();
		int searchRadius = ARTICLE_SEARCH_RADIUS;
		TravelGpx travelGpx = null;
		do {
			for (AmenityIndexRepository repo : getTravelGpxRepositories()) {
				searchAmenity(foundAmenities, location, repo, searchRadius, 15, ROUTE_TRACK, null);
			}
			for (Pair<File, Amenity> foundGpx : foundAmenities) {
				Amenity amenity = foundGpx.second;
				final String aRouteId = amenity.getRouteId();
				final String lcRouteId = aRouteId != null ? aRouteId.toLowerCase() : null;
				if (lcSearchRouteId.equals(lcRouteId)) {
					travelGpx = getTravelGpx(foundGpx.first, amenity);
					break;
				}
			}
			searchRadius *= 2;
		} while (travelGpx == null && searchRadius < MAX_SEARCH_RADIUS);
		if (travelGpx == null) {
			LOG.error(String.format("searchTravelGpx(%s, %s) failed", location, routeId));
		}
		return travelGpx;
	}

	private void searchAmenity(@NonNull List<Pair<File, Amenity>> amenitiesList, @NonNull LatLon location,
	                           @NonNull AmenityIndexRepository repo, int searchRadius, int zoom,
	                           @NonNull String searchFilter, @Nullable String lang) {
		repo.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(
				location, searchRadius, zoom, getSearchFilter(searchFilter), new ResultMatcher<Amenity>() {
					@Override
					public boolean publish(Amenity object) {
						if (lang == null || object.getNamesMap(true).containsKey(lang)) {
							amenitiesList.add(new Pair<>(repo.getFile(), object));
						}
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				}));
	}

	@Nullable
	private TravelArticle cacheTravelArticles(@NonNull File file, @NonNull Amenity amenity, @Nullable String lang,
	                                          boolean readPoints, @Nullable GpxReadCallback callback) {
		TravelArticle article = null;
		Map<String, TravelArticle> articles;
		if (amenity.isRouteTrack()) {
			articles = readRoutePoint(file, amenity);
		} else {
			articles = readArticles(file, amenity);
		}
		if (!Algorithms.isEmpty(articles)) {
			TravelArticleIdentifier newArticleId = articles.values().iterator().next().generateIdentifier();
			cachedArticles.put(newArticleId, articles);
			article = getCachedArticle(newArticleId, lang, readPoints, callback);
		}
		return article;
	}

	@NonNull
	private Map<String, TravelArticle> readRoutePoint(@NonNull File file, @NonNull Amenity amenity) {
		Map<String, TravelArticle> articles = new HashMap<>();
		TravelGpx res = getTravelGpx(file, amenity);
		articles.put("", res);
		return articles;
	}

	@NonNull
	private TravelGpx getTravelGpx(@NonNull File file, @NonNull Amenity amenity) {
		TravelGpx travelGpx = new TravelGpx();
		travelGpx.file = file;
		String title = amenity.getName("en");
		travelGpx.title = Algorithms.isEmpty(title) ? amenity.getName() : title;
		travelGpx.lat = amenity.getLocation().getLatitude();
		travelGpx.lon = amenity.getLocation().getLongitude();
		travelGpx.description = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.DESCRIPTION));
		travelGpx.routeId = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID));
		travelGpx.user = Algorithms.emptyIfNull(amenity.getTagContent(USER));
		travelGpx.activityType = Algorithms.emptyIfNull(amenity.getTagContent(ROUTE_ACTIVITY_TYPE));
		travelGpx.ref = Algorithms.emptyIfNull(amenity.getRef());
		travelGpx.totalDistance = Algorithms.parseFloatSilently(amenity.getTagContent(DISTANCE), 0);
		travelGpx.diffElevationUp = Algorithms.parseDoubleSilently(amenity.getTagContent(DIFF_ELEVATION_UP), 0);
		travelGpx.diffElevationDown = Algorithms.parseDoubleSilently(amenity.getTagContent(DIFF_ELEVATION_DOWN), 0);
		travelGpx.minElevation = Algorithms.parseDoubleSilently(amenity.getTagContent(MIN_ELEVATION), 0);
		travelGpx.avgElevation = Algorithms.parseDoubleSilently(amenity.getTagContent(AVERAGE_ELEVATION), 0);
		travelGpx.maxElevation = Algorithms.parseDoubleSilently(amenity.getTagContent(MAX_ELEVATION), 0);
		String radius = amenity.getTagContent(ROUTE_BBOX_RADIUS);
		if (radius != null) {
			travelGpx.routeRadius = MapUtils.convertCharToDist(radius.charAt(0), TRAVEL_GPX_CONVERT_FIRST_LETTER,
					TRAVEL_GPX_CONVERT_FIRST_DIST, TRAVEL_GPX_CONVERT_MULT_1, TRAVEL_GPX_CONVERT_MULT_2);
		}
		return travelGpx;
	}

	@NonNull
	private static SearchPoiTypeFilter getSearchFilter(@NonNull String... filterSubcategories) {
		return new SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				for (String filter : filterSubcategories) {
					if (subcategory.equals(filter)) {
						return true;
					}
					if (ROUTE_TRACK.equals(filter) && subcategory.startsWith(ROUTES_PREFIX)) {
						return true; // include routes:routes_xxx with routes:route_track filter
					}
				}
				return false;
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
	private TravelArticle readArticle(@NonNull File file, @NonNull Amenity amenity, @NonNull String lang) {
		TravelArticle res = new TravelArticle();
		res.file = file;
		String title = amenity.getName(lang);
		res.title = Algorithms.isEmpty(title) ? amenity.getName() : title;
		res.content = amenity.getDescription(lang);
		res.isPartOf = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.IS_PART, lang));
		res.isParentOf = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.IS_PARENT_OF, lang));
		res.lat = amenity.getLocation().getLatitude();
		res.lon = amenity.getLocation().getLongitude();
		res.imageTitle = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.IMAGE_TITLE));
		res.routeId = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID));
		res.routeSource = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_SOURCE));
		res.originalId = 0;
		res.lang = lang;
		res.contentsJson = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.CONTENT_JSON, lang));
		res.aggregatedPartOf = Algorithms.emptyIfNull(amenity.getStrictTagContent(Amenity.IS_AGGR_PART, lang));
		return res;
	}

	@Override
	public boolean isAnyTravelBookPresent() {
		return !app.isApplicationInitializing() && !app.getResourceManager().isWikivoyageRepositoryEmpty();
	}

	@NonNull
	@Override
	public synchronized List<WikivoyageSearchResult> search(@NonNull String searchQuery, int requestNumber) {
		String appLang = app.getLanguage();
		List<WikivoyageSearchResult> res = searchWithLang(searchQuery, appLang, requestNumber);
		if (Algorithms.isEmpty(res)) {
			res = searchWithLang(searchQuery, "en", requestNumber);
		}
		return res;
	}

	@NonNull
	private synchronized List<WikivoyageSearchResult> searchWithLang(@NonNull String searchQuery, @NonNull String appLang, int reqNumber) {
		List<WikivoyageSearchResult> res = new ArrayList<>();
		Map<File, List<Amenity>> amenityMap = new HashMap<>();
		SearchUICore searchUICore = app.getSearchUICore().getCore();
		SearchSettings settings = searchUICore.getSearchSettings();
		SearchPhrase phrase = searchUICore.getPhrase().generateNewPhrase(searchQuery, settings);
		NameStringMatcher matcher = phrase.getFirstUnknownNameStringMatcher();
		List<WikivoyageSearchResult> empty = new ArrayList<>();

		for (AmenityIndexRepository repo : getWikivoyageRepositories()) {
			if (requestNumber != reqNumber) {
				return empty;
			}
			List<BinaryMapPoiReaderAdapter.PoiRegion> poiIndexes = repo.getReaderPoiIndexes();
			QuadRect bbox = new QuadRect();
			for (BinaryMapPoiReaderAdapter.PoiRegion poiRegion : poiIndexes) {
				bbox.expand(poiRegion.getLeft31(), poiRegion.getTop31(), poiRegion.getRight31(), poiRegion.getBottom31());
			}
			SearchRequest<Amenity> searchRequest = BinaryMapIndexReader.buildSearchPoiRequest(0, 0, searchQuery,
					(int) bbox.left, (int) bbox.right, (int) bbox.top, (int) bbox.bottom, getSearchFilter(ROUTE_ARTICLE), new ResultMatcher<Amenity>() {
						@Override
						public boolean publish(Amenity object) {
							List<String> otherNames = object.getOtherNames(false);
							String localeName = object.getName(appLang);
							return matcher.matches(localeName) || matcher.matches(otherNames);
						}

						@Override
						public boolean isCancelled() {
							return requestNumber != reqNumber;
						}
					}, null);

			List<Amenity> amenities = repo.searchPoiByName(searchRequest);
			if (requestNumber != reqNumber) {
				return empty;
			}
			if (!Algorithms.isEmpty(amenities)) {
				amenityMap.put(repo.getFile(), amenities);
			}
		}
		if (!Algorithms.isEmpty(amenityMap)) {
			boolean appLangEn = "en".equals(appLang);
			TLongSet uniqueIds = new TLongHashSet();
			for (Entry<File, List<Amenity>> entry : amenityMap.entrySet()) {
				File file = entry.getKey();
				for (Amenity amenity : entry.getValue()) {
					long routeId = Algorithms.parseLongSilently(amenity.getRouteId().replace("Q", ""), -1);
					if (!uniqueIds.add(routeId)) {
						continue;
					}
					Set<String> nameLangs = getLanguages(amenity);
					if (nameLangs.contains(appLang) || Algorithms.isEmpty(appLang)) {
						TravelArticle article = readArticle(file, amenity, appLang);
						ArrayList<String> langs = new ArrayList<>(nameLangs);
						Collections.sort(langs, (l1, l2) -> {
							if (l1.equals(appLang)) {
								l1 = "1";
							}
							if (l2.equals(appLang)) {
								l2 = "1";
							}
							if (!appLangEn) {
								if (l1.equals("en")) {
									l1 = "2";
								}
								if (l2.equals("en")) {
									l2 = "2";
								}
							}
							return l1.compareTo(l2);
						});
						WikivoyageSearchResult r = new WikivoyageSearchResult(article, langs);
						res.add(r);
					}
				}
			}
			sortSearchResults(res, searchQuery);
		}
		return res;
	}

	@NonNull
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

	private void sortSearchResults(@NonNull List<WikivoyageSearchResult> results, @NonNull String searchQuery) {
		results.sort(new SearchResultComparator(searchQuery, collator));
	}

	private static class SearchResultComparator implements Comparator<WikivoyageSearchResult> {
		private final Collator collator;
		private final String searchQuery;
		private final String searchQueryLC;


		public SearchResultComparator(@NonNull String searchQuery, @NonNull Collator collator) {
			this.searchQuery = searchQuery;
			this.collator = collator;
			searchQueryLC = searchQuery.toLowerCase();
		}

		@Override
		public int compare(@NonNull WikivoyageSearchResult sr1, @NonNull WikivoyageSearchResult sr2) {
			for (ResultCompareStep step : ResultCompareStep.values()) {
				int res = step.compare(sr1, sr2, this);
				if (res != 0) {
					return res;
				}
			}
			return 0;
		}

	}

	private enum ResultCompareStep {
		MACH_TITLE,
		CONTAINS_OF_TITLE,
		OTHER;

		// -1 - means 1st is less (higher list position) than 2nd
		public int compare(@NonNull WikivoyageSearchResult sr1, @NonNull WikivoyageSearchResult sr2,
		                   @NonNull SearchResultComparator c) {
			String articleTitle1 = sr1.getArticleTitle();
			String articleTitle2 = sr2.getArticleTitle();
			boolean sr1Comparison = !c.collator.equals(articleTitle1, c.searchQuery.trim());
			boolean sr2Comparison = !c.collator.equals(articleTitle2, c.searchQuery.trim());
			switch (this) {
				case MACH_TITLE:
					return Boolean.compare(sr1Comparison, sr2Comparison);
				case CONTAINS_OF_TITLE:
					boolean title1contains = articleTitle1.toLowerCase().contains(c.searchQueryLC);
					boolean title2contains = articleTitle2.toLowerCase().contains(c.searchQueryLC);
					return -Boolean.compare(title1contains, title2contains);
				case OTHER:
					int comp = c.collator.compare(articleTitle1, articleTitle2);
					return (comp != 0) ? comp : c.collator.compare(sr1.isPartOf, sr2.isPartOf);
			}
			return 0;
		}
	}

	@NonNull
	@Override
	public List<TravelArticle> getPopularArticles() {
		return popularArticles.getArticles();
	}

	@NonNull
	@Override
	public synchronized Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> getNavigationMap(@NonNull TravelArticle article) {
		String lang = article.getLang();
		String title = article.getTitle();
		if (TextUtils.isEmpty(lang) || TextUtils.isEmpty(title)) {
			return Collections.emptyMap();
		}
		String[] parts;
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
		Set<String> headers = new LinkedHashSet<>();
		Map<String, WikivoyageSearchResult> headerObjs = new HashMap<>();
		if (parts != null && parts.length > 0) {
			headers.addAll(Arrays.asList(parts));
			if (!Algorithms.isEmpty(article.isParentOf)) {
				headers.add(title);
			}
		}

		for (String header : headers) {
			String parentLang = header.startsWith("en:") ? "en" : lang;
			header = WikivoyageUtils.getTitleWithoutPrefix(header);
			TravelArticle parentArticle = getParentArticleByTitle(header, parentLang);
			if (parentArticle == null) {
				continue;
			}
			navMap.put(header, new ArrayList<>());
			String[] isParentOf = parentArticle.isParentOf.split(";");
			for (String childTitle : isParentOf) {
				if (!childTitle.isEmpty()) {
					WikivoyageSearchResult searchResult = new WikivoyageSearchResult("", childTitle, null,
							null, Collections.singletonList(parentLang));
					List<WikivoyageSearchResult> resultList = navMap.computeIfAbsent(header, k -> new ArrayList<>());
					resultList.add(searchResult);
					if (headers.contains(childTitle)) {
						headerObjs.put(childTitle, searchResult);
					}
				}
			}
		}

		Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> res = new LinkedHashMap<>();
		for (String header : headers) {
			String parentLang = header.startsWith("en:") ? "en" : lang;
			header = WikivoyageUtils.getTitleWithoutPrefix(header);
			WikivoyageSearchResult searchResult = headerObjs.get(header);
			List<WikivoyageSearchResult> results = navMap.get(header);
			if (results != null) {
				sortSearchResults(results, header);
				WikivoyageSearchResult emptyResult = new WikivoyageSearchResult("", header, null,
						null, Collections.singletonList(parentLang));
				searchResult = searchResult != null ? searchResult : emptyResult;
				res.put(searchResult, results);
			}
		}
		return res;
	}

	@Nullable
	private TravelArticle getParentArticleByTitle(@NonNull String title, @NonNull String lang) {
		TravelArticle article = null;
		List<Amenity> amenities = new ArrayList<>();
		for (AmenityIndexRepository repo : getWikivoyageRepositories()) {
			SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
					0, 0, title, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, getSearchFilter(ROUTE_ARTICLE),
					new ResultMatcher<Amenity>() {
						boolean done;

						@Override
						public boolean publish(Amenity amenity) {
							if (Algorithms.stringsEqual(title, Algorithms.emptyIfNull(amenity.getName(lang)))) {
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
			repo.searchPoiByName(req);
			if (!Algorithms.isEmpty(amenities)) {
				article = readArticle(repo.getFile(), amenities.get(0), lang);
				break;
			}
		}
		return article;
	}

	@Nullable
	@Override
	public TravelArticle getArticleById(@NonNull TravelArticleIdentifier articleId, @Nullable String lang,
	                                    boolean readGpx, @Nullable GpxReadCallback callback) {
		TravelArticle article = getCachedArticle(articleId, lang, readGpx, callback);
		if (article == null) {
			article = localDataHelper.getSavedArticle(articleId.file, articleId.routeId, lang);
			if (article != null && callback != null && readGpx) {
				callback.onGpxFileRead(article.gpxFile);
			}
		}
		return article;
	}

	@Nullable
	private TravelArticle getCachedArticle(@NonNull TravelArticleIdentifier articleId, @Nullable String lang,
	                                       boolean readGpx, @Nullable GpxReadCallback callback) {
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
		if (article == null && articles == null) {
			article = findArticleById(articleId, lang, readGpx, callback);
		}
		if (article != null && readGpx && (!Algorithms.isEmpty(lang) || article instanceof TravelGpx)) {
			readGpxFile(article, callback);
		}
		return article;
	}

	@Override
	public void openTrackMenu(@NonNull TravelArticle article, @NonNull MapActivity mapActivity,
	                          @NonNull String gpxFileName, @NonNull LatLon latLon, boolean adjustMapPosition) {
		GpxReadCallback callback = new GpxReadCallback() {
			@Override
			public void onGpxFileReading() {
			}

			@Override
			public void onGpxFileRead(@Nullable GpxFile gpxFile) {
				if (gpxFile != null) {
					WptPt wptPt = new WptPt();
					wptPt.setLat(latLon.getLatitude());
					wptPt.setLon(latLon.getLongitude());

					String name = gpxFileName.endsWith(GPX_FILE_EXT) ? gpxFileName : gpxFileName + GPX_FILE_EXT;
					File file = new File(FileUtils.getTempDir(app), name);
					GpxUiHelper.saveAndOpenGpx(mapActivity, file, gpxFile, wptPt, article.getAnalysis(), null, adjustMapPosition);
				}
			}
		};
		readGpxFile(article, callback);
	}

	private void readGpxFile(@NonNull TravelArticle article, @Nullable GpxReadCallback callback) {
		if (!article.gpxFileRead) {
			if (!app.isApplicationInitializing()) {
				MapActivity mapActivity = app.getOsmandMap().getMapView().getMapActivity();
				if (mapActivity != null) {
					new GpxFileReader(mapActivity, article, callback, getTravelGpxRepositories())
							.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
			}
		} else if (callback != null) {
			callback.onGpxFileRead(article.gpxFile);
		}
	}

	@Nullable
	private synchronized TravelArticle findArticleById(@NonNull TravelArticleIdentifier articleId,
	                                                   @Nullable String lang, boolean readGpx,
	                                                   @Nullable GpxReadCallback callback) {
		TravelArticle article = null;
		boolean isDbArticle = articleId.file != null && articleId.file.getName().endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT);
		List<Amenity> amenities = new ArrayList<>();
		for (AmenityIndexRepository repo : getWikivoyageRepositories()) {
			if (articleId.file != null && !articleId.file.equals(repo.getFile()) && !isDbArticle) {
				continue;
			}
			SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
					Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
					getSearchFilter(ROUTE_ARTICLE), new ResultMatcher<Amenity>() {
						boolean done;

						@Override
						public boolean publish(Amenity amenity) {
							if (Algorithms.stringsEqual(articleId.routeId,
									Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID))) || isDbArticle) {
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
					repo.searchPoiByName(req);
				} else {
					repo.searchPoi(req);
				}
			} else {
				repo.searchPoi(req);
			}
			if (!Algorithms.isEmpty(amenities)) {
				article = cacheTravelArticles(repo.getFile(), amenities.get(0), lang, readGpx, callback);
			}
		}
		return article;
	}

	@Nullable
	@Override
	public synchronized TravelArticle findSavedArticle(@NonNull TravelArticle savedArticle) {
		List<Pair<File, Amenity>> amenities = new ArrayList<>();
		TravelArticle article = null;
		final TravelArticleIdentifier articleId = savedArticle.generateIdentifier();
		String lang = savedArticle.getLang();
		long lastModified = savedArticle.getLastModified();
		SearchRequest<Amenity> req = null;
		for (AmenityIndexRepository repo : getWikivoyageRepositories()) {
			if (articleId.file != null && articleId.file.equals(repo.getFile())) {
				if (lastModified == repo.getFile().lastModified()) {
					req = BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
							Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
							getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK), new ResultMatcher<Amenity>() {
								boolean done;

								@Override
								public boolean publish(Amenity amenity) {
									if (Algorithms.stringsEqual(articleId.routeId,
											Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID)))) {
										amenities.add(new Pair<>(repo.getFile(), amenity));
										done = true;
									}
									return false;
								}

								@Override
								public boolean isCancelled() {
									return done;
								}
							}, null);
					req.setBBoxRadius(articleId.lat, articleId.lon, ARTICLE_SEARCH_RADIUS);
				} else {
					if (!Algorithms.isEmpty(articleId.title)) {
						req = getEqualsTitleRequest(articleId, lang, amenities, repo.getFile());
						req.setBBoxRadius(articleId.lat, articleId.lon, ARTICLE_SEARCH_RADIUS / 10);
					}
				}
			}
			if (req != null) {
				if (!Double.isNaN(articleId.lat)) {
					if (!Algorithms.isEmpty(articleId.title)) {
						repo.searchPoiByName(req);
					} else {
						repo.searchPoi(req);
					}
				} else {
					repo.searchPoi(req);
				}
				break;
			}
		}
		if (amenities.isEmpty() && !Algorithms.isEmpty(articleId.title)) {
			for (AmenityIndexRepository repo : getWikivoyageRepositories()) {
				req = getEqualsTitleRequest(articleId, lang, amenities, repo.getFile());
				req.setBBoxRadius(articleId.lat, articleId.lon, SAVED_ARTICLE_SEARCH_RADIUS);
				if (!Double.isNaN(articleId.lat)) {
					repo.searchPoiByName(req);
				} else {
					repo.searchPoi(req);
				}
			}
		}
		if (amenities.isEmpty()) {
			for (AmenityIndexRepository repo : getWikivoyageRepositories()) {
				req = BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
						Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
						getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK), new ResultMatcher<Amenity>() {
							boolean done;

							@Override
							public boolean publish(Amenity amenity) {
								if (Algorithms.stringsEqual(articleId.routeId,
										Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID)))
										&& Algorithms.stringsEqual(articleId.routeSource,
										Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_SOURCE)))) {
									amenities.add(new Pair<>(repo.getFile(), amenity));
									done = true;
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return done;
							}
						}, null);
				req.setBBoxRadius(articleId.lat, articleId.lon, SAVED_ARTICLE_SEARCH_RADIUS);
				if (!Double.isNaN(articleId.lat)) {
					if (!Algorithms.isEmpty(articleId.title)) {
						repo.searchPoiByName(req);
					} else {
						repo.searchPoi(req);
					}
				} else {
					repo.searchPoi(req);
				}
			}
		}
		if (!Algorithms.isEmpty(amenities)) {
			article = cacheTravelArticles(amenities.get(0).first, amenities.get(0).second, lang, false, null);
		}
		return article;
	}

	@NonNull
	private SearchRequest<Amenity> getEqualsTitleRequest(@NonNull TravelArticleIdentifier articleId,
	                                                     @Nullable String lang,
	                                                     @NonNull List<Pair<File, Amenity>> amenities,
	                                                     @NonNull File readerFile) {
		return BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
				Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
				getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK), new ResultMatcher<Amenity>() {
					boolean done;

					@Override
					public boolean publish(Amenity amenity) {
						if (Algorithms.stringsEqual(Algorithms.emptyIfNull(articleId.title),
								Algorithms.emptyIfNull(amenity.getName(lang)))) {
							amenities.add(new Pair<>(readerFile, amenity));
							done = true;
						}
						return false;
					}

					@Override
					public boolean isCancelled() {
						return done;
					}
				}, null);
	}

	@Nullable
	@Override
	public TravelArticle getArticleByTitle(@NonNull String title, @NonNull String lang,
	                                       boolean readGpx, @Nullable GpxReadCallback callback) {
		return getArticleByTitle(title, new QuadRect(), lang, readGpx, callback);
	}

	@Nullable
	@Override
	public TravelArticle getArticleByTitle(@NonNull String title, @NonNull LatLon latLon,
	                                       @NonNull String lang, boolean readGpx, @Nullable GpxReadCallback callback) {
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), ARTICLE_SEARCH_RADIUS);
		return getArticleByTitle(title, rect, lang, readGpx, callback);
	}

	@Nullable
	@Override
	public synchronized TravelArticle getArticleByTitle(@NonNull String title, @NonNull QuadRect rect,
	                                                    @NonNull String lang, boolean readGpx, @Nullable GpxReadCallback callback) {
		TravelArticle article = null;
		List<Amenity> amenities = new ArrayList<>();
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
		for (AmenityIndexRepository repo : getWikivoyageRepositories()) {
			SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
					x, y, title, left, right, top, bottom, getSearchFilter(ROUTE_ARTICLE),
					new ResultMatcher<Amenity>() {
						boolean done;

						@Override
						public boolean publish(Amenity amenity) {
							if (Algorithms.stringsEqual(title, Algorithms.emptyIfNull(amenity.getName(lang)))) {
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
			repo.searchPoiByName(req);
			if (!Algorithms.isEmpty(amenities)) {
				article = cacheTravelArticles(repo.getFile(), amenities.get(0), lang, readGpx, callback);
				break;
			}
		}
		return article;
	}

	@NonNull
	private List<AmenityIndexRepository> getWikivoyageRepositories() {
		if (!app.isApplicationInitializing()) {
			return app.getResourceManager().getWikivoyageRepositories();
		} else {
			return new ArrayList<>();
		}
	}

	@NonNull
	private List<AmenityIndexRepository> getTravelGpxRepositories() {
		if (!app.isApplicationInitializing()) {
			return app.getResourceManager().getTravelGpxRepositories();
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
			TravelArticle article = getArticleByTitle(title, lang, false, null);
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
		TravelArticle article = getArticleById(articleId, "", false, null);
		if (article != null) {
			Map<String, TravelArticle> articles = cachedArticles.get(article.generateIdentifier());
			if (articles != null) {
				res.addAll(articles.keySet());
			}
		} else {
			List<TravelArticle> articles = localDataHelper.getSavedArticles(articleId.file, articleId.routeId);
			for (TravelArticle a : articles) {
				res.add(a.getLang());
			}
		}
		return res;
	}

	@NonNull
	@Override
	public String getGPXName(@NonNull TravelArticle article) {
		return article.getGpxFileName() + GPX_FILE_EXT;
	}

	@NonNull
	@Override
	public File createGpxFile(@NonNull TravelArticle article) {
		GpxFile gpx;
		gpx = article.getGpxFile();
		File file = app.getAppPath(IndexConstants.GPX_TRAVEL_DIR + getGPXName(article));
		SharedUtil.writeGpxFile(file, gpx);
		return file;
	}

	@Nullable
	@Override
	public String getSelectedTravelBookName() {
		return null;
	}

	@NonNull
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

	private boolean fetchSegmentsAndPoints(@NonNull List<AmenityIndexRepository> repos,
	                                       @NonNull TravelArticle article,
	                                       @NonNull List<BinaryMapDataObject> segmentList,
	                                       @NonNull List<Amenity> pointList,
	                                       @NonNull Map<String, String> gpxFileExtensions,
	                                       @NonNull List<String> pgNames,
	                                       @NonNull List<String> pgIcons,
	                                       @NonNull List<String> pgColors,
	                                       @NonNull List<String> pgBackgrounds,
	                                       @NonNull InterfaceIsCancelled isCancelled) {
		boolean allowReadFromMultipleMaps = article.hasOsmRouteId() && article.routeRadius > 0;
		for (AmenityIndexRepository repo : repos) {
			try {
				if (isCancelled.isCancelled()) {
					return false;
				}
				if (!allowReadFromMultipleMaps &&
						!Algorithms.objectEquals(repo.getFile(), article.file)) {
					continue; // speed up reading of Wikivoyage and User's GPX files in OBF
				}
				if (article instanceof TravelGpx) {
					BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> sr = BinaryMapIndexReader.buildSearchRequest(
							0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 15, null,
							matchSegmentsByRefTitleRouteId(article, segmentList, isCancelled));
					if (article.routeRadius >= 0) {
						sr.setBBoxRadius(article.lat, article.lon, article.routeRadius);
					}
					repo.searchMapIndex(sr); // TODO radius is excessive; consider route_bbox_latlon
				}
				BinaryMapIndexReader.SearchRequest<Amenity> pointRequest = BinaryMapIndexReader.buildSearchPoiRequest(
						0, 0, Algorithms.emptyIfNull(article.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
						getSearchFilter(article.getMainFilterString(), article.getPointFilterString()),
						matchPointsAndTags(article, pointList, gpxFileExtensions, pgNames, pgIcons, pgColors, pgBackgrounds, isCancelled),
						null);
				if (article.routeRadius >= 0) {
					pointRequest.setBBoxRadius(article.lat, article.lon, article.routeRadius);
				}
				if (!Algorithms.isEmpty(article.title)) {
					repo.searchPoiByName(pointRequest);
				} else {
					repo.searchPoi(pointRequest);
				}
				if (!allowReadFromMultipleMaps && !Algorithms.isEmpty(segmentList)) {
					break; // speed up reading of User's GPX files
				}
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
		}
		return !isCancelled.isCancelled(); // might be partially succeed
	}

	@NonNull
	private ResultMatcher<Amenity> matchPointsAndTags(@NonNull TravelArticle article,
	                                                  @NonNull List<Amenity> pointList,
	                                                  @NonNull Map<String, String> gpxFileExtensions,
	                                                  @NonNull List<String> pgNames,
	                                                  @NonNull List<String> pgIcons,
	                                                  @NonNull List<String> pgColors,
	                                                  @NonNull List<String> pgBackgrounds,
	                                                  @NonNull InterfaceIsCancelled isCancelled) {
		return new ResultMatcher<Amenity>() {
			boolean isAlreadyProcessed = false;
			@Override
			public boolean publish(Amenity amenity) {
				if (amenity.getRouteId().equals(article.getRouteId())) {
					if (amenity.isRouteTrack()) {
						if (!isAlreadyProcessed) {
							isAlreadyProcessed = true;
							reconstructActivityFromAmenity(amenity, gpxFileExtensions);
							amenity.getNamesMap(true).forEach((lang, value) ->
									{
										if (!"ref".equals(lang) && !"sym".equals(lang)) {
											gpxFileExtensions.put("name:" + lang, value);
										}
									}
							);
							for (String tag : amenity.getAdditionalInfoKeys()) {
								String value = amenity.getAdditionalInfo(tag);
								if (tag.startsWith(OBF_POINTS_GROUPS_PREFIX)) {
									List<String> values = Arrays.asList(value.split(OBF_POINTS_GROUPS_DELIMITER));
									switch (tag) {
										case OBF_POINTS_GROUPS_NAMES -> pgNames.addAll(values);
										case OBF_POINTS_GROUPS_ICONS -> pgIcons.addAll(values);
										case OBF_POINTS_GROUPS_COLORS -> pgColors.addAll(values);
										case OBF_POINTS_GROUPS_BACKGROUNDS -> pgBackgrounds.addAll(values);
									}
								} else if (!doNotSaveAmenityGpxTags.contains(tag)) {
									gpxFileExtensions.put(tag, value);
								}
							}
						}
					} else if (ROUTE_TRACK_POINT.equals(amenity.getSubType())) {
						pointList.add(amenity);
					} else {
						String amenityLang = amenity.getTagSuffix(Amenity.LANG_YES + ":");
						if (Algorithms.stringsEqual(article.lang, amenityLang)) {
							pointList.add(amenity);
						}
					}
				}
				return false;
			}
			@Override
			public boolean isCancelled() {
				return isCancelled.isCancelled();
			}
		};
	}

	private void reconstructActivityFromAmenity(@NonNull Amenity amenity,
	                                            @NonNull Map<String, String> gpxFileExtensions) {
		if (amenity.isRouteTrack() && amenity.getSubType() != null) {
			String subType = amenity.getSubType();
			if (subType.startsWith(ROUTES_PREFIX)) {
				String osmValue = amenity.getType().getPoiTypeByKeyName(subType).getOsmValue();
				if (!Algorithms.isEmpty(osmValue)) {
					if (amenity.hasOsmRouteId() || !"other".equals(osmValue)) {
						gpxFileExtensions.put(ROUTE_TYPE, osmValue); // do not litter gpx with default route_type
					}
					RouteActivityHelper helper = app.getRouteActivityHelper();
					for (String key : amenity.getAdditionalInfoKeys()) {
						if (key.startsWith(ROUTE_ACTIVITY_TYPE + "_")) {
							String activityType = amenity.getAdditionalInfo(key);
							if (!activityType.isEmpty() && helper.findRouteActivity(activityType) != null) {
								gpxFileExtensions.put(GpxUtilities.ACTIVITY_TYPE, activityType); // osmand:activity in gpx
								break;
							}
						}
					}
				}
			}
		}
	}

	@NonNull
	private ResultMatcher<BinaryMapDataObject> matchSegmentsByRefTitleRouteId(
			@NonNull TravelArticle article,
			@NonNull List<BinaryMapDataObject> segmentList,
			@NonNull InterfaceIsCancelled isCancelled) {
		return new ResultMatcher<BinaryMapDataObject>() {
			@Override
			public boolean publish(BinaryMapDataObject object) {
				if (object.getPointsLength() > 1) {
					String routeId = article.getRouteId();
					boolean equalRouteId = !Algorithms.isEmpty(routeId) && routeId.equals(object.getTagValue(ROUTE_ID));

					if (article instanceof TravelGpx && equalRouteId) {
						segmentList.add(object); // GPX-in-OBF requires mandatory route_id
					} else {
						String name = article.getTitle(), ref = article.ref;
						boolean equalName = !Algorithms.isEmpty(name) && name.equals(object.getName());
						boolean equalRef = !Algorithms.isEmpty(ref) && ref.equals(object.getTagValue(REF));
						if ((equalRouteId && (equalRef || equalName) || (equalRef && equalName))) {
							segmentList.add(object); // Wikivoyage is allowed to match mixed tags
						}
					}
				}
				return false;
			}
			@Override
			public boolean isCancelled() {
				return isCancelled.isCancelled();
			}
		};
	}

	@Nullable
	private synchronized GpxFile buildGpxFile(@NonNull List<AmenityIndexRepository> repos,
	                                          @NonNull TravelArticle article,
	                                          @NonNull InterfaceIsCancelled isCancelled) {
		List<BinaryMapDataObject> segmentList = new ArrayList<>();
		Map<String, String> gpxFileExtensions = new TreeMap<>();
		List<Amenity> pointList = new ArrayList<>();
		List<String> pgNames = new ArrayList<>();
		List<String> pgIcons = new ArrayList<>();
		List<String> pgColors = new ArrayList<>();
		List<String> pgBackgrounds = new ArrayList<>();

		boolean loaded = fetchSegmentsAndPoints(repos, article, segmentList, pointList, gpxFileExtensions,
				pgNames, pgIcons, pgColors, pgBackgrounds, isCancelled);

		if (!loaded || isCancelled.isCancelled()) {
			return null;
		}

		GpxFile gpxFile;
		if (article instanceof TravelGpx) {
			gpxFile = new GpxFile(Version.getFullVersion(app));
			gpxFile.getMetadata().setName(Objects.requireNonNullElse(article.title, article.routeId)); // path is name
			if (!Algorithms.isEmpty(article.title) && article.hasOsmRouteId()) {
				gpxFileExtensions.putIfAbsent("name", article.title);
			}
			if (!Algorithms.isEmpty(article.description)) {
				gpxFile.getMetadata().setDesc(article.description);
			}
		} else {
			String description = article.getDescription();
			String title = FileUtils.isValidFileName(description) ? description : article.getTitle();
			gpxFile = new GpxFile(title, article.getLang(), article.getContent());
		}

		if (gpxFileExtensions.containsKey(TAG_URL) && gpxFileExtensions.containsKey(TAG_URL_TEXT)) {
			gpxFile.getMetadata().setLink(new Link(gpxFileExtensions.get(TAG_URL), gpxFileExtensions.get(TAG_URL_TEXT)));
			gpxFileExtensions.remove(TAG_URL_TEXT);
			gpxFileExtensions.remove(TAG_URL);
		} else if (gpxFileExtensions.containsKey(TAG_URL)) {
			gpxFile.getMetadata().setLink(new Link(gpxFileExtensions.get(TAG_URL)));
			gpxFileExtensions.remove(TAG_URL);
		}

		if (!Algorithms.isEmpty(article.getImageTitle())) {
			gpxFile.getMetadata().setLink(new Link(TravelArticle.getImageUrl(article.getImageTitle(), false)));
		}

		if (!segmentList.isEmpty()) {
			boolean hasAltitude = false;
			Track track = new Track();
			for (BinaryMapDataObject segment : segmentList) {
				TrkSegment trkSegment = new TrkSegment();
				for (int i = 0; i < segment.getPointsLength(); i++) {
					WptPt point = new WptPt();
					point.setLat(MapUtils.get31LatitudeY(segment.getPoint31YTile(i)));
					point.setLon(MapUtils.get31LongitudeX(segment.getPoint31XTile(i)));
					trkSegment.getPoints().add(point);
				}
				String ele_graph = segment.getTagValue(ELE_GRAPH);
				if (!Algorithms.isEmpty(ele_graph)) {
					hasAltitude = true;
					List<Integer> heightRes = KMapAlgorithms.INSTANCE.decodeIntHeightArrayGraph(ele_graph, 3);
					double startEle = Algorithms.parseDoubleSilently(segment.getTagValue(START_ELEVATION), 0);
					KMapAlgorithms.INSTANCE.augmentTrkSegmentWithAltitudes(trkSegment, heightRes, startEle);
				}
				track.getSegments().add(trkSegment);
			}
			gpxFile.setTracks(new ArrayList<>());
//			gpxFile.getTracks().add(OverlappedSegmentsMerger.mergeSegmentsWithOverlapHandling(track));
			gpxFile.getTracks().add(GpxTrackProcessor.processTrack(track));
			if (!(article instanceof TravelGpx)) {
				gpxFile.setRef(article.ref);
			}
			gpxFile.setHasAltitude(hasAltitude);
			if (gpxFileExtensions.containsKey(GpxUtilities.ACTIVITY_TYPE)) {
				final String activityType =  gpxFileExtensions.get(GpxUtilities.ACTIVITY_TYPE);
				gpxFile.getMetadata().getExtensionsToWrite().put(GpxUtilities.ACTIVITY_TYPE, activityType);

				// cleanup type and activity tags
				gpxFileExtensions.remove(ROUTE_TYPE);
				gpxFileExtensions.remove(ROUTE_ACTIVITY_TYPE);
				gpxFileExtensions.remove(ROUTE_ACTIVITY_TYPE + "_" + activityType);
				gpxFileExtensions.remove(GpxUtilities.ACTIVITY_TYPE); // moved to the metadata
			}

			Gson gson = new Gson();
			Type type = new TypeToken<Map<String, String>>() {}.getType();
			if (gpxFileExtensions.containsKey(EXTENSIONS_EXTRA_TAGS)) {
				Map<String, String> jsonMap = gson.fromJson(gpxFileExtensions.get(EXTENSIONS_EXTRA_TAGS), type);
				if (jsonMap != null) {
					gpxFile.getExtensionsToWrite().putAll(jsonMap);
				}
				gpxFileExtensions.remove(EXTENSIONS_EXTRA_TAGS);
			}
			if (gpxFileExtensions.containsKey(METADATA_EXTRA_TAGS)) {
				Map<String, String> jsonMap = gson.fromJson(gpxFileExtensions.get(METADATA_EXTRA_TAGS), type);
				if (jsonMap != null) {
					gpxFile.getMetadata().getExtensionsToWrite().putAll(jsonMap);
				}
				gpxFileExtensions.remove(METADATA_EXTRA_TAGS);
			}

			gpxFile.getExtensionsToWrite().putAll(gpxFileExtensions); // finally
		}
		reconstructPointsGroups(gpxFile, pgNames, pgIcons, pgColors, pgBackgrounds); // create groups before points
		if (!pointList.isEmpty()) {
			for (Amenity wayPoint : pointList) {
				gpxFile.addPoint(article.createWptPt(wayPoint, article.getLang()));
			}
		}

		article.gpxFile = gpxFile;
		return gpxFile;
	}

	private void reconstructPointsGroups(@NonNull GpxFile gpxFile,
	                                     @NonNull List<String> pgNames,
	                                     @NonNull List<String> pgIcons,
	                                     @NonNull List<String> pgColors,
	                                     @NonNull List<String> pgBackgrounds) {
		if (pgNames.size() == pgIcons.size() &&
				pgIcons.size() == pgColors.size() && pgColors.size() == pgBackgrounds.size()) {
			for (int i = 0; i < pgNames.size(); i++) {
				String name = pgNames.get(i);
				String icon = pgIcons.get(i);
				String background = pgBackgrounds.get(i);
				int color = KAlgorithms.INSTANCE.parseColor(pgColors.get(i));
				if (name.isEmpty() || OBF_POINTS_GROUPS_EMPTY_NAME_STUB.equals(name)) {
					name = GpxFile.DEFAULT_WPT_GROUP_NAME; // follow current default
				}
				GpxUtilities.PointsGroup pg = new GpxUtilities.PointsGroup(name, icon, background, color);
				gpxFile.addPointsGroup(pg);
			}
		}
	}

	private class GpxFileReader extends BaseLoadAsyncTask<Void, Void, GpxFile> {

		private final TravelArticle article;
		private final GpxReadCallback callback;
		private final List<AmenityIndexRepository> repos;

		public GpxFileReader(@NonNull MapActivity mapActivity,
		                     @NonNull TravelArticle article,
		                     @Nullable GpxReadCallback callback,
		                     @NonNull List<AmenityIndexRepository> repos) {
			super(mapActivity);
			this.article = article;
			this.callback = callback;
			this.repos = repos;
		}

		@Override
		protected void onPreExecute() {
			if (isShouldShowProgress()) {
				showProgress(true);
			}
			if (callback != null) {
				callback.onGpxFileReading();
			}
		}

		@Override
		protected GpxFile doInBackground(Void... voids) {
			return buildGpxFile(repos, article, this::isCancelled);
		}

		@Override
		protected void onPostExecute(@Nullable GpxFile gpxFile) {
			if (gpxFile != null) {
				article.gpxFileRead = true;
				article.gpxFile = gpxFile;
				if (callback != null) {
					callback.onGpxFileRead(gpxFile);
				}
			}
			hideProgress();
		}
	}
}
