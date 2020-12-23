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
	List<WikivoyageSearchResult> search(@NonNull String searchQuery);

	@NonNull
	List<TravelArticle> getPopularArticles();

	@NonNull
	Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> getNavigationMap(@NonNull final TravelArticle article);

	@Nullable
	TravelArticle getArticleById(@NonNull String routeId, @NonNull String lang);

	@Nullable
	TravelArticle getArticleByTitle(@NonNull String title, @NonNull String lang);

	@NonNull
	String getArticleId(@NonNull String title, @NonNull String lang);

	@NonNull
	ArrayList<String> getArticleLangs(@NonNull String routeId);

	@NonNull
	String getGPXName(@NonNull final TravelArticle article);

	@NonNull
	File createGpxFile(@NonNull final TravelArticle article);

	// TODO: this method should be deleted once TravelDBHelper is deleted
	// For TravelOBFHelper it could always return "" and should be no problem
	// Bookmarks should be refactored properly to support multiple files
	String getSelectedTravelBookName();
}
