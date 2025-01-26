package net.osmand.plus.settings.fragments.search;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.PreferencePath;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;

class SearchResultsFilter implements de.KnollFrank.lib.settingssearch.results.SearchResultsFilter {

	private final Predicate<String> isDisabledProfile;
	private boolean removeSearchResultsConnectedToDisabledProfiles = true;

	SearchResultsFilter(final Predicate<String> isDisabledProfile) {
		this.isDisabledProfile = isDisabledProfile;
	}

	@Override
	public boolean includePreferenceInSearchResults(final SearchablePreference preference) {
		return removeSearchResultsConnectedToDisabledProfiles ?
				!isConnectedToDisabledProfile(preference) :
				true;
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
		final var startOfPreferencePath = preferencePath.preferences().get(0);
		return isDisabledProfile.test(startOfPreferencePath.getKey().orElseThrow());
	}
}
