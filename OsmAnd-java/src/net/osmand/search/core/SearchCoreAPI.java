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

}
