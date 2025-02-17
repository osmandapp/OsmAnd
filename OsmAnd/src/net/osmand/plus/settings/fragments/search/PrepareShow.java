package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SettingsSearchConfigurer.setConfigureSettingsSearch;

import androidx.fragment.app.Fragment;

class PrepareShow implements de.KnollFrank.lib.settingssearch.provider.PrepareShow {

	@Override
	public void prepareShow(final Fragment fragment) {
		setConfigureSettingsSearch(fragment, false);
	}
}
