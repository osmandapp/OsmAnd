package net.osmand.search;

import net.osmand.data.LatLon;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory.SearchBaseAPI;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchProgressListener;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchResultCollectionSnapshot;
import net.osmand.search.core.SearchResultProgressSnapshot;
import net.osmand.search.core.SearchResultProgressSnapshot.Stage;
import net.osmand.search.core.SearchResultSnapshot;
import net.osmand.search.core.SearchSettings;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
		SearchResult sourceResult = createResult(phrase, "Result", 0);
		collection.addSearchResults(Arrays.asList(sourceResult), false, false);

		SearchResultCollectionSnapshot snapshot = collection.toSnapshot(42);
		SearchResultCollection uiCollection = SearchResultCollection.fromSnapshot(snapshot);
		SearchResult uiResult = uiCollection.getCurrentSearchResults().get(0);

		Assert.assertEquals(42, snapshot.getRequestId());
		Assert.assertEquals(1, snapshot.getCurrentSearchResults().size());
		Assert.assertNotSame(sourceResult, uiResult);
		uiResult.localeName = "UI change";
		Assert.assertEquals("Result", sourceResult.localeName);
		Assert.assertEquals("Result", snapshot.getCurrentSearchResults().get(0).getLocaleName());
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

	@Test
	public void testSnapshotCopyPreservesSearchResultSubtype() {
		SearchPhrase phrase = createPhrase();
		TestSearchResult source = new TestSearchResult(phrase, "extra");
		source.objectType = ObjectType.LOCATION;
		source.localeName = "Result";

		SearchResult copy = SearchResultSnapshot.from(source).toSearchResult();

		Assert.assertTrue(copy instanceof TestSearchResult);
		Assert.assertEquals("extra", ((TestSearchResult) copy).getExtraData());
		Assert.assertNotSame(source, copy);
	}

	@Test
	public void testMatcherAggregatesApiProgressInCore() {
		SearchPhrase phrase = createPhrase();
		AtomicInteger requestNumber = new AtomicInteger(7);
		List<SearchResultProgressSnapshot> published = new ArrayList<>();
		SearchProgressListener progressListener = new SearchProgressListener() {
			@Override
			public void onSearchStarted(long requestId, SearchPhrase phrase) {
			}

			@Override
			public void onProgress(SearchResultProgressSnapshot progress) {
				published.add(progress);
			}

			@Override
			public void onPartialLocation(long requestId, SearchPhrase phrase) {
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		};
		SearchResultMatcher matcher = new SearchResultMatcher(null, progressListener, phrase,
				requestNumber.get(), requestNumber, -1);

		TestSearchApi firstApi = new TestSearchApi();
		SearchResult partialLocation = createResult(phrase, "Partial", 0);
		partialLocation.objectType = ObjectType.PARTIAL_LOCATION;
		matcher.publish(partialLocation);
		matcher.publish(createResult(phrase, "First", 0));
		matcher.apiSearchFinished(firstApi, phrase);
		SearchResultProgressSnapshot firstProgress = published.get(published.size() - 1);

		Assert.assertEquals(Stage.API_FINISHED, firstProgress.getStage());
		Assert.assertSame(firstApi, firstProgress.getSearchApi());
		Assert.assertFalse(firstProgress.shouldAppend());
		Assert.assertEquals(7, firstProgress.getResultCollection().getRequestId());
		Assert.assertEquals(1, firstProgress.getResultCollection().getCurrentSearchResults().size());
		Assert.assertEquals(2, matcher.getRequestResults().size());
		Assert.assertEquals("First", firstProgress.getResultCollection().getCurrentSearchResults().get(0).getLocaleName());

		TestSearchApi secondApi = new TestSearchApi();
		matcher.publish(createResult(phrase, "Second", 0));
		matcher.apiSearchFinished(secondApi, phrase);
		SearchResultProgressSnapshot secondProgress = published.get(published.size() - 1);

		Assert.assertTrue(secondProgress.shouldAppend());
		Assert.assertEquals(2, secondProgress.getResultCollection().getCurrentSearchResults().size());
	}

	@Test
	public void testProgressListenerReceivesSnapshotsWithoutMutableResults() {
		SearchPhrase phrase = createPhrase();
		AtomicInteger requestNumber = new AtomicInteger(11);
		List<Long> startedRequests = new ArrayList<>();
		List<Long> partialLocationRequests = new ArrayList<>();
		List<SearchResultProgressSnapshot> progressSnapshots = new ArrayList<>();
		SearchProgressListener progressListener = new SearchProgressListener() {
			@Override
			public void onSearchStarted(long requestId, SearchPhrase phrase) {
				startedRequests.add(requestId);
			}

			@Override
			public void onProgress(SearchResultProgressSnapshot progress) {
				progressSnapshots.add(progress);
			}

			@Override
			public void onPartialLocation(long requestId, SearchPhrase phrase) {
				partialLocationRequests.add(requestId);
			}

			@Override
			public boolean isCancelled() {
				return false;
			}
		};
		SearchResultMatcher matcher = new SearchResultMatcher(null, progressListener, phrase,
				requestNumber.get(), requestNumber, -1);

		matcher.searchStarted(phrase);
		SearchResult partialLocation = createResult(phrase, "Partial", 0);
		partialLocation.objectType = ObjectType.PARTIAL_LOCATION;
		matcher.publish(partialLocation);
		SearchResult sourceResult = createResult(phrase, "Result", 0);
		matcher.publish(sourceResult);
		matcher.apiSearchFinished(new TestSearchApi(), phrase);
		sourceResult.localeName = "Changed";

		Assert.assertEquals(Arrays.asList(11L), startedRequests);
		Assert.assertEquals(Arrays.asList(11L), partialLocationRequests);
		Assert.assertEquals(2, matcher.getRequestResults().size());
		Assert.assertEquals(1, progressSnapshots.size());
		SearchResultProgressSnapshot progress = progressSnapshots.get(0);
		Assert.assertEquals(Stage.API_FINISHED, progress.getStage());
		Assert.assertEquals(11, progress.getRequestId());
		Assert.assertEquals("Result",
				progress.getResultCollection().getCurrentSearchResults().get(0).getLocaleName());
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

	private static class TestSearchApi extends SearchBaseAPI {
		TestSearchApi() {
			super(ObjectType.LOCATION);
		}
	}

	private static class TestSearchResult extends SearchResult {
		private final String extraData;

		TestSearchResult(SearchPhrase phrase, String extraData) {
			super(phrase);
			this.extraData = extraData;
		}

		String getExtraData() {
			return extraData;
		}

		@Override
		public SearchResultFactory getSnapshotResultFactory() {
			String data = extraData;
			return phrase -> new TestSearchResult(phrase, data);
		}
	}
}
