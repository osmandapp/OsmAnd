package net.osmand.plus.wikivoyage.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface TravelHelper {

	TravelLocalDataHelper getLocalDataHelper();

	void initTravelBooks();

	void loadDataForSelectedTravelBook();

	File getSelectedTravelBook();

	List<File> getExistingTravelBooks();

	void selectTravelBook(File f);

	@NonNull
	List<WikivoyageSearchResult> search(final String searchQuery);

	@NonNull
	List<TravelArticle> getPopularArticles();

	@NonNull
	List<TravelArticle> loadPopularArticles();

	LinkedHashMap<WikivoyageSearchResult, List<WikivoyageSearchResult>> getNavigationMap(
			final TravelArticle article);

	TravelArticle getArticle(long cityId, String lang);

	TravelArticle getArticle(String title, String lang);

	long getArticleId(String title, String lang);

	ArrayList<String> getArticleLangs(long cityId);

	String formatTravelBookName(File tb);

	String getGPXName(TravelArticle article);

	File createGpxFile(TravelArticle article);
}
