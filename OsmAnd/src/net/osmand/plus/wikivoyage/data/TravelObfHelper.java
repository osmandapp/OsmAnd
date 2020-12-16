package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
		List<Amenity> searchObjects = new ArrayList<>();
		for (BinaryMapIndexReader reader : application.getResourceManager().getTravelRepositories()) {
			try {
				BinaryMapIndexReader.SearchRequest<Amenity> searchRequest = BinaryMapIndexReader.
						buildSearchPoiRequest(0, 0, searchQuery,
								0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, null);

				searchObjects = reader.searchPoiByName(searchRequest);
			} catch (IOException e) {
				LOG.error(e);
			}
		}
		for (Amenity obj : searchObjects) {
			WikivoyageSearchResult r = new WikivoyageSearchResult();
			TravelArticle article = readArticle(obj, "en");
			r.articleTitles = new ArrayList<>(Collections.singletonList(article.title));
			r.imageTitle = article.imageTitle;
			r.routeId = article.routeId;
			r.isPartOf = new ArrayList<>(Collections.singletonList(article.isPartOf));
			r.langs = new ArrayList<>(Collections.singletonList("en"));
			res.add(r);
		}
		res = new ArrayList(groupSearchResultsByRouteId(res));
		sortSearchResults(searchQuery, res);
		return res;
	}

	private void sortSearchResults(final String searchQuery, List<WikivoyageSearchResult> list) {
		Collections.sort(list, new Comparator<WikivoyageSearchResult>() {
			@Override
			public int compare(WikivoyageSearchResult t0, WikivoyageSearchResult t1) {
				return t0.articleTitles.get(0).compareTo(t1.articleTitles.get(0));
			}
		});
	}

	private Collection<WikivoyageSearchResult> groupSearchResultsByRouteId(List<WikivoyageSearchResult> res) {
		String baseLng = application.getLanguage();
		Map<String, WikivoyageSearchResult> wikivoyage = new HashMap<>();
		for (WikivoyageSearchResult rs : res) {
			WikivoyageSearchResult prev = wikivoyage.get(rs.routeId);
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
				wikivoyage.put(rs.routeId, rs);
			}
		}
		return wikivoyage.values();
	}

	private TravelArticle readArticle(Amenity amenity, String lang) {
		TravelArticle res = new TravelArticle();
		res.title = amenity.getName(lang).equals("") ? amenity.getName() : amenity.getName(lang);
		res.content = amenity.getDescription(lang);
		res.isPartOf = amenity.getTagContent(Amenity.IS_PART, lang) == null ? "" : amenity.getTagContent(Amenity.IS_PART, lang);
		res.lat = amenity.getLocation().getLatitude();
		res.lon = amenity.getLocation().getLongitude();
		res.imageTitle = amenity.getTagContent(Amenity.IMAGE_TITLE, lang) == null ? "" : amenity.getTagContent(Amenity.IMAGE_TITLE, lang);
		String routeId = amenity.getAdditionalInfo("route_id");
		res.routeId = routeId == null || routeId.equals("") ? "" : routeId;
		res.originalId = amenity.getId();
		res.lang = lang;
		res.contentsJson = amenity.getTagContent(Amenity.CONTENT_JSON, lang) == null ? "" : amenity.getTagContent(Amenity.CONTENT_JSON, lang);
		res.aggregatedPartOf = amenity.getTagContent(Amenity.IS_AGGR_PART, lang) == null ? "" : amenity.getTagContent(Amenity.IS_AGGR_PART, lang);
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
