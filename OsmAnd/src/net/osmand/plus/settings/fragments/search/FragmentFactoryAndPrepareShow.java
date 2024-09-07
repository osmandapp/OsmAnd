package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.settings.fragments.MainSettingsFragment.APP_PROFILES;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferenceWithHost;
import de.KnollFrank.lib.settingssearch.fragment.DefaultFragmentFactory;
import de.KnollFrank.lib.settingssearch.fragment.FragmentFactory;
import de.KnollFrank.lib.settingssearch.provider.PrepareShow;

class FragmentFactoryAndPrepareShow implements FragmentFactory, PrepareShow {

	@Override
	public Fragment instantiate(final String fragmentClassName, final Optional<PreferenceWithHost> src, final Context context) {
		final Fragment fragment = new DefaultFragmentFactory().instantiate(fragmentClassName, src, context);
		src.ifPresent(_src -> configureFragment(fragment, _src));
		setConfigureSettingsSearch(fragment, true);
		return fragment;
	}

	@Override
	public void prepareShow(final PreferenceFragmentCompat preferenceFragment) {
		setConfigureSettingsSearch(preferenceFragment, false);
	}

	private static void configureFragment(final Fragment fragment, final PreferenceWithHost src) {
		// FK-TODO: DRY: copied from MainSettingsFragment.onPreferenceClick():
		if (src.preference.getParent() != null && APP_PROFILES.equals(src.preference.getParent().getKey())) {
			final ApplicationMode appMode = ApplicationMode.valueOfStringKey(src.preference.getKey(), null);
			final Bundle args = new Bundle();
			if (appMode != null) {
				args.putString(APP_MODE_KEY, appMode.getStringKey());
			}
			fragment.setArguments(args);
		} else if (src.host instanceof final BaseSettingsFragment baseSettingsFragment) {
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
