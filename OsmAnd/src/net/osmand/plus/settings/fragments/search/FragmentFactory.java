package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SettingsSearchConfigurer.setConfigureSettingsSearch;

import android.content.Context;

import androidx.fragment.app.Fragment;

import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferenceWithHost;
import de.KnollFrank.lib.settingssearch.fragment.DefaultFragmentFactory;
import de.KnollFrank.lib.settingssearch.fragment.Fragments;

class FragmentFactory implements de.KnollFrank.lib.settingssearch.fragment.FragmentFactory {

	@Override
	public Fragment instantiate(final String fragmentClassName,
								final Optional<PreferenceWithHost> src,
								final Context context,
								final Fragments fragments) {
		final Fragment fragment = _instantiate(fragmentClassName, src, context, fragments);
		if (ConfigureMapFragment.PreferenceFragment.class.getName().equals(fragmentClassName)) {
			final ConfigureMapFragment.PreferenceFragment preferenceFragment = (ConfigureMapFragment.PreferenceFragment) fragment;
			preferenceFragment.setFragments(fragments);
			return preferenceFragment;
		}
		setConfigureSettingsSearch(fragment, true);
		return fragment;
	}

	private static Fragment _instantiate(final String fragmentClassName,
										 final Optional<PreferenceWithHost> src,
										 final Context context,
										 final Fragments fragments) {
		return FragmentFactory
				.instantiate(src, context)
				.orElseGet(() -> createDefaultInstance(fragmentClassName, src, context, fragments));
	}

	private static Optional<Fragment> instantiate(final Optional<PreferenceWithHost> src, final Context context) {
		return src
				.filter(preferenceWithHost -> preferenceWithHost.host() instanceof PreferenceFragmentHandlerProvider)
				.flatMap(preferenceWithHost -> ((PreferenceFragmentHandlerProvider) preferenceWithHost.host()).getPreferenceFragmentHandler(preferenceWithHost.preference()))
				.map(preferenceFragmentHandler -> preferenceFragmentHandler.createPreferenceFragment(context, Optional.empty()));
	}

	private static Fragment createDefaultInstance(final String fragmentClassName,
												  final Optional<PreferenceWithHost> src,
												  final Context context,
												  final Fragments fragments) {
		final Fragment fragment = new DefaultFragmentFactory().instantiate(fragmentClassName, src, context, fragments);
		src.ifPresent(_src -> configureFragment(fragment, _src));
		return fragment;
	}

	private static void configureFragment(final Fragment fragment, final PreferenceWithHost src) {
		if (src.host() instanceof final BaseSettingsFragment baseSettingsFragment) {
			fragment.setArguments(baseSettingsFragment.buildArguments());
		}
	}
}
