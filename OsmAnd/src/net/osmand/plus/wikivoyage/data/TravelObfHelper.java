package net.osmand.plus.wikivoyage.data;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.data.Amenity.ROUTE;
import static net.osmand.data.Amenity.ROUTE_ID;
import static net.osmand.osm.MapPoiTypes.ROUTES_PREFIX;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK;
import static net.osmand.plus.wikivoyage.data.PopularArticles.ARTICLES_PER_PAGE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ROUTE_TYPE;

import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapPoiReaderAdapter;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.wikivoyage.WikivoyageUtils;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.AmenityIndexRepository;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.core.SearchSettings;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);
	private static final String WORLD_WIKIVOYAGE_FILE_NAME = "World_wikivoyage.travel.obf";
	private static final int ARTICLE_SEARCH_RADIUS = 500 * 1000;
	private static final int SAVED_ARTICLE_SEARCH_RADIUS = 30 * 1000;
	private static final int MAX_SEARCH_RADIUS = 800 * 1000;
	private static final int TRAVEL_GPX_SEARCH_RADIUS = 10 * 1000; // Ref: POI_SEARCH_POINTS_INTERVAL_M in tools

	private final OsmandApplication app;
	private final Collator collator;

	private PopularArticles popularArticles = new PopularArticles();
	private final Map<TravelArticleIdentifier, Map<String, TravelArticle>> cachedArticles = new ConcurrentHashMap<>();
	private final TravelLocalDataHelper localDataHelper;
	private int searchRadius = ARTICLE_SEARCH_RADIUS;
	private int foundAmenitiesIndex;
	private final List<Pair<File, Amenity>> foundAmenities = new ArrayList<>();
	public volatile int requestNumber = 0;


	public static final String TAG_URL = "url";
	public static final String TAG_URL_TEXT = "url_text";
	public static final String WPT_EXTRA_TAGS = "wpt_extra_tags";
	public static final String METADATA_EXTRA_TAGS = "metadata_extra_tags";
	public static final String EXTENSIONS_EXTRA_TAGS = "extensions_extra_tags";

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
		return tags.containsKey(ROUTE_ID)
				&& ("segment".equals(tags.get(ROUTE)) || tags.containsKey(ROUTE_TYPE));
	}

	@Nullable
	public synchronized TravelGpx searchTravelGpx(@NonNull LatLon location, @Nullable String routeId) {
		if (Algorithms.isEmpty(routeId)) {
			LOG.error(String.format("searchTravelGpx(%s, null) failed due to empty routeId", location));
			return null;
		}
		List<Pair<File, Amenity>> foundAmenities = new ArrayList<>();
		int searchRadius = TRAVEL_GPX_SEARCH_RADIUS;
		TravelGpx travelGpx = null;
		do {
			for (AmenityIndexRepository repo : getTravelGpxRepositories()) {
				if (repo.isWorldMap()) {
					continue;
				}
				if (!isLocationIntersectsWithRepo(repo, location)) {
					continue;
				}
				int previousFoundSize = foundAmenities.size();
				boolean firstSearchCycle = searchRadius == TRAVEL_GPX_SEARCH_RADIUS;
				if (firstSearchCycle) {
					searchTravelGpxAmenityByRouteId(foundAmenities, repo, routeId, location, searchRadius); // indexed
				}
				boolean nothingFound = previousFoundSize == foundAmenities.size();
				if (nothingFound) {
					// fallback to non-indexed route_id (compatibility with old files)
					searchAmenity(foundAmenities, location, repo, searchRadius, 15, ROUTE_TRACK, null);
				}
			}
			for (Pair<File, Amenity> foundGpx : foundAmenities) {
				Amenity amenity = foundGpx.second;
				final String aRouteId = amenity.getRouteId();
				final String lcRouteId = aRouteId != null ? aRouteId.toLowerCase() : null;
				if (routeId.toLowerCase().equals(lcRouteId)) {
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

	private void searchTravelGpxAmenityByRouteId(@NonNull List<Pair<File, Amenity>> amenitiesList,
	                                             @NonNull AmenityIndexRepository repo, @NonNull String routeId,
	                                             @NonNull LatLon location, int searchRadius) {
		int left = 0, right = Integer.MAX_VALUE, top = 0, bottom = Integer.MAX_VALUE;
		SearchPoiTypeFilter poiTypeFilter = new BinaryMapIndexReader.SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory poiCategory, String subType) {
				return subType.startsWith(ROUTES_PREFIX) || ROUTE_TRACK.equals(subType);
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};
		SearchRequest<Amenity> pointRequest = BinaryMapIndexReader.buildSearchPoiRequest(
				0, 0, routeId, left, right, top, bottom, poiTypeFilter,
				new ResultMatcher<Amenity>() {
					@Override
					public boolean publish(Amenity amenity) {
						if (amenity.getRouteId() != null && amenity.getRouteId().equals(routeId)) {
							amenitiesList.add(new Pair<>(repo.getFile(), amenity));
						}
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				}, null);
		pointRequest.setBBoxRadius(location.getLatitude(), location.getLongitude(), searchRadius);
		repo.searchPoiByName(pointRequest);
	}

	private boolean isLocationIntersectsWithRepo(AmenityIndexRepository repo, @NonNull LatLon location) {
		int x31 = MapUtils.get31TileNumberX(location.getLongitude());
		int y31 = MapUtils.get31TileNumberY(location.getLatitude());
		for (BinaryMapPoiReaderAdapter.PoiRegion poiIndex : repo.getReaderPoiIndexes()) {
			QuadRect bbox = new QuadRect(poiIndex.getLeft31(), poiIndex.getTop31(), poiIndex.getRight31(), poiIndex.getBottom31());
			if (bbox.contains(x31, y31, x31, y31)) {
				return true;
			}
		}
		return false;
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
		TravelGpx travelGpx = new TravelGpx(amenity);
		travelGpx.file = file;
		return travelGpx;
	}

	@NonNull
	public static SearchPoiTypeFilter getSearchFilter(@NonNull String... filterSubcategories) {
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
		return !app.isApplicationInitializing() && app.getResourceManager().hasTravelRepositories();
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
					OsmAndTaskManager.executeTask(new TravelObfGpxFileReader(mapActivity, article, callback, getTravelGpxRepositories()));
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
	public List<String> getArticleLangs(@NonNull TravelArticleIdentifier articleId) {
		return new ArrayList<>(getArticleByLangs(articleId).keySet());
	}

	@NonNull
	@Override
	public Map<String, TravelArticle> getArticleByLangs(@NonNull TravelArticleIdentifier articleId) {
		Map<String, TravelArticle> res = new LinkedHashMap<>();
		TravelArticle article = getArticleById(articleId, "", false, null);
		if (article != null) {
			Map<String, TravelArticle> articles = cachedArticles.get(article.generateIdentifier());
			if (articles != null) {
				res.putAll(articles);
			}
		} else {
			List<TravelArticle> articles = localDataHelper.getSavedArticles(articleId.file, articleId.routeId);
			for (TravelArticle a : articles) {
				res.put(a.getLang(), a);
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

}
