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


	private final OsmandApplication application;

	private TravelLocalDataHelper localDataHelper;

	private List<TravelArticle> popularArticles = new ArrayList<TravelArticle>();


	public TravelObfHelper(OsmandApplication application) {
		this.application = application;
		localDataHelper = new TravelLocalDataHelper(application);
	}

	public TravelLocalDataHelper getBookmarksHelper() {
		return localDataHelper;
	}

	@Override
	public void initializeDataOnAppStartup() {

	}

	@Override
	public boolean isAnyTravelBookPresent() {
		return checkIfObfFileExists(application);
	}


	public void initializeDataToDisplay() {
		localDataHelper.refreshCachedData();
		loadPopularArticles();
	}


	@NonNull
	public List<WikivoyageSearchResult> search(final String searchQuery) {
		List<WikivoyageSearchResult> res = new ArrayList<>();
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

	@Override
	public String getSelectedTravelBookName() {
		return null;
	}

	public static boolean checkIfObfFileExists(OsmandApplication app) {
		File[] files = app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.getName().contains(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)) {
					return true;
				}
			}
		}
		return false;
	}
}
