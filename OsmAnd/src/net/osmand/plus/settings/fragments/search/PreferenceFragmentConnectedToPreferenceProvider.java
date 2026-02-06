package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Optional;

// FK-TODO: replace "2" with "To" in every name within package net.osmand.plus.settings.fragments.search
class PreferenceFragmentConnectedToPreferenceProvider implements de.KnollFrank.lib.settingssearch.provider.PreferenceFragmentConnectedToPreferenceProvider {

	@Override
	public Optional<Class<? extends PreferenceFragmentCompat>> getPreferenceFragmentConnectedToPreference(
			final Preference preference,
			final PreferenceFragmentCompat hostOfPreference) {
		return hostOfPreference instanceof final PreferenceFragmentHandlerProvider preferenceFragmentHandlerProvider ?
				preferenceFragmentHandlerProvider
						.getPreferenceFragmentHandler(preference)
						.map(PreferenceFragmentHandler::getClassOfPreferenceFragment) :
				Optional.empty();
	}
}
