package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;

import java.util.Set;

import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.ActivitySearchDatabaseConfigs;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.ActivityWithRootPreferenceFragment;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.FragmentWithPreferenceFragmentConnection;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.PreferenceFragmentFactory;

class ActivitySearchDatabaseConfigsFactory {

	public static ActivitySearchDatabaseConfigs createActivitySearchDatabaseConfigs() {
		return new ActivitySearchDatabaseConfigs(
				Set.of(new ActivityWithRootPreferenceFragment<>(MapActivity.class, ConfigureMapFragment.PreferenceFragment.class)),
				Set.of(
						new PreferenceFragmentFactory<>(
								new FragmentWithPreferenceFragmentConnection<>(
										ConfigureMapFragment.class,
										ConfigureMapFragment.PreferenceFragment.class)) {

							@Override
							protected void initializePreferenceFragmentWithFragment(final ConfigureMapFragment.PreferenceFragment preferenceFragment, final ConfigureMapFragment fragment) {
								preferenceFragment.beforeOnCreate(fragment);
							}
						}));
	}
}
