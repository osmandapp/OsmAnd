package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Optional;

class PreferenceConnected2PreferenceFragmentProvider implements de.KnollFrank.lib.settingssearch.provider.PreferenceConnected2PreferenceFragmentProvider {

	@Override
	public Optional<String> getClassNameOfConnectedPreferenceFragment(final Preference preference, final PreferenceFragmentCompat hostOfPreference) {
		return hostOfPreference instanceof final InfoProvider infoProvider ?
				infoProvider
						.getInfo(preference)
						.map(Info::getClassNameOfPreferenceFragment) :
				Optional.empty();
	}
}
