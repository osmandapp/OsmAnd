package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SettingsSearchConfigurer.setConfigureSettingsSearch;

import androidx.preference.PreferenceFragmentCompat;

class PrepareShow implements de.KnollFrank.lib.settingssearch.provider.PrepareShow {

	@Override
	public void prepareShow(final PreferenceFragmentCompat preferenceFragment) {
		setConfigureSettingsSearch(preferenceFragment, false);
	}
}
