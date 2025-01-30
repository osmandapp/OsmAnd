package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SettingsSearchConfigurer.setConfigureSettingsSearch;

import android.content.Context;

import androidx.fragment.app.Fragment;

import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferenceWithHost;
import de.KnollFrank.lib.settingssearch.fragment.DefaultFragmentFactory;

class FragmentFactory implements de.KnollFrank.lib.settingssearch.fragment.FragmentFactory {

	@Override
	public Fragment instantiate(final String fragmentClassName,
								final Optional<PreferenceWithHost> src,
								final Context context) {
		final Fragment fragment = _instantiate(fragmentClassName, src, context);
		setConfigureSettingsSearch(fragment, true);
		return fragment;
	}

	private static Fragment _instantiate(final String fragmentClassName,
										 final Optional<PreferenceWithHost> src,
										 final Context context) {
		return FragmentFactory
				.instantiate(src, context)
				.orElseGet(() -> createDefaultInstance(fragmentClassName, src, context));
	}

	private static Optional<Fragment> instantiate(final Optional<PreferenceWithHost> src, final Context context) {
		return src
				.filter(preferenceWithHost -> preferenceWithHost.host() instanceof PreferenceFragmentHandlerProvider)
				.flatMap(preferenceWithHost -> ((PreferenceFragmentHandlerProvider) preferenceWithHost.host()).getPreferenceFragmentHandler(preferenceWithHost.preference()))
				.map(preferenceFragmentHandler -> preferenceFragmentHandler.createPreferenceFragment(context, Optional.empty()));
	}

	private static Fragment createDefaultInstance(final String fragmentClassName,
												  final Optional<PreferenceWithHost> src,
												  final Context context) {
		final Fragment fragment = new DefaultFragmentFactory().instantiate(fragmentClassName, src, context);
		src.ifPresent(_src -> configureFragment(fragment, _src));
		return fragment;
	}

	private static void configureFragment(final Fragment fragment, final PreferenceWithHost src) {
		if (src.host() instanceof final BaseSettingsFragment baseSettingsFragment) {
			fragment.setArguments(baseSettingsFragment.buildArguments());
		}
	}
}
