package net.osmand.plus.settings.fragments.search;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferenceWithHost;
import de.KnollFrank.lib.settingssearch.fragment.DefaultFragmentFactory;
import de.KnollFrank.lib.settingssearch.fragment.FragmentFactory;
import de.KnollFrank.lib.settingssearch.provider.PrepareShow;

class FragmentFactoryAndPrepareShow implements FragmentFactory, PrepareShow {

	@Override
	public Fragment instantiate(final String fragmentClassName,
								final Optional<PreferenceWithHost> src,
								final Context context) {
		final Fragment fragment = _instantiate(fragmentClassName, src, context);
		setConfigureSettingsSearch(fragment, true);
		return fragment;
	}

	@Override
	public void prepareShow(final PreferenceFragmentCompat preferenceFragment) {
		setConfigureSettingsSearch(preferenceFragment, false);
	}

	private static Fragment _instantiate(final String fragmentClassName,
										 final Optional<PreferenceWithHost> src,
										 final Context context) {
		if (src.isPresent() && src.get().host() instanceof final InfoProvider infoProvider) {
			final Optional<Info> info = infoProvider.getInfo(src.get().preference());
			if (info.isPresent()) {
				return info.get().createFragment(context);
			}
		}
		final Fragment fragment = new DefaultFragmentFactory().instantiate(fragmentClassName, src, context);
		src.ifPresent(_src -> configureFragment(fragment, _src));
		return fragment;
	}

	private static void configureFragment(final Fragment fragment, final PreferenceWithHost src) {
		if (src.host() instanceof final BaseSettingsFragment baseSettingsFragment) {
			fragment.setArguments(baseSettingsFragment.buildArguments());
		}
	}

	private static void setConfigureSettingsSearch(final Fragment fragment, final boolean configureSettingsSearch) {
		final Bundle arguments = getBundle(fragment);
		arguments.putBoolean(BaseSettingsFragment.CONFIGURE_SETTINGS_SEARCH, configureSettingsSearch);
		fragment.setArguments(arguments);
	}

	private static Bundle getBundle(final Fragment fragment) {
		return fragment.getArguments() != null ?
				fragment.getArguments() :
				new Bundle();
	}
}
