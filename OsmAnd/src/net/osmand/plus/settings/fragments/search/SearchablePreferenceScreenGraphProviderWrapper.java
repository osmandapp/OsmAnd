package net.osmand.plus.settings.fragments.search;

import androidx.preference.PreferenceManager;

import net.osmand.plus.R;

import de.KnollFrank.lib.settingssearch.graph.ComputeAndPersist;
import de.KnollFrank.lib.settingssearch.graph.SearchablePreferenceScreenGraphLoader;
import de.KnollFrank.lib.settingssearch.graph.SearchablePreferenceScreenGraphProvider;

class SearchablePreferenceScreenGraphProviderWrapper {

	public static SearchablePreferenceScreenGraphProvider wrapSearchablePreferenceScreenGraphProvider(
			final SearchablePreferenceScreenGraphProvider searchablePreferenceScreenGraphProvider,
			final PreferenceManager preferenceManager,
			final GraphDAOMode graphDAOMode) {
		return switch (graphDAOMode) {
			case PERSIST_GRAPH -> new ComputeAndPersist(
					searchablePreferenceScreenGraphProvider,
					preferenceManager.getContext());
			case LOAD_GRAPH -> new SearchablePreferenceScreenGraphLoader(
					R.raw.searchable_preference_screen_graph,
					preferenceManager);
		};
	}
}
