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
		// FK-TODO: handle more preference dialogs which shall be searchable
		return hostOfPreference instanceof final ShowableSearchablePreferenceDialogProvider showableSearchablePreferenceDialogProvider ?
				showableSearchablePreferenceDialogProvider
						.getShowableSearchablePreferenceDialog(preference, Optional.empty())
						.map(ShowableSearchablePreferenceDialog::asPreferenceDialogAndSearchableInfoByPreferenceDialogProvider) :
				Optional.empty();
	}
}
