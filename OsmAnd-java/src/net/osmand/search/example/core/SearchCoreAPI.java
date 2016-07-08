package net.osmand.search.example.core;

import java.io.IOException;

import net.osmand.search.example.SearchUICore.SearchResultMatcher;

public interface SearchCoreAPI {

	public int getSearchPriority(SearchPhrase p);

	public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException;

}
