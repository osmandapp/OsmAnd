package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoByPreferenceDialogProvider;

class PreferenceDialogAndSearchableInfoProvider implements de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoProvider {

	@Override
	public Optional<PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<?>> getPreferenceDialogAndSearchableInfoByPreferenceDialogProvider(
			final Preference preference,
			final PreferenceFragmentCompat hostOfPreference) {
		// FK-TODO: handle more preference dialogs, which shall be searchable
		if (hostOfPreference instanceof final SearchablePreferenceDialogProvider searchablePreferenceDialogProvider) {
			return searchablePreferenceDialogProvider.getPreferenceDialogAndSearchableInfoByPreferenceDialogProvider(preference);
		}
		if (hostOfPreference instanceof final ShowableSearchablePreferenceDialogProvider showableSearchablePreferenceDialogProvider) {
			return showableSearchablePreferenceDialogProvider
					.getShowableSearchablePreferenceDialog(preference, null)
					.map(ShowableSearchablePreferenceDialog::asPreferenceDialogAndSearchableInfoByPreferenceDialogProvider);
		}
		return Optional.empty();
	}
}
