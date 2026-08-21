package net.osmand.search.core;

/** Receives request-scoped search lifecycle events without exposing mutable search results. */
public interface SearchProgressListener {

	void onSearchStarted(long requestId, SearchPhrase phrase);

	void onProgress(SearchResultProgressSnapshot progress);

	void onPartialLocation(long requestId, SearchPhrase phrase);

	boolean isCancelled();
}
