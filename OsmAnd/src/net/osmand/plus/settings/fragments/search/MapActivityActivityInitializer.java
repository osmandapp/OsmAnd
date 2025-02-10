package net.osmand.plus.settings.fragments.search;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.settings.fragments.ConfigureProfileFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.settingssearch.provider.ActivityInitializer;

class MapActivityActivityInitializer implements ActivityInitializer {

	private final FragmentManager fragmentManager;

	public MapActivityActivityInitializer(final FragmentManager fragmentManager) {
		this.fragmentManager = fragmentManager;
	}

	@Override
	public void beforeStartActivity(final PreferenceFragmentCompat src) {
		final ConfigureProfileFragment configureProfileFragment = (ConfigureProfileFragment) src;
		configureProfileFragment.setAppModeToSelected();
		SearchPreferenceFragments.hideSearchPreferenceFragment(fragmentManager);
	}

	@Override
	public Optional<Bundle> createExtras(final PreferenceFragmentCompat src) {
		final ConfigureProfileFragment configureProfileFragment = (ConfigureProfileFragment) src;
		return Optional.of(configureProfileFragment.createMapActivityExtras());
	}
}
