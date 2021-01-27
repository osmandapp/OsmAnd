package net.osmand.plus.wikivoyage.data;

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
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.ColorDialogs;
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

import static net.osmand.GPXUtilities.WptPt;
import static net.osmand.GPXUtilities.writeGpxFile;
import static net.osmand.plus.helpers.GpxUiHelper.getGpxTitle;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DIFF_ELE_DOWN;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DIFF_ELE_UP;
import static net.osmand.plus.wikivoyage.data.TravelGpx.DISTANCE;
import static net.osmand.plus.wikivoyage.data.TravelGpx.USER;
import static net.osmand.util.Algorithms.capitalizeFirstLetter;

public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);
	private static final String WORLD_WIKIVOYAGE_FILE_NAME = "World_wikivoyage.travel.obf";
	public static final String ROUTE_ARTICLE = "route_article";
	public static final String ROUTE_ARTICLE_POINT = "route_article_point";
	public static final String ROUTE_TRACK = "route_track";
	public static final int POPULAR_ARTICLES_SEARCH_RADIUS = 100000;
	public static final int ARTICLE_SEARCH_RADIUS = 50000;
	public static final int GPX_TRACKS_SEARCH_RADIUS = 10000;
	public static final int MAX_POPULAR_ARTICLES_COUNT = 100;

	private final OsmandApplication app;
	private final Collator collator;

	private List<TravelArticle> popularArticles = new ArrayList<>();
	private final Map<TravelArticleIdentifier, Map<String, TravelArticle>> cachedArticles = new ConcurrentHashMap<>();
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
	public synchronized List<TravelArticle> loadPopularArticles() {
		String lang = app.getLanguage();
		List<TravelArticle> popularArticles = new ArrayList<>();
		final List<Pair<File, Amenity>> amenities = new ArrayList<>();
		final LatLon location = app.getMapViewTrackingUtilities().getMapLocation();
		for (final BinaryMapIndexReader reader : getReaders()) {
			try {
				searchAmenity(amenities, location, reader, POPULAR_ARTICLES_SEARCH_RADIUS, -1, ROUTE_ARTICLE);
				searchAmenity(amenities, location, reader, GPX_TRACKS_SEARCH_RADIUS, 15, ROUTE_TRACK);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}

		if (amenities.size() > 0) {
			Collections.sort(amenities, new Comparator<Pair<File, Amenity>>() {
				@Override
				public int compare(Pair article1, Pair article2) {
					int d1 = (int) (MapUtils.getDistance(((Amenity) article1.second).getLocation(), location));
					int d2 = (int) (MapUtils.getDistance(((Amenity) article2.second).getLocation(), location));
					return d1 < d2 ? -1 : (d1 == d2 ? 0 : 1);
				}
			});
			for (Pair<File, Amenity> amenity : amenities) {
				if (!Algorithms.isEmpty(amenity.second.getName(lang))) {
					TravelArticle article = cacheTravelArticles(amenity.first, amenity.second, lang, false);
					if (article != null) {
						popularArticles.add(article);
						if (popularArticles.size() >= MAX_POPULAR_ARTICLES_COUNT) {
							break;
						}
					}
				}
			}
		}
		this.popularArticles = popularArticles;
		return popularArticles;
	}

	private void searchAmenity(final List<Pair<File, Amenity>> amenitiesList, LatLon location,
	                           final BinaryMapIndexReader reader, int searchRadius, int zoom,
	                           String searchFilter) throws IOException {
		reader.searchPoi(BinaryMapIndexReader.buildSearchPoiRequest(
				location, searchRadius, zoom, getSearchFilter(searchFilter), new ResultMatcher<Amenity>() {
					@Override
					public boolean publish(Amenity object) {
						amenitiesList.add(new Pair<>(reader.getFile(), object));
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				}));
	}

	@Nullable
	private TravelArticle cacheTravelArticles(File file, Amenity amenity, String lang, boolean readPoints) {
		TravelArticle article = null;
		Map<String, TravelArticle> articles;
		if (amenity.getSubType().equals(ROUTE_TRACK)) {
			articles = readRoutePoint(file, amenity);
		} else {
			articles = readArticles(file, amenity, false);
		}
		if (!Algorithms.isEmpty(articles)) {
			TravelArticleIdentifier newArticleId = articles.values().iterator().next().generateIdentifier();
			cachedArticles.put(newArticleId, articles);
			article = getCachedArticle(newArticleId, lang, readPoints);
		}
		return article;
	}

	private Map<String, TravelArticle> readRoutePoint(File file, Amenity amenity) {
		Map<String, TravelArticle> articles = new HashMap<>();
		TravelGpx res = new TravelGpx();
		res.file = file;
		String title = amenity.getName("en");
		res.title = capitalizeFirstLetter(getGpxTitle(Algorithms.isEmpty(title) ? amenity.getName() : title));
		res.lat = amenity.getLocation().getLatitude();
		res.lon = amenity.getLocation().getLongitude();
		res.routeId = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID));
		try {
			res.totalDistance = Float.parseFloat(Algorithms.emptyIfNull(amenity.getTagContent(DISTANCE)));
		} catch (NumberFormatException e) {
			LOG.debug(e.getMessage(), e);
		}
		try {
			res.diffElevationUp = Double.parseDouble(Algorithms.emptyIfNull(amenity.getTagContent(DIFF_ELE_UP)));
		} catch (NumberFormatException e) {
			LOG.debug(e.getMessage(), e);
		}
		try {
			res.diffElevationDown = Double.parseDouble(Algorithms.emptyIfNull(amenity.getTagContent(DIFF_ELE_DOWN)));
		} catch (NumberFormatException e) {
			LOG.debug(e.getMessage(), e);
		}
		res.user = Algorithms.emptyIfNull(amenity.getTagContent(USER));
		articles.put("en", res);
		return articles;
	}


	private BinaryMapIndexReader.SearchPoiTypeFilter getSearchFilter(final String filterSubcategory) {
		return new BinaryMapIndexReader.SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				return subcategory.equals(filterSubcategory);
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};
	}

	@NonNull
	private Map<String, TravelArticle> readArticles(@NonNull File file, @NonNull Amenity amenity, boolean readPoints) {
		Map<String, TravelArticle> articles = new HashMap<>();
		Set<String> langs = getLanguages(amenity);
		for (String lang : langs) {
			articles.put(lang, readArticle(file, amenity, lang, readPoints));
		}
		return articles;
	}

	@NonNull
	private TravelArticle readArticle(@NonNull File file, @NonNull Amenity amenity, @NonNull String lang, boolean readPoints) {
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
		res.aggregatedPartOf = Algorithms.emptyIfNull(amenity.getTagContent(Amenity.IS_AGGR_PART, lang));
		if (readPoints) {
			res.gpxFile = buildGpxFile(res);
			res.gpxFileRead = true;
		}
		return res;
	}

	private GPXFile buildGpxFile(@NonNull TravelArticle article) {
		GPXFile gpxFile = null;
		List<Amenity> pointList = getPointList(article);
		if (!Algorithms.isEmpty(pointList)) {
			gpxFile = new GPXFile(article.getTitle(), article.getLang(), article.getContent());
			gpxFile.metadata.link = TravelArticle.getImageUrl(article.getImageTitle(), false);
			for (Amenity amenity : pointList) {
				WptPt wptPt = createWptPt(amenity, article.getLang());
				gpxFile.addPoint(wptPt);
			}
		}
		return gpxFile;
	}

	@Nullable
	private GPXFile buildTravelGpxFile(@NonNull final TravelGpx article) {
		String routeId = article.getRouteId();
		final String ref = routeId.substring(routeId.length() - 3);
		final List<BinaryMapDataObject> segmentList = new ArrayList<>();

		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				if (article.file != null && !article.file.equals(reader.getFile())) {
					continue;
				}
				BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> sr = BinaryMapIndexReader.buildSearchRequest(
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 15, null,
						new ResultMatcher<BinaryMapDataObject>() {
							@Override
							public boolean publish(BinaryMapDataObject object) {
								if (object.getPointsLength() > 1) {
									if (object.getObjectNames().get(2).equals(ref)
											&& capitalizeFirstLetter(getGpxTitle(object.getObjectNames().get(1))).equals(article.title)) {
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
				reader.searchMapIndex(sr);
				if (!Algorithms.isEmpty(segmentList)) {
					break;
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		GPXFile gpxFile = null;
		if (!segmentList.isEmpty()) {
			GPXUtilities.Track track = new GPXUtilities.Track();
			for (BinaryMapDataObject segment : segmentList) {
				List<WptPt> pointList = new ArrayList<>();
				GPXUtilities.TrkSegment trkSegment = new GPXUtilities.TrkSegment();
				for (int i = 0; i < segment.getPointsLength(); i++) {
					WptPt point = new WptPt();
					point.lat = MapUtils.get31LatitudeY(segment.getPoint31YTile(i));
					point.lon = MapUtils.get31LongitudeX(segment.getPoint31XTile(i));
					pointList.add(point);
				}
				trkSegment.points = pointList;
				track.segments.add(trkSegment);
			}
			gpxFile = new GPXFile(article.getTitle(), article.getLang(), "");
			gpxFile.tracks = new ArrayList<>();
			gpxFile.tracks.add(track);
		}
		article.gpxFile = gpxFile;
		return gpxFile;
	}

	@NonNull
	private List<Amenity> getPointList(@NonNull final TravelArticle article) {
		final List<Amenity> pointList = new ArrayList<>();
		final String lang = article.getLang();
		for (BinaryMapIndexReader reader : getReaders()) {
			try {
				if (article.file != null && !article.file.equals(reader.getFile())) {
					continue;
				}
				SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(0, 0,
						Algorithms.emptyIfNull(article.title), 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
						getSearchFilter(ROUTE_ARTICLE_POINT), new ResultMatcher<Amenity>() {

							@Override
							public boolean publish(Amenity amenity) {
								String amenityLang = amenity.getTagSuffix(Amenity.LANG_YES + ":");
								if (Algorithms.stringsEqual(lang, amenityLang)
										&& Algorithms.stringsEqual(article.routeId,
										Algorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID)))) {
									pointList.add(amenity);
								}
								return false;
							}

							@Override
							public boolean isCancelled() {
								return false;
							}
						}, null);

					if (!Algorithms.isEmpty(article.title)) {
						reader.searchPoiByName(req);
					} else {
						reader.searchPoi(req);
					}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
		return pointList;
	}

	@NonNull
	private WptPt createWptPt(@NonNull Amenity amenity, @Nullable String lang) {
		WptPt wptPt = new WptPt();
		wptPt.name = amenity.getName();
		wptPt.lat = amenity.getLocation().getLatitude();
		wptPt.lon = amenity.getLocation().getLongitude();
		wptPt.desc = amenity.getDescription(lang);
		wptPt.link = amenity.getSite();
		String color = amenity.getColor();
		if (color != null) {
			wptPt.setColor(ColorDialogs.getColorByTag(color));
		}
		String iconName = amenity.getGpxIcon();
		if (iconName != null) {
			wptPt.setIconName(iconName);
		}
		String category = amenity.getTagSuffix("category_");
		if (category != null) {
			wptPt.category = capitalizeFirstLetter(category);
		}
		return wptPt;
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
								List<String> otherNames = object.getAllNames(false);
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
						TravelArticle article = readArticle(file, amenity, appLang, false);
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
				article = readArticle(reader.getFile(), amenities.get(0), lang, false);
			}
		}
		return article;
	}

	@Override
	public TravelArticle getArticleById(@NonNull TravelArticleIdentifier articleId, @NonNull String lang) {
		TravelArticle article = getCachedArticle(articleId, lang, true);
		return article == null ? localDataHelper.getSavedArticle(articleId.file, articleId.routeId, lang) : article;
	}

	@Nullable
	private TravelArticle getCachedArticle(@NonNull TravelArticleIdentifier articleId, @NonNull String lang, boolean forceReadPoints) {
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
		if (article == null) {
			article = findArticleById(articleId, lang);
		}
		if (article != null && !article.gpxFileRead && forceReadPoints) {
			article.gpxFile = buildGpxFile(article);
			article.gpxFileRead = true;
		}
		return article;
	}

	private TravelArticle findArticleById(@NonNull final TravelArticleIdentifier articleId, final String lang) {
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
				article = cacheTravelArticles(reader.getFile(), amenities.get(0), lang, true);
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
				article = cacheTravelArticles(reader.getFile(), amenities.get(0), lang, true);
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
	public String getGPXName(@NonNull final TravelArticle article) {
		return article.getTitle().replace('/', '_').replace('\'', '_')
				.replace('\"', '_') + IndexConstants.GPX_FILE_EXT;
	}

	@NonNull
	@Override
	public File createGpxFile(@NonNull final TravelArticle article) {
		final GPXFile gpx;
		if (article instanceof TravelGpx) {
			gpx = buildTravelGpxFile((TravelGpx) article);
		} else {
			gpx = article.getGpxFile();
		}
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
}