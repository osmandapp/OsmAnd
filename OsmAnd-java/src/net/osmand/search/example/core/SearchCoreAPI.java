package net.osmand.search.example.core;

import java.util.List;

public interface SearchCoreAPI {
	
	public int getSearchPriority(SearchPhrase p);
	
	public interface SearchCallback {
		
		public boolean accept(SearchResult r);
	}
	
	public List<SearchResult> search(
			SearchPhrase phrase,
			int radiusLevel,
			SearchCallback callback,
			List<SearchResult> existingSearchResults);

}
