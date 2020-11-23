package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryIndexPart;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.actions.OsmAndAction;
import net.osmand.plus.api.SQLiteAPI;
import net.osmand.plus.resources.AmenityIndexRepository;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class TravelObfHelper implements TravelHelper{

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);

	private final OsmandApplication application;
	private Collator collator;
	private TravelLocalDataHelper localDataHelper;

	private File selectedTravelBook = null;
	private List<File> existingTravelBooks = new ArrayList<>();
	private List<TravelArticle> popularArticles = new ArrayList<TravelArticle>();

	private BinaryMapIndexReader index = null;


	public TravelObfHelper(OsmandApplication application) {
		this.application = application;
		collator = OsmAndCollator.primaryCollator();
		localDataHelper = new TravelLocalDataHelper(application); //will I need it?
	}

	public static boolean checkIfObfFileExists(OsmandApplication app) {
		File[] files = app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.getName().equals("Wikivoyage.obf")) {
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

	/** TODO
	 * 1.implement regional travelbooks
	 * 2. check settings for default?
	 */
	public void initTravelBooks() {
		List<File> files = getPossibleFiles();
		String travelBook = application.getSettings().SELECTED_TRAVEL_BOOK.get();
		existingTravelBooks.clear();
		if (files != null && !files.isEmpty()) {
			for (File f : files) {
				existingTravelBooks.add(f);
//				if (selectedTravelBook == null) {
//					selectedTravelBook = f;
//				} else if (Algorithms.objectEquals(travelBook, f.getName())) {
//					selectedTravelBook = f;
//				}
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
		return null;
	}

	@Override
	public void selectTravelBook(File f) {

	}

	@NonNull
	@Override
	public List<WikivoyageSearchResult> search(String searchQuery) {
		return null;
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
									if (object.getSubType().equals("route_article")) {
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
					popularArticles.add(readArticle(a, language));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return popularArticles;
	}


	private TravelArticle readArticle(Amenity amenity, String lang) {
		TravelArticle res = new TravelArticle();

		res.title = amenity.getName(lang).equals("") ? amenity.getName() : amenity.getName(lang);
		res.content = amenity.getDescription(lang);
		res.isPartOf = ""; //add to file
		res.lat = amenity.getLocation().getLatitude();
		res.lon = amenity.getLocation().getLongitude();
		res.imageTitle = "";//add to file
		res.tripId = amenity.getId();
		res.originalId = amenity.getId()>>6;
		res.lang = lang;
		res.contentsJson = ""; //add to file
		res.aggregatedPartOf = ""; //add to file
//		try {
//			String gpxContent = Algorithms.gzipToString();
			res.gpxFile = null; //GPXUtilities.loadGPXFile(new ByteArrayInputStream(gpxContent.getBytes("UTF-8")));
//		} catch (IOException e) {
//			LOG.error(e.getMessage(), e);
//		}

		return res;
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

	@Override
	public TravelArticle getArticle(long cityId, String lang) {
		return null;
	}

	@Override
	public TravelArticle getArticle(String title, String lang) {
		return null;
	}

	@Override
	public long getArticleId(String title, String lang) {
		return 0;
	}

	@Override
	public ArrayList<String> getArticleLangs(long cityId) {
		return null;
	}

	@Override
	public String formatTravelBookName(File tb) {
		return null;
	}

	@Override
	public String getGPXName(TravelArticle article) {
		return null;
	}

	@Override
	public File createGpxFile(TravelArticle article) {
		return null;
	}
}
