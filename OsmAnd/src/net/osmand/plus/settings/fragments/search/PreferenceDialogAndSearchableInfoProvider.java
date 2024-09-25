package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoByPreferenceDialogProvider;

class PreferenceDialogAndSearchableInfoProvider implements de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoProvider {

	@Override
	public Optional<PreferenceDialogAndSearchableInfoByPreferenceDialogProvider> getPreferenceDialogAndSearchableInfoByPreferenceDialogProvider(
			final PreferenceFragmentCompat hostOfPreference,
			final Preference preference) {
		return hostOfPreference instanceof final SearchablePreferenceDialogProvider searchablePreferenceDialogProvider ?
				searchablePreferenceDialogProvider.getPreferenceDialogAndSearchableInfoByPreferenceDialogProvider(preference) :
				Optional.empty();
	}
}
