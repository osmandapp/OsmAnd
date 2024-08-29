package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface TravelHelper {

	interface GpxReadCallback {
		void onGpxFileReading();

		void onGpxFileRead(@Nullable GpxFile gpxFile);
	}


	TravelLocalDataHelper getBookmarksHelper();

	void initializeDataOnAppStartup();

	void initializeDataToDisplay(boolean resetData);

	boolean isAnyTravelBookPresent();

	@NonNull
	List<WikivoyageSearchResult> search(@NonNull String searchQuery, int requestNumber);

	@NonNull
	List<TravelArticle> getPopularArticles();

	@NonNull
	Map<WikivoyageSearchResult, List<WikivoyageSearchResult>> getNavigationMap(@NonNull TravelArticle article);

	@Nullable
	TravelArticle getArticleById(@NonNull TravelArticleIdentifier articleId, @Nullable String lang, boolean readGpx, @Nullable GpxReadCallback callback);

	@Nullable
	TravelArticle findSavedArticle(@NonNull TravelArticle savedArticle);

	@Nullable
	TravelArticle getArticleByTitle(@NonNull String title, @NonNull String lang, boolean readGpx, @Nullable GpxReadCallback callback);

	@Nullable
	TravelArticle getArticleByTitle(@NonNull String title, @NonNull LatLon latLon, @NonNull String lang, boolean readGpx, @Nullable GpxReadCallback callback);

	@Nullable
	TravelArticle getArticleByTitle(@NonNull String title, @NonNull QuadRect rect, @NonNull String lang, boolean readGpx, @Nullable GpxReadCallback callback);

	@Nullable
	TravelArticleIdentifier getArticleId(@NonNull String title, @NonNull String lang);

	@NonNull
	ArrayList<String> getArticleLangs(@NonNull TravelArticleIdentifier articleId);

	@Nullable
	TravelGpx searchGpx(@NonNull LatLon location, @Nullable String fileName, @Nullable String ref);

	void openTrackMenu(@NonNull TravelArticle article, @NonNull MapActivity mapActivity,
					   @NonNull String gpxFileName, @NonNull LatLon location);

	@NonNull
	String getGPXName(@NonNull TravelArticle article);

	@NonNull
	File createGpxFile(@NonNull TravelArticle article);

	// TODO: this method should be deleted once TravelDBHelper is deleted
	@Nullable
	String getSelectedTravelBookName();

	String getWikivoyageFileName();

	void saveOrRemoveArticle(@NonNull TravelArticle article, boolean save);

}
