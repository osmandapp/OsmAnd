package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SettingsSearchConfigurer.setConfigureSettingsSearch;

import android.content.Context;

import androidx.fragment.app.Fragment;

import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferenceWithHost;
import de.KnollFrank.lib.settingssearch.fragment.DefaultFragmentFactory;
import de.KnollFrank.lib.settingssearch.fragment.IFragments;

class FragmentFactory implements de.KnollFrank.lib.settingssearch.fragment.FragmentFactory {

	@Override
	public <T extends Fragment> T instantiate(final Class<T> fragmentClass,
											  final Optional<PreferenceWithHost> src,
											  final Context context,
											  final IFragments fragments) {
		final T fragment = _instantiate(fragmentClass, src, context, fragments);
		setConfigureSettingsSearch(fragment, true);
		return fragment;
	}

	private static <T extends Fragment> T _instantiate(final Class<T> fragmentClass,
													   final Optional<PreferenceWithHost> src,
													   final Context context,
													   final IFragments fragments) {
		return FragmentFactory
				.instantiate(src, context, fragmentClass)
				.orElseGet(() -> createDefaultInstance(fragmentClass, src, context, fragments));
	}

	private static <T extends Fragment> Optional<T> instantiate(final Optional<PreferenceWithHost> src,
																final Context context,
																final Class<T> classOfT) {
		return src
				.filter(preferenceWithHost -> preferenceWithHost.host() instanceof PreferenceFragmentHandlerProvider)
				.flatMap(preferenceWithHost -> ((PreferenceFragmentHandlerProvider) preferenceWithHost.host()).getPreferenceFragmentHandler(preferenceWithHost.preference()))
				.map(preferenceFragmentHandler -> preferenceFragmentHandler.createPreferenceFragment(context, Optional.empty()))
				.flatMap(
						preferenceFragment ->
								classOfT.isAssignableFrom(preferenceFragment.getClass()) ?
										Optional.of((T) preferenceFragment) :
										Optional.empty());
	}

	private static <T extends Fragment> T createDefaultInstance(final Class<T> fragmentClass,
																final Optional<PreferenceWithHost> src,
																final Context context,
																final IFragments fragments) {
		final T fragment = new DefaultFragmentFactory().instantiate(fragmentClass, src, context, fragments);
		src.ifPresent(_src -> configureFragment(fragment, _src));
		return fragment;
	}

	private static void configureFragment(final Fragment fragment, final PreferenceWithHost src) {
		if (src.host() instanceof final BaseSettingsFragment baseSettingsFragment) {
			fragment.setArguments(baseSettingsFragment.buildArguments());
		}
	}
}
