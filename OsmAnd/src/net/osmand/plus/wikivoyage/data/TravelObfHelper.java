package net.osmand.plus.wikivoyage.data;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
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

import static net.osmand.GPXUtilities.TRAVEL_GPX_CONVERT_FIRST_DIST;
import static net.osmand.GPXUtilities.TRAVEL_GPX_CONVERT_FIRST_LETTER;
import static net.osmand.GPXUtilities.TRAVEL_GPX_CONVERT_MULT_1;
import static net.osmand.GPXUtilities.TRAVEL_GPX_CONVERT_MULT_2;
import static net.osmand.GPXUtilities.writeGpxFile;
import static net.osmand.data.Amenity.REF;
import static net.osmand.data.Amenity.ROUTE_ID;
import static net.osmand.plus.helpers.GpxUiHelper.getGpxTitle;
import static net.osmand.plus.wikivoyage.data.PopularArticles.ARTICLES_PER_PAGE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ACTIVITY_TYPE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DIFF_ELE_DOWN;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DIFF_ELE_UP;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DISTANCE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.ROUTE_TRACK_POINT;
import static net.osmand.plus.wikivoyage.data.TravelGpx.USER;
import static net.osmand.util.Algorithms.capitalizeFirstLetter;

public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);
	private static final String WORLD_WIKIVOYAGE_FILE_NAME = "World_wikivoyage.travel.obf";
	public static final String ROUTE_ARTICLE = "route_article";
	public static final String ROUTE_TRACK = "route_track";
	public static final String ROUTE_ARTICLE_POINT = "route_article_point";
	public static final String ROUTE_RADIUS = "route_radius";
	public static final int ARTICLE_SEARCH_RADIUS = 50 * 1000;
	public static final int SAVED_ARTICLE_SEARCH_RADIUS = 30 * 1000;
	public static final int MAX_SEARCH_RADIUS = 800 * 1000;

	private final OsmandApplication app;
	private final Collator collator;

	private PopularArticles popularArticles = new PopularArticles();
	private final Map<TravelArticleIdentifier, Map<String, TravelArticle>> cachedArticles = new ConcurrentHashMap<>();
	private final TravelLocalDataHelper localDataHelper;
	private int searchRadius = ARTICLE_SEARCH_RADIUS;
	private int foundAmenitiesIndex = 0;
	private final List<Pair<File, Amenity>> foundAmenities = new ArrayList<>();

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
		PopularArticles popularArticles = new PopularArticles(this.popularArticles);
		if (isAnyTravelBookPresent()) {
			boolean articlesLimitReached = false;
			do {
				if (foundAmenities.size() - foundAmenitiesIndex < ARTICLES_PER_PAGE) {
					final LatLon location = app.getMapViewTrackingUtilities().getMapLocation();
					for (final BinaryMapIndexReader reader : getReaders()) {
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
		this.popularArticles = popularArticles;
		return popularArticles;
	}

	@Nullable
	public synchronized TravelGpx searchGpx(LatLon location, String routeID, String ref) {
		final List<Pair<File, Amenity>> foundAmenities = new ArrayList<>();
		int searchRadius = ARTICLE_SEARCH_RADIUS;
		TravelGpx travelGpx = null;
		do {
			for (final BinaryMapIndexReader reader : getReaders()) {
				try {
					searchAmenity(foundAmenities, location, reader, searchRadius, 15, ROUTE_TRACK, null);
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
			for (Pair<File, Amenity> foundGpx : foundAmenities) {
				Amenity amenity = foundGpx.second;
				if (amenity.getRouteId().equals(routeID) && amenity.getRef().equals(ref)) {
					travelGpx = getTravelGpx(foundGpx.first, amenity);
					break;
				}
			}
			searchRadius *= 2;
		} while (travelGpx == null && searchRadius < MAX_SEARCH_RADIUS);
		return travelGpx;
	}

	private void searchAmenity(final List<Pair<File, Amenity>> amenitiesList, LatLon location,
	                           final BinaryMapIndexReader reader, int searchRadius, int zoom,
	                           String searchFilter, final String lang) throws IOException {
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
		travelGpx.routeId = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID));
		travelGpx.user = Algorithms.emptyIfNull(amenity.getTagContent(USER));
		travelGpx.activityType = Algorithms.emptyIfNull(amenity.getTagContent(ACTIVITY_TYPE));
		travelGpx.ref = Algorithms.emptyIfNull(amenity.getRef());
		try {
			travelGpx.totalDistance = Float.parseFloat(Algorithms.emptyIfNull(amenity.getTagContent(DISTANCE)));
			travelGpx.diffElevationUp = Double.parseDouble(Algorithms.emptyIfNull(amenity.getTagContent(DIFF_ELE_UP)));
			travelGpx.diffElevationDown = Double.parseDouble(Algorithms.emptyIfNull(amenity.getTagContent(DIFF_ELE_DOWN)));
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
	private SearchPoiTypeFilter getSearchFilter(final String... filterSubcategory) {
		return new SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				for (String filterSubcategory : filterSubcategory) {
					return subcategory.equals(filterSubcategory);
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
	public synchronized List<WikivoyageSearchResult> search(@NonNull String searchQuery) {
		List<WikivoyageSearchResult> res = new ArrayList<>();
		Map<File, List<Amenity>> amenityMap = new HashMap<>();
		final String appLang = app.getLanguage();
		final NameStringMatcher nm = new NameStringMatcher(searchQuery, StringMatcherMode.CHECK_STARTS_FROM_SPACE);
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				SearchRequest<Amenity> searchRequest = BinaryMapIndexReader.buildSearchPoiRequest(0, 0, searchQuery,
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, getSearchFilter(ROUTE_ARTICLE), new ResultMatcher<Amenity>() {
							@Override
							public boolean publish(Amenity object) {
								List<String> otherNames = object.getOtherNames(false);
								String localeName = object.getName(appLang);
								return nm.matches(localeName) || nm.matches(otherNames);
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						}, null);

				List<Amenity> amenities = reader.searchPoiByName(searchRequest);
				if (!Algorithms.isEmpty(amenities)) {
					amenityMap.put(reader.getFile(), amenities);
				}
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		if (!Algorithms.isEmpty(amenityMap)) {
			final boolean appLangEn = "en".equals(appLang);
			for (Entry<File, List<Amenity>> entry : amenityMap.entrySet()) {
				File file = entry.getKey();
				for (Amenity amenity : entry.getValue()) {
					Set<String> nameLangs = getLanguages(amenity);
					if (nameLangs.contains(appLang)) {
						TravelArticle article = readArticle(file, amenity, appLang);
						ArrayList<String> langs = new ArrayList<>(nameLangs);
						Collections.sort(langs, new Comparator<String>() {
							@Override
							public int compare(String l1, String l2) {
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
							}
						});
						WikivoyageSearchResult r = new WikivoyageSearchResult(article, langs);
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
				return collator.compare(res1.getArticleTitle(), res2.getArticleTitle());
			}
		});
	}

	@NonNull
	@Override
	public List<TravelArticle> getPopularArticles() {
		return popularArticles.getArticles();
	}

	@NonNull
	@Override
	public synchronized Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> getNavigationMap(@NonNull final TravelArticle article) {
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
		Set<String> headers = new LinkedHashSet<>();
		Map<String, WikivoyageSearchResult> headerObjs = new HashMap<>();
		if (parts != null && parts.length > 0) {
			headers.addAll(Arrays.asList(parts));
			if (!Algorithms.isEmpty(article.isParentOf)) {
				headers.add(title);
			}
		}

		for (String header : headers) {
			TravelArticle parentArticle = getParentArticleByTitle(header, lang);
			if (parentArticle == null) {
				continue;
			}
			navMap.put(header, new ArrayList<WikivoyageSearchResult>());
			String[] isParentOf = parentArticle.isParentOf.split(";");
			for (String childTitle : isParentOf) {
				if (!childTitle.isEmpty()) {
					WikivoyageSearchResult searchResult = new WikivoyageSearchResult("", childTitle, null,
							null, Collections.singletonList(lang));
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

	private TravelArticle getParentArticleByTitle(final String title, final String lang) {
		TravelArticle article = null;
		final List<Amenity> amenities = new ArrayList<>();
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
						0, 0, title, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, getSearchFilter(ROUTE_ARTICLE),
						new ResultMatcher<Amenity>() {
							boolean done = false;

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

	public void readGpxFile(@NonNull TravelArticle article, @Nullable GpxReadCallback callback) {
		if (!article.gpxFileRead) {
			new GpxFileReader(article, callback, getReaders()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else if (callback != null) {
			callback.onGpxFileRead(article.gpxFile);
		}
	}

	private synchronized TravelArticle findArticleById(@NonNull final TravelArticleIdentifier articleId,
	                                                   String lang, boolean readGpx, @Nullable GpxReadCallback callback) {
		TravelArticle article = null;
		final boolean isDbArticle = articleId.file != null && articleId.file.getName().endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT);
		final List<Amenity> amenities = new ArrayList<>();
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				if (articleId.file != null && !articleId.file.equals(reader.getFile()) && !isDbArticle) {
					continue;
				}
				SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
						Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
						getSearchFilter(ROUTE_ARTICLE), new ResultMatcher<Amenity>() {
							boolean done = false;

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
		final List<Pair<File, Amenity>> amenities = new ArrayList<>();
		TravelArticle article = null;
		TravelArticleIdentifier articleId = savedArticle.generateIdentifier();
		String lang = savedArticle.getLang();
		long lastModified = savedArticle.getLastModified();
		final TravelArticleIdentifier finalArticleId = articleId;
		SearchRequest<Amenity> req = null;
		for (final BinaryMapIndexReader reader : getReaders()) {
			try {
				if (articleId.file != null && articleId.file.equals(reader.getFile())) {
					if (lastModified == reader.getFile().lastModified()) {
						req = BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
								Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
								getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK), new ResultMatcher<Amenity>() {
									boolean done = false;

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
			for (final BinaryMapIndexReader reader : getReaders()) {
				try {
					req = BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
							Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
							getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK), new ResultMatcher<Amenity>() {
								boolean done = false;

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

	private SearchRequest<Amenity> getEqualsTitleRequest(@NonNull final TravelArticleIdentifier articleId,
	                                                     final String lang, final List<Pair<File, Amenity>> amenities,
	                                                     final BinaryMapIndexReader reader) {
		return BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
				Algorithms.emptyIfNull(articleId.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
				getSearchFilter(ROUTE_ARTICLE, ROUTE_TRACK), new ResultMatcher<Amenity>() {
					boolean done = false;

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
	public TravelArticle getArticleByTitle(@NonNull final String title, @NonNull final String lang,
	                                       boolean readGpx, @Nullable GpxReadCallback callback) {
		return getArticleByTitle(title, new QuadRect(), lang, readGpx, callback);
	}

	@Nullable
	@Override
	public TravelArticle getArticleByTitle(@NonNull final String title, @NonNull LatLon latLon,
	                                       @NonNull final String lang, boolean readGpx, @Nullable GpxReadCallback callback) {
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), ARTICLE_SEARCH_RADIUS);
		return getArticleByTitle(title, rect, lang, readGpx, callback);
	}

	@Nullable
	@Override
	public synchronized TravelArticle getArticleByTitle(@NonNull final String title, @NonNull QuadRect rect,
	                                                    @NonNull final String lang, boolean readGpx, @Nullable GpxReadCallback callback) {
		TravelArticle article = null;
		final List<Amenity> amenities = new ArrayList<>();
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
							boolean done = false;

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
				.replace('\"', '_') + IndexConstants.GPX_FILE_EXT;
	}

	@NonNull
	@Override
	public File createGpxFile(@NonNull TravelArticle article) {
		final GPXFile gpx;
		gpx = article.getGpxFile();
		File file = app.getAppPath(IndexConstants.GPX_TRAVEL_DIR + getGPXName(article));
		writeGpxFile(file, gpx);
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
	private synchronized GPXFile buildGpxFile(@NonNull List<BinaryMapIndexReader> readers, TravelArticle article) {
		final List<BinaryMapDataObject> segmentList = new ArrayList<>();
		final List<Amenity> pointList = new ArrayList<>();
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
												&& object.getTagValue(ROUTE_ID).equals(article.routeId)) {
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
						getSearchFilter(article.getPointFilterString()),
						new ResultMatcher<Amenity>() {
							@Override
							public boolean publish(Amenity amenity) {
								if (amenity.getRouteId().equals(article.getRouteId())) {
									if (article.getPointFilterString().equals(ROUTE_TRACK_POINT)) {
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
		GPXFile gpxFile = null;
		if (!segmentList.isEmpty()) {
			GPXUtilities.Track track = new GPXUtilities.Track();
			for (BinaryMapDataObject segment : segmentList) {
				GPXUtilities.TrkSegment trkSegment = new GPXUtilities.TrkSegment();
				for (int i = 0; i < segment.getPointsLength(); i++) {
					GPXUtilities.WptPt point = new GPXUtilities.WptPt();
					point.lat = MapUtils.get31LatitudeY(segment.getPoint31YTile(i));
					point.lon = MapUtils.get31LongitudeX(segment.getPoint31XTile(i));
					trkSegment.points.add(point);
				}
				track.segments.add(trkSegment);
			}
			gpxFile = new GPXFile(article.getTitle(), article.getLang(), article.getContent());
			if (!Algorithms.isEmpty(article.getImageTitle())) {
				gpxFile.metadata.link = TravelArticle.getImageUrl(article.getImageTitle(), false);
			}
			gpxFile.tracks = new ArrayList<>();
			gpxFile.tracks.add(track);
			gpxFile.setRef(article.ref);
		}
		if (!pointList.isEmpty()) {
			if (gpxFile == null) {
				gpxFile = new GPXFile(article.getTitle(), article.getLang(), article.getContent());
				if (!Algorithms.isEmpty(article.getImageTitle())) {
					gpxFile.metadata.link = TravelArticle.getImageUrl(article.getImageTitle(), false);
				}
			}
			for (Amenity wayPoint : pointList) {
				gpxFile.addPoint(article.createWptPt(wayPoint, article.getLang()));
			}
		}
		article.gpxFile = gpxFile;
		return gpxFile;
	}

	@NonNull
	public String createTitle(@NonNull String name) {
		return capitalizeFirstLetter(getGpxTitle(name));
	}

	private class GpxFileReader extends AsyncTask<Void, Void, GPXFile> {

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
		protected GPXFile doInBackground(Void... voids) {
			return buildGpxFile(readers, article);
		}

		@Override
		protected void onPostExecute(GPXFile gpxFile) {
			article.gpxFileRead = true;
			article.gpxFile = gpxFile;
			if (callback != null) {
				callback.onGpxFileRead(gpxFile);
			}
		}
	}
}
