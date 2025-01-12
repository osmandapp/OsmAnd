package net.osmand.plus.settings.fragments.search;

import java.util.Collection;
import java.util.Collections;

import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;

class SearchResultsFilter implements de.KnollFrank.lib.settingssearch.results.SearchResultsFilter {

	private boolean ignoreSearchResults = false;

	@Override
	public Collection<SearchablePreference> filter(final Collection<SearchablePreference> searchResults) {
		return ignoreSearchResults ? Collections.emptyList() : searchResults;
	}

	public boolean isIgnoreSearchResults() {
		return ignoreSearchResults;
	}

	public void setIgnoreSearchResults(final boolean ignoreSearchResults) {
		this.ignoreSearchResults = ignoreSearchResults;
	}
}
