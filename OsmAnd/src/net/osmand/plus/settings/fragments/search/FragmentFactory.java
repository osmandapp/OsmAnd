package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SettingsSearchConfigurer.setConfigureSettingsSearch;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.configmap.ConfigureMapDialogs;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.configmap.MapModeFragment;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.widgets.alert.InstallMapLayersDialogFragment;
import net.osmand.plus.widgets.alert.MapLayerSelectionDialogFragment;
import net.osmand.plus.widgets.alert.MultiSelectionDialogFragment;
import net.osmand.plus.widgets.alert.RoadStyleSelectionDialogFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.FragmentClassOfActivity;
import de.KnollFrank.lib.settingssearch.PreferenceOfHostOfActivity;
import de.KnollFrank.lib.settingssearch.fragment.DefaultFragmentFactory;
import de.KnollFrank.lib.settingssearch.fragment.InstantiateAndInitializeFragment;

class FragmentFactory implements de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.FragmentFactory {

	@Override
	public <T extends Fragment> T instantiate(
			final FragmentClassOfActivity<T> fragmentClass,
			final Optional<PreferenceOfHostOfActivity> src,
			final Context context,
			final InstantiateAndInitializeFragment instantiateAndInitializeFragment) {
		final T fragment = _instantiate(fragmentClass, src, context, instantiateAndInitializeFragment);
		setConfigureSettingsSearch(fragment, true);
		return fragment;
	}

	private static <T extends Fragment> T _instantiate(final FragmentClassOfActivity<T> fragmentClass,
													   final Optional<PreferenceOfHostOfActivity> src,
													   final Context context,
													   final InstantiateAndInitializeFragment instantiateAndInitializeFragment) {
		return FragmentFactory
				.instantiateFragment(fragmentClass, src)
				.or(() -> instantiatePreferenceFragmentUsingPreferenceFragmentHandler(fragmentClass, src, context))
				.orElseGet(() -> createDefaultInstance(fragmentClass, src, context, instantiateAndInitializeFragment));
	}

	private static <T extends Fragment> Optional<T> instantiateFragment(
			final FragmentClassOfActivity<T> fragmentClass,
			final Optional<PreferenceOfHostOfActivity> src) {
		if (InstallMapLayersDialogFragment.class.equals(fragmentClass.fragment()) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().hostOfPreference();
			if (srcProxy instanceof final MapLayerSelectionDialogFragment.MapLayerSelectionDialogFragmentProxy _srcProxy) {
				return (Optional<T>) _srcProxy.getPrincipal().installMapLayersDialogFragment;
			} else if (srcProxy instanceof final InstallMapLayersDialogFragment.InstallMapLayersDialogFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal());
			}
		}
		if (MapLayerSelectionDialogFragment.class.equals(fragmentClass.fragment()) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().hostOfPreference();
			if (srcProxy instanceof final ConfigureMapFragment.ConfigureMapFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal().getDialogs().mapLayerDialog().orElseThrow());
			} else if (srcProxy instanceof final MapLayerSelectionDialogFragment.MapLayerSelectionDialogFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal());
			}
		}
		if (RoadStyleSelectionDialogFragment.class.equals(fragmentClass.fragment()) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().hostOfPreference();
			if (srcProxy instanceof final ConfigureMapFragment.ConfigureMapFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal().getDialogs().roadStyleDialog().orElseThrow());
			} else if (srcProxy instanceof final RoadStyleSelectionDialogFragment.RoadStyleSelectionDialogFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal());
			}
		}
		if (MultiSelectionDialogFragment.class.equals(fragmentClass.fragment()) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().hostOfPreference();
			if (srcProxy instanceof final ConfigureMapFragment.ConfigureMapFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal().getDialogs().hideDialog().orElseThrow());
			} else if (srcProxy instanceof final MultiSelectionDialogFragment.MultiSelectionDialogFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal());
			}
		}
		if (ConfigureMapDialogs.MapLanguageDialog.class.equals(fragmentClass.fragment()) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().hostOfPreference();
			if (srcProxy instanceof final ConfigureMapFragment.ConfigureMapFragmentProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal().getDialogs().mapLanguageDialog());
			} else if (srcProxy instanceof final ConfigureMapDialogs.MapLanguageDialog.MapLanguageDialogProxy _srcProxy) {
				return Optional.of((T) _srcProxy.getPrincipal());
			}
		}
		if (MapModeFragment.class.equals(fragmentClass.fragment()) && src.isPresent()) {
			final PreferenceFragmentCompat srcProxy = src.orElseThrow().hostOfPreference();
			if (srcProxy instanceof final ConfigureMapFragment.ConfigureMapFragmentProxy _srcProxy) {
				return Optional.of((T) MapModeFragment.createInstanceAndRegisterMapModeController(_srcProxy.getPrincipal().getApp(), _srcProxy.getPrincipal().appMode));
			} else if (srcProxy instanceof final MapModeFragment.MapModeFragmentProxy _srcProxy) {
				return Optional.of((T) MapModeFragment.createInstanceAndRegisterMapModeController(_srcProxy.getPrincipal().getApp(), _srcProxy.getPrincipal().getAppMode()));
			}
		}
		return Optional.empty();
	}

	private static <T extends Fragment> Optional<T> instantiatePreferenceFragmentUsingPreferenceFragmentHandler(
			final FragmentClassOfActivity<T> fragmentClass,
			final Optional<PreferenceOfHostOfActivity> src,
			final Context context) {
		return src
				.filter(preferenceWithHost -> preferenceWithHost.hostOfPreference() instanceof PreferenceFragmentHandlerProvider)
				.flatMap(preferenceWithHost -> ((PreferenceFragmentHandlerProvider) preferenceWithHost.hostOfPreference()).getPreferenceFragmentHandler(preferenceWithHost.preference()))
				.map(preferenceFragmentHandler -> preferenceFragmentHandler.createPreferenceFragment(context, Optional.empty()))
				.flatMap(
						preferenceFragment ->
								fragmentClass.fragment().isAssignableFrom(preferenceFragment.getClass()) ?
										Optional.of((T) preferenceFragment) :
										Optional.empty());
	}

	private static <T extends Fragment> T createDefaultInstance(final FragmentClassOfActivity<T> fragmentClass,
																final Optional<PreferenceOfHostOfActivity> src,
																final Context context,
																final InstantiateAndInitializeFragment instantiateAndInitializeFragment) {
		final T fragment = new DefaultFragmentFactory().instantiate(fragmentClass, src, context, instantiateAndInitializeFragment);
		src.ifPresent(_src -> configureFragment(fragment, _src));
		return fragment;
	}

	private static void configureFragment(final Fragment fragment, final PreferenceOfHostOfActivity src) {
		if (src.hostOfPreference() instanceof final BaseSettingsFragment baseSettingsFragment) {
			fragment.setArguments(baseSettingsFragment.buildArguments());
		}
	}
}
