package net.osmand.search.example.core;

import java.io.IOException;

import net.osmand.search.example.SearchUICore.SearchResultMatcher;

public interface SearchCoreAPI {

	/**
	 * @param p
	 * @return order in which search core apis should be called, -1 means do not call
	 */
	public int getSearchPriority(SearchPhrase p);

	public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException;

}
