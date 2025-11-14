package net.osmand.plus.settings.fragments.search;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import de.KnollFrank.lib.settingssearch.MergedPreferenceScreen;
import de.KnollFrank.lib.settingssearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.settingssearch.common.task.AsyncTaskWithProgressUpdateListeners;
import de.KnollFrank.lib.settingssearch.db.preference.db.DAOProvider;

public class SettingsSearchButtonHelper {

	private final BaseSettingsFragment rootSearchPreferenceFragment;
	private final @IdRes int fragmentContainerViewId;
	private final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<Void, DAOProvider>>> createSearchDatabaseTaskSupplier;
	private final OsmandPreference<String> availableAppModes;
	private final TileSourceTemplatesProvider tileSourceTemplatesProvider;
	private final DAOProvider daoProvider;
	private final Configuration configuration;

	public static SettingsSearchButtonHelper of(final BaseSettingsFragment rootSearchPreferenceFragment,
												final @IdRes int fragmentContainerViewId,
												final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<Void, DAOProvider>>> createSearchDatabaseTaskSupplier,
												final OsmandApplication app,
												final Configuration configuration) {
		final DAOProvider daoProvider = app.daoProviderManager.getDAOProvider();
		return new SettingsSearchButtonHelper(
				rootSearchPreferenceFragment,
				fragmentContainerViewId,
				createSearchDatabaseTaskSupplier,
				app.getSettings().AVAILABLE_APP_MODES,
				app.getTileSourceTemplatesProvider(),
				daoProvider,
				configuration);
	}

	private SettingsSearchButtonHelper(final BaseSettingsFragment rootSearchPreferenceFragment,
									   final int fragmentContainerViewId,
									   final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<Void, DAOProvider>>> createSearchDatabaseTaskSupplier,
									   final OsmandPreference<String> availableAppModes,
									   final TileSourceTemplatesProvider tileSourceTemplatesProvider,
									   final DAOProvider daoProvider,
									   final Configuration configuration) {
		this.rootSearchPreferenceFragment = rootSearchPreferenceFragment;
		this.fragmentContainerViewId = fragmentContainerViewId;
		this.createSearchDatabaseTaskSupplier = createSearchDatabaseTaskSupplier;
		this.availableAppModes = availableAppModes;
		this.tileSourceTemplatesProvider = tileSourceTemplatesProvider;
		this.daoProvider = daoProvider;
		this.configuration = configuration;
	}

	public void configureSettingsSearchButton(final ImageView settingsSearchButton) {
		onClickShowSearchPreferenceFragment(settingsSearchButton);
		settingsSearchButton.setImageDrawable(rootSearchPreferenceFragment.getIcon(R.drawable.searchpreference_ic_search));
		settingsSearchButton.setVisibility(View.VISIBLE);
	}

	public static SearchPreferenceFragments createSearchPreferenceFragments(
			final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<Void, DAOProvider>>> createSearchDatabaseTaskSupplier,
			final Consumer<MergedPreferenceScreen> onMergedPreferenceScreenAvailable,
			final FragmentActivity fragmentActivity,
			final @IdRes int fragmentContainerViewId,
			final Class<? extends BaseSettingsFragment> rootPreferenceFragment,
			final OsmandPreference<String> availableAppModes,
			final TileSourceTemplatesProvider tileSourceTemplatesProvider,
			final DAOProvider daoProvider,
			final Configuration configuration) {
		return SearchPreferenceFragments
				.builder(
						SearchDatabaseConfigFactory.createSearchDatabaseConfig(
								rootPreferenceFragment,
								tileSourceTemplatesProvider,
								fragmentActivity.getSupportFragmentManager()),
						SearchConfigFactory.createSearchConfig(
								fragmentActivity,
								fragmentContainerViewId,
								availableAppModes),
						fragmentActivity,
						daoProvider,
						new ConfigurationBundleConverter().doForward(configuration))
				.withCreateSearchDatabaseTaskSupplier(createSearchDatabaseTaskSupplier)
				.withOnMergedPreferenceScreenAvailable(onMergedPreferenceScreenAvailable)
				.build();
	}

	private void onClickShowSearchPreferenceFragment(final ImageView searchPreferenceButton) {
		final SearchPreferenceFragments searchPreferenceFragments =
				createSearchPreferenceFragments(
						createSearchDatabaseTaskSupplier,
						mergedPreferenceScreen -> {
						},
						rootSearchPreferenceFragment.requireActivity(),
						fragmentContainerViewId,
						rootSearchPreferenceFragment.getClass(),
						availableAppModes,
						tileSourceTemplatesProvider,
						daoProvider,
						configuration);
		searchPreferenceButton.setOnClickListener(v -> showSearchPreferenceFragment(searchPreferenceFragments));
	}

	private void showSearchPreferenceFragment(final SearchPreferenceFragments searchPreferenceFragments) {
		searchPreferenceFragments.showSearchPreferenceFragment();
	}
}
