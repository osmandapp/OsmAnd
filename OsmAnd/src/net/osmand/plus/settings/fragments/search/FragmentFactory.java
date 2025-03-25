package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SettingsSearchConfigurer.setConfigureSettingsSearch;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.configmap.ConfigureMapDialogs;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.widgets.alert.CustomAlert;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferenceWithHost;
import de.KnollFrank.lib.settingssearch.fragment.DefaultFragmentFactory;
import de.KnollFrank.lib.settingssearch.fragment.InstantiateAndInitializeFragment;

class FragmentFactory implements de.KnollFrank.lib.settingssearch.fragment.FragmentFactory {

	@Override
	public <T extends Fragment> T instantiate(final Class<T> fragmentClass,
											  final Optional<PreferenceWithHost> src,
											  final Context context,
											  final InstantiateAndInitializeFragment instantiateAndInitializeFragment) {
		final T fragment = _instantiate(fragmentClass, src, context, instantiateAndInitializeFragment);
		setConfigureSettingsSearch(fragment, true);
		return fragment;
	}

	private static <T extends Fragment> T _instantiate(final Class<T> fragmentClass,
													   final Optional<PreferenceWithHost> src,
													   final Context context,
													   final InstantiateAndInitializeFragment instantiateAndInitializeFragment) {
		return FragmentFactory
				.getExistingInstance(fragmentClass, src)
				.or(() -> instantiateFromPreferenceFragmentHandlerProvider(fragmentClass, src, context))
				.orElseGet(() -> createDefaultInstance(fragmentClass, src, context, instantiateAndInitializeFragment));
	}

	private static <T extends Fragment> Optional<T> getExistingInstance(final Class<T> fragmentClass, final Optional<PreferenceWithHost> src) {
		// FK-TODO: folgendes ist absolut unklar programmiert!!!
		if (CustomAlert.SingleSelectionDialogFragment.class.equals(fragmentClass) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().host();
			if (srcProxy instanceof final ConfigureMapFragment.PreferenceFragment _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal().getRoadStyleDialog().orElseThrow());
			} else if (srcProxy instanceof final CustomAlert.SingleSelectionDialogFragment.PreferenceFragment _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal());
			}
		}
		if (CustomAlert.MultiSelectionDialogFragment.class.equals(fragmentClass) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().host();
			if (srcProxy instanceof final ConfigureMapFragment.PreferenceFragment _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal().getHideDialog().orElseThrow());
			}
		}
		if (ConfigureMapDialogs.MapLanguageDialog.class.equals(fragmentClass) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().host();
			if (srcProxy instanceof final ConfigureMapFragment.PreferenceFragment _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal().getMapLanguageDialog());
			} else if (srcProxy instanceof final ConfigureMapDialogs.MapLanguageDialog.PreferenceFragment _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal());
			}
		}
		return Optional.empty();
	}

	private static <T extends Fragment> Optional<T> instantiateFromPreferenceFragmentHandlerProvider(
			final Class<T> fragmentClass,
			final Optional<PreferenceWithHost> src,
			final Context context) {
		return src
				.filter(preferenceWithHost -> preferenceWithHost.host() instanceof PreferenceFragmentHandlerProvider)
				.flatMap(preferenceWithHost -> ((PreferenceFragmentHandlerProvider) preferenceWithHost.host()).getPreferenceFragmentHandler(preferenceWithHost.preference()))
				.map(preferenceFragmentHandler -> preferenceFragmentHandler.createPreferenceFragment(context, Optional.empty()))
				.flatMap(
						preferenceFragment ->
								fragmentClass.isAssignableFrom(preferenceFragment.getClass()) ?
										Optional.of((T) preferenceFragment) :
										Optional.empty());
	}

	private static <T extends Fragment> T createDefaultInstance(final Class<T> fragmentClass,
																final Optional<PreferenceWithHost> src,
																final Context context,
																final InstantiateAndInitializeFragment instantiateAndInitializeFragment) {
		final T fragment = new DefaultFragmentFactory().instantiate(fragmentClass, src, context, instantiateAndInitializeFragment);
		src.ifPresent(_src -> configureFragment(fragment, _src));
		return fragment;
	}

	private static void configureFragment(final Fragment fragment, final PreferenceWithHost src) {
		if (src.host() instanceof final BaseSettingsFragment baseSettingsFragment) {
			fragment.setArguments(baseSettingsFragment.buildArguments());
		}
	}
}
