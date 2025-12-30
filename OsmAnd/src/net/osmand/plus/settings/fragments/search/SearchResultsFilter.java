package net.osmand.plus.settings.fragments.search;

import java.util.function.Predicate;

import de.KnollFrank.lib.settingssearch.PreferencePath;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceOfHostWithinGraph;

class SearchResultsFilter implements de.KnollFrank.lib.settingssearch.results.SearchResultsFilter {

	private final Predicate<String> isDisabledProfile;
	private boolean removeSearchResultsConnectedToDisabledProfiles = true;

	SearchResultsFilter(final Predicate<String> isDisabledProfile) {
		this.isDisabledProfile = isDisabledProfile;
	}

	@Override
	public boolean includePreferenceInSearchResults(final SearchablePreferenceOfHostWithinGraph preference) {
		return new IncludePreferenceInSearchResultsPredicate().includePreferenceInSearchResults(preference) &&
				(removeSearchResultsConnectedToDisabledProfiles ?
						!isConnectedToDisabledProfile(preference) :
						true);
	}

	public boolean shallRemoveSearchResultsConnectedToDisabledProfiles() {
		return removeSearchResultsConnectedToDisabledProfiles;
	}

	public void setRemoveSearchResultsConnectedToDisabledProfiles(final boolean removeSearchResultsConnectedToDisabledProfiles) {
		this.removeSearchResultsConnectedToDisabledProfiles = removeSearchResultsConnectedToDisabledProfiles;
	}

	private boolean isConnectedToDisabledProfile(final SearchablePreferenceOfHostWithinGraph searchablePreference) {
		return startsWithDisabledProfile(searchablePreference.getPreferencePath());
	}

	private boolean startsWithDisabledProfile(final PreferencePath preferencePath) {
		final SearchablePreferenceOfHostWithinGraph startOfPreferencePath = preferencePath.preferences().get(0);
		return isDisabledProfile.test(startOfPreferencePath.searchablePreference().getKey());
	}
}
