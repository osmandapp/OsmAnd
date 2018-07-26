package net.osmand.search.core;

import java.io.IOException;

import net.osmand.search.SearchUICore.SearchResultMatcher;

public interface SearchCoreAPI {

	/**
	 * @param p
	 * @return order in which search core apis should be called, -1 means do not call
	 */
	public int getSearchPriority(SearchPhrase p);

	public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException;

	/**
	 * @param phrase
	 * @return true if search more available (should be consistent with -1 search priority)
	 */
	public boolean isSearchMoreAvailable(SearchPhrase phrase);

	boolean isSearchAvailable(SearchPhrase p);

	/**
	 * @param phrase
	 * @return minimal search radius in meters
	 */
	int getMinimalSearchRadius(SearchPhrase phrase);

	/**
	 * @param phrase
	 * @return next search radius in meters
	 */
	int getNextSearchRadius(SearchPhrase phrase);
}
