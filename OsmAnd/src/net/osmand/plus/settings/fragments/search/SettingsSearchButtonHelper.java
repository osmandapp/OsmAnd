package net.osmand.plus.settings.fragments.search;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import de.KnollFrank.lib.settingssearch.client.SearchConfigBuilder;
import de.KnollFrank.lib.settingssearch.client.SearchConfiguration;
import de.KnollFrank.lib.settingssearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.*;
import de.KnollFrank.lib.settingssearch.common.task.AsyncTaskWithProgressUpdateListeners;

public class SettingsSearchButtonHelper {

	private final BaseSettingsFragment rootSearchPreferenceFragment;
	private final @IdRes int fragmentContainerViewId;
	private final SearchDatabaseStatusHandler searchDatabaseStatusHandler;
	private final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<?>>> createSearchDatabaseTaskSupplier;
	private final OsmandPreference<String> availableAppModes;

	public SettingsSearchButtonHelper(final BaseSettingsFragment rootSearchPreferenceFragment,
									  final @IdRes int fragmentContainerViewId,
									  final OsmandApplication app,
									  final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<?>>> createSearchDatabaseTaskSupplier) {
		this.rootSearchPreferenceFragment = rootSearchPreferenceFragment;
		this.fragmentContainerViewId = fragmentContainerViewId;
		this.searchDatabaseStatusHandler =
				new SearchDatabaseStatusHandler(
						new SetStringPreference(
								app.getSettings().PLUGINS_COVERED_BY_SETTINGS_SEARCH));
		this.createSearchDatabaseTaskSupplier = createSearchDatabaseTaskSupplier;
		this.availableAppModes = app.getSettings().AVAILABLE_APP_MODES;
	}

	public void configureSettingsSearchButton(final ImageView settingsSearchButton) {
		onClickShowSearchPreferenceFragment(settingsSearchButton);
		settingsSearchButton.setImageDrawable(rootSearchPreferenceFragment.getIcon(R.drawable.searchpreference_ic_search));
		settingsSearchButton.setVisibility(View.VISIBLE);
	}

	public static SearchPreferenceFragments createSearchPreferenceFragments(
			final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<?>>> createSearchDatabaseTaskSupplier,
			final FragmentActivity fragmentActivity,
			final @IdRes int fragmentContainerViewId,
			final Class<? extends BaseSettingsFragment> rootPreferenceFragment,
			final OsmandPreference<String> availableAppModes) {
		final SearchResultsFilter searchResultsFilter =
				SearchResultsFilterFactory.createSearchResultsFilter(
						PreferencePathDisplayerFactory.getApplicationModeKeys(),
						availableAppModes);
		return SearchPreferenceFragments
				.builder(
						new SearchConfiguration(
								fragmentContainerViewId,
								Optional.of("Search Settings"),
								rootPreferenceFragment),
						fragmentActivity.getSupportFragmentManager(),
						fragmentActivity)
				.withSearchDatabaseConfig(
						new SearchDatabaseConfigBuilder()
								.withFragmentFactory(new FragmentFactory())
								.withActivitySearchDatabaseConfigs(createActivitySearchDatabaseConfigs())
								.withPreferenceFragmentConnected2PreferenceProvider(new PreferenceFragmentConnected2PreferenceProvider())
								.withSearchableInfoProvider(SettingsSearchButtonHelper::getSearchableInfo)
								.withPreferenceDialogAndSearchableInfoProvider(new PreferenceDialogAndSearchableInfoProvider())
								.withPreferenceSearchablePredicate(new PreferenceSearchablePredicate())
								.build())
				.withSearchConfig(
						new SearchConfigBuilder(fragmentActivity)
								.withSearchResultsFilter(searchResultsFilter)
								.withPreferencePathDisplayer(PreferencePathDisplayerFactory.createPreferencePathDisplayer(fragmentActivity))
								.withSearchPreferenceFragmentUI(new SearchPreferenceFragmentUI(searchResultsFilter))
								.withSearchResultsFragmentUI(new SearchResultsFragmentUI())
								.withPrepareShow(new PrepareShow())
								.withIncludePreferenceInSearchResultsPredicate(new IncludePreferenceInSearchResultsPredicate())
								.build())
				.withCreateSearchDatabaseTaskSupplier(createSearchDatabaseTaskSupplier)
				.build();
	}

	private static ActivitySearchDatabaseConfigs createActivitySearchDatabaseConfigs() {
		return new ActivitySearchDatabaseConfigs(
				Set.of(new ActivityWithRootPreferenceFragment<>(MapActivity.class, ConfigureMapFragment.PreferenceFragment.class)),
				Set.of(new FragmentWithPreferenceFragmentConnection<>(ConfigureMapFragment.class, ConfigureMapFragment.PreferenceFragment.class)));
	}

	private static Optional<String> getSearchableInfo(final Preference preference) {
		return preference instanceof final SearchableInfoProvider searchableInfoProvider ?
				Optional.of(searchableInfoProvider.getSearchableInfo()) :
				Optional.empty();
	}

	private void onClickShowSearchPreferenceFragment(final ImageView searchPreferenceButton) {
		final SearchPreferenceFragments searchPreferenceFragments =
				createSearchPreferenceFragments(
						createSearchDatabaseTaskSupplier,
						rootSearchPreferenceFragment.requireActivity(),
						fragmentContainerViewId,
						rootSearchPreferenceFragment.getClass(),
						availableAppModes);
		searchPreferenceButton.setOnClickListener(v -> showSearchPreferenceFragment(searchPreferenceFragments));
	}

	private void showSearchPreferenceFragment(final SearchPreferenceFragments searchPreferenceFragments) {
		if (!searchDatabaseStatusHandler.isSearchDatabaseUpToDate()) {
			searchPreferenceFragments.rebuildSearchDatabase();
			searchDatabaseStatusHandler.setSearchDatabaseUpToDate();
		}
		searchPreferenceFragments.showSearchPreferenceFragment();
	}
}
