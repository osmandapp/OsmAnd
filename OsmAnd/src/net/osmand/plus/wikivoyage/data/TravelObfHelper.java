package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryIndexPart;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;


public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);

	private static final String WIKIVOYAGE_OBF = "Wikivoyage.obf";
	public static final String ROUTE_ARTICLE = "route_article";

	private final OsmandApplication application;
	private Collator collator;
	private TravelLocalDataHelper localDataHelper;

	private File selectedTravelBook = null;
	private List<File> existingTravelBooks = new ArrayList<>();
	private List<TravelArticle> popularArticles = new ArrayList<TravelArticle>();

	private TLongObjectMap<TravelArticle> cachedArticles;


	public TravelObfHelper(OsmandApplication application) {
		this.application = application;
		collator = OsmAndCollator.primaryCollator();
		localDataHelper = new TravelLocalDataHelper(application);
		cachedArticles = new TLongObjectHashMap<>();
	}

	public static boolean checkIfObfFileExists(OsmandApplication app) {
		File[] files = app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.getName().equals(WIKIVOYAGE_OBF)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public TravelLocalDataHelper getLocalDataHelper() {
		return localDataHelper;
	}

	/**
	 * TODO
	 * 1. implement regional travelbooks
	 * 2. check settings for default?
	 */
	public void initTravelBooks() {
		List<File> files = getPossibleFiles();
		String travelBook = application.getSettings().SELECTED_TRAVEL_BOOK.get();
		existingTravelBooks.clear();
		if (files != null && !files.isEmpty()) {
			for (File f : files) {
				existingTravelBooks.add(f);
				if (selectedTravelBook == null) {
					selectedTravelBook = f;
				} else if (Algorithms.objectEquals(travelBook, f.getName())) {
					selectedTravelBook = f;
				}
			}
			selectedTravelBook = files.get(0);
		} else {
			selectedTravelBook = null;
		}

	}

	/**
	 * todo: get all obf files from folder, may be we should add some suffix like 'wikivoyage'
	 * to filenames to distinguish from other maps? Or add some checks right there.
	 */
	@Nullable
	private List<File> getPossibleFiles() {
		File[] files = application.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).listFiles();
		if (files != null) {
			List<File> res = new ArrayList<>();
			for (File file : files) {
				if (file.getName().equals("Wikivoyage.obf")) {
					res.add(file);
					LOG.debug(String.format("FIle name: %s", file.getAbsolutePath()));
				}
			}
			return res;
		}
		return null;
	}

	public void loadDataForSelectedTravelBook() {
		localDataHelper.refreshCachedData();
		loadPopularArticles();
	}

	@Override
	public File getSelectedTravelBook() {
		return selectedTravelBook;
	}

	@Override
	public List<File> getExistingTravelBooks() {
		return existingTravelBooks;
	}

	@Override
	public void selectTravelBook(File f) {
		//todo
	}

	@NonNull
	@Override
	public List<WikivoyageSearchResult> search(String searchQuery) {
		List<WikivoyageSearchResult> res = new ArrayList<>();
		CollatorStringMatcher matcher = new CollatorStringMatcher(searchQuery,
				CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE);

		for (TravelArticle article : popularArticles) {
			if (checkArticleMatches(matcher, article)) {

				WikivoyageSearchResult searchResult = convertArticleToSearchResult(article);
				res.add(searchResult);
			}
		}
		res = Lists.newArrayList(groupSearchResultsByCityId(res).iterator());
		sortSearchResults(searchQuery, res);
		return res;
	}

	private WikivoyageSearchResult convertArticleToSearchResult(TravelArticle article) {
		WikivoyageSearchResult searchResult = new WikivoyageSearchResult();
		searchResult.articleTitles = Collections.singletonList(article.title);
		searchResult.isPartOf = Collections.singletonList(article.isPartOf);
		searchResult.imageTitle = article.imageTitle;
		searchResult.langs = Collections.singletonList(article.lang);
		searchResult.tripId = article.tripId;
		return searchResult;
	}

	private Collection<WikivoyageSearchResult> groupSearchResultsByCityId(List<WikivoyageSearchResult> res) {
		String baseLng = application.getLanguage();
		TLongObjectHashMap<WikivoyageSearchResult> wikivoyage = new TLongObjectHashMap<>();
		for (WikivoyageSearchResult rs : res) {
			WikivoyageSearchResult prev = wikivoyage.get(rs.tripId);
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
				wikivoyage.put(rs.tripId, rs);
			}
		}
		return wikivoyage.valueCollection();
	}

	private void sortSearchResults(final String searchQuery, List<WikivoyageSearchResult> list) {
		Collections.sort(list, new Comparator<WikivoyageSearchResult>() {
			@Override
			public int compare(WikivoyageSearchResult o1, WikivoyageSearchResult o2) {
				boolean c1 = CollatorStringMatcher.cmatches(collator, searchQuery, o1.articleTitles.get(0),
						CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH);
				boolean c2 = CollatorStringMatcher.cmatches(collator, searchQuery, o2.articleTitles.get(0),
						CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH);
				if (c1 == c2) {
					return collator.compare(o1.articleTitles.get(0), o2.articleTitles.get(0));
				} else if (c1) {
					return -1;
				} else if (c2) {
					return 1;
				}
				return 0;
			}
		});
	}

	private boolean checkArticleMatches(CollatorStringMatcher matcher, TravelArticle article) {
		return matcher.matches(article.getTitle())
				|| matcher.matches(article.getContent())
				|| matcher.matches(article.getContentsJson());
	}

	@NonNull
	public List<TravelArticle> getPopularArticles() {
		return popularArticles;
	}

	//TODO for now it reads any articles, since we didn't have popular articles in the obf
	@NonNull
	public List<TravelArticle> loadPopularArticles() {
		String language = application.getLanguage();
		final List<Amenity> articles = new ArrayList<>();
		try {
			BinaryMapIndexReader bookIndexReader = getBookBinaryIndex();
			if (bookIndexReader == null) {
				popularArticles = new ArrayList<>();
				return popularArticles;
			}
			LatLon ll = application.getMapViewTrackingUtilities().getMapLocation();
			float coeff = 2;
			BinaryMapIndexReader.SearchRequest<Amenity> req =
					BinaryMapIndexReader.buildSearchPoiRequest(
							MapUtils.get31TileNumberX(ll.getLongitude() - coeff),
							MapUtils.get31TileNumberX(ll.getLongitude() + coeff),
							MapUtils.get31TileNumberY(ll.getLatitude() + coeff),
							MapUtils.get31TileNumberY(ll.getLatitude() - coeff),
							-1,
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

			bookIndexReader.searchPoi(req);
			bookIndexReader.close();

			if (articles.size() > 0) {
				Iterator<Amenity> it = articles.iterator();
				while (it.hasNext()) {
					Amenity a = it.next();
					if (!a.getName(language).equals("")) {
						TravelArticle article = readArticle(a, language);
						popularArticles.add(article);
						writeToCache(article);
					}
				}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
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
		res.tripId = getTripId(amenity);
		res.originalId = 0; //?
		res.lang = lang;
		res.contentsJson = amenity.getTagContent(Amenity.CONTENT_JSON, lang) == null ? "" : amenity.getTagContent(Amenity.CONTENT_JSON, lang);
		res.aggregatedPartOf = amenity.getTagContent(Amenity.IS_AGGR_PART, lang) == null ? "" : amenity.getTagContent(Amenity.IS_AGGR_PART, lang);

//      occasional crashes
//		try {
//			String gpxContent = amenity.getAdditionalInfo("gpx_info");
//			res.gpxFile = GPXUtilities.loadGPXFile(new ByteArrayInputStream(gpxContent.getBytes("UTF-8")));
//		} catch (IOException e) {
//			LOG.error(e.getMessage(), e);
//		}

		return res;
	}

	private long getTripId(Amenity amenity) {
		long tripId = -1;
		String val = amenity.getTagContent(Amenity.ROUTE_ID, null);
		if (val != null && val.startsWith("Q")) {
			try {
				tripId = Long.parseLong(val.substring(1));
			} catch (NumberFormatException nfe) {
				LOG.error(nfe.getMessage());
			}
		}
		return tripId;
	}

	private BinaryMapIndexReader getBookBinaryIndex() throws IOException {
		application.getSettings().SELECTED_TRAVEL_BOOK.set(selectedTravelBook.getName());
		try {
			RandomAccessFile r = new RandomAccessFile(selectedTravelBook.getAbsolutePath(), "r");
			BinaryMapIndexReader index = new BinaryMapIndexReader(r, selectedTravelBook);
			for (BinaryIndexPart p : index.getIndexes()) {
				if (p instanceof BinaryMapPoiReaderAdapter.PoiRegion) {
					return index;
				}
			}
		} catch (IOException e) {
			System.err.println("File doesn't have valid structure : " + selectedTravelBook.getName() + " " + e.getMessage());
			throw e;
		}
		return null;
	}

	@Override
	public LinkedHashMap<WikivoyageSearchResult, List<WikivoyageSearchResult>> getNavigationMap(TravelArticle article) {
		return null;
	}

	private void writeToCache(TravelArticle article) {
		cachedArticles.put(article.tripId, article);
	}

	private TravelArticle getArticleFromCache(long tripId, String lang) {

		TravelArticle article = cachedArticles.get(tripId);
//		if (aa != null) {
//			article = readArticle(aa, lang);
//		}
		return article;
	}

	@Override
	public TravelArticle getArticle(long resId, String lang) {
		TravelArticle article = getArticleFromCache(resId, lang);
		if (article != null) {
			return article;
		}
		String name = ""; //???
		return getArticle(name, lang);
	}

	@Override
	public TravelArticle getArticle(final String title, final String lang) {
		TravelArticle res = null;
		List<Amenity> amenities = Collections.emptyList();
		try {
			BinaryMapIndexReader indexReader = getBookBinaryIndex();
			if (indexReader != null) {
				int left = 0;
				int top = 0;
				int right = Integer.MAX_VALUE;
				int bottom = Integer.MAX_VALUE;
				LatLon ll = application.getMapViewTrackingUtilities().getMapLocation();
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

				amenities = indexReader.searchPoiByName(req);
			}
		} catch (IOException e) {
			//todo
		}
		if (!amenities.isEmpty()) {
			for (Amenity a : amenities) {
				LOG.debug("searched article: " + a);
			}
		}
		return res;
	}

	@Override
	public long getArticleId(String title, String lang) {
		return 0;
	}


	//TODO finish stub
	@Override
	public ArrayList<String> getArticleLangs(long cityId) {
		ArrayList<String> res = new ArrayList<>();
		res.add("en");

		for (TravelArticle article : popularArticles) {
			if (article.getTripId() == cityId) {
				res.add(article.getLang());
			}
		}
		return res;
	}

	@Override
	public String formatTravelBookName(File tb) {
		if (tb == null) {
			return application.getString(R.string.shared_string_none);
		}
		String nm = tb.getName();
		return nm.substring(0, nm.indexOf('.')).replace('_', ' ');
	}

	@Override
	public String getGPXName(TravelArticle article) {
		return article.getTitle().replace('/', '_').replace('\'', '_')
				.replace('\"', '_') + IndexConstants.GPX_FILE_EXT;
	}

	@Override
	public File createGpxFile(TravelArticle article) {
		final GPXUtilities.GPXFile gpx = article.getGpxFile();
		File file = application.getAppPath(IndexConstants.GPX_TRAVEL_DIR + getGPXName(article));
		if (!file.exists()) {
			GPXUtilities.writeGpxFile(file, gpx);
		}
		return file;
	}
}
