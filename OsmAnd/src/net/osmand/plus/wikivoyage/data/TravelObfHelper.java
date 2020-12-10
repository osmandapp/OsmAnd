package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CollatorStringMatcher;
import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.MapObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.api.SQLiteAPI;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TravelObfHelper implements TravelHelper {

	private static final Log LOG = PlatformUtil.getLog(TravelObfHelper.class);

	private static final int POPULAR_LIMIT = 25;

	private final OsmandApplication application;

	private TravelLocalDataHelper localDataHelper;
	private SQLiteAPI.SQLiteConnection connection = null;

	private File selectedTravelBook = null;
	private List<BinaryMapIndexReader> files;
	private List<File> existingTravelBooks = new ArrayList<>();
	private List<TravelArticle> popularArticles = new ArrayList<TravelArticle>();


	public TravelObfHelper(OsmandApplication application) {
		this.application = application;
		localDataHelper = new TravelLocalDataHelper(application);
	}

	public TravelLocalDataHelper getBookmarksHelper() {
		return localDataHelper;
	}

	@Override
	public boolean isAnyTravelBookPresent() {
		return selectedTravelBook != null;
	}

	public void initializeDataOnAppStartup() {
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
		} else {
			selectedTravelBook = null;
		}
	}

	@Nullable
	private List<File> getPossibleFiles() {
		File[] files = application.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).listFiles();
		if (files != null) {
			List<File> res = new ArrayList<>();
			for (File file : files) {
				if (file.getName().endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT)) {
					res.add(file);
				}
			}
			return res;
		}
		return null;
	}

	public void initializeDataToDisplay() {
		localDataHelper.refreshCachedData();
		loadPopularArticles();
	}


	public String getSelectedTravelBookName() {
		if (selectedTravelBook != null) {
			return selectedTravelBook.getName();
		}
		return null;
	}

	public List<File> getExistingTravelBooks() {
		return existingTravelBooks;
	}

	@NonNull
	public List<WikivoyageSearchResult> search(final String searchQuery) {
		// TODO remove
		this.files = application.getResourceManager().getTravelFiles();
		List<WikivoyageSearchResult> res = new ArrayList<>();
		List<Amenity> searchObjects = new ArrayList<>();
		for (BinaryMapIndexReader reader : files) {
			try {
				BinaryMapIndexReader.SearchRequest<Amenity> searchRequest = BinaryMapIndexReader.
						buildSearchPoiRequest(0, 0, searchQuery,
								0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, null);

				searchObjects = reader.searchPoiByName(searchRequest);
			} catch (IOException e) {
				LOG.error(e);
			}
		}
		for (MapObject obj : searchObjects) {
			//TODO map
			WikivoyageSearchResult r = new WikivoyageSearchResult();
			r.articleTitles = Collections.singletonList(obj.getName());
			r.langs = Collections.singletonList(obj.getName());
			r.imageTitle = (obj.getName());
			r.isPartOf = Collections.singletonList(obj.getName());
			r.routeId = "routeid";//obj.getId();
			res.add(r);
		}
		return res;
	}

	@NonNull
	public List<TravelArticle> getPopularArticles() {
		return popularArticles;
	}

	@Override
	public Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> getNavigationMap(TravelArticle article) {
		return null;
	}

	@Override
	public TravelArticle getArticleById(String routeId, String lang) {
		return null;
	}

	@Override
	public TravelArticle getArticleByTitle(String title, String lang) {
		return null;
	}

	@Override
	public String getArticleId(String title, String lang) {
		return null;
	}

	@Override
	public ArrayList<String> getArticleLangs(String articleId) {
		return null;
	}

	@NonNull
	public List<TravelArticle> loadPopularArticles() {
		popularArticles = new ArrayList<>();
		return popularArticles;
	}

	public String formatTravelBookName(File tb) {
		if (tb == null) {
			return application.getString(R.string.shared_string_none);
		}
		String nm = tb.getName();
		return nm.substring(0, nm.indexOf('.')).replace('_', ' ');
	}

	public String getGPXName(TravelArticle article) {
		return article.getTitle().replace('/', '_').replace('\'', '_')
				.replace('\"', '_') + IndexConstants.GPX_FILE_EXT;
	}

	public File createGpxFile(TravelArticle article) {
		final GPXUtilities.GPXFile gpx = article.getGpxFile();
		File file = application.getAppPath(IndexConstants.GPX_TRAVEL_DIR + getGPXName(article));
		if (!file.exists()) {
			GPXUtilities.writeGpxFile(file, gpx);
		}
		return file;
	}

	// might use in future
	protected static class PopularArticle {
		String tripId;
		String title;
		String lang;
		int popIndex;
		int order;
		double lat;
		double lon;

		public boolean isLocationSpecified() {
			return !Double.isNaN(lat) && !Double.isNaN(lon);
		}
	}
}
