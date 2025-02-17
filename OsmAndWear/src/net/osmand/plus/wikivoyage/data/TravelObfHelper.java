package net.osmand.plus.wikivoyage.data;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.data.Amenity.REF;
import static net.osmand.data.Amenity.ROUTE_ID;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK_POINT;
import static net.osmand.plus.wikivoyage.data.PopularArticles.ARTICLES_PER_PAGE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ACTIVITY_TYPE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.AVERAGE_ELEVATION;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DIFF_ELEVATION_DOWN;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DIFF_ELEVATION_UP;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DISTANCE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.MAX_ELEVATION;
import static net.osmand.plus.wikivoyage.data.TravelGpx.MIN_ELEVATION;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ROUTE_RADIUS;
import static net.osmand.plus.wikivoyage.data.TravelGpx.USER;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_BACKGROUNDS;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_COLORS;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_DELIMITER;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_ICONS;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_NAMES;
import static net.osmand.shared.gpx.GpxUtilities.PointsGroup.OBF_POINTS_GROUPS_PREFIX;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_FIRST_DIST;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_FIRST_LETTER;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_MULT_1;
import static net.osmand.shared.gpx.GpxUtilities.TRAVEL_GPX_CONVERT_MULT_2;
import static net.osmand.shared.gpx.primitives.GpxExtensions.OBF_GPX_EXTENSION_TAG_PREFIX;
import static net.osmand.util.Algorithms.capitalizeFirstLetter;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.binary.BinaryMapPoiReaderAdapter;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
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
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.util.KAlgorithms;
import net.osmand.shared.util.KMapAlgorithms;
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

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);
	private static final String WORLD_WIKIVOYAGE_FILE_NAME = "World_wikivoyage.travel.obf";
	public static final int ARTICLE_SEARCH_RADIUS = 50 * 1000;
	public static final int SAVED_ARTICLE_SEARCH_RADIUS = 30 * 1000;
	public static final int MAX_SEARCH_RADIUS = 800 * 1000;

	private final OsmandApplication app;
	private final Collator collator;

	private PopularArticles popularArticles = new PopularArticles();
	private final Map<TravelArticleIdentifier, Map<String, TravelArticle>> cachedArticles = new ConcurrentHashMap<>();
	private final TravelLocalDataHelper localDataHelper;
	private int searchRadius = ARTICLE_SEARCH_RADIUS;
	private int foundAmenitiesIndex;
	private final List<Pair<File, Amenity>> foundAmenities = new ArrayList<>();
	public volatile int requestNumber = 0;


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

	@NonNull
	public synchronized PopularArticles loadPopularArticles() {
		String lang = app.getLanguage();
		PopularArticles popularArticles = loadPopularArticlesForLang(lang);
		if (popularArticles.isEmpty()) {
			popularArticles = loadPopularArticlesForLang("en");
		}
		this.popularArticles = popularArticles;
		return popularArticles;
	}

	private synchronized PopularArticles loadPopularArticlesForLang(String lang) {
		PopularArticles popularArticles = new PopularArticles(this.popularArticles);
		if (isAnyTravelBookPresent()) {
			boolean articlesLimitReached = false;
			do {
				if (foundAmenities.size() - foundAmenitiesIndex < ARTICLES_PER_PAGE) {
					LatLon location = app.getMapViewTrackingUtilities().getMapLocation();
					for (BinaryMapIndexReader reader : getReaders()) {
						try {
							searchAmenity(foundAmenities, location, reader, searchRadius, -1, ROUTE_ARTICLE, lang);
							searchAmenity(foundAmenities, location, reader, searchRadius / 5, 15, ROUTE_TRACK, null);
						} catch (Exception e) {
							LOG.error(e.getMessage(), e);
						}
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

	@Nullable
	public synchronized TravelGpx searchGpx(@NonNull LatLon location, @Nullable String filter, @Nullable String ref) {
		List<Pair<File, Amenity>> foundAmenities = new ArrayList<>();
		int searchRadius = ARTICLE_SEARCH_RADIUS;
		TravelGpx travelGpx = null;
		do {
			for (BinaryMapIndexReader reader : getReaders()) {
				try {
					searchAmenity(foundAmenities, location, reader, searchRadius, 15, ROUTE_TRACK, null);
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
			for (Pair<File, Amenity> foundGpx : foundAmenities) {
				Amenity amenity = foundGpx.second;
				if ((Algorithms.objectEquals(amenity.getRouteId(), filter)
						|| Algorithms.objectEquals(amenity.getName(), filter))
						&& Algorithms.objectEquals(amenity.getRef(), ref)) {
					travelGpx = getTravelGpx(foundGpx.first, amenity);
					break;
				}
			}
			searchRadius *= 2;
		} while (travelGpx == null && searchRadius < MAX_SEARCH_RADIUS);
		return travelGpx;
	}

	private void searchAmenity(List<Pair<File, Amenity>> amenitiesList, LatLon location,
	                           BinaryMapIndexReader reader, int searchRadius, int zoom,
	                           String searchFilter, String lang) throws IOException {
		reader.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(
				location, searchRadius, zoom, getSearchFilter(searchFilter), new ResultMatcher<Amenity>() {
					@Override
					public boolean publish(Amenity object) {
						if (lang == null || object.getNamesMap(true).containsKey(lang)) {
							amenitiesList.add(new Pair<>(reader.getFile(), object));
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
	private TravelArticle cacheTravelArticles(File file, Amenity amenity, String lang, boolean readPoints, @Nullable GpxReadCallback callback) {
		TravelArticle article = null;
		Map<String, TravelArticle> articles;
		if (ROUTE_TRACK.equals(amenity.getSubType())) {
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

	private Map<String, TravelArticle> readRoutePoint(File file, Amenity amenity) {
		Map<String, TravelArticle> articles = new HashMap<>();
		TravelGpx res = getTravelGpx(file, amenity);
		articles.put("", res);
		return articles;
	}

	@NonNull
	private TravelGpx getTravelGpx(File file, Amenity amenity) {
		TravelGpx travelGpx = new TravelGpx();
		travelGpx.file = file;
		String title = amenity.getName("en");
		travelGpx.title = createTitle(Algorithms.isEmpty(title) ? amenity.getName() : title);
		travelGpx.lat = amenity.getLocation().getLatitude();
		travelGpx.lon = amenity.getLocation().getLongitude();
		travelGpx.description = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.DESCRIPTION));
		travelGpx.routeId = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID));
		travelGpx.user = Algorithms.emptyIfNull(amenity.getTagContent(USER));
		travelGpx.activityType = Algorithms.emptyIfNull(amenity.getTagContent(ACTIVITY_TYPE));
		travelGpx.ref = Algorithms.emptyIfNull(amenity.getRef());
		try {
			travelGpx.totalDistance = Float.parseFloat(Algorithms.emptyIfNull(amenity.getTagContent(DISTANCE)));
			travelGpx.diffElevationUp = Double.parseDouble(Algorithms.emptyIfNull(amenity.getTagContent(DIFF_ELEVATION_UP)));
			travelGpx.diffElevationDown = Double.parseDouble(Algorithms.emptyIfNull(amenity.getTagContent(DIFF_ELEVATION_DOWN)));
			travelGpx.maxElevation = Double.parseDouble(Algorithms.emptyIfNull(amenity.getTagContent(MAX_ELEVATION)));
			travelGpx.minElevation = Double.parseDouble(Algorithms.emptyIfNull(amenity.getTagContent(MIN_ELEVATION)));
			travelGpx.avgElevation = Double.parseDouble(Algorithms.emptyIfNull(amenity.getTagContent(AVERAGE_ELEVATION)));
			String radius = amenity.getTagContent(ROUTE_RADIUS);
			if (radius != null) {
				travelGpx.routeRadius = MapUtils.convertCharToDist(radius.charAt(0), TRAVEL_GPX_CONVERT_FIRST_LETTER,
						TRAVEL_GPX_CONVERT_FIRST_DIST, TRAVEL_GPX_CONVERT_MULT_1, TRAVEL_GPX_CONVERT_MULT_2);
			}
		} catch (NumberFormatException e) {
			LOG.debug(e.getMessage(), e);
		}
		return travelGpx;
	}

	@NonNull
	public static SearchPoiTypeFilter getSearchFilter(String... filterSubcategory) {
		return new SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				for (String filterSubcategory : filterSubcategory) {
					if (subcategory.equals(filterSubcategory)) {
						return true;
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
		return !Algorithms.isEmpty(getReaders());
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

		for (BinaryMapIndexReader reader : getReaders()) {
			if (requestNumber != reqNumber) {
				return empty;
			}
			try {
				List<BinaryMapPoiReaderAdapter.PoiRegion> poiIndexes = reader.getPoiIndexes();
				QuadRect bbox = new QuadRect();
				for (BinaryMapPoiReaderAdapter.PoiRegion poiRegion : poiIndexes) {
					bbox.expand(poiRegion.getLeft31(), poiRegion.getTop31(), poiRegion.getRight31(), poiRegion.getBottom31());
				}
				SearchRequest<Amenity> searchRequest = BinaryMapIndexReader.buildSearchPoiRequest(0, 0, searchQuery,
						(int)bbox.left, (int)bbox.right, (int)bbox.top, (int)bbox.bottom, getSearchFilter(ROUTE_ARTICLE), new ResultMatcher<Amenity>() {
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

				List<Amenity> amenities = reader.searchPoiByName(searchRequest);
				if (requestNumber != reqNumber) {
					return empty;
				}
				if (!Algorithms.isEmpty(amenities)) {
					amenityMap.put(reader.getFile(), amenities);
				}
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
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

	public void sortSearchResults(List<WikivoyageSearchResult> results, String searchQuery) {
		results.sort(new SearchResultComparator(searchQuery, collator));
	}

	public static class SearchResultComparator implements Comparator<WikivoyageSearchResult> {
		private final Collator collator;
		private final String searchQuery;
		private final String searchQueryLC;


		public SearchResultComparator(String searchQuery, Collator collator) {
			this.searchQuery = searchQuery;
			this.collator = collator;
			searchQueryLC = searchQuery.toLowerCase();
		}

		@Override
		public int compare(WikivoyageSearchResult sr1, WikivoyageSearchResult sr2) {
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
		public int compare(WikivoyageSearchResult sr1, WikivoyageSearchResult sr2, SearchResultComparator c) {
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
					List<WikivoyageSearchResult> resultList = navMap.get(header);
					if (resultList == null) {
						resultList = new ArrayList<>();
						navMap.put(header, resultList);
					}
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

	private TravelArticle getParentArticleByTitle(String title, String lang) {
		TravelArticle article = null;
		List<Amenity> amenities = new ArrayList<>();
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
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
				reader.searchPoiByName(req);
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
			if (!Algorithms.isEmpty(amenities)) {
				article = readArticle(reader.getFile(), amenities.get(0), lang);
				break;
			}
		}
		return article;
	}

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
	                          @NonNull String gpxFileName, @NonNull LatLon latLon) {
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
					GpxUiHelper.saveAndOpenGpx(mapActivity, file, gpxFile, wptPt, article.getAnalysis(), null);
				}
			}
		};
		readGpxFile(article, callback);
	}

	private void readGpxFile(@NonNull TravelArticle article, @Nullable GpxReadCallback callback) {
		if (!article.gpxFileRead) {
			new GpxFileReader(article, callback, getReaders()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else if (callback != null) {
			callback.onGpxFileRead(article.gpxFile);
		}
	}

	private synchronized TravelArticle findArticleById(@NonNull TravelArticleIdentifier articleId,
	                                                   String lang, boolean readGpx, @Nullable GpxReadCallback callback) {
		TravelArticle article = null;
		boolean isDbArticle = articleId.file != null && articleId.file.getName().endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT);
		List<Amenity> amenities = new ArrayList<>();
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				if (articleId.file != null && !articleId.file.equals(reader.getFile()) && !isDbArticle) {
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
			if (!Algorithms.isEmpty(amenities)) {
				article = cacheTravelArticles(reader.getFile(), amenities.get(0), lang, readGpx, callback);
			}
		}
		return article;
	}

	@Override
	public synchronized TravelArticle findSavedArticle(@NonNull TravelArticle savedArticle) {
		List<Pair<File, Amenity>> amenities = new ArrayList<>();
		TravelArticle article = null;
		TravelArticleIdentifier articleId = savedArticle.generateIdentifier();
		String lang = savedArticle.getLang();
		long lastModified = savedArticle.getLastModified();
		TravelArticleIdentifier finalArticleId = articleId;
		SearchRequest<Amenity> req = null;
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				if (articleId.file != null && articleId.file.equals(reader.getFile())) {
					if (lastModified == reader.getFile().lastModified()) {
						req = BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
								Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
								getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK), new ResultMatcher<Amenity>() {
									boolean done;

									@Override
									public boolean publish(Amenity amenity) {
										if (Algorithms.stringsEqual(finalArticleId.routeId,
												Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID)))) {
											amenities.add(new Pair<>(reader.getFile(), amenity));
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
							req = getEqualsTitleRequest(articleId, lang, amenities, reader);
							req.setBBoxRadius(articleId.lat, articleId.lon, ARTICLE_SEARCH_RADIUS / 10);
						}
					}
				}
				if (req != null) {
					if (!Double.isNaN(articleId.lat)) {
						if (!Algorithms.isEmpty(articleId.title)) {
							reader.searchPoiByName(req);
						} else {
							reader.searchPoi(req);
						}
					} else {
						reader.searchPoi(req);
					}
					break;
				}
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
		}
		if (amenities.isEmpty() && !Algorithms.isEmpty(articleId.title)) {
			for (BinaryMapIndexReader reader : getReaders()) {
				try {
					req = getEqualsTitleRequest(articleId, lang, amenities, reader);
					req.setBBoxRadius(articleId.lat, articleId.lon, SAVED_ARTICLE_SEARCH_RADIUS);
					if (!Double.isNaN(articleId.lat)) {
						reader.searchPoiByName(req);
					} else {
						reader.searchPoi(req);
					}
				} catch (IOException e) {
					LOG.error(e.getMessage());
				}
			}
		}
		if (amenities.isEmpty()) {
			for (BinaryMapIndexReader reader : getReaders()) {
				try {
					req = BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
							Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
							getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK), new ResultMatcher<Amenity>() {
								boolean done;

								@Override
								public boolean publish(Amenity amenity) {
									if (Algorithms.stringsEqual(finalArticleId.routeId,
											Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID)))
											&& Algorithms.stringsEqual(finalArticleId.routeSource,
											Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_SOURCE)))) {
										amenities.add(new Pair<>(reader.getFile(), amenity));
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
			}
		}
		if (!Algorithms.isEmpty(amenities)) {
			article = cacheTravelArticles(amenities.get(0).first, amenities.get(0).second, lang, false, null);
		}
		return article;
	}

	private SearchRequest<Amenity> getEqualsTitleRequest(@NonNull TravelArticleIdentifier articleId,
	                                                     String lang, List<Pair<File, Amenity>> amenities,
	                                                     BinaryMapIndexReader reader) {
		return BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
				Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
				getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK), new ResultMatcher<Amenity>() {
					boolean done;

					@Override
					public boolean publish(Amenity amenity) {
						if (Algorithms.stringsEqual(Algorithms.emptyIfNull(articleId.title),
								Algorithms.emptyIfNull(amenity.getName(lang)))) {
							amenities.add(new Pair<>(reader.getFile(), amenity));
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
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
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
				reader.searchPoiByName(req);
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
			if (!Algorithms.isEmpty(amenities)) {
				article = cacheTravelArticles(reader.getFile(), amenities.get(0), lang, readGpx, callback);
				break;
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
		return article.getTitle().replace('/', '_').replace('\'', '_')
				.replace('\"', '_') + GPX_FILE_EXT;
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

	@Override
	public String getSelectedTravelBookName() {
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

	@Nullable
	private synchronized GpxFile buildGpxFile(@NonNull List<BinaryMapIndexReader> readers, TravelArticle article) {
		List<BinaryMapDataObject> segmentList = new ArrayList<>();
		Map<String, String> gpxFileExtensions = new HashMap<>();
		List<Amenity> pointList = new ArrayList<>();
		List<String> pgNames = new ArrayList<>();
		List<String> pgIcons = new ArrayList<>();
		List<String> pgColors = new ArrayList<>();
		List<String> pgBackgrounds = new ArrayList<>();
		for (BinaryMapIndexReader reader : readers) {
			try {
				if (article.file != null && !article.file.equals(reader.getFile())) {
					continue;
				}
				if (article instanceof TravelGpx) {
					BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> sr = BinaryMapIndexReader.buildSearchRequest(
							0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 15, null,
							new ResultMatcher<BinaryMapDataObject>() {
								@Override
								public boolean publish(BinaryMapDataObject object) {
									if (object.getPointsLength() > 1) {
										if (object.getTagValue(REF).equals(article.ref)
												&& (object.getTagValue(ROUTE_ID).equals(article.routeId)
												|| createTitle(object.getName()).equals(article.getTitle()))) {
											segmentList.add(object);
										}
									}
									return false;
								}

								@Override
								public boolean isCancelled() {
									return false;
								}
							});
					if (article.routeRadius >= 0) {
						sr.setBBoxRadius(article.lat, article.lon, article.routeRadius);
					}
					reader.searchMapIndex(sr);
				}

				BinaryMapIndexReader.SearchRequest<Amenity> pointRequest = BinaryMapIndexReader.buildSearchPoiRequest(
						0, 0, Algorithms.emptyIfNull(article.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
						getSearchFilter(article.getMainFilterString(), article.getPointFilterString()),
						new ResultMatcher<Amenity>() {
							@Override
							public boolean publish(Amenity amenity) {
								if (amenity.getRouteId().equals(article.getRouteId())) {
									if (ROUTE_TRACK.equals(amenity.getSubType())) {
										for (String key : amenity.getAdditionalInfoKeys()) {
											if (key.startsWith(OBF_GPX_EXTENSION_TAG_PREFIX)) {
												String tag = key.replaceFirst(OBF_GPX_EXTENSION_TAG_PREFIX, "");
												String val = amenity.getAdditionalInfo(key);
												gpxFileExtensions.put(tag, val);
											} else if (key.startsWith(OBF_POINTS_GROUPS_PREFIX)) {
												final String delimiter = OBF_POINTS_GROUPS_DELIMITER;
												String joinedValues = amenity.getAdditionalInfo(key);
												List<String> values = Arrays.asList(joinedValues.split(delimiter));
												if (OBF_POINTS_GROUPS_NAMES.equals(key)) {
													pgNames.addAll(values);
												} else if (OBF_POINTS_GROUPS_ICONS.equals(key)) {
													pgIcons.addAll(values);
												} else if (OBF_POINTS_GROUPS_COLORS.equals(key)) {
													pgColors.addAll(values);
												} else if (OBF_POINTS_GROUPS_BACKGROUNDS.equals(key)) {
													pgBackgrounds.addAll(values);
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
								return false;
							}
						}, null);
				if (article.routeRadius >= 0) {
					pointRequest.setBBoxRadius(article.lat, article.lon, article.routeRadius);
				}
				if (!Algorithms.isEmpty(article.title)) {
					reader.searchPoiByName(pointRequest);
				} else {
					reader.searchPoi(pointRequest);
				}
				if (!Algorithms.isEmpty(segmentList)) {
					break;
				}
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
		}
		GpxFile gpxFile = null;
		String description = article.getDescription();
		String title = FileUtils.isValidFileName(description) ? description : article.getTitle();
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
				String ele_graph = segment.getTagValue("ele_graph");
				if (!Algorithms.isEmpty(ele_graph)) {
					hasAltitude = true;
					List<Integer> heightRes = KMapAlgorithms.INSTANCE.decodeIntHeightArrayGraph(ele_graph, 3);
					double startEle = 0;
					try {
						startEle = Double.parseDouble(segment.getTagValue("start_ele"));
					} catch (NumberFormatException e) {
						LOG.debug(e.getMessage(), e);
					}
					KMapAlgorithms.INSTANCE.augmentTrkSegmentWithAltitudes(trkSegment, heightRes, startEle);
				}
				track.getSegments().add(trkSegment);
			}
			gpxFile = new GpxFile(title, article.getLang(), article.getContent());
			if (!Algorithms.isEmpty(article.getImageTitle())) {
				gpxFile.getMetadata().setLink(TravelArticle.getImageUrl(article.getImageTitle(), false));
			}
			gpxFile.setTracks(new ArrayList<>());
			gpxFile.getTracks().add(track);
			gpxFile.setRef(article.ref);
			gpxFile.setHasAltitude(hasAltitude);
			gpxFile.getExtensionsToWrite().putAll(gpxFileExtensions);
		}
		reconstructPointsGroups(gpxFile, pgNames, pgIcons, pgColors, pgBackgrounds); // create groups before points
		if (!pointList.isEmpty()) {
			if (gpxFile == null) {
				gpxFile = new GpxFile(title, article.getLang(), article.getContent());
				if (!Algorithms.isEmpty(article.getImageTitle())) {
					gpxFile.getMetadata().setLink(TravelArticle.getImageUrl(article.getImageTitle(), false));
				}
			}
			for (Amenity wayPoint : pointList) {
				gpxFile.addPoint(article.createWptPt(wayPoint, article.getLang()));
			}
		}
		article.gpxFile = gpxFile;
		return gpxFile;
	}

	private void reconstructPointsGroups(GpxFile gpxFile, List<String> pgNames, List<String> pgIcons,
										 List<String> pgColors, List<String> pgBackgrounds) {
		if (pgNames.size() == pgIcons.size() &&
				pgIcons.size() == pgColors.size() && pgColors.size() == pgBackgrounds.size()) {
			for (int i = 0; i < pgNames.size(); i++) {
				String name = pgNames.get(i);
				String icon = pgIcons.get(i);
				String background = pgBackgrounds.get(i);
				int color = KAlgorithms.INSTANCE.parseColor(pgColors.get(i));
				if (name.isEmpty()) name = GpxFile.DEFAULT_WPT_GROUP_NAME; // follow current default
				GpxUtilities.PointsGroup pg = new GpxUtilities.PointsGroup(name, icon, background, color);
				gpxFile.addPointsGroup(pg);
			}
		}
	}

	@NonNull
	public String createTitle(@NonNull String name) {
		return capitalizeFirstLetter(GpxHelper.INSTANCE.getGpxTitle(name));
	}

	private class GpxFileReader extends AsyncTask<Void, Void, GpxFile> {

		private final TravelArticle article;
		private final GpxReadCallback callback;
		private final List<BinaryMapIndexReader> readers;

		public GpxFileReader(@NonNull TravelArticle article, @Nullable GpxReadCallback callback,
		                     @NonNull List<BinaryMapIndexReader> readers) {
			this.article = article;
			this.callback = callback;
			this.readers = readers;
		}

		@Override
		protected void onPreExecute() {
			if (callback != null) {
				callback.onGpxFileReading();
			}
		}

		@Override
		protected GpxFile doInBackground(Void... voids) {
			return buildGpxFile(readers, article);
		}

		@Override
		protected void onPostExecute(GpxFile gpxFile) {
			article.gpxFileRead = true;
			article.gpxFile = gpxFile;
			if (callback != null) {
				callback.onGpxFileRead(gpxFile);
			}
		}
	}
}
