package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SearchableInfoProviderHelper.joinNonNullElements;

import net.osmand.plus.settings.preferences.EditTextPreferenceEx;

import java.util.Arrays;

import de.KnollFrank.lib.preferencesearch.search.provider.SearchableInfoProvider;

class EditTextPreferenceExSearchableInfoProvider implements SearchableInfoProvider<EditTextPreferenceEx> {

	@Override
	public String getSearchableInfo(final EditTextPreferenceEx preference) {
		return joinNonNullElements(
				", ",
				Arrays.asList(
						preference.getText(),
						preference.getDescription()));
	}
}
