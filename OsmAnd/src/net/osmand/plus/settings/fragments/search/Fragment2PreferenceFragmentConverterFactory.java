package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.configmap.ConfigureMapFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.Fragment2PreferenceFragmentConverter;
import de.KnollFrank.lib.settingssearch.fragment.IFragments;

class Fragment2PreferenceFragmentConverterFactory implements de.KnollFrank.lib.settingssearch.Fragment2PreferenceFragmentConverterFactory {

	@Override
	public Fragment2PreferenceFragmentConverter createFragment2PreferenceFragmentConverter(final IFragments fragments) {
		return new Fragment2PreferenceFragmentConverter() {

			@Override
			public Optional<PreferenceFragmentCompat> asPreferenceFragment(final Fragment fragment) {
				return fragment instanceof final ConfigureMapFragment configureMapFragment ?
						Optional.of(
								(PreferenceFragmentCompat) fragments.instantiateAndInitializeFragment(
										configureMapFragment.asPreferenceFragment().getClass(),
										Optional.empty())) :
						Optional.empty();
			}
		};
	}
}
