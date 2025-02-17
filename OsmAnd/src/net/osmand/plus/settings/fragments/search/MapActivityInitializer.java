package net.osmand.plus.settings.fragments.search;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import net.osmand.plus.settings.fragments.ConfigureProfileFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.settingssearch.provider.ActivityInitializer;

class MapActivityInitializer implements ActivityInitializer<ConfigureProfileFragment> {

	private final FragmentManager fragmentManager;

	public MapActivityInitializer(final FragmentManager fragmentManager) {
		this.fragmentManager = fragmentManager;
	}

	@Override
	public void beforeStartActivity(final ConfigureProfileFragment src) {
		src.setAppModeToSelected();
		SearchPreferenceFragments.hideSearchPreferenceFragment(fragmentManager);
	}

	@Override
	public Optional<Bundle> createExtras(final ConfigureProfileFragment src) {
		return Optional.of(src.createMapActivityExtras());
	}
}
