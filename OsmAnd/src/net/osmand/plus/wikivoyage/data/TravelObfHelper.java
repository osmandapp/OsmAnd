package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryIndexPart;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);
	public static final String ROUTE_ARTICLE = "route_article";

	private final OsmandApplication app;

	private File selectedTravelBook = null;
	private final List<File> existingTravelBooks = new ArrayList<>();
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

		BinaryMapIndexReader[] readers = app.getResourceManager().getTravelFiles();
		String travelBook = app.getSettings().SELECTED_TRAVEL_BOOK.get();
		existingTravelBooks.clear();
		if (readers != null) {
			for (BinaryMapIndexReader reader : readers) {
				File f = reader.getFile();
				existingTravelBooks.add(f);
				if (selectedTravelBook == null) {
					selectedTravelBook = f;
				} else if (Algorithms.objectEquals(travelBook, f.getName())) {
					selectedTravelBook = f;
				}
				selectedTravelBook = reader.getFile();
			}
		}
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
		try {
			BinaryMapIndexReader bookIndexReader = getBookBinaryIndex();
			if (bookIndexReader == null) {
				popularArticles = new ArrayList<>();
				return popularArticles;
			}
			LatLon ll = app.getMapViewTrackingUtilities().getMapLocation();
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
						cachedArticles.put(article.routeId, article);
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
		res.routeId = getRouteId(amenity);
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

	private String getRouteId(Amenity amenity) {
		return amenity.getTagContent(Amenity.ROUTE_ID, null);
	}


	private BinaryMapIndexReader getBookBinaryIndex() throws IOException {
		app.getSettings().SELECTED_TRAVEL_BOOK.set(selectedTravelBook.getName());
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
	public boolean isAnyTravelBookPresent() {
		return selectedTravelBook != null;
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
		TravelArticle article = cachedArticles.get(routeId);
		if (article != null) {
			return article;
		}
		String name = ""; //???
		return getArticleByTitle(name, lang);
	}

	@Override
	public TravelArticle getArticleByTitle(final String title, final String lang) {
		TravelArticle res = null;
		List<Amenity> amenities = Collections.emptyList();
		try {
			BinaryMapIndexReader indexReader = getBookBinaryIndex();
			if (indexReader != null) {
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
