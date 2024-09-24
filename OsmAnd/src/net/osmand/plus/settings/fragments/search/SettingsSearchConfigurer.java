package net.osmand.plus.settings.fragments.search;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import net.osmand.plus.settings.fragments.BaseSettingsFragment;

class SettingsSearchConfigurer {

	public static void setConfigureSettingsSearch(final Fragment fragment, final boolean configureSettingsSearch) {
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
