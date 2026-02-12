package net.osmand.search.core;

import java.io.IOException;
import java.util.Collection;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.search.SearchUICore.SearchResultMatcher;

public interface SearchCoreAPI {

	public interface SearchCoreAPIUnit {
		
		public int getSearchPriority(SearchPhrase p);

		public BinaryMapIndexReader getRegion();
		
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException;

	}
	
	public Collection<SearchCoreAPIUnit> getSearchUnits(SearchPhrase p) throws IOException;

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
