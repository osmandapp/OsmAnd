package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.configmap.ConfigureMapFragment;

import java.util.Optional;

class Fragment2PreferenceFragmentConverter implements de.KnollFrank.lib.settingssearch.Fragment2PreferenceFragmentConverter {

	@Override
	public Optional<Class<? extends PreferenceFragmentCompat>> asPreferenceFragment(final Class<? extends Fragment> fragment) {
		return ConfigureMapFragment.class.equals(fragment) ?
				Optional.of(ConfigureMapFragment.PreferenceFragment.class) :
				Optional.empty();
	}
}
