package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.PreferencePath;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;

class SearchResultsFilter implements de.KnollFrank.lib.settingssearch.results.SearchResultsFilter {

	private final Set<String> allProfiles;
	private final OsmandPreference<String> enabledProfiles;
	private boolean removeSearchResultsConnectedToDisabledProfiles = true;

	public SearchResultsFilter(final Set<String> allProfiles, final OsmandPreference<String> enabledProfiles) {
		this.allProfiles = allProfiles;
		this.enabledProfiles = enabledProfiles;
	}

	@Override
	public Collection<SearchablePreference> filter(final Collection<SearchablePreference> searchResults) {
		return removeSearchResultsConnectedToDisabledProfiles ?
				removeSearchResultsConnectedToDisabledProfiles(searchResults) :
				searchResults;
	}

	public boolean shallRemoveSearchResultsConnectedToDisabledProfiles() {
		return removeSearchResultsConnectedToDisabledProfiles;
	}

	public void setRemoveSearchResultsConnectedToDisabledProfiles(final boolean removeSearchResultsConnectedToDisabledProfiles) {
		this.removeSearchResultsConnectedToDisabledProfiles = removeSearchResultsConnectedToDisabledProfiles;
	}

	private Collection<SearchablePreference> removeSearchResultsConnectedToDisabledProfiles(final Collection<SearchablePreference> searchResults) {
		return searchResults
				.stream()
				.filter(searchResult -> !isConnectedToDisabledProfile(searchResult))
				.collect(Collectors.toList());
	}

	private boolean isConnectedToDisabledProfile(final SearchablePreference searchablePreference) {
		return startsWithDisabledProfile(searchablePreference.getPreferencePath());
	}

	private boolean startsWithDisabledProfile(final PreferencePath preferencePath) {
		return isDisabledProfile(preferencePath.preferences().get(0).getKey().orElseThrow());
	}

	private boolean isDisabledProfile(final String key) {
		return isProfile(key) && isDisabled(key);
	}

	private boolean isProfile(final String key) {
		return allProfiles.contains(key);
	}

	private boolean isDisabled(final String profile) {
		return !enabledProfiles.get().contains(profile);
	}
}
