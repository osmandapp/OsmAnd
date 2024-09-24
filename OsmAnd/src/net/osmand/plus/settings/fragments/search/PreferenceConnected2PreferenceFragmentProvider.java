package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.keyevent.fragments.MainExternalInputDevicesFragment;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.Optional;

class PreferenceConnected2PreferenceFragmentProvider implements de.KnollFrank.lib.settingssearch.provider.PreferenceConnected2PreferenceFragmentProvider {

	private final OsmandSettings settings;

	public PreferenceConnected2PreferenceFragmentProvider(final OsmandSettings settings) {
		this.settings = settings;
	}

	@Override
	public Optional<Class<? extends PreferenceFragmentCompat>> getConnectedPreferenceFragment(final Preference preference) {
		return settings.EXTERNAL_INPUT_DEVICE.getId().equals(preference.getKey()) ?
				// FK-FIXME: MainExternalInputDevicesFragment muß in der FragmentFactory (genauer: FragmentFactoryAndPrepareShow) korrekt instanziiert werden und zwar genau wie in GeneralProfileSettingsFragment.onPreferenceClick() { ... showInstance() ... }
				Optional.of(MainExternalInputDevicesFragment.class) :
				Optional.empty();
	}
}
