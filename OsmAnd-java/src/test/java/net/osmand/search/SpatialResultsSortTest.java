package net.osmand.search;

import static org.junit.Assert.assertEquals;

import net.osmand.data.LatLon;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.SearchSettings.SortType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpatialResultsSortTest {

	private static final LatLon CENTER = new LatLon(49.84, 24.03);

	@Test
	public void nearestSortsVisibleResultsByDistance() {
		assertEquals(Arrays.asList("near", "middle", "far"), names(collection(SortType.ONLY_BY_DISTANCE, 0)));
	}

	@Test
	public void relevanceKeepsEngineOrder() {
		assertEquals(Arrays.asList("far", "near", "middle"), names(collection(SortType.BY_RELEVANCE, 0)));
	}

	@Test
	public void nearestDoesNotShowHiddenLevels() {
		SearchResultCollection collection = collection(SortType.ONLY_BY_DISTANCE, 0);
		assertEquals(3, collection.getVisibleSpatialSearchResults().size());
		collection.showMoreSpatialSearchResults();
		assertEquals(Arrays.asList("next level", "near", "middle", "far"), names(collection));
	}

	@Test
	public void nearestKeepsRowsWithoutLocationOnTop() {
		SearchResultCollection collection = collection(SortType.ONLY_BY_DISTANCE, 0);
		SearchResult category = result(collection.getPhrase(), "category", null, 0);
		collection.addSearchResults(Collections.singletonList(category), false, false);
		assertEquals("category", names(collection).get(0));
	}

	private SearchResultCollection collection(SortType sortType, int visibleLevel) {
		SearchSettings settings = new SearchSettings(Collections.emptyList()).setOriginalLocation(CENTER);
		settings.setSortType(sortType);
		SearchPhrase phrase = SearchPhrase.emptyPhrase(settings);
		List<SearchResult> results = Arrays.asList(
				result(phrase, "far", new LatLon(49.87, 24.03), 0),
				result(phrase, "near", new LatLon(49.841, 24.03), 0),
				result(phrase, "middle", new LatLon(49.85, 24.03), 0),
				result(phrase, "next level", new LatLon(49.8405, 24.03), 1));
		return new SearchResultCollection(phrase, true, visibleLevel).addSearchResults(results, false, false);
	}

	private SearchResult result(SearchPhrase phrase, String name, LatLon location, int level) {
		SearchResult result = new SearchResult(phrase);
		result.localeName = name;
		result.location = location;
		result.spatialSearchVisibleLevel = level;
		return result;
	}

	private List<String> names(SearchResultCollection collection) {
		List<String> names = new ArrayList<>();
		for (SearchResult result : collection.getVisibleSpatialSearchResults()) {
			names.add(result.localeName);
		}
		return names;
	}
}
