package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import java.util.Collection;
import java.util.function.Predicate;

class SearchResultsFilterFactory {

	public static EnabledProfilesSearchResultsFilter createSearchResultsFilter(final Collection<String> allProfiles,
																			   final OsmandPreference<String> enabledProfiles) {
		return new EnabledProfilesSearchResultsFilter(
				isDisabledProfile(allProfiles, enabledProfiles),
				new ActivePluginsSearchResultsFilter());
	}

	private static Predicate<String> isDisabledProfile(final Collection<String> allProfiles,
													   final OsmandPreference<String> enabledProfiles) {
		return new Predicate<>() {

			@Override
			public boolean test(final String key) {
				return isDisabledProfile(key);
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
		};
	}
}
