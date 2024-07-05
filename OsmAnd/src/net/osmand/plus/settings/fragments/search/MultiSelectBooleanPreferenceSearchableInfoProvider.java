package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.ListPreferenceExSearchableInfoProvider.getSearchableInfos;
import static net.osmand.plus.settings.fragments.search.SearchableInfoProviderHelper.join;

import net.osmand.plus.settings.preferences.MultiSelectBooleanPreference;

import de.KnollFrank.lib.preferencesearch.search.provider.SearchableInfoProvider;

class MultiSelectBooleanPreferenceSearchableInfoProvider implements SearchableInfoProvider<MultiSelectBooleanPreference> {

	@Override
	public String getSearchableInfo(final MultiSelectBooleanPreference preference) {
		return join(
				", ",
				getSearchableInfos(
						preference.getEntries(),
						preference.getDescription()));
	}
}
