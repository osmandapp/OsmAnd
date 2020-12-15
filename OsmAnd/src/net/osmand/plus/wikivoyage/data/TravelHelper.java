package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface TravelHelper {

	TravelLocalDataHelper getBookmarksHelper();

	void initializeDataOnAppStartup();

	void initializeDataToDisplay();

	boolean isAnyTravelBookPresent();

	@NonNull
	List<WikivoyageSearchResult> search(final String searchQuery);

	@NonNull
	List<TravelArticle> getPopularArticles();

	Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> getNavigationMap(
			final TravelArticle article);

	@Nullable
	TravelArticle getArticleById(String routeId, String lang);

	TravelArticle getArticleByTitle(String title, String lang);

	String getArticleId(String title, String lang);

	ArrayList<String> getArticleLangs(String routeId);

	String getGPXName(TravelArticle article);

	File createGpxFile(TravelArticle article);

	// TODO: this method should be deleted once TravelDBHelper is deleted
	// For TravelOBFHelper it could always return "" and should be no problem
	// Bookmarks should be refactored properly to support multiple files
	String getSelectedTravelBookName();
}
