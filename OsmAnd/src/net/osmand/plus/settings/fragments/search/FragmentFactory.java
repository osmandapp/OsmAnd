package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SettingsSearchConfigurer.setConfigureSettingsSearch;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.configmap.ConfigureMapDialogs;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.configmap.MapModeFragment;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.widgets.alert.MultiSelectionDialogFragment;
import net.osmand.plus.widgets.alert.RoadStyleSelectionDialogFragment;

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
				.instantiateFragment(fragmentClass, src)
				.or(() -> instantiatePreferenceFragmentUsingPreferenceFragmentHandler(fragmentClass, src, context))
				.orElseGet(() -> createDefaultInstance(fragmentClass, src, context, instantiateAndInitializeFragment));
	}

	private static <T extends Fragment> Optional<T> instantiateFragment(final Class<T> fragmentClass, final Optional<PreferenceWithHost> src) {
		if (RoadStyleSelectionDialogFragment.class.equals(fragmentClass) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().host();
			if (srcProxy instanceof final ConfigureMapFragment.ConfigureMapFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal().getDialogs().roadStyleDialog().orElseThrow());
			} else if (srcProxy instanceof final RoadStyleSelectionDialogFragment.RoadStyleSelectionDialogFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal());
			}
		}
		if (MultiSelectionDialogFragment.class.equals(fragmentClass) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().host();
			if (srcProxy instanceof final ConfigureMapFragment.ConfigureMapFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal().getDialogs().hideDialog().orElseThrow());
			} else if (srcProxy instanceof final MultiSelectionDialogFragment.MultiSelectionDialogFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal());
			}
		}
		if (ConfigureMapDialogs.MapLanguageDialog.class.equals(fragmentClass) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().host();
			if (srcProxy instanceof final ConfigureMapFragment.ConfigureMapFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal().getDialogs().mapLanguageDialog());
			} else if (srcProxy instanceof final ConfigureMapDialogs.MapLanguageDialog.MapLanguageDialogProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal());
			}
		}
		if (MapModeFragment.class.equals(fragmentClass) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().host();
			if (srcProxy instanceof final ConfigureMapFragment.ConfigureMapFragmentProxy _srcProxy) {
				return Optional.of((T) MapModeFragment.createInstanceAndRegisterMapModeController(_srcProxy.getPrincipal().getApp()));
			} else if (srcProxy instanceof final MapModeFragment.MapModeFragmentProxy _srcProxy) {
				return Optional.of((T) MapModeFragment.createInstanceAndRegisterMapModeController(_srcProxy.getPrincipal().getApp()));
			}
		}
		return Optional.empty();
	}

	private static <T extends Fragment> Optional<T> instantiatePreferenceFragmentUsingPreferenceFragmentHandler(
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
