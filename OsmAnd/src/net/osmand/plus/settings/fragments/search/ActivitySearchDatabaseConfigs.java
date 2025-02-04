package net.osmand.plus.settings.fragments.search;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;

import java.util.List;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.ActivitySearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.ActivityWithRootPreferenceFragment;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.FragmentWithPreferenceFragmentConnection;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.PreferenceFragmentFactory;

class ActivitySearchDatabaseConfigs {

	public static List<ActivitySearchDatabaseConfig<? extends AppCompatActivity, ? extends Fragment, ? extends PreferenceFragmentCompat, ? extends PreferenceFragmentCompat>> createActivitySearchDatabaseConfigs() {
		return List.of(createMapActivitySearchDatabaseConfig());
	}

	private static ActivitySearchDatabaseConfig<MapActivity, ConfigureMapFragment, ConfigureMapFragment.PreferenceFragment, ConfigureMapFragment.PreferenceFragment> createMapActivitySearchDatabaseConfig() {
		return new ActivitySearchDatabaseConfig<>(
				new ActivityWithRootPreferenceFragment<>(MapActivity.class, ConfigureMapFragment.PreferenceFragment.class),
				Optional.of(
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
