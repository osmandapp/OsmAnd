package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Optional;

class PreferenceFragmentConnected2PreferenceProvider implements de.KnollFrank.lib.settingssearch.provider.PreferenceFragmentConnected2PreferenceProvider {

	@Override
	public Optional<Class<? extends PreferenceFragmentCompat>> getPreferenceFragmentConnected2Preference(Preference preference, final PreferenceFragmentCompat hostOfPreference) {
		return hostOfPreference instanceof final PreferenceFragmentHandlerProvider preferenceFragmentHandlerProvider ?
				preferenceFragmentHandlerProvider
						.getPreferenceFragmentHandler(preference)
						.map(PreferenceFragmentHandler::getClassOfPreferenceFragment) :
				Optional.empty();
	}
}
