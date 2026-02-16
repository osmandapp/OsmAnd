package net.osmand.plus.settings.fragments.search;

import java.util.Locale;
import java.util.function.Predicate;

import de.KnollFrank.lib.settingssearch.PreferencePath;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceOfHostWithinTree;
import de.KnollFrank.lib.settingssearch.results.SearchResultsFilter;

class EnabledProfilesSearchResultsFilter implements SearchResultsFilter {

	private final Predicate<String> isDisabledProfile;
	private final SearchResultsFilter delegate;
	private boolean removeSearchResultsConnectedToDisabledProfiles = true;

	public EnabledProfilesSearchResultsFilter(final Predicate<String> isDisabledProfile,
											  final SearchResultsFilter delegate) {
		this.isDisabledProfile = isDisabledProfile;
		this.delegate = delegate;
	}

	@Override
	public boolean includePreferenceInSearchResults(final SearchablePreferenceOfHostWithinTree preference, final Locale locale) {
		return delegate.includePreferenceInSearchResults(preference, locale) &&
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

	private boolean isConnectedToDisabledProfile(final SearchablePreferenceOfHostWithinTree searchablePreference) {
		return startsWithDisabledProfile(searchablePreference.getPreferencePath());
	}

	private boolean startsWithDisabledProfile(final PreferencePath preferencePath) {
		return isDisabledProfile.test(preferencePath.getStart().searchablePreference().getKey());
	}
}
