package net.osmand.plus.settings.fragments;

import androidx.preference.Preference;

import net.osmand.plus.settings.preferences.ListPreferenceEx;

import java.util.Optional;

import de.KnollFrank.lib.preferencesearch.search.SearchableInfoProvider;

class ListPreferenceExSearchableInfoProvider implements SearchableInfoProvider {

	@Override
	public Optional<String> getSearchableInfo(final Preference preference) {
		return preference instanceof ListPreferenceEx ?
				Optional.of(join(((ListPreferenceEx) preference).getEntries())) :
				Optional.empty();
	}

	private static String join(final CharSequence[] charSequences) {
		return charSequences == null ?
				"" :
				String.join(", ", charSequences);
	}
}
