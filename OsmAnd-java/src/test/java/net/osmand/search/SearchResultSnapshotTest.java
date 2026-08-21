package net.osmand.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchResultCollectionSnapshot;
import net.osmand.search.core.SearchResultSnapshot;
import net.osmand.search.core.SearchSettings;

import org.junit.Assert;
import org.junit.Test;

public class SearchResultSnapshotTest {

	@Test
	public void testResultSnapshotDoesNotReflectMutableSearchMetadataChanges() {
		SearchPhrase phrase = createPhrase();
		SearchResult parent = createResult(phrase, "Parent", 0);
		SearchResult result = createResult(phrase, "Original", 0);
		result.parentSearchResult = parent;
		List<String> otherNames = new ArrayList<>(Arrays.asList("First", "Second"));
		result.otherNames = otherNames;

		SearchResultSnapshot snapshot = SearchResultSnapshot.from(result);
		result.localeName = "Changed";
		parent.localeName = "Changed parent";
		otherNames.add("Third");

		Assert.assertEquals("Original", snapshot.getLocaleName());
		Assert.assertEquals("Parent", snapshot.getParentSearchResult().getLocaleName());
		Assert.assertEquals(Arrays.asList("First", "Second"), snapshot.getOtherNames());
		try {
			snapshot.getOtherNames().add("Third");
			Assert.fail("Snapshot metadata must be immutable");
		} catch (UnsupportedOperationException expected) {
			// Expected.
		}
	}

	@Test
	public void testCollectionSnapshotIsImmutableAndKeepsRequestId() {
		SearchPhrase phrase = createPhrase();
		SearchResultCollection collection = new SearchResultCollection(phrase);
		collection.addSearchResults(Arrays.asList(createResult(phrase, "Result", 0)), false, false);

		SearchResultCollectionSnapshot snapshot = collection.toSnapshot(42);

		Assert.assertEquals(42, snapshot.getRequestId());
		Assert.assertEquals(1, snapshot.getCurrentSearchResults().size());
		try {
			snapshot.getCurrentSearchResults().clear();
			Assert.fail("Snapshot results must be immutable");
		} catch (UnsupportedOperationException expected) {
			// Expected.
		}
	}

	@Test
	public void testSpatialVisibilityCreatesANewSnapshot() {
		SearchPhrase phrase = createPhrase();
		SearchResult first = createResult(phrase, "First", 0);
		SearchResult second = createResult(phrase, "Second", 1);
		SearchResultCollection collection = new SearchResultCollection(phrase, true);
		collection.addSearchResults(Arrays.asList(first, second), false, false);

		SearchResultCollectionSnapshot initial = collection.toSnapshot();
		SearchResultCollectionSnapshot expanded = initial.withMoreSpatialSearchResults();

		Assert.assertNotSame(initial, expanded);
		Assert.assertEquals(1, initial.getVisibleSpatialSearchResults().size());
		Assert.assertEquals(2, expanded.getVisibleSpatialSearchResults().size());
		Assert.assertEquals(0, initial.getSpatialSearchVisibleLevel());
		Assert.assertEquals(1, expanded.getSpatialSearchVisibleLevel());
	}

	private SearchPhrase createPhrase() {
		SearchSettings settings = new SearchSettings((SearchSettings) null);
		settings = settings.setOriginalLocation(new LatLon(0, 0));
		return SearchPhrase.emptyPhrase(settings);
	}

	private SearchResult createResult(SearchPhrase phrase, String name, int visibleLevel) {
		SearchResult result = new SearchResult(phrase);
		result.objectType = ObjectType.LOCATION;
		result.localeName = name;
		result.location = new LatLon(0, 0);
		result.spatialSearchVisibleLevel = visibleLevel;
		return result;
	}
}
