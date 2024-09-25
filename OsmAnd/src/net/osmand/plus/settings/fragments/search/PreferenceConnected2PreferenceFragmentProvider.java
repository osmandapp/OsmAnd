package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Optional;

class PreferenceConnected2PreferenceFragmentProvider implements de.KnollFrank.lib.settingssearch.provider.PreferenceConnected2PreferenceFragmentProvider {

	@Override
	public Optional<String> getClassNameOfConnectedPreferenceFragment(final Preference preference, final PreferenceFragmentCompat hostOfPreference) {
		// FK-FIXME: MainExternalInputDevicesFragment muß in der FragmentFactory (genauer: FragmentFactoryAndPrepareShow) korrekt instanziiert werden und zwar genau wie in GeneralProfileSettingsFragment.onPreferenceClick() { ... showInstance() ... }
		return hostOfPreference instanceof final InfoProvider infoProvider ?
				infoProvider
						.getInfo(preference)
						.map(Info::getClassNameOfPreferenceFragment) :
				Optional.empty();
	}
}
