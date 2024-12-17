package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.provider.PreferenceDialogAndSearchableInfoByPreferenceDialogProvider;

// FK-TODO: remove this interface, use ShowableSearchablePreferenceDialogProvider instead
public interface SearchablePreferenceDialogProvider {

	Optional<PreferenceDialogAndSearchableInfoByPreferenceDialogProvider<?>> getPreferenceDialogAndSearchableInfoByPreferenceDialogProvider(final Preference preference);
}
