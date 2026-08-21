package net.osmand.search.core;

import java.util.Objects;

import net.osmand.binary.BinaryMapIndexReader;

/** Immutable result state produced by the search worker at a progress boundary. */
public final class SearchResultProgressSnapshot {

	public enum Stage {
		FILTER_FINISHED,
		API_FINISHED,
		REGION_FINISHED,
		SEARCH_FINISHED
	}

	private final Stage stage;
	private final SearchCoreAPI searchApi;
	private final BinaryMapIndexReader region;
	private final SearchResultCollectionSnapshot resultCollection;
	private final boolean append;
	private final boolean impreciseResults;
	private final boolean regionResultsPublished;

	public SearchResultProgressSnapshot(Stage stage, SearchCoreAPI searchApi, BinaryMapIndexReader region,
	                                    SearchResultCollectionSnapshot resultCollection, boolean append,
	                                    boolean impreciseResults, boolean regionResultsPublished) {
		this.stage = Objects.requireNonNull(stage);
		this.searchApi = searchApi;
		this.region = region;
		this.resultCollection = Objects.requireNonNull(resultCollection);
		this.append = append;
		this.impreciseResults = impreciseResults;
		this.regionResultsPublished = regionResultsPublished;
	}

	public Stage getStage() {
		return stage;
	}

	public long getRequestId() {
		return resultCollection.getRequestId();
	}

	public SearchCoreAPI getSearchApi() {
		return searchApi;
	}

	public BinaryMapIndexReader getRegion() {
		return region;
	}

	public SearchResultCollectionSnapshot getResultCollection() {
		return resultCollection;
	}

	public boolean shouldAppend() {
		return append;
	}

	public boolean hasImpreciseResults() {
		return impreciseResults;
	}

	public boolean hasPublishedRegionResults() {
		return regionResultsPublished;
	}
}
