package net.osmand.plus.settings.fragments.search;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import de.KnollFrank.lib.settingssearch.client.SearchConfig;

class SearchConfigFactory {

	public static SearchConfig createSearchConfig(final FragmentActivity fragmentActivity,
												  final @IdRes int fragmentContainerViewId,
												  final OsmandPreference<String> availableAppModes) {
		final SearchResultsFilter searchResultsFilter =
				SearchResultsFilterFactory.createSearchResultsFilter(
						PreferencePathDisplayerFactory.getApplicationModeKeys(),
						availableAppModes);
		return SearchConfig
				.builder(fragmentContainerViewId, fragmentActivity)
				.withQueryHint("Search Settings")
				.withSearchResultsFilter(searchResultsFilter)
				.withPreferencePathDisplayer(PreferencePathDisplayerFactory.createPreferencePathDisplayer(fragmentActivity))
				.withSearchPreferenceFragmentUI(new SearchPreferenceFragmentUI(searchResultsFilter))
				.withSearchResultsFragmentUI(new SearchResultsFragmentUI())
				.withPrepareShow(new PrepareShow())
				.withShowSettingsFragmentAndHighlightSetting(new ShowSettingsFragmentAndHighlightSetting(fragmentContainerViewId))
				.build();
	}
}
