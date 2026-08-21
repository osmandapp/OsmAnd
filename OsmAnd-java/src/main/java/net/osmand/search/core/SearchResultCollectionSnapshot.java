package net.osmand.search.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable, request-scoped snapshot of an aggregated search result collection. */
public final class SearchResultCollectionSnapshot {

	public static final long NO_REQUEST_ID = -1;

	private final long requestId;
	private final SearchPhrase phrase;
	private final List<SearchResultSnapshot> searchResults;
	private final boolean skipSorting;
	private final int spatialSearchVisibleLevel;
	private final boolean useLimit;

	private SearchResultCollectionSnapshot(long requestId, SearchPhrase phrase,
	                                       List<SearchResultSnapshot> searchResults, boolean skipSorting,
	                                       int spatialSearchVisibleLevel, boolean useLimit) {
		this.requestId = requestId;
		this.phrase = Objects.requireNonNull(phrase);
		this.searchResults = Collections.unmodifiableList(new ArrayList<>(searchResults));
		this.skipSorting = skipSorting;
		this.spatialSearchVisibleLevel = spatialSearchVisibleLevel;
		this.useLimit = useLimit;
	}

	public static SearchResultCollectionSnapshot from(long requestId, SearchPhrase phrase,
	                                                  List<SearchResult> searchResults, boolean skipSorting,
	                                                  int spatialSearchVisibleLevel, boolean useLimit) {
		return new SearchResultCollectionSnapshot(requestId, phrase, SearchResultSnapshot.fromAll(searchResults),
				skipSorting, spatialSearchVisibleLevel, useLimit);
	}

	public long getRequestId() {
		return requestId;
	}

	public SearchPhrase getPhrase() {
		return phrase;
	}

	public List<SearchResultSnapshot> getCurrentSearchResults() {
		return searchResults;
	}

	public List<SearchResultSnapshot> getVisibleSpatialSearchResults() {
		if (!skipSorting) {
			return searchResults;
		}
		List<SearchResultSnapshot> visibleResults = new ArrayList<>();
		for (int level = 0; level <= spatialSearchVisibleLevel; level++) {
			for (SearchResultSnapshot result : searchResults) {
				if (result.getSpatialSearchVisibleLevel() == level) {
					visibleResults.add(result);
				}
			}
		}
		return Collections.unmodifiableList(visibleResults);
	}

	public boolean hasSearchResults() {
		return !searchResults.isEmpty();
	}

	public boolean isSkipSorting() {
		return skipSorting;
	}

	public int getSpatialSearchVisibleLevel() {
		return spatialSearchVisibleLevel;
	}

	public boolean getUseLimit() {
		return useLimit;
	}

	public boolean hasMoreSpatialSearchResults() {
		if (!skipSorting) {
			return false;
		}
		for (SearchResultSnapshot result : searchResults) {
			if (result.getSpatialSearchVisibleLevel() > spatialSearchVisibleLevel) {
				return true;
			}
		}
		return false;
	}

	public SearchResultCollectionSnapshot withMoreSpatialSearchResults() {
		if (!hasMoreSpatialSearchResults()) {
			return this;
		}
		return new SearchResultCollectionSnapshot(requestId, phrase, searchResults, skipSorting,
				spatialSearchVisibleLevel + 1, useLimit);
	}
}
